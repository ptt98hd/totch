package io.github.ptt98hd.totch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.AppUpdater
import io.github.ptt98hd.totch.BuildConfig
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.core.settings.AppLanguage
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.ThemeMode
import io.github.ptt98hd.totch.core.update.Release
import io.github.ptt98hd.totch.core.update.Releases
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Self-update progress shown on the Check for updates row (update spec §5). */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    /** Handed over to the system installer. */
    data object Installing : UpdateState
    data object Failed : UpdateState
}

data class AppSettingsUiState(
    val loading: Boolean = true,
    val settings: Settings = Settings(),
    val update: UpdateState = UpdateState.Idle,
    val updateDialog: Boolean = false,
)

private data class UpdateUi(val state: UpdateState = UpdateState.Idle, val dialog: Boolean = false)

/** App settings: system, appearance and about (FR-U12, FR-U23, FR-U27). */
class AppSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val updater: AppUpdater,
    private val currentVersion: String,
) : ViewModel() {
    private val update = MutableStateFlow(UpdateUi())

    val state: StateFlow<AppSettingsUiState> = combine(settingsRepository.settings, update) { settings, u ->
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

    /** UP-6: checks GitHub, or reopens the dialog of a release already found. */
    fun onUpdateRowClick() {
        when (update.value.state) {
            UpdateState.Checking, is UpdateState.Downloading -> return
            is UpdateState.Available -> update.update { it.copy(dialog = true) }
            else -> {
                update.value = UpdateUi(UpdateState.Checking)
                viewModelScope.launch {
                    val result = try {
                        val release = updater.latestRelease()
                        if (release != null && Releases.isNewer(release, currentVersion)) {
                            UpdateUi(UpdateState.Available(release), dialog = true)
                        } else {
                            UpdateUi(UpdateState.UpToDate)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        UpdateUi(UpdateState.Failed)
                    }
                    update.value = result
                }
            }
        }
    }

    fun dismissUpdateDialog() = update.update { it.copy(dialog = false) }

    /** UP-7 */
    fun startUpdate() {
        val release = (update.value.state as? UpdateState.Available)?.release ?: return
        update.value = UpdateUi(UpdateState.Downloading(0f))
        viewModelScope.launch {
            val ok = try {
                updater.downloadAndInstall(release) { p -> update.value = UpdateUi(UpdateState.Downloading(p.coerceIn(0f, 1f))) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            update.value = UpdateUi(if (ok) UpdateState.Installing else UpdateState.Failed)
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AppSettingsViewModel(container.settingsRepository, container.appUpdater, BuildConfig.VERSION_NAME) }
        }
    }
}
