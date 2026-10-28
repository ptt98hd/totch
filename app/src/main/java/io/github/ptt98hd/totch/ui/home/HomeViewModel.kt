package io.github.ptt98hd.totch.ui.home

import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.Requirements
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.SystemStatus
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.effectiveAction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RequirementType { SHIZUKU_RUNNING, SHIZUKU_PERMISSION, ACCESSIBILITY, NOTIFICATIONS, MEDIA_ACCESS, BATTERY }

/** A requirement card. [actionEnabled] is false when its button cannot do anything yet. */
data class RequirementItem(val type: RequirementType, val satisfied: Boolean, val actionEnabled: Boolean = true)

/** A string resource plus its format arguments. */
data class StatusText(@StringRes val resId: Int, val args: List<String> = emptyList())

/** UI-2: one status text per [EngineStatus]. */
fun statusText(status: EngineStatus): StatusText = when (status) {
    EngineStatus.Disabled -> StatusText(R.string.status_disabled)
    EngineStatus.WaitingShizuku -> StatusText(R.string.status_waiting_shizuku)
    EngineStatus.ShizukuPermissionRequired -> StatusText(R.string.status_shizuku_permission_required)
    EngineStatus.AccessibilityRequired -> StatusText(R.string.status_accessibility_required)
    is EngineStatus.Error -> StatusText(R.string.status_error, listOf(status.message))
    EngineStatus.Running -> StatusText(R.string.status_running)
    EngineStatus.PausedRotation -> StatusText(R.string.status_paused_rotation)
}

/** State icon in the box of the master card (UI-42). */
enum class MasterIcon { CHECK, ERROR, OFF }

fun masterIcon(status: EngineStatus): MasterIcon = when (status) {
    EngineStatus.Running -> MasterIcon.CHECK
    is EngineStatus.Error -> MasterIcon.ERROR
    else -> MasterIcon.OFF
}

data class HomeUiState(
    val loading: Boolean = true,
    val enabled: Boolean = false,
    val mode: DetectionMode = DetectionMode.ACCESSIBILITY,
    val status: EngineStatus = EngineStatus.Disabled,
    val requirements: List<RequirementItem> = emptyList(),
)

private val mediaActions = setOf(Action.MediaPlayPause, Action.MediaNext, Action.MediaPrevious)

internal fun requirementsFor(
    mode: DetectionMode,
    r: Requirements,
    notificationPermissionRequired: Boolean,
    mediaBound: Boolean,
) = buildList {
    when (mode) {
        DetectionMode.ACCESSIBILITY -> add(RequirementItem(RequirementType.ACCESSIBILITY, r.accessibilityEnabled))
        DetectionMode.SHIZUKU -> {
            add(RequirementItem(RequirementType.SHIZUKU_RUNNING, r.shizukuRunning))
            add(RequirementItem(RequirementType.SHIZUKU_PERMISSION, r.shizukuGranted, actionEnabled = r.shizukuRunning))
            if (notificationPermissionRequired) add(RequirementItem(RequirementType.NOTIFICATIONS, r.notificationsGranted))
        }
    }
    if (mediaBound) add(RequirementItem(RequirementType.MEDIA_ACCESS, r.mediaAccessGranted)) // UI-3
    add(RequirementItem(RequirementType.BATTERY, r.batteryUnrestricted)) // UI-4, recommended
}

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    engineStatus: StateFlow<EngineStatus>,
    private val systemStatus: SystemStatus,
    /** True on Android 13+ where POST_NOTIFICATIONS is a runtime permission. */
    private val notificationPermissionRequired: Boolean,
) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        combine(settingsRepository.settings, engineStatus, systemStatus.requirements) { settings, status, reqs ->
            HomeUiState(
                loading = false,
                enabled = settings.enabled,
                mode = settings.mode,
                status = status,
                requirements = requirementsFor(
                    settings.mode,
                    reqs,
                    notificationPermissionRequired,
                    mediaBound = Gesture.entries.any { settings.effectiveAction(it) in mediaActions },
                ),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** UI-1: always persisted, even when requirements are missing. */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.update { it.copy(enabled = enabled) } }
    }

    fun setMode(mode: DetectionMode) {
        viewModelScope.launch { settingsRepository.update { it.copy(mode = mode) } }
    }

    fun refresh() = systemStatus.refresh()

    fun requestShizukuPermission() = systemStatus.requestShizukuPermission()

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                HomeViewModel(
                    container.settingsRepository,
                    container.engineStatus,
                    container.systemStatus,
                    notificationPermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                )
            }
        }
    }
}
