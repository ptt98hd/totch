package io.github.ptt98hd.totch.core.action

/** Shell commands (uid shell, through Shizuku) that grant or revoke the optional permissions (PC-1..PC-11). */
object PermissionCommands {
    private const val ACCESSIBILITY_SERVICES = "enabled_accessibility_services"
    private val PACKAGE = Regex("""[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+""")
    private val CLASS = Regex("""[A-Za-z0-9_.]+""")

    /**
     * PC-1: writes the enabled accessibility services with [component] added or removed; other services are
     * kept. Short names (`pkg/.Cls`) are compared in their expanded form.
     */
    fun accessibility(current: String?, component: String, enabled: Boolean): List<String> {
        val services = accessibilityServices(current, component, enabled)
        return buildList {
            add(
                if (services.isEmpty()) {
                    "settings delete secure $ACCESSIBILITY_SERVICES"
                } else {
                    "settings put secure $ACCESSIBILITY_SERVICES ${quote(services)}"
                },
            )
            if (enabled) add("settings put secure accessibility_enabled 1")
        }
    }

    /** PC-9: the new value of `enabled_accessibility_services` (`""` when none is left), by the rules of PC-1. */
    fun accessibilityServices(current: String?, component: String, enabled: Boolean): String {
        val target = expand(component)
        val others = current.orEmpty().split(':').map { it.trim() }
            .filter { it.isNotEmpty() && it != "null" && expand(it) != target }
        return (if (enabled) others + component else others).joinToString(":")
    }

    /** PC-2: notification access of a listener component. */
    fun notificationListener(component: String, allowed: Boolean): String =
        "cmd notification ${if (allowed) "allow_listener" else "disallow_listener"} ${quote(component)}"

    /** PC-8: Do Not Disturb access, needed by the sound mode actions (FR-D12). */
    fun dndAccess(packageName: String, allowed: Boolean): String =
        "cmd notification ${if (allowed) "allow_dnd" else "disallow_dnd"} ${quote(packageName)}"

    /** PC-3, PC-3b: battery optimization exemption (device idle whitelist); granting also lifts the background restriction. */
    fun batteryUnrestricted(packageName: String, unrestricted: Boolean): List<String> = buildList {
        add("cmd deviceidle whitelist ${if (unrestricted) "+" else "-"}${quote(packageName)}")
        if (unrestricted) add("cmd appops set ${quote(packageName)} RUN_ANY_IN_BACKGROUND allow")
    }

    /** PC-10: lets Totch turn its accessibility service back on by itself, without Shizuku (FR-D15). */
    fun grantWriteSecureSettings(packageName: String): String =
        "pm grant ${quote(packageName)} android.permission.WRITE_SECURE_SETTINGS"

    /** PC-11: exemption from app hibernation, "pause app activity if unused" (FR-D16). */
    fun keepWhenUnused(packageName: String, keep: Boolean): String =
        "cmd appops set ${quote(packageName)} AUTO_REVOKE_PERMISSIONS_IF_UNUSED ${if (keep) "ignore" else "allow"}"

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
