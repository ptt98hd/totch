package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.model.Rect
import kotlin.math.roundToInt

data class IslandMetrics(val screenWidthPx: Int, val density: Float) {
    fun dp(value: Int): Int = (value * density).roundToInt()
}

enum class IslandControl { PREVIOUS, PLAY_PAUSE, NEXT }

sealed interface IslandHit {
    data object Outside : IslandHit
    data object Body : IslandHit
    data class Control(val control: IslandControl) : IslandHit
}

/**
 * Island geometry in screen pixels around the physical [notch] (no padding), shared by drawing and hit testing
 * (IG-4, IG-5, IG-10…IG-15). Every shape is built from [rest]: the notch in a black rim.
 */
object IslandLayout {
    const val RIM_DP = 4
    const val MIN_HEIGHT_DP = 28

    /** IG-10 (FR-O21): the notch with a black rim, at least [MIN_HEIGHT_DP] tall and as wide as tall, same center. */
    fun rest(notch: Rect, m: IslandMetrics): Rect {
        val h = maxOf(notch.height + 2 * m.dp(RIM_DP), m.dp(MIN_HEIGHT_DP))
        val w = maxOf(notch.width + 2 * m.dp(RIM_DP), h)
        val left = notch.left - (w - notch.width) / 2
        val top = notch.top - (h - notch.height) / 2
        return Rect(maxOf(left, 0), maxOf(top, 0), minOf(left + w, m.screenWidthPx), top + h)
    }

    fun pill(content: IslandContent, notch: Rect, m: IslandMetrics): Rect {
        val rest = rest(notch, m)
        return when {
            content == IslandContent.Hidden -> rest // IG-11
            content is IslandContent.Media && content.expanded -> // IG-13
                Rect(m.dp(12), rest.top, m.screenWidthPx - m.dp(12), rest.bottom + m.dp(136))
            else -> stretched(rest, m, rest.height + m.dp(if (content is IslandContent.Sound) 40 else 16)) // IG-12
        }
    }

    /** IG-14: the wing of the touch-feedback result, as of a compact island. */
    fun feedbackWing(notch: Rect, m: IslandMetrics): Int = rest(notch, m).height + m.dp(16)

    /** TF-7: [shape] grown by [stretchPx] on each side, kept off the screen edges. */
    fun stretched(shape: Rect, m: IslandMetrics, stretchPx: Int): Rect {
        if (stretchPx <= 0) return shape
        return Rect(
            maxOf(shape.left - stretchPx, m.dp(8)),
            shape.top,
            minOf(shape.right + stretchPx, m.screenWidthPx - m.dp(8)),
            shape.bottom,
        )
    }

    /** IG-4: the media buttons along the bottom of an expanded [pill]. */
    fun controls(pill: Rect, m: IslandMetrics): Map<IslandControl, Rect> {
        val size = m.dp(48)
        val bottom = pill.bottom - m.dp(12)
        val cx = (pill.left + pill.right) / 2
        fun at(center: Int) = Rect(center - size / 2, bottom - size, center + size / 2, bottom)
        return mapOf(
            IslandControl.PREVIOUS to at(cx - m.dp(72)),
            IslandControl.PLAY_PAUSE to at(cx),
            IslandControl.NEXT to at(cx + m.dp(72)),
        )
    }

    /** IG-5 */
    fun hit(content: IslandContent, notch: Rect, m: IslandMetrics, x: Float, y: Float): IslandHit {
        if (content == IslandContent.Hidden) return IslandHit.Outside
        val pill = pill(content, notch, m)
        if (content is IslandContent.Media && content.expanded) {
            controls(pill, m).entries.firstOrNull { it.value.contains(x, y) }?.let { return IslandHit.Control(it.key) }
        }
        return if (pill.contains(x, y)) IslandHit.Body else IslandHit.Outside
    }
}
