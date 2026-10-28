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

/** Display state in the current rotation; [rotation] is a `Surface.ROTATION_*` value. */
data class DisplaySnapshot(
    val widthPx: Int,
    val heightPx: Int,
    val rotation: Int,
    val statusBarPx: Int,
    val cutoutTop: List<Rect>,
    val density: Float,
) {
    /** Size in the natural (ROTATION_0) orientation, as used by getevent coordinates. */
    val naturalSize: Pair<Int, Int>
        get() = if (rotation % 2 == 0) widthPx to heightPx else heightPx to widthPx
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

/** Combines every input of the engine into the single status shown by the UI (detection-modes §1). */
fun resolveEngineStatus(
    settings: Settings,
    shizuku: ShizukuState,
    pipeline: PipelineState,
    rotation: Int,
    errorMessage: (PipelineError) -> String,
): EngineStatus = when {
    !settings.enabled -> when (pipeline) {
        PipelineState.Running, PipelineState.Connecting -> EngineStatus.Stopping // DM-30
        else -> EngineStatus.Disabled
    }
    !shizuku.running -> EngineStatus.WaitingShizuku
    !shizuku.granted -> EngineStatus.ShizukuPermissionRequired
    pipeline is PipelineState.Failed -> EngineStatus.Error(errorMessage(pipeline.error))
    rotation != 0 -> EngineStatus.PausedRotation
    pipeline == PipelineState.Running -> EngineStatus.Running
    else -> EngineStatus.Starting // DM-30
}

/** [notchRegion] and the same region without padding, for the highlight (ui spec §2); null when inactive. */
fun highlightArea(settings: Settings, display: DisplaySnapshot): HighlightArea? {
    val touch = notchRegion(settings, display) ?: return null
    val notch = notchRegion(settings.copy(paddingDp = 0), display) ?: touch
    return HighlightArea(touch, notch)
}
