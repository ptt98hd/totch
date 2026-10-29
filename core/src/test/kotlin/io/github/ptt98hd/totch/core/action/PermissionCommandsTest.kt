package io.github.ptt98hd.totch.core.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PermissionCommandsTest {
    private val totch = "io.github.ptt98hd.totch/io.github.ptt98hd.totch.accessibility.NotchAccessibilityService"
    private val totchShort = "io.github.ptt98hd.totch/.accessibility.NotchAccessibilityService"
    private val other = "com.other/com.other.Svc"

    @Test fun `PC-1 accessibility keeps other services`() {
        assertEquals(
            listOf("settings put secure enabled_accessibility_services '$other:$totch'", "settings put secure accessibility_enabled 1"),
            PermissionCommands.accessibility(other, totch, enabled = true),
        )
        assertEquals(
            listOf("settings put secure enabled_accessibility_services '$totch'", "settings put secure accessibility_enabled 1"),
            PermissionCommands.accessibility(null, totch, enabled = true),
        )
        // Already enabled under its short name: not added twice.
        assertEquals(
            listOf("settings put secure enabled_accessibility_services '$other:$totch'", "settings put secure accessibility_enabled 1"),
            PermissionCommands.accessibility("$totchShort:$other", totch, enabled = true),
        )
        assertEquals(
            listOf("settings put secure enabled_accessibility_services '$other'"),
            PermissionCommands.accessibility("$totchShort:$other", totch, enabled = false),
        )
        assertEquals(
            listOf("settings delete secure enabled_accessibility_services"),
            PermissionCommands.accessibility(totch, totch, enabled = false),
        )
        assertEquals(
            listOf("settings delete secure enabled_accessibility_services"),
            PermissionCommands.accessibility("null", totch, enabled = false),
        )
    }

    @Test fun `PC-2 notification listener`() {
        assertEquals("cmd notification allow_listener 'a.b/a.b.L'", PermissionCommands.notificationListener("a.b/a.b.L", true))
        assertEquals("cmd notification disallow_listener 'a.b/a.b.L'", PermissionCommands.notificationListener("a.b/a.b.L", false))
    }

    @Test fun `PC-3 battery`() {
        assertEquals("cmd deviceidle whitelist +'a.b'", PermissionCommands.batteryUnrestricted("a.b", true))
        assertEquals("cmd deviceidle whitelist -'a.b'", PermissionCommands.batteryUnrestricted("a.b", false))
    }

    @Test fun `PC-4 notifications grant and quoting`() {
        assertEquals("pm grant 'a.b' android.permission.POST_NOTIFICATIONS", PermissionCommands.grantNotifications("a.b"))
        assertEquals("cmd deviceidle whitelist +'a'\\''b'", PermissionCommands.batteryUnrestricted("a'b", true))
    }

    @Test fun `PC-5 notifications revoke runs detached and reopens the app`() {
        assertEquals(
            "setsid sh -c 'sleep 1; pm revoke a.b android.permission.POST_NOTIFICATIONS; am start -n a.b/a.b.ui.Main' " +
                "</dev/null >/dev/null 2>&1 &",
            PermissionCommands.revokeNotificationsAndRestart("a.b", "a.b.ui.Main"),
        )
        assertNull(PermissionCommands.revokeNotificationsAndRestart("a.b; reboot", "a.b.Main"))
        assertNull(PermissionCommands.revokeNotificationsAndRestart("a.b", "Main'x"))
    }

    @Test fun `PC-6 install packages allow`() {
        assertEquals("appops set 'a.b' REQUEST_INSTALL_PACKAGES allow", PermissionCommands.allowInstallPackages("a.b"))
    }

    @Test fun `PC-7 install packages deny runs detached and reopens the app`() {
        assertEquals(
            "setsid sh -c 'sleep 1; appops set a.b REQUEST_INSTALL_PACKAGES deny; am start -n a.b/a.b.ui.Main' " +
                "</dev/null >/dev/null 2>&1 &",
            PermissionCommands.denyInstallPackagesAndRestart("a.b", "a.b.ui.Main"),
        )
        assertNull(PermissionCommands.denyInstallPackagesAndRestart("a.b; reboot", "a.b.Main"))
        assertNull(PermissionCommands.denyInstallPackagesAndRestart("a.b", "Main'x"))
    }

    @Test fun `PC-8 do not disturb access`() {
        assertEquals("cmd notification allow_dnd 'a.b'", PermissionCommands.dndAccess("a.b", true))
        assertEquals("cmd notification disallow_dnd 'a.b'", PermissionCommands.dndAccess("a.b", false))
    }
}
