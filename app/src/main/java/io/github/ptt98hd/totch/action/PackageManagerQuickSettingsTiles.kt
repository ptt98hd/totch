package io.github.ptt98hd.totch.action

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.TileService
import io.github.ptt98hd.totch.QuickSettingsTiles
import io.github.ptt98hd.totch.TileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Tile services SystemUI can bind (actions spec §9), visible through `<queries>` for `ACTION_QS_TILE`. */
class PackageManagerQuickSettingsTiles(private val pm: PackageManager, private val ownPackage: String) : QuickSettingsTiles {

    override suspend fun tiles(): List<TileEntry> = withContext(Dispatchers.IO) {
        pm.queryIntentServices(Intent(TileService.ACTION_QS_TILE), 0)
            .map { it.serviceInfo }
            .filter {
                it.exported && it.enabled && it.permission == Manifest.permission.BIND_QUICK_SETTINGS_TILE &&
                    it.packageName != ownPackage
            }
            .map { TileEntry(it.packageName, it.applicationInfo.loadLabel(pm).toString(), it.name, it.loadLabel(pm).toString()) }
    }
}
