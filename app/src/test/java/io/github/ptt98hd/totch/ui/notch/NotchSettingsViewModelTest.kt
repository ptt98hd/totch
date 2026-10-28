package io.github.ptt98hd.totch.ui.notch

import app.cash.turbine.test
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

class NotchSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val auto = ManualRegionDp(0.5f, widthDp = 40, heightDp = 40, topDp = 0)
    private val area = ManualRegionDp(0.2f, widthDp = 50, heightDp = 20, topDp = 10)
    private val custom = Settings(
        doubleTapMs = 200, longPressMs = 900, swipeMinDistanceDp = 100,
        paddingDp = 0, manualRegion = area, haptics = false,
    )

    private fun NotchSetting.isArea() = name.startsWith("AREA_")

    @Test
    fun `UI-30 notch page has exactly the sliders`() {
        assertEquals(
            listOf("DOUBLE_TAP", "LONG_PRESS", "SWIPE_DISTANCE", "AREA_X", "AREA_Y", "AREA_WIDTH", "AREA_HEIGHT", "PADDING"),
            NotchSetting.entries.map { it.name },
        )
    }

    @Test
    fun `UI-30 each slider resets only its own field`() {
        NotchSetting.entries.forEach { item ->
            assertFalse(item.isDefault(custom, auto), "$item")
            val reset = item.reset(custom, auto)
            assertTrue(item.isDefault(reset, auto), "$item")
            NotchSetting.entries.filter { it != item }
                .forEach { other -> assertFalse(other.isDefault(reset, auto), "$item reset $other") }
            assertTrue(item.isDefault(Settings(), auto), "$item")
        }
        assertEquals(area.copy(centerXFraction = 0.5f), NotchSetting.AREA_X.reset(custom, auto).manualRegion)
        assertEquals(area.copy(topDp = 0), NotchSetting.AREA_Y.reset(custom, auto).manualRegion)
        assertEquals(area.copy(widthDp = 40), NotchSetting.AREA_WIDTH.reset(custom, auto).manualRegion)
        assertEquals(area.copy(heightDp = 40), NotchSetting.AREA_HEIGHT.reset(custom, auto).manualRegion)
    }

    @Test
    fun `UI-31 resetting the last custom area field returns to the detected region`() {
        val s = Settings(manualRegion = auto.copy(widthDp = 120))
        assertNull(NotchSetting.AREA_WIDTH.reset(s, auto).manualRegion)
        NotchSetting.entries.filter { it.isArea() }.forEach { assertTrue(it.isDefault(Settings(), auto), "$it") }
    }

    @Test
    fun `UI-33 area falls back to the detected region, then to the default`() = runTest {
        val repo = FakeSettingsRepository()
        val touchArea = FakeTouchArea(detected = auto)
        val vm = NotchSettingsViewModel(repo, touchArea)
        vm.state.test {
            assertEquals(auto, expectMostRecentItem().area)
            touchArea.detectedRegion.value = null
            assertEquals(ManualRegionDp(), expectMostRecentItem().area)
            touchArea.detectedRegion.value = auto
            vm.setArea(auto.copy(topDp = 24))
            assertEquals(auto.copy(topDp = 24), expectMostRecentItem().area)
        }
        assertEquals(auto.copy(topDp = 24), repo.current.manualRegion)
        vm.setArea(auto)
        assertNull(repo.current.manualRegion)
    }

    @Test
    fun `UI-30 reset goes through the repository`() = runTest {
        val repo = FakeSettingsRepository(custom)
        val vm = NotchSettingsViewModel(repo, FakeTouchArea(detected = auto))
        vm.state.test {
            expectMostRecentItem()
            vm.reset(NotchSetting.AREA_Y)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(custom.copy(manualRegion = area.copy(topDp = 0)), repo.current)
    }

    @Test
    fun `UI-34 sensitivity, padding and haptics are persisted and clamped`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = NotchSettingsViewModel(repo, FakeTouchArea())
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
}
