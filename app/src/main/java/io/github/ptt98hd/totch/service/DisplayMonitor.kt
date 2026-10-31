package io.github.ptt98hd.totch.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.WindowInsets
import android.view.WindowManager
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.engine.DisplaySnapshot
import io.github.ptt98hd.totch.engine.toNatural
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * Default display size, rotation, cutout and status bar height. Re-read on DisplayManager callbacks, configuration
 * changes and whatever else calls [refresh] (FR-O13, notch-overlay §7).
 */
class DisplayMonitor(private val context: Context, scope: CoroutineScope) {
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val refresher = DisplayRefresher(scope, ::snapshot)

    val state: StateFlow<DisplaySnapshot> = refresher.state

    init {
        displayManager.registerDisplayListener(
            object : DisplayManager.DisplayListener {
                override fun onDisplayChanged(displayId: Int) {
                    if (displayId == Display.DEFAULT_DISPLAY) refresh()
                }

                override fun onDisplayAdded(displayId: Int) = Unit
                override fun onDisplayRemoved(displayId: Int) = Unit
            },
            Handler(Looper.getMainLooper()),
        )
        // Delivered to this process (it has a foreground service) on every rotation, also when no display event is.
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = refresh()
            },
            IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED),
            Context.RECEIVER_NOT_EXPORTED,
        )
    }

    /** The display may have changed: read it now and again shortly (DR-2). */
    fun refresh() = refresher.refresh()

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
        // FR-R8: every cutout rect seen from the natural orientation; the ones on its top edge.
        val naturalCutout = display.cutout?.boundingRects.orEmpty()
            .map { toNatural(Rect(it.left, it.top, it.right, it.bottom), display.rotation, size.x, size.y) }
            .filter { it.top == 0 && it.width > 0 && it.height > 0 }
        return DisplaySnapshot(size.x, size.y, display.rotation, statusBarHeight(density), cutout, density, naturalCutout)
    }

    private fun statusBarHeight(density: Float): Int = runCatching {
        context.getSystemService(WindowManager::class.java).maximumWindowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
    }.getOrNull()?.takeIf { it > 0 } ?: (DEFAULT_STATUS_BAR_DP * density).roundToInt()

    private companion object {
        const val DEFAULT_STATUS_BAR_DP = 24
    }
}
