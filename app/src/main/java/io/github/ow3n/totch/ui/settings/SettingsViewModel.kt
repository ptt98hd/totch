package io.github.ow3n.totch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ow3n.totch.AppContainer
import io.github.ow3n.totch.SettingsRepository
import io.github.ow3n.totch.TouchAreaPreview
import io.github.ow3n.totch.core.model.Rect
import io.github.ow3n.totch.core.settings.ManualRegionDp
import io.github.ow3n.totch.core.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: Settings = Settings(),
    val previewRegion: Rect? = null,
    /** Shows the in-app drawing of the touch area (overlay preview not possible). */
    val showInAppPreview: Boolean = false,
    val confirmReset: Boolean = false,
)

private data class Dialogs(val inAppPreview: Boolean = false, val confirmReset: Boolean = false)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val touchAreaPreview: TouchAreaPreview,
) : ViewModel() {
    private val dialogs = MutableStateFlow(Dialogs())

    val state: StateFlow<SettingsUiState> =
        combine(settingsRepository.settings, touchAreaPreview.region, dialogs) { settings, region, d ->
            SettingsUiState(
                loading = false,
                settings = settings,
                previewRegion = region,
                showInAppPreview = d.inAppPreview,
                confirmReset = d.confirmReset,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private fun save(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun setDoubleTapMs(value: Long) = save { it.copy(doubleTapMs = value) }
    fun setLongPressMs(value: Long) = save { it.copy(longPressMs = value) }
    fun setSwipeDistanceDp(value: Int) = save { it.copy(swipeMinDistanceDp = value) }
    fun setPaddingDp(value: Int) = save { it.copy(paddingDp = value) }
    fun setHaptics(value: Boolean) = save { it.copy(haptics = value) }

    fun setCustomArea(enabled: Boolean) = save {
        it.copy(manualRegion = if (enabled) it.manualRegion ?: ManualRegionDp() else null)
    }

    fun setManualRegion(region: ManualRegionDp) = save { it.copy(manualRegion = region) }

    fun preview() {
        if (!touchAreaPreview.show(PREVIEW_DURATION_MS)) dialogs.update { it.copy(inAppPreview = true) }
    }

    fun dismissPreview() = dialogs.update { it.copy(inAppPreview = false) }

    fun requestReset() = dialogs.update { it.copy(confirmReset = true) }

    fun cancelReset() = dialogs.update { it.copy(confirmReset = false) }

    /** Restores every default except the master switch and the detection mode. */
    fun confirmReset() {
        dialogs.update { it.copy(confirmReset = false) }
        save { Settings(enabled = it.enabled, mode = it.mode) }
    }

    companion object {
        const val PREVIEW_DURATION_MS = 3_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { SettingsViewModel(container.settingsRepository, container.touchAreaPreview) }
        }
    }
}
