package io.github.ptt98hd.totch.core.action

import io.github.ptt98hd.totch.core.model.Action
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
