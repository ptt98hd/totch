package io.github.ptt98hd.totch.action

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.net.Uri
import android.util.Log
import io.github.ptt98hd.totch.AppShortcuts
import io.github.ptt98hd.totch.ShortcutEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

/**
 * Static shortcuts declared through `android.app.shortcuts` meta-data of launcher activities
 * (actions spec §7, ADR-6). Only shortcuts whose target activity exists and is exported are kept,
 * since Totch opens them with a plain `startActivity` / `am start`.
 */
class PackageManagerAppShortcuts(private val pm: PackageManager) : AppShortcuts {

    override suspend fun shortcuts(): List<ShortcutEntry> = withContext(Dispatchers.IO) {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcher, PackageManager.GET_META_DATA)
            .flatMap { info ->
                try {
                    shortcutsOf(info)
                } catch (e: Exception) { // malformed XML, missing resources, package removed meanwhile…
                    Log.w(TAG, "Skipping shortcuts of ${info.activityInfo.packageName}", e)
                    emptyList()
                }
            }
            .distinctBy { it.packageName to it.id }
    }

    private fun shortcutsOf(info: ResolveInfo): List<ShortcutEntry> {
        val activity = info.activityInfo
        val xmlId = activity.metaData?.getInt(META_SHORTCUTS, 0)?.takeIf { it != 0 } ?: return emptyList()
        val packageName = activity.packageName
        val appLabel = info.loadLabel(pm).toString()
        val res = pm.getResourcesForApplication(activity.applicationInfo)
        return res.getXml(xmlId).use { parser -> parse(parser, res, packageName) }
            .mapNotNull { (id, label, intent) ->
                if (!isLaunchable(intent)) return@mapNotNull null
                ShortcutEntry(packageName, appLabel, id, label, intent.toUri(Intent.URI_INTENT_SCHEME))
            }
    }

    private data class Parsed(val id: String, val label: String, val intent: Intent)

    private fun parse(parser: XmlResourceParser, res: Resources, publisher: String): List<Parsed> {
        val out = mutableListOf<Parsed>()
        var id: String? = null
        var label: String? = null
        var enabled = true
        var intent: Intent? = null // the last <intent> of the current <shortcut>
        while (true) {
            when (parser.next()) {
                XmlPullParser.END_DOCUMENT -> return out
                XmlPullParser.START_TAG -> when (parser.name) {
                    "shortcut" -> {
                        id = parser.getAttributeValue(ANDROID_NS, "shortcutId")
                        enabled = parser.getAttributeBooleanValue(ANDROID_NS, "enabled", true)
                        label = parser.text(res, "shortcutShortLabel") ?: parser.text(res, "shortcutLongLabel")
                        intent = null
                    }
                    "intent" -> intent = parser.intent(publisher)
                    "extra" -> {
                        val name = parser.getAttributeValue(ANDROID_NS, "name")
                        val value = parser.getAttributeValue(ANDROID_NS, "value")
                        if (name != null && value != null) intent?.putExtra(name, value)
                    }
                    "categories" -> parser.getAttributeValue(ANDROID_NS, "name")?.let { intent?.addCategory(it) }
                }
                XmlPullParser.END_TAG -> if (parser.name == "shortcut") {
                    val i = intent
                    val l = label
                    if (enabled && id != null && !l.isNullOrBlank() && i != null) out += Parsed(id, l, i)
                    id = null
                    label = null
                    intent = null
                }
            }
        }
    }

    private fun XmlResourceParser.intent(publisher: String): Intent {
        val targetPackage = getAttributeValue(ANDROID_NS, "targetPackage") ?: publisher
        val intent = Intent(getAttributeValue(ANDROID_NS, "action") ?: Intent.ACTION_VIEW)
        getAttributeValue(ANDROID_NS, "data")?.let { intent.data = Uri.parse(it) }
        val targetClass = getAttributeValue(ANDROID_NS, "targetClass")
        if (targetClass != null) {
            val cls = if (targetClass.startsWith(".")) targetPackage + targetClass else targetClass
            intent.component = ComponentName(targetPackage, cls)
        } else {
            intent.setPackage(targetPackage)
        }
        return intent
    }

    /** Literal string or `@string/…` reference resolved in the publishing app's resources. */
    private fun XmlResourceParser.text(res: Resources, attr: String): String? {
        val resId = getAttributeResourceValue(ANDROID_NS, attr, 0)
        return if (resId != 0) runCatching { res.getString(resId) }.getOrNull() else getAttributeValue(ANDROID_NS, attr)
    }

    /** The target must exist and be exported, otherwise another app (Totch or shell) cannot open it. */
    private fun isLaunchable(intent: Intent): Boolean = try {
        val activity = intent.component?.let { pm.getActivityInfo(it, 0) }
            ?: pm.resolveActivity(intent, 0)?.activityInfo
        activity != null && activity.exported && activity.enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val TAG = "Totch/Shortcuts"
        const val META_SHORTCUTS = "android.app.shortcuts"
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
