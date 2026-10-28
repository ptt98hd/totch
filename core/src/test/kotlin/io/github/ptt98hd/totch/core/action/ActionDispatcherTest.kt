package io.github.ptt98hd.totch.core.action

import io.github.ptt98hd.totch.core.model.Action
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
            Action.MediaPrevious, Action.VolumeUp, Action.VolumeDown,
        )
        local.forEach { assertEquals(setOf(Capability.LOCAL), ActionRequirements.of(it)) }
    }

    @Test fun `AD-7 assistant and shortcuts run on accessibility or shizuku`() {
        val system = setOf(Capability.ACCESSIBILITY, Capability.SHIZUKU)
        assertEquals(system, ActionRequirements.of(Action.Assistant))
        assertEquals(system, ActionRequirements.of(Action.LaunchShortcut("com.a", "id", "L", "intent:#Intent;end")))
    }
}
