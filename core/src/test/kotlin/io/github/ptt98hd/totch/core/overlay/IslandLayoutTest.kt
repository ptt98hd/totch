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

    @Test fun `IG-16 rest is the notch itself, kept a pill`() {
        assertEquals(notch, IslandLayout.rest(notch, m))
        assertEquals(Rect(520, 30, 560, 70), IslandLayout.rest(Rect(520, 30, 560, 70), m))
        assertEquals(Rect(520, 0, 560, 40), IslandLayout.rest(Rect(530, 0, 550, 40), m))
        assertEquals(Rect(0, 0, 25, 40), IslandLayout.rest(Rect(0, 0, 10, 40), m))
    }

    @Test fun `IG-11 hidden is the rest shape`() {
        assertEquals(notch, IslandLayout.pill(IslandContent.Hidden, notch, m))
    }

    @Test fun `IG-17 icon and padding follow the notch height`() {
        assertEquals(44, IslandLayout.iconPx(notch, m))
        assertEquals(18, IslandLayout.padPx(notch, m))
        assertEquals(20, IslandLayout.iconPx(Rect(500, 0, 580, 20), m))
        assertEquals(8, IslandLayout.padPx(Rect(500, 0, 580, 20), m))
        assertEquals(10, IslandLayout.iconPx(Rect(500, 0, 580, 10), m))
        assertEquals(8, IslandLayout.padPx(Rect(500, 0, 580, 10), m))
    }

    @Test fun `IG-18 each compact content grows by its own wing`() {
        listOf(compact, IslandContent.Notification("p", 3), IslandContent.Volume(1f, VolumeStream.RING))
            .forEach { assertEquals(Rect(420, 0, 660, 80), IslandLayout.pill(it, notch, m)) }
        assertEquals(Rect(396, 0, 684, 80), IslandLayout.pill(IslandContent.Charging(5), notch, m))
        assertEquals(Rect(336, 0, 744, 80), IslandLayout.pill(IslandContent.Sound(SoundMode.RING, 0.5f), notch, m))
    }

    @Test fun `IG-18 compact is clamped to the screen margins`() {
        assertEquals(Rect(16, 0, 180, 80), IslandLayout.pill(compact, Rect(20, 0, 100, 80), m))
        assertEquals(Rect(900, 0, 1064, 80), IslandLayout.pill(compact, Rect(980, 0, 1060, 80), m))
    }

    @Test fun `IG-19 expanded media and its controls`() {
        val pill = IslandLayout.pill(expanded, notch, m)
        assertEquals(Rect(24, 0, 1056, 352), pill)
        val controls = IslandLayout.controls(pill, m)
        assertEquals(Rect(492, 232, 588, 328), controls[IslandControl.PLAY_PAUSE])
        assertEquals(Rect(348, 232, 444, 328), controls[IslandControl.PREVIOUS])
        assertEquals(Rect(636, 232, 732, 328), controls[IslandControl.NEXT])
    }

    @Test fun `IG-20 the touch-feedback wing is an icon wing`() {
        assertEquals(80, IslandLayout.feedbackWing(notch, m))
    }

    @Test fun `IG-5 hit test`() {
        assertEquals(IslandHit.Control(IslandControl.NEXT), IslandLayout.hit(expanded, notch, m, 700f, 300f))
        assertEquals(IslandHit.Body, IslandLayout.hit(expanded, notch, m, 100f, 100f))
        assertEquals(IslandHit.Outside, IslandLayout.hit(expanded, notch, m, 5f, 100f))
        assertEquals(IslandHit.Body, IslandLayout.hit(compact, notch, m, 430f, 40f))
        assertEquals(IslandHit.Outside, IslandLayout.hit(compact, notch, m, 700f, 300f))
        assertEquals(IslandHit.Outside, IslandLayout.hit(IslandContent.Hidden, notch, m, 540f, 40f))
    }
}
