package io.github.ow3n.totch.core.gesture

data class GestureConfig(
    val doubleTapMs: Long = 300,
    val longPressMs: Long = 500,
    val touchSlopPx: Float = 24f,
    val swipeMinDistancePx: Float = 60f,
    /** `false` => TAP is emitted immediately on UP (FR-G5). */
    val doubleTapEnabled: Boolean = true,
)
