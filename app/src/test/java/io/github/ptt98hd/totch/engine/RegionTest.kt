package io.github.ptt98hd.totch.engine

import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegionTest {
    private val display = DisplaySnapshot(
        widthPx = 1080, heightPx = 2400, rotation = 0, statusBarPx = 90,
        cutoutTop = listOf(Rect(500, 0, 580, 80)), density = 2f,
    )

    @Test
    fun `cutout region is expanded by padding in px`() {
        assertEquals(Rect(484, 0, 596, 96), notchRegion(Settings(paddingDp = 8), display))
    }

    @Test
    fun `manual region is converted from dp`() {
        val s = Settings(paddingDp = 0, manualRegion = ManualRegionDp(0.5f, widthDp = 100, heightDp = 30))
        assertEquals(Rect(440, 0, 640, 60), notchRegion(s, display))
        val offset = s.copy(manualRegion = ManualRegionDp(0.5f, widthDp = 100, heightDp = 30, topDp = 10))
        assertEquals(Rect(440, 20, 640, 80), notchRegion(offset, display))
    }

    @Test
    fun `NR-12 detected cutout region in dp`() {
        assertEquals(ManualRegionDp(0.5f, widthDp = 40, heightDp = 40, topDp = 0), detectedRegionDp(display))
    }

    @Test
    fun `NR-13 detected default region in dp without cutout`() {
        assertEquals(ManualRegionDp(0.5f, widthDp = 108, heightDp = 45, topDp = 0), detectedRegionDp(display.copy(cutoutTop = emptyList())))
    }

    @Test
    fun `NR-14 no detected region when rotated`() {
        assertNull(detectedRegionDp(display.copy(rotation = 1, widthPx = 2400, heightPx = 1080)))
    }

    @Test
    fun `no cutout falls back to the status bar default`() {
        assertEquals(Rect(432, 0, 648, 90), notchRegion(Settings(paddingDp = 0), display.copy(cutoutTop = emptyList())))
    }

    @Test
    fun `rotated display has no region`() {
        assertNull(notchRegion(Settings(), display.copy(rotation = 1, widthPx = 2400, heightPx = 1080)))
    }

    @Test
    fun `natural size swaps axes when rotated`() {
        assertEquals(1080 to 2400, display.copy(rotation = 1, widthPx = 2400, heightPx = 1080).naturalSize)
        assertEquals(1080 to 2400, display.naturalSize)
    }

    @Test
    fun `NR-15 rotation 90 maps the left edge to the natural top`() {
        assertEquals(Rect(500, 0, 580, 80), toNatural(Rect(0, 500, 80, 580), rotation = 1, widthPx = 2400, heightPx = 1080))
    }

    @Test
    fun `NR-16 rotations 270, 180 and 0`() {
        assertEquals(Rect(500, 0, 580, 80), toNatural(Rect(2320, 500, 2400, 580), rotation = 3, widthPx = 2400, heightPx = 1080))
        assertEquals(Rect(500, 0, 580, 80), toNatural(Rect(500, 2320, 580, 2400), rotation = 2, widthPx = 1080, heightPx = 2400))
        assertEquals(Rect(500, 0, 580, 80), toNatural(Rect(500, 0, 580, 80), rotation = 0, widthPx = 1080, heightPx = 2400))
    }

    @Test
    fun `NR-17 engine region follows the physical notch only with landscape gestures`() {
        val rotated = display.copy(rotation = 1, widthPx = 2400, heightPx = 1080, cutoutTop = emptyList(), naturalCutout = display.cutoutTop)
        assertNull(engineRegion(Settings(), rotated))
        assertEquals(notchRegion(Settings(), display), engineRegion(Settings(landscapeGestures = true), rotated))
        assertEquals(display, rotated.natural())
        assertEquals(notchRegion(Settings(), display), engineRegion(Settings(), display))
    }
}
