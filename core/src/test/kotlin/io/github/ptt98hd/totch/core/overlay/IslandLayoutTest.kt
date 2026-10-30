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

    @Test fun `IG-1 hidden is the notch`() {
        assertEquals(notch, IslandLayout.pill(IslandContent.Hidden, notch, m))
    }

    @Test fun `IG-2 compact contents grow by a wing on each side`() {
        listOf(compact, IslandContent.Charging(5), IslandContent.Notification("p"), IslandContent.Volume(1f))
            .forEach { assertEquals(Rect(388, 0, 692, 80), IslandLayout.pill(it, notch, m)) }
        assertEquals(Rect(340, 0, 740, 80), IslandLayout.pill(IslandContent.Sound(SoundMode.RING), notch, m))
    }

    @Test fun `IG-2 compact is clamped to the screen margins`() {
        assertEquals(Rect(16, 0, 212, 80), IslandLayout.pill(compact, Rect(20, 0, 100, 80), m))
        assertEquals(Rect(868, 0, 1064, 80), IslandLayout.pill(compact, Rect(980, 0, 1060, 80), m))
    }

    @Test fun `IG-3 expanded media`() {
        assertEquals(Rect(24, 0, 1056, 352), IslandLayout.pill(expanded, notch, m))
    }

    @Test fun `IG-4 and IG-6 controls`() {
        val controls = IslandLayout.controls(IslandLayout.pill(expanded, notch, m), m)
        assertEquals(Rect(492, 232, 588, 328), controls[IslandControl.PLAY_PAUSE])
        assertEquals(Rect(348, 232, 444, 328), controls[IslandControl.PREVIOUS])
        assertEquals(Rect(636, 232, 732, 328), controls[IslandControl.NEXT])
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
