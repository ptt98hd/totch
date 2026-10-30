package io.github.ptt98hd.totch.ui.picker

import io.github.ptt98hd.totch.ui.common.settingsPageLabels
import io.github.ptt98hd.totch.ui.common.settingsGroupLabels
import io.github.ptt98hd.totch.ui.common.openAppDetails
import io.github.ptt98hd.totch.ui.common.filterGroups
import io.github.ptt98hd.totch.core.model.IntentTarget
import io.github.ptt98hd.totch.core.action.IntentFilters
import io.github.ptt98hd.totch.IntentEntry
import io.github.ptt98hd.totch.ContactEntry
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material.icons.outlined.Contacts
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.pm.PackageManager
import android.app.Activity
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.ActivityEntry
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.ShortcutEntry
import io.github.ptt98hd.totch.TileEntry
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.ui.common.ActionCatalog
import io.github.ptt98hd.totch.ui.common.ActionGroup
import io.github.ptt98hd.totch.ui.common.filterCatalog
import io.github.ptt98hd.totch.ui.common.searchKey
import io.github.ptt98hd.totch.ui.common.Gutter
import io.github.ptt98hd.totch.ui.common.SectionHeader
import io.github.ptt98hd.totch.ui.common.TonalIcon
import io.github.ptt98hd.totch.ui.common.TotchListItem
import io.github.ptt98hd.totch.ui.common.groupItem
import io.github.ptt98hd.totch.ui.common.icon
import io.github.ptt98hd.totch.ui.common.labelRes
import io.github.ptt98hd.totch.ui.gestures.AppIcon
import io.github.ptt98hd.totch.ui.theme.TotchTheme

/**
 * FR-U16, FR-U37: the action picker, content of the Gestures screen's native bottom sheet (full height, the bottom bar
 * stays usable). It cannot be dragged away, only closed with its button (or Back). Its view model lives exactly as
 * long as one opening, so every opening starts fresh.
 */
@Composable
fun ActionPickerSheet(container: AppContainer, gesture: Gesture, onDismiss: () -> Unit) {
    val owner = remember(gesture) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val vm: ActionPickerViewModel =
        viewModel(key = gesture.name, viewModelStoreOwner = owner, factory = ActionPickerViewModel.factory(container, gesture))
    val state by vm.state.collectAsStateWithLifecycle()
    // UI-76: READ_CONTACTS is checked on every resume and after the request; denied for good → app info.
    val context = LocalContext.current
    fun contactsGranted() = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    var contactsBlocked by remember { mutableStateOf(false) }
    val contactsRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setContactsPermission(granted)
        val activity = context as? Activity
        contactsBlocked = !granted && activity != null &&
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
    }
    LifecycleResumeEffect(Unit) {
        vm.setContactsPermission(contactsGranted())
        onPauseOrDispose {}
    }
    LaunchedEffect(state.finished) {
        if (state.finished) onDismiss()
    }
    BackHandler(onBack = onDismiss)
    ActionPickerContent(
        state = state,
        callbacks = PickerCallbacks(
            onClose = onDismiss,
            onTab = vm::selectTab,
            onSelect = vm::select,
            onQuery = vm::setQuery,
            onSelectApp = vm::selectApp,
            onSelectShortcut = vm::selectShortcut,
            onToggleApp = vm::toggleExpanded,
            onSelectActivity = vm::selectActivity,
            onToggleActivityApp = vm::toggleActivityApp,
            onSelectTile = vm::selectTile,
            onSelectIntent = vm::selectIntent,
            onToggleIntentApp = vm::toggleIntentApp,
            onSelectContact = vm::selectContact,
            onRequestContacts = {
                if (contactsBlocked) context.openAppDetails() else contactsRequest.launch(Manifest.permission.READ_CONTACTS)
            },
        ),
        contactsBlocked = contactsBlocked,
    )
}

data class PickerCallbacks(
    val onClose: () -> Unit = {},
    val onTab: (PickerTab) -> Unit = {},
    val onSelect: (Action) -> Unit = {},
    /** UI-71: the search of the current tab. */
    val onQuery: (String) -> Unit = {},
    val onSelectApp: (AppInfo) -> Unit = {},
    val onSelectShortcut: (ShortcutEntry) -> Unit = {},
    val onToggleApp: (String) -> Unit = {},
    val onSelectActivity: (ActivityEntry) -> Unit = {},
    val onToggleActivityApp: (String) -> Unit = {},
    val onSelectTile: (TileEntry) -> Unit = {},
    val onSelectIntent: (IntentEntry) -> Unit = {},
    val onToggleIntentApp: (String) -> Unit = {},
    val onSelectContact: (ContactEntry) -> Unit = {},
    /** UI-76: asks for READ_CONTACTS, or opens the app info page once it was denied for good. */
    val onRequestContacts: () -> Unit = {},
)

private val PickerTab.icon: ImageVector
    get() = when (this) {
        PickerTab.BASIC -> Icons.Outlined.Category
        PickerTab.APPS -> Icons.Outlined.Apps
        PickerTab.SHORTCUTS -> ShortcutIcon
        PickerTab.ACTIVITIES -> ActivityIcon
        PickerTab.TILES -> TileIcon
        PickerTab.SETTINGS -> SettingsIcon
        PickerTab.INTENTS -> IntentIcon
        PickerTab.CONTACTS -> Icons.Outlined.Contacts
    }

private val PickerTab.labelRes: Int
    get() = when (this) {
        PickerTab.BASIC -> R.string.tab_basic
        PickerTab.APPS -> R.string.tab_apps
        PickerTab.SHORTCUTS -> R.string.tab_shortcuts
        PickerTab.ACTIVITIES -> R.string.tab_activities
        PickerTab.TILES -> R.string.tab_tiles
        PickerTab.SETTINGS -> R.string.tab_settings
        PickerTab.INTENTS -> R.string.tab_intents
        PickerTab.CONTACTS -> R.string.tab_contacts
    }

/** A row of the Shortcuts or Activities tab: an app (expandable) or one of its shortcuts / activities. */
private sealed interface ExpandableRow {
    val key: String

    data class App(val packageName: String, val appLabel: String, val expanded: Boolean) : ExpandableRow {
        override val key = "app-$packageName"
    }

    data class Shortcut(val entry: ShortcutEntry) : ExpandableRow {
        override val key = "sc-${entry.packageName}-${entry.id}"
    }

    data class Activity(val entry: ActivityEntry) : ExpandableRow {
        override val key = "act-${entry.packageName}-${entry.name}"
    }

    data class Intent(val entry: IntentEntry) : ExpandableRow {
        override val key = "int-${entry.packageName}-${entry.className}-${entry.intentAction}"
    }
}

/**
 * FR-U9, FR-U20, FR-U58, FR-A14, FR-A15, FR-A21…FR-A23: action chooser; one search field also picks the action type
 * (UI-73).
 */
@Composable
fun ActionPickerContent(
    state: ActionPickerUiState,
    callbacks: PickerCallbacks,
    modifier: Modifier = Modifier,
    contactsBlocked: Boolean = false,
) {
    Column(modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = Gutter, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(state.gesture.labelRes),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = callbacks.onClose) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.picker_close))
            }
        }
        PickerSearchField(
            tab = state.tab,
            query = state.query,
            onQuery = callbacks.onQuery,
            onTab = callbacks.onTab,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(bottom = 12.dp),
        )
        AnimatedContent(
            targetState = state.tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "pickerTab",
        ) { tab ->
            Column(Modifier.fillMaxSize()) {
                when (tab) {
                    PickerTab.BASIC -> {
                        val resources = LocalResources.current
                        val groups = remember(state.basicQuery, resources) {
                            filterCatalog(
                                ActionCatalog.groups,
                                state.basicQuery,
                                actionLabel = { resources.getString(it.labelRes) },
                                groupLabel = { resources.getString(it.labelRes()) },
                            )
                        }
                        // FR-U43: "Nothing" leads the list, hidden by a query that does not match it (UI-72).
                        val showNothing = remember(state.basicQuery, resources) {
                            searchKey(state.basicQuery) in searchKey(resources.getString(Action.None.labelRes))
                        }
                        SearchableList(
                            loading = false,
                            empty = groups.isEmpty() && !showNothing,
                            emptyText = stringResource(R.string.action_search_empty, state.basicQuery.trim()),
                        ) { basicTab(groups, showNothing, state, callbacks.onSelect) }
                    }
                    PickerTab.APPS -> SearchableList(
                        loading = state.appsLoading,
                        empty = state.apps.isEmpty(),
                        emptyText = if (state.appQuery.isBlank()) {
                            stringResource(R.string.app_picker_none)
                        } else {
                            stringResource(R.string.app_picker_empty, state.appQuery.trim())
                        },
                    ) { appsTab(state, callbacks.onSelectApp) }
                    PickerTab.SHORTCUTS -> SearchableList(
                        loading = state.shortcutsLoading,
                        empty = state.shortcutGroups.isEmpty(),
                        emptyText = if (state.shortcutQuery.isBlank()) {
                            stringResource(R.string.shortcut_none)
                        } else {
                            stringResource(R.string.shortcut_empty, state.shortcutQuery.trim())
                        },
                    ) { shortcutsTab(state, callbacks) }
                    PickerTab.ACTIVITIES -> SearchableList(
                        loading = state.activitiesLoading,
                        empty = state.activityGroups.isEmpty(),
                        emptyText = if (state.activityQuery.isBlank()) {
                            stringResource(R.string.activity_none)
                        } else {
                            stringResource(R.string.activity_empty, state.activityQuery.trim())
                        },
                    ) { activitiesTab(state, callbacks) }
                    PickerTab.TILES -> SearchableList(
                        loading = state.tilesLoading,
                        empty = state.tiles.isEmpty(),
                        emptyText = if (state.tileQuery.isBlank()) {
                            stringResource(R.string.tile_none)
                        } else {
                            stringResource(R.string.tile_empty, state.tileQuery.trim())
                        },
                    ) { tilesTab(state, callbacks.onSelectTile) }
                    PickerTab.SETTINGS -> {
                        val resources = LocalResources.current
                        val groups = remember(state.settingsGroups, state.settingsQuery, resources) {
                            filterGroups(
                                state.settingsGroups,
                                state.settingsQuery,
                                id = { it.group },
                                items = { it.intentActions },
                                copy = { g, pages -> g.copy(intentActions = pages) },
                                itemLabel = { page -> settingsPageLabels[page]?.let(resources::getString) ?: page },
                                groupLabel = { group -> resources.getString(settingsGroupLabels.getValue(group)) },
                            )
                        }
                        SearchableList(
                            loading = state.settingsLoading,
                            empty = groups.isEmpty(),
                            emptyText = stringResource(R.string.settings_page_empty, state.settingsQuery.trim()),
                        ) { settingsTab(groups, state, callbacks.onSelect) }
                    }
                    PickerTab.INTENTS -> SearchableList(
                        loading = state.intentsLoading,
                        empty = state.intentGroups.isEmpty(),
                        emptyText = if (state.intentQuery.isBlank()) {
                            stringResource(R.string.intent_none)
                        } else {
                            stringResource(R.string.intent_empty, state.intentQuery.trim())
                        },
                    ) { intentsTab(state, callbacks) }
                    PickerTab.CONTACTS -> if (!state.contactsPermission) {
                        ContactsPermission(contactsBlocked, callbacks.onRequestContacts)
                    } else {
                        SearchableList(
                            loading = state.contactsLoading,
                            empty = state.contacts.isEmpty(),
                            emptyText = if (state.contactQuery.isBlank()) {
                                stringResource(R.string.contact_none)
                            } else {
                                stringResource(R.string.contact_empty, state.contactQuery.trim())
                            },
                        ) { contactsTab(state, callbacks.onSelectContact) }
                    }
                }
            }
        }
    }
}

private const val DisabledAlpha = 0.38f

private val listPadding = PaddingValues(start = Gutter, end = Gutter, bottom = 24.dp)

/**
 * UI-26: each group is one rounded card of rows (icon, name, radio) separated by dividers; FR-U35 groups. FR-U43: the
 * "Nothing" card comes first. FR-U44: actions that cannot run are disabled and say why.
 */
private fun LazyListScope.basicTab(
    groups: List<ActionGroup>,
    showNothing: Boolean,
    state: ActionPickerUiState,
    onSelect: (Action) -> Unit,
) {
    if (showNothing) {
        item(key = "action-none") {
            Choice(
                selected = state.current == Action.None,
                onClick = { onSelect(Action.None) },
                leading = { TonalIcon(Action.None.icon) },
                title = stringResource(Action.None.labelRes),
                modifier = Modifier.padding(top = 8.dp).groupItem(0, 1),
            )
        }
    }
    groups.forEach { group ->
        item(key = "header-${group.id}") {
            SectionHeader(stringResource(group.id.labelRes()), Modifier.padding(top = 8.dp, bottom = 8.dp))
        }
        itemsIndexed(group.actions, key = { _, action -> "action-$action" }) { index, action ->
            val need = state.need(action)
            Choice(
                selected = action == state.current,
                onClick = { onSelect(action) },
                leading = { TonalIcon(action.icon) },
                title = stringResource(action.labelRes),
                supporting = need?.let { stringResource(it.labelRes) },
                enabled = need == null,
                modifier = Modifier.groupItem(index, group.actions.size),
            )
        }
    }
}

private fun LazyListScope.appsTab(state: ActionPickerUiState, onSelectApp: (AppInfo) -> Unit) {
    val current = (state.current as? Action.LaunchApp)?.packageName
    itemsIndexed(state.apps, key = { _, app -> app.packageName }) { index, app ->
        val need = state.need(Action.LaunchApp(app.packageName))
        Choice(
            selected = app.packageName == current,
            onClick = { onSelectApp(app) },
            leading = { AppIcon(app.packageName) },
            title = app.label,
            supporting = need?.let { stringResource(it.labelRes) },
            enabled = need == null,
            modifier = Modifier.groupItem(index, state.apps.size),
        )
    }
}

/** UI-25: apps listed like the Applications tab; each expands to show its shortcuts right below it. */
private fun LazyListScope.shortcutsTab(state: ActionPickerUiState, callbacks: PickerCallbacks) {
    val current = state.current as? Action.LaunchShortcut
    val rows = state.shortcutGroups.flatMap { group ->
        val expanded = state.isExpanded(group.packageName)
        listOf(ExpandableRow.App(group.packageName, group.appLabel, expanded)) +
            if (expanded) group.shortcuts.map(ExpandableRow::Shortcut) else emptyList()
    }
    expandableRows(rows) { row, modifier ->
        when (row) {
            is ExpandableRow.App -> AppHeader(row, modifier, onClick = { callbacks.onToggleApp(row.packageName) })
            is ExpandableRow.Shortcut -> {
                val need = state.need(ShortcutAction)
                Choice(
                    selected = current?.packageName == row.entry.packageName && current.shortcutId == row.entry.id,
                    onClick = { callbacks.onSelectShortcut(row.entry) },
                    leading = { TonalIcon(ShortcutIcon) },
                    title = row.entry.label,
                    supporting = need?.let { stringResource(it.labelRes) },
                    enabled = need == null,
                    modifier = modifier.padding(start = 24.dp),
                )
            }
            is ExpandableRow.Activity, is ExpandableRow.Intent -> Unit
        }
    }
}

/** UI-27: like the Shortcuts tab; each activity shows its label and short class name. */
private fun LazyListScope.activitiesTab(state: ActionPickerUiState, callbacks: PickerCallbacks) {
    val current = state.current as? Action.LaunchActivity
    val rows = state.activityGroups.flatMap { group ->
        val expanded = state.isActivityAppExpanded(group.packageName)
        listOf(ExpandableRow.App(group.packageName, group.appLabel, expanded)) +
            if (expanded) group.activities.map(ExpandableRow::Activity) else emptyList()
    }
    expandableRows(rows) { row, modifier ->
        when (row) {
            is ExpandableRow.App -> AppHeader(row, modifier, onClick = { callbacks.onToggleActivityApp(row.packageName) })
            is ExpandableRow.Activity -> {
                val need = state.need(ActivityAction)
                Choice(
                    selected = current?.packageName == row.entry.packageName && current.activityName == row.entry.name,
                    onClick = { callbacks.onSelectActivity(row.entry) },
                    leading = { TonalIcon(ActivityIcon) },
                    title = row.entry.label,
                    supporting = need?.let { stringResource(it.labelRes) } ?: shortActivityName(row.entry.packageName, row.entry.name),
                    enabled = need == null,
                    modifier = modifier.padding(start = 24.dp),
                )
            }
            is ExpandableRow.Shortcut, is ExpandableRow.Intent -> Unit
        }
    }
}

/** UI-74: grouped like the Basic tab. */
private fun LazyListScope.settingsTab(groups: List<SettingsPageGroup>, state: ActionPickerUiState, onSelect: (Action) -> Unit) {
    val current = (state.current as? Action.OpenSettings)?.intentAction
    groups.forEach { group ->
        item(key = "header-${group.group}") {
            SectionHeader(stringResource(settingsGroupLabels.getValue(group.group)), Modifier.padding(top = 8.dp, bottom = 8.dp))
        }
        itemsIndexed(group.intentActions, key = { _, page -> "page-$page" }) { index, page ->
            val action = Action.OpenSettings(page)
            val need = state.need(action)
            Choice(
                selected = page == current,
                onClick = { onSelect(action) },
                leading = { TonalIcon(SettingsIcon) },
                title = settingsPageLabels[page]?.let { stringResource(it) } ?: page,
                supporting = need?.let { stringResource(it.labelRes) },
                enabled = need == null,
                modifier = Modifier.groupItem(index, group.intentActions.size),
            )
        }
    }
}

/** UI-75: like the Shortcuts tab; each intent shows its short action and whether it is an activity or a broadcast. */
private fun LazyListScope.intentsTab(state: ActionPickerUiState, callbacks: PickerCallbacks) {
    val current = state.current as? Action.SendIntent
    val rows = state.intentGroups.flatMap { group ->
        val expanded = state.isIntentAppExpanded(group.packageName)
        listOf(ExpandableRow.App(group.packageName, group.appLabel, expanded)) +
            if (expanded) group.intents.map(ExpandableRow::Intent) else emptyList()
    }
    expandableRows(rows) { row, modifier ->
        when (row) {
            is ExpandableRow.App -> AppHeader(row, modifier, onClick = { callbacks.onToggleIntentApp(row.packageName) })
            is ExpandableRow.Intent -> {
                val e = row.entry
                val need = state.need(Action.SendIntent(e.packageName, e.className, e.intentAction, e.target))
                val component = shortActivityName(e.packageName, e.className)
                val kind = stringResource(
                    if (e.target == IntentTarget.BROADCAST) R.string.intent_broadcast else R.string.intent_activity,
                    component,
                )
                Choice(
                    selected = current?.packageName == e.packageName && current.className == e.className &&
                        current.intentAction == e.intentAction,
                    onClick = { callbacks.onSelectIntent(e) },
                    leading = { TonalIcon(IntentIcon) },
                    title = IntentFilters.shortIntentAction(e.intentAction),
                    supporting = need?.let { "$kind · ${stringResource(it.labelRes)}" } ?: kind,
                    enabled = need == null,
                    modifier = modifier.padding(start = 24.dp),
                )
            }
            is ExpandableRow.Shortcut, is ExpandableRow.Activity -> Unit
        }
    }
}

/** UI-76: one card of contacts, each with its initial. */
private fun LazyListScope.contactsTab(state: ActionPickerUiState, onSelectContact: (ContactEntry) -> Unit) {
    val current = (state.current as? Action.OpenContact)?.lookupUri
    itemsIndexed(state.contacts, key = { _, contact -> contact.lookupUri }) { index, contact ->
        val need = state.need(Action.OpenContact(contact.lookupUri, contact.name))
        Choice(
            selected = contact.lookupUri == current,
            onClick = { onSelectContact(contact) },
            leading = { Initial(contact.name) },
            title = contact.name,
            supporting = need?.let { stringResource(it.labelRes) },
            enabled = need == null,
            modifier = Modifier.groupItem(index, state.contacts.size),
        )
    }
}

@Composable
private fun Initial(name: String) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(40.dp).background(colors.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onPrimaryContainer,
        )
    }
}

/** UI-76: shown in place of the list until READ_CONTACTS is granted. */
@Composable
private fun ContactsPermission(blocked: Boolean, onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(Gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Contacts, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text(
            stringResource(R.string.contacts_permission_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onRequest) {
            Text(stringResource(if (blocked) R.string.contacts_permission_settings else R.string.contacts_permission_allow))
        }
    }
}

/** UI-28: one card of tiles; each shows its app below the tile label. */
private fun LazyListScope.tilesTab(state: ActionPickerUiState, onSelectTile: (TileEntry) -> Unit) {
    val current = state.current as? Action.ClickTile
    itemsIndexed(state.tiles, key = { _, tile -> "${tile.packageName}/${tile.name}" }) { index, tile ->
        val need = state.need(Action.ClickTile(tile.packageName, tile.name, tile.label))
        Choice(
            selected = current?.packageName == tile.packageName && current.serviceName == tile.name,
            onClick = { onSelectTile(tile) },
            leading = { AppIcon(tile.packageName) },
            title = tile.label,
            supporting = need?.let { "${tile.appLabel} · ${stringResource(it.labelRes)}" } ?: tile.appLabel,
            enabled = need == null,
            modifier = Modifier.groupItem(index, state.tiles.size),
        )
    }
}

/** UI-26: [rows] as one rounded card; [content] gets the row's background modifier. */
private fun LazyListScope.expandableRows(
    rows: List<ExpandableRow>,
    content: @Composable LazyItemScope.(ExpandableRow, Modifier) -> Unit,
) {
    itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
        content(row, Modifier.groupItem(index, rows.size).animateItem())
    }
}

private val ShortcutAction = Action.LaunchShortcut("", "", "", "")
private val ActivityAction = Action.LaunchActivity("", "", "")
private val ShortcutIcon = ShortcutAction.icon
private val ActivityIcon = ActivityAction.icon
private val TileIcon = Action.ClickTile("", "", "").icon
private val SettingsIcon = Action.OpenSettings("").icon
private val IntentIcon = Action.SendIntent("", "", "", IntentTarget.ACTIVITY).icon

@Composable
private fun AppHeader(row: ExpandableRow.App, modifier: Modifier, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (row.expanded) 180f else 0f, label = "expand")
    val state = stringResource(if (row.expanded) R.string.state_expanded else R.string.state_collapsed)
    TotchListItem(
        headline = row.appLabel,
        modifier = modifier.clickable(onClick = onClick).semantics { stateDescription = state },
        leading = { AppIcon(row.packageName) },
        trailing = {
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotation))
        },
        containerColor = Color.Transparent,
    )
}

@Composable
private fun Choice(
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    /** FR-U44: false for an action that cannot run; the row is dimmed and cannot be chosen. */
    enabled: Boolean = true,
) {
    TotchListItem(
        headline = title,
        modifier = modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        supporting = supporting,
        leading = leading,
        trailing = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
        containerColor = Color.Transparent,
        contentAlpha = if (enabled) 1f else DisabledAlpha,
    )
}

@Composable
private fun SearchableList(
    loading: Boolean,
    empty: Boolean,
    emptyText: String,
    content: LazyListScope.() -> Unit,
) {
    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        empty -> Box(Modifier.fillMaxSize().padding(Gutter), contentAlignment = Alignment.Center) {
            Text(emptyText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(contentPadding = listPadding, content = content)
    }
}

/**
 * FR-U58: the search field of the current action type, fully rounded. Its leading icon is the type and opens a menu of
 * the types (icon, name, radio; the current one highlighted); the trailing button clears the query, or opens the
 * same menu while the query is empty.
 */
@Composable
private fun PickerSearchField(
    tab: PickerTab,
    query: String,
    onQuery: (String) -> Unit,
    onTab: (PickerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val locale = LocalConfiguration.current.locales[0]
    var expanded by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf(0.dp) }
    val typeLabel = stringResource(tab.labelRes)
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "typeMenu")
    Box(modifier.onSizeChanged { width = with(density) { it.width.toDp() } }) {
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            decorationBox = { field ->
                Row(
                    Modifier.fillMaxSize().background(colors.surfaceContainerHigh, CircleShape).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            tab.icon,
                            contentDescription = stringResource(R.string.picker_type_cd, typeLabel),
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        if (query.isEmpty()) {
                            Text(
                                stringResource(R.string.picker_search, typeLabel.lowercase(locale)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        field()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.app_picker_clear),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Outlined.ExpandMore,
                                contentDescription = stringResource(R.string.picker_type_cd, typeLabel),
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.rotate(arrow),
                            )
                        }
                    }
                }
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.width(width),
        ) {
            PickerTab.entries.forEach { entry ->
                val isSelected = entry == tab
                val content = if (isSelected) colors.onSecondaryContainer else colors.onSurface
                DropdownMenuItem(
                    text = { Text(stringResource(entry.labelRes), color = content) },
                    onClick = {
                        expanded = false
                        onTab(entry)
                    },
                    leadingIcon = {
                        Icon(entry.icon, contentDescription = null, tint = if (isSelected) content else colors.onSurfaceVariant)
                    },
                    trailingIcon = { RadioButton(selected = isSelected, onClick = null) },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (isSelected) colors.secondaryContainer else Color.Transparent)
                        .semantics { selected = isSelected },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionPickerSystemPreview() {
    TotchTheme {
        ActionPickerContent(
            state = ActionPickerUiState(gesture = Gesture.TAP, current = Action.Home),
            callbacks = PickerCallbacks(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionPickerShortcutsPreview() {
    val entries = listOf(
        ShortcutEntry("org.example.browser", "Browser", "tab", "New tab", "intent:#Intent;end"),
        ShortcutEntry("org.example.browser", "Browser", "incognito", "New private tab", "intent:#Intent;end"),
        ShortcutEntry("org.example.camera", "Camera", "selfie", "Selfie", "intent:#Intent;end"),
    )
    TotchTheme {
        ActionPickerContent(
            state = ActionPickerUiState(
                gesture = Gesture.SWIPE_RIGHT_HOLD,
                tab = PickerTab.SHORTCUTS,
                shortcutsLoading = false,
                shortcutGroups = groupShortcuts(entries, ""),
                expanded = setOf("org.example.browser"),
            ),
            callbacks = PickerCallbacks(),
        )
    }
}
