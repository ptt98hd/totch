package io.github.ow3n.totch.engine

import io.github.ow3n.totch.core.action.DispatchResult
import io.github.ow3n.totch.core.gesture.GestureConfig
import io.github.ow3n.totch.core.gesture.GestureRecognizer
import io.github.ow3n.totch.core.model.Action
import io.github.ow3n.totch.core.model.Gesture
import io.github.ow3n.totch.core.model.Rect
import io.github.ow3n.totch.core.model.TouchEvent
import io.github.ow3n.totch.core.settings.Settings
import io.github.ow3n.totch.core.settings.actionFor
import io.github.ow3n.totch.core.settings.toGestureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** User-visible feedback after a dispatch (FR-A9, FR-A10). */
interface EngineFeedback {
    /** Action performed and haptics are enabled. */
    fun onPerformed()
    fun onFailed(action: Action, reason: String)
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
) {
    private val lock = Any()

    /** Current touch region in screen pixels; `null` disables recognition. */
    @Volatile var region: Rect? = null

    @Volatile private var settings = Settings()
    private var density = density
    private val recognizer = GestureRecognizer(settings.toGestureConfig(density)) { region }
    private var timer: Job? = null
    private var clockOffsetMs = 0L
    private val actions = Channel<Action>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (action in actions) {
                when (val r = dispatch(action)) {
                    DispatchResult.Ok -> if (settings.haptics) feedback.onPerformed()
                    is DispatchResult.Failed -> feedback.onFailed(action, r.reason)
                    DispatchResult.Skipped -> Unit
                }
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
        recognizer.config = newSettings.toGestureConfig(density)
        schedule()
    }

    fun onTouch(event: TouchEvent) = synchronized(lock) {
        val skew = event.timeMs - clock()
        clockOffsetMs = if (abs(skew) > CLOCK_SKEW_TOLERANCE_MS) skew else 0
        emit(recognizer.onTouch(event))
        schedule()
    }

    /** Drops any gesture in progress (e.g. when the touch source changes). */
    fun reset() = synchronized(lock) {
        recognizer.reset()
        schedule()
    }

    private fun now() = clock() + clockOffsetMs

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
            val action = settings.actionFor(g)
            if (action != Action.None) actions.trySend(action)
        }
    }

    companion object {
        const val CLOCK_SKEW_TOLERANCE_MS = 10_000L
    }
}
