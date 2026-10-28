package io.github.ow3n.totch.service

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.WindowInsets
import android.view.WindowManager
import io.github.ow3n.totch.core.model.Rect
import io.github.ow3n.totch.engine.DisplaySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

/** Default display size, rotation, cutout and status bar height; updated by DisplayManager callbacks. */
class DisplayMonitor(private val context: Context, scope: CoroutineScope) {
    private val displayManager = context.getSystemService(DisplayManager::class.java)

    val state: StateFlow<DisplaySnapshot> = callbackFlow {
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == Display.DEFAULT_DISPLAY) trySend(snapshot())
            }

            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
        }
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        send(snapshot())
        awaitClose { displayManager.unregisterDisplayListener(listener) }
    }.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, snapshot())

    /**
     * Uses [Display] APIs because the app context is not a UI context (the foreground service has
     * no window); `getRealSize` returns the logical size in the current rotation.
     */
    @Suppress("DEPRECATION")
    private fun snapshot(): DisplaySnapshot {
        val density = context.resources.displayMetrics.density
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            ?: return DisplaySnapshot(0, 0, 0, 0, emptyList(), density)
        val size = Point().also(display::getRealSize)
        val cutout = display.cutout?.boundingRectTop?.takeUnless { it.isEmpty }
            ?.let { listOf(Rect(it.left, it.top, it.right, it.bottom)) }
            .orEmpty()
        return DisplaySnapshot(size.x, size.y, display.rotation, statusBarHeight(density), cutout, density)
    }

    private fun statusBarHeight(density: Float): Int = runCatching {
        context.getSystemService(WindowManager::class.java).maximumWindowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
    }.getOrNull()?.takeIf { it > 0 } ?: (DEFAULT_STATUS_BAR_DP * density).roundToInt()

    private companion object {
        const val DEFAULT_STATUS_BAR_DP = 24
    }
}
