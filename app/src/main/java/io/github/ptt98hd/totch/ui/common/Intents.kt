package io.github.ptt98hd.totch.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
const val SHIZUKU_WEBSITE = "https://shizuku.rikka.app/"

fun Context.isShizukuInstalled(): Boolean = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE) != null

/** Opens the Shizuku app, or its website when it is not installed. */
fun Context.openShizuku() {
    val intent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        ?: Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_WEBSITE))
    safeStart(intent)
}

fun Context.openAccessibilitySettings() = safeStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

fun Context.openAppNotificationSettings() = safeStart(
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
)

private fun Context.safeStart(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Nothing sensible to open on this device.
    }
}
