package io.github.ptt98hd.totch.service

import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class BootActionTest {
    @Test fun `DM-22 start on boot off disables gestures in both modes`() {
        DetectionMode.entries.forEach { mode ->
            assertEquals(BootAction.DISABLE, bootAction(Settings(enabled = true, mode = mode, startOnBoot = false)))
        }
    }

    @Test fun `DM-20 Shizuku mode starts the service`() {
        assertEquals(BootAction.START_SERVICE, bootAction(Settings(enabled = true, mode = DetectionMode.SHIZUKU)))
    }

    @Test fun `DM-21 Accessibility mode needs nothing`() {
        assertEquals(BootAction.NONE, bootAction(Settings(enabled = true, mode = DetectionMode.ACCESSIBILITY)))
    }

    @Test fun `DM-22 disabled settings need nothing`() {
        DetectionMode.entries.forEach { mode ->
            listOf(true, false).forEach { boot ->
                assertEquals(BootAction.NONE, bootAction(Settings(enabled = false, mode = mode, startOnBoot = boot)))
            }
        }
    }
}
