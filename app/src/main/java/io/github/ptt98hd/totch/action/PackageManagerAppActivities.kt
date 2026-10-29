package io.github.ptt98hd.totch.action

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import io.github.ptt98hd.totch.ActivityEntry
import io.github.ptt98hd.totch.AppActivities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Activities another app may start (actions spec §8): exported, enabled and not guarded by a permission, of the
 * packages that have a launcher activity (visible through `<queries>`).
 */
class PackageManagerAppActivities(private val pm: PackageManager) : AppActivities {

    override suspend fun activities(): List<ActivityEntry> = withContext(Dispatchers.IO) {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcher, 0)
            .distinctBy { it.activityInfo.packageName }
            .flatMap { info ->
                val packageName = info.activityInfo.packageName
                try {
                    val appLabel = info.loadLabel(pm).toString()
                    pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities.orEmpty()
                        .filter { it.exported && it.enabled && it.permission == null }
                        .map { ActivityEntry(packageName, appLabel, it.name, it.loadLabel(pm).toString()) }
                } catch (e: Exception) { // package removed meanwhile, oversized binder transaction…
                    Log.w(TAG, "Skipping activities of $packageName", e)
                    emptyList()
                }
            }
    }

    private companion object {
        const val TAG = "Totch/Activities"
    }
}
