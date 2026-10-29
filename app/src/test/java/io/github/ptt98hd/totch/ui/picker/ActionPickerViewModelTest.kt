package io.github.ptt98hd.totch.ui.picker

import app.cash.turbine.test
import io.github.ptt98hd.totch.ActivityEntry
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.ShortcutEntry
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DefaultBindings
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.ui.FakeAppActivities
import io.github.ptt98hd.totch.ui.FakeAppShortcuts
import io.github.ptt98hd.totch.ui.FakeInstalledApps
import io.github.ptt98hd.totch.ui.FakeSettingsRepository
import io.github.ptt98hd.totch.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActionPickerViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val apps = listOf(
        AppInfo("org.zed", "zebra"),
        AppInfo("com.android.camera", "Camera"),
        AppInfo("com.b", "Browser"),
        AppInfo("org.cam.scanner", "camScanner"),
        AppInfo("com.a", "alarm"),
    )
    private val shortcut = ShortcutEntry("com.b", "Browser", "new_tab", "New tab", "intent:#Intent;component=com.b/.Tab;end")

    private val installed = FakeInstalledApps(apps)
    private val shortcuts = FakeAppShortcuts(listOf(shortcut))
    private val activity = ActivityEntry("com.b", "Browser", "com.b.Settings", "Browser settings")
    private val activities = FakeAppActivities(listOf(activity))

    private fun vm(repo: FakeSettingsRepository = FakeSettingsRepository(), gesture: Gesture = Gesture.SWIPE_RIGHT) =
        ActionPickerViewModel(gesture, repo, installed, shortcuts, activities, main.dispatcher)

    private fun bound(action: Action) = FakeSettingsRepository(Settings(bindings = DefaultBindings + (Gesture.TAP to action)))

    @Test
    fun `UI-20 initial tab follows the current action`() = runTest {
        assertEquals(PickerTab.SYSTEM, vm(bound(Action.Back), Gesture.TAP).state.value.tab)
        assertEquals(PickerTab.APPS, vm(bound(Action.LaunchApp("com.a")), Gesture.TAP).state.value.tab)
        val sc = Action.LaunchShortcut("com.b", "new_tab", "New tab", "intent:#Intent;end")
        assertEquals(PickerTab.SHORTCUTS, vm(bound(sc), Gesture.TAP).state.value.tab)
        val act = Action.LaunchActivity("com.b", "com.b.Settings", "Browser settings")
        assertEquals(PickerTab.ACTIVITIES, vm(bound(act), Gesture.TAP).state.value.tab)
    }

    @Test
    fun `UI-22 apps and shortcuts load only when their tab is opened`() = runTest {
        val vm = vm(bound(Action.Back), Gesture.TAP)
        vm.state.test {
            expectMostRecentItem()
            assertEquals(0, installed.loads)
            assertEquals(0, shortcuts.loads)
            vm.selectTab(PickerTab.APPS)
            assertEquals(5, expectMostRecentItem().apps.size)
            assertEquals(1, installed.loads)
            assertEquals(0, shortcuts.loads)
            vm.selectTab(PickerTab.SHORTCUTS)
            assertEquals(listOf("com.b"), expectMostRecentItem().shortcutGroups.map { it.packageName })
            vm.selectTab(PickerTab.APPS)
            vm.selectTab(PickerTab.SHORTCUTS)
            expectMostRecentItem()
            assertEquals(1, installed.loads)
            assertEquals(1, shortcuts.loads)
        }
    }

    @Test
    fun `UI-11 apps are sorted by label case-insensitively`() = runTest {
        val vm = vm()
        vm.state.test {
            vm.selectTab(PickerTab.APPS)
            val state = expectMostRecentItem()
            assertFalse(state.appsLoading)
            assertEquals(listOf("alarm", "Browser", "Camera", "camScanner", "zebra"), state.apps.map { it.label })
        }
    }

    @Test
    fun `UI-11 filtering by query ignores case, keeps sort order, blank shows all`() = runTest {
        val vm = vm()
        vm.state.test {
            vm.selectTab(PickerTab.APPS)
            vm.setAppQuery("CAM")
            val state = expectMostRecentItem()
            assertEquals("CAM", state.appQuery)
            assertEquals(listOf("Camera", "camScanner"), state.apps.map { it.label })
            vm.setAppQuery("   ")
            assertEquals(5, expectMostRecentItem().apps.size)
            vm.setAppQuery("xyz")
            assertTrue(expectMostRecentItem().apps.isEmpty())
        }
    }

    @Test
    fun `UI-10 choosing a system action saves it, switches the gesture on and finishes`() = runTest {
        val repo = FakeSettingsRepository(Settings(disabledGestures = setOf(Gesture.SWIPE_RIGHT, Gesture.TAP)))
        val vm = vm(repo)
        vm.state.test {
            vm.select(Action.Assistant)
            assertEquals(Settings().bindings + (Gesture.SWIPE_RIGHT to Action.Assistant), repo.current.bindings)
            assertEquals(setOf(Gesture.TAP), repo.current.disabledGestures)
            assertTrue(expectMostRecentItem().finished)
        }
    }

    @Test
    fun `UI-25 shortcut apps start collapsed and expand one by one`() = runTest {
        val vm = vm(FakeSettingsRepository())
        vm.state.test {
            vm.selectTab(PickerTab.SHORTCUTS)
            val loaded = expectMostRecentItem()
            val group = loaded.shortcutGroups.first()
            assertTrue(loaded.expanded.isEmpty())
            assertFalse(loaded.isExpanded(group.packageName))
            vm.toggleExpanded(group.packageName)
            assertTrue(expectMostRecentItem().isExpanded(group.packageName))
            vm.toggleExpanded(group.packageName)
            assertFalse(expectMostRecentItem().isExpanded(group.packageName))
            // While searching, every matching app is shown expanded.
            vm.setShortcutQuery("tab")
            assertTrue(expectMostRecentItem().isExpanded(group.packageName))
        }
    }

    @Test
    fun `UI-10 choosing an app saves LaunchApp`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = vm(repo)
        vm.state.test {
            vm.selectApp(AppInfo("com.android.camera", "Camera"))
            assertEquals(Action.LaunchApp("com.android.camera"), repo.current.bindings[Gesture.SWIPE_RIGHT])
            assertTrue(expectMostRecentItem().finished)
        }
    }

    @Test
    fun `UI-21 choosing a shortcut saves LaunchShortcut`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = vm(repo)
        vm.state.test {
            vm.selectTab(PickerTab.SHORTCUTS)
            vm.setShortcutQuery("browser")
            assertEquals(1, expectMostRecentItem().shortcutGroups.size)
            vm.selectShortcut(shortcut)
            assertEquals(
                Action.LaunchShortcut("com.b", "new_tab", "New tab", "intent:#Intent;component=com.b/.Tab;end"),
                repo.current.bindings[Gesture.SWIPE_RIGHT],
            )
            assertTrue(expectMostRecentItem().finished)
        }
    }

    @Test
    fun `current action is exposed for the radio state`() = runTest {
        vm(bound(Action.Home), Gesture.TAP).state.test {
            assertEquals(Action.Home, expectMostRecentItem().current)
        }
    }

    @Test
    fun `UI-27 activities load once, expand per app and are saved as LaunchActivity`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = vm(repo)
        vm.state.test {
            expectMostRecentItem()
            assertEquals(0, activities.loads)
            vm.selectTab(PickerTab.ACTIVITIES)
            val state = expectMostRecentItem()
            assertFalse(state.activitiesLoading)
            assertEquals(listOf("com.b"), state.activityGroups.map { it.packageName })
            assertFalse(state.isActivityAppExpanded("com.b"))
            vm.toggleActivityApp("com.b")
            val expanded = expectMostRecentItem()
            assertTrue(expanded.isActivityAppExpanded("com.b"))
            assertFalse(expanded.isExpanded("com.b")) // Shortcuts keep their own state
            vm.selectTab(PickerTab.SYSTEM)
            vm.selectTab(PickerTab.ACTIVITIES)
            vm.setActivityQuery("zzz")
            assertTrue(expectMostRecentItem().activityGroups.isEmpty())
            assertEquals(1, activities.loads)
            vm.selectActivity(activity)
            assertTrue(expectMostRecentItem().finished)
        }
        assertEquals(
            Action.LaunchActivity("com.b", "com.b.Settings", "Browser settings"),
            repo.current.bindings[Gesture.SWIPE_RIGHT],
        )
    }
}
