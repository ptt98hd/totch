package io.github.ptt98hd.totch.core.action

/** Shell commands (uid shell, through Shizuku) that grant or revoke the optional permissions (PC-1..PC-4). */
object PermissionCommands {
    private const val ACCESSIBILITY_SERVICES = "enabled_accessibility_services"
    private val PACKAGE = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+""")
    private val CLASS = Regex("""[A-Za-z0-9_.]+""")

    /**
     * PC-1: writes the enabled accessibility services with [component] added or removed; other services are
     * kept. Short names (`pkg/.Cls`) are compared in their expanded form.
     */
    fun accessibility(current: String?, component: String, enabled: Boolean): List<String> {
        val target = expand(component)
        val others = current.orEmpty().split(':').map { it.trim() }
            .filter { it.isNotEmpty() && it != "null" && expand(it) != target }
        val services = if (enabled) others + component else others
        return buildList {
            add(
                if (services.isEmpty()) {
                    "settings delete secure $ACCESSIBILITY_SERVICES"
                } else {
                    "settings put secure $ACCESSIBILITY_SERVICES ${quote(services.joinToString(":"))}"
                },
            )
            if (enabled) add("settings put secure accessibility_enabled 1")
        }
    }

    /** PC-2: notification access of a listener component. */
    fun notificationListener(component: String, allowed: Boolean): String =
        "cmd notification ${if (allowed) "allow_listener" else "disallow_listener"} ${quote(component)}"

    /** PC-3: battery optimization exemption (device idle whitelist). */
    fun batteryUnrestricted(packageName: String, unrestricted: Boolean): String =
        "cmd deviceidle whitelist ${if (unrestricted) "+" else "-"}${quote(packageName)}"

    /** PC-4: runtime notification permission (Android 13+). */
    fun grantNotifications(packageName: String): String =
        "pm grant ${quote(packageName)} android.permission.POST_NOTIFICATIONS"

    /**
     * PC-5: revoking a runtime permission kills the app, so the revoke runs detached in its own session (it outlives
     * the Shizuku user service) after a short delay, then reopens [activity]. `null` for unsafe names.
     */
    fun revokeNotificationsAndRestart(packageName: String, activity: String): String? =
        detachedThenRestart(packageName, activity, "pm revoke $packageName android.permission.POST_NOTIFICATIONS")

    /** PC-6: "Install unknown apps" for the self-update (FR-D11). */
    fun allowInstallPackages(packageName: String): String =
        "appops set ${quote(packageName)} REQUEST_INSTALL_PACKAGES allow"

    /** PC-7: denying the app-op also kills the app, so it is detached like [revokeNotificationsAndRestart]. */
    fun denyInstallPackagesAndRestart(packageName: String, activity: String): String? =
        detachedThenRestart(packageName, activity, "appops set $packageName REQUEST_INSTALL_PACKAGES deny")

    private fun detachedThenRestart(packageName: String, activity: String, command: String): String? {
        if (!PACKAGE.matches(packageName) || !CLASS.matches(activity)) return null
        val script = "sleep 1; $command; am start -n $packageName/$activity"
        return "setsid sh -c ${quote(script)} </dev/null >/dev/null 2>&1 &"
    }

    private fun expand(component: String): String {
        val pkg = component.substringBefore('/')
        val cls = component.substringAfter('/', "")
        return if (cls.startsWith('.')) "$pkg/$pkg$cls" else component
    }

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
