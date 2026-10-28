package io.github.ptt98hd.totch.ui.settings

import app.cash.turbine.test
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.ui.FakeSettingsRepository
import io.github.ptt98hd.totch.ui.FakeTouchArea
import io.github.ptt98hd.totch.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `FR-U4 slider values are persisted and clamped by the repository`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo, FakeTouchArea())
        vm.setDoubleTapMs(9999)
        vm.setLongPressMs(800)
        vm.setSwipeDistanceDp(60)
        vm.setPaddingDp(12)
        vm.setHaptics(false)
        with(repo.current) {
            assertEquals(600, doubleTapMs)
            assertEquals(800, longPressMs)
            assertEquals(60, swipeMinDistanceDp)
            assertEquals(12, paddingDp)
            assertFalse(haptics)
        }
    }

    @Test
    fun `FR-U4 custom touch area toggles the manual region`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo, FakeTouchArea())
        vm.setCustomArea(true)
        assertEquals(ManualRegionDp(), repo.current.manualRegion)
        vm.setManualRegion(ManualRegionDp(0.3f, 120, 40))
        assertEquals(ManualRegionDp(0.3f, 120, 40), repo.current.manualRegion)
        vm.setCustomArea(false)
        assertNull(repo.current.manualRegion)
    }

    @Test
    fun `FR-U4 reset restores defaults but keeps enabled and mode`() = runTest {
        val custom = Settings(
            enabled = true, mode = DetectionMode.SHIZUKU, doubleTapMs = 500, haptics = false,
            bindings = Settings().bindings + (Gesture.TAP to Action.Back),
        )
        val repo = FakeSettingsRepository(custom)
        val vm = SettingsViewModel(repo, FakeTouchArea())
        vm.state.test {
            vm.requestReset()
            assertTrue(expectMostRecentItem().confirmReset)
            vm.confirmReset()
            assertFalse(expectMostRecentItem().confirmReset)
        }
        assertEquals(Settings(enabled = true, mode = DetectionMode.SHIZUKU).sanitized(), repo.current)
    }
}
