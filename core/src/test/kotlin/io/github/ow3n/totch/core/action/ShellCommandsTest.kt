package io.github.ow3n.totch.core.action

import io.github.ow3n.totch.core.model.Action
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
            Action.None to null,
            Action.ToggleTorch to null,
            Action.MediaPlayPause to null,
            Action.MediaNext to null,
            Action.MediaPrevious to null,
            Action.VolumeUp to null,
            Action.VolumeDown to null,
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
}
