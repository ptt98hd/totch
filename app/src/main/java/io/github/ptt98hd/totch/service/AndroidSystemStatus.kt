package io.github.ptt98hd.totch.service

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import android.os.Build
import io.github.ptt98hd.totch.Permission
import io.github.ptt98hd.totch.Requirements
import io.github.ptt98hd.totch.SystemStatus
import io.github.ptt98hd.totch.accessibility.AccessibilityBridge
import io.github.ptt98hd.totch.accessibility.NotchAccessibilityService
import io.github.ptt98hd.totch.action.MediaListenerService
import io.github.ptt98hd.totch.core.action.PermissionCommands
import io.github.ptt98hd.totch.shizuku.ShizukuManager
import io.github.ptt98hd.totch.shizuku.ShizukuShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Permission/requirement states for the Home screen (FR-D4). */
class AndroidSystemStatus(
    private val context: Context,
    private val shizuku: ShizukuManager,
    private val shell: ShizukuShell,
    accessibility: AccessibilityBridge,
    scope: CoroutineScope,
) : SystemStatus {
    private val component = ComponentName(context, NotchAccessibilityService::class.java)
    private val accessibilityEnabled = MutableStateFlow(false)
    private val notificationsGranted = MutableStateFlow(false)
    private val mediaAccessGranted = MutableStateFlow(false)
    private val batteryUnrestricted = MutableStateFlow(false)
    private val powerManager = context.getSystemService(PowerManager::class.java)

    override val requirements: StateFlow<Requirements> = combine(
        combine(shizuku.state, accessibilityEnabled, accessibility.service, ::Triple),
        notificationsGranted, mediaAccessGranted, batteryUnrestricted,
    ) { (s, enabled, service), notifications, media, battery ->
        Requirements(s.running, s.granted, enabled || service != null, notifications, media, battery)
    }.stateIn(scope, SharingStarted.Eagerly, Requirements())

    init {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, observer,
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(ENABLED_NOTIFICATION_LISTENERS), false, observer,
        )
        refresh()
    }

    override fun refresh() {
        shizuku.refresh()
        accessibilityEnabled.value = Settings.Secure
            .getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty()
            .split(':')
            .any { ComponentName.unflattenFromString(it) == component }
        notificationsGranted.value = NotificationManagerCompat.from(context).areNotificationsEnabled()
        mediaAccessGranted.value = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
        batteryUnrestricted.value = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    override fun requestShizukuPermission() = shizuku.requestPermission()

    override suspend fun setPermission(permission: Permission, granted: Boolean): Boolean {
        val pkg = context.packageName
        val commands = when (permission) {
            Permission.ACCESSIBILITY -> PermissionCommands.accessibility(
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                component.flattenToString(),
                granted,
            )
            Permission.MEDIA_ACCESS -> listOf(
                PermissionCommands.notificationListener(
                    ComponentName(context, MediaListenerService::class.java).flattenToString(), granted,
                ),
            )
            Permission.BATTERY -> listOf(PermissionCommands.batteryUnrestricted(pkg, granted))
            // Revoking a runtime permission kills the app; Android 12 has no runtime notification permission.
            Permission.NOTIFICATIONS ->
                if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(PermissionCommands.grantNotifications(pkg))
                } else {
                    return false
                }
        }
        return shell.exec(commands).also { refresh() }
    }

    private companion object {
        /** Hidden `Settings.Secure.ENABLED_NOTIFICATION_LISTENERS`; stable since API 18. */
        const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    }
}
