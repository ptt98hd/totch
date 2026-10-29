package io.github.ptt98hd.totch

import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.update.Release
import io.github.ptt98hd.totch.update.UpdateController
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
    val quickSettingsTiles: QuickSettingsTiles
    val appUpdater: AppUpdater
    /** Shared update state and dialog (update spec §5). */
    val updates: UpdateController
    val touchArea: TouchArea
}

interface SettingsRepository {
    /** Always emits sanitized settings. */
    val settings: Flow<Settings>
    suspend fun update(transform: (Settings) -> Settings)
}

sealed interface EngineStatus {
    data object Disabled : EngineStatus
    /** No touch source: neither Shizuku nor the accessibility service is available (FR-D13, DM-41). */
    data object NeedsAccess : EngineStatus
    /** Shizuku is ready and the touch pipeline is connecting. */
    data object Starting : EngineStatus
    /** Switched off while the touch pipeline is still shutting down. */
    data object Stopping : EngineStatus
    data class Error(val message: String) : EngineStatus
    data object Running : EngineStatus
    data object PausedRotation : EngineStatus
}

/** Permissions shown on Home; each can be switched through Shizuku (detection-modes §6). */
enum class Permission { ACCESSIBILITY, NOTIFICATIONS, MEDIA_ACCESS, DND_ACCESS, BATTERY, INSTALL_UPDATES }

data class Requirements(
    val shizukuRunning: Boolean = false,
    val shizukuGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val notificationsGranted: Boolean = false,
    /** Notification access for [io.github.ptt98hd.totch.action.MediaListenerService] (media control, UI-3). */
    val mediaAccessGranted: Boolean = false,
    /** App is exempt from battery optimization (FR-D7, recommended). */
    val batteryUnrestricted: Boolean = false,
    /** "Install unknown apps" for the self-update (FR-D11). */
    val installUpdatesAllowed: Boolean = false,
    /** Do Not Disturb access for the sound mode actions (FR-D12). */
    val dndAccessGranted: Boolean = false,
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

/** A Quick Settings tile (`TileService`) of another app (actions spec §9); [name] is the service class name. */
data class TileEntry(
    val packageName: String,
    val appLabel: String,
    val name: String,
    val label: String,
)

interface QuickSettingsTiles {
    /** Exported tile services guarded by `BIND_QUICK_SETTINGS_TILE`, except Totch's own; unordered. */
    suspend fun tiles(): List<TileEntry>
}

/** Self-update from GitHub Releases (update spec §4). */
interface AppUpdater {
    /** Latest usable release; throws on network failure, null when there is none (UP-3). */
    suspend fun latestRelease(): Release?
    /** Downloads, verifies (UP-8) and hands the APK to Shizuku or the system installer (update spec §7); progress 0..1. */
    suspend fun downloadAndInstall(release: Release, onProgress: (Float) -> Unit): InstallResult
}

/** Who the verified APK was handed to (update spec §7). */
enum class InstallResult { FAILED, SYSTEM_INSTALLER, SHIZUKU }

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
