package io.github.ptt98hd.totch.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeepAliveTest {
    @Test fun `DM-54 accessibility is restored only when wanted and lost`() {
        for (canWrite in listOf(true, false)) for (shizuku in listOf(true, false)) {
            assertEquals(RestoreMethod.NONE, AccessibilityKeeper.decide(wanted = false, enabled = false, canWrite, shizuku))
            assertEquals(RestoreMethod.NONE, AccessibilityKeeper.decide(wanted = true, enabled = true, canWrite, shizuku))
        }
        assertEquals(RestoreMethod.WRITE, AccessibilityKeeper.decide(wanted = true, enabled = false, canWriteSecure = true, shizukuReady = true))
        assertEquals(RestoreMethod.WRITE, AccessibilityKeeper.decide(wanted = true, enabled = false, canWriteSecure = true, shizukuReady = false))
        assertEquals(RestoreMethod.SHIZUKU, AccessibilityKeeper.decide(wanted = true, enabled = false, canWriteSecure = false, shizukuReady = true))
        assertEquals(RestoreMethod.NONE, AccessibilityKeeper.decide(wanted = true, enabled = false, canWriteSecure = false, shizukuReady = false))
    }

    @Test fun `DM-50 watchdog period is fifteen minutes`() {
        assertEquals(15 * 60 * 1000L, KeepAliveScheduler.PERIOD_MS)
        assertEquals(1_000L, KeepAliveScheduler.RESTART_DELAY_MS)
    }

    @Test fun `DM-56 auto-start pages are well formed and unique`() {
        val component = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+/[A-Za-z0-9_.$]+""")
        assertTrue(OemSettings.autostart.isNotEmpty())
        OemSettings.autostart.forEach { assertTrue(component.matches(it), it) }
        assertEquals(OemSettings.autostart.size, OemSettings.autostart.toSet().size)
        assertEquals(OemSettings.autostart.map { it.substringBefore('/') }.toSet(), OemSettings.packages)
    }
}
