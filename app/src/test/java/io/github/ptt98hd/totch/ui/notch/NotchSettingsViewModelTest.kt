package io.github.ptt98hd.totch.ui.notch

import app.cash.turbine.test
import io.github.ptt98hd.totch.Requirements
import io.github.ptt98hd.totch.core.overlay.IslandActivity
import io.github.ptt98hd.totch.core.settings.NotchRegionPx
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.ui.FakeSettingsRepository
import io.github.ptt98hd.totch.ui.FakeTouchArea
import io.github.ptt98hd.totch.ui.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotchSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val auto = NotchRegionPx(540, widthPx = 80, heightPx = 80, topPx = 0)
    private val area = NotchRegionPx(216, widthPx = 41, heightPx = 39, topPx = 30)
    private val custom = Settings(paddingPx = 3, notchRegion = area, haptics = false)

    private fun NotchSetting.isArea() = name.startsWith("AREA_")

    @Test
    fun `UI-85 notch page has exactly the px sliders`() {
        assertEquals(
            listOf("AREA_X", "AREA_Y", "AREA_WIDTH", "AREA_HEIGHT", "PADDING"),
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
        assertEquals(area.copy(centerXPx = 540), NotchSetting.AREA_X.reset(custom, auto).notchRegion)
        assertEquals(area.copy(topPx = 0), NotchSetting.AREA_Y.reset(custom, auto).notchRegion)
        assertEquals(area.copy(widthPx = 80), NotchSetting.AREA_WIDTH.reset(custom, auto).notchRegion)
        assertEquals(area.copy(heightPx = 80), NotchSetting.AREA_HEIGHT.reset(custom, auto).notchRegion)
        assertEquals(24, NotchSetting.PADDING.reset(custom, auto).paddingPx)
    }

    @Test
    fun `UI-31 resetting the last custom area field returns to the detected region`() {
        val s = Settings(notchRegion = auto.copy(widthPx = 120))
        assertNull(NotchSetting.AREA_WIDTH.reset(s, auto).notchRegion)
        NotchSetting.entries.filter { it.isArea() }.forEach { assertTrue(it.isDefault(Settings(), auto), "$it") }
    }

    @Test
    fun `UI-86 area falls back to the detected region, then to the default, in px`() = runTest {
        val repo = FakeSettingsRepository()
        val touchArea = FakeTouchArea(detected = auto, screenWidth = 1440)
        val vm = NotchSettingsViewModel(repo, touchArea)
        vm.state.test {
            assertEquals(auto, expectMostRecentItem().area)
            touchArea.detectedRegion.value = null
            val fallback = expectMostRecentItem()
            assertEquals(NotchRegionPx(720, 80, 80, 0), fallback.area)
            assertEquals(1440, fallback.screenWidthPx)
            touchArea.detectedRegion.value = auto
            vm.setArea(auto.copy(topPx = 31))
            assertEquals(auto.copy(topPx = 31), expectMostRecentItem().area)
        }
        assertEquals(auto.copy(topPx = 31), repo.current.notchRegion)
        vm.setArea(auto)
        assertNull(repo.current.notchRegion)
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
        assertEquals(custom.copy(notchRegion = area.copy(topPx = 0)), repo.current)
    }

    @Test
    fun `UI-86 padding is persisted in px and clamped`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = NotchSettingsViewModel(repo, FakeTouchArea())
        vm.setPaddingPx(0)
        assertEquals(0, repo.current.paddingPx)
        vm.setPaddingPx(999)
        assertEquals(60, repo.current.paddingPx)
    }

    @Test
    fun `UI-78 overlay switches are persisted`() = runTest {
        val repo = FakeSettingsRepository(custom)
        val vm = NotchSettingsViewModel(repo, FakeTouchArea())
        vm.setIsland(true)
        vm.setIslandActivity(IslandActivity.CHARGING, false)
        vm.setTouchEffects(true)
        assertEquals(
            custom.copy(
                island = true,
                islandActivities = IslandActivity.entries.toSet() - IslandActivity.CHARGING,
                touchEffects = true,
            ),
            repo.current,
        )
    }

    @Test
    fun `UI-79 switching off the last activity switches the island off`() = runTest {
        val repo = FakeSettingsRepository(Settings(island = true, islandActivities = setOf(IslandActivity.MEDIA)))
        val vm = NotchSettingsViewModel(repo, FakeTouchArea())
        vm.setIslandActivity(IslandActivity.MEDIA, false)
        assertFalse(repo.current.island)
        assertEquals(setOf(IslandActivity.MEDIA), repo.current.islandActivities)
        repo.update { it.copy(islandActivities = emptySet()) }
        vm.setIsland(true)
        assertTrue(repo.current.island)
        assertEquals(IslandActivity.entries.toSet(), repo.current.islandActivities)
    }

    @Test
    fun `UI-80 overlay issues`() = runTest {
        val requirements = MutableStateFlow(Requirements())
        val vm = NotchSettingsViewModel(FakeSettingsRepository(), FakeTouchArea(), requirements)
        vm.state.test {
            val none = expectMostRecentItem()
            OverlayFeature.entries.forEach { assertEquals(OverlayIssue.ACCESSIBILITY, none.overlayIssue(it), "$it") }
            requirements.value = Requirements(accessibilityEnabled = true)
            val a11y = expectMostRecentItem()
            val needsAccess = setOf(OverlayFeature.ISLAND_MEDIA, OverlayFeature.ISLAND_NOTIFICATIONS)
            OverlayFeature.entries.forEach {
                assertEquals(if (it in needsAccess) OverlayIssue.NOTIFICATION_ACCESS else null, a11y.overlayIssue(it), "$it")
            }
            requirements.value = Requirements(accessibilityEnabled = true, mediaAccessGranted = true)
            val all = expectMostRecentItem()
            OverlayFeature.entries.forEach { assertNull(all.overlayIssue(it), "$it") }
        }
    }
}
