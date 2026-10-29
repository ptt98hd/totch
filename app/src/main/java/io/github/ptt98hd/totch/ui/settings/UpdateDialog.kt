package io.github.ptt98hd.totch.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.update.UpdateController
import io.github.ptt98hd.totch.update.UpdateState

/**
 * UI-37, update spec §8: the update dialog in every state, drawn above every screen by `MainActivity`. Opened from the
 * top bar, the Settings row (new release), when the app opens or from the update notification.
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

@Composable
private fun UpdateDialog(
    state: UpdateState,
    versionName: String,
    shizukuReady: Boolean,
    onUpdate: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = (state as? UpdateState.Available)?.let { stringResource(R.string.update_dialog_title, it.release.version.toString()) }
        ?: stringResource(R.string.update_title)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
        title = { Text(title) },
        text = {
            when (state) {
                UpdateState.Idle, UpdateState.Checking -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(stringResource(R.string.update_dialog_checking))
                }
                UpdateState.UpToDate -> Text(stringResource(R.string.update_dialog_up_to_date, versionName))
                is UpdateState.Available -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        state.release.notes.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    )
                    Text(
                        stringResource(if (shizukuReady) R.string.update_via_shizuku else R.string.update_via_system),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is UpdateState.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.update_downloading, (state.progress * 100).toInt()))
                }
                is UpdateState.Installing ->
                    Text(stringResource(if (state.viaShizuku) R.string.update_installing_shizuku else R.string.update_installing))
                UpdateState.Failed -> Text(stringResource(R.string.update_dialog_failed))
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Available -> TextButton(onClick = onUpdate) { Text(stringResource(R.string.update_action)) }
                UpdateState.Failed -> TextButton(onClick = onRetry) { Text(stringResource(R.string.update_retry)) }
                is UpdateState.Downloading -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_hide)) }
                UpdateState.Idle, UpdateState.Checking -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                UpdateState.UpToDate, is UpdateState.Installing ->
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
            }
        },
        dismissButton = when (state) {
            is UpdateState.Available -> {
                { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) } }
            }
            UpdateState.Failed -> {
                { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
            }
            else -> null
        },
    )
}
