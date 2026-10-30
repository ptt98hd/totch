package io.github.ptt98hd.totch.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * FR-U59: auto-start / background managers that some makers add on top of Android (dontkillmyapp.com); the first one
 * that resolves on this phone is offered on Home. Every package is also listed in the manifest's `<queries>` (DM-56).
 */
object OemSettings {
    val autostart: List<String> = listOf(
        // Xiaomi, Redmi, POCO
        "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
        // Huawei
        "com.huawei.systemmanager/com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        "com.huawei.systemmanager/com.huawei.systemmanager.optimize.process.ProtectActivity",
        "com.huawei.systemmanager/com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
        // Honor
        "com.hihonor.systemmanager/com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        // OPPO, realme, OnePlus (ColorOS)
        "com.coloros.safecenter/com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter/com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.oppo.safe/com.oppo.safe.permission.startup.StartupAppListActivity",
        // OnePlus (OxygenOS)
        "com.oneplus.security/com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
        // vivo, iQOO
        "com.vivo.permissionmanager/com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.iqoo.secure/com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
        "com.iqoo.secure/com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        // Samsung (battery: never sleeping apps)
        "com.samsung.android.lool/com.samsung.android.sm.battery.ui.BatteryActivity",
        // Asus
        "com.asus.mobilemanager/com.asus.mobilemanager.autostart.AutoStartActivity",
        "com.asus.mobilemanager/com.asus.mobilemanager.entry.FunctionActivity",
        // Letv
        "com.letv.android.letvsafe/com.letv.android.letvsafe.AutobootManageActivity",
    )

    val packages: Set<String> = autostart.mapTo(linkedSetOf()) { it.substringBefore('/') }

    /** The first auto-start page that exists and can be opened, or null (stock Android, Pixel…). */
    fun autostartIntent(context: Context): Intent? = autostart.asSequence()
        .map { Intent().setComponent(ComponentName.unflattenFromString(it)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        .firstOrNull { intent ->
            runCatching { context.packageManager.resolveActivity(intent, 0)?.activityInfo?.exported == true }
                .getOrDefault(false)
        }
}
