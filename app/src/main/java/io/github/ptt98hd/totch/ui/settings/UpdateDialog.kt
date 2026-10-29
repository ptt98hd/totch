package io.github.ptt98hd.totch.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.ui.common.DialogButton
import io.github.ptt98hd.totch.ui.common.DialogChip
import io.github.ptt98hd.totch.ui.common.DialogText
import io.github.ptt98hd.totch.ui.common.DialogTextButton
import io.github.ptt98hd.totch.ui.common.TotchDialog
import io.github.ptt98hd.totch.update.UpdateController
import io.github.ptt98hd.totch.update.UpdateState

/**
 * UI-37, update spec §8: the update dialog in every state, drawn above every screen by `MainActivity`. Opened from the
 * top bar, the Settings row (UP-23), when the app opens or from the update notification.
 */
@Composable
fun UpdateDialogHost(updates: UpdateController, versionName: String, shizukuReady: Boolean) {
    val ui by updates.state.collectAsStateWithLifecycle()
    if (!ui.dialog || ui.state == UpdateState.Idle) return
    UpdateDialog(
        state = ui.state,
        versionName = versionName,
        shizukuReady = shizukuReady,
        onUpdate = updates::startUpdate,
        onRetry = updates::openDialog,
        onDismiss = updates::dismissDialog,
    )
}

/** FR-U48: the update dialog drawn with [TotchDialog]; one look per [UpdateState] (update spec §8). */
@Composable
private fun UpdateDialog(
    state: UpdateState,
    versionName: String,
    shizukuReady: Boolean,
    onUpdate: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    when (state) {
        UpdateState.Idle, UpdateState.Checking -> TotchDialog(
            onDismissRequest = onDismiss,
            icon = Icons.Outlined.SystemUpdate,
            title = stringResource(R.string.update_dialog_checking),
            busy = true,
            buttons = { DialogTextButton(stringResource(R.string.close), onDismiss) },
        )
        UpdateState.UpToDate -> TotchDialog(
            onDismissRequest = onDismiss,
            icon = Icons.Outlined.CheckCircle,
            title = stringResource(R.string.update_up_to_date),
            subtitle = { DialogChip("Totch $versionName") },
            buttons = { DialogButton(stringResource(R.string.ok), onDismiss) },
        )
        is UpdateState.Available -> {
            val version = state.release.version.toString()
            TotchDialog(
                onDismissRequest = onDismiss,
                icon = Icons.Outlined.SystemUpdate,
                title = stringResource(R.string.update_dialog_title, version),
                subtitle = { DialogChip(stringResource(R.string.update_version_change, versionName, version)) },
                content = {
                    val notes = state.release.notes.trim()
                    if (notes.isNotEmpty()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceContainer, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.update_whats_new),
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.primary,
                            )
                            Text(
                                notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface,
                                modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            if (shizukuReady) Icons.Outlined.Terminal else Icons.Outlined.InstallMobile,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            stringResource(if (shizukuReady) R.string.update_via_shizuku else R.string.update_via_system),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                },
                buttons = {
                    DialogButton(stringResource(R.string.update_action), onUpdate, icon = Icons.Outlined.Download)
                    DialogTextButton(stringResource(R.string.update_later), onDismiss)
                },
            )
        }
        is UpdateState.Downloading -> TotchDialog(
            onDismissRequest = onDismiss,
            icon = Icons.Outlined.Download,
            title = stringResource(R.string.update_downloading, (state.progress * 100).toInt()),
            progress = state.progress,
            content = { LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth()) },
            buttons = { DialogTextButton(stringResource(R.string.update_hide), onDismiss) },
        )
        is UpdateState.Installing -> TotchDialog(
            onDismissRequest = onDismiss,
            icon = Icons.Outlined.InstallMobile,
            title = stringResource(if (state.viaShizuku) R.string.update_installing_shizuku else R.string.update_installing),
            busy = state.viaShizuku,
            buttons = { DialogButton(stringResource(R.string.ok), onDismiss) },
        )
        UpdateState.Failed -> TotchDialog(
            onDismissRequest = onDismiss,
            icon = Icons.Outlined.ErrorOutline,
            title = stringResource(R.string.update_install_failed),
            iconContainer = colors.errorContainer,
            iconContent = colors.onErrorContainer,
            content = { DialogText(stringResource(R.string.update_dialog_failed)) },
            buttons = {
                DialogButton(stringResource(R.string.update_retry), onRetry)
                DialogTextButton(stringResource(R.string.close), onDismiss)
            },
        )
    }
}
