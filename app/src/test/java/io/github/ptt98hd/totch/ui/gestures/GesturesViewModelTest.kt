package io.github.ptt98hd.totch.ui.gestures

import app.cash.turbine.test
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DefaultBindings
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.ui.FakeInstalledApps
import io.github.ptt98hd.totch.ui.FakeSettingsRepository
import io.github.ptt98hd.totch.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GesturesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun GesturesUiState.row(g: Gesture) = groups.flatMap { it.rows }.single { it.gesture == g }

    @Test
    fun `UI-13 gestures are listed in four groups in order`() = runTest {
        GesturesViewModel(FakeSettingsRepository(), FakeInstalledApps()).state.test {
            val state = expectMostRecentItem()
            assertEquals(
                listOf(
                    GestureGroup.TAP to listOf(Gesture.TAP, Gesture.DOUBLE_TAP),
                    GestureGroup.PRESS to listOf(Gesture.LONG_PRESS),
                    GestureGroup.SWIPE to listOf(Gesture.SWIPE_LEFT, Gesture.SWIPE_RIGHT, Gesture.SWIPE_DOWN),
                    GestureGroup.SWIPE_HOLD to listOf(Gesture.SWIPE_LEFT_HOLD, Gesture.SWIPE_RIGHT_HOLD, Gesture.SWIPE_DOWN_HOLD),
                ),
                state.groups.map { section -> section.group to section.rows.map { it.gesture } },
            )
            assertEquals(Action.LockScreen, state.row(Gesture.DOUBLE_TAP).action)
        }
    }

    @Test
    fun `UI-14 swipe down rows are disabled and switched off`() = runTest {
        val repo = FakeSettingsRepository(Settings(bindings = DefaultBindings + (Gesture.SWIPE_DOWN_HOLD to Action.Home)))
        GesturesViewModel(repo, FakeInstalledApps()).state.test {
            val rows = expectMostRecentItem().groups.flatMap { it.rows }
            val disabled = rows.filterNot { it.enabled }
            assertEquals(setOf(Gesture.SWIPE_DOWN, Gesture.SWIPE_DOWN_HOLD), disabled.map { it.gesture }.toSet())
            assertTrue(disabled.none { it.on })
            assertEquals(Action.Notifications, disabled.single { it.gesture == Gesture.SWIPE_DOWN }.action)
        }
    }

    @Test
    fun `UI-16 switching a gesture off keeps its action`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = GesturesViewModel(repo, FakeInstalledApps())
        vm.state.test {
            val state = expectMostRecentItem()
            assertTrue(state.row(Gesture.DOUBLE_TAP).on)
            assertFalse(state.row(Gesture.TAP).on) // bound to None
            vm.setOn(Gesture.DOUBLE_TAP, false)
            assertEquals(setOf(Gesture.DOUBLE_TAP), repo.current.disabledGestures)
            assertEquals(Action.LockScreen, repo.current.bindings[Gesture.DOUBLE_TAP])
            val off = expectMostRecentItem().row(Gesture.DOUBLE_TAP)
            assertFalse(off.on)
            assertEquals(Action.LockScreen, off.action)
            vm.setOn(Gesture.DOUBLE_TAP, true)
            assertEquals(emptySet(), repo.current.disabledGestures)
            assertTrue(expectMostRecentItem().row(Gesture.DOUBLE_TAP).on)
        }
    }

    @Test
    fun `UI-17 switching on an unbound gesture opens the picker instead`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = GesturesViewModel(repo, FakeInstalledApps())
        vm.state.test {
            vm.openPicker(Gesture.TAP)
            assertNull(expectMostRecentItem().picking) // switched off: no picker
            vm.setOn(Gesture.TAP, true)
            assertEquals(Settings(), repo.current)
            assertEquals(Gesture.TAP, expectMostRecentItem().picking)
            vm.closePicker()
            assertNull(expectMostRecentItem().picking)
            vm.openPicker(Gesture.DOUBLE_TAP)
            assertEquals(Gesture.DOUBLE_TAP, expectMostRecentItem().picking)
        }
    }

    @Test
    fun `UI-12 app label for LaunchApp and LaunchShortcut, null when not installed`() = runTest {
        val repo = FakeSettingsRepository(
            Settings(
                bindings = DefaultBindings +
                    (Gesture.TAP to Action.LaunchApp("com.example.cam")) +
                    (Gesture.LONG_PRESS to Action.LaunchApp("com.gone.app")) +
                    (Gesture.SWIPE_LEFT to Action.LaunchShortcut("com.example.cam", "s", "Selfie", "intent:#Intent;end")),
            ),
        )
        val vm = GesturesViewModel(repo, FakeInstalledApps(listOf(AppInfo("com.example.cam", "Camera"))))
        vm.state.test {
            val state = expectMostRecentItem()
            assertEquals("Camera", state.row(Gesture.TAP).appLabel)
            assertNull(state.row(Gesture.LONG_PRESS).appLabel)
            assertEquals("Camera", state.row(Gesture.SWIPE_LEFT).appLabel)
        }
    }
}
