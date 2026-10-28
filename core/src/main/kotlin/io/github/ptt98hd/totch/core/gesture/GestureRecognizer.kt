package io.github.ptt98hd.totch.core.gesture

import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.model.TouchAction
import io.github.ptt98hd.totch.core.model.TouchEvent
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Deterministic gesture state machine (see docs/specs/gesture-recognition.md).
 * Not thread-safe: all calls must come from one thread/coroutine.
 */
class GestureRecognizer(config: GestureConfig, private val region: () -> Rect?) {
    /** Replaceable at runtime (FR-S3); timeouts are always computed from the current value. */
    var config: GestureConfig = config

    private sealed interface State {
        data object Idle : State
        data object Ignoring : State
        data class Pressed(val x: Float, val y: Float, val timeMs: Long, val isSecond: Boolean) : State
        /** Swipe candidate from ([x],[y]); the anchor is where the finger last came to rest (spec §4). */
        data class Moved(val x: Float, val y: Float, val anchorX: Float, val anchorY: Float, val anchorTimeMs: Long) : State
        /** LONG_PRESS or `SWIPE_*_HOLD` emitted; [nextRepeatMs] is set while repeating. */
        data class Held(val gesture: Gesture, val nextRepeatMs: Long?) : State
        data class WaitingSecondTap(val upTimeMs: Long) : State
    }

    private var state: State = State.Idle

    fun onTouch(event: TouchEvent): List<Gesture> {
        val out = mutableListOf<Gesture>()
        expire(event.timeMs, out)
        when (event.action) {
            TouchAction.DOWN -> onDown(event, out)
            TouchAction.MOVE -> onMove(event, out)
            TouchAction.UP -> onUp(event, out)
            TouchAction.CANCEL -> if (inStroke()) abort(out)
        }
        return out
    }

    fun onTick(nowMs: Long): List<Gesture> = mutableListOf<Gesture>().also { expire(nowMs, it) }

    fun nextDeadlineMs(): Long? = when (val s = state) {
        is State.Pressed -> s.timeMs + config.longPressMs
        is State.WaitingSecondTap -> s.upTimeMs + config.doubleTapMs
        is State.Moved -> if (holdCandidate(s) != null) s.anchorTimeMs + config.longPressMs else null
        is State.Held -> s.nextRepeatMs
        else -> null
    }

    fun reset() {
        state = State.Idle
    }

    private fun expire(nowMs: Long, out: MutableList<Gesture>) {
        val deadline = nextDeadlineMs() ?: return
        if (nowMs < deadline) return
        when (val s = state) {
            is State.Pressed -> {
                if (s.isSecond) out += Gesture.TAP
                hold(Gesture.LONG_PRESS, nowMs, out)
            }
            is State.WaitingSecondTap -> {
                out += Gesture.TAP
                state = State.Idle
            }
            is State.Moved -> hold(holdCandidate(s) ?: return, nowMs, out)
            is State.Held -> {
                out += s.gesture
                state = s.copy(nextRepeatMs = nowMs + config.repeatIntervalMs) // one repeat per tick, no burst
            }
            else -> Unit
        }
    }

    private fun onDown(e: TouchEvent, out: MutableList<Gesture>) {
        if (inStroke()) abort(out) // GR-26: missing UP, treat old stroke as cancelled
        val startable = e.pointerCount == 1 && region()?.contains(e.x, e.y) == true
        val waiting = state is State.WaitingSecondTap
        state = when {
            startable -> State.Pressed(e.x, e.y, e.timeMs, isSecond = waiting)
            else -> {
                if (waiting) out += Gesture.TAP // GR-17
                State.Ignoring
            }
        }
    }

    private fun onMove(e: TouchEvent, out: MutableList<Gesture>) {
        val s = state
        if (e.pointerCount > 1 && s !is State.Ignoring && inStroke()) {
            abort(out)
            state = State.Ignoring
            return
        }
        if (s is State.Pressed && hypot(e.x - s.x, e.y - s.y) > config.touchSlopPx) {
            if (s.isSecond) out += Gesture.TAP // GR-15
            state = State.Moved(s.x, s.y, e.x, e.y, e.timeMs)
        } else if (s is State.Moved && hypot(e.x - s.anchorX, e.y - s.anchorY) > config.touchSlopPx) {
            state = s.copy(anchorX = e.x, anchorY = e.y, anchorTimeMs = e.timeMs)
        }
    }

    private fun onUp(e: TouchEvent, out: MutableList<Gesture>) {
        when (val s = state) {
            is State.Pressed -> {
                state = State.Idle
                when {
                    hypot(e.x - s.x, e.y - s.y) > config.touchSlopPx -> {
                        if (s.isSecond) out += Gesture.TAP
                        swipe(e.x - s.x, e.y - s.y)?.let { out += it }
                    }
                    s.isSecond -> out += Gesture.DOUBLE_TAP
                    config.doubleTapEnabled -> state = State.WaitingSecondTap(e.timeMs)
                    else -> out += Gesture.TAP
                }
            }
            is State.Moved -> {
                state = State.Idle
                swipe(e.x - s.x, e.y - s.y)?.let { out += it }
            }
            is State.Held, State.Ignoring -> state = State.Idle
            State.Idle, is State.WaitingSecondTap -> Unit // GR-25
        }
    }

    private fun swipe(dx: Float, dy: Float): Gesture? {
        val min = config.swipeMinDistancePx
        return if (abs(dy) > abs(dx)) {
            Gesture.SWIPE_DOWN.takeIf { dy >= min } // swipe up unsupported (GR-12)
        } else when {
            abs(dx) < min -> null
            dx < 0 -> Gesture.SWIPE_LEFT
            else -> Gesture.SWIPE_RIGHT
        }
    }

    private fun hold(gesture: Gesture, nowMs: Long, out: MutableList<Gesture>) {
        out += gesture
        val next = if (gesture in config.repeating) nowMs + config.repeatIntervalMs else null
        state = State.Held(gesture, next)
    }

    /** Hold gesture for a finger resting at the anchor, if that direction is bound (spec §4). */
    private fun holdCandidate(s: State.Moved): Gesture? {
        val held = when (swipe(s.anchorX - s.x, s.anchorY - s.y)) {
            Gesture.SWIPE_DOWN -> Gesture.SWIPE_DOWN_HOLD
            Gesture.SWIPE_LEFT -> Gesture.SWIPE_LEFT_HOLD
            Gesture.SWIPE_RIGHT -> Gesture.SWIPE_RIGHT_HOLD
            else -> null
        }
        return held?.takeIf { it in config.holdSwipes }
    }

    private fun inStroke() = when (state) {
        State.Idle, is State.WaitingSecondTap -> false
        else -> true
    }

    /** Ends the current stroke silently, except that a completed first tap is never lost. */
    private fun abort(out: MutableList<Gesture>) {
        if ((state as? State.Pressed)?.isSecond == true) out += Gesture.TAP
        state = State.Idle
    }
}
