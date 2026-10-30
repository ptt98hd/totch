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
import io.github.ptt98hd.totch.AppIntents
import io.github.ptt98hd.totch.ContactEntry
import io.github.ptt98hd.totch.ContactSource
import io.github.ptt98hd.totch.IntentEntry
import io.github.ptt98hd.totch.SettingsPageSource
import io.github.ptt98hd.totch.core.action.IntentFilters
import io.github.ptt98hd.totch.core.action.SettingsGroup
import io.github.ptt98hd.totch.core.action.SettingsPages
import io.github.ptt98hd.totch.InstalledApps
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.ShortcutEntry
import io.github.ptt98hd.totch.QuickSettingsTiles
import io.github.ptt98hd.totch.Requirements
import io.github.ptt98hd.totch.TileEntry
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.actionFor
import io.github.ptt98hd.totch.core.settings.withBinding
import io.github.ptt98hd.totch.ui.common.Capabilities
import io.github.ptt98hd.totch.ui.common.Need
import io.github.ptt98hd.totch.ui.common.capabilities
import io.github.ptt98hd.totch.ui.common.searchKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI-66: BASIC (FR-U46, formerly System) holds Nothing and the fixed actions; UI-73: the order of the type menu. */
enum class PickerTab { BASIC, SETTINGS, APPS, SHORTCUTS, ACTIVITIES, INTENTS, TILES, CONTACTS }

data class ShortcutGroup(val packageName: String, val appLabel: String, val shortcuts: List<ShortcutEntry>)

data class ActivityGroup(val packageName: String, val appLabel: String, val activities: List<ActivityEntry>)

data class IntentGroup(val packageName: String, val appLabel: String, val intents: List<IntentEntry>)

/** UI-74: the available pages of [group], in `SettingsPages` order. */
data class SettingsPageGroup(val group: SettingsGroup, val intentActions: List<String>)

/** UI-20: the tab that holds [action]. */
fun initialTab(action: Action): PickerTab = when (action) {
    is Action.LaunchApp -> PickerTab.APPS
    is Action.LaunchShortcut -> PickerTab.SHORTCUTS
    is Action.LaunchActivity -> PickerTab.ACTIVITIES
    is Action.ClickTile -> PickerTab.TILES
    is Action.OpenSettings -> PickerTab.SETTINGS
    is Action.SendIntent -> PickerTab.INTENTS
    is Action.OpenContact -> PickerTab.CONTACTS
    else -> PickerTab.BASIC
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

/** IN-6: intents grouped per app; a query matching the app label keeps the whole group. */
fun groupIntents(entries: List<IntentEntry>, query: String): List<IntentGroup> {
    val q = query.trim()
    return entries.groupBy { it.packageName }
        .map { (pkg, items) ->
            val appLabel = items.first().appLabel
            val appMatches = q.isEmpty() || appLabel.contains(q, ignoreCase = true)
            val intents = items
                .filter { appMatches || it.intentAction.contains(q, ignoreCase = true) || it.className.contains(q, ignoreCase = true) }
                .sortedWith(
                    compareBy<IntentEntry, String>(String.CASE_INSENSITIVE_ORDER) { IntentFilters.shortIntentAction(it.intentAction) }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.intentAction }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.className },
                )
            IntentGroup(pkg, appLabel, intents)
        }
        .filter { it.intents.isNotEmpty() }
        .sortedWith(compareBy<IntentGroup, String>(String.CASE_INSENSITIVE_ORDER) { it.appLabel }.thenBy { it.packageName })
}

/** CT-1: contacts by normalized name; the query ignores case and Vietnamese marks. */
fun filterContacts(entries: List<ContactEntry>, query: String): List<ContactEntry> {
    val q = searchKey(query)
    return entries
        .filter { q.isEmpty() || q in searchKey(it.name) }
        .sortedWith(compareBy<ContactEntry> { searchKey(it.name) }.thenBy { it.lookupUri })
}

/** UI-74: groups of the available pages, empty groups dropped. */
internal fun settingsPageGroups(available: Set<String>): List<SettingsPageGroup> =
    SettingsPages.all.filter { it.intentAction in available }
        .groupBy { it.group }
        .map { (group, pages) -> SettingsPageGroup(group, pages.map { it.intentAction }) }

/** QT-1: tiles by label then app label; the query matches either (case-insensitive). */
fun filterTiles(entries: List<TileEntry>, query: String): List<TileEntry> {
    val q = query.trim()
    return entries
        .filter { q.isEmpty() || it.label.contains(q, ignoreCase = true) || it.appLabel.contains(q, ignoreCase = true) }
        .sortedWith(
            compareBy<TileEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel },
        )
}

data class ActionPickerUiState(
    val gesture: Gesture,
    val current: Action = Action.None,
    val tab: PickerTab = PickerTab.BASIC,
    /** Search of the Basic tab, applied with [filterCatalog] where the labels are known (UI-24, UI-29). */
    val basicQuery: String = "",
    /** What can run right now (UI-62); every action is available by default. */
    val capabilities: Capabilities = Capabilities(),
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
    val tileQuery: String = "",
    val tiles: List<TileEntry> = emptyList(),
    val tilesLoading: Boolean = true,
    /** Search of the Settings tab, applied with [io.github.ptt98hd.totch.ui.common.filterGroups] where labels are known. */
    val settingsQuery: String = "",
    val settingsGroups: List<SettingsPageGroup> = emptyList(),
    val settingsLoading: Boolean = true,
    val intentQuery: String = "",
    val intentGroups: List<IntentGroup> = emptyList(),
    val intentsLoading: Boolean = true,
    /** Apps of the Intents tab whose intents are shown (UI-75). */
    val expandedIntents: Set<String> = emptySet(),
    val contactQuery: String = "",
    val contacts: List<ContactEntry> = emptyList(),
    val contactsLoading: Boolean = true,
    /** `READ_CONTACTS` is granted (UI-76); set by the screen. */
    val contactsPermission: Boolean = false,
    /** True once an action was chosen and saved; the screen should navigate back. */
    val finished: Boolean = false,
) {
    /** UI-29, UI-29b, UI-62: why [action] cannot run (its row is disabled with this reason), null when it can. */
    fun need(action: Action): Need? = capabilities.need(action)

    /** UI-71: the search of the current tab; each tab keeps its own. */
    val query: String
        get() = when (tab) {
            PickerTab.BASIC -> basicQuery
            PickerTab.APPS -> appQuery
            PickerTab.SHORTCUTS -> shortcutQuery
            PickerTab.ACTIVITIES -> activityQuery
            PickerTab.TILES -> tileQuery
            PickerTab.SETTINGS -> settingsQuery
            PickerTab.INTENTS -> intentQuery
            PickerTab.CONTACTS -> contactQuery
        }

    /** UI-25: while searching, every matching app is shown expanded. */
    fun isExpanded(packageName: String): Boolean = shortcutQuery.isNotBlank() || packageName in expanded

    /** UI-27: as [isExpanded], for the Activities tab. */
    fun isActivityAppExpanded(packageName: String): Boolean = activityQuery.isNotBlank() || packageName in expandedActivities

    /** UI-75: as [isExpanded], for the Intents tab. */
    fun isIntentAppExpanded(packageName: String): Boolean = intentQuery.isNotBlank() || packageName in expandedIntents
}

private data class Local(
    val tab: PickerTab? = null,
    val basicQuery: String = "",
    val appQuery: String = "",
    val apps: List<AppInfo>? = null,
    val shortcutQuery: String = "",
    val shortcuts: List<ShortcutEntry>? = null,
    val expanded: Set<String> = emptySet(),
    val activityQuery: String = "",
    val activities: List<ActivityEntry>? = null,
    val expandedActivities: Set<String> = emptySet(),
    val tileQuery: String = "",
    val tiles: List<TileEntry>? = null,
    val settingsQuery: String = "",
    val settingsPages: Set<String>? = null,
    val intentQuery: String = "",
    val intents: List<IntentEntry>? = null,
    val expandedIntents: Set<String> = emptySet(),
    val contactQuery: String = "",
    val contacts: List<ContactEntry>? = null,
    val contactsPermission: Boolean = false,
    val finished: Boolean = false,
)

class ActionPickerViewModel(
    private val gesture: Gesture,
    private val settingsRepository: SettingsRepository,
    private val installedApps: InstalledApps,
    private val appShortcuts: AppShortcuts,
    private val appActivities: AppActivities,
    private val quickSettingsTiles: QuickSettingsTiles,
    private val settingsPageSource: SettingsPageSource,
    private val appIntents: AppIntents,
    private val contactSource: ContactSource,
    requirements: Flow<Requirements>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val local = MutableStateFlow(Local())
    private var appsRequested = false
    private var shortcutsRequested = false
    private var activitiesRequested = false
    private var tilesRequested = false
    private var settingsRequested = false
    private var intentsRequested = false
    private var contactsRequested = false

    val state: StateFlow<ActionPickerUiState> =
        combine(settingsRepository.settings, local, requirements) { settings: Settings, l, r ->
            val current = settings.actionFor(gesture)
            ActionPickerUiState(
                gesture = gesture,
                current = current,
                tab = l.tab ?: initialTab(current),
                basicQuery = l.basicQuery,
                capabilities = r.capabilities(),
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
                tileQuery = l.tileQuery,
                tiles = l.tiles?.let { filterTiles(it, l.tileQuery) }.orEmpty(),
                tilesLoading = l.tiles == null,
                settingsQuery = l.settingsQuery,
                settingsGroups = l.settingsPages?.let(::settingsPageGroups).orEmpty(),
                settingsLoading = l.settingsPages == null,
                intentQuery = l.intentQuery,
                intentGroups = l.intents?.let { groupIntents(it, l.intentQuery) }.orEmpty(),
                intentsLoading = l.intents == null,
                expandedIntents = l.expandedIntents,
                contactQuery = l.contactQuery,
                contacts = l.contacts?.let { filterContacts(it, l.contactQuery) }.orEmpty(),
                contactsLoading = l.contacts == null,
                contactsPermission = l.contactsPermission,
                finished = l.finished,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ActionPickerUiState(gesture))

    init {
        viewModelScope.launch {
            val tab = initialTab(settingsRepository.settings.first().actionFor(gesture))
            if (local.value.tab == null) selectTab(tab)
        }
    }

    /** UI-22, UI-27, UI-28, UI-74…UI-76: each list is loaded once, the first time its tab is shown. */
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
            PickerTab.TILES -> if (!tilesRequested) {
                tilesRequested = true
                viewModelScope.launch {
                    val tiles = withContext(ioDispatcher) { quickSettingsTiles.tiles() }
                    local.update { it.copy(tiles = tiles) }
                }
            }
            PickerTab.SETTINGS -> if (!settingsRequested) {
                settingsRequested = true
                viewModelScope.launch {
                    val pages = withContext(ioDispatcher) { settingsPageSource.available() }
                    local.update { it.copy(settingsPages = pages) }
                }
            }
            PickerTab.INTENTS -> if (!intentsRequested) {
                intentsRequested = true
                viewModelScope.launch {
                    val intents = withContext(ioDispatcher) { appIntents.intents() }
                    local.update { it.copy(intents = intents) }
                }
            }
            PickerTab.CONTACTS -> loadContacts()
            PickerTab.BASIC -> Unit
        }
    }

    /** UI-76: contacts need the tab open and the permission. */
    private fun loadContacts() {
        val l = local.value
        if (contactsRequested || !l.contactsPermission || l.tab != PickerTab.CONTACTS) return
        contactsRequested = true
        viewModelScope.launch {
            val contacts = withContext(ioDispatcher) { contactSource.contacts() }
            local.update { it.copy(contacts = contacts) }
        }
    }

    /** UI-76: the screen reports whether `READ_CONTACTS` is granted (on start, resume and after the request). */
    fun setContactsPermission(granted: Boolean) {
        local.update { it.copy(contactsPermission = granted) }
        loadContacts()
    }

    /** UI-71: sets the search of the current tab (FR-U58: one search field for every tab). */
    fun setQuery(value: String) = when (state.value.tab) {
        PickerTab.BASIC -> setBasicQuery(value)
        PickerTab.APPS -> setAppQuery(value)
        PickerTab.SHORTCUTS -> setShortcutQuery(value)
        PickerTab.ACTIVITIES -> setActivityQuery(value)
        PickerTab.TILES -> setTileQuery(value)
        PickerTab.SETTINGS -> local.update { it.copy(settingsQuery = value) }
        PickerTab.INTENTS -> local.update { it.copy(intentQuery = value) }
        PickerTab.CONTACTS -> local.update { it.copy(contactQuery = value) }
    }

    fun setBasicQuery(value: String) = local.update { it.copy(basicQuery = value) }

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

    fun setTileQuery(value: String) = local.update { it.copy(tileQuery = value) }

    fun toggleIntentApp(packageName: String) = local.update {
        val expanded = it.expandedIntents
        it.copy(expandedIntents = if (packageName in expanded) expanded - packageName else expanded + packageName)
    }

    /** UI-10: saves the binding, switches the gesture on and finishes; UI-62: unavailable actions are ignored. UI-65: None clears it, the gesture stays on. */
    fun select(action: Action) {
        if (state.value.need(action) != null) return
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

    /** UI-28 */
    fun selectTile(entry: TileEntry) = select(Action.ClickTile(entry.packageName, entry.name, entry.label))

    /** UI-75 */
    fun selectIntent(entry: IntentEntry) =
        select(Action.SendIntent(entry.packageName, entry.className, entry.intentAction, entry.target))

    /** UI-76 */
    fun selectContact(entry: ContactEntry) = select(Action.OpenContact(entry.lookupUri, entry.name))

    companion object {
        fun factory(container: AppContainer, gesture: Gesture) = viewModelFactory {
            initializer {
                ActionPickerViewModel(
                    gesture,
                    container.settingsRepository,
                    container.installedApps,
                    container.appShortcuts,
                    container.appActivities,
                    container.quickSettingsTiles,
                    container.settingsPageSource,
                    container.appIntents,
                    container.contactSource,
                    container.systemStatus.requirements,
                )
            }
        }
    }
}
