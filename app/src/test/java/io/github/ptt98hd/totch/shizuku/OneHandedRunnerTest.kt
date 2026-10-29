package io.github.ptt98hd.totch.shizuku

import io.github.ptt98hd.totch.core.action.OneHandedMethod
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OneHandedRunnerTest {
    private var preferred = OneHandedMethod.SYSTEM
    private val saved = mutableListOf<OneHandedMethod>()
    private val tried = mutableListOf<OneHandedMethod>()

    private fun runner(works: Set<OneHandedMethod>) = OneHandedRunner(
        preferred = { preferred },
        remember = { saved += it },
        attempt = { tried += it; it in works },
    )

    @Test
    fun `OH-5 the working fallback becomes the preferred method`() = runTest {
        assertTrue(runner(setOf(OneHandedMethod.GESTURE)).run().isSuccess)
        assertEquals(listOf(OneHandedMethod.SYSTEM, OneHandedMethod.GESTURE), tried)
        assertEquals(listOf(OneHandedMethod.GESTURE), saved)
    }

    @Test
    fun `OH-5 nothing is saved when the preferred method works`() = runTest {
        preferred = OneHandedMethod.GESTURE
        assertTrue(runner(setOf(OneHandedMethod.GESTURE, OneHandedMethod.SYSTEM)).run().isSuccess)
        assertEquals(listOf(OneHandedMethod.GESTURE), tried)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `OH-5 fails when no method works`() = runTest {
        assertTrue(runner(emptySet()).run().isFailure)
        assertEquals(2, tried.size)
        assertTrue(saved.isEmpty())
    }
}
