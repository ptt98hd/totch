package io.github.ptt98hd.totch.core.gesture

import io.github.ptt98hd.totch.core.model.Gesture

data class GestureConfig(
    val doubleTapMs: Long = 300,
    val longPressMs: Long = 500,
    val touchSlopPx: Float = 24f,
    val swipeMinDistancePx: Float = 60f,
    /** `false` => TAP is emitted immediately on UP (FR-G5). */
    val doubleTapEnabled: Boolean = true,
    /** `SWIPE_*_HOLD` gestures recognized when the finger pauses after a swipe. */
    val holdSwipes: Set<Gesture> = emptySet(),
    /** Hold gestures re-emitted every [repeatIntervalMs] until release. */
    val repeating: Set<Gesture> = emptySet(),
    val repeatIntervalMs: Long = 300,
)
