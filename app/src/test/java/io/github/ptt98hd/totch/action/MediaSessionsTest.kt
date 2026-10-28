package io.github.ptt98hd.totch.action

import io.github.ptt98hd.totch.core.model.Action
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaSessionsTest {
    private fun session(pkg: String, playing: Boolean) = MediaSessionInfo(pkg, playing)

    @Test
    fun `MC-1 no sessions gives no target`() {
        assertNull(chooseMediaSession(emptyList()))
    }

    @Test
    fun `MC-2 the first playing session wins`() {
        val sessions = listOf(session("a", false), session("b", true), session("c", true))
        assertEquals(1, chooseMediaSession(sessions))
    }

    @Test
    fun `MC-3 without a playing session the highest priority one is used`() {
        assertEquals(0, chooseMediaSession(listOf(session("a", false), session("b", false))))
    }

    @Test
    fun `MC-4 play-pause toggles on the session state, skips map directly`() {
        assertEquals(MediaCommand.PAUSE, mediaCommand(Action.MediaPlayPause, playing = true))
        assertEquals(MediaCommand.PLAY, mediaCommand(Action.MediaPlayPause, playing = false))
        assertEquals(MediaCommand.NEXT, mediaCommand(Action.MediaNext, playing = false))
        assertEquals(MediaCommand.PREVIOUS, mediaCommand(Action.MediaPrevious, playing = true))
        assertNull(mediaCommand(Action.ToggleTorch, playing = true))
    }
}
