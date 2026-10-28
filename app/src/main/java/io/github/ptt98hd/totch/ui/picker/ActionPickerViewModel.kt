package io.github.ptt98hd.totch.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.AppShortcuts
import io.github.ptt98hd.totch.InstalledApps
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.ShortcutEntry
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.actionFor
import io.github.ptt98hd.totch.core.settings.withBinding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PickerTab { SYSTEM, APPS, SHORTCUTS }

data class ShortcutGroup(val packageName: String, val appLabel: String, val shortcuts: List<ShortcutEntry>)

/** UI-20: the tab that holds [action]. */
fun initialTab(action: Action): PickerTab = when (action) {
    is Action.LaunchApp -> PickerTab.APPS
    is Action.LaunchShortcut -> PickerTab.SHORTCUTS
    else -> PickerTab.SYSTEM
}

private val labelOrder: Comparator<AppInfo> =
    compareBy<AppInfo, String>(String.CASE_INSENSITIVE_ORDER) { it.label }.thenBy { it.packageName }

/** UI-11: case-insensitive match on label or package name; result keeps label order. */
internal fun List<AppInfo>.filterApps(query: String): List<AppInfo> {
    val q = query.trim()
    val sorted = sortedWith(labelOrder)
    if (q.isEmpty()) return sorted
    return sorted.filter { it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true) }
}

/** SH-1, SH-2: shortcuts grouped per app; a query matching the app label keeps the whole group. */
fun groupShortcuts(entries: List<ShortcutEntry>, query: String): List<ShortcutGroup> {
    val q = query.trim()
    return entries.groupBy { it.packageName }
        .map { (pkg, items) ->
            val appLabel = items.first().appLabel
            val appMatches = q.isEmpty() || appLabel.contains(q, ignoreCase = true)
            val shortcuts = items
                .filter { appMatches || it.label.contains(q, ignoreCase = true) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            ShortcutGroup(pkg, appLabel, shortcuts)
        }
        .filter { it.shortcuts.isNotEmpty() }
        .sortedWith(compareBy<ShortcutGroup, String>(String.CASE_INSENSITIVE_ORDER) { it.appLabel }.thenBy { it.packageName })
}

data class ActionPickerUiState(
    val gesture: Gesture,
    val current: Action = Action.None,
    val mode: DetectionMode = DetectionMode.ACCESSIBILITY,
    val tab: PickerTab = PickerTab.SYSTEM,
    val systemQuery: String = "",
    val appQuery: String = "",
    val apps: List<AppInfo> = emptyList(),
    val appsLoading: Boolean = true,
    val shortcutQuery: String = "",
    val shortcutGroups: List<ShortcutGroup> = emptyList(),
    val shortcutsLoading: Boolean = true,
    /** True once an action was chosen and saved; the screen should navigate back. */
    val finished: Boolean = false,
)

private data class Local(
    val tab: PickerTab? = null,
    val systemQuery: String = "",
    val appQuery: String = "",
    val apps: List<AppInfo>? = null,
    val shortcutQuery: String = "",
    val shortcuts: List<ShortcutEntry>? = null,
    val finished: Boolean = false,
)

class ActionPickerViewModel(
    private val gesture: Gesture,
    private val settingsRepository: SettingsRepository,
    private val installedApps: InstalledApps,
    private val appShortcuts: AppShortcuts,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val local = MutableStateFlow(Local())
    private var appsRequested = false
    private var shortcutsRequested = false

    val state: StateFlow<ActionPickerUiState> =
        combine(settingsRepository.settings, local) { settings: Settings, l ->
            val current = settings.actionFor(gesture)
            ActionPickerUiState(
                gesture = gesture,
                current = current,
                mode = settings.mode,
                tab = l.tab ?: initialTab(current),
                systemQuery = l.systemQuery,
                appQuery = l.appQuery,
                apps = l.apps?.filterApps(l.appQuery).orEmpty(),
                appsLoading = l.apps == null,
                shortcutQuery = l.shortcutQuery,
                shortcutGroups = l.shortcuts?.let { groupShortcuts(it, l.shortcutQuery) }.orEmpty(),
                shortcutsLoading = l.shortcuts == null,
                finished = l.finished,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ActionPickerUiState(gesture))

    init {
        viewModelScope.launch {
            val tab = initialTab(settingsRepository.settings.first().actionFor(gesture))
            if (local.value.tab == null) selectTab(tab)
        }
    }

    /** UI-22: apps and shortcuts are loaded once, the first time their tab is shown. */
    fun selectTab(tab: PickerTab) {
        local.update { it.copy(tab = tab) }
        when (tab) {
            PickerTab.APPS -> if (!appsRequested) {
                appsRequested = true
                viewModelScope.launch {
                    val apps = withContext(ioDispatcher) { installedApps.launchableApps() }
                    local.update { it.copy(apps = apps) }
                }
            }
            PickerTab.SHORTCUTS -> if (!shortcutsRequested) {
                shortcutsRequested = true
                viewModelScope.launch {
                    val shortcuts = withContext(ioDispatcher) { appShortcuts.shortcuts() }
                    local.update { it.copy(shortcuts = shortcuts) }
                }
            }
            PickerTab.SYSTEM -> Unit
        }
    }

    fun setSystemQuery(value: String) = local.update { it.copy(systemQuery = value) }

    fun setAppQuery(value: String) = local.update { it.copy(appQuery = value) }

    fun setShortcutQuery(value: String) = local.update { it.copy(shortcutQuery = value) }

    /** UI-10: saves the binding, switches the gesture on and finishes. */
    fun select(action: Action) {
        viewModelScope.launch {
            settingsRepository.update { it.withBinding(gesture, action) }
            local.update { it.copy(finished = true) }
        }
    }

    fun selectApp(app: AppInfo) = select(Action.LaunchApp(app.packageName))

    /** UI-21 */
    fun selectShortcut(entry: ShortcutEntry) =
        select(Action.LaunchShortcut(entry.packageName, entry.id, entry.label, entry.intentUri))

    companion object {
        fun factory(container: AppContainer, gesture: Gesture) = viewModelFactory {
            initializer {
                ActionPickerViewModel(gesture, container.settingsRepository, container.installedApps, container.appShortcuts)
            }
        }
    }
}
