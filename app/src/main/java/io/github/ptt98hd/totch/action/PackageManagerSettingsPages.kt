package io.github.ptt98hd.totch.action

import android.content.Intent
import android.content.pm.PackageManager
import io.github.ptt98hd.totch.SettingsPageSource
import io.github.ptt98hd.totch.core.action.SettingsPages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Settings pages this device has; each action is listed in the manifest's `<queries>` (actions spec §15). */
class PackageManagerSettingsPages(private val pm: PackageManager) : SettingsPageSource {
    override suspend fun available(): Set<String> = withContext(Dispatchers.IO) {
        SettingsPages.all
            .map { it.intentAction }
            .filterTo(mutableSetOf()) { pm.resolveActivity(Intent(it), PackageManager.MATCH_DEFAULT_ONLY) != null }
    }
}
