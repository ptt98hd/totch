package io.github.ptt98hd.totch.service

import io.github.ptt98hd.totch.core.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class BootActionTest {
    @Test fun `DM-22 start on boot off disables gestures`() {
        assertEquals(BootAction.DISABLE, bootAction(Settings(enabled = true, startOnBoot = false)))
    }

    @Test fun `DM-20 enabled gestures start the service`() {
        assertEquals(BootAction.START_SERVICE, bootAction(Settings(enabled = true)))
    }

    @Test fun `DM-22 disabled settings need nothing`() {
        listOf(true, false).forEach { boot ->
            assertEquals(BootAction.NONE, bootAction(Settings(enabled = false, startOnBoot = boot)))
        }
    }
}
