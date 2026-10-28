package io.github.ptt98hd.totch.service

import android.app.Application
import android.os.SystemClock
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.SystemStatus
import io.github.ptt98hd.totch.AppShortcuts
import io.github.ptt98hd.totch.TouchArea
import io.github.ptt98hd.totch.HighlightArea
import io.github.ptt98hd.totch.accessibility.AccessibilityBridge
import io.github.ptt98hd.totch.accessibility.AccessibilityExecutor
import io.github.ptt98hd.totch.action.AndroidFeedback
import io.github.ptt98hd.totch.action.LocalExecutor
import io.github.ptt98hd.totch.action.PackageManagerAppShortcuts
import io.github.ptt98hd.totch.action.PackageManagerInstalledApps
import io.github.ptt98hd.totch.core.action.ActionDispatcher
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.data.DataStoreSettingsRepository
import io.github.ptt98hd.totch.data.settingsDataStore
import io.github.ptt98hd.totch.engine.GestureEngine
import io.github.ptt98hd.totch.engine.PipelineError
import io.github.ptt98hd.totch.engine.detectedRegionDp
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

    val shizuku = ShizukuManager()
    val accessibility = AccessibilityBridge()
    override val installedApps = PackageManagerInstalledApps(app.packageManager)

    private val localExecutor = LocalExecutor(app, accessibility)

    val pipeline: ShizukuTouchPipeline = ShizukuTouchPipeline(app, shizuku, display.state, onTouch = { engine.onTouch(it) }, onReset = { engine.reset() })

    private val dispatcher = ActionDispatcher(
        listOf(localExecutor, AccessibilityExecutor(accessibility), ShizukuExecutor(pipeline.service, app.packageManager)),
    )

    val engine: GestureEngine = GestureEngine(
        scope = scope,
        clock = SystemClock::uptimeMillis, // same clock as MotionEvent.eventTime and getevent (CLOCK_MONOTONIC)
        density = display.state.value.density,
        dispatch = dispatcher::dispatch,
        feedback = AndroidFeedback(app, installedApps),
    )

    override val engineStatus: StateFlow<EngineStatus> = combine(
        settings,
        shizuku.state,
        pipeline.state,
        display.state.map { it.rotation },
    ) { s, shizukuState, pipelineState, rotation ->
        resolveEngineStatus(s, shizukuState, pipelineState, rotation, ::errorMessage)
    }.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, EngineStatus.Disabled)

    override val systemStatus: SystemStatus = AndroidSystemStatus(app, shizuku, ShizukuShell(app, shizuku), accessibility, scope)
    override val touchArea: TouchArea = object : TouchArea {
        override val area: StateFlow<HighlightArea?> = combine(settings, display.state, ::highlightArea)
            .stateIn(scope, SharingStarted.Eagerly, null)
        override fun areaFor(settings: Settings): HighlightArea? = highlightArea(settings, display.state.value)
        override val detectedRegion: StateFlow<ManualRegionDp?> = display.state.map(::detectedRegionDp)
            .stateIn(scope, SharingStarted.Eagerly, detectedRegionDp(display.state.value))
    }

    override val appShortcuts: AppShortcuts = PackageManagerAppShortcuts(app.packageManager)

    init {
        scope.launch { combine(settings, display.state) { s, d -> s to d.density }.collect { (s, d) -> engine.updateSettings(s, d) } }
        scope.launch { region.collect { engine.region = it } }
        scope.launch { localExecutor.warmUp() }
        scope.launch { syncLanguage() }
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
