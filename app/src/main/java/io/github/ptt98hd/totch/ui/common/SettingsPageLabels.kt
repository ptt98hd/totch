package io.github.ptt98hd.totch.ui.common

import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.action.SettingsGroup

/** Label of each page of `SettingsPages.all` (actions spec §15, SP-3). */
val settingsPageLabels: Map<String, Int> = mapOf(
    "android.settings.WIRELESS_SETTINGS" to R.string.settings_page_wireless,
    "android.settings.WIFI_SETTINGS" to R.string.settings_page_wifi,
    "android.settings.BLUETOOTH_SETTINGS" to R.string.settings_page_bluetooth,
    "android.settings.DATA_USAGE_SETTINGS" to R.string.settings_page_data_usage,
    "android.settings.NETWORK_OPERATOR_SETTINGS" to R.string.settings_page_mobile_network,
    "android.settings.AIRPLANE_MODE_SETTINGS" to R.string.settings_page_airplane,
    "android.settings.NFC_SETTINGS" to R.string.settings_page_nfc,
    "android.settings.VPN_SETTINGS" to R.string.settings_page_vpn,
    "android.settings.CAST_SETTINGS" to R.string.settings_page_cast,
    "android.settings.DISPLAY_SETTINGS" to R.string.settings_page_display,
    "android.settings.NIGHT_DISPLAY_SETTINGS" to R.string.settings_page_night_light,
    "android.settings.DARK_THEME_SETTINGS" to R.string.settings_page_dark_theme,
    "android.settings.SOUND_SETTINGS" to R.string.settings_page_sound,
    "android.settings.ZEN_MODE_SETTINGS" to R.string.settings_page_dnd,
    "android.intent.action.POWER_USAGE_SUMMARY" to R.string.settings_page_battery,
    "android.settings.BATTERY_SAVER_SETTINGS" to R.string.settings_page_battery_saver,
    "android.settings.INTERNAL_STORAGE_SETTINGS" to R.string.settings_page_storage,
    "android.settings.DREAM_SETTINGS" to R.string.settings_page_screen_saver,
    "android.settings.APPLICATION_SETTINGS" to R.string.settings_page_apps,
    "android.settings.MANAGE_DEFAULT_APPS_SETTINGS" to R.string.settings_page_default_apps,
    "android.settings.HOME_SETTINGS" to R.string.settings_page_home_app,
    "android.settings.NOTIFICATION_SETTINGS" to R.string.settings_page_notifications,
    "android.settings.action.MANAGE_OVERLAY_PERMISSION" to R.string.settings_page_overlay,
    "android.settings.USAGE_ACCESS_SETTINGS" to R.string.settings_page_usage_access,
    "android.settings.MANAGE_UNKNOWN_APP_SOURCES" to R.string.settings_page_unknown_apps,
    "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS" to R.string.settings_page_battery_optimization,
    "android.settings.SECURITY_SETTINGS" to R.string.settings_page_security,
    "android.settings.PRIVACY_SETTINGS" to R.string.settings_page_privacy,
    "android.settings.LOCATION_SOURCE_SETTINGS" to R.string.settings_page_location,
    "android.settings.SYNC_SETTINGS" to R.string.settings_page_accounts,
    "android.settings.SETTINGS" to R.string.settings_page_settings,
    "android.settings.ACCESSIBILITY_SETTINGS" to R.string.settings_page_accessibility,
    "android.settings.LOCALE_SETTINGS" to R.string.settings_page_languages,
    "android.settings.INPUT_METHOD_SETTINGS" to R.string.settings_page_keyboards,
    "android.settings.DATE_SETTINGS" to R.string.settings_page_date_time,
    "android.settings.APPLICATION_DEVELOPMENT_SETTINGS" to R.string.settings_page_developer,
    "android.settings.DEVICE_INFO_SETTINGS" to R.string.settings_page_about,
    "android.settings.panel.action.INTERNET_CONNECTIVITY" to R.string.settings_page_panel_internet,
    "android.settings.panel.action.WIFI" to R.string.settings_page_panel_wifi,
    "android.settings.panel.action.NFC" to R.string.settings_page_panel_nfc,
    "android.settings.panel.action.VOLUME" to R.string.settings_page_panel_volume,
)

val settingsGroupLabels: Map<SettingsGroup, Int> = mapOf(
    SettingsGroup.CONNECTIONS to R.string.settings_group_connections,
    SettingsGroup.DEVICE to R.string.settings_group_device,
    SettingsGroup.APPS to R.string.settings_group_apps,
    SettingsGroup.SECURITY to R.string.settings_group_security,
    SettingsGroup.SYSTEM to R.string.settings_group_system,
    SettingsGroup.PANELS to R.string.settings_group_panels,
)
