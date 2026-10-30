package io.github.ptt98hd.totch.core.settings

import io.github.ptt98hd.totch.core.action.OneHandedMethod
import io.github.ptt98hd.totch.core.gesture.GestureConfig
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.overlay.IslandActivity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

/** How often the background job looks for a new release (update spec §6, UP-10). */
@Serializable
enum class UpdateInterval(val days: Int) {
    @SerialName("never") NEVER(0),
    @SerialName("daily") DAILY(1),
    @SerialName("weekly") WEEKLY(7),
    @SerialName("monthly") MONTHLY(30),
}

/** Manual notch region override in dp (converted to px by the app); [topDp] is the vertical position (FR-R6). */
@Serializable
data class ManualRegionDp(
    val centerXFraction: Float = 0.5f,
    val widthDp: Int = 80,
    val heightDp: Int = 32,
    val topDp: Int = 0,
)

/** Default bindings per FR-S2 (v1.8): every gesture starts not set, so all are off on first launch. */
val DefaultBindings: Map<Gesture, Action> = Gesture.entries.associateWith { Action.None }

/** Gestures that keep the finger down after recognition; they fire once (FR-G11). */
val HoldGestures: Set<Gesture> =
    setOf(Gesture.LONG_PRESS, Gesture.SWIPE_DOWN_HOLD, Gesture.SWIPE_LEFT_HOLD, Gesture.SWIPE_RIGHT_HOLD)

private val SwipeHoldGestures = HoldGestures - Gesture.LONG_PRESS

val SwipeDownGestures: Set<Gesture> = setOf(Gesture.SWIPE_DOWN, Gesture.SWIPE_DOWN_HOLD)

/**
 * Shizuku only reads touches, so swiping down at the notch also pulls the shade (FR-G10, R2) unless the accessibility
 * overlay blocks them there ([canBlockTouches], FR-G14).
 */
fun Gesture.isAvailable(canBlockTouches: Boolean): Boolean = canBlockTouches || this !in SwipeDownGestures

/** Touch slop used by [toGestureConfig], in dp. */
const val TOUCH_SLOP_DP: Int = 12

@Serializable
data class Settings(
    val enabled: Boolean = false,
    val bindings: Map<Gesture, Action> = DefaultBindings,
    val doubleTapMs: Long = 300,
    val longPressMs: Long = 300,
    val swipeMinDistanceDp: Int = 40,
    val paddingDp: Int = 8,
    val manualRegion: ManualRegionDp? = null,
    val haptics: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val startOnBoot: Boolean = true,
    /** Keep detecting gestures at the physical notch while rotated (FR-R8). */
    val landscapeGestures: Boolean = false,
    /** Look for a new release when the app opens (FR-U29). */
    val updateOnLaunch: Boolean = false,
    val updateInterval: UpdateInterval = UpdateInterval.NEVER,
    /** The one-handed method that last worked; written by the Shizuku executor (OH-5). */
    val oneHandedMethod: OneHandedMethod = OneHandedMethod.SYSTEM,
    /**
     * Gestures switched on: they are recognized even when not set, which then does nothing (FR-G15). Switched-off
     * gestures keep their binding for when they are switched on again (FR-G12).
     */
    val enabledGestures: Set<Gesture> = emptySet(),
    /** Labels under the bottom bar icons (FR-U49). */
    val navLabels: Boolean = true,
    /**
     * The user wants the accessibility service on: written by the service itself, true once connected, false when it is
     * unbound while Totch runs (switched off). Lets Totch turn it back on when Android drops it (FR-D15, DM-53).
     */
    val accessibilityWanted: Boolean = false,
    /**
     * Dynamic Island around the notch (FR-O2…FR-O5), showing [islandActivities]; its notifications activity also lights
     * the indicator ring while notifications are unseen (FR-O1, FR-O12).
     */
    val island: Boolean = false,
    val islandActivities: Set<IslandActivity> = IslandActivity.entries.toSet(),
    /** Ripple, glow and squish when the notch is touched (FR-O6). */
    val touchEffects: Boolean = false,
) {
    /** Clamps values to FR-S4 limits and ensures every [Gesture] has a binding. */
    fun sanitized(): Settings = copy(
        bindings = Gesture.entries.associateWith { bindings[it] ?: Action.None },
        doubleTapMs = doubleTapMs.coerceIn(100, 1000),
        longPressMs = longPressMs.coerceIn(100, 1000),
        swipeMinDistanceDp = swipeMinDistanceDp.coerceIn(16, 200),
        paddingDp = paddingDp.coerceIn(1, 10),
        manualRegion = manualRegion?.run {
            ManualRegionDp(
                centerXFraction = if (centerXFraction.isNaN()) 0.5f else centerXFraction.coerceIn(0f, 1f),
                widthDp = widthDp.coerceIn(8, 400),
                heightDp = heightDp.coerceIn(8, 100),
                topDp = topDp.coerceIn(0, 100),
            )
        },
    )
}

/** ST-38: what the island may show; nothing while it is switched off. */
fun Settings.activeIslandActivities(): Set<IslandActivity> = if (island) islandActivities else emptySet()

fun Settings.actionFor(g: Gesture): Action = bindings[g] ?: Action.None

/** State of the gesture's switch: available and switched on, whatever its action (ST-22, ST-31). */
fun Settings.isGestureOn(g: Gesture, canBlockTouches: Boolean = false): Boolean =
    g.isAvailable(canBlockTouches) && g in enabledGestures

/** The action that actually runs for [g] (ST-12, ST-19, ST-24). */
fun Settings.effectiveAction(g: Gesture, canBlockTouches: Boolean = false): Action =
    if (isGestureOn(g, canBlockTouches)) actionFor(g) else Action.None

/** The accessibility overlay should block touches at the notch (DM-15, ST-26). */
fun Settings.needsTouchBlocking(): Boolean = enabled && SwipeDownGestures.any { isGestureOn(it, canBlockTouches = true) }

fun Settings.withGestureOn(g: Gesture, on: Boolean): Settings =
    copy(enabledGestures = if (on) enabledGestures + g else enabledGestures - g)

/** Binds [action] to [g] and switches it on; [Action.None] (Nothing) leaves it listening but doing nothing (FR-G15). */
fun Settings.withBinding(g: Gesture, action: Action): Settings =
    copy(bindings = bindings + (g to action), enabledGestures = enabledGestures + g)

fun Settings.toGestureConfig(density: Float, canBlockTouches: Boolean = false): GestureConfig {
    val bound = Gesture.entries.filterTo(mutableSetOf()) { isGestureOn(it, canBlockTouches) }
    return GestureConfig(
        doubleTapMs = doubleTapMs,
        longPressMs = longPressMs,
        touchSlopPx = TOUCH_SLOP_DP * density,
        swipeMinDistancePx = swipeMinDistanceDp * density,
        doubleTapEnabled = Gesture.DOUBLE_TAP in bound,
        holdSwipes = SwipeHoldGestures intersect bound,
    )
}

