package io.github.ptt98hd.totch.accessibility

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.model.TouchAction
import io.github.ptt98hd.totch.core.model.TouchEvent
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.service.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Accessibility mode (FR-D3, DM-10..DM-14): a transparent `TYPE_ACCESSIBILITY_OVERLAY` window over the
 * notch region consumes touches and feeds them to the engine. Receives no accessibility events.
 */
class NotchAccessibilityService : AccessibilityService() {
    private lateinit var platform: DefaultAppContainer
    private lateinit var windowManager: WindowManager
    private var scope: CoroutineScope? = null
    private var overlay: View? = null
    private var previewJob: Job? = null

    override fun onServiceConnected() {
        platform = (application as TotchApp).platform
        windowManager = getSystemService(WindowManager::class.java)
        val scope = MainScope().also { scope = it }
        platform.accessibility.attach(this)
        scope.launch {
            combine(platform.settingsRepository.settings, platform.region) { settings, region ->
                region.takeIf { settings.enabled && settings.mode == DetectionMode.ACCESSIBILITY }
            }.distinctUntilChanged().collect(::updateOverlay)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        tearDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }

    /** Launches [packageName]'s launcher activity; allowed from the background for a bound a11y service. */
    fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: error("not installed")
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Tints [rect] with the dynamic primary color for [durationMs] without intercepting touches. */
    fun showPreview(rect: Rect, durationMs: Long): Boolean {
        val scope = scope ?: return false
        previewJob?.cancel()
        previewJob = scope.launch {
            val view = View(this@NotchAccessibilityService).apply {
                setBackgroundColor(getColor(android.R.color.system_accent1_500) and 0x00FFFFFF or PREVIEW_ALPHA)
            }
            if (!place(view, rect, touchable = false, attached = false)) return@launch
            try {
                delay(durationMs)
            } finally {
                removeView(view)
            }
        }
        return true
    }

    private fun updateOverlay(rect: Rect?) {
        platform.engine.reset()
        if (rect == null) {
            removeView(overlay)
            overlay = null
            return
        }
        val view = overlay ?: TouchView(this) { platform.engine.onTouch(it) }
        overlay = view.takeIf { place(it, rect, touchable = true, attached = overlay != null) }
    }

    private fun place(view: View, rect: Rect, touchable: Boolean, attached: Boolean): Boolean = try {
        val params = layoutParams(rect, touchable)
        if (attached) windowManager.updateViewLayout(view, params) else windowManager.addView(view, params)
        true
    } catch (e: RuntimeException) { // BadTokenException / IllegalStateException when the service is going away
        Log.w(TAG, "Overlay update failed", e)
        false
    }

    private fun removeView(view: View?) {
        if (view != null) runCatching { windowManager.removeViewImmediate(view) }
    }

    private fun tearDown() {
        scope?.cancel()
        scope = null
        removeView(overlay)
        overlay = null
        if (::platform.isInitialized) {
            platform.accessibility.detach(this)
            platform.engine.reset()
        }
    }

    private fun layoutParams(rect: Rect, touchable: Boolean) = LayoutParams(
        rect.width,
        rect.height,
        LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if (touchable) 0 else LayoutParams.FLAG_NOT_TOUCHABLE),
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = rect.left
        y = rect.top
        layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        fitInsetsTypes = 0
        title = if (touchable) "Totch notch" else "Totch preview"
    }

    /** Converts screen-space touches (rawX/rawY, uptime clock) into [TouchEvent]s. */
    @SuppressLint("ViewConstructor")
    private class TouchView(context: Context, private val onEvent: (TouchEvent) -> Unit) : View(context) {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val action = when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> TouchAction.DOWN
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> TouchAction.MOVE // FR-G7
                MotionEvent.ACTION_UP -> TouchAction.UP
                MotionEvent.ACTION_CANCEL -> TouchAction.CANCEL
                else -> return true
            }
            onEvent(TouchEvent(action, event.rawX, event.rawY, event.eventTime, event.pointerCount))
            return true
        }
    }

    private companion object {
        const val TAG = "TotchA11y"
        const val PREVIEW_ALPHA = 0x66 shl 24 // ~40 %
    }
}
