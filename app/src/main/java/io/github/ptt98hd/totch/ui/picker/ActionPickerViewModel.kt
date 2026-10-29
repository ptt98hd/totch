package io.github.ptt98hd.totch.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ptt98hd.totch.ActivityEntry
import io.github.ptt98hd.totch.AppActivities
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.AppShortcuts
import io.github.ptt98hd.totch.InstalledApps
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.ShortcutEntry
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
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

enum class PickerTab { SYSTEM, APPS, SHORTCUTS, ACTIVITIES }

data class ShortcutGroup(val packageName: String, val appLabel: String, val shortcuts: List<ShortcutEntry>)

data class ActivityGroup(val packageName: String, val appLabel: String, val activities: List<ActivityEntry>)

/** UI-20: the tab that holds [action]. */
fun initialTab(action: Action): PickerTab = when (action) {
    is Action.LaunchApp -> PickerTab.APPS
    is Action.LaunchShortcut -> PickerTab.SHORTCUTS
    is Action.LaunchActivity -> PickerTab.ACTIVITIES
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

/** AC-3: class name without the package prefix (`.ui.Main`), or the full name when it lives elsewhere. */
fun shortActivityName(packageName: String, name: String): String =
    if (name.startsWith("$packageName.")) name.removePrefix(packageName) else name

/** AC-1, AC-2: activities grouped per app; a query matching the app label keeps the whole group. */
fun groupActivities(entries: List<ActivityEntry>, query: String): List<ActivityGroup> {
    val q = query.trim()
    return entries.groupBy { it.packageName }
        .map { (pkg, items) ->
            val appLabel = items.first().appLabel
            val appMatches = q.isEmpty() || appLabel.contains(q, ignoreCase = true)
            val activities = items
                .filter { appMatches || it.label.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
                .sortedWith(
                    compareBy<ActivityEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
                )
            ActivityGroup(pkg, appLabel, activities)
        }
        .filter { it.activities.isNotEmpty() }
        .sortedWith(compareBy<ActivityGroup, String>(String.CASE_INSENSITIVE_ORDER) { it.appLabel }.thenBy { it.packageName })
}

data class ActionPickerUiState(
    val gesture: Gesture,
    val current: Action = Action.None,
    val tab: PickerTab = PickerTab.SYSTEM,
    val appQuery: String = "",
    val apps: List<AppInfo> = emptyList(),
    val appsLoading: Boolean = true,
    val shortcutQuery: String = "",
    val shortcutGroups: List<ShortcutGroup> = emptyList(),
    val shortcutsLoading: Boolean = true,
    /** Apps of the Shortcuts tab whose shortcuts are shown; all collapsed at first (UI-25). */
    val expanded: Set<String> = emptySet(),
    val activityQuery: String = "",
    val activityGroups: List<ActivityGroup> = emptyList(),
    val activitiesLoading: Boolean = true,
    /** Apps of the Activities tab whose activities are shown (UI-27). */
    val expandedActivities: Set<String> = emptySet(),
    /** True once an action was chosen and saved; the screen should navigate back. */
    val finished: Boolean = false,
) {
    /** UI-25: while searching, every matching app is shown expanded. */
    fun isExpanded(packageName: String): Boolean = shortcutQuery.isNotBlank() || packageName in expanded

    /** UI-27: as [isExpanded], for the Activities tab. */
    fun isActivityAppExpanded(packageName: String): Boolean = activityQuery.isNotBlank() || packageName in expandedActivities
}

private data class Local(
    val tab: PickerTab? = null,
    val appQuery: String = "",
    val apps: List<AppInfo>? = null,
    val shortcutQuery: String = "",
    val shortcuts: List<ShortcutEntry>? = null,
    val expanded: Set<String> = emptySet(),
    val activityQuery: String = "",
    val activities: List<ActivityEntry>? = null,
    val expandedActivities: Set<String> = emptySet(),
    val finished: Boolean = false,
)

class ActionPickerViewModel(
    private val gesture: Gesture,
    private val settingsRepository: SettingsRepository,
    private val installedApps: InstalledApps,
    private val appShortcuts: AppShortcuts,
    private val appActivities: AppActivities,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val local = MutableStateFlow(Local())
    private var appsRequested = false
    private var shortcutsRequested = false
    private var activitiesRequested = false

    val state: StateFlow<ActionPickerUiState> =
        combine(settingsRepository.settings, local) { settings: Settings, l ->
            val current = settings.actionFor(gesture)
            ActionPickerUiState(
                gesture = gesture,
                current = current,
                tab = l.tab ?: initialTab(current),
                appQuery = l.appQuery,
                apps = l.apps?.filterApps(l.appQuery).orEmpty(),
                appsLoading = l.apps == null,
                shortcutQuery = l.shortcutQuery,
                shortcutGroups = l.shortcuts?.let { groupShortcuts(it, l.shortcutQuery) }.orEmpty(),
                shortcutsLoading = l.shortcuts == null,
                expanded = l.expanded,
                activityQuery = l.activityQuery,
                activityGroups = l.activities?.let { groupActivities(it, l.activityQuery) }.orEmpty(),
                activitiesLoading = l.activities == null,
                expandedActivities = l.expandedActivities,
                finished = l.finished,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ActionPickerUiState(gesture))

    init {
        viewModelScope.launch {
            val tab = initialTab(settingsRepository.settings.first().actionFor(gesture))
            if (local.value.tab == null) selectTab(tab)
        }
    }

    /** UI-22, UI-27: apps, shortcuts and activities are loaded once, the first time their tab is shown. */
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
            PickerTab.ACTIVITIES -> if (!activitiesRequested) {
                activitiesRequested = true
                viewModelScope.launch {
                    val activities = withContext(ioDispatcher) { appActivities.activities() }
                    local.update { it.copy(activities = activities) }
                }
            }
            PickerTab.SYSTEM -> Unit
        }
    }

    fun setAppQuery(value: String) = local.update { it.copy(appQuery = value) }

    fun setShortcutQuery(value: String) = local.update { it.copy(shortcutQuery = value) }

    fun toggleExpanded(packageName: String) = local.update {
        it.copy(expanded = if (packageName in it.expanded) it.expanded - packageName else it.expanded + packageName)
    }

    fun setActivityQuery(value: String) = local.update { it.copy(activityQuery = value) }

    fun toggleActivityApp(packageName: String) = local.update {
        val expanded = it.expandedActivities
        it.copy(expandedActivities = if (packageName in expanded) expanded - packageName else expanded + packageName)
    }

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

    /** UI-27 */
    fun selectActivity(entry: ActivityEntry) = select(Action.LaunchActivity(entry.packageName, entry.name, entry.label))

    companion object {
        fun factory(container: AppContainer, gesture: Gesture) = viewModelFactory {
            initializer {
                ActionPickerViewModel(
                    gesture,
                    container.settingsRepository,
                    container.installedApps,
                    container.appShortcuts,
                    container.appActivities,
                )
            }
        }
    }
}
