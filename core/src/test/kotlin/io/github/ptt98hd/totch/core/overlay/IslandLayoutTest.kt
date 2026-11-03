package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.model.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class IslandLayoutTest {
    private val notch = Rect(500, 0, 580, 80)
    private val m = IslandMetrics(screenWidthPx = 1080, density = 2f)
    private val media = MediaInfo("com.music", "Song", null, playing = true)
    private val compact = IslandContent.Media(media, expanded = false)
    private val expanded = IslandContent.Media(media, expanded = true)

    @Test fun `IG-10 rest is the notch in a black rim`() {
        assertEquals(Rect(492, 0, 588, 88), IslandLayout.rest(notch, m))
        assertEquals(Rect(512, 22, 568, 78), IslandLayout.rest(Rect(520, 30, 560, 70), m))
        assertEquals(Rect(512, 0, 568, 53), IslandLayout.rest(Rect(525, 10, 555, 40), m))
        assertEquals(Rect(0, 0, 48, 48), IslandLayout.rest(Rect(0, 0, 40, 40), m))
    }

    @Test fun `IG-11 hidden is the rest shape`() {
        assertEquals(Rect(492, 0, 588, 88), IslandLayout.pill(IslandContent.Hidden, notch, m))
    }

    @Test fun `IG-12 compact contents grow the rest shape by a wing on each side`() {
        listOf(compact, IslandContent.Charging(5), IslandContent.Notification("p"), IslandContent.Volume(1f))
            .forEach { assertEquals(Rect(372, 0, 708, 88), IslandLayout.pill(it, notch, m)) }
        assertEquals(Rect(324, 0, 756, 88), IslandLayout.pill(IslandContent.Sound(SoundMode.RING), notch, m))
    }

    @Test fun `IG-12 compact is clamped to the screen margins`() {
        assertEquals(Rect(16, 0, 228, 88), IslandLayout.pill(compact, Rect(20, 0, 100, 80), m))
        assertEquals(Rect(852, 0, 1064, 88), IslandLayout.pill(compact, Rect(980, 0, 1060, 80), m))
    }

    @Test fun `IG-13 expanded media and its controls`() {
        val pill = IslandLayout.pill(expanded, notch, m)
        assertEquals(Rect(24, 0, 1056, 360), pill)
        val controls = IslandLayout.controls(pill, m)
        assertEquals(Rect(492, 240, 588, 336), controls[IslandControl.PLAY_PAUSE])
        assertEquals(Rect(348, 240, 444, 336), controls[IslandControl.PREVIOUS])
        assertEquals(Rect(636, 240, 732, 336), controls[IslandControl.NEXT])
    }

    @Test fun `IG-5 hit test`() {
        assertEquals(IslandHit.Control(IslandControl.NEXT), IslandLayout.hit(expanded, notch, m, 700f, 300f))
        assertEquals(IslandHit.Body, IslandLayout.hit(expanded, notch, m, 100f, 100f))
        assertEquals(IslandHit.Outside, IslandLayout.hit(expanded, notch, m, 5f, 100f))
        assertEquals(IslandHit.Body, IslandLayout.hit(compact, notch, m, 400f, 40f))
        assertEquals(IslandHit.Outside, IslandLayout.hit(compact, notch, m, 700f, 300f))
        assertEquals(IslandHit.Outside, IslandLayout.hit(IslandContent.Hidden, notch, m, 540f, 40f))
    }
}
