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
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.overlay.IslandContent
import io.github.ptt98hd.totch.core.overlay.IslandHit
import io.github.ptt98hd.totch.core.overlay.IslandLayout
import io.github.ptt98hd.totch.core.overlay.IslandMetrics
import io.github.ptt98hd.totch.core.overlay.IslandTap
import io.github.ptt98hd.totch.core.overlay.overlayVisible
import io.github.ptt98hd.totch.core.model.TouchEvent
import io.github.ptt98hd.totch.core.settings.needsTouchBlocking
import io.github.ptt98hd.totch.engine.TouchSource
import io.github.ptt98hd.totch.overlay.NotchOverlayWindow
import io.github.ptt98hd.totch.overlay.OverlayFrame
import io.github.ptt98hd.totch.service.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Accessibility service (detection-modes §6–§8): runs actions — global actions, launching apps, shortcuts and
 * activities from the background — and puts an overlay on the notch. Without Shizuku the overlay is the touch source
 * and passes its touches to the engine (FR-D13); with Shizuku it only swallows them while a swipe-down gesture is on,
 * so the system does not pull down the shade (FR-G14). Also hosts the notch overlay — indicator, island and touch
 * effects (notch-overlay, ADR-8) — and only receives window-state events of System UI, to know the shade opened (FR-O8).
 */
class NotchAccessibilityService : AccessibilityService() {
    private var platform: DefaultAppContainer? = null
    private var scope: CoroutineScope? = null
    private var overlay: NotchView? = null
    private var visuals: NotchOverlayWindow? = null

    override fun onServiceConnected() {
        val platform = (application as TotchApp).platform.also { platform = it }
        platform.accessibility.attach(this)
        platform.setAccessibilityWanted(true) // DM-53
        val scope = MainScope().also { scope = it }
        val visuals = NotchOverlayWindow(this, platform.overlay).also { visuals = it }
        // notch-overlay §1: the drawing window, while a feature is on (OV-1…OV-5; OV-4 is checked inside).
        val frames = combine(platform.settingsRepository.settings, platform.touchArea.area, platform.display.state) { s, area, d ->
            area?.takeIf { overlayVisible(s, accessibility = true, rotation = d.rotation, statusBarVisible = true) }?.let {
                OverlayFrame(it.notch, IslandMetrics(d.widthPx, d.density), s.notificationIndicator, s.island, s.touchEffects, s.longPressMs)
            }
        }.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, null)
        scope.launch { frames.collect(visuals::update) }
        // The island catches touches only while it shows something to tap (§3.4).
        val islandTarget = combine(frames, platform.overlay.islandContent) { frame, content ->
            frame?.takeIf { it.island && content.isTappable() }?.let { IslandTarget(it, IslandLayout.pill(content, it.notch, it.metrics)) }
        }.distinctUntilChanged()
        scope.launch {
            combine(
                platform.settingsRepository.settings,
                platform.engineStatus,
                platform.region,
                platform.touchSource,
                islandTarget,
            ) { settings, status, region, source, island ->
                val forward = source == TouchSource.ACCESSIBILITY
                region.takeIf { status == EngineStatus.Running && (forward || settings.needsTouchBlocking() || island != null) } // DM-15
                    ?.let { OverlaySpec(it, forward, island) }
            }.distinctUntilChanged().collect(::updateOverlay)
        }
    }

    /** FR-O8: System UI changed window (shade, quick settings, lock screen); the volume dialog is not the shade. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.packageName != SYSTEM_UI) return
        if (event.className?.contains("Volume", ignoreCase = true) == true) return
        platform?.overlay?.onShadeOpened()
    }

    override fun onInterrupt() = Unit

    /**
     * The system unbinds a live service only when it is switched off (by the user, or Home); a killed or force-stopped
     * process never gets here, so Totch may turn it back on then (DM-53, FR-D15).
     */
    override fun onUnbind(intent: Intent?): Boolean {
        platform?.setAccessibilityWanted(false)
        tearDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }

    private fun IslandContent.isTappable() = this is IslandContent.Media || this is IslandContent.Notification

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

    /** Starts [intent] as a new task: Settings pages, contacts and app intents (FR-A21…FR-A23). */
    fun start(intent: Intent) {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Opens the default assistant (FR-A11); ACTION_ASSIST first, voice command as fallback. */
    fun openAssistant() {
        val intent = listOf(Intent.ACTION_ASSIST, Intent.ACTION_VOICE_COMMAND)
            .map { Intent(it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            .firstOrNull { it.resolveActivity(packageManager) != null }
            ?: error("no assistant")
        startActivity(intent)
    }

    private fun updateOverlay(spec: OverlaySpec?) {
        val windowManager = getSystemService(WindowManager::class.java)
        if (spec == null) {
            overlay?.let { runCatching { windowManager.removeViewImmediate(it) } }
            overlay = null
            return
        }
        val rect = spec.window
        val view = overlay ?: NotchView(this)
        view.onTouch = if (spec.forward) platform?.engine?.let { engine -> engine::onTouch } else null
        view.touchRect = spec.rect
        view.onIslandTap = spec.island?.let { target -> { x: Float, y: Float -> islandTap(target.frame, x, y) } }
        overlay = try {
            if (overlay == null) windowManager.addView(view, layoutParams(rect)) else windowManager.updateViewLayout(view, layoutParams(rect))
            view
        } catch (e: RuntimeException) { // BadTokenException / IllegalStateException when the service is going away
            Log.w(TAG, "Overlay update failed", e)
            null
        }
    }

    /** §3.4: a tap on the island (outside the gesture region). */
    private fun islandTap(frame: OverlayFrame, x: Float, y: Float) {
        val platform = platform ?: return
        val hub = platform.overlay
        val handled = when (val hit = IslandLayout.hit(hub.islandContent.value, frame.notch, frame.metrics, x, y)) {
            is IslandHit.Control -> {
                hub.onIslandControl(hit.control)
                true
            }
            IslandHit.Body -> when (hub.onIslandTap()) {
                IslandTap.OPEN_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                IslandTap.NONE -> false
                else -> true
            }
            IslandHit.Outside -> false
        }
        if (handled && platform.settingsEnabled && platform.hapticsEnabled) platform.feedback.onRecognized()
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
        visuals?.update(null)
        visuals = null
        platform?.accessibility?.detach(this)
        platform = null
    }

    /**
     * Overlay on the gesture region [rect]; [forward] passes its touches to the engine (touch source = Accessibility,
     * FR-D13). With [island] the window also covers the tappable island (notch-overlay §1).
     */
    private data class OverlaySpec(val rect: Rect, val forward: Boolean, val island: IslandTarget? = null) {
        val window: Rect
            get() {
                val pill = island?.pill ?: return rect
                return Rect(minOf(rect.left, pill.left), minOf(rect.top, pill.top), maxOf(rect.right, pill.right), maxOf(rect.bottom, pill.bottom))
            }
    }

    /** The tappable island: its frame and current [pill] in screen pixels. */
    private data class IslandTarget(val frame: OverlayFrame, val pill: Rect)

    /**
     * Swallows every touch (DM-16). With [onTouch] set it also reports them (detection-modes §8); otherwise gestures
     * come from getevent and these events are ignored.
     */
    @SuppressLint("ViewConstructor")
    private class NotchView(context: Context) : View(context) {
        @Volatile var onTouch: ((TouchEvent) -> Unit)? = null
        /** The gesture region; a touch starting outside it is on the island. */
        var touchRect: Rect? = null
        var onIslandTap: ((Float, Float) -> Unit)? = null

        private val slop = ViewConfiguration.get(context).scaledTouchSlop
        private var onIsland = false
        private var islandMoved = false
        private var downX = 0f
        private var downY = 0f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onIsland = onIslandTap != null && touchRect?.contains(event.rawX, event.rawY) != true
                islandMoved = false
                downX = event.rawX
                downY = event.rawY
            }
            if (onIsland) {
                islandTouch(event)
                return true
            }
            onTouch?.let { report ->
                touchEventOf(event.actionMasked, event.rawX, event.rawY, event.eventTime, event.pointerCount)?.let(report)
            }
            return true
        }

        /** notch-overlay §3.4: a release within the touch slop is a tap. */
        private fun islandTouch(event: MotionEvent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE ->
                    if (hypot(event.rawX - downX, event.rawY - downY) > slop || event.pointerCount > 1) islandMoved = true
                MotionEvent.ACTION_UP -> if (!islandMoved) onIslandTap?.invoke(event.rawX, event.rawY)
            }
        }
    }

    private companion object {
        const val TAG = "TotchA11y"
        const val SYSTEM_UI = "com.android.systemui"
        const val URI_GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }
}
