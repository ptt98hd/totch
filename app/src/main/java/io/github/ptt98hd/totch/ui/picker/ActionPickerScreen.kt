package io.github.ptt98hd.totch.ui.picker

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
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
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.ui.common.ActionCatalog
import io.github.ptt98hd.totch.ui.common.Gutter
import io.github.ptt98hd.totch.ui.common.SectionHeader
import io.github.ptt98hd.totch.ui.common.TonalIcon
import io.github.ptt98hd.totch.ui.common.groupItem
import io.github.ptt98hd.totch.ui.common.icon
import io.github.ptt98hd.totch.ui.common.labelRes
import io.github.ptt98hd.totch.ui.gestures.AppIcon
import io.github.ptt98hd.totch.ui.theme.TotchTheme

/**
 * FR-U16, FR-U20: the action picker as a sheet over the Gestures content; the top and bottom bars stay usable.
 * It cannot be dragged away, only closed with its button (or Back). Its view model lives exactly as long as the
 * sheet, so every opening starts fresh.
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
            onSelectApp = vm::selectApp,
            onSelectShortcut = vm::selectShortcut,
            onAppQuery = vm::setAppQuery,
            onShortcutQuery = vm::setShortcutQuery,
            onToggleApp = vm::toggleExpanded,
            onSelectActivity = vm::selectActivity,
            onActivityQuery = vm::setActivityQuery,
            onToggleActivityApp = vm::toggleActivityApp,
        ),
    )
}

data class PickerCallbacks(
    val onClose: () -> Unit = {},
    val onTab: (PickerTab) -> Unit = {},
    val onSelect: (Action) -> Unit = {},
    val onSelectApp: (AppInfo) -> Unit = {},
    val onSelectShortcut: (ShortcutEntry) -> Unit = {},
    val onAppQuery: (String) -> Unit = {},
    val onShortcutQuery: (String) -> Unit = {},
    val onToggleApp: (String) -> Unit = {},
    val onSelectActivity: (ActivityEntry) -> Unit = {},
    val onActivityQuery: (String) -> Unit = {},
    val onToggleActivityApp: (String) -> Unit = {},
)

private val PickerTab.icon: ImageVector
    get() = when (this) {
        PickerTab.SYSTEM -> Icons.Outlined.Smartphone
        PickerTab.APPS -> Icons.Outlined.Apps
        PickerTab.SHORTCUTS -> ShortcutIcon
        PickerTab.ACTIVITIES -> ActivityIcon
    }

/** FR-U25: the sections as a horizontally scrolling chip row, so new sections never squeeze the labels. */
@Composable
private fun TabChips(selected: PickerTab, onTab: (PickerTab) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == selected.ordinal }
        val fullyVisible = item != null && item.offset >= info.viewportStartOffset &&
            item.offset + item.size <= info.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem(selected.ordinal)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).selectableGroup(),
    ) {
        items(PickerTab.entries, key = { it.name }) { tab ->
            FilterChip(
                selected = tab == selected,
                onClick = { onTab(tab) },
                label = { Text(stringResource(tab.labelRes)) },
                leadingIcon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
            )
        }
    }
}

private val PickerTab.labelRes: Int
    get() = when (this) {
        PickerTab.SYSTEM -> R.string.tab_system
        PickerTab.APPS -> R.string.tab_apps
        PickerTab.SHORTCUTS -> R.string.tab_shortcuts
        PickerTab.ACTIVITIES -> R.string.tab_activities
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

/** FR-U9, FR-U20, FR-U25, FR-A14: action chooser with chip sections (System / Apps / Shortcuts / Activities). */
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
        TabChips(state.tab, callbacks.onTab)
        AnimatedContent(
            targetState = state.tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "pickerTab",
        ) { tab ->
            Column(Modifier.fillMaxSize()) {
                when (tab) {
                    PickerTab.SYSTEM -> LazyColumn(contentPadding = listPadding) { systemTab(state, callbacks.onSelect) }
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
                }
            }
        }
    }
}

private val listPadding = PaddingValues(start = Gutter, end = Gutter, bottom = 24.dp)

/** UI-26: each group is one rounded card of rows (icon, name, radio) separated by dividers. */
private fun LazyListScope.systemTab(state: ActionPickerUiState, onSelect: (Action) -> Unit) {
    ActionCatalog.groups.forEach { group ->
        item(key = "header-${group.id}") {
            SectionHeader(stringResource(group.id.labelRes()), Modifier.padding(top = 8.dp, bottom = 8.dp))
        }
        itemsIndexed(group.actions, key = { _, action -> "action-$action" }) { index, action ->
            Choice(
                selected = action == state.current,
                onClick = { onSelect(action) },
                leading = { TonalIcon(action.icon) },
                title = stringResource(action.labelRes),
                modifier = Modifier.groupItem(index, group.actions.size),
            )
        }
    }
}

private fun LazyListScope.appsTab(state: ActionPickerUiState, onSelectApp: (AppInfo) -> Unit) {
    val current = (state.current as? Action.LaunchApp)?.packageName
    itemsIndexed(state.apps, key = { _, app -> app.packageName }) { index, app ->
        Choice(
            selected = app.packageName == current,
            onClick = { onSelectApp(app) },
            leading = { AppIcon(app.packageName) },
            title = app.label,
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
            is ExpandableRow.Shortcut -> Choice(
                selected = current?.packageName == row.entry.packageName && current.shortcutId == row.entry.id,
                onClick = { callbacks.onSelectShortcut(row.entry) },
                leading = { TonalIcon(ShortcutIcon) },
                title = row.entry.label,
                modifier = modifier.padding(start = 24.dp),
            )
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
            is ExpandableRow.Activity -> Choice(
                selected = current?.packageName == row.entry.packageName && current.activityName == row.entry.name,
                onClick = { callbacks.onSelectActivity(row.entry) },
                leading = { TonalIcon(ActivityIcon) },
                title = row.entry.label,
                supporting = shortActivityName(row.entry.packageName, row.entry.name),
                modifier = modifier.padding(start = 24.dp),
            )
            is ExpandableRow.Shortcut -> Unit
        }
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

private val ShortcutIcon = Action.LaunchShortcut("", "", "", "").icon
private val ActivityIcon = Action.LaunchActivity("", "", "").icon

@Composable
private fun AppHeader(row: ExpandableRow.App, modifier: Modifier, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (row.expanded) 180f else 0f, label = "expand")
    val state = stringResource(if (row.expanded) R.string.state_expanded else R.string.state_collapsed)
    ListItem(
        modifier = modifier.clickable(onClick = onClick).semantics { stateDescription = state },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { AppIcon(row.packageName) },
        headlineContent = { Text(row.appLabel) },
        trailingContent = {
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotation))
        },
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
) {
    ListItem(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = leading,
        headlineContent = { Text(title) },
        supportingContent = supporting?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val focusManager = LocalFocusManager.current
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQuery,
                onSearch = { focusManager.clearFocus() },
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(placeholder) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (query.isEmpty()) null else {
                    {
                        IconButton(onClick = { onQuery("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.app_picker_clear))
                        }
                    }
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        windowInsets = WindowInsets(0),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(bottom = 12.dp),
    ) {}
    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        empty -> Box(Modifier.fillMaxSize().padding(Gutter), contentAlignment = Alignment.Center) {
            Text(emptyText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(contentPadding = listPadding, content = content)
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
