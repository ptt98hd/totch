package io.github.ptt98hd.totch.service

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatusBarWatcherTest {
    private var shown = true
    private var reads = 0
    private fun read(): Boolean {
        reads++
        return shown
    }

    @Test
    fun `SB-1 starts visible without reading`() = runTest {
        val w = StatusBarWatcher(backgroundScope, ::read)
        runCurrent()
        assertTrue(w.visible.value)
        assertEquals(0, reads)
    }

    @Test
    fun `SB-2 active reads now and every poll`() = runTest {
        val w = StatusBarWatcher(backgroundScope, ::read)
        w.active = true
        runCurrent()
        assertEquals(1, reads)
        shown = false // a video goes full screen
        advanceTimeBy(StatusBarWatcher.POLL_MS - 1)
        assertTrue(w.visible.value)
        advanceTimeBy(2)
        assertEquals(2, reads)
        assertFalse(w.visible.value)
        advanceTimeBy(StatusBarWatcher.POLL_MS)
        assertEquals(3, reads)
    }

    @Test
    fun `SB-3 inactive stops reading and keeps the last value`() = runTest {
        val w = StatusBarWatcher(backgroundScope, ::read)
        shown = false
        w.active = true
        runCurrent()
        w.active = false
        advanceTimeBy(10 * StatusBarWatcher.POLL_MS)
        assertEquals(1, reads)
        assertFalse(w.visible.value)
    }

    @Test
    fun `SB-4 refresh reads at once even when inactive`() = runTest {
        val w = StatusBarWatcher(backgroundScope, ::read)
        shown = false
        w.refresh()
        runCurrent()
        assertEquals(1, reads)
        assertFalse(w.visible.value)
    }
}
