package io.github.ptt98hd.totch.service

import io.github.ptt98hd.totch.core.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class BootActionTest {
    @Test fun `DM-22 start on boot off disables gestures`() {
        assertEquals(BootAction.DISABLE, startupAction(Settings(enabled = true, startOnBoot = false), StartupEvent.BOOT))
    }

    @Test fun `DM-20 enabled gestures start the service`() {
        assertEquals(BootAction.START_SERVICE, startupAction(Settings(enabled = true), StartupEvent.BOOT))
    }

    @Test fun `DM-22 disabled settings need nothing`() {
        listOf(true, false).forEach { boot ->
            StartupEvent.entries.forEach { event ->
                assertEquals(BootAction.NONE, startupAction(Settings(enabled = false, startOnBoot = boot), event))
            }
        }
    }

    @Test fun `DM-51 an app update restarts enabled gestures and never disables them`() {
        assertEquals(BootAction.START_SERVICE, startupAction(Settings(enabled = true), StartupEvent.PACKAGE_REPLACED))
        assertEquals(
            BootAction.START_SERVICE,
            startupAction(Settings(enabled = true, startOnBoot = false), StartupEvent.PACKAGE_REPLACED),
        )
    }

    @Test fun `DM-51 quick boot intents count as a boot`() {
        assertEquals(StartupEvent.BOOT, startupEvent("android.intent.action.BOOT_COMPLETED"))
        assertEquals(StartupEvent.BOOT, startupEvent("android.intent.action.QUICKBOOT_POWERON"))
        assertEquals(StartupEvent.BOOT, startupEvent("com.htc.intent.action.QUICKBOOT_POWERON"))
        assertEquals(StartupEvent.PACKAGE_REPLACED, startupEvent("android.intent.action.MY_PACKAGE_REPLACED"))
        assertEquals(null, startupEvent("android.intent.action.SCREEN_ON"))
    }
}
