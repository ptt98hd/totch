package io.github.ptt98hd.totch.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.core.settings.DetectionMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * DM-20: restarts the Shizuku-mode service after boot (it waits for Shizuku if needed). Done here,
 * while the BOOT_COMPLETED background-start exemption applies. DM-21: the system rebinds the
 * accessibility service by itself.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val platform = (context.applicationContext as TotchApp).platform
        val pending = goAsync()
        platform.scope.launch {
            try {
                val settings = platform.settingsRepository.settings.first()
                if (settings.enabled && settings.mode == DetectionMode.SHIZUKU) {
                    NotchForegroundService.setRunning(context.applicationContext, true)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
