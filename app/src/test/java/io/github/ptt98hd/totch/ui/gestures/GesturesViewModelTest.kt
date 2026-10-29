package io.github.ptt98hd.totch.ui.gestures

import app.cash.turbine.test
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.ui.FakeInstalledApps
import io.github.ptt98hd.totch.ui.FakeSettingsRepository
import io.github.ptt98hd.totch.ui.LegacyBindings
import io.github.ptt98hd.totch.ui.common.Capabilities
import io.github.ptt98hd.totch.ui.common.Need
import io.github.ptt98hd.totch.ui.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
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
        GesturesViewModel(FakeSettingsRepository(Settings(bindings = LegacyBindings)), FakeInstalledApps()).state.test {
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
        val repo = FakeSettingsRepository(Settings(bindings = LegacyBindings + (Gesture.SWIPE_DOWN_HOLD to Action.Home)))
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
        val repo = FakeSettingsRepository(Settings(bindings = LegacyBindings))
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
        val repo = FakeSettingsRepository(Settings(bindings = LegacyBindings))
        val vm = GesturesViewModel(repo, FakeInstalledApps())
        vm.state.test {
            vm.openPicker(Gesture.TAP)
            assertNull(expectMostRecentItem().picking) // switched off: no picker
            vm.setOn(Gesture.TAP, true)
            assertEquals(Settings(bindings = LegacyBindings), repo.current)
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
                bindings = LegacyBindings +
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

    @Test
    fun `UI-14 swipe down rows are usable while accessibility is on`() = runTest {
        val accessibility = MutableStateFlow(Capabilities())
        val repo = FakeSettingsRepository(Settings(bindings = LegacyBindings + (Gesture.SWIPE_DOWN_HOLD to Action.Home)))
        GesturesViewModel(repo, FakeInstalledApps(), accessibility).state.test {
            val rows = expectMostRecentItem().groups.flatMap { it.rows }
            assertTrue(rows.all { it.enabled })
            assertTrue(rows.single { it.gesture == Gesture.SWIPE_DOWN }.on)
            assertTrue(rows.single { it.gesture == Gesture.SWIPE_DOWN_HOLD }.on)
            accessibility.value = Capabilities(accessibility = false)
            assertFalse(expectMostRecentItem().row(Gesture.SWIPE_DOWN).enabled)
        }
    }

    @Test
    fun `UI-60 every gesture is disabled without Accessibility and Shizuku`() = runTest {
        val caps = MutableStateFlow(Capabilities(accessibility = false, shizuku = false))
        val repo = FakeSettingsRepository(Settings(bindings = LegacyBindings))
        val vm = GesturesViewModel(repo, FakeInstalledApps(), caps)
        vm.state.test {
            val rows = expectMostRecentItem().groups.flatMap { it.rows }
            assertTrue(rows.none { it.enabled || it.on })
            assertTrue(rows.all { it.need == Need.ACCESS })
            vm.openPicker(Gesture.DOUBLE_TAP)
            assertNull(vm.state.value.picking)

            caps.value = Capabilities(accessibility = false, shizuku = true)
            val shizukuOnly = expectMostRecentItem()
            assertEquals(Need.ACCESSIBILITY, shizukuOnly.row(Gesture.SWIPE_DOWN).need)
            assertFalse(shizukuOnly.row(Gesture.SWIPE_DOWN).enabled)
            assertTrue(shizukuOnly.row(Gesture.DOUBLE_TAP).enabled)
            assertTrue(shizukuOnly.row(Gesture.DOUBLE_TAP).on)
            assertNull(shizukuOnly.row(Gesture.DOUBLE_TAP).need)
        }
    }

    @Test
    fun `UI-61 a gesture whose action cannot run is off but still opens the picker`() = runTest {
        val caps = MutableStateFlow(Capabilities(accessibility = true, shizuku = false, dndAccess = false))
        val repo = FakeSettingsRepository(
            Settings(bindings = LegacyBindings + (Gesture.TAP to Action.ToggleWifi) + (Gesture.LONG_PRESS to Action.RingerSilent)),
        )
        val vm = GesturesViewModel(repo, FakeInstalledApps(), caps)
        vm.state.test {
            val state = expectMostRecentItem()
            val wifi = state.row(Gesture.TAP)
            assertTrue(wifi.enabled)
            assertFalse(wifi.on)
            assertEquals(Need.SHIZUKU, wifi.need)
            assertEquals(Need.DND, state.row(Gesture.LONG_PRESS).need)
            assertTrue(state.row(Gesture.DOUBLE_TAP).on)

            vm.setOn(Gesture.TAP, true)
            assertEquals(LegacyBindings + (Gesture.TAP to Action.ToggleWifi) + (Gesture.LONG_PRESS to Action.RingerSilent), repo.current.bindings)
            assertNull(vm.state.value.picking)
            vm.openPicker(Gesture.TAP)
            assertEquals(Gesture.TAP, expectMostRecentItem().picking)

            caps.value = Capabilities()
            val ready = expectMostRecentItem().row(Gesture.TAP)
            assertTrue(ready.on)
            assertNull(ready.need)
        }
    }
}
