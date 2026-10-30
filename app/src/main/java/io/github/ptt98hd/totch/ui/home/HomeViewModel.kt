package io.github.ptt98hd.totch.ui.home

import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.Permission
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.Requirements
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.SystemStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shizuku under Home › Advanced (UI-50); optional since v1.7 (FR-D13). */
enum class ShizukuStatus { NOT_RUNNING, PERMISSION_REQUIRED, READY }

/** A permission row (UI-51). */
data class PermissionItem(val permission: Permission, val granted: Boolean)

/** A string resource plus its format arguments. */
data class StatusText(@StringRes val resId: Int, val args: List<String> = emptyList())

/** UI-2: one status text per [EngineStatus]. */
fun statusText(status: EngineStatus): StatusText = when (status) {
    EngineStatus.Disabled -> StatusText(R.string.status_disabled)
    EngineStatus.NeedsAccess -> StatusText(R.string.status_needs_access) // UI-55
    EngineStatus.Starting -> StatusText(R.string.status_starting)
    EngineStatus.Stopping -> StatusText(R.string.status_stopping)
    is EngineStatus.Error -> StatusText(R.string.status_error, listOf(status.message))
    EngineStatus.Running -> StatusText(R.string.status_running)
    EngineStatus.PausedRotation -> StatusText(R.string.status_paused_rotation)
}

/** State icon in the box of the master card (UI-42). */
enum class MasterIcon { ON, OFF, PENDING }

fun masterIcon(status: EngineStatus): MasterIcon = when (status) {
    EngineStatus.Running -> MasterIcon.ON
    EngineStatus.Starting, EngineStatus.Stopping -> MasterIcon.PENDING
    else -> MasterIcon.OFF
}

data class HomeUiState(
    val loading: Boolean = true,
    val enabled: Boolean = false,
    val status: EngineStatus = EngineStatus.Disabled,
    val shizuku: ShizukuStatus = ShizukuStatus.NOT_RUNNING,
    val permissions: List<PermissionItem> = emptyList(),
)

internal fun shizukuStatus(r: Requirements) = when {
    !r.shizukuRunning -> ShizukuStatus.NOT_RUNNING
    !r.shizukuGranted -> ShizukuStatus.PERMISSION_REQUIRED
    else -> ShizukuStatus.READY
}

internal fun permissionsFor(r: Requirements, notificationPermissionRequired: Boolean) = buildList {
    add(PermissionItem(Permission.ACCESSIBILITY, r.accessibilityEnabled))
    if (notificationPermissionRequired) add(PermissionItem(Permission.NOTIFICATIONS, r.notificationsGranted))
    add(PermissionItem(Permission.MEDIA_ACCESS, r.mediaAccessGranted))
    add(PermissionItem(Permission.DND_ACCESS, r.dndAccessGranted))
    add(PermissionItem(Permission.BATTERY, r.batteryUnrestricted))
    add(PermissionItem(Permission.KEEP_WHEN_UNUSED, r.keepWhenUnused))
    add(PermissionItem(Permission.INSTALL_UPDATES, r.installUpdatesAllowed))
}

/** UI-53: revoking notifications or update installs restarts Totch (FR-D10, FR-D11), so it is confirmed first. */
fun needsConfirmation(permission: Permission, granted: Boolean): Boolean =
    !granted && (permission == Permission.NOTIFICATIONS || permission == Permission.INSTALL_UPDATES)

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
                status = status,
                shizuku = shizukuStatus(reqs),
                permissions = permissionsFor(reqs, notificationPermissionRequired),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** UI-1: always persisted, even when no touch source is available. */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.update { it.copy(enabled = enabled) } }
    }

    /** UI-52: through Shizuku when possible; otherwise [onFallback] opens the system page. */
    fun setPermission(permission: Permission, granted: Boolean, onFallback: (Permission) -> Unit) {
        viewModelScope.launch {
            if (!systemStatus.setPermission(permission, granted)) onFallback(permission)
        }
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
