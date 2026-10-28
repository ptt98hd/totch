package io.github.ptt98hd.totch.ui

import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.InstalledApps
import io.github.ptt98hd.totch.Requirements
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.SystemStatus
import io.github.ptt98hd.totch.AppShortcuts
import io.github.ptt98hd.totch.ShortcutEntry
import io.github.ptt98hd.totch.TouchArea
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    private val state = MutableStateFlow(initial.sanitized())
    val current: Settings get() = state.value
    override val settings: StateFlow<Settings> = state
    override suspend fun update(transform: (Settings) -> Settings) {
        state.value = transform(state.value).sanitized()
    }
}

class FakeInstalledApps(private val apps: List<AppInfo> = emptyList()) : InstalledApps {
    override suspend fun launchableApps(): List<AppInfo> = apps
    override fun labelOf(packageName: String): String? = apps.firstOrNull { it.packageName == packageName }?.label
}

class FakeSystemStatus(initial: Requirements = Requirements()) : SystemStatus {
    override val requirements = MutableStateFlow(initial)
    var refreshCalls = 0
    var permissionRequests = 0
    override fun refresh() { refreshCalls++ }
    override fun requestShizukuPermission() { permissionRequests++ }
}

class FakeTouchArea(initial: Rect? = null, private val regionOf: (Settings) -> Rect? = { initial }) : TouchArea {
    override val region: StateFlow<Rect?> = MutableStateFlow(initial)
    override fun regionFor(settings: Settings): Rect? = regionOf(settings)
}

class FakeAppShortcuts(private val entries: List<ShortcutEntry> = emptyList()) : AppShortcuts {
    var loads = 0
    override suspend fun shortcuts(): List<ShortcutEntry> {
        loads++
        return entries
    }
}
