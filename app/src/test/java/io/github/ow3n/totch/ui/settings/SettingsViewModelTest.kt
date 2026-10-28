package io.github.ow3n.totch.ui.settings

import app.cash.turbine.test
import io.github.ow3n.totch.core.model.Action
import io.github.ow3n.totch.core.model.Gesture
import io.github.ow3n.totch.core.model.Rect
import io.github.ow3n.totch.core.settings.DetectionMode
import io.github.ow3n.totch.core.settings.ManualRegionDp
import io.github.ow3n.totch.core.settings.Settings
import io.github.ow3n.totch.ui.FakeSettingsRepository
import io.github.ow3n.totch.ui.FakeTouchAreaPreview
import io.github.ow3n.totch.ui.MainDispatcherRule
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
        val vm = SettingsViewModel(repo, FakeTouchAreaPreview())
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
        val vm = SettingsViewModel(repo, FakeTouchAreaPreview())
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
        val vm = SettingsViewModel(repo, FakeTouchAreaPreview())
        vm.state.test {
            vm.requestReset()
            assertTrue(expectMostRecentItem().confirmReset)
            vm.confirmReset()
            assertFalse(expectMostRecentItem().confirmReset)
        }
        assertEquals(Settings(enabled = true, mode = DetectionMode.SHIZUKU).sanitized(), repo.current)
    }

    @Test
    fun `FR-U4 preview falls back to the in-app drawing when the overlay is unavailable`() = runTest {
        val region = Rect(500, 0, 580, 60)
        val preview = FakeTouchAreaPreview(region, canShow = false)
        val vm = SettingsViewModel(FakeSettingsRepository(), preview)
        vm.state.test {
            vm.preview()
            val state = expectMostRecentItem()
            assertEquals(1, preview.showCalls)
            assertTrue(state.showInAppPreview)
            assertEquals(region, state.previewRegion)
            vm.dismissPreview()
            assertFalse(expectMostRecentItem().showInAppPreview)
        }
    }

    @Test
    fun `FR-U4 preview uses the overlay when available`() = runTest {
        val preview = FakeTouchAreaPreview(Rect(0, 0, 1, 1), canShow = true)
        val vm = SettingsViewModel(FakeSettingsRepository(), preview)
        vm.state.test {
            vm.preview()
            assertEquals(1, preview.showCalls)
            assertFalse(expectMostRecentItem().showInAppPreview)
        }
    }
}
