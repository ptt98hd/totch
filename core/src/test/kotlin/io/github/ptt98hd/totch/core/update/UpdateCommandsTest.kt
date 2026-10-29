package io.github.ptt98hd.totch.core.update

import io.github.ptt98hd.totch.core.action.ShellSim
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateCommandsTest {
    private val apk = "/data/local/tmp/totch-update.apk"

    @Test fun `UP-14 shell install runs detached and rejects unsafe names`() {
        val command = UpdateCommands.shellInstall("a.b", "a.b.ui.Main", apk)!!
        assertEquals(
            "setsid sh -c '${UpdateCommands.shellInstallScript("a.b", "a.b.ui.Main", apk)}' </dev/null >/dev/null 2>&1 &",
            command,
        )
        assertNull(UpdateCommands.shellInstall("a.b; reboot", "a.b.Main", apk))
        assertNull(UpdateCommands.shellInstall("a.b", "Main'x", apk))
        listOf("/sdcard/x.apk", "/data/local/tmp/../x.apk", "/data/local/tmp/a b.apk", "/data/local/tmp/x.txt", "/data/local/tmp/x'.apk")
            .forEach { assertNull(UpdateCommands.shellInstall("a.b", "a.b.Main", it), it) }
        assertEquals(true, UpdateCommands.isSafeApkPath(apk))
    }

    @Test fun `UP-15 install success reopens the app, failure reports it`() {
        val ok = ShellSim()
        assertEquals(0, ok.run(UpdateCommands.shellInstallScript("a.b", "a.b.ui.Main", apk)))
        assertEquals(listOf("pm install -r $apk", "am start -n a.b/a.b.ui.Main"), ok.log)

        val failed = ShellSim().apply { failing += "pm install" }
        failed.run(UpdateCommands.shellInstallScript("a.b", "a.b.ui.Main", apk))
        assertEquals(
            listOf("pm install -r $apk", "am start -n a.b/a.b.ui.Main -a a.b.action.UPDATE_FAILED"),
            failed.log,
        )
    }
}
