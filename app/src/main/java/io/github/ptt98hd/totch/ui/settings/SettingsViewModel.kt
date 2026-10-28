package io.github.ptt98hd.totch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.core.settings.AppLanguage
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.ThemeMode
import io.github.ptt98hd.totch.core.settings.withDefaultPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A settings row with its own reset button (UI-30). */
enum class SettingItem {
    DOUBLE_TAP, LONG_PRESS, SWIPE_DISTANCE, REPEAT_INTERVAL,
    PADDING, CUSTOM_AREA, AREA_X, AREA_WIDTH, AREA_HEIGHT,
    HAPTICS, THEME, AMOLED, LANGUAGE, START_ON_BOOT,
}

private val defaults = Settings()
private val defaultRegion = ManualRegionDp()

private fun Settings.region(transform: (ManualRegionDp) -> ManualRegionDp) =
    copy(manualRegion = manualRegion?.let(transform))

/** UI-30: [this] item back to the value of `Settings()`; other fields untouched. */
fun SettingItem.reset(s: Settings): Settings = when (this) {
    SettingItem.DOUBLE_TAP -> s.copy(doubleTapMs = defaults.doubleTapMs)
    SettingItem.LONG_PRESS -> s.copy(longPressMs = defaults.longPressMs)
    SettingItem.SWIPE_DISTANCE -> s.copy(swipeMinDistanceDp = defaults.swipeMinDistanceDp)
    SettingItem.REPEAT_INTERVAL -> s.copy(repeatIntervalMs = defaults.repeatIntervalMs)
    SettingItem.PADDING -> s.copy(paddingDp = defaults.paddingDp)
    SettingItem.CUSTOM_AREA -> s.copy(manualRegion = defaults.manualRegion)
    SettingItem.AREA_X -> s.region { it.copy(centerXFraction = defaultRegion.centerXFraction) }
    SettingItem.AREA_WIDTH -> s.region { it.copy(widthDp = defaultRegion.widthDp) }
    SettingItem.AREA_HEIGHT -> s.region { it.copy(heightDp = defaultRegion.heightDp) }
    SettingItem.HAPTICS -> s.copy(haptics = defaults.haptics)
    SettingItem.THEME -> s.copy(theme = defaults.theme)
    SettingItem.AMOLED -> s.copy(amoledBlack = defaults.amoledBlack)
    SettingItem.LANGUAGE -> s.copy(language = defaults.language)
    SettingItem.START_ON_BOOT -> s.copy(startOnBoot = defaults.startOnBoot)
}

fun SettingItem.isDefault(s: Settings): Boolean = reset(s) == s

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: Settings = Settings(),
    val confirmReset: Boolean = false,
)

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val confirmReset = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> =
        combine(settingsRepository.settings, confirmReset) { settings, confirm ->
            SettingsUiState(loading = false, settings = settings, confirmReset = confirm)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private fun save(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun setDoubleTapMs(value: Long) = save { it.copy(doubleTapMs = value) }
    fun setLongPressMs(value: Long) = save { it.copy(longPressMs = value) }
    fun setSwipeDistanceDp(value: Int) = save { it.copy(swipeMinDistanceDp = value) }
    fun setRepeatIntervalMs(value: Long) = save { it.copy(repeatIntervalMs = value) }
    fun setPaddingDp(value: Int) = save { it.copy(paddingDp = value) }
    fun setHaptics(value: Boolean) = save { it.copy(haptics = value) }
    fun setTheme(value: ThemeMode) = save { it.copy(theme = value) }
    fun setAmoledBlack(value: Boolean) = save { it.copy(amoledBlack = value) }
    fun setLanguage(value: AppLanguage) = save { it.copy(language = value) }
    fun setStartOnBoot(value: Boolean) = save { it.copy(startOnBoot = value) }

    fun setCustomArea(enabled: Boolean) = save {
        it.copy(manualRegion = if (enabled) it.manualRegion ?: ManualRegionDp() else null)
    }

    fun setManualRegion(region: ManualRegionDp) = save { it.copy(manualRegion = region) }

    fun reset(item: SettingItem) = save(item::reset)

    fun requestReset() {
        confirmReset.value = true
    }

    fun cancelReset() {
        confirmReset.value = false
    }

    /** UI-31: preferences only; gestures, the switch and the mode are kept. */
    fun confirmReset() {
        confirmReset.value = false
        save { it.withDefaultPreferences() }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { SettingsViewModel(container.settingsRepository) }
        }
    }
}
