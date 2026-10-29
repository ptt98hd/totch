package io.github.ptt98hd.totch.ui.notch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.TouchArea
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A slider of the Preferences screen; only sliders have a reset button (UI-30, FR-S9). */
enum class NotchSetting {
    DOUBLE_TAP, LONG_PRESS, SWIPE_DISTANCE,
    AREA_X, AREA_Y, AREA_WIDTH, AREA_HEIGHT, PADDING,
}

private val defaults = Settings()

/** Applies [transform] to the manual region; matching the detected region [auto] means "automatic" again (UI-31). */
private fun Settings.area(auto: ManualRegionDp, transform: (ManualRegionDp) -> ManualRegionDp): Settings {
    val region = manualRegion ?: return this
    return copy(manualRegion = transform(region).takeUnless { it == auto })
}

/** UI-30: [this] slider back to its default (`Settings()`, or [auto] for the touch area); other fields untouched. */
fun NotchSetting.reset(s: Settings, auto: ManualRegionDp): Settings = when (this) {
    NotchSetting.DOUBLE_TAP -> s.copy(doubleTapMs = defaults.doubleTapMs)
    NotchSetting.LONG_PRESS -> s.copy(longPressMs = defaults.longPressMs)
    NotchSetting.SWIPE_DISTANCE -> s.copy(swipeMinDistanceDp = defaults.swipeMinDistanceDp)
    NotchSetting.AREA_X -> s.area(auto) { it.copy(centerXFraction = auto.centerXFraction) }
    NotchSetting.AREA_Y -> s.area(auto) { it.copy(topDp = auto.topDp) }
    NotchSetting.AREA_WIDTH -> s.area(auto) { it.copy(widthDp = auto.widthDp) }
    NotchSetting.AREA_HEIGHT -> s.area(auto) { it.copy(heightDp = auto.heightDp) }
    NotchSetting.PADDING -> s.copy(paddingDp = defaults.paddingDp)
}

fun NotchSetting.isDefault(s: Settings, auto: ManualRegionDp): Boolean = reset(s, auto) == s

data class NotchSettingsUiState(
    val loading: Boolean = true,
    val settings: Settings = Settings(),
    /** The detected region, or the default one when there is none (landscape). */
    val auto: ManualRegionDp = ManualRegionDp(),
) {
    /** UI-33: the region the touch-area sliders show. */
    val area: ManualRegionDp get() = settings.manualRegion ?: auto

    fun isDefault(item: NotchSetting): Boolean = item.isDefault(settings, auto)
}

/** Preferences (notch settings): feedback, sensitivity, position and size (FR-U12, FR-U17). */
class NotchSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val touchArea: TouchArea,
) : ViewModel() {
    val state: StateFlow<NotchSettingsUiState> =
        combine(settingsRepository.settings, touchArea.detectedRegion) { settings, detected ->
            NotchSettingsUiState(loading = false, settings = settings, auto = detected ?: ManualRegionDp())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotchSettingsUiState())

    private val auto: ManualRegionDp get() = touchArea.detectedRegion.value ?: ManualRegionDp()

    private fun save(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun setDoubleTapMs(value: Long) = save { it.copy(doubleTapMs = value) }
    fun setLongPressMs(value: Long) = save { it.copy(longPressMs = value) }
    fun setSwipeDistanceDp(value: Int) = save { it.copy(swipeMinDistanceDp = value) }
    fun setPaddingDp(value: Int) = save { it.copy(paddingDp = value) }

    /** UI-33: any touch-area slider; the detected region itself is stored as "automatic". */
    fun setArea(region: ManualRegionDp) {
        val auto = auto
        save { it.copy(manualRegion = region.takeUnless { r -> r == auto }) }
    }

    fun reset(item: NotchSetting) {
        val auto = auto
        save { item.reset(it, auto) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { NotchSettingsViewModel(container.settingsRepository, container.touchArea) }
        }
    }
}
