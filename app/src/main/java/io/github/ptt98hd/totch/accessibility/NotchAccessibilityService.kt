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
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.settings.needsTouchBlocking
import io.github.ptt98hd.totch.service.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Optional accessibility service (detection-modes §6, §7): runs actions — global actions, launching apps, shortcuts
 * and activities from the background — and blocks touches at the notch with an empty overlay while a swipe-down
 * gesture is on, so the system does not pull down the shade (FR-G14). Touches are still read through Shizuku; the
 * overlay only swallows them. Receives no accessibility events.
 */
class NotchAccessibilityService : AccessibilityService() {
    private var platform: DefaultAppContainer? = null
    private var scope: CoroutineScope? = null
    private var overlay: View? = null

    override fun onServiceConnected() {
        val platform = (application as TotchApp).platform.also { platform = it }
        platform.accessibility.attach(this)
        val scope = MainScope().also { scope = it }
        scope.launch {
            combine(platform.settingsRepository.settings, platform.engineStatus, platform.region) { settings, status, region ->
                region.takeIf { status == EngineStatus.Running && settings.needsTouchBlocking() } // DM-15
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

    /** Starts an exported activity of another app (FR-A14). */
    fun launchActivity(packageName: String, activityName: String) {
        startActivity(Intent().setClassName(packageName, activityName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
        val windowManager = getSystemService(WindowManager::class.java)
        if (rect == null) {
            overlay?.let { runCatching { windowManager.removeViewImmediate(it) } }
            overlay = null
            return
        }
        val view = overlay ?: BlockingView(this)
        overlay = try {
            if (overlay == null) windowManager.addView(view, layoutParams(rect)) else windowManager.updateViewLayout(view, layoutParams(rect))
            view
        } catch (e: RuntimeException) { // BadTokenException / IllegalStateException when the service is going away
            Log.w(TAG, "Overlay update failed", e)
            null
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

    private fun tearDown() {
        scope?.cancel()
        scope = null
        updateOverlay(null)
        platform?.accessibility?.detach(this)
        platform = null
    }

    /** Swallows every touch (DM-16); gestures are recognized from getevent, not from these events. */
    @SuppressLint("ViewConstructor")
    private class BlockingView(context: Context) : View(context) {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean = true
    }

    private companion object {
        const val TAG = "TotchA11y"
        const val URI_GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }
}
