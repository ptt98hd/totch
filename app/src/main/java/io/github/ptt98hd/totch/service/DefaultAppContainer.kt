package io.github.ptt98hd.totch.service

import android.app.Application
import android.os.SystemClock
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.SystemStatus
import io.github.ptt98hd.totch.AppActivities
import io.github.ptt98hd.totch.AppShortcuts
import io.github.ptt98hd.totch.TouchArea
import io.github.ptt98hd.totch.HighlightArea
import io.github.ptt98hd.totch.accessibility.AccessibilityBridge
import io.github.ptt98hd.totch.accessibility.AccessibilityExecutor
import io.github.ptt98hd.totch.action.AndroidFeedback
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

    val engine: GestureEngine = GestureEngine(
        scope = scope,
        clock = SystemClock::uptimeMillis, // same clock as MotionEvent.eventTime and getevent (CLOCK_MONOTONIC)
        density = display.state.value.density,
        dispatch = dispatcher::dispatch,
        feedback = AndroidFeedback(app, installedApps),
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
    override val appUpdater: AppUpdater = GitHubAppUpdater(app, ShizukuInstaller(app, shell)::install)
    override val updates = UpdateController(appUpdater, BuildConfig.VERSION_NAME, scope)

    init {
        scope.launch { combine(settings, display.state) { s, d -> s to d.density }.collect { (s, d) -> engine.updateSettings(s, d) } }
        scope.launch { engineRegion.collect { engine.region = it } }
        scope.launch { accessibilityConnected.collect { engine.canBlockTouches = it } }
        scope.launch { touchSource.collect { engine.reset() } } // a gesture never spans two sources
        scope.launch { localExecutor.warmUp() }
        scope.launch { syncLanguage() }
        scope.launch { settingsRepository.settings.map { it.updateInterval }.distinctUntilChanged().collect { UpdateScheduler.apply(app, it) } } // UP-11
        // Repository flow (not the defaulted StateFlow) so a sticky restart is never stopped by a placeholder value.
        scope.launch {
            settingsRepository.settings
                .map { it.enabled }
                .distinctUntilChanged()
                .collect { NotchForegroundService.setRunning(app, it) } // DM-1, DM-7
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
