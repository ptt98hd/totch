package io.github.ow3n.totch.ui.gestures

import app.cash.turbine.test
import io.github.ow3n.totch.AppInfo
import io.github.ow3n.totch.core.model.Action
import io.github.ow3n.totch.core.model.Gesture
import io.github.ow3n.totch.ui.FakeInstalledApps
import io.github.ow3n.totch.ui.FakeSettingsRepository
import io.github.ow3n.totch.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPickerViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val apps = listOf(
        AppInfo("org.zed", "zebra"),
        AppInfo("com.android.camera", "Camera"),
        AppInfo("com.b", "Browser"),
        AppInfo("org.cam.scanner", "camScanner"),
        AppInfo("com.a", "alarm"),
    )

    private fun vm(repo: FakeSettingsRepository = FakeSettingsRepository()) =
        AppPickerViewModel(Gesture.SWIPE_RIGHT, repo, FakeInstalledApps(apps), main.dispatcher)

    @Test
    fun `UI-11 apps are sorted by label case-insensitively`() = runTest {
        vm().state.test {
            val state = expectMostRecentItem()
            assertFalse(state.loading)
            assertEquals(listOf("alarm", "Browser", "Camera", "camScanner", "zebra"), state.apps.map { it.label })
        }
    }

    @Test
    fun `UI-11 filtering by query ignores case and keeps sort order`() = runTest {
        val vm = vm()
        vm.state.test {
            vm.setQuery("CAM")
            val state = expectMostRecentItem()
            assertEquals("CAM", state.query)
            assertEquals(listOf("Camera", "camScanner"), state.apps.map { it.label })
        }
    }

    @Test
    fun `UI-11 blank query shows every app`() = runTest {
        val vm = vm()
        vm.state.test {
            vm.setQuery("zeb")
            vm.setQuery("   ")
            assertEquals(5, expectMostRecentItem().apps.size)
        }
    }

    @Test
    fun `UI-11 query without matches yields an empty list`() = runTest {
        val vm = vm()
        vm.state.test {
            vm.setQuery("xyz")
            assertTrue(expectMostRecentItem().apps.isEmpty())
        }
    }

    @Test
    fun `FR-U3 choosing an app saves LaunchApp for the gesture and finishes`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = vm(repo)
        vm.state.test {
            vm.select(AppInfo("com.android.camera", "Camera"))
            assertEquals(Action.LaunchApp("com.android.camera"), repo.current.bindings[Gesture.SWIPE_RIGHT])
            assertTrue(expectMostRecentItem().finished)
        }
    }
}
