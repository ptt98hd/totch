package io.github.ptt98hd.totch.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.core.settings.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class BootAction { START_SERVICE, DISABLE, NONE }

/** What woke [BootReceiver] (DM-51). */
enum class StartupEvent { BOOT, PACKAGE_REPLACED }

/** DM-51: a boot, including the quick boot of some makers, or an update of Totch; null for anything else. */
fun startupEvent(action: String?): StartupEvent? = when (action) {
    Intent.ACTION_BOOT_COMPLETED, QUICKBOOT_POWERON, HTC_QUICKBOOT_POWERON -> StartupEvent.BOOT
    Intent.ACTION_MY_PACKAGE_REPLACED -> StartupEvent.PACKAGE_REPLACED
    else -> null
}

/** What to do after boot (DM-20…DM-22) or an update, which never switches gestures off (DM-51). */
fun startupAction(settings: Settings, event: StartupEvent): BootAction = when {
    !settings.enabled -> BootAction.NONE
    event == StartupEvent.BOOT && !settings.startOnBoot -> BootAction.DISABLE
    else -> BootAction.START_SERVICE
}

private const val QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
private const val HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"

/**
 * DM-20: restarts the gesture service after boot (it waits for Shizuku if needed). Done here, while the
 * BOOT_COMPLETED / MY_PACKAGE_REPLACED background-start exemption applies. DM-22: with "start on boot" off, gestures
 * stay off until the user turns them on again.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = startupEvent(intent.action) ?: return
        val platform = (context.applicationContext as TotchApp).platform
        val pending = goAsync()
        platform.scope.launch {
            try {
                when (startupAction(platform.settingsRepository.settings.first(), event)) {
                    BootAction.START_SERVICE -> platform.keepAlive()
                    BootAction.DISABLE -> platform.settingsRepository.update { it.copy(enabled = false) }
                    BootAction.NONE -> Unit
                }
            } finally {
                pending.finish()
            }
        }
    }
}
