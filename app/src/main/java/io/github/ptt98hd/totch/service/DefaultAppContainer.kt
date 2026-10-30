package io.github.ptt98hd.totch.service

import android.app.Application
import android.os.SystemClock
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.SystemStatus
import io.github.ptt98hd.totch.Permission
import io.github.ptt98hd.totch.core.action.PermissionCommands
import io.github.ptt98hd.totch.AppActivities
import io.github.ptt98hd.totch.AppShortcuts
import io.github.ptt98hd.totch.AppIntents
import io.github.ptt98hd.totch.ContactSource
import io.github.ptt98hd.totch.SettingsPageSource
import io.github.ptt98hd.totch.TouchArea
import io.github.ptt98hd.totch.HighlightArea
import io.github.ptt98hd.totch.accessibility.AccessibilityBridge
import io.github.ptt98hd.totch.accessibility.AccessibilityExecutor
import io.github.ptt98hd.totch.action.AndroidFeedback
import io.github.ptt98hd.totch.action.ContentResolverContacts
import io.github.ptt98hd.totch.action.ManifestAppIntents
import io.github.ptt98hd.totch.action.PackageManagerSettingsPages
import io.github.ptt98hd.totch.action.LocalExecutor
import io.github.ptt98hd.totch.action.PackageManagerAppActivities
import io.github.ptt98hd.totch.action.PackageManagerAppShortcuts
import io.github.ptt98hd.totch.action.PackageManagerInstalledApps
import io.github.ptt98hd.totch.action.PackageManagerQuickSettingsTiles
import io.github.ptt98hd.totch.update.GitHubAppUpdater
import io.github.ptt98hd.totch.update.ShizukuInstaller
import io.github.ptt98hd.totch.update.UpdateController
import io.github.ptt98hd.totch.update.UpdateScheduler
import io.github.ptt98hd.totch.BuildConfig
import io.github.ptt98hd.totch.QuickSettingsTiles
import io.github.ptt98hd.totch.AppUpdater
import io.github.ptt98hd.totch.core.action.ActionDispatcher
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.data.DataStoreSettingsRepository
import io.github.ptt98hd.totch.data.settingsDataStore
import io.github.ptt98hd.totch.engine.GestureEngine
import io.github.ptt98hd.totch.engine.PipelineError
import io.github.ptt98hd.totch.engine.TouchSource
import io.github.ptt98hd.totch.engine.touchSource
import io.github.ptt98hd.totch.engine.detectedRegionDp
import io.github.ptt98hd.totch.engine.engineRegion
import io.github.ptt98hd.totch.engine.highlightArea
import io.github.ptt98hd.totch.engine.notchRegion
import io.github.ptt98hd.totch.engine.resolveEngineStatus
import io.github.ptt98hd.totch.locale.AppLocales
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.overlay.IslandActivity
import io.github.ptt98hd.totch.core.settings.activeIslandActivities
import io.github.ptt98hd.totch.overlay.MediaWatcher
import io.github.ptt98hd.totch.overlay.OverlayHub
import io.github.ptt98hd.totch.overlay.SystemEventsReceiver
import io.github.ptt98hd.totch.shizuku.ShizukuExecutor
import io.github.ptt98hd.totch.shizuku.ShizukuManager
import io.github.ptt98hd.totch.shizuku.ShizukuShell
import io.github.ptt98hd.totch.shizuku.ShizukuTouchPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Process-wide object graph; lives as long as the application process. */
class DefaultAppContainer(private val app: Application) : AppContainer {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val settingsRepository = DataStoreSettingsRepository(app.settingsDataStore)
    private val settings: StateFlow<Settings> =
        settingsRepository.settings.stateIn(scope, SharingStarted.Eagerly, Settings())

    /** Gestures are switched on, as last read from the repository. */
    val settingsEnabled: Boolean get() = settings.value.enabled

    /** Haptic feedback is on (FR-A10), as last read; also used for island taps (notch-overlay §3.4). */
    val hapticsEnabled: Boolean get() = settings.value.haptics

    val display = DisplayMonitor(app, scope)

    /** Notch region for the current settings and display, independent of `enabled`; `null` when rotated. */
    val region: StateFlow<Rect?> = combine(settings, display.state, ::notchRegion)
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Region the engine listens to, in natural coordinates; also set while rotated with landscape gestures (FR-R8). */
    private val engineRegion: StateFlow<Rect?> = combine(settings, display.state, ::engineRegion)
        .stateIn(scope, SharingStarted.Eagerly, null)

    val shizuku = ShizukuManager()
    val accessibility = AccessibilityBridge()
    override val installedApps = PackageManagerInstalledApps(app.packageManager)

    private val localExecutor = LocalExecutor(app, accessibility)

    val pipeline: ShizukuTouchPipeline = ShizukuTouchPipeline(app, shizuku, display.state, onTouch = { engine.onTouch(it) }, onReset = { engine.reset() })

    private val dispatcher = ActionDispatcher(
        listOf(
            localExecutor,
            AccessibilityExecutor(accessibility),
            ShizukuExecutor(
                pipeline.service,
                app.packageManager,
                display = { display.state.value },
                oneHandedMethod = { settings.value.oneHandedMethod },
                rememberOneHandedMethod = { method -> settingsRepository.update { it.copy(oneHandedMethod = method) } },
            ),
        ),
    )

    /** Notification indicator, Dynamic Island and touch effects (notch-overlay). */
    val overlay = OverlayHub(app.packageName, settings)
    val mediaWatcher = MediaWatcher(app, overlay)
    private val systemEvents = SystemEventsReceiver(app, overlay)

    val feedback = AndroidFeedback(app, installedApps)

    val engine: GestureEngine = GestureEngine(
        scope = scope,
        clock = SystemClock::uptimeMillis, // same clock as MotionEvent.eventTime and getevent (CLOCK_MONOTONIC)
        density = display.state.value.density,
        dispatch = { action ->
            if (action == Action.Notifications || action == Action.QuickSettings) overlay.onShadeOpened() // NI-5
            dispatcher.dispatch(action)
        },
        feedback = feedback,
        observer = overlay,
    )

    private val accessibilityConnected: StateFlow<Boolean> = accessibility.service.map { it != null }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** Where touches come from: Shizuku's getevent or the accessibility overlay (FR-D13, DM-40). */
    val touchSource: StateFlow<TouchSource> = combine(shizuku.state, pipeline.state, accessibilityConnected, ::touchSource)
        .stateIn(scope, SharingStarted.Eagerly, TouchSource.NONE)

    override val engineStatus: StateFlow<EngineStatus> = combine(
        settings,
        shizuku.state,
        pipeline.state,
        display.state.map { it.rotation },
        accessibilityConnected,
    ) { s, shizukuState, pipelineState, rotation, a11y ->
        resolveEngineStatus(s, shizukuState, pipelineState, rotation, a11y, ::errorMessage)
    }.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, EngineStatus.Disabled)

    private val shell = ShizukuShell(app, shizuku)
    override val systemStatus: SystemStatus = AndroidSystemStatus(app, shizuku, shell, accessibility, scope)
    override val touchArea: TouchArea = object : TouchArea {
        override val area: StateFlow<HighlightArea?> = combine(settings, display.state, ::highlightArea)
            .stateIn(scope, SharingStarted.Eagerly, null)
        override fun areaFor(settings: Settings): HighlightArea? = highlightArea(settings, display.state.value)
        override val detectedRegion: StateFlow<ManualRegionDp?> = display.state.map(::detectedRegionDp)
            .stateIn(scope, SharingStarted.Eagerly, detectedRegionDp(display.state.value))
    }

    override val appShortcuts: AppShortcuts = PackageManagerAppShortcuts(app.packageManager)
    override val appActivities: AppActivities = PackageManagerAppActivities(app.packageManager)
    override val quickSettingsTiles: QuickSettingsTiles = PackageManagerQuickSettingsTiles(app.packageManager, app.packageName)
    override val settingsPageSource: SettingsPageSource = PackageManagerSettingsPages(app.packageManager)
    override val appIntents: AppIntents = ManifestAppIntents(app)
    override val contactSource: ContactSource = ContentResolverContacts(app)
    override val appUpdater: AppUpdater = GitHubAppUpdater(app, ShizukuInstaller(app, shell)::install)
    override val updates = UpdateController(appUpdater, BuildConfig.VERSION_NAME, scope)

    init {
        scope.launch { combine(settings, display.state) { s, d -> s to d.density }.collect { (s, d) -> engine.updateSettings(s, d) } }
        scope.launch { engineRegion.collect { engine.region = it } }
        scope.launch { accessibilityConnected.collect { engine.canBlockTouches = it } }
        scope.launch { touchSource.collect { engine.reset() } } // a gesture never spans two sources
        scope.launch { localExecutor.warmUp() }
        scope.launch {
            settings.map { s -> s.enabled && s.activeIslandActivities().any { it == IslandActivity.CHARGING || it == IslandActivity.SOUND } }
                .distinctUntilChanged()
                .collect { systemEvents.setRegistered(it) }
        }
        scope.launch { syncLanguage() }
        scope.launch { settingsRepository.settings.map { it.updateInterval }.distinctUntilChanged().collect { UpdateScheduler.apply(app, it) } } // UP-11
        // Repository flow (not the defaulted StateFlow) so a sticky restart is never stopped by a placeholder value.
        scope.launch {
            settingsRepository.settings
                .map { it.enabled }
                .distinctUntilChanged()
                .collect {
                    NotchForegroundService.setRunning(app, it) // DM-1, DM-7
                    KeepAliveScheduler.apply(app, it) // DM-50
                }
        }
        // DM-55: on process start and whenever Shizuku becomes ready.
        scope.launch {
            shizuku.state.map { it.ready }.distinctUntilChanged().collect { ready ->
                if (ready) grantWriteSecureSettings()
                restoreAccessibility()
            }
        }
    }

    /** FR-D14: what the watchdog, boot and update receivers do; the service start is a no-op when it runs. */
    suspend fun keepAlive() {
        if (settingsRepository.settings.first().enabled) NotchForegroundService.setRunning(app, true)
        restoreAccessibility()
    }

    /** FR-D15, DM-54: turns accessibility back on when Android dropped it but the user did not switch it off. */
    private suspend fun restoreAccessibility() {
        val method = AccessibilityKeeper.decide(
            wanted = settingsRepository.settings.first().accessibilityWanted,
            enabled = AccessibilityKeeper.isEnabled(app),
            canWriteSecure = AccessibilityKeeper.canWriteSecureSettings(app),
            shizukuReady = shizuku.state.value.ready,
        )
        when (method) {
            RestoreMethod.WRITE -> AccessibilityKeeper.write(app, true)
            RestoreMethod.SHIZUKU -> systemStatus.setPermission(Permission.ACCESSIBILITY, true)
            RestoreMethod.NONE -> return
        }
        systemStatus.refresh()
    }

    /** PC-10: once, while Shizuku is available, so accessibility can be restored without it after a reboot. */
    private suspend fun grantWriteSecureSettings() {
        if (!AccessibilityKeeper.canWriteSecureSettings(app)) {
            shell.exec(listOf(PermissionCommands.grantWriteSecureSettings(app.packageName)))
        }
    }

    /** DM-53: the accessibility service records whether the user wants it on. */
    fun setAccessibilityWanted(wanted: Boolean) {
        scope.launch {
            settingsRepository.update { if (it.accessibilityWanted == wanted) it else it.copy(accessibilityWanted = wanted) }
        }
    }

    /**
     * LG-3: a language picked in the system per-app settings (Android 13+) wins once at start; afterwards
     * the saved language is pushed to the system (LG-2).
     */
    private suspend fun syncLanguage() {
        val saved = settingsRepository.settings.first().language
        val system = AppLocales.systemSelection(app)
        if (system != null && system != saved) settingsRepository.update { it.copy(language = system) }
        settingsRepository.settings.map { it.language }.distinctUntilChanged().collect { AppLocales.apply(app, it) }
    }

    private fun errorMessage(error: PipelineError): String = app.getString(
        when (error) {
            PipelineError.NO_TOUCHSCREEN -> R.string.platform_error_no_touchscreen
            PipelineError.STREAM_ENDED -> R.string.platform_error_stream_ended
            PipelineError.SERVICE_FAILED -> R.string.platform_error_service_failed
        },
    )
}
