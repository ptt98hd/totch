package io.github.ptt98hd.totch.ui.common

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
import io.github.ptt98hd.totch.action.MediaListenerService
import io.github.ptt98hd.totch.service.OemSettings

const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
const val SHIZUKU_WEBSITE = "https://shizuku.rikka.app/"

fun Context.isShizukuInstalled(): Boolean = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE) != null

/** Opens the Shizuku app, or its website when it is not installed. */
fun Context.openShizuku() {
    val intent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        ?: Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_WEBSITE))
    safeStart(intent)
}

/**
 * FR-U52: opens [url] in a Custom Tab inside Totch, coloured for the [dark] theme; with any app that can view it when
 * no browser supports Custom Tabs.
 */
fun Context.openInApp(url: String, dark: Boolean) {
    val uri = Uri.parse(url)
    val tab = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setColorScheme(if (dark) CustomTabsIntent.COLOR_SCHEME_DARK else CustomTabsIntent.COLOR_SCHEME_LIGHT)
        .build()
    try {
        tab.launchUrl(this, uri)
    } catch (_: ActivityNotFoundException) {
        safeStart(Intent(Intent.ACTION_VIEW, uri))
    }
}

fun Context.openAccessibilitySettings() = safeStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

fun Context.openAppNotificationSettings() = safeStart(
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
)

/** Notification access page of the media listener, falling back to the full list. */
/** FR-D12: the system list of apps with Do Not Disturb access. */
fun Context.openDndAccessSettings() {
    safeStart(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
}

fun Context.openMediaAccessSettings() {
    val component = ComponentName(this, MediaListenerService::class.java).flattenToString()
    val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
    if (!safeStart(detail)) safeStart(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}

/** FR-D7: system dialog to exempt Totch from battery optimization, else the full list. */
@SuppressLint("BatteryLife") // F-Droid app; the exemption keeps the gesture service alive.
fun Context.requestUnrestrictedBattery() {
    val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
    if (!safeStart(request)) safeStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

/** List of apps and their battery optimization, where the exemption can be removed. */
fun Context.openBatteryOptimizationSettings() = safeStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

/** FR-D11: "Install unknown apps" page of Totch. */
fun Context.openInstallUnknownAppsSettings() = safeStart(
    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")),
)

/** FR-D16: Totch's "pause app activity if unused" switch, else its app info page. */
fun Context.openKeepWhenUnusedSettings() {
    val uri = Uri.fromParts("package", packageName, null)
    if (!safeStart(Intent(Intent.ACTION_AUTO_REVOKE_PERMISSIONS, uri))) {
        safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
    }
}

/** FR-U59: the maker's auto-start page found by [OemSettings], else Totch's app info page. */
fun Context.openAutostartSettings() {
    val page = OemSettings.autostartIntent(this)
    if (page == null || !safeStart(page)) {
        safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }
}

private fun Context.safeStart(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false // Nothing sensible to open on this device.
} catch (_: SecurityException) {
    false
}
