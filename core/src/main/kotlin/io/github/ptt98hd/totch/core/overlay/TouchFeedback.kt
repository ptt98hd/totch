package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.Settings
import kotlin.math.hypot

/** What the island shows for a touch at the notch (notch-overlay §4); coordinates are natural (ROTATION_0). */
sealed interface TouchFeedback {
    data object Idle : TouchFeedback
    /** A finger is down at ([x], [y]), having gone down at ([downX], [downY]): ring lit, the pill stretches. */
    data class Pressing(val downX: Float, val downY: Float, val downMs: Long, val x: Float, val y: Float) : TouchFeedback
    /** Released; a gesture may still come (a tap waits for a second one): ring lit around the notch. */
    data object Waiting : TouchFeedback
    /** [gesture] ran [action] (`Action.None` = Nothing): gesture icon on the left, action icon on the right. */
    data class Result(val gesture: Gesture, val action: Action) : TouchFeedback
}

/** TF-1…TF-5. Pure: time comes in as parameters and [nextDeadlineMs] tells when [onTick] is due. Not thread-safe. */
class TouchFeedbackController {
    var state: TouchFeedback = TouchFeedback.Idle
        private set
    private var until: Long? = null

    fun onPress(x: Float, y: Float, nowMs: Long) {
        set(TouchFeedback.Pressing(x, y, nowMs, x, y), null)
    }

    fun onMove(x: Float, y: Float) {
        (state as? TouchFeedback.Pressing)?.let { state = it.copy(x = x, y = y) }
    }

    fun onRelease(nowMs: Long, waitMs: Long) {
        if (state is TouchFeedback.Pressing) set(TouchFeedback.Waiting, nowMs + waitMs)
    }

    fun onGesture(g: Gesture, action: Action, nowMs: Long) {
        set(TouchFeedback.Result(g, action), nowMs + RESULT_MS)
    }

    fun onTick(nowMs: Long) {
        until?.let { if (nowMs >= it) set(TouchFeedback.Idle, null) }
    }

    fun nextDeadlineMs(): Long? = until

    private fun set(next: TouchFeedback, deadline: Long?) {
        state = next
        until = deadline
    }

    companion object {
        const val RESULT_MS = 1_200L
        /** Added to the double-tap window while waiting for a tap after release. */
        const val WAIT_MARGIN_MS = 100L
    }
}

/** TF-6: how far the pill stretches on each side while pressed — with the finger, or with time toward a long press. */
fun pressStretch(p: TouchFeedback.Pressing, nowMs: Long, longPressMs: Long, maxPx: Float): Float {
    val drag = hypot(p.x - p.downX, p.y - p.downY)
    val hold = if (longPressMs <= 0) {
        1f
    } else {
        val grace = longPressMs / 3f
        ((nowMs - p.downMs - grace) / (longPressMs - grace)).coerceIn(0f, 1f)
    }
    return maxOf(drag, hold * maxPx).coerceIn(0f, maxPx)
}

/** TE-2: how far a press is toward a long press, 0–1. */
fun pressProgress(downMs: Long, nowMs: Long, longPressMs: Long): Float =
    if (longPressMs <= 0) 1f else ((nowMs - downMs).toFloat() / longPressMs).coerceIn(0f, 1f)

/** OV-1, OV-2, OV-5: whether the drawing overlay is shown at all; in every rotation (FR-O15). */
fun overlayVisible(settings: Settings, accessibility: Boolean): Boolean =
    settings.enabled && accessibility && (settings.island || settings.touchEffects)

/** OV-4: island activities stay off full-screen apps; touch feedback does not go through here. */
fun islandShown(content: IslandContent, statusBarVisible: Boolean): IslandContent =
    if (statusBarVisible) content else IslandContent.Hidden

/** VR-1: degrees of the volume ring for a volume [fraction] (FR-O18). */
fun volumeSweep(fraction: Float): Float = 360f * fraction.coerceIn(0f, 1f)
