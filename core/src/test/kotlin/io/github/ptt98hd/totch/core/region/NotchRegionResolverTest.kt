package io.github.ptt98hd.totch.core.region

import io.github.ptt98hd.totch.core.model.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotchRegionResolverTest {
    private fun resolve(
        cutout: Cutout? = null,
        manual: ManualRegion? = null,
        padding: Int = 0,
        rotation: Int = 0,
        width: Int = 1080,
        statusBar: Int = 90,
    ) = NotchRegionResolver.resolve(width, statusBar, cutout, manual, padding, rotation)

    private val single = Cutout(listOf(Rect(500, 0, 580, 80)))

    @Test fun `NR-1 non portrait rotation disables region`() {
        (1..3).forEach { assertNull(resolve(cutout = single, rotation = it)) }
    }

    @Test fun `NR-2 single cutout rect is used as is`() {
        assertEquals(Rect(500, 0, 580, 80), resolve(cutout = single))
    }

    @Test fun `NR-3 padding expands left right and bottom only`() {
        assertEquals(Rect(490, 0, 590, 90), resolve(cutout = single, padding = 10))
    }

    @Test fun `NR-4 multiple cutout rects are unioned`() {
        val c = Cutout(listOf(Rect(100, 0, 150, 60), Rect(900, 0, 960, 70)))
        assertEquals(Rect(100, 0, 960, 70), resolve(cutout = c))
    }

    @Test fun `NR-5 missing cutout falls back to centered 20 percent`() {
        assertEquals(Rect(432, 0, 648, 90), resolve(cutout = null))
        assertEquals(Rect(432, 0, 648, 90), resolve(cutout = Cutout(emptyList())))
    }

    @Test fun `NR-6 manual region overrides cutout`() {
        val m = ManualRegion(centerXFraction = 0.5f, widthPx = 200, heightPx = 60)
        assertEquals(Rect(400, 0, 600, 60), resolve(cutout = single, manual = m, width = 1000))
    }

    @Test fun `NR-7 padding is clamped to screen`() {
        assertEquals(Rect(0, 0, 70, 70), resolve(cutout = Cutout(listOf(Rect(0, 0, 50, 50))), padding = 20))
        assertEquals(Rect(1010, 0, 1080, 70), resolve(cutout = Cutout(listOf(Rect(1030, 0, 1080, 50))), padding = 20))
    }

    @Test fun `NR-8 empty result is null`() {
        assertNull(resolve(manual = ManualRegion(0.5f, 0, 60)))
        assertNull(resolve(manual = ManualRegion(0.5f, 100, 0)))
        assertNull(resolve(cutout = null, statusBar = 0))
        assertNull(resolve(manual = ManualRegion(2f, 100, 60), width = 0))
    }

    @Test fun `NR-9 manual fraction is clamped`() {
        assertEquals(Rect(0, 0, 100, 60), resolve(manual = ManualRegion(-1f, 200, 60), width = 1000))
        assertEquals(Rect(900, 0, 1000, 60), resolve(manual = ManualRegion(3f, 200, 60), width = 1000))
    }
}
