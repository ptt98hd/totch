package io.github.ptt98hd.totch.service

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import io.github.ptt98hd.totch.accessibility.NotchAccessibilityService
import io.github.ptt98hd.totch.core.action.PermissionCommands

/** How a lost accessibility service is turned back on (DM-54). */
enum class RestoreMethod { NONE, WRITE, SHIZUKU }

/**
 * FR-D15: turns the accessibility service back on when Android dropped it (a force stop removes it from the enabled
 * list) while the user still wants it ([io.github.ptt98hd.totch.core.settings.Settings.accessibilityWanted]).
 */
object AccessibilityKeeper {
    private const val TAG = "Totch/A11yKeeper"

    /** DM-54 */
    fun decide(wanted: Boolean, enabled: Boolean, canWriteSecure: Boolean, shizukuReady: Boolean): RestoreMethod = when {
        !wanted || enabled -> RestoreMethod.NONE
        canWriteSecure -> RestoreMethod.WRITE
        shizukuReady -> RestoreMethod.SHIZUKU
        else -> RestoreMethod.NONE
    }

    fun canWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun component(context: Context): String = ComponentName(context, NotchAccessibilityService::class.java).flattenToString()

    fun isEnabled(context: Context): Boolean = enabledServices(context).orEmpty().split(':')
        .any { ComponentName.unflattenFromString(it) == ComponentName.unflattenFromString(component(context)) }

    /** Adds or removes the service directly in `Settings.Secure` (PC-9); needs WRITE_SECURE_SETTINGS. */
    fun write(context: Context, enabled: Boolean): Boolean = runCatching {
        val resolver = context.contentResolver
        val value = PermissionCommands.accessibilityServices(enabledServices(context), component(context), enabled)
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, value.ifEmpty { null })
        if (enabled) Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        true
    }.onFailure { Log.w(TAG, "cannot write secure settings", it) }.getOrDefault(false)

    private fun enabledServices(context: Context): String? =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
}
