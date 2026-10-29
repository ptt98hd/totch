package io.github.ptt98hd.totch

import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
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
    val appShortcuts: AppShortcuts
    val appActivities: AppActivities
    val touchArea: TouchArea
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
    /** Shizuku is ready and the touch pipeline is connecting. */
    data object Starting : EngineStatus
    /** Switched off while the touch pipeline is still shutting down. */
    data object Stopping : EngineStatus
    data class Error(val message: String) : EngineStatus
    data object Running : EngineStatus
    data object PausedRotation : EngineStatus
}

/** Optional permissions shown on Home; each can be switched through Shizuku (detection-modes §6). */
enum class Permission { ACCESSIBILITY, NOTIFICATIONS, MEDIA_ACCESS, BATTERY }

data class Requirements(
    val shizukuRunning: Boolean = false,
    val shizukuGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val notificationsGranted: Boolean = false,
    /** Notification access for [io.github.ptt98hd.totch.action.MediaListenerService] (media control, UI-3). */
    val mediaAccessGranted: Boolean = false,
    /** App is exempt from battery optimization (FR-D7, recommended). */
    val batteryUnrestricted: Boolean = false,
)

interface SystemStatus {
    val requirements: StateFlow<Requirements>
    /** Re-reads states that have no change callback (accessibility, notifications). Call on resume. */
    fun refresh()
    fun requestShizukuPermission()
    /**
     * Grants or revokes [permission] through Shizuku. Returns false when it could not (Shizuku not ready, command
     * failed, or not possible that way); the caller then opens the matching system page.
     */
    suspend fun setPermission(permission: Permission, granted: Boolean): Boolean
}

data class AppInfo(val packageName: String, val label: String)

interface InstalledApps {
    /** Apps with a MAIN/LAUNCHER activity, sorted by label (case-insensitive). */
    suspend fun launchableApps(): List<AppInfo>
    /** Label of an installed package, or null when not installed. */
    fun labelOf(packageName: String): String?
}

/** A static shortcut another app can be asked to open (actions spec §7). */
data class ShortcutEntry(
    val packageName: String,
    val appLabel: String,
    val id: String,
    val label: String,
    /** `Intent.toUri(Intent.URI_INTENT_SCHEME)` of the shortcut's last intent. */
    val intentUri: String,
)

interface AppShortcuts {
    /** Static shortcuts of launchable apps that Totch is able to open; unordered. */
    suspend fun shortcuts(): List<ShortcutEntry>
}

/** An exported activity of a launchable app (actions spec §8); [name] is the fully qualified class name. */
data class ActivityEntry(
    val packageName: String,
    val appLabel: String,
    val name: String,
    val label: String,
)

interface AppActivities {
    /** Exported activities without a required permission, of launchable apps; unordered. */
    suspend fun activities(): List<ActivityEntry>
}

/** The highlighted touch area in screen pixels: [notch] (width × height) inside [touch] (notch + padding). */
data class HighlightArea(val touch: Rect, val notch: Rect)

/** Notch touch area for the in-app highlight (ui spec §2). */
interface TouchArea {
    /** Area for the saved settings, or null when inactive (landscape). */
    val area: StateFlow<HighlightArea?>
    /** Area the given (possibly unsaved draft) settings would produce on the current display. */
    fun areaFor(settings: Settings): HighlightArea?
    /** The automatically detected region in dp, shown while no manual region is set (FR-R5); null when rotated. */
    val detectedRegion: StateFlow<ManualRegionDp?>
}
