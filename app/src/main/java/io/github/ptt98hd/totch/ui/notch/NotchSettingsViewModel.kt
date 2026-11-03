package io.github.ptt98hd.totch.ui.notch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.Requirements
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.TouchArea
import io.github.ptt98hd.totch.core.overlay.IslandActivity
import io.github.ptt98hd.totch.core.settings.NotchRegionPx
import io.github.ptt98hd.totch.core.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A slider of the Notch screen; only sliders have a reset button (UI-85, FR-S9). */
enum class NotchSetting { AREA_X, AREA_Y, AREA_WIDTH, AREA_HEIGHT, PADDING }

private val defaults = Settings()

/** Applies [transform] to the manual region; matching the detected region [auto] means "automatic" again (UI-31). */
private fun Settings.area(auto: NotchRegionPx, transform: (NotchRegionPx) -> NotchRegionPx): Settings {
    val region = notchRegion ?: return this
    return copy(notchRegion = transform(region).takeUnless { it == auto })
}

/** UI-30: [this] slider back to its default (`Settings()`, or [auto] for the touch area); other fields untouched. */
fun NotchSetting.reset(s: Settings, auto: NotchRegionPx): Settings = when (this) {
    NotchSetting.AREA_X -> s.area(auto) { it.copy(centerXPx = auto.centerXPx) }
    NotchSetting.AREA_Y -> s.area(auto) { it.copy(topPx = auto.topPx) }
    NotchSetting.AREA_WIDTH -> s.area(auto) { it.copy(widthPx = auto.widthPx) }
    NotchSetting.AREA_HEIGHT -> s.area(auto) { it.copy(heightPx = auto.heightPx) }
    NotchSetting.PADDING -> s.copy(paddingPx = defaults.paddingPx)
}

fun NotchSetting.isDefault(s: Settings, auto: NotchRegionPx): Boolean = reset(s, auto) == s

/** A switch of the Effects section (notch-overlay §6). */
enum class OverlayFeature { ISLAND, ISLAND_MEDIA, ISLAND_NOTIFICATIONS, ISLAND_CHARGING, ISLAND_SOUND, TOUCH_EFFECTS }

/** Why an overlay feature cannot show yet (UI-80). */
enum class OverlayIssue { ACCESSIBILITY, NOTIFICATION_ACCESS }

private val needsNotificationAccess =
    setOf(OverlayFeature.ISLAND_MEDIA, OverlayFeature.ISLAND_NOTIFICATIONS)

data class NotchSettingsUiState(
    val loading: Boolean = true,
    val settings: Settings = Settings(),
    /** The detected region, or the default one when there is none (landscape). */
    val auto: NotchRegionPx = defaultRegion(DEFAULT_SCREEN_WIDTH_PX),
    /** Width of the natural display: the scale of the horizontal position (UI-86). */
    val screenWidthPx: Int = DEFAULT_SCREEN_WIDTH_PX,
    val requirements: Requirements = Requirements(),
) {
    /** UI-80: accessibility first, then notification access for the features that read notifications or media. */
    fun overlayIssue(feature: OverlayFeature): OverlayIssue? = when {
        !requirements.accessibilityEnabled -> OverlayIssue.ACCESSIBILITY
        feature in needsNotificationAccess && !requirements.mediaAccessGranted -> OverlayIssue.NOTIFICATION_ACCESS
        else -> null
    }

    /** UI-33: the region the touch-area sliders show. */
    val area: NotchRegionPx get() = settings.notchRegion ?: auto

    fun isDefault(item: NotchSetting): Boolean = item.isDefault(settings, auto)
}

private const val DEFAULT_SCREEN_WIDTH_PX = 1080

/** Shown when nothing is detected (landscape): centered, 80 px square. */
private fun defaultRegion(screenWidthPx: Int) = NotchRegionPx(screenWidthPx / 2, 80, 80)

/** Notch settings: effects, position and size in px (FR-U12, FR-R10). */
class NotchSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val touchArea: TouchArea,
    requirements: Flow<Requirements> = flowOf(Requirements()),
) : ViewModel() {
    val state: StateFlow<NotchSettingsUiState> =
        combine(settingsRepository.settings, touchArea.detectedRegion, touchArea.screenWidthPx, requirements) { settings, detected, width, r ->
            NotchSettingsUiState(
                loading = false, settings = settings, auto = detected ?: defaultRegion(width), screenWidthPx = width, requirements = r,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotchSettingsUiState())

    private val auto: NotchRegionPx get() = touchArea.detectedRegion.value ?: defaultRegion(touchArea.screenWidthPx.value)

    private fun save(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun setPaddingPx(value: Int) = save { it.copy(paddingPx = value) }

    /** UI-86: any touch-area slider; the detected region itself is stored as "automatic". */
    fun setArea(region: NotchRegionPx) {
        val auto = auto
        save { it.copy(notchRegion = region.takeUnless { r -> r == auto }) }
    }

    fun setTouchEffects(on: Boolean) = save { it.copy(touchEffects = on) }

    /** UI-79: an island without activities (old data) comes back with all of them. */
    fun setIsland(on: Boolean) = save {
        it.copy(island = on, islandActivities = it.islandActivities.ifEmpty { IslandActivity.entries.toSet() })
    }

    /** UI-79: switching off the last activity switches the island off and keeps that activity for next time. */
    fun setIslandActivity(activity: IslandActivity, on: Boolean) = save {
        val activities = if (on) it.islandActivities + activity else it.islandActivities - activity
        if (activities.isEmpty()) it.copy(island = false) else it.copy(islandActivities = activities)
    }

    fun reset(item: NotchSetting) {
        val auto = auto
        save { item.reset(it, auto) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                NotchSettingsViewModel(container.settingsRepository, container.touchArea, container.systemStatus.requirements)
            }
        }
    }
}
