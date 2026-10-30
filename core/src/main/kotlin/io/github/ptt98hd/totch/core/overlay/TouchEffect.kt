package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.Settings

/** Feedback drawn when a gesture is recognized at the notch (notch-overlay §4). */
sealed interface TouchEffect {
    data object Pulse : TouchEffect
    data object DoublePulse : TouchEffect
    data object Glow : TouchEffect
    /** Light running in the swipe direction; ([dx], [dy]) is a unit vector, y down. */
    data class Streak(val dx: Int, val dy: Int) : TouchEffect
}

/** TE-1 */
fun touchEffectFor(g: Gesture): TouchEffect = when (g) {
    Gesture.TAP -> TouchEffect.Pulse
    Gesture.DOUBLE_TAP -> TouchEffect.DoublePulse
    Gesture.LONG_PRESS -> TouchEffect.Glow
    Gesture.SWIPE_DOWN, Gesture.SWIPE_DOWN_HOLD -> TouchEffect.Streak(0, 1)
    Gesture.SWIPE_LEFT, Gesture.SWIPE_LEFT_HOLD -> TouchEffect.Streak(-1, 0)
    Gesture.SWIPE_RIGHT, Gesture.SWIPE_RIGHT_HOLD -> TouchEffect.Streak(1, 0)
}

/** TE-2: how far a press is toward a long press, 0–1. */
fun pressProgress(downMs: Long, nowMs: Long, longPressMs: Long): Float =
    if (longPressMs <= 0) 1f else ((nowMs - downMs).toFloat() / longPressMs).coerceIn(0f, 1f)

/** OV-1…OV-5: whether the drawing overlay is shown at all. */
fun overlayVisible(settings: Settings, accessibility: Boolean, rotation: Int, statusBarVisible: Boolean): Boolean =
    settings.enabled && accessibility && rotation == 0 && statusBarVisible &&
        (settings.notificationIndicator || settings.island || settings.touchEffects)
