package io.github.ow3n.totch.ui.gestures

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ow3n.totch.AppContainer
import io.github.ow3n.totch.R
import io.github.ow3n.totch.core.model.Action
import io.github.ow3n.totch.core.model.Gesture
import io.github.ow3n.totch.core.settings.DetectionMode
import io.github.ow3n.totch.core.settings.DefaultBindings
import io.github.ow3n.totch.ui.common.ActionCatalog
import io.github.ow3n.totch.ui.common.Gutter
import io.github.ow3n.totch.ui.common.TonalIcon
import io.github.ow3n.totch.ui.common.actionDisplayName
import io.github.ow3n.totch.ui.common.icon
import io.github.ow3n.totch.ui.common.labelRes
import io.github.ow3n.totch.ui.common.withGutters
import io.github.ow3n.totch.ui.theme.TotchTheme

@Composable
fun GesturesRoute(container: AppContainer, onPickApp: (Gesture) -> Unit) {
    val vm: GesturesViewModel = viewModel(factory = GesturesViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    GesturesScreen(state = state, onGestureClick = vm::openSheet)

    val editing = state.editingGesture
    if (editing != null) {
        val row = state.rows.firstOrNull { it.gesture == editing }
        ActionSheet(
            gesture = editing,
            current = row?.action ?: Action.None,
            currentAppLabel = row?.appLabel,
            mode = state.mode,
            onSelect = vm::selectAction,
            onPickApp = {
                vm.dismissSheet()
                onPickApp(editing)
            },
            onDismiss = vm::dismissSheet,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesScreen(
    state: GesturesUiState,
    onGestureClick: (Gesture) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text(stringResource(R.string.gestures_title)) }, scrollBehavior = scrollBehavior)
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding.withGutters(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "intro") {
                Text(
                    stringResource(R.string.gestures_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item(key = "list") {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    state.rows.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = Gutter))
                        GestureListItem(row, state.mode, onClick = { onGestureClick(row.gesture) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureListItem(row: GestureRow, mode: DetectionMode, onClick: () -> Unit) {
    val actionName = actionDisplayName(row.action, row.appLabel)
    val notInstalled = row.action is Action.LaunchApp && row.appLabel == null
    val available = ActionCatalog.isAvailable(row.action, mode)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = { TonalIcon(row.gesture.icon) },
        headlineContent = { Text(stringResource(row.gesture.labelRes)) },
        supportingContent = {
            Text(
                if (notInstalled) "$actionName · ${stringResource(R.string.app_not_installed)}" else actionName,
                color = if (notInstalled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = if (available) null else {
            {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = stringResource(R.string.action_unavailable_in_mode),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun GesturesScreenPreview() {
    TotchTheme {
        GesturesScreen(
            state = GesturesUiState(
                rows = Gesture.entries.map { g ->
                    if (g == Gesture.TAP) {
                        GestureRow(g, Action.LaunchApp("org.example.camera"), "Camera")
                    } else {
                        GestureRow(g, DefaultBindings.getValue(g))
                    }
                },
            ),
            onGestureClick = {},
        )
    }
}
