package io.github.ptt98hd.totch.engine

import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.HighlightArea
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.region.Cutout
import io.github.ptt98hd.totch.core.region.ManualRegion
import io.github.ptt98hd.totch.core.region.NotchRegionResolver
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import kotlin.math.roundToInt

data class ShizukuState(val running: Boolean = false, val granted: Boolean = false) {
    val ready: Boolean get() = running && granted
}

enum class PipelineError { NO_TOUCHSCREEN, STREAM_ENDED, SERVICE_FAILED }

/** State of the Shizuku touch pipeline (UserService + getevent stream). */
sealed interface PipelineState {
    data object Idle : PipelineState
    data object Connecting : PipelineState
    data object Running : PipelineState
    data class Failed(val error: PipelineError) : PipelineState
}

/**
 * Display state in the current rotation; [rotation] is a `Surface.ROTATION_*` value. [naturalCutout] is the cutout on
 * the top edge of the natural (ROTATION_0) orientation, in natural coordinates (FR-R8).
 */
data class DisplaySnapshot(
    val widthPx: Int,
    val heightPx: Int,
    val rotation: Int,
    val statusBarPx: Int,
    val cutoutTop: List<Rect>,
    val density: Float,
    val naturalCutout: List<Rect> = cutoutTop,
) {
    /** Size in the natural (ROTATION_0) orientation, as used by getevent coordinates. */
    val naturalSize: Pair<Int, Int>
        get() = if (rotation % 2 == 0) widthPx to heightPx else heightPx to widthPx

    /** The same display seen in its natural orientation (NR-17). */
    fun natural(): DisplaySnapshot {
        val (w, h) = naturalSize
        return DisplaySnapshot(w, h, 0, statusBarPx, naturalCutout, density, naturalCutout)
    }
}

/** NR-15, NR-16: [rect] of a display rotated by [rotation] ([widthPx] × [heightPx] as rotated) in natural coordinates. */
fun toNatural(rect: Rect, rotation: Int, widthPx: Int, heightPx: Int): Rect {
    fun point(x: Int, y: Int): Pair<Int, Int> = when (rotation) {
        1 -> heightPx - y to x
        2 -> widthPx - x to heightPx - y
        3 -> y to widthPx - x
        else -> x to y
    }
    val (x1, y1) = point(rect.left, rect.top)
    val (x2, y2) = point(rect.right, rect.bottom)
    return Rect(minOf(x1, x2), minOf(y1, y2), maxOf(x1, x2), maxOf(y1, y2))
}

/** Notch touch region for [settings] on [display], or `null` when inactive (FR-R4). */
fun notchRegion(settings: Settings, display: DisplaySnapshot): Rect? {
    fun px(dp: Int) = (dp * display.density).roundToInt()
    return NotchRegionResolver.resolve(
        screenWidthPx = display.widthPx,
        statusBarHeightPx = display.statusBarPx,
        cutout = Cutout(display.cutoutTop),
        manual = settings.manualRegion?.let { ManualRegion(it.centerXFraction, px(it.widthDp), px(it.heightDp), px(it.topDp)) },
        paddingPx = px(settings.paddingDp),
        rotation = display.rotation,
    )
}

/** Region the engine listens to, in natural coordinates like getevent: rotated only with landscape gestures (NR-17). */
fun engineRegion(settings: Settings, display: DisplaySnapshot): Rect? = when {
    display.rotation == 0 -> notchRegion(settings, display)
    settings.landscapeGestures -> notchRegion(settings, display.natural())
    else -> null
}

/** The automatically detected region (cutout or default, no padding) in dp, or `null` when rotated (NR-12). */
fun detectedRegionDp(display: DisplaySnapshot): ManualRegionDp? {
    val rect = NotchRegionResolver.resolve(
        screenWidthPx = display.widthPx,
        statusBarHeightPx = display.statusBarPx,
        cutout = Cutout(display.cutoutTop),
        manual = null,
        paddingPx = 0,
        rotation = display.rotation,
    ) ?: return null
    return ManualRegionDp(
        centerXFraction = (rect.left + rect.right) / 2f / display.widthPx,
        widthDp = (rect.width / display.density).roundToInt(),
        heightDp = (rect.height / display.density).roundToInt(),
    )
}

/** Where touches at the notch come from (FR-D13, detection-modes §8). */
enum class TouchSource { NONE, SHIZUKU, ACCESSIBILITY }

/** DM-40: Shizuku while it is ready and getevent works, otherwise the accessibility overlay. */
fun touchSource(shizuku: ShizukuState, pipeline: PipelineState, accessibility: Boolean): TouchSource = when {
    shizuku.ready && pipeline !is PipelineState.Failed -> TouchSource.SHIZUKU
    accessibility -> TouchSource.ACCESSIBILITY
    shizuku.ready -> TouchSource.SHIZUKU // failed: shown as Error
    else -> TouchSource.NONE
}

/** Combines every input of the engine into the single status shown by the UI (detection-modes §1, §8). */
fun resolveEngineStatus(
    settings: Settings,
    shizuku: ShizukuState,
    pipeline: PipelineState,
    rotation: Int,
    accessibility: Boolean,
    errorMessage: (PipelineError) -> String,
): EngineStatus {
    if (!settings.enabled) {
        return when (pipeline) {
            PipelineState.Running, PipelineState.Connecting -> EngineStatus.Stopping // DM-30
            else -> EngineStatus.Disabled
        }
    }
    return when (touchSource(shizuku, pipeline, accessibility)) {
        TouchSource.NONE -> EngineStatus.NeedsAccess // DM-41
        TouchSource.ACCESSIBILITY -> if (rotation != 0) EngineStatus.PausedRotation else EngineStatus.Running // DM-42
        TouchSource.SHIZUKU -> when {
            pipeline is PipelineState.Failed -> EngineStatus.Error(errorMessage(pipeline.error))
            rotation != 0 && !settings.landscapeGestures -> EngineStatus.PausedRotation // DM-31
            pipeline == PipelineState.Running -> EngineStatus.Running
            else -> EngineStatus.Starting // DM-30
        }
    }
}

/** [notchRegion] and the same region without padding, for the highlight (ui spec §2); null when inactive. */
fun highlightArea(settings: Settings, display: DisplaySnapshot): HighlightArea? {
    val touch = notchRegion(settings, display) ?: return null
    val notch = notchRegion(settings.copy(paddingDp = 0), display) ?: touch
    return HighlightArea(touch, notch)
}
