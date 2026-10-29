package io.github.ptt98hd.totch.core.update

/** Shell side of the update install through Shizuku (update spec §7). */
object UpdateCommands {
    /** Where the shell user service copies the downloaded APK; readable by `pm`, not by the app. */
    const val APK_PATH = "/data/local/tmp/totch-update.apk"

    private val PACKAGE = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+""")
    private val CLASS = Regex("""[A-Za-z0-9_.]+""")
    private val APK = Regex("""/data/local/tmp/[A-Za-z0-9_-]+\.apk""")

    /** UP-14, UP-18: only a plain file name directly in `/data/local/tmp`. */
    fun isSafeApkPath(path: String): Boolean = APK.matches(path)

    /**
     * UP-14: [shellInstallScript] detached in its own session, so it outlives the app and the Shizuku user service,
     * which both die when the package is replaced. `null` for unsafe names.
     */
    fun shellInstall(packageName: String, activity: String, apkPath: String): String? {
        if (!PACKAGE.matches(packageName) || !CLASS.matches(activity) || !isSafeApkPath(apkPath)) return null
        return "setsid sh -c '${shellInstallScript(packageName, activity, apkPath)}' </dev/null >/dev/null 2>&1 &"
    }

    /** UP-15: installs, removes the copy, then reopens the app — with `UPDATE_FAILED` when `pm` failed. */
    fun shellInstallScript(packageName: String, activity: String, apkPath: String): String {
        val start = "am start -n $packageName/$activity"
        return "sleep 1; pm install -r $apkPath; r=\$?; rm -f $apkPath; " +
            "if [ \$r = 0 ]; then $start; else $start -a $packageName.action.UPDATE_FAILED; fi"
    }
}
