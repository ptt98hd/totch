package io.github.ow3n.totch.ui.gestures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ow3n.totch.AppContainer
import io.github.ow3n.totch.AppInfo
import io.github.ow3n.totch.InstalledApps
import io.github.ow3n.totch.SettingsRepository
import io.github.ow3n.totch.core.model.Action
import io.github.ow3n.totch.core.model.Gesture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppPickerUiState(
    val gesture: Gesture,
    val query: String = "",
    val apps: List<AppInfo> = emptyList(),
    val loading: Boolean = true,
    /** True once an app was chosen and saved; the screen should navigate back. */
    val finished: Boolean = false,
)

private val labelOrder: Comparator<AppInfo> =
    compareBy<AppInfo, String>(String.CASE_INSENSITIVE_ORDER) { it.label }.thenBy { it.packageName }

/** UI-11: case-insensitive match on label or package name; result keeps label order. */
internal fun List<AppInfo>.filterApps(query: String): List<AppInfo> {
    val q = query.trim()
    val sorted = sortedWith(labelOrder)
    if (q.isEmpty()) return sorted
    return sorted.filter { it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true) }
}

class AppPickerViewModel(
    private val gesture: Gesture,
    private val settingsRepository: SettingsRepository,
    private val installedApps: InstalledApps,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val allApps = MutableStateFlow<List<AppInfo>?>(null)
    private val query = MutableStateFlow("")
    private val finished = MutableStateFlow(false)

    val state: StateFlow<AppPickerUiState> =
        combine(allApps, query, finished) { apps, q, done ->
            AppPickerUiState(
                gesture = gesture,
                query = q,
                apps = apps?.filterApps(q).orEmpty(),
                loading = apps == null,
                finished = done,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPickerUiState(gesture))

    init {
        viewModelScope.launch {
            allApps.value = withContext(ioDispatcher) { installedApps.launchableApps() }
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun select(app: AppInfo) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(bindings = it.bindings + (gesture to Action.LaunchApp(app.packageName))) }
            finished.value = true
        }
    }

    companion object {
        fun factory(container: AppContainer, gesture: Gesture) = viewModelFactory {
            initializer { AppPickerViewModel(gesture, container.settingsRepository, container.installedApps) }
        }
    }
}
