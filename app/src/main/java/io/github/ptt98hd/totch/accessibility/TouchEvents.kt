package io.github.ptt98hd.totch.accessibility

import android.view.MotionEvent
import io.github.ptt98hd.totch.core.model.TouchAction
import io.github.ptt98hd.totch.core.model.TouchEvent

/**
 * DM-44: a touch on the accessibility overlay as the engine's [TouchEvent], in screen pixels ([x], [y] = raw
 * coordinates) and `uptimeMillis` ([timeMs] = `eventTime`). A second finger is reported as a move with its pointer
 * count, which cancels the gesture (FR-G7); other actions are dropped.
 */
fun touchEventOf(actionMasked: Int, x: Float, y: Float, timeMs: Long, pointerCount: Int): TouchEvent? {
    val action = when (actionMasked) {
        MotionEvent.ACTION_DOWN -> TouchAction.DOWN
        MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> TouchAction.MOVE
        MotionEvent.ACTION_UP -> TouchAction.UP
        MotionEvent.ACTION_CANCEL -> TouchAction.CANCEL
        else -> return null
    }
    return TouchEvent(action, x, y, timeMs, pointerCount)
}
