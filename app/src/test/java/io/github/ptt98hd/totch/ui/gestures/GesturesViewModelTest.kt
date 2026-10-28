package io.github.ptt98hd.totch.ui.gestures

import app.cash.turbine.test
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.ui.FakeInstalledApps
import io.github.ptt98hd.totch.ui.FakeSettingsRepository
import io.github.ptt98hd.totch.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GesturesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `UI-10 selecting an action updates the binding via repository and closes the sheet`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = GesturesViewModel(repo, FakeInstalledApps())
        vm.state.test {
            vm.openSheet(Gesture.TAP)
            assertEquals(Gesture.TAP, expectMostRecentItem().editingGesture)

            vm.selectAction(Action.Back)

            assertEquals(Action.Back, repo.current.bindings[Gesture.TAP])
            val state = expectMostRecentItem()
            assertNull(state.editingGesture)
            assertEquals(Action.Back, state.rows.single { it.gesture == Gesture.TAP }.action)
        }
    }

    @Test
    fun `UI-10 other bindings are left untouched`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = GesturesViewModel(repo, FakeInstalledApps())
        vm.openSheet(Gesture.SWIPE_LEFT)
        vm.selectAction(Action.VolumeDown)
        assertEquals(Settings().bindings + (Gesture.SWIPE_LEFT to Action.VolumeDown), repo.current.bindings)
    }

    @Test
    fun `UI-10 dismissing the sheet does not change bindings`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = GesturesViewModel(repo, FakeInstalledApps())
        vm.state.test {
            vm.openSheet(Gesture.TAP)
            vm.dismissSheet()
            assertNull(expectMostRecentItem().editingGesture)
        }
        assertEquals(Settings().bindings, repo.current.bindings)
    }

    @Test
    fun `FR-U2 state lists the 6 gestures with their bound action and current mode`() = runTest {
        val repo = FakeSettingsRepository(Settings(mode = DetectionMode.SHIZUKU))
        val vm = GesturesViewModel(repo, FakeInstalledApps())
        vm.state.test {
            val state = expectMostRecentItem()
            assertEquals(Gesture.entries, state.rows.map { it.gesture })
            assertEquals(Action.LockScreen, state.rows.single { it.gesture == Gesture.DOUBLE_TAP }.action)
            assertEquals(DetectionMode.SHIZUKU, state.mode)
        }
    }

    @Test
    fun `UI-12 LaunchApp row shows the app label, or null when the app is not installed`() = runTest {
        val repo = FakeSettingsRepository(
            Settings(
                bindings = Settings().bindings +
                    (Gesture.TAP to Action.LaunchApp("com.example.cam")) +
                    (Gesture.LONG_PRESS to Action.LaunchApp("com.gone.app")),
            ),
        )
        val vm = GesturesViewModel(repo, FakeInstalledApps(listOf(AppInfo("com.example.cam", "Camera"))))
        vm.state.test {
            val rows = expectMostRecentItem().rows
            assertEquals("Camera", rows.single { it.gesture == Gesture.TAP }.appLabel)
            assertNull(rows.single { it.gesture == Gesture.LONG_PRESS }.appLabel)
        }
    }
}
