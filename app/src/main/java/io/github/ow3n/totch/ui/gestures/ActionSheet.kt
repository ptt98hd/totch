package io.github.ow3n.totch.ui.gestures

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.ow3n.totch.R
import io.github.ow3n.totch.core.model.Action
import io.github.ow3n.totch.core.model.Gesture
import io.github.ow3n.totch.core.settings.DetectionMode
import io.github.ow3n.totch.ui.common.ActionCatalog
import io.github.ow3n.totch.ui.common.ActionGroupId
import io.github.ow3n.totch.ui.common.Gutter
import io.github.ow3n.totch.ui.common.TonalIcon
import io.github.ow3n.totch.ui.common.actionDisplayName
import io.github.ow3n.totch.ui.common.icon
import io.github.ow3n.totch.ui.common.labelRes
import kotlinx.coroutines.launch

/** Bottom sheet listing actions grouped as System / Media / Apps (FR-U2). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    gesture: Gesture,
    current: Action,
    currentAppLabel: String?,
    mode: DetectionMode,
    onSelect: (Action) -> Unit,
    onPickApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Animate the sheet away before reporting the choice, so it does not vanish abruptly.
    fun hideThen(block: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { block() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            stringResource(R.string.sheet_title, stringResource(gesture.labelRes)),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Gutter + 8.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding()) {
            ActionCatalog.groups.forEach { group ->
                item(key = "header-${group.id}") {
                    Text(
                        stringResource(group.id.labelRes()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = Gutter + 8.dp, end = Gutter, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(group.actions.size, key = { "action-${group.id}-$it" }) { index ->
                    val action = group.actions[index]
                    ActionOption(
                        action = action,
                        label = stringResource(action.labelRes),
                        selected = action == current,
                        available = ActionCatalog.isAvailable(action, mode),
                        onClick = { hideThen { onSelect(action) } },
                    )
                }
                if (group.id == ActionGroupId.APPS) {
                    item(key = "open-app") {
                        val launch = current as? Action.LaunchApp
                        ActionOption(
                            action = Action.LaunchApp(""),
                            label = stringResource(R.string.action_open_app),
                            supporting = launch?.let { actionDisplayName(it, currentAppLabel) },
                            selected = launch != null,
                            available = ActionCatalog.isAvailable(Action.LaunchApp(""), mode),
                            navigates = true,
                            onClick = { hideThen(onPickApp) },
                        )
                    }
                }
            }
            item(key = "bottom-space") { Spacer(Modifier.padding(bottom = 16.dp)) }
        }
    }
}

@Composable
private fun ActionOption(
    action: Action,
    label: String,
    selected: Boolean,
    available: Boolean,
    onClick: () -> Unit,
    supporting: String? = null,
    navigates: Boolean = false,
) {
    val warning = if (available) null else stringResource(R.string.action_unavailable_in_mode)
    ListItem(
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        leadingContent = { TonalIcon(action.icon) },
        headlineContent = { Text(label) },
        supportingContent = if (supporting == null && warning == null) null else {
            {
                Column {
                    supporting?.let { Text(it) }
                    warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        trailingContent = {
            if (navigates) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
            } else {
                RadioButton(selected = selected, onClick = null)
            }
        },
    )
}
