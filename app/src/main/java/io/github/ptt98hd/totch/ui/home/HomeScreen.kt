package io.github.ptt98hd.totch.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.ui.common.Gutter
import io.github.ptt98hd.totch.ui.common.SectionHeader
import io.github.ptt98hd.totch.ui.common.TotchTopBar
import io.github.ptt98hd.totch.ui.common.requestUnrestrictedBattery
import io.github.ptt98hd.totch.ui.common.isShizukuInstalled
import io.github.ptt98hd.totch.ui.common.openAccessibilitySettings
import io.github.ptt98hd.totch.ui.common.openMediaAccessSettings
import io.github.ptt98hd.totch.ui.common.openAppNotificationSettings
import io.github.ptt98hd.totch.ui.common.openShizuku
import io.github.ptt98hd.totch.ui.common.withGutters
import io.github.ptt98hd.totch.ui.theme.TotchTheme

@Composable
fun HomeRoute(container: AppContainer, onOpenSettings: () -> Unit) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var shizukuInstalled by remember { mutableStateOf(context.isShizukuInstalled()) }
    var notificationAsked by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        shizukuInstalled = context.isShizukuInstalled()
        vm.refresh()
        onPauseOrDispose { }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationAsked = true
        vm.refresh()
    }

    HomeScreen(
        state = state,
        shizukuInstalled = shizukuInstalled,
        onEnabledChange = vm::setEnabled,
        onModeChange = vm::setMode,
        onOpenSettings = onOpenSettings,
        onRequirementAction = { type ->
            when (type) {
                RequirementType.SHIZUKU_RUNNING -> context.openShizuku()
                RequirementType.SHIZUKU_PERMISSION -> vm.requestShizukuPermission()
                RequirementType.ACCESSIBILITY -> context.openAccessibilitySettings()
                RequirementType.MEDIA_ACCESS -> context.openMediaAccessSettings()
                RequirementType.BATTERY -> context.requestUnrestrictedBattery()
                RequirementType.NOTIFICATIONS ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationAsked) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Already denied once: the system may not show the dialog again.
                        context.openAppNotificationSettings()
                    }
            }
        },
    )
}


@Composable
fun HomeScreen(
    state: HomeUiState,
    shizukuInstalled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (DetectionMode) -> Unit,
    onOpenSettings: () -> Unit,
    onRequirementAction: (RequirementType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TotchTopBar(stringResource(R.string.nav_home), onOpenSettings = onOpenSettings) },
    ) { padding ->
        LazyColumn(
            contentPadding = padding.withGutters(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "master") {
                MasterSwitchCard(enabled = state.enabled, status = state.status, onEnabledChange = onEnabledChange)
            }
            item(key = "mode") { ModeSelector(mode = state.mode, onModeChange = onModeChange) }
            if (state.requirements.isNotEmpty()) {
                item(key = "requirements") {
                    RequirementsCard(state.requirements, shizukuInstalled, onRequirementAction)
                }
            }
        }
    }
}

@Composable
private fun MasterSwitchCard(enabled: Boolean, status: EngineStatus, onEnabledChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val running = status == EngineStatus.Running
    val container by animateColorAsState(
        when {
            status is EngineStatus.Error -> colors.errorContainer
            running -> colors.primaryContainer
            else -> colors.surfaceContainerHigh
        },
        label = "masterContainer",
    )
    val content = when {
        status is EngineStatus.Error -> colors.onErrorContainer
        running -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    val text = statusText(status)
    val statusString = stringResource(text.resId, *text.args.toTypedArray())
    val switchDescription = stringResource(R.string.master_switch_cd)

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
                .semantics { contentDescription = switchDescription }
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MasterIconBox(masterIcon(status))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.master_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(statusString, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = enabled, onCheckedChange = null)
        }
    }
}

/** FR-U15: ✓ / ✗ inside a rounded box on the left of the master card. */
@Composable
private fun MasterIconBox(icon: MasterIcon) {
    val colors = MaterialTheme.colorScheme
    val (box, tint) = when (icon) {
        MasterIcon.CHECK -> colors.primary to colors.onPrimary
        MasterIcon.ERROR -> colors.error to colors.onError
        MasterIcon.OFF -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    val background by animateColorAsState(box, label = "masterIconBox")
    Box(
        Modifier.size(48.dp).background(background, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (icon == MasterIcon.CHECK) Icons.Outlined.Check else Icons.Outlined.Close,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun ModeSelector(mode: DetectionMode, onModeChange: (DetectionMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(R.string.mode_title))
        val modes = listOf(DetectionMode.ACCESSIBILITY, DetectionMode.SHIZUKU)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = mode == item,
                    onClick = { onModeChange(item) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    label = {
                        Text(
                            stringResource(
                                if (item == DetectionMode.ACCESSIBILITY) R.string.mode_accessibility else R.string.mode_shizuku,
                            ),
                        )
                    },
                )
            }
        }
    }
}

private data class RequirementStrings(val title: Int, val ok: Int, val missing: Int)

private fun RequirementType.strings() = when (this) {
    RequirementType.SHIZUKU_RUNNING -> RequirementStrings(
        R.string.req_shizuku_running_title, R.string.req_shizuku_running_ok, R.string.req_shizuku_running_missing,
    )
    RequirementType.SHIZUKU_PERMISSION -> RequirementStrings(
        R.string.req_shizuku_permission_title, R.string.req_shizuku_permission_ok, R.string.req_shizuku_permission_missing,
    )
    RequirementType.ACCESSIBILITY -> RequirementStrings(
        R.string.req_accessibility_title, R.string.req_accessibility_ok, R.string.req_accessibility_missing,
    )
    RequirementType.NOTIFICATIONS -> RequirementStrings(
        R.string.req_notifications_title, R.string.req_notifications_ok, R.string.req_notifications_missing,
    )
    RequirementType.MEDIA_ACCESS -> RequirementStrings(
        R.string.req_media_title, R.string.req_media_ok, R.string.req_media_missing,
    )
    RequirementType.BATTERY -> RequirementStrings(
        R.string.req_battery_title, R.string.req_battery_ok, R.string.req_battery_missing,
    )
}

@Composable
private fun RequirementsCard(
    items: List<RequirementItem>,
    shizukuInstalled: Boolean,
    onAction: (RequirementType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(R.string.requirements_title))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = Gutter))
                RequirementRow(item, shizukuInstalled, onAction)
            }
        }
    }
}

@Composable
private fun RequirementRow(item: RequirementItem, shizukuInstalled: Boolean, onAction: (RequirementType) -> Unit) {
    val strings = item.type.strings()
    val stateDescription = stringResource(if (item.satisfied) R.string.state_done else R.string.state_missing)
    ListItem(
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = {
            Icon(
                imageVector = if (item.satisfied) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = stateDescription,
                tint = if (item.satisfied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        },
        headlineContent = { Text(stringResource(strings.title)) },
        supportingContent = { Text(stringResource(if (item.satisfied) strings.ok else strings.missing)) },
        trailingContent = if (item.satisfied) null else {
            {
                val label = when (item.type) {
                    RequirementType.SHIZUKU_RUNNING ->
                        if (shizukuInstalled) R.string.action_open else R.string.action_install
                    RequirementType.SHIZUKU_PERMISSION, RequirementType.NOTIFICATIONS -> R.string.action_grant
                    RequirementType.ACCESSIBILITY, RequirementType.MEDIA_ACCESS -> R.string.action_open_settings
                    RequirementType.BATTERY -> R.string.action_allow
                }
                FilledTonalButton(onClick = { onAction(item.type) }, enabled = item.actionEnabled) {
                    Text(stringResource(label))
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    TotchTheme {
        HomeScreen(
            state = HomeUiState(
                loading = false,
                enabled = true,
                mode = DetectionMode.SHIZUKU,
                status = EngineStatus.Running,
                requirements = listOf(
                    RequirementItem(RequirementType.SHIZUKU_RUNNING, true),
                    RequirementItem(RequirementType.SHIZUKU_PERMISSION, false),
                    RequirementItem(RequirementType.NOTIFICATIONS, false),
                ),
            ),
            shizukuInstalled = true,
            onEnabledChange = {},
            onModeChange = {},
            onOpenSettings = {},
            onRequirementAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenAccessibilityPreview() {
    TotchTheme {
        HomeScreen(
            state = HomeUiState(
                loading = false,
                requirements = listOf(
                    RequirementItem(RequirementType.ACCESSIBILITY, false),
                    RequirementItem(RequirementType.BATTERY, false),
                ),
            ),
            shizukuInstalled = false,
            onEnabledChange = {},
            onModeChange = {},
            onOpenSettings = {},
            onRequirementAction = {},
        )
    }
}
