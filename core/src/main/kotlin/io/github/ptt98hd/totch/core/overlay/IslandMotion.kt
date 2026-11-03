package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.model.Rect

/** One stage of the island's motion (notch-overlay §3.5). */
enum class IslandStep { APPEAR, MORPH, DISAPPEAR }

/** One beat each way: grow while fading in, shrink while fading out (IM-4, IM-5, IM-8, IM-9; FR-O25). */
object IslandMotion {
    const val APPEAR_FADE_MS = 160
    const val DISAPPEAR_MS = 240

    /** IM-8, IM-9: [visible] = the island or its ring can be seen; [shown] = there is something to show. */
    fun steps(visible: Boolean, shown: Boolean): List<IslandStep> = when {
        !shown -> listOf(IslandStep.DISAPPEAR)
        visible -> listOf(IslandStep.MORPH)
        else -> listOf(IslandStep.APPEAR)
    }

    /** IM-4, IM-5: the shape to grow to, or null when there is nothing to show (the island shrinks to the notch). */
    fun target(content: IslandContent, notch: Rect, m: IslandMetrics): Rect? =
        if (content == IslandContent.Hidden) null else IslandLayout.pill(content, notch, m)

}
