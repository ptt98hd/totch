package io.github.ptt98hd.totch.ui.picker

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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
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
            onBasicQuery = vm::setBasicQuery,
            onSelectApp = vm::selectApp,
            onSelectShortcut = vm::selectShortcut,
            onAppQuery = vm::setAppQuery,
            onShortcutQuery = vm::setShortcutQuery,
            onToggleApp = vm::toggleExpanded,
            onSelectActivity = vm::selectActivity,
            onActivityQuery = vm::setActivityQuery,
            onToggleActivityApp = vm::toggleActivityApp,
            onSelectTile = vm::selectTile,
            onTileQuery = vm::setTileQuery,
        ),
    )
}

data class PickerCallbacks(
    val onClose: () -> Unit = {},
    val onTab: (PickerTab) -> Unit = {},
    val onSelect: (Action) -> Unit = {},
    val onBasicQuery: (String) -> Unit = {},
    val onSelectApp: (AppInfo) -> Unit = {},
    val onSelectShortcut: (ShortcutEntry) -> Unit = {},
    val onAppQuery: (String) -> Unit = {},
    val onShortcutQuery: (String) -> Unit = {},
    val onToggleApp: (String) -> Unit = {},
    val onSelectActivity: (ActivityEntry) -> Unit = {},
    val onActivityQuery: (String) -> Unit = {},
    val onToggleActivityApp: (String) -> Unit = {},
    val onSelectTile: (TileEntry) -> Unit = {},
    val onTileQuery: (String) -> Unit = {},
)

private val PickerTab.icon: ImageVector
    get() = when (this) {
        PickerTab.BASIC -> Icons.Outlined.Category
        PickerTab.APPS -> Icons.Outlined.Apps
        PickerTab.SHORTCUTS -> ShortcutIcon
        PickerTab.ACTIVITIES -> ActivityIcon
        PickerTab.TILES -> TileIcon
    }

/** FR-U47: the sections in a drop-down select (replacing the chip row of FR-U25); each entry has its icon. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabSelect(current: PickerTab, onTab: (PickerTab) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(bottom = 12.dp),
    ) {
        OutlinedTextField(
            value = stringResource(current.labelRes),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.picker_tab_label)) },
            leadingIcon = { Icon(current.icon, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PickerTab.entries.forEach { tab ->
                val isSelected = tab == current
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(tab.labelRes),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                        )
                    },
                    onClick = {
                        expanded = false
                        onTab(tab)
                    },
                    leadingIcon = {
                        Icon(
                            tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.semantics { selected = isSelected },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

private val PickerTab.labelRes: Int
    get() = when (this) {
        PickerTab.BASIC -> R.string.tab_basic
        PickerTab.APPS -> R.string.tab_apps
        PickerTab.SHORTCUTS -> R.string.tab_shortcuts
        PickerTab.ACTIVITIES -> R.string.tab_activities
        PickerTab.TILES -> R.string.tab_tiles
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
}

/** FR-U9, FR-U20, FR-U47, FR-A14, FR-A15: action chooser with a section select (Basic / Apps / Shortcuts / Activities / Tiles). */
@Composable
fun ActionPickerContent(state: ActionPickerUiState, callbacks: PickerCallbacks, modifier: Modifier = Modifier) {
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
        TabSelect(state.tab, callbacks.onTab)
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
                        // FR-U43: "Not set" leads the list, hidden by a query that does not match it.
                        val showNotSet = remember(state.basicQuery, resources) {
                            searchKey(state.basicQuery) in searchKey(resources.getString(Action.None.labelRes))
                        }
                        SearchableList(
                            query = state.basicQuery,
                            onQuery = callbacks.onBasicQuery,
                            placeholder = stringResource(R.string.action_search),
                            loading = false,
                            empty = groups.isEmpty() && !showNotSet,
                            emptyText = stringResource(R.string.action_search_empty, state.basicQuery.trim()),
                        ) { basicTab(groups, showNotSet, state, callbacks.onSelect) }
                    }
                    PickerTab.APPS -> SearchableList(
                        query = state.appQuery,
                        onQuery = callbacks.onAppQuery,
                        placeholder = stringResource(R.string.app_picker_search),
                        loading = state.appsLoading,
                        empty = state.apps.isEmpty(),
                        emptyText = if (state.appQuery.isBlank()) {
                            stringResource(R.string.app_picker_none)
                        } else {
                            stringResource(R.string.app_picker_empty, state.appQuery.trim())
                        },
                    ) { appsTab(state, callbacks.onSelectApp) }
                    PickerTab.SHORTCUTS -> SearchableList(
                        query = state.shortcutQuery,
                        onQuery = callbacks.onShortcutQuery,
                        placeholder = stringResource(R.string.shortcut_search),
                        loading = state.shortcutsLoading,
                        empty = state.shortcutGroups.isEmpty(),
                        emptyText = if (state.shortcutQuery.isBlank()) {
                            stringResource(R.string.shortcut_none)
                        } else {
                            stringResource(R.string.shortcut_empty, state.shortcutQuery.trim())
                        },
                    ) { shortcutsTab(state, callbacks) }
                    PickerTab.ACTIVITIES -> SearchableList(
                        query = state.activityQuery,
                        onQuery = callbacks.onActivityQuery,
                        placeholder = stringResource(R.string.activity_search),
                        loading = state.activitiesLoading,
                        empty = state.activityGroups.isEmpty(),
                        emptyText = if (state.activityQuery.isBlank()) {
                            stringResource(R.string.activity_none)
                        } else {
                            stringResource(R.string.activity_empty, state.activityQuery.trim())
                        },
                    ) { activitiesTab(state, callbacks) }
                    PickerTab.TILES -> SearchableList(
                        query = state.tileQuery,
                        onQuery = callbacks.onTileQuery,
                        placeholder = stringResource(R.string.tile_search),
                        loading = state.tilesLoading,
                        empty = state.tiles.isEmpty(),
                        emptyText = if (state.tileQuery.isBlank()) {
                            stringResource(R.string.tile_none)
                        } else {
                            stringResource(R.string.tile_empty, state.tileQuery.trim())
                        },
                    ) { tilesTab(state, callbacks.onSelectTile) }
                }
            }
        }
    }
}

private const val DisabledAlpha = 0.38f

private val listPadding = PaddingValues(start = Gutter, end = Gutter, bottom = 24.dp)

/**
 * UI-26: each group is one rounded card of rows (icon, name, radio) separated by dividers; FR-U35 groups. FR-U43: the
 * "Not set" card comes first. FR-U44: actions that cannot run are disabled and say why.
 */
private fun LazyListScope.basicTab(
    groups: List<ActionGroup>,
    showNotSet: Boolean,
    state: ActionPickerUiState,
    onSelect: (Action) -> Unit,
) {
    if (showNotSet) {
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
            is ExpandableRow.Activity -> Unit
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
            is ExpandableRow.Shortcut -> Unit
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
    query: String,
    onQuery: (String) -> Unit,
    placeholder: String,
    loading: Boolean,
    empty: Boolean,
    emptyText: String,
    content: LazyListScope.() -> Unit,
) {
    CompactSearchField(
        query = query,
        onQuery = onQuery,
        placeholder = placeholder,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(bottom = 12.dp),
    )
    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        empty -> Box(Modifier.fillMaxSize().padding(Gutter), contentAlignment = Alignment.Center) {
            Text(emptyText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(contentPadding = listPadding, content = content)
    }
}

/** FR-U38: a 44 dp search field (the M3 SearchBar is 56 dp), fully rounded. */
@Composable
private fun CompactSearchField(query: String, onQuery: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier.height(44.dp),
        decorationBox = { field ->
            Row(
                Modifier.fillMaxSize().background(colors.surfaceContainerHigh, CircleShape).padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    if (query.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    field()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQuery("") }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.app_picker_clear),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    )
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
