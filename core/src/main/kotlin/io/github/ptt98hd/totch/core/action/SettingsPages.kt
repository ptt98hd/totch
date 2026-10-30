package io.github.ptt98hd.totch.core.action

enum class SettingsGroup { CONNECTIONS, DEVICE, APPS, SECURITY, SYSTEM, PANELS }

data class SettingsPage(val intentAction: String, val group: SettingsGroup)

/** Curated system Settings pages of [io.github.ptt98hd.totch.core.model.Action.OpenSettings] (actions spec §15). */
object SettingsPages {
    private fun page(group: SettingsGroup, name: String) =
        SettingsPage(if ('.' in name) name else "android.settings.$name", group)

    val all: List<SettingsPage> = listOf(
        page(SettingsGroup.CONNECTIONS, "WIRELESS_SETTINGS"),
        page(SettingsGroup.CONNECTIONS, "WIFI_SETTINGS"),
        page(SettingsGroup.CONNECTIONS, "BLUETOOTH_SETTINGS"),
        page(SettingsGroup.CONNECTIONS, "DATA_USAGE_SETTINGS"),
        page(SettingsGroup.CONNECTIONS, "NETWORK_OPERATOR_SETTINGS"),
        page(SettingsGroup.CONNECTIONS, "AIRPLANE_MODE_SETTINGS"),
        page(SettingsGroup.CONNECTIONS, "NFC_SETTINGS"),
        page(SettingsGroup.CONNECTIONS, "VPN_SETTINGS"),
        page(SettingsGroup.CONNECTIONS, "CAST_SETTINGS"),
        page(SettingsGroup.DEVICE, "DISPLAY_SETTINGS"),
        page(SettingsGroup.DEVICE, "NIGHT_DISPLAY_SETTINGS"),
        page(SettingsGroup.DEVICE, "DARK_THEME_SETTINGS"),
        page(SettingsGroup.DEVICE, "SOUND_SETTINGS"),
        page(SettingsGroup.DEVICE, "ZEN_MODE_SETTINGS"),
        page(SettingsGroup.DEVICE, "android.intent.action.POWER_USAGE_SUMMARY"),
        page(SettingsGroup.DEVICE, "BATTERY_SAVER_SETTINGS"),
        page(SettingsGroup.DEVICE, "INTERNAL_STORAGE_SETTINGS"),
        page(SettingsGroup.DEVICE, "DREAM_SETTINGS"),
        page(SettingsGroup.APPS, "APPLICATION_SETTINGS"),
        page(SettingsGroup.APPS, "MANAGE_DEFAULT_APPS_SETTINGS"),
        page(SettingsGroup.APPS, "HOME_SETTINGS"),
        page(SettingsGroup.APPS, "NOTIFICATION_SETTINGS"),
        page(SettingsGroup.APPS, "android.settings.action.MANAGE_OVERLAY_PERMISSION"),
        page(SettingsGroup.APPS, "USAGE_ACCESS_SETTINGS"),
        page(SettingsGroup.APPS, "MANAGE_UNKNOWN_APP_SOURCES"),
        page(SettingsGroup.APPS, "IGNORE_BATTERY_OPTIMIZATION_SETTINGS"),
        page(SettingsGroup.SECURITY, "SECURITY_SETTINGS"),
        page(SettingsGroup.SECURITY, "PRIVACY_SETTINGS"),
        page(SettingsGroup.SECURITY, "LOCATION_SOURCE_SETTINGS"),
        page(SettingsGroup.SECURITY, "SYNC_SETTINGS"),
        page(SettingsGroup.SYSTEM, "SETTINGS"),
        page(SettingsGroup.SYSTEM, "ACCESSIBILITY_SETTINGS"),
        page(SettingsGroup.SYSTEM, "LOCALE_SETTINGS"),
        page(SettingsGroup.SYSTEM, "INPUT_METHOD_SETTINGS"),
        page(SettingsGroup.SYSTEM, "DATE_SETTINGS"),
        page(SettingsGroup.SYSTEM, "APPLICATION_DEVELOPMENT_SETTINGS"),
        page(SettingsGroup.SYSTEM, "DEVICE_INFO_SETTINGS"),
        page(SettingsGroup.PANELS, "android.settings.panel.action.INTERNET_CONNECTIVITY"),
        page(SettingsGroup.PANELS, "android.settings.panel.action.WIFI"),
        page(SettingsGroup.PANELS, "android.settings.panel.action.NFC"),
        page(SettingsGroup.PANELS, "android.settings.panel.action.VOLUME"),
    )
}
