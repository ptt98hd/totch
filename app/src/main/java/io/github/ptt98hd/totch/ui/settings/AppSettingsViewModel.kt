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
import io.github.ptt98hd.totch.core.settings.UpdateInterval
import io.github.ptt98hd.totch.update.UpdateController
import io.github.ptt98hd.totch.update.UpdateState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppSettingsUiState(
    val loading: Boolean = true,
    val settings: Settings = Settings(),
    val update: UpdateState = UpdateState.Idle,
    val updateDialog: Boolean = false,
)

/** App settings: system, appearance, update and about (FR-U12, FR-U23, FR-U27, FR-U29). */
class AppSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val updates: UpdateController,
) : ViewModel() {

    val state: StateFlow<AppSettingsUiState> = combine(settingsRepository.settings, updates.state) { settings, u ->
        AppSettingsUiState(loading = false, settings = settings, update = u.state, updateDialog = u.dialog)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettingsUiState())

    private fun save(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun setTheme(value: ThemeMode) = save { it.copy(theme = value) }
    fun setAmoledBlack(value: Boolean) = save { it.copy(amoledBlack = value) }
    fun setLanguage(value: AppLanguage) = save { it.copy(language = value) }
    fun setStartOnBoot(value: Boolean) = save { it.copy(startOnBoot = value) }
    fun setHaptics(value: Boolean) = save { it.copy(haptics = value) }
    fun setLandscapeGestures(value: Boolean) = save { it.copy(landscapeGestures = value) }

    /** UI-67 */
    fun setNavLabels(value: Boolean) = save { it.copy(navLabels = value) }

    /** UP-13 */
    fun setUpdateOnLaunch(value: Boolean) = save { it.copy(updateOnLaunch = value) }
    fun setUpdateInterval(value: UpdateInterval) = save { it.copy(updateInterval = value) }

    /** UP-23: the same dialog as the top bar button, which checks GitHub unless a result is pending or known. */
    fun onUpdateRowClick() = updates.openDialog()

    fun dismissUpdateDialog() = updates.dismissDialog()

    /** UP-7 */
    fun startUpdate() = updates.startUpdate()

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AppSettingsViewModel(container.settingsRepository, container.updates) }
        }
    }
}
