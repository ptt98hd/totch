package io.github.ptt98hd.totch.action

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.util.Log
import io.github.ptt98hd.totch.AppIntents
import io.github.ptt98hd.totch.IntentEntry
import io.github.ptt98hd.totch.core.action.IntentFilters
import io.github.ptt98hd.totch.core.action.ManifestFilter
import io.github.ptt98hd.totch.core.model.IntentTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

/**
 * Intents declared by launchable apps (actions spec §16): the compiled `AndroidManifest.xml` gives the intent
 * filters, `PackageInfo` tells which components are enabled, exported and not guarded by a permission.
 */
class ManifestAppIntents(private val context: Context) : AppIntents {
    private val pm = context.packageManager

    private class Component(val name: String, val target: IntentTarget, val filters: MutableList<ManifestFilter> = mutableListOf())

    override suspend fun intents(): List<IntentEntry> = withContext(Dispatchers.IO) {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcher, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != context.packageName }
            .flatMap { info ->
                val packageName = info.activityInfo.packageName
                try {
                    intentsOf(packageName, info.loadLabel(pm).toString())
                } catch (e: Exception) { // package removed meanwhile, unreadable manifest…
                    Log.w(TAG, "Skipping intents of $packageName", e)
                    emptyList()
                }
            }
    }

    private fun intentsOf(packageName: String, appLabel: String): List<IntentEntry> {
        val info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS)
        val usable: Set<String> = (info.activities.orEmpty().asList() + info.receivers.orEmpty().asList())
            .filter { it.isOpen() }
            .mapTo(mutableSetOf()) { it.name }
        val parser = context.createPackageContext(packageName, 0).assets.openXmlResourceParser("AndroidManifest.xml")
        return parser.use { components(it, packageName) }
            .filter { it.name in usable }
            .flatMap { c ->
                IntentFilters.usableActions(c.target, c.filters).map { IntentEntry(packageName, appLabel, c.name, it, c.target) }
            }
    }

    /** Receivers are described by [ActivityInfo] too. */
    private fun ActivityInfo.isOpen(): Boolean = exported && enabled && permission == null

    /** Components with their filters; `<data>` only marks a filter, its values are not needed. */
    private fun components(parser: XmlResourceParser, packageName: String): List<Component> {
        val result = mutableListOf<Component>()
        var component: Component? = null
        var actions: MutableList<String>? = null
        var hasData = false
        while (true) {
            when (parser.next()) {
                XmlPullParser.END_DOCUMENT -> return result
                XmlPullParser.START_TAG -> when (parser.name) {
                    "activity", "activity-alias", "receiver" -> {
                        val target = if (parser.name == "receiver") IntentTarget.BROADCAST else IntentTarget.ACTIVITY
                        component = parser.getAttributeValue(ANDROID, "name")
                            ?.let { Component(IntentFilters.resolveClassName(packageName, it), target) }
                            ?.also { result += it }
                    }
                    "intent-filter" -> if (component != null) {
                        actions = mutableListOf()
                        hasData = false
                    }
                    "action" -> parser.getAttributeValue(ANDROID, "name")?.let { actions?.add(it) }
                    "data" -> hasData = true
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "activity", "activity-alias", "receiver" -> component = null
                    "intent-filter" -> {
                        actions?.let { component?.filters?.add(ManifestFilter(it, hasData)) }
                        actions = null
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "Totch/Intents"
        const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
