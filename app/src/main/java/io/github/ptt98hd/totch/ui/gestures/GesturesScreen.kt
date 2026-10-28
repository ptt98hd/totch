package io.github.ptt98hd.totch.ui.gestures

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOn
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DefaultBindings
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.HoldGestures
import io.github.ptt98hd.totch.core.settings.isAvailableIn
import io.github.ptt98hd.totch.ui.common.ActionCatalog
import io.github.ptt98hd.totch.ui.common.Gutter
import io.github.ptt98hd.totch.ui.common.SectionHeader
import io.github.ptt98hd.totch.ui.common.TonalIcon
import io.github.ptt98hd.totch.ui.common.TopBarNav
import io.github.ptt98hd.totch.ui.common.TotchTopBar
import io.github.ptt98hd.totch.ui.common.actionDisplayName
import io.github.ptt98hd.totch.ui.common.icon
import io.github.ptt98hd.totch.ui.common.labelRes
import io.github.ptt98hd.totch.ui.common.withGutters
import io.github.ptt98hd.totch.ui.theme.TotchTheme

@Composable
fun GesturesRoute(container: AppContainer, onOpenSettings: () -> Unit, onPickAction: (Gesture) -> Unit) {
    val vm: GesturesViewModel = viewModel(factory = GesturesViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    GesturesScreen(
        state = state,
        onOpenSettings = onOpenSettings,
        onGestureClick = onPickAction,
        onRepeatChange = vm::setRepeat,
        onRequestReset = vm::requestResetGestures,
    )
    if (state.confirmReset) {
        AlertDialog(
            onDismissRequest = vm::cancelResetGestures,
            icon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
            title = { Text(stringResource(R.string.reset_gestures_title)) },
            text = { Text(stringResource(R.string.reset_gestures_message)) },
            confirmButton = {
                TextButton(onClick = vm::confirmResetGestures) { Text(stringResource(R.string.reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = vm::cancelResetGestures) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@get:StringRes
private val GestureGroup.labelRes: Int
    get() = when (this) {
        GestureGroup.TAP -> R.string.group_tap
        GestureGroup.PRESS -> R.string.group_press
        GestureGroup.SWIPE -> R.string.group_swipe
        GestureGroup.SWIPE_HOLD -> R.string.group_swipe_hold
    }

@Composable
fun GesturesScreen(
    state: GesturesUiState,
    onOpenSettings: () -> Unit,
    onGestureClick: (Gesture) -> Unit,
    onRepeatChange: (Gesture, Boolean) -> Unit,
    onRequestReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TotchTopBar(stringResource(R.string.nav_gestures), TopBarNav.Settings(onOpenSettings)) {
                IconButton(onClick = onRequestReset) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = stringResource(R.string.reset_gestures_cd))
                }
            }
        },
    ) { padding ->
        LazyColumn(contentPadding = padding.withGutters(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.groups, key = { it.group }) { section ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    SectionHeader(stringResource(section.group.labelRes))
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        section.rows.forEachIndexed { index, row ->
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = Gutter))
                            GestureListItem(
                                row = row,
                                mode = state.mode,
                                onClick = { onGestureClick(row.gesture) },
                                onRepeatChange = { onRepeatChange(row.gesture, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureListItem(row: GestureRow, mode: DetectionMode, onClick: () -> Unit, onRepeatChange: (Boolean) -> Unit) {
    val actionName = actionDisplayName(row.action, row.appLabel)
    val packageAction = row.action is Action.LaunchApp || row.action is Action.LaunchShortcut
    val notInstalled = packageAction && row.appLabel == null
    val available = ActionCatalog.isAvailable(row.action, mode)
    val supporting = when {
        !row.enabled -> stringResource(R.string.gesture_unavailable_shizuku)
        notInstalled -> "$actionName · ${stringResource(R.string.app_not_installed)}"
        else -> actionName
    }
    ListItem(
        modifier = Modifier
            .clickable(enabled = row.enabled, onClick = onClick)
            .alpha(if (row.enabled) 1f else 0.5f),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = { TonalIcon(row.gesture.icon) },
        headlineContent = { Text(stringResource(row.gesture.labelRes)) },
        supportingContent = {
            Text(
                supporting,
                color = if (notInstalled && row.enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = when {
            !row.enabled -> null
            row.repeatable -> {
                {
                    IconToggleButton(
                        checked = row.repeat,
                        onCheckedChange = onRepeatChange,
                        enabled = row.action != Action.None,
                    ) {
                        Icon(
                            if (row.repeat) Icons.Outlined.RepeatOn else Icons.Outlined.Repeat,
                            contentDescription = stringResource(R.string.repeat_cd),
                            tint = if (row.repeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            !available -> {
                {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = stringResource(R.string.action_unavailable_in_mode),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            else -> null
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun GesturesScreenPreview() {
    val mode = DetectionMode.SHIZUKU
    TotchTheme {
        GesturesScreen(
            state = GesturesUiState(
                groups = GestureGroups.map { (group, gestures) ->
                    GestureSection(
                        group,
                        gestures.map { g ->
                            val action = if (g == Gesture.TAP) Action.LaunchApp("org.example.camera") else DefaultBindings.getValue(g)
                            GestureRow(
                                g, action, appLabel = if (g == Gesture.TAP) "Camera" else null,
                                enabled = g.isAvailableIn(mode), repeatable = g in HoldGestures, repeat = g == Gesture.LONG_PRESS,
                            )
                        },
                    )
                },
                mode = mode,
            ),
            onOpenSettings = {},
            onGestureClick = {},
            onRepeatChange = { _, _ -> },
            onRequestReset = {},
        )
    }
}
