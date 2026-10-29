package io.github.ptt98hd.totch.core.action

import io.github.ptt98hd.totch.core.model.Action

/** Shell commands run by the Shizuku executor (uid shell). */
object ShellCommands {
    private val packageName = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+""")
    private val activityName = Regex("""[A-Za-z0-9_.$]+""")

    /** Command for [action], or `null` if it is not a shell action (LOCAL actions, None, LaunchApp, LaunchShortcut, LaunchActivity). */
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

    /** `am start` for an intent URI, single-quoted for the shell; `null` if it is not an intent URI (SC-4..SC-6). */
    fun startIntent(intentUri: String): String? {
        if (!intentUri.startsWith("intent:") && !intentUri.startsWith("#Intent;")) return null
        if (intentUri.any { it.isISOControl() }) return null
        return "am start '" + intentUri.replace("'", "'\\''") + "'"
    }
}
