package io.github.ptt98hd.totch.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.Permission
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.ui.common.DonateRow
import io.github.ptt98hd.totch.ui.common.GroupCard
import io.github.ptt98hd.totch.ui.common.IconBox
import io.github.ptt98hd.totch.ui.common.SectionHeader
import io.github.ptt98hd.totch.ui.common.ToggleListItem
import io.github.ptt98hd.totch.ui.common.TotchTopBar
import io.github.ptt98hd.totch.ui.common.isShizukuInstalled
import io.github.ptt98hd.totch.ui.common.openAccessibilitySettings
import io.github.ptt98hd.totch.ui.common.openAppNotificationSettings
import io.github.ptt98hd.totch.ui.common.openBatteryOptimizationSettings
import io.github.ptt98hd.totch.ui.common.openInstallUnknownAppsSettings
import io.github.ptt98hd.totch.ui.common.requestUnrestrictedBattery
import io.github.ptt98hd.totch.ui.common.openDndAccessSettings
import io.github.ptt98hd.totch.ui.common.openMediaAccessSettings
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
    var confirmRevoke by rememberSaveable { mutableStateOf<Permission?>(null) }

    LifecycleResumeEffect(Unit) {
        shizukuInstalled = context.isShizukuInstalled()
        vm.refresh()
        onPauseOrDispose { }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationAsked = true
        vm.refresh()
    }

    // UI-52: used when Shizuku could not switch the permission.
    val openSystemPage: (Permission) -> Unit = { permission ->
        when (permission) {
            Permission.ACCESSIBILITY -> context.openAccessibilitySettings()
            Permission.MEDIA_ACCESS -> context.openMediaAccessSettings()
            Permission.DND_ACCESS -> context.openDndAccessSettings()
            Permission.INSTALL_UPDATES -> context.openInstallUnknownAppsSettings()
            Permission.BATTERY ->
                if (state.permissions.any { it.permission == Permission.BATTERY && it.granted }) {
                    context.openBatteryOptimizationSettings()
                } else {
                    context.requestUnrestrictedBattery()
                }
            Permission.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationAsked &&
                    state.permissions.none { it.permission == Permission.NOTIFICATIONS && it.granted }
                ) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Already denied once (the dialog may not show again), or revoking.
                    context.openAppNotificationSettings()
                }
        }
    }

    HomeScreen(
        state = state,
        shizukuInstalled = shizukuInstalled,
        onEnabledChange = vm::setEnabled,
        onShizukuClick = {
            if (state.shizuku == ShizukuStatus.PERMISSION_REQUIRED) vm.requestShizukuPermission() else context.openShizuku()
        },
        onPermissionChange = { permission, granted ->
            if (needsConfirmation(permission, granted)) {
                confirmRevoke = permission
            } else {
                vm.setPermission(permission, granted, openSystemPage)
            }
        },
    )

    confirmRevoke?.let { permission ->
        AlertDialog(
            onDismissRequest = { confirmRevoke = null },
            title = {
                Text(
                    stringResource(
                        if (permission == Permission.INSTALL_UPDATES) R.string.revoke_install_title else R.string.revoke_notifications_title,
                    ),
                )
            },
            text = { Text(stringResource(R.string.revoke_notifications_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRevoke = null
                        vm.setPermission(permission, false, openSystemPage)
                    },
                ) { Text(stringResource(R.string.revoke_notifications_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevoke = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    shizukuInstalled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onShizukuClick: () -> Unit,
    onPermissionChange: (Permission, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TotchTopBar(stringResource(R.string.nav_home)) },
    ) { padding ->
        LazyColumn(
            contentPadding = padding.withGutters(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "master") {
                MasterSwitchCard(enabled = state.enabled, status = state.status, onEnabledChange = onEnabledChange)
            }
            // UI-54: Permissions, then Advanced (Shizuku, optional — FR-U42), then Support (FR-U40).
            if (state.permissions.isNotEmpty()) {
                item(key = "permissions") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(stringResource(R.string.permissions_title))
                        GroupCard(items = state.permissions) { item ->
                            PermissionRow(item, onPermissionChange)
                        }
                    }
                }
            }
            item(key = "advanced") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(stringResource(R.string.advanced_title))
                    GroupCard {
                        ShizukuRow(state.shizuku, shizukuInstalled, onShizukuClick)
                    }
                }
            }
            item(key = "support") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(stringResource(R.string.section_support))
                    GroupCard { DonateRow() }
                }
            }
        }
    }
}

@Composable
private fun MasterSwitchCard(enabled: Boolean, status: EngineStatus, onEnabledChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val icon = masterIcon(status)
    val problem = enabled && icon == MasterIcon.OFF && status !is EngineStatus.PausedRotation
    val container by animateColorAsState(
        when {
            problem -> colors.errorContainer
            icon == MasterIcon.ON -> colors.primaryContainer
            else -> colors.surfaceContainerHigh
        },
        label = "masterContainer",
    )
    val content = when {
        problem -> colors.onErrorContainer
        icon == MasterIcon.ON -> colors.onPrimaryContainer
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
            MasterIconBox(icon, problem)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                // FR-U45: one line, even for a long error.
                Text(statusString, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = enabled, onCheckedChange = null)
        }
    }
}

/** UI-42: on (✓), off (✗) or a spinning sync icon while turning on / off, inside a rounded box. */
@Composable
private fun MasterIconBox(icon: MasterIcon, problem: Boolean) {
    val colors = MaterialTheme.colorScheme
    val (box, tint) = when {
        icon == MasterIcon.ON -> colors.primary to colors.onPrimary
        icon == MasterIcon.PENDING -> colors.tertiary to colors.onTertiary
        problem -> colors.error to colors.onError
        else -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    val background by animateColorAsState(box, label = "masterIconBox")
    IconBox(container = background) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut() },
            label = "masterIcon",
        ) { target ->
            when (target) {
                MasterIcon.ON -> Icon(Icons.Outlined.Check, null, tint = tint, modifier = Modifier.size(28.dp))
                MasterIcon.OFF -> Icon(Icons.Outlined.Close, null, tint = tint, modifier = Modifier.size(28.dp))
                MasterIcon.PENDING -> {
                    val rotation by rememberInfiniteTransition(label = "pending").animateFloat(
                        initialValue = 0f,
                        targetValue = -360f,
                        animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing), RepeatMode.Restart),
                        label = "pendingRotation",
                    )
                    Icon(Icons.Outlined.Sync, null, tint = tint, modifier = Modifier.size(28.dp).rotate(rotation))
                }
            }
        }
    }
}

/** UI-50: optional Shizuku (FR-D13); the switch shows whether it is ready and leads to the next step. */
@Composable
private fun ShizukuRow(status: ShizukuStatus, installed: Boolean, onClick: () -> Unit) {
    val description = when {
        status == ShizukuStatus.READY -> R.string.shizuku_ready
        status == ShizukuStatus.PERMISSION_REQUIRED -> R.string.shizuku_permission_required
        installed -> R.string.shizuku_not_running
        else -> R.string.shizuku_not_installed
    }
    ToggleListItem(
        icon = Icons.Outlined.Terminal,
        title = stringResource(R.string.shizuku_title),
        description = stringResource(description),
        checked = status == ShizukuStatus.READY,
        onCheckedChange = { onClick() },
    )
}

private val Permission.icon: ImageVector
    get() = when (this) {
        Permission.ACCESSIBILITY -> Icons.Outlined.AccessibilityNew
        Permission.NOTIFICATIONS -> Icons.Outlined.Notifications
        Permission.MEDIA_ACCESS -> Icons.Outlined.MusicNote
        Permission.DND_ACCESS -> Icons.Outlined.DoNotDisturbOn
        Permission.BATTERY -> Icons.Outlined.BatteryFull
        Permission.INSTALL_UPDATES -> Icons.Outlined.InstallMobile
    }

private val Permission.titleRes: Int
    get() = when (this) {
        Permission.ACCESSIBILITY -> R.string.permission_accessibility
        Permission.NOTIFICATIONS -> R.string.permission_notifications
        Permission.MEDIA_ACCESS -> R.string.permission_media
        Permission.DND_ACCESS -> R.string.permission_dnd
        Permission.BATTERY -> R.string.permission_battery
        Permission.INSTALL_UPDATES -> R.string.permission_install_updates
    }

/** UI-51: permission icon, name, granted state and a switch. */
@Composable
private fun PermissionRow(item: PermissionItem, onChange: (Permission, Boolean) -> Unit) {
    ToggleListItem(
        icon = item.permission.icon,
        title = stringResource(item.permission.titleRes),
        description = stringResource(if (item.granted) R.string.permission_granted else R.string.permission_not_granted),
        checked = item.granted,
        onCheckedChange = { onChange(item.permission, it) },
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
                status = EngineStatus.Running,
                shizuku = ShizukuStatus.READY,
                permissions = listOf(
                    PermissionItem(Permission.ACCESSIBILITY, true),
                    PermissionItem(Permission.NOTIFICATIONS, false),
                    PermissionItem(Permission.MEDIA_ACCESS, false),
                    PermissionItem(Permission.DND_ACCESS, false),
                    PermissionItem(Permission.BATTERY, true),
                    PermissionItem(Permission.INSTALL_UPDATES, false),
                ),
            ),
            shizukuInstalled = true,
            onEnabledChange = {},
            onShizukuClick = {},
            onPermissionChange = { _, _ -> },
        )
    }
}
