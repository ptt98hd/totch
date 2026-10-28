package io.github.ow3n.totch

import io.github.ow3n.totch.core.model.Rect
import io.github.ow3n.totch.core.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract between the UI layer (`ui/`) and the platform layer (`data/`, `engine/`, `shizuku/`,
 * `accessibility/`, `service/`, `action/`). See docs/specs/detection-modes.md and docs/specs/ui.md.
 */
interface AppContainer {
    val settingsRepository: SettingsRepository
    val engineStatus: StateFlow<EngineStatus>
    val systemStatus: SystemStatus
    val installedApps: InstalledApps
    val touchAreaPreview: TouchAreaPreview
}

interface SettingsRepository {
    /** Always emits sanitized settings. */
    val settings: Flow<Settings>
    suspend fun update(transform: (Settings) -> Settings)
}

sealed interface EngineStatus {
    data object Disabled : EngineStatus
    data object WaitingShizuku : EngineStatus
    data object ShizukuPermissionRequired : EngineStatus
    data object AccessibilityRequired : EngineStatus
    data class Error(val message: String) : EngineStatus
    data object Running : EngineStatus
    data object PausedRotation : EngineStatus
}

data class Requirements(
    val shizukuRunning: Boolean = false,
    val shizukuGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val notificationsGranted: Boolean = false,
)

interface SystemStatus {
    val requirements: StateFlow<Requirements>
    /** Re-reads states that have no change callback (accessibility, notifications). Call on resume. */
    fun refresh()
    fun requestShizukuPermission()
}

data class AppInfo(val packageName: String, val label: String)

interface InstalledApps {
    /** Apps with a MAIN/LAUNCHER activity, sorted by label (case-insensitive). */
    suspend fun launchableApps(): List<AppInfo>
    /** Label of an installed package, or null when not installed. */
    fun labelOf(packageName: String): String?
}

interface TouchAreaPreview {
    /** Notch region in screen pixels for the current settings, or null when inactive. */
    val region: StateFlow<Rect?>
    /** Highlights the touch area on screen (accessibility overlay) for [durationMs]. Returns false if not possible. */
    fun show(durationMs: Long = 3000): Boolean
}
