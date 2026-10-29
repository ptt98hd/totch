package io.github.ptt98hd.totch.core.action

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OneHandedTest {
    private val shortcutKey = "accessibility_shortcut_target_service"
    private val activated = "settings put secure one_handed_mode_activated"

    @Test fun `OH-1 unsupported devices exit 3 without writing settings`() {
        val sim = ShellSim().apply { props.remove("ro.support_one_handed_mode") }
        assertEquals(3, sim.run(OneHandedCommands.system()))
        assertTrue(sim.calls("settings").none { "put" in it })
    }

    @Test fun `OH-1 without the shortcut it is added for the toggle, then restored`() {
        val sim = ShellSim().apply { secure[shortcutKey] = "com.x/.Magnifier" }
        assertEquals(0, sim.run(OneHandedCommands.system()))
        assertEquals(2, sim.wmState)
        assertEquals("com.x/.Magnifier", sim.secure[shortcutKey])
        assertTrue("settings put secure $shortcutKey com.x/.Magnifier:com.android.server.accessibility.OneHandedMode" in sim.log)
        assertEquals(listOf("$activated 0", "$activated 1"), sim.calls("settings").filter { it.startsWith(activated) })

        val unset = ShellSim()
        assertEquals(0, unset.run(OneHandedCommands.system()))
        assertEquals(2, unset.wmState)
        assertFalse(shortcutKey in unset.secure) // "null" before → deleted again
    }

    @Test fun `OH-1 the direction follows the real state and the shortcut is left alone when set`() {
        val sim = ShellSim().apply {
            wmState = 2
            secure["accessibility_button_targets"] = "com.android.server.accessibility.OneHandedMode"
            secure["one_handed_mode_activated"] = "0" // stale: the system does not always write it back
        }
        assertEquals(0, sim.run(OneHandedCommands.system()))
        assertEquals(0, sim.wmState)
        assertTrue(sim.calls("settings").none { it.startsWith("settings put secure $shortcutKey") })
        assertEquals(listOf("$activated 1", "$activated 0"), sim.calls("settings").filter { it.startsWith(activated) })
    }

    @Test fun `OH-1 a toggle without effect exits 2 and still restores the shortcut`() {
        val sim = ShellSim().apply { nativeWorks = false }
        assertEquals(2, sim.run(OneHandedCommands.system()))
        assertEquals(0, sim.wmState)
        assertFalse(shortcutKey in sim.secure)
    }

    @Test fun `OH-1 without a readable state the saved value decides and success is assumed`() {
        val sim = ShellSim().apply {
            wmState = null
            secure["one_handed_mode_activated"] = "1"
        }
        assertEquals(0, sim.run(OneHandedCommands.system()))
        assertEquals("$activated 0", sim.calls("settings").last { it.startsWith(activated) })

        val notifications = ShellSim().apply { secure["swipe_bottom_to_notification_enabled"] = "1" }
        assertEquals(0, notifications.run(OneHandedCommands.system()))
    }

    @Test fun `OH-2 gesture coordinates`() {
        val script = OneHandedCommands.gesture(1080, 2400, 2.5f)
        assertTrue("input swipe 540 2380 540 2500 150" in script)
        assertTrue("input tap 540 360" in script)
    }

    @Test fun `OH-2 the gesture needs gesture navigation and enables the feature`() {
        val script = OneHandedCommands.gesture(1080, 2400, 2.5f)
        val buttons = ShellSim().apply { secure["navigation_mode"] = "0" }
        assertEquals(4, buttons.run(script))
        assertTrue(buttons.calls("input").isEmpty())

        val sim = ShellSim().apply { secure["navigation_mode"] = "2" }
        assertEquals(0, sim.run(script))
        assertEquals("1", sim.secure["one_handed_mode_enabled"])
        assertEquals(listOf("input swipe 540 2380 540 2500 150"), sim.calls("input"))
        assertEquals(2, sim.wmState)

        assertEquals(0, sim.run(script)) // active → tap above the lowered screen
        assertEquals(listOf("input tap 540 360"), sim.calls("input"))
        assertEquals(0, sim.wmState)
    }

    @Test fun `OH-2 failures`() {
        val script = OneHandedCommands.gesture(1080, 2400, 2.5f)
        assertEquals(2, ShellSim().apply { secure["navigation_mode"] = "2"; gestureWorks = false }.run(script))
        assertEquals(3, ShellSim().apply { secure["navigation_mode"] = "2"; props.clear() }.run(script))
        val invalid = ShellSim().apply { secure["navigation_mode"] = "2" }
        assertEquals(2, invalid.run(OneHandedCommands.gesture(0, 2400, 2.5f)))
        assertEquals(2, invalid.run(OneHandedCommands.gesture(1080, 2400, 0f)))
        assertTrue(invalid.log.isEmpty())
    }

    @Test fun `OH-3 the preferred method goes first`() {
        assertEquals(listOf(OneHandedMethod.SYSTEM, OneHandedMethod.GESTURE), OneHandedFallback.order(OneHandedMethod.SYSTEM))
        assertEquals(listOf(OneHandedMethod.GESTURE, OneHandedMethod.SYSTEM), OneHandedFallback.order(OneHandedMethod.GESTURE))
    }

    @Test fun `OH-4 the first method that works is returned`() = runTest {
        val tried = mutableListOf<OneHandedMethod>()
        assertEquals(OneHandedMethod.SYSTEM, OneHandedFallback.run(OneHandedMethod.SYSTEM) { tried += it; true })
        assertEquals(listOf(OneHandedMethod.SYSTEM), tried)

        tried.clear()
        assertEquals(OneHandedMethod.GESTURE, OneHandedFallback.run(OneHandedMethod.SYSTEM) { tried += it; it == OneHandedMethod.GESTURE })
        assertEquals(listOf(OneHandedMethod.SYSTEM, OneHandedMethod.GESTURE), tried)

        assertNull(OneHandedFallback.run(OneHandedMethod.GESTURE) { false })
        assertEquals(
            OneHandedMethod.SYSTEM,
            OneHandedFallback.run(OneHandedMethod.GESTURE) { if (it == OneHandedMethod.GESTURE) error("binder") else true },
        )
        assertFailsWith<CancellationException> { OneHandedFallback.run(OneHandedMethod.SYSTEM) { throw CancellationException() } }
    }
}
