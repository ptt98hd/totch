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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import io.github.ptt98hd.totch.ui.common.isShizukuInstalled
import io.github.ptt98hd.totch.ui.common.openAccessibilitySettings
import io.github.ptt98hd.totch.ui.common.openMediaAccessSettings
import io.github.ptt98hd.totch.ui.common.openAppNotificationSettings
import io.github.ptt98hd.totch.ui.common.openShizuku
import io.github.ptt98hd.totch.ui.common.withGutters
import io.github.ptt98hd.totch.ui.theme.TotchTheme

@Composable
fun HomeRoute(container: AppContainer) {
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
        onRequirementAction = { type ->
            when (type) {
                RequirementType.SHIZUKU_RUNNING -> context.openShizuku()
                RequirementType.SHIZUKU_PERMISSION -> vm.requestShizukuPermission()
                RequirementType.ACCESSIBILITY -> context.openAccessibilitySettings()
                RequirementType.MEDIA_ACCESS -> context.openMediaAccessSettings()
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    shizukuInstalled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (DetectionMode) -> Unit,
    onRequirementAction: (RequirementType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text(stringResource(R.string.app_name)) }, scrollBehavior = scrollBehavior)
        },
    ) { padding ->
        LazyColumn(
            contentPadding = padding.withGutters(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "master") {
                MasterSwitchCard(enabled = state.enabled, status = state.status, onEnabledChange = onEnabledChange)
            }
            if (state.showOnboarding) {
                item(key = "onboarding") { OnboardingCard() }
            }
            item(key = "mode") { ModeSelector(mode = state.mode, onModeChange = onModeChange) }
            if (state.requirements.isNotEmpty()) {
                item(key = "requirements") {
                    RequirementsCard(state.requirements, shizukuInstalled, onRequirementAction)
                }
            }
            if (state.mode == DetectionMode.SHIZUKU) {
                item(key = "note") { ShizukuShadeNote() }
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
            Box(
                Modifier
                    .size(12.dp)
                    .background(if (running) colors.primary else colors.outline, CircleShape),
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.master_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(statusString, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = enabled, onCheckedChange = null)
        }
    }
}

@Composable
private fun OnboardingCard() {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(R.string.onboarding_body), style = MaterialTheme.typography.bodyMedium)
            OnboardingLine(Icons.Outlined.AccessibilityNew, stringResource(R.string.onboarding_accessibility))
            OnboardingLine(Icons.Outlined.Terminal, stringResource(R.string.onboarding_shizuku))
        }
    }
}

@Composable
private fun OnboardingLine(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(
            stringResource(
                if (mode == DetectionMode.ACCESSIBILITY) R.string.mode_accessibility_desc else R.string.mode_shizuku_desc,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
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
                }
                FilledTonalButton(onClick = { onAction(item.type) }, enabled = item.actionEnabled) {
                    Text(stringResource(label))
                }
            }
        },
    )
}

@Composable
private fun ShizukuShadeNote() {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(Modifier.padding(Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Text(stringResource(R.string.note_shizuku_shade), style = MaterialTheme.typography.bodyMedium)
        }
    }
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
            onRequirementAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenOnboardingPreview() {
    TotchTheme {
        HomeScreen(
            state = HomeUiState(
                loading = false,
                requirements = listOf(RequirementItem(RequirementType.ACCESSIBILITY, false)),
                showOnboarding = true,
            ),
            shizukuInstalled = false,
            onEnabledChange = {},
            onModeChange = {},
            onRequirementAction = {},
        )
    }
}
