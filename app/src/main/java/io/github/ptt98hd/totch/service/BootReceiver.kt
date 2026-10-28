package io.github.ptt98hd.totch.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.core.settings.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class BootAction { START_SERVICE, DISABLE, NONE }

/** What to do after boot (detection-modes DM-20…DM-22). */
fun bootAction(settings: Settings): BootAction = when {
    !settings.enabled -> BootAction.NONE
    !settings.startOnBoot -> BootAction.DISABLE
    else -> BootAction.START_SERVICE
}

/**
 * DM-20: restarts the gesture service after boot (it waits for Shizuku if needed). Done here, while the
 * BOOT_COMPLETED background-start exemption applies. DM-22: with "start on boot" off, gestures stay off
 * until the user turns them on again.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val platform = (context.applicationContext as TotchApp).platform
        val pending = goAsync()
        platform.scope.launch {
            try {
                when (bootAction(platform.settingsRepository.settings.first())) {
                    BootAction.START_SERVICE -> NotchForegroundService.setRunning(context.applicationContext, true)
                    BootAction.DISABLE -> platform.settingsRepository.update { it.copy(enabled = false) }
                    BootAction.NONE -> Unit
                }
            } finally {
                pending.finish()
            }
        }
    }
}
