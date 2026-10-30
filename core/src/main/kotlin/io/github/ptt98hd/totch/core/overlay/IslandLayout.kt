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

/** Island geometry in screen pixels around the [notch] (no padding), shared by drawing and hit testing (IG-1…IG-6). */
object IslandLayout {
    fun pill(content: IslandContent, notch: Rect, m: IslandMetrics): Rect = when {
        content == IslandContent.Hidden -> notch // IG-1
        content is IslandContent.Media && content.expanded -> // IG-3
            Rect(m.dp(12), notch.top, m.screenWidthPx - m.dp(12), notch.bottom + m.dp(136))
        else -> { // IG-2
            val wing = notch.height + m.dp(if (content is IslandContent.Sound) 40 else 16)
            Rect(
                maxOf(notch.left - wing, m.dp(8)),
                notch.top,
                minOf(notch.right + wing, m.screenWidthPx - m.dp(8)),
                notch.bottom,
            )
        }
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
