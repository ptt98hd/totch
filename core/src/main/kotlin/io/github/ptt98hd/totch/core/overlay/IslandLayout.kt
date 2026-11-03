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
 * (IG-4, IG-5, IG-16…IG-20). Every shape is built from [rest]: the notch itself.
 */
object IslandLayout {
    /** IG-16 (FR-O22): the notch as set, as wide as tall at least, same center. */
    fun rest(notch: Rect, m: IslandMetrics): Rect {
        val w = maxOf(notch.width, notch.height)
        val left = notch.left - (w - notch.width) / 2
        return Rect(maxOf(left, 0), notch.top, minOf(left + w, m.screenWidthPx), notch.bottom)
    }

    /** IG-17 (FR-O23): the icon size of a compact island. */
    fun iconPx(notch: Rect, m: IslandMetrics): Int {
        val h = rest(notch, m).height
        return minOf((h * 0.64f).roundToInt().coerceIn(m.dp(10), m.dp(22)), h)
    }

    /** IG-17: from an icon to the pill's end, as much as above and below it. */
    fun padPx(notch: Rect, m: IslandMetrics): Int = maxOf((rest(notch, m).height - iconPx(notch, m)) / 2, m.dp(4))

    /** IG-18: how far [content] stretches the rest shape on each side. */
    fun wing(content: IslandContent, notch: Rect, m: IslandMetrics): Int {
        val pad = padPx(notch, m)
        val iconWing = iconPx(notch, m) + 2 * pad
        return when (content) {
            IslandContent.Hidden -> 0
            is IslandContent.Charging -> maxOf(iconWing, 2 * pad + m.dp(34))
            is IslandContent.Sound -> maxOf(iconWing, 2 * pad + m.dp(64))
            else -> iconWing
        }
    }

    fun pill(content: IslandContent, notch: Rect, m: IslandMetrics): Rect {
        val rest = rest(notch, m)
        return when {
            content == IslandContent.Hidden -> rest // IG-11
            content is IslandContent.Media && content.expanded -> // IG-19
                Rect(m.dp(12), rest.top, m.screenWidthPx - m.dp(12), rest.bottom + m.dp(136))
            else -> stretched(rest, m, wing(content, notch, m)) // IG-18
        }
    }

    /** IG-20: the wing of the touch-feedback result, an icon on each side. */
    fun feedbackWing(notch: Rect, m: IslandMetrics): Int = iconPx(notch, m) + 2 * padPx(notch, m)

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
