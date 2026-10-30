package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.overlay.IslandController.Companion.CHARGING_MS
import io.github.ptt98hd.totch.core.overlay.IslandController.Companion.EXPANDED_MS
import io.github.ptt98hd.totch.core.overlay.IslandController.Companion.MEDIA_LINGER_MS
import io.github.ptt98hd.totch.core.overlay.IslandController.Companion.NOTIFICATION_MS
import io.github.ptt98hd.totch.core.overlay.IslandController.Companion.SOUND_MS
import io.github.ptt98hd.totch.core.overlay.IslandController.Companion.VOLUME_MS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IslandControllerTest {
    private val island = IslandController(IslandActivity.entries.toSet())
    private val playing = MediaInfo("com.music", "Song", "Artist", playing = true)
    private val paused = playing.copy(playing = false)

    @Test fun `IS-1 nothing to show`() {
        assertEquals(IslandContent.Hidden, island.content)
        assertNull(island.nextDeadlineMs())
    }

    @Test fun `IS-2 playing media shows compact`() {
        island.setMedia(playing, 0)
        assertEquals(IslandContent.Media(playing, expanded = false), island.content)
        assertNull(island.nextDeadlineMs())
    }

    @Test fun `IS-3 paused media lingers then hides`() {
        island.setMedia(playing, 0)
        island.setMedia(paused, 1_000)
        assertEquals(IslandContent.Media(paused, expanded = false), island.content)
        assertEquals(1_000 + MEDIA_LINGER_MS, island.nextDeadlineMs())
        island.onTick(1_000 + MEDIA_LINGER_MS)
        assertEquals(IslandContent.Hidden, island.content)
    }

    @Test fun `IS-3 playing again before the linger ends keeps it`() {
        island.setMedia(playing, 0)
        island.setMedia(paused, 1_000)
        island.setMedia(playing, 2_000)
        assertNull(island.nextDeadlineMs())
        island.onTick(1_000 + MEDIA_LINGER_MS)
        assertEquals(IslandContent.Media(playing, expanded = false), island.content)
    }

    @Test fun `IS-3 paused media never seen playing stays hidden`() {
        island.setMedia(paused, 0)
        assertEquals(IslandContent.Hidden, island.content)
    }

    @Test fun `IS-4 media gone`() {
        island.setMedia(playing, 0)
        island.setMedia(null, 10)
        assertEquals(IslandContent.Hidden, island.content)
    }

    @Test fun `IS-5 transient events show for their duration`() {
        val cases = listOf(
            IslandEvent.Charging(54) to (IslandContent.Charging(54) to CHARGING_MS),
            IslandEvent.Notification("com.chat") to (IslandContent.Notification("com.chat") to NOTIFICATION_MS),
            IslandEvent.Sound(SoundMode.SILENT) to (IslandContent.Sound(SoundMode.SILENT) to SOUND_MS),
            IslandEvent.Volume(0.5f) to (IslandContent.Volume(0.5f) to VOLUME_MS),
        )
        for ((event, expected) in cases) {
            val c = IslandController(IslandActivity.entries.toSet())
            c.setMedia(playing, 0)
            c.onEvent(event, 100)
            assertEquals(expected.first, c.content)
            assertEquals(100 + expected.second, c.nextDeadlineMs())
            c.onTick(99 + expected.second)
            assertEquals(expected.first, c.content)
            c.onTick(100 + expected.second)
            assertEquals(IslandContent.Media(playing, expanded = false), c.content)
        }
    }

    @Test fun `IS-6 a new transient replaces the current one`() {
        island.onEvent(IslandEvent.Charging(10), 0)
        island.onEvent(IslandEvent.Sound(SoundMode.RING), 1_000)
        assertEquals(IslandContent.Sound(SoundMode.RING), island.content)
        assertEquals(1_000 + SOUND_MS, island.nextDeadlineMs())
    }

    @Test fun `IS-7 transient over expanded media collapses it afterwards`() {
        island.setMedia(playing, 0)
        island.onTap(0)
        island.onEvent(IslandEvent.Charging(80), 1_000)
        assertEquals(IslandContent.Charging(80), island.content)
        island.onTick(1_000 + CHARGING_MS)
        assertEquals(IslandContent.Media(playing, expanded = false), island.content)
    }

    @Test fun `IS-8 tap expands and collapses media`() {
        island.setMedia(playing, 0)
        assertEquals(IslandTap.EXPANDED, island.onTap(10))
        assertEquals(IslandContent.Media(playing, expanded = true), island.content)
        assertEquals(IslandTap.COLLAPSED, island.onTap(20))
        assertEquals(IslandContent.Media(playing, expanded = false), island.content)
    }

    @Test fun `IS-9 expanded media collapses after inactivity`() {
        island.setMedia(playing, 0)
        island.onTap(100)
        assertEquals(100 + EXPANDED_MS, island.nextDeadlineMs())
        island.onInteraction(3_000)
        island.onTick(100 + EXPANDED_MS)
        assertEquals(IslandContent.Media(playing, expanded = true), island.content)
        island.onTick(3_000 + EXPANDED_MS)
        assertEquals(IslandContent.Media(playing, expanded = false), island.content)
    }

    @Test fun `IS-10 tapping a notification opens the shade`() {
        island.setMedia(playing, 0)
        island.onEvent(IslandEvent.Notification("com.chat"), 10)
        assertEquals(IslandTap.OPEN_NOTIFICATIONS, island.onTap(20))
        assertEquals(IslandContent.Media(playing, expanded = false), island.content)
    }

    @Test fun `IS-11 other contents ignore taps`() {
        assertEquals(IslandTap.NONE, island.onTap(0))
        island.onEvent(IslandEvent.Charging(5), 0)
        assertEquals(IslandTap.NONE, island.onTap(1))
        assertEquals(IslandContent.Charging(5), island.content)
    }

    @Test fun `IS-12 disabled activities are ignored`() {
        val c = IslandController(setOf(IslandActivity.CHARGING))
        c.setMedia(playing, 0)
        assertEquals(IslandContent.Hidden, c.content)
        c.onEvent(IslandEvent.Notification("com.chat"), 0)
        c.onEvent(IslandEvent.Sound(SoundMode.DND_ON), 0)
        c.onEvent(IslandEvent.Volume(0.2f), 0)
        assertEquals(IslandContent.Hidden, c.content)
        c.onEvent(IslandEvent.Charging(30), 0)
        assertEquals(IslandContent.Charging(30), c.content)
        c.activities = setOf(IslandActivity.MEDIA, IslandActivity.CHARGING)
        c.onTick(CHARGING_MS)
        assertEquals(IslandContent.Media(playing, expanded = false), c.content)
        c.activities = emptySet()
        assertEquals(IslandContent.Hidden, c.content)
    }

    @Test fun `IS-13 earliest deadline and early ticks`() {
        island.setMedia(playing, 0)
        island.setMedia(paused, 0)
        island.onEvent(IslandEvent.Volume(0.1f), 1_000)
        assertEquals(1_000 + VOLUME_MS, island.nextDeadlineMs())
        island.onTick(500)
        assertEquals(IslandContent.Volume(0.1f), island.content)
        island.onTick(1_000 + VOLUME_MS)
        assertEquals(IslandContent.Media(paused, expanded = false), island.content)
        assertEquals(MEDIA_LINGER_MS, island.nextDeadlineMs())
    }

    @Test fun `IS-14 consecutive volume changes extend`() {
        island.onEvent(IslandEvent.Volume(0.1f), 0)
        island.onEvent(IslandEvent.Volume(0.2f), 1_000)
        assertEquals(IslandContent.Volume(0.2f), island.content)
        island.onTick(VOLUME_MS)
        assertEquals(IslandContent.Volume(0.2f), island.content)
        assertEquals(1_000 + VOLUME_MS, island.nextDeadlineMs())
    }
}
