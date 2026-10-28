package io.github.ptt98hd.totch.service

import android.app.Application
import android.os.SystemClock
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.SystemStatus
import io.github.ptt98hd.totch.AppShortcuts
import io.github.ptt98hd.totch.ShortcutEntry
import io.github.ptt98hd.totch.TouchArea
import io.github.ptt98hd.totch.accessibility.AccessibilityBridge
import io.github.ptt98hd.totch.accessibility.AccessibilityExecutor
import io.github.ptt98hd.totch.action.AndroidFeedback
import io.github.ptt98hd.totch.action.LocalExecutor
import io.github.ptt98hd.totch.action.PackageManagerInstalledApps
import io.github.ptt98hd.totch.core.action.ActionDispatcher
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.data.DataStoreSettingsRepository
import io.github.ptt98hd.totch.data.settingsDataStore
import io.github.ptt98hd.totch.engine.GestureEngine
import io.github.ptt98hd.totch.engine.PipelineError
import io.github.ptt98hd.totch.engine.notchRegion
import io.github.ptt98hd.totch.engine.resolveEngineStatus
import io.github.ptt98hd.totch.shizuku.ShizukuExecutor
import io.github.ptt98hd.totch.shizuku.ShizukuManager
import io.github.ptt98hd.totch.shizuku.ShizukuTouchPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
        accessibility.service.map { it != null },
        display.state.map { it.rotation },
    ) { s, shizukuState, pipelineState, a11y, rotation ->
        resolveEngineStatus(s, shizukuState, pipelineState, a11y, rotation, ::errorMessage)
    }.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, EngineStatus.Disabled)

    override val systemStatus: SystemStatus = AndroidSystemStatus(app, shizuku, accessibility, scope)
    override val touchArea: TouchArea = object : TouchArea {
        override val region: StateFlow<Rect?> = this@DefaultAppContainer.region
        override fun regionFor(settings: Settings): Rect? = notchRegion(settings, display.state.value)
    }

    // TODO(platform): static shortcut parser (actions spec §7).
    override val appShortcuts: AppShortcuts = object : AppShortcuts {
        override suspend fun shortcuts(): List<ShortcutEntry> = emptyList()
    }

    init {
        scope.launch { combine(settings, display.state) { s, d -> s to d.density }.collect { (s, d) -> engine.updateSettings(s, d) } }
        scope.launch { region.collect { engine.region = it } }
        scope.launch { localExecutor.warmUp() }
        // Repository flow (not the defaulted StateFlow) so a sticky restart is never stopped by a placeholder value.
        scope.launch {
            settingsRepository.settings
                .map { it.enabled && it.mode == DetectionMode.SHIZUKU }
                .distinctUntilChanged()
                .collect { NotchForegroundService.setRunning(app, it) } // DM-1, DM-7
        }
    }

    private fun errorMessage(error: PipelineError): String = app.getString(
        when (error) {
            PipelineError.NO_TOUCHSCREEN -> R.string.platform_error_no_touchscreen
            PipelineError.STREAM_ENDED -> R.string.platform_error_stream_ended
            PipelineError.SERVICE_FAILED -> R.string.platform_error_service_failed
        },
    )
}
