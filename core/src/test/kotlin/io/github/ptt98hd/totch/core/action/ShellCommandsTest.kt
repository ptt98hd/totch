package io.github.ptt98hd.totch.core.action

import io.github.ptt98hd.totch.core.model.Action
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShellCommandsTest {
    @Test fun `SC-1 command table`() {
        val expected = mapOf(
            Action.Back to "input keyevent 4",
            Action.Home to "input keyevent 3",
            Action.Recents to "input keyevent 187",
            Action.LockScreen to "input keyevent 223",
            Action.Screenshot to "input keyevent 120",
            Action.Notifications to "cmd statusbar expand-notifications",
            Action.QuickSettings to "cmd statusbar expand-settings",
            Action.Assistant to "input keyevent 219",
            Action.LaunchShortcut("com.a", "id", "Label", "intent:#Intent;end") to null,
            Action.None to null,
            Action.ToggleTorch to null,
            Action.MediaPlayPause to null,
            Action.MediaNext to null,
            Action.MediaPrevious to null,
            Action.VolumeUp to null,
            Action.VolumeDown to null,
            Action.VolumePanel to null,
            Action.LaunchApp("com.example") to null,
        )
        expected.forEach { (action, cmd) -> assertEquals(cmd, ShellCommands.of(action), "$action") }
        assertEquals(
            "am start -n com.example.app/com.example.app.MainActivity -a android.intent.action.MAIN " +
                "-c android.intent.category.LAUNCHER -f 0x10200000",
            ShellCommands.launch("com.example.app", "com.example.app.MainActivity"),
        )
        assertEquals(
            "am start -n org.foo_1.bar/.Main\$Inner -a android.intent.action.MAIN " +
                "-c android.intent.category.LAUNCHER -f 0x10200000",
            ShellCommands.launch("org.foo_1.bar", ".Main\$Inner"),
        )
    }

    @Test fun `SC-2 invalid package or activity names are rejected`() {
        listOf("", "com", "1com.foo", "com..foo", "com.foo.", "com.foo bar", "com.foo\$x", "com/foo.bar")
            .forEach { assertNull(ShellCommands.launch(it, "Main"), it) }
        listOf("", "Main Activity", "Main;id", "Main/x", "Main`id`", "Main\nx")
            .forEach { assertNull(ShellCommands.launch("com.foo", it), it) }
    }

    @Test fun `SC-3 shell injection attempt returns null`() {
        assertNull(ShellCommands.launch("com.foo; rm -rf /", "X"))
    }

    @Test fun `SC-4 startIntent quotes the intent uri`() {
        assertEquals(
            "am start 'intent:#Intent;component=com.a/.B;end'",
            ShellCommands.startIntent("intent:#Intent;component=com.a/.B;end"),
        )
        assertEquals("am start '#Intent;action=x;end'", ShellCommands.startIntent("#Intent;action=x;end"))
    }

    @Test fun `SC-5 single quotes cannot escape the quoted string`() {
        assertEquals(
            "am start 'intent:#Intent;S.q=a'\\''; reboot;'\\'';end'",
            ShellCommands.startIntent("intent:#Intent;S.q=a'; reboot;';end"),
        )
    }

    @Test fun `SC-6 non intent uris and control characters are rejected`() {
        listOf("", "http://x", "; reboot", "intent:#Intent;S.a=1\nreboot;end", "intent:#Intent;\rend", "intent:\u0000")
            .forEach { assertNull(ShellCommands.startIntent(it), it) }
    }

    @Test fun `SC-7 startActivity quotes the component`() {
        assertEquals("am start -n 'com.a/com.a.Main\$Sub' -f 0x10000000", ShellCommands.startActivity("com.a", "com.a.Main\$Sub"))
        assertNull(ShellCommands.startActivity("com.a; reboot", "Main"))
        assertNull(ShellCommands.startActivity("com.a", "Main'; reboot"))
        assertNull(ShellCommands.of(Action.LaunchActivity("com.a", "com.a.Main", "Main")))
    }

    @Test fun `SC-8 clickTile quotes the component`() {
        assertTrue("c='com.a/com.a.qs.Tile\$Inner';" in ShellCommands.clickTile("com.a", "com.a.qs.Tile\$Inner")!!)
        assertNull(ShellCommands.clickTile("com.a; reboot", "Tile"))
        assertNull(ShellCommands.clickTile("com.a", "Tile'; reboot"))
        assertNull(ShellCommands.of(Action.ClickTile("com.a", "com.a.Tile", "Tile")))
    }

    @Test fun `QT-2 a tile already in the panel is only clicked`() {
        val sim = ShellSim().apply { secure["sysui_qs_tiles"] = "wifi,custom(com.a/.qs.Tile),bt" }
        assertEquals(0, sim.run(ShellCommands.clickTile("com.a", "com.a.qs.Tile")!!))
        assertEquals(listOf("cmd statusbar click-tile com.a/com.a.qs.Tile"), sim.calls("cmd"))

        val full = ShellSim().apply { secure["sysui_qs_tiles"] = "custom(com.a/com.a.qs.Tile)" }
        full.run(ShellCommands.clickTile("com.a", "com.a.qs.Tile")!!)
        assertEquals(listOf("cmd statusbar click-tile com.a/com.a.qs.Tile"), full.calls("cmd"))
    }

    @Test fun `QT-2 a missing tile is added, clicked and removed`() {
        val sim = ShellSim().apply { secure["sysui_qs_tiles"] = "wifi,custom(com.a/.Other)" }
        assertEquals(0, sim.run(ShellCommands.clickTile("com.a", "com.a.qs.Tile\$Inner")!!))
        assertEquals(
            listOf(
                "cmd statusbar add-tile com.a/com.a.qs.Tile\$Inner",
                "cmd statusbar click-tile com.a/com.a.qs.Tile\$Inner",
                "cmd statusbar remove-tile com.a/com.a.qs.Tile\$Inner",
            ),
            sim.calls("cmd"),
        )

        val failing = ShellSim().apply { failing += "click-tile" }
        assertEquals(1, failing.run(ShellCommands.clickTile("com.a", "org.b.Tile")!!))
        assertEquals("cmd statusbar remove-tile com.a/org.b.Tile", failing.calls("cmd").last())
        assertFalse("custom(com.a/." in ShellCommands.clickTile("com.a", "org.b.Tile")!!)
    }

    @Test fun `SC-9 one-handed mode has no plain shell command`() {
        assertNull(ShellCommands.of(Action.OneHandedMode))
    }

    @Test fun `SC-10 brightness keys`() {
        assertEquals("input keyevent 221", ShellCommands.of(Action.BrightnessUp))
        assertEquals("input keyevent 220", ShellCommands.of(Action.BrightnessDown))
    }

    /** Key read by each toggle, a value meaning "on", and the switch-on / switch-off calls (actions spec §13). */
    private val toggles = listOf(
        Toggle(Action.ToggleAutoBrightness, "screen_brightness_mode", "1", "settings put system screen_brightness_mode 1", "settings put system screen_brightness_mode 0"),
        Toggle(Action.ToggleAutoRotate, "accelerometer_rotation", "1", "settings put system accelerometer_rotation 1", "settings put system accelerometer_rotation 0"),
        Toggle(Action.ToggleWifi, "wifi_on", "1", "svc wifi enable", "svc wifi disable"),
        Toggle(Action.ToggleBluetooth, "bluetooth_on", "1", "cmd bluetooth_manager enable", "cmd bluetooth_manager disable"),
        Toggle(Action.ToggleMobileData, "mobile_data", "1", "svc data enable", "svc data disable"),
        Toggle(Action.ToggleAirplaneMode, "airplane_mode_on", "1", "cmd connectivity airplane-mode enable", "cmd connectivity airplane-mode disable"),
        Toggle(Action.ToggleLocation, "location_mode", "3", "cmd location set-location-enabled true", "cmd location set-location-enabled false"),
    )

    private data class Toggle(val action: Action, val key: String, val onValue: String, val enable: String, val disable: String)

    /** Calls other than reading the state. */
    private fun ShellSim.changes() = log.filterNot { it.startsWith("settings get ") }

    @Test fun `SC-11 toggles switch off when on and on otherwise`() {
        toggles.forEach { t ->
            val script = assertNotNull(ShellCommands.of(t.action), "${t.action}")

            val on = ShellSim().apply { secure[t.key] = t.onValue }
            assertEquals(0, on.run(script), "${t.action}")
            assertEquals(listOf(t.disable), on.changes(), "${t.action} on")

            val off = ShellSim().apply { secure[t.key] = "0" }
            assertEquals(0, off.run(script))
            assertEquals(listOf(t.enable), off.changes(), "${t.action} off")

            val unset = ShellSim()
            assertEquals(0, unset.run(script))
            assertEquals(listOf(t.enable), unset.changes(), "${t.action} null")
        }
        val failing = ShellSim().apply { failing += "svc data" }
        assertEquals(1, failing.run(ShellCommands.of(Action.ToggleMobileData)!!))
    }

    @Test fun `SC-12 wifi airplane states and bluetooth fallback`() {
        val wifi = ShellCommands.of(Action.ToggleWifi)!!
        val airplaneOn = ShellSim().apply { secure["wifi_on"] = "2" }
        airplaneOn.run(wifi)
        assertEquals(listOf("svc wifi disable"), airplaneOn.changes())
        val airplaneOff = ShellSim().apply { secure["wifi_on"] = "3" }
        airplaneOff.run(wifi)
        assertEquals(listOf("svc wifi enable"), airplaneOff.changes())

        val android12 = ShellSim().apply { failing += "cmd bluetooth_manager" }
        assertEquals(0, android12.run(ShellCommands.of(Action.ToggleBluetooth)!!))
        assertEquals(listOf("cmd bluetooth_manager enable", "svc bluetooth enable"), android12.changes())
    }

    @Test fun `SM-3 sound modes are not shell actions`() {
        listOf(Action.RingerNormal, Action.RingerVibrate, Action.RingerSilent, Action.CycleRingerMode, Action.ToggleDnd)
            .forEach { assertNull(ShellCommands.of(it), "$it") }
    }
}
