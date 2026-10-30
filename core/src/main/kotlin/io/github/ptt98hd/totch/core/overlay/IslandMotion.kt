package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.model.Rect

/** One stage of the island's motion (notch-overlay §3.5). */
enum class IslandStep { RING_IN, MORPH, RING_OUT }

/** Ring → grow → shrink: how the island and its ring move between states (IM-1…IM-5). */
object IslandMotion {
    const val RING_IN_MS = 220
    const val RING_OUT_MS = 280

    /** IM-1…IM-3: [ringLit] = the ring is fully lit; [shown] = there is something to show. */
    fun steps(ringLit: Boolean, shown: Boolean): List<IslandStep> = when {
        !shown -> listOf(IslandStep.MORPH, IslandStep.RING_OUT)
        ringLit -> listOf(IslandStep.MORPH)
        else -> listOf(IslandStep.RING_IN, IslandStep.MORPH)
    }

    /** IM-4, IM-5: the shape to grow to, or null when there is nothing to show (the island shrinks to the notch). */
    fun target(content: IslandContent, notch: Rect, m: IslandMetrics): Rect? =
        if (content == IslandContent.Hidden) null else IslandLayout.pill(content, notch, m)
}
