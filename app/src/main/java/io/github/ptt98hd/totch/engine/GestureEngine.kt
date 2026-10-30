package io.github.ptt98hd.totch.engine

import io.github.ptt98hd.totch.core.action.DispatchResult
import io.github.ptt98hd.totch.core.gesture.GestureConfig
import io.github.ptt98hd.totch.core.gesture.GestureRecognizer
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.model.TouchAction
import io.github.ptt98hd.totch.core.model.TouchEvent
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.effectiveAction
import io.github.ptt98hd.totch.core.settings.isGestureOn
import io.github.ptt98hd.totch.core.settings.toGestureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** User-visible feedback after a dispatch (FR-A9, FR-A10). */
interface EngineFeedback {
    /** A switched-on gesture was recognized and haptics are enabled, even one set to Nothing; called before dispatch (HF-4, HF-5). */
    fun onRecognized()
    fun onFailed(action: Action, reason: String)
}

/** What the notch overlay draws touch effects from (notch-overlay §4, EN-16…EN-18). */
interface EngineObserver {
    /** A single finger went down inside the region. */
    fun onPress(x: Float, y: Float, timeMs: Long)
    /** That finger went up or the touch was cancelled. */
    fun onRelease()
    /** A switched-on gesture was recognized, whatever its action and the haptics setting. */
    fun onGesture(g: Gesture)

    companion object {
        val None = object : EngineObserver {
            override fun onPress(x: Float, y: Float, timeMs: Long) = Unit
            override fun onRelease() = Unit
            override fun onGesture(g: Gesture) = Unit
        }
    }
}

/**
 * Touch source → [GestureRecognizer] → bindings → dispatcher (docs/specs/detection-modes.md §5).
 *
 * Thread-safe: [onTouch] may be called from binder or main threads; recognizer access is serialized.
 * At most one timer coroutine is pending at any time (NFR-3) and actions run one after another.
 *
 * Time base: [clock] must tick in the same domain as [TouchEvent.timeMs]. On Android both
 * `MotionEvent.eventTime` and `getevent -t` timestamps (getevent sets `EVIOCSCLOCKID` to
 * `CLOCK_MONOTONIC`) are `SystemClock.uptimeMillis()`. If a source ever reports another clock
 * (offset > [CLOCK_SKEW_TOLERANCE_MS]), event times are re-based onto [clock] by that offset.
 */
class GestureEngine(
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    density: Float,
    private val dispatch: suspend (Action) -> DispatchResult,
    private val feedback: EngineFeedback,
    @Volatile var observer: EngineObserver = EngineObserver.None,
) {
    private val lock = Any()

    /** Current touch region in screen pixels; `null` disables recognition. */
    @Volatile var region: Rect? = null

    /** Swipe-down gestures can run: the accessibility overlay blocks touches at the notch (FR-G14, EN-12). */
    @Volatile var canBlockTouches: Boolean = false
        set(value) = synchronized(lock) {
            field = value
            recognizer.config = settings.toGestureConfig(density, value)
            schedule()
        }

    @Volatile private var settings = Settings()
    private var density = density
    private val recognizer = GestureRecognizer(settings.toGestureConfig(density)) { region }
    private var timer: Job? = null
    private var clockOffsetMs = 0L
    private var pressed = false
    private val actions = Channel<Action>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (action in actions) {
                val r = dispatch(action)
                if (r is DispatchResult.Failed) feedback.onFailed(action, r.reason)
            }
        }
    }

    val config: GestureConfig get() = synchronized(lock) { recognizer.config }

    /** Number of pending timeout coroutines (0 or 1). */
    internal val pendingTimers: Int get() = synchronized(lock) { if (timer?.isActive == true) 1 else 0 }

    /** Applies new settings (and optionally a new display density) immediately (FR-S3). */
    fun updateSettings(newSettings: Settings, density: Float = this.density) = synchronized(lock) {
        settings = newSettings
        this.density = density
        recognizer.config = newSettings.toGestureConfig(density, canBlockTouches)
        schedule()
    }

    fun onTouch(event: TouchEvent) = synchronized(lock) {
        val skew = event.timeMs - clock()
        clockOffsetMs = if (abs(skew) > CLOCK_SKEW_TOLERANCE_MS) skew else 0
        observe(event)
        emit(recognizer.onTouch(event))
        schedule()
    }

    /** Drops any gesture in progress (e.g. when the touch source changes). */
    fun reset() = synchronized(lock) {
        recognizer.reset()
        if (pressed) observe(TouchEvent(TouchAction.CANCEL, 0f, 0f, now()))
        schedule()
    }

    private fun now() = clock() + clockOffsetMs

    /** EN-16, EN-17 */
    private fun observe(event: TouchEvent) {
        when (event.action) {
            TouchAction.DOWN -> if (event.pointerCount == 1 && region?.contains(event.x, event.y) == true) {
                pressed = true
                observer.onPress(event.x, event.y, event.timeMs)
            }
            TouchAction.UP, TouchAction.CANCEL -> if (pressed) {
                pressed = false
                observer.onRelease()
            }
            TouchAction.MOVE -> Unit
        }
    }

    private fun schedule() {
        timer?.cancel()
        val deadline = recognizer.nextDeadlineMs() ?: return run { timer = null }
        timer = scope.launch {
            delay((deadline - now()).coerceAtLeast(0))
            synchronized(lock) {
                emit(recognizer.onTick(now()))
                if (timer === coroutineContext[Job]) schedule()
            }
        }
    }

    private fun emit(gestures: List<Gesture>) {
        for (g in gestures) {
            if (!settings.isGestureOn(g, canBlockTouches)) continue
            observer.onGesture(g) // EN-18
            // EN-13: vibrate now; the action may be slow or still queued behind another one. EN-15: Nothing vibrates too.
            if (settings.haptics) feedback.onRecognized()
            val action = settings.effectiveAction(g, canBlockTouches)
            if (action != Action.None) actions.trySend(action)
        }
    }

    companion object {
        const val CLOCK_SKEW_TOLERANCE_MS = 10_000L
    }
}
