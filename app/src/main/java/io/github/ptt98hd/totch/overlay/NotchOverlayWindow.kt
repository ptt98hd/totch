package io.github.ptt98hd.totch.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.overlay.IslandMetrics
import io.github.ptt98hd.totch.engine.fromNatural
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** What the drawing window needs from settings and the display (notch-overlay §1, §8). */
data class OverlayFrame(
    /** Notch without padding, in natural (ROTATION_0) screen pixels. */
    val notch: Rect,
    /** Metrics of the natural orientation. */
    val metrics: IslandMetrics,
    val indicator: Boolean,
    val island: Boolean,
    val touchEffects: Boolean,
    val longPressMs: Long,
    /** `Surface.ROTATION_*` of the display, and its size as rotated. */
    val rotation: Int = 0,
    val displayWidthPx: Int = metrics.screenWidthPx,
    val displayHeightPx: Int = 0,
) {
    /** Room for the expanded island and its glow. */
    val heightPx: Int get() = notch.bottom + metrics.dp(136 + 24)

    /** The window on the current display: the natural top strip, rotated (§8). */
    val window: Rect get() = fromNatural(Rect(0, 0, metrics.screenWidthPx, heightPx), rotation, displayWidthPx, displayHeightPx)
}

/** RO-1: a natural point `p` is drawn at `rotate(p, degrees) + (x, y)` in the window (clockwise, about the origin). */
data class NaturalLayer(val degrees: Float, val x: Int, val y: Int) {
    fun map(px: Float, py: Float): Pair<Float, Float> {
        val r = Math.toRadians(degrees.toDouble())
        val c = cos(r).roundToInt()
        val s = sin(r).roundToInt()
        return px * c - py * s + x to px * s + py * c + y
    }
}

/** RO-1: how the natural content is placed in a [windowW] × [windowH] window on a display rotated by [rotation]. */
fun naturalLayer(rotation: Int, windowW: Int, windowH: Int): NaturalLayer = when (rotation) {
    1 -> NaturalLayer(270f, 0, windowH)
    2 -> NaturalLayer(180f, windowW, windowH)
    3 -> NaturalLayer(90f, windowW, 0)
    else -> NaturalLayer(0f, 0, 0)
}

/**
 * The full-width, untouchable accessibility overlay that draws the indicator, island and touch effects above the
 * status bar (ADR-8), along the edge with the notch in every rotation (§8). [onInsets] hears every insets change — a
 * hint that the display may have rotated (FR-O13). Main thread only.
 */
class NotchOverlayWindow(private val context: Context, private val hub: OverlayHub, private val onInsets: () -> Unit = {}) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val frame = MutableStateFlow<OverlayFrame?>(null)
    private var view: ComposeView? = null
    private var owner: OverlayLifecycleOwner? = null

    fun update(next: OverlayFrame?) {
        frame.value = next
        if (next == null) return remove()
        val current = view
        try {
            if (current == null) {
                add(next)
            } else {
                windowManager.updateViewLayout(current, params(next))
                current.requestApplyInsets() // OV-4: read the status bar again after a rotation
            }
        } catch (e: RuntimeException) { // service going away
            Log.w(TAG, "Overlay window update failed", e)
            remove()
        }
    }

    private fun add(f: OverlayFrame) {
        val lifecycle = OverlayLifecycleOwner().also { it.start() }
        val v = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setOnApplyWindowInsetsListener { _, insets ->
                hub.windowStatusBar.value = insets.isVisible(WindowInsets.Type.statusBars()) // OV-4
                onInsets()
                insets
            }
            setContent {
                val current by frame.collectAsState()
                current?.let { NotchOverlayContent(hub, it) }
            }
        }
        windowManager.addView(v, params(f))
        view = v
        owner = lifecycle
    }

    private fun remove() {
        view?.let { runCatching { windowManager.removeViewImmediate(it) } }
        owner?.destroy()
        view = null
        owner = null
        hub.windowStatusBar.value = true
    }

    private fun params(f: OverlayFrame) = LayoutParams(
        f.window.width,
        f.window.height,
        LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        LayoutParams.FLAG_NOT_TOUCHABLE or LayoutParams.FLAG_NOT_FOCUSABLE or
            LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = f.window.left
        y = f.window.top
        layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        fitInsetsTypes = 0
        title = "Totch island"
    }

    private companion object {
        const val TAG = "TotchOverlay"
    }
}

/** Lifecycle for a Compose view living in a service window rather than an activity. */
private class OverlayLifecycleOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    fun start() {
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
