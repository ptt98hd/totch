package io.github.ptt98hd.totch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.core.settings.AppLanguage
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppSettingsUiState(
    val loading: Boolean = true,
    val settings: Settings = Settings(),
)

/** App settings: system, appearance and about (FR-U12, FR-U23). */
class AppSettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val state: StateFlow<AppSettingsUiState> = settingsRepository.settings
        .map { AppSettingsUiState(loading = false, settings = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsUiState())

    private fun save(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun setTheme(value: ThemeMode) = save { it.copy(theme = value) }
    fun setAmoledBlack(value: Boolean) = save { it.copy(amoledBlack = value) }
    fun setLanguage(value: AppLanguage) = save { it.copy(language = value) }
    fun setStartOnBoot(value: Boolean) = save { it.copy(startOnBoot = value) }
    fun setHaptics(value: Boolean) = save { it.copy(haptics = value) }
    fun setLandscapeGestures(value: Boolean) = save { it.copy(landscapeGestures = value) }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AppSettingsViewModel(container.settingsRepository) }
        }
    }
}
