package io.github.ptt98hd.totch.core.action

import io.github.ptt98hd.totch.core.model.Action

/** Shell commands run by the Shizuku executor (uid shell). */
object ShellCommands {
    private val packageName = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+""")
    private val activityName = Regex("""[A-Za-z0-9_.$]+""")

    /**
     * Command for [action], or `null` if it is not a plain shell action (LOCAL actions, None, LaunchApp, LaunchShortcut,
     * LaunchActivity, ClickTile, OneHandedMode — see [OneHandedCommands]).
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
        else -> null
    }

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
