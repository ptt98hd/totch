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
import kotlinx.coroutines.flow.MutableStateFlow

/** What the drawing window needs from settings and the display (notch-overlay §1). */
data class OverlayFrame(
    /** Notch without padding, in screen pixels (portrait). */
    val notch: Rect,
    val metrics: IslandMetrics,
    val indicator: Boolean,
    val island: Boolean,
    val touchEffects: Boolean,
    val longPressMs: Long,
) {
    /** Room for the expanded island and its glow. */
    val heightPx: Int get() = notch.bottom + metrics.dp(136 + 24)
}

/**
 * The full-width, untouchable accessibility overlay that draws the indicator, island and touch effects above the
 * status bar (ADR-8). Main thread only.
 */
class NotchOverlayWindow(private val context: Context, private val hub: OverlayHub) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val frame = MutableStateFlow<OverlayFrame?>(null)
    private var view: ComposeView? = null
    private var owner: OverlayLifecycleOwner? = null

    fun update(next: OverlayFrame?) {
        frame.value = next
        if (next == null) return remove()
        val current = view
        try {
            if (current == null) add(next) else windowManager.updateViewLayout(current, params(next))
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
                hub.statusBarVisible.value = insets.isVisible(WindowInsets.Type.statusBars()) // OV-4
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
        hub.statusBarVisible.value = true
    }

    private fun params(f: OverlayFrame) = LayoutParams(
        LayoutParams.MATCH_PARENT,
        f.heightPx,
        LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        LayoutParams.FLAG_NOT_TOUCHABLE or LayoutParams.FLAG_NOT_FOCUSABLE or
            LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
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
