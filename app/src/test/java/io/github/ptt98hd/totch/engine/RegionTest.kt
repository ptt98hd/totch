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
}
