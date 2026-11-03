package io.github.ptt98hd.totch.engine

import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.settings.NotchRegionPx
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
    fun `NR-22 cutout region is expanded by padding in px, not dp`() {
        assertEquals(Rect(492, 0, 588, 88), notchRegion(Settings(paddingPx = 8), display))
    }

    @Test
    fun `NR-20 manual region is used in px whatever the density`() {
        val s = Settings(paddingPx = 0, notchRegion = NotchRegionPx(540, widthPx = 41, heightPx = 41, topPx = 30))
        assertEquals(Rect(520, 30, 561, 71), notchRegion(s, display))
        assertEquals(Rect(520, 30, 561, 71), notchRegion(s, display.copy(density = 3.5f)))
        val area = highlightArea(s.copy(paddingPx = 10), display)
        assertEquals(Rect(520, 30, 561, 71), area?.notch)
        assertEquals(Rect(510, 20, 571, 81), area?.touch)
    }

    @Test
    fun `NR-23 detected region in px rebuilds the detected rect`() {
        assertEquals(NotchRegionPx(540, 80, 80, 0), detectedRegionPx(display))
        val odd = display.copy(cutoutTop = listOf(Rect(500, 0, 581, 80)))
        val detected = detectedRegionPx(odd)
        assertEquals(NotchRegionPx(540, 81, 80, 0), detected)
        assertEquals(Rect(500, 0, 581, 80), notchRegion(Settings(paddingPx = 0, notchRegion = detected), odd))
        assertEquals(NotchRegionPx(540, 216, 90, 0), detectedRegionPx(display.copy(cutoutTop = emptyList())))
        assertNull(detectedRegionPx(display.copy(rotation = 1, widthPx = 2400, heightPx = 1080)))
    }

    @Test
    fun `no cutout falls back to the status bar default`() {
        assertEquals(Rect(432, 0, 648, 90), notchRegion(Settings(paddingPx = 0), display.copy(cutoutTop = emptyList())))
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

    @Test
    fun `NR-18 fromNatural is the inverse of toNatural`() {
        val notch = Rect(500, 0, 580, 80)
        assertEquals(Rect(0, 500, 80, 580), fromNatural(notch, rotation = 1, widthPx = 2400, heightPx = 1080))
        assertEquals(Rect(2320, 500, 2400, 580), fromNatural(notch, rotation = 3, widthPx = 2400, heightPx = 1080))
        assertEquals(Rect(500, 2320, 580, 2400), fromNatural(notch, rotation = 2, widthPx = 1080, heightPx = 2400))
        assertEquals(notch, fromNatural(notch, rotation = 0, widthPx = 1080, heightPx = 2400))
        val r = Rect(100, 20, 300, 260)
        listOf(0 to (1080 to 2400), 1 to (2400 to 1080), 2 to (1080 to 2400), 3 to (2400 to 1080)).forEach { (rotation, size) ->
            val (w, h) = size
            assertEquals(r, toNatural(fromNatural(r, rotation, w, h), rotation, w, h), "rotation $rotation")
        }
    }

    @Test
    fun `NR-19 points to natural coordinates`() {
        assertEquals(540f to 40f, pointToNatural(40f, 540f, rotation = 1, widthPx = 2400, heightPx = 1080))
        assertEquals(540f to 40f, pointToNatural(2360f, 540f, rotation = 3, widthPx = 2400, heightPx = 1080))
        assertEquals(540f to 40f, pointToNatural(540f, 2360f, rotation = 2, widthPx = 1080, heightPx = 2400))
        assertEquals(540f to 40f, pointToNatural(540f, 40f, rotation = 0, widthPx = 1080, heightPx = 2400))
    }
}
