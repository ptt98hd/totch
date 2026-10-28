package io.github.ptt98hd.totch.ui.settings

import app.cash.turbine.test
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.AppLanguage
import io.github.ptt98hd.totch.core.settings.DefaultBindings
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.ThemeMode
import io.github.ptt98hd.totch.ui.FakeSettingsRepository
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

    private val custom = Settings(
        enabled = true,
        mode = DetectionMode.SHIZUKU,
        bindings = DefaultBindings + (Gesture.TAP to Action.Back),
        repeatOnHold = setOf(Gesture.LONG_PRESS),
        doubleTapMs = 200, longPressMs = 900, swipeMinDistanceDp = 100, paddingDp = 0,
        manualRegion = ManualRegionDp(0.2f, 50, 20), haptics = false, repeatIntervalMs = 500,
        theme = ThemeMode.DARK, amoledBlack = true, language = AppLanguage.VIETNAMESE, startOnBoot = false,
    )

    @Test
    fun `FR-U4 slider values are persisted and clamped by the repository`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
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
        val vm = SettingsViewModel(repo)
        vm.setCustomArea(true)
        assertEquals(ManualRegionDp(), repo.current.manualRegion)
        vm.setManualRegion(ManualRegionDp(0.3f, 120, 40))
        assertEquals(ManualRegionDp(0.3f, 120, 40), repo.current.manualRegion)
        vm.setCustomArea(false)
        assertNull(repo.current.manualRegion)
    }

    @Test
    fun `UI-32 appearance, language, boot and repeat interval setters persist`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        vm.setTheme(ThemeMode.LIGHT)
        vm.setAmoledBlack(true)
        vm.setLanguage(AppLanguage.ENGLISH)
        vm.setStartOnBoot(false)
        vm.setRepeatIntervalMs(450)
        with(repo.current) {
            assertEquals(ThemeMode.LIGHT, theme)
            assertTrue(amoledBlack)
            assertEquals(AppLanguage.ENGLISH, language)
            assertFalse(startOnBoot)
            assertEquals(450, repeatIntervalMs)
        }
    }

    private val areaItems = setOf(SettingItem.CUSTOM_AREA, SettingItem.AREA_X, SettingItem.AREA_WIDTH, SettingItem.AREA_HEIGHT)

    /** Resetting the custom area clears the whole manual region, so its sub-items become default too. */
    private fun SettingItem.sharesFieldWith(other: SettingItem) =
        this == SettingItem.CUSTOM_AREA && other in areaItems

    @Test
    fun `UI-30 each item resets only its own field`() = runTest {
        val defaults = Settings()
        SettingItem.entries.forEach { item ->
            assertFalse(item.isDefault(custom), "$item")
            val reset = item.reset(custom)
            assertTrue(item.isDefault(reset), "$item")
            SettingItem.entries.filter { it != item && !item.sharesFieldWith(it) }
                .forEach { other -> assertFalse(other.isDefault(reset), "$item reset $other") }
            assertTrue(item.isDefault(defaults), "$item")
        }
        assertNull(SettingItem.CUSTOM_AREA.reset(custom).manualRegion)
        assertEquals(ManualRegionDp(0.5f, 50, 20), SettingItem.AREA_X.reset(custom).manualRegion)

        val repo = FakeSettingsRepository(custom)
        SettingsViewModel(repo).reset(SettingItem.THEME)
        assertEquals(custom.copy(theme = ThemeMode.SYSTEM), repo.current)
    }

    @Test
    fun `UI-31 reset all restores preferences but keeps gestures, switch and mode`() = runTest {
        val repo = FakeSettingsRepository(custom)
        val vm = SettingsViewModel(repo)
        vm.state.test {
            vm.requestReset()
            assertTrue(expectMostRecentItem().confirmReset)
            vm.cancelReset()
            assertFalse(expectMostRecentItem().confirmReset)
            assertEquals(custom, repo.current)
            vm.requestReset()
            vm.confirmReset()
            assertFalse(expectMostRecentItem().confirmReset)
        }
        assertEquals(
            Settings(enabled = true, mode = DetectionMode.SHIZUKU, bindings = custom.bindings, repeatOnHold = custom.repeatOnHold),
            repo.current,
        )
    }
}
