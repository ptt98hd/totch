package io.github.ptt98hd.totch.ui.gestures

import app.cash.turbine.test
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DefaultBindings
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.HoldGestures
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
    fun `UI-14 swipe down rows are disabled in Shizuku mode only`() = runTest {
        val repo = FakeSettingsRepository(Settings(mode = DetectionMode.SHIZUKU))
        GesturesViewModel(repo, FakeInstalledApps()).state.test {
            val state = expectMostRecentItem()
            assertEquals(DetectionMode.SHIZUKU, state.mode)
            val disabled = state.groups.flatMap { it.rows }.filterNot { it.enabled }.map { it.gesture }.toSet()
            assertEquals(setOf(Gesture.SWIPE_DOWN, Gesture.SWIPE_DOWN_HOLD), disabled)
            repo.update { it.copy(mode = DetectionMode.ACCESSIBILITY) }
            assertTrue(expectMostRecentItem().groups.flatMap { it.rows }.all { it.enabled })
        }
    }

    @Test
    fun `UI-15 only hold gestures are repeatable and setRepeat updates repeatOnHold`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = GesturesViewModel(repo, FakeInstalledApps())
        vm.state.test {
            val rows = expectMostRecentItem().groups.flatMap { it.rows }
            assertEquals(HoldGestures, rows.filter { it.repeatable }.map { it.gesture }.toSet())
            vm.setRepeat(Gesture.SWIPE_RIGHT_HOLD, true)
            assertEquals(setOf(Gesture.SWIPE_RIGHT_HOLD), repo.current.repeatOnHold)
            assertTrue(expectMostRecentItem().row(Gesture.SWIPE_RIGHT_HOLD).repeat)
            vm.setRepeat(Gesture.SWIPE_RIGHT_HOLD, false)
            assertEquals(emptySet(), repo.current.repeatOnHold)
            assertFalse(expectMostRecentItem().row(Gesture.SWIPE_RIGHT_HOLD).repeat)
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
