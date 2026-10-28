package io.github.ptt98hd.totch.core.settings

import io.github.ptt98hd.totch.core.gesture.GestureConfig
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import kotlinx.serialization.Serializable

enum class DetectionMode { SHIZUKU, ACCESSIBILITY }

/** Manual notch region override in dp (converted to px by the app). */
@Serializable
data class ManualRegionDp(val centerXFraction: Float = 0.5f, val widthDp: Int = 80, val heightDp: Int = 32)

/** Default bindings per FR-S2. */
val DefaultBindings: Map<Gesture, Action> = mapOf(
    Gesture.TAP to Action.None,
    Gesture.DOUBLE_TAP to Action.LockScreen,
    Gesture.LONG_PRESS to Action.ToggleTorch,
    Gesture.SWIPE_DOWN to Action.Notifications,
    Gesture.SWIPE_LEFT to Action.MediaPrevious,
    Gesture.SWIPE_RIGHT to Action.MediaNext,
)

/** Touch slop used by [toGestureConfig], in dp. */
const val TOUCH_SLOP_DP: Int = 12

@Serializable
data class Settings(
    val enabled: Boolean = false,
    val mode: DetectionMode = DetectionMode.ACCESSIBILITY,
    val bindings: Map<Gesture, Action> = DefaultBindings,
    val doubleTapMs: Long = 300,
    val longPressMs: Long = 500,
    val swipeMinDistanceDp: Int = 40,
    val paddingDp: Int = 8,
    val manualRegion: ManualRegionDp? = null,
    val haptics: Boolean = true,
) {
    /** Clamps values to FR-S4 limits and ensures every [Gesture] has a binding. */
    fun sanitized(): Settings = copy(
        bindings = Gesture.entries.associateWith { bindings[it] ?: Action.None },
        doubleTapMs = doubleTapMs.coerceIn(150, 600),
        longPressMs = longPressMs.coerceIn(300, 1500),
        swipeMinDistanceDp = swipeMinDistanceDp.coerceIn(16, 200),
        paddingDp = paddingDp.coerceIn(0, 48),
        manualRegion = manualRegion?.run {
            ManualRegionDp(
                centerXFraction = if (centerXFraction.isNaN()) 0.5f else centerXFraction.coerceIn(0f, 1f),
                widthDp = widthDp.coerceIn(8, 400),
                heightDp = heightDp.coerceIn(8, 100),
            )
        },
    )
}

fun Settings.actionFor(g: Gesture): Action = bindings[g] ?: Action.None

fun Settings.toGestureConfig(density: Float): GestureConfig = GestureConfig(
    doubleTapMs = doubleTapMs,
    longPressMs = longPressMs,
    touchSlopPx = TOUCH_SLOP_DP * density,
    swipeMinDistancePx = swipeMinDistanceDp * density,
    doubleTapEnabled = actionFor(Gesture.DOUBLE_TAP) != Action.None,
)
