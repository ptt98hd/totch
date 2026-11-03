package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.model.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IslandMotionTest {
    private val notch = Rect(500, 0, 580, 80)
    private val m = IslandMetrics(screenWidthPx = 1080, density = 2f)

    @Test fun `IM-8 the island appears in one beat`() {
        assertEquals(listOf(IslandStep.APPEAR), IslandMotion.steps(visible = false, shown = true))
    }

    @Test fun `IM-9 a visible island only morphs, and disappears in one beat`() {
        assertEquals(listOf(IslandStep.MORPH), IslandMotion.steps(visible = true, shown = true))
        listOf(true, false).forEach {
            assertEquals(listOf(IslandStep.DISAPPEAR), IslandMotion.steps(visible = it, shown = false))
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
