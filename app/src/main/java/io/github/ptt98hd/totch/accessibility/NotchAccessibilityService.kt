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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
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

    /** Opens a static app shortcut from its stored intent URI, without selector or URI grants (FR-A12). */
    fun launchShortcut(intentUri: String) {
        val intent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME).apply {
            selector = null
            removeFlags(URI_GRANT_FLAGS)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    /** Opens the default assistant (FR-A11); ACTION_ASSIST first, voice command as fallback. */
    fun openAssistant() {
        val intent = listOf(Intent.ACTION_ASSIST, Intent.ACTION_VOICE_COMMAND)
            .map { Intent(it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            .firstOrNull { it.resolveActivity(packageManager) != null }
            ?: error("no assistant")
        startActivity(intent)
    }

    private fun updateOverlay(rect: Rect?) {
        platform.engine.reset()
        if (rect == null) {
            removeView(overlay)
            overlay = null
            return
        }
        val view = overlay ?: TouchView(this) { platform.engine.onTouch(it) }
        overlay = view.takeIf { place(it, rect, attached = overlay != null) }
    }

    private fun place(view: View, rect: Rect, attached: Boolean): Boolean = try {
        val params = layoutParams(rect)
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

    private fun layoutParams(rect: Rect) = LayoutParams(
        rect.width,
        rect.height,
        LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = rect.left
        y = rect.top
        layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        fitInsetsTypes = 0
        title = "Totch notch"
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
        const val URI_GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }
}
