package io.github.ptt98hd.totch.ui.picker

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.ShortcutEntry
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.ui.common.ActionCatalog
import io.github.ptt98hd.totch.ui.common.ActionGroup
import io.github.ptt98hd.totch.ui.common.Gutter
import io.github.ptt98hd.totch.ui.common.TonalIcon
import io.github.ptt98hd.totch.ui.common.icon
import io.github.ptt98hd.totch.ui.common.labelRes
import io.github.ptt98hd.totch.ui.gestures.AppIcon
import io.github.ptt98hd.totch.ui.theme.TotchTheme

/**
 * FR-U16: the action picker as a bottom sheet over the Gestures screen. Its view model lives exactly as long
 * as the sheet, so every opening starts fresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPickerSheet(container: AppContainer, gesture: Gesture, onDismiss: () -> Unit) {
    val owner = remember(gesture) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val vm: ActionPickerViewModel =
        viewModel(viewModelStoreOwner = owner, factory = ActionPickerViewModel.factory(container, gesture))
    val state by vm.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(state.finished) {
        if (state.finished) {
            sheetState.hide()
            onDismiss()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ActionPickerContent(
            state = state,
            callbacks = PickerCallbacks(
                onTab = vm::selectTab,
                onSelect = vm::select,
                onSelectApp = vm::selectApp,
                onSelectShortcut = vm::selectShortcut,
                onSystemQuery = vm::setSystemQuery,
                onAppQuery = vm::setAppQuery,
                onShortcutQuery = vm::setShortcutQuery,
            ),
        )
    }
}

data class PickerCallbacks(
    val onTab: (PickerTab) -> Unit = {},
    val onSelect: (Action) -> Unit = {},
    val onSelectApp: (AppInfo) -> Unit = {},
    val onSelectShortcut: (ShortcutEntry) -> Unit = {},
    val onSystemQuery: (String) -> Unit = {},
    val onAppQuery: (String) -> Unit = {},
    val onShortcutQuery: (String) -> Unit = {},
)

private val PickerTab.labelRes: Int
    get() = when (this) {
        PickerTab.SYSTEM -> R.string.tab_system
        PickerTab.APPS -> R.string.tab_apps
        PickerTab.SHORTCUTS -> R.string.tab_shortcuts
    }

/** FR-U9, FR-U16: action chooser with segmented sections (System / Applications / Shortcuts), each searchable. */
@Composable
fun ActionPickerContent(state: ActionPickerUiState, callbacks: PickerCallbacks, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    Column(modifier.fillMaxSize().imePadding()) {
        Text(
            stringResource(state.gesture.labelRes),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Gutter).padding(bottom = 16.dp),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Gutter)) {
            PickerTab.entries.forEach { tab ->
                SegmentedButton(
                    selected = state.tab == tab,
                    onClick = { callbacks.onTab(tab) },
                    shape = SegmentedButtonDefaults.itemShape(tab.ordinal, PickerTab.entries.size),
                    label = { Text(stringResource(tab.labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        val bottom = PaddingValues(bottom = 16.dp)
        when (state.tab) {
            PickerTab.SYSTEM -> {
                val groups = ActionCatalog.search(state.systemQuery) { resources.getString(it.labelRes) }
                SearchableList(
                    query = state.systemQuery,
                    onQuery = callbacks.onSystemQuery,
                    placeholder = stringResource(R.string.action_search),
                    loading = false,
                    empty = groups.isEmpty(),
                    emptyText = stringResource(R.string.action_search_empty, state.systemQuery.trim()),
                    contentPadding = bottom,
                ) { systemTab(groups, state, callbacks.onSelect) }
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
                contentPadding = bottom,
            ) {
                val current = (state.current as? Action.LaunchApp)?.packageName
                items(state.apps, key = { it.packageName }) { app ->
                    Choice(
                        selected = app.packageName == current,
                        onClick = { callbacks.onSelectApp(app) },
                        leading = { AppIcon(app.packageName) },
                        title = app.label,
                    )
                }
            }
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
                contentPadding = bottom,
            ) {
                val current = state.current as? Action.LaunchShortcut
                state.shortcutGroups.forEach { group ->
                    item(key = "app-${group.packageName}") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = Gutter + 8.dp, end = Gutter, top = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AppIcon(group.packageName, size = 24.dp)
                            Text(group.appLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    items(group.shortcuts, key = { "sc-${it.packageName}-${it.id}" }) { entry ->
                        Choice(
                            selected = current?.packageName == entry.packageName && current.shortcutId == entry.id,
                            onClick = { callbacks.onSelectShortcut(entry) },
                            leading = { TonalIcon(Action.LaunchShortcut("", "", "", "").icon) },
                            title = entry.label,
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.systemTab(groups: List<ActionGroup>, state: ActionPickerUiState, onSelect: (Action) -> Unit) {
    groups.forEach { group ->
        item(key = "header-${group.id}") {
            Text(
                stringResource(group.id.labelRes()),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Gutter + 8.dp, end = Gutter, top = 16.dp, bottom = 4.dp),
            )
        }
        items(group.actions, key = { "action-$it" }) { action ->
            Choice(
                selected = action == state.current,
                onClick = { onSelect(action) },
                leading = { TonalIcon(action.icon) },
                title = stringResource(action.labelRes),
                warning = if (ActionCatalog.isAvailable(action, state.mode)) null else stringResource(R.string.action_unavailable_in_mode),
            )
        }
    }
}

@Composable
private fun Choice(
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    title: String,
    warning: String? = null,
) {
    ListItem(
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick).padding(horizontal = 8.dp),
        leadingContent = leading,
        headlineContent = { Text(title) },
        supportingContent = warning?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
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
    contentPadding: PaddingValues,
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = 8.dp, bottom = 8.dp),
    ) {}
    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        empty -> Box(Modifier.fillMaxSize().padding(Gutter), contentAlignment = Alignment.Center) {
            Text(emptyText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(contentPadding = contentPadding, content = content)
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionPickerSystemPreview() {
    TotchTheme {
        ActionPickerContent(
            state = ActionPickerUiState(gesture = Gesture.TAP, current = Action.Home, mode = DetectionMode.SHIZUKU),
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
            ),
            callbacks = PickerCallbacks(),
        )
    }
}
