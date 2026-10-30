package io.github.ptt98hd.totch.core.action

import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.IntentTarget
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionDispatcherTest {
    private class FakeExecutor(
        override val capability: Capability,
        var available: Boolean = true,
        var supported: Boolean = true,
        val result: () -> Result<Unit> = { Result.success(Unit) },
    ) : ActionExecutor {
        val executed = mutableListOf<Action>()
        override fun isAvailable() = available
        override fun supports(action: Action) = supported
        override suspend fun execute(action: Action): Result<Unit> {
            executed += action
            return result()
        }
    }

    @Test fun `AD-1 None is skipped without calling executors`() = runTest {
        val all = Capability.entries.map { FakeExecutor(it) }
        assertEquals(DispatchResult.Skipped, ActionDispatcher(all).dispatch(Action.None))
        assertTrue(all.all { it.executed.isEmpty() })
    }

    @Test fun `AD-2 accessibility preferred over shizuku`() = runTest {
        val shizuku = FakeExecutor(Capability.SHIZUKU)
        val a11y = FakeExecutor(Capability.ACCESSIBILITY)
        assertEquals(DispatchResult.Ok, ActionDispatcher(listOf(shizuku, a11y)).dispatch(Action.Back))
        assertEquals(listOf<Action>(Action.Back), a11y.executed)
        assertTrue(shizuku.executed.isEmpty())
    }

    @Test fun `AD-3 falls back to shizuku when accessibility unavailable`() = runTest {
        val shizuku = FakeExecutor(Capability.SHIZUKU)
        val a11y = FakeExecutor(Capability.ACCESSIBILITY, available = false)
        assertEquals(DispatchResult.Ok, ActionDispatcher(listOf(a11y, shizuku)).dispatch(Action.Back))
        assertEquals(listOf<Action>(Action.Back), shizuku.executed)
        assertTrue(a11y.executed.isEmpty())
    }

    @Test fun `AD-4 local action without local executor fails as unavailable`() = runTest {
        val shizuku = FakeExecutor(Capability.SHIZUKU)
        assertEquals(DispatchResult.Failed("unavailable"), ActionDispatcher(listOf(shizuku)).dispatch(Action.ToggleTorch))
        assertTrue(shizuku.executed.isEmpty())
    }

    @Test fun `AD-5 executor failure becomes Failed and is not retried`() = runTest {
        val throwing = FakeExecutor(Capability.ACCESSIBILITY, result = { throw IllegalStateException("boom") })
        val shizuku = FakeExecutor(Capability.SHIZUKU)
        assertEquals(DispatchResult.Failed("boom"), ActionDispatcher(listOf(throwing, shizuku)).dispatch(Action.Home))
        assertTrue(shizuku.executed.isEmpty())

        val failing = FakeExecutor(Capability.LOCAL, result = { Result.failure(RuntimeException()) })
        assertEquals(DispatchResult.Failed("error"), ActionDispatcher(listOf(failing)).dispatch(Action.VolumeUp))
    }

    @Test fun `AD-6 executor not supporting the action is skipped`() = runTest {
        val a11y = FakeExecutor(Capability.ACCESSIBILITY, supported = false)
        val shizuku = FakeExecutor(Capability.SHIZUKU)
        val launch = Action.LaunchApp("com.example")
        assertEquals(DispatchResult.Ok, ActionDispatcher(listOf(a11y, shizuku)).dispatch(launch))
        assertEquals(listOf<Action>(launch), shizuku.executed)
        assertTrue(a11y.executed.isEmpty())
    }

    @Test fun `requirements matrix`() {
        assertEquals(emptySet(), ActionRequirements.of(Action.None))
        val system = listOf(
            Action.Back, Action.Home, Action.Recents, Action.LockScreen, Action.Screenshot,
            Action.Notifications, Action.QuickSettings, Action.LaunchApp("a.b"),
        )
        system.forEach { assertEquals(setOf(Capability.ACCESSIBILITY, Capability.SHIZUKU), ActionRequirements.of(it)) }
        val local = listOf(
            Action.ToggleTorch, Action.MediaPlayPause, Action.MediaNext,
            Action.MediaPrevious, Action.VolumeUp, Action.VolumeDown, Action.VolumePanel,
        )
        local.forEach { assertEquals(setOf(Capability.LOCAL), ActionRequirements.of(it)) }
    }

    @Test fun `AD-7 assistant and shortcuts run on accessibility or shizuku`() {
        val system = setOf(Capability.ACCESSIBILITY, Capability.SHIZUKU)
        assertEquals(system, ActionRequirements.of(Action.Assistant))
        assertEquals(system, ActionRequirements.of(Action.LaunchShortcut("com.a", "id", "L", "intent:#Intent;end")))
    }

    @Test fun `AD-8 volume panel runs locally`() {
        assertEquals(setOf(Capability.LOCAL), ActionRequirements.of(Action.VolumePanel))
    }

    @Test fun `AD-9 activities run on accessibility or shizuku`() {
        assertEquals(
            setOf(Capability.ACCESSIBILITY, Capability.SHIZUKU),
            ActionRequirements.of(Action.LaunchActivity("com.a", "com.a.Main", "Main")),
        )
    }

    @Test fun `AD-10 quick settings tiles run on shizuku only`() = runTest {
        val tile = Action.ClickTile("com.a", "com.a.Tile", "Tile")
        assertEquals(setOf(Capability.SHIZUKU), ActionRequirements.of(tile))
        assertEquals(setOf(Capability.SHIZUKU), ActionRequirements.of(Action.OneHandedMode)) // AD-11
        val a11y = FakeExecutor(Capability.ACCESSIBILITY)
        assertEquals(DispatchResult.Failed("unavailable"), ActionDispatcher(listOf(a11y)).dispatch(tile))
        assertTrue(a11y.executed.isEmpty())
    }

    @Test fun `AD-12 display and connection actions need Shizuku, sound modes run locally`() {
        listOf(
            Action.BrightnessUp, Action.BrightnessDown, Action.ToggleAutoBrightness, Action.ToggleAutoRotate,
            Action.ToggleWifi, Action.ToggleBluetooth, Action.ToggleMobileData, Action.ToggleAirplaneMode,
            Action.ToggleLocation,
        ).forEach { assertEquals(setOf(Capability.SHIZUKU), ActionRequirements.of(it), "$it") }
        listOf(Action.RingerNormal, Action.RingerSilent, Action.CycleRingerMode, Action.ToggleDnd)
            .forEach { assertEquals(setOf(Capability.LOCAL), ActionRequirements.of(it), "$it") }
    }

    @Test fun `AD-13 settings, contacts and activity intents need a system backend, broadcasts run locally`() {
        val system = setOf(Capability.ACCESSIBILITY, Capability.SHIZUKU)
        assertEquals(system, ActionRequirements.of(Action.OpenSettings("android.settings.WIFI_SETTINGS")))
        assertEquals(system, ActionRequirements.of(Action.OpenContact("content://com.android.contacts/contacts/lookup/k/1", "Ann")))
        assertEquals(system, ActionRequirements.of(Action.SendIntent("com.a", "com.a.Main", "com.a.OPEN", IntentTarget.ACTIVITY)))
        assertEquals(
            setOf(Capability.LOCAL),
            ActionRequirements.of(Action.SendIntent("com.a", "com.a.Receiver", "com.a.PING", IntentTarget.BROADCAST)),
        )
    }
}
