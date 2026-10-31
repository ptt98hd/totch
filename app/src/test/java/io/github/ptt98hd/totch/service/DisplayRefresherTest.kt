package io.github.ptt98hd.totch.service

import io.github.ptt98hd.totch.engine.DisplaySnapshot
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayRefresherTest {
    private var rotation = 0
    private var reads = 0
    private fun read(): DisplaySnapshot {
        reads++
        return DisplaySnapshot(1080, 2400, rotation, 90, emptyList(), 2f)
    }

    @Test
    fun `DR-1 reads at once`() = runTest {
        val r = DisplayRefresher(backgroundScope, ::read)
        assertEquals(1, reads)
        assertEquals(0, r.state.value.rotation)
    }

    @Test
    fun `DR-2 refresh reads now and again after 250 and 1000 ms`() = runTest {
        val r = DisplayRefresher(backgroundScope, ::read)
        r.refresh()
        runCurrent()
        assertEquals(2, reads)
        rotation = 1 // the system updates late
        advanceTimeBy(249)
        assertEquals(0, r.state.value.rotation)
        advanceTimeBy(2)
        assertEquals(3, reads)
        assertEquals(1, r.state.value.rotation)
        advanceTimeBy(750)
        assertEquals(4, reads)
        advanceTimeBy(10_000)
        assertEquals(4, reads)
    }

    @Test
    fun `DR-3 a new refresh restarts the schedule`() = runTest {
        val r = DisplayRefresher(backgroundScope, ::read)
        r.refresh()
        runCurrent()
        advanceTimeBy(200)
        r.refresh()
        runCurrent()
        assertEquals(3, reads)
        advanceTimeBy(100) // 300 ms after the first call: its 250 ms read was cancelled
        assertEquals(3, reads)
        advanceTimeBy(151)
        assertEquals(4, reads)
        advanceTimeBy(750)
        assertEquals(5, reads)
        advanceTimeBy(10_000)
        assertEquals(5, reads)
    }
}
