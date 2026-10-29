package io.github.ptt98hd.totch.core.action

import io.github.ptt98hd.totch.core.model.Action

/** Shell commands run by the Shizuku executor (uid shell). */
object ShellCommands {
    private val packageName = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+""")
    private val activityName = Regex("""[A-Za-z0-9_.$]+""")

    /**
     * Command or script for [action], or `null` if it is not a plain shell action (LOCAL actions, None, LaunchApp,
     * LaunchShortcut, LaunchActivity, ClickTile, OneHandedMode — see [OneHandedCommands]).
     */
    fun of(action: Action): String? = when (action) {
        Action.Back -> "input keyevent 4"
        Action.Home -> "input keyevent 3"
        Action.Recents -> "input keyevent 187"
        Action.LockScreen -> "input keyevent 223"
        Action.Screenshot -> "input keyevent 120"
        Action.Notifications -> "cmd statusbar expand-notifications"
        Action.QuickSettings -> "cmd statusbar expand-settings"
        Action.Assistant -> "input keyevent 219" // KEYCODE_ASSIST: the system's default assistant
        Action.BrightnessUp -> "input keyevent 221" // stepped and shown by the system, like the hardware keys
        Action.BrightnessDown -> "input keyevent 220"
        Action.ToggleAutoBrightness -> settingToggle("screen_brightness_mode")
        Action.ToggleAutoRotate -> settingToggle("accelerometer_rotation")
        Action.ToggleWifi -> toggle("global wifi_on", listOf("1", "2"), "svc wifi enable", "svc wifi disable")
        Action.ToggleBluetooth -> toggle(
            "global bluetooth_on",
            listOf("1", "2"),
            "cmd bluetooth_manager enable || svc bluetooth enable", // bluetooth_manager has no shell command before Android 13
            "cmd bluetooth_manager disable || svc bluetooth disable",
        )
        Action.ToggleMobileData -> toggle("global mobile_data", listOf("1"), "svc data enable", "svc data disable")
        Action.ToggleAirplaneMode -> toggle(
            "global airplane_mode_on",
            listOf("1"),
            "cmd connectivity airplane-mode enable",
            "cmd connectivity airplane-mode disable",
        )
        Action.ToggleLocation -> toggle(
            "secure location_mode",
            listOf("1", "2", "3"),
            "cmd location set-location-enabled true",
            "cmd location set-location-enabled false",
        )
        else -> null
    }

    /** Actions spec §13: switches off when the setting holds one of [onValues], on otherwise (also when unset). */
    private fun toggle(setting: String, onValues: List<String>, enable: String, disable: String): String =
        "case \"$(settings get $setting)\" in ${onValues.joinToString("|")}) $disable ;; *) $enable ;; esac"

    /** A 0/1 switch in `Settings.System`. */
    private fun settingToggle(key: String): String =
        toggle("system $key", listOf("1"), "settings put system $key 1", "settings put system $key 0")

    /** `am start` command for an explicit launcher activity; `null` if either name is unsafe (SC-2). */
    fun launch(packageName: String, activityName: String): String? {
        if (!this.packageName.matches(packageName) || !this.activityName.matches(activityName)) return null
        return "am start -n $packageName/$activityName -a android.intent.action.MAIN " +
            "-c android.intent.category.LAUNCHER -f 0x10200000"
    }

    /** `am start` for an exported activity, component single-quoted (`$` in nested class names); `null` if unsafe (SC-7). */
    fun startActivity(packageName: String, activityName: String): String? {
        if (!this.packageName.matches(packageName) || !this.activityName.matches(activityName)) return null
        return "am start -n '$packageName/$activityName' -f 0x10000000"
    }

    /**
     * Clicks the Quick Settings tile of a `TileService`; `null` if unsafe (SC-8). SystemUI only clicks tiles in the
     * panel, so a missing tile is added for the click and removed again (QT-2). The panel stores app tiles as
     * `custom(<flattenToShortString>)`, so both forms of the component are matched.
     */
    fun clickTile(packageName: String, serviceName: String): String? {
        if (!this.packageName.matches(packageName) || !activityName.matches(serviceName)) return null
        val short = serviceName.takeIf { it.startsWith("$packageName.") }?.let { "|*',custom($packageName/${it.removePrefix(packageName)}),'*" }
        return "c='$packageName/$serviceName'; case \",$(settings get secure sysui_qs_tiles),\" in " +
            "*\",custom(\$c),\"*${short.orEmpty()}) cmd statusbar click-tile \"\$c\" ;; " +
            "*) cmd statusbar add-tile \"\$c\" && sleep 0.8 && cmd statusbar click-tile \"\$c\"; r=\$?; " +
            "sleep 2; cmd statusbar remove-tile \"\$c\"; exit \$r ;; esac"
    }

    /** `am start` for an intent URI, single-quoted for the shell; `null` if it is not an intent URI (SC-4..SC-6). */
    fun startIntent(intentUri: String): String? {
        if (!intentUri.startsWith("intent:") && !intentUri.startsWith("#Intent;")) return null
        if (intentUri.any { it.isISOControl() }) return null
        return "am start '" + intentUri.replace("'", "'\\''") + "'"
    }
}
