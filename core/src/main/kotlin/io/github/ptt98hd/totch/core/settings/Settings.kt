package io.github.ptt98hd.totch.core.settings

import io.github.ptt98hd.totch.core.gesture.GestureConfig
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class DetectionMode { SHIZUKU, ACCESSIBILITY }

@Serializable
enum class ThemeMode {
    @SerialName("system") SYSTEM,
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK,
}

/** Display language; [tag] is the BCP 47 tag, `null` = follow the system (FR-S7). */
@Serializable
enum class AppLanguage(val tag: String?) {
    @SerialName("system") SYSTEM(null),
    @SerialName("en") ENGLISH("en"),
    @SerialName("vi") VIETNAMESE("vi"),
}

/** Manual notch region override in dp (converted to px by the app); [topDp] is the vertical position (FR-R6). */
@Serializable
data class ManualRegionDp(
    val centerXFraction: Float = 0.5f,
    val widthDp: Int = 80,
    val heightDp: Int = 32,
    val topDp: Int = 0,
)

/** Default bindings per FR-S2; gestures added in v1.1 default to None. */
val DefaultBindings: Map<Gesture, Action> = mapOf(
    Gesture.TAP to Action.None,
    Gesture.DOUBLE_TAP to Action.LockScreen,
    Gesture.LONG_PRESS to Action.ToggleTorch,
    Gesture.SWIPE_DOWN to Action.Notifications,
    Gesture.SWIPE_LEFT to Action.MediaPrevious,
    Gesture.SWIPE_RIGHT to Action.MediaNext,
    Gesture.SWIPE_DOWN_HOLD to Action.None,
    Gesture.SWIPE_LEFT_HOLD to Action.None,
    Gesture.SWIPE_RIGHT_HOLD to Action.None,
)

/** Gestures that keep the finger down after recognition and may repeat (FR-G9). */
val HoldGestures: Set<Gesture> =
    setOf(Gesture.LONG_PRESS, Gesture.SWIPE_DOWN_HOLD, Gesture.SWIPE_LEFT_HOLD, Gesture.SWIPE_RIGHT_HOLD)

private val SwipeHoldGestures = HoldGestures - Gesture.LONG_PRESS

/** Shizuku mode cannot block touches, so swiping down always pulls the shade (FR-G10, R2). */
fun Gesture.isAvailableIn(mode: DetectionMode): Boolean =
    !(mode == DetectionMode.SHIZUKU && (this == Gesture.SWIPE_DOWN || this == Gesture.SWIPE_DOWN_HOLD))

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
    val repeatOnHold: Set<Gesture> = emptySet(),
    val repeatIntervalMs: Long = 300,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val startOnBoot: Boolean = true,
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
                topDp = topDp.coerceIn(0, 100),
            )
        },
        repeatOnHold = repeatOnHold intersect HoldGestures,
        repeatIntervalMs = repeatIntervalMs.coerceIn(100, 1000),
    )
}

fun Settings.actionFor(g: Gesture): Action = bindings[g] ?: Action.None

/** The action that actually runs for [g] in the current mode (ST-12). */
fun Settings.effectiveAction(g: Gesture): Action = if (g.isAvailableIn(mode)) actionFor(g) else Action.None

fun Settings.toGestureConfig(density: Float): GestureConfig {
    val bound = Gesture.entries.filterTo(mutableSetOf()) { effectiveAction(it) != Action.None }
    return GestureConfig(
        doubleTapMs = doubleTapMs,
        longPressMs = longPressMs,
        touchSlopPx = TOUCH_SLOP_DP * density,
        swipeMinDistancePx = swipeMinDistanceDp * density,
        doubleTapEnabled = Gesture.DOUBLE_TAP in bound,
        holdSwipes = SwipeHoldGestures intersect bound,
        repeating = repeatOnHold intersect bound,
        repeatIntervalMs = repeatIntervalMs,
    )
}

