package io.github.ow3n.totch.ui

import io.github.ow3n.totch.AppInfo
import io.github.ow3n.totch.InstalledApps
import io.github.ow3n.totch.Requirements
import io.github.ow3n.totch.SettingsRepository
import io.github.ow3n.totch.SystemStatus
import io.github.ow3n.totch.TouchAreaPreview
import io.github.ow3n.totch.core.model.Rect
import io.github.ow3n.totch.core.settings.Settings
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

class FakeTouchAreaPreview(initial: Rect? = null, private val canShow: Boolean = true) : TouchAreaPreview {
    override val region: StateFlow<Rect?> = MutableStateFlow(initial)
    var showCalls = 0
    override fun show(durationMs: Long): Boolean {
        showCalls++
        return canShow
    }
}
