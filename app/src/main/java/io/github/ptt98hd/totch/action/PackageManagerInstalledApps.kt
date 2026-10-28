package io.github.ptt98hd.totch.action

import android.content.Intent
import android.content.pm.PackageManager
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.InstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Launchable apps, visible through the MAIN/LAUNCHER `<queries>` entry (no QUERY_ALL_PACKAGES). */
class PackageManagerInstalledApps(private val pm: PackageManager) : InstalledApps {

    override suspend fun launchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    override fun labelOf(packageName: String): String? = try {
        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
