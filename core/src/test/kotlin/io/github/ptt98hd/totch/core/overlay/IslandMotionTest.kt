package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.model.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IslandMotionTest {
    private val notch = Rect(500, 0, 580, 80)
    private val m = IslandMetrics(screenWidthPx = 1080, density = 2f)

    @Test fun `IM-1 the ring lights up before the island grows`() {
        assertEquals(listOf(IslandStep.RING_IN, IslandStep.MORPH), IslandMotion.steps(ringLit = false, shown = true))
    }

    @Test fun `IM-2 a lit ring only morphs`() {
        assertEquals(listOf(IslandStep.MORPH), IslandMotion.steps(ringLit = true, shown = true))
    }

    @Test fun `IM-3 the island shrinks before the ring goes out`() {
        listOf(true, false).forEach {
            assertEquals(listOf(IslandStep.MORPH, IslandStep.RING_OUT), IslandMotion.steps(ringLit = it, shown = false))
        }
    }

    @Test fun `IM-4 content grows to its pill`() {
        val sound = IslandContent.Sound(SoundMode.RING)
        assertEquals(IslandLayout.pill(sound, notch, m), IslandMotion.target(sound, notch, m))
    }

    @Test fun `IM-5 nothing to show`() {
        assertNull(IslandMotion.target(IslandContent.Hidden, notch, m))
    }
}
