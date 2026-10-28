package io.github.ow3n.totch.core.model

enum class TouchAction { DOWN, MOVE, UP, CANCEL }

/** A single-pointer view of a touch stream in screen pixels; [pointerCount] > 1 means multi-touch. */
data class TouchEvent(
    val action: TouchAction,
    val x: Float,
    val y: Float,
    val timeMs: Long,
    val pointerCount: Int = 1,
)

enum class Gesture { TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT }
