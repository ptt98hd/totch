package io.github.ptt98hd.totch.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import io.github.ptt98hd.totch.core.settings.AppLanguage
import java.util.Locale

/**
 * Display language (ui spec LG-1…LG-3). Android 13+ stores the per-app language in the system
 * ([LocaleManager]); Android 12 keeps a synchronous copy here and contexts are wrapped by [wrap].
 * Without an explicit choice the system locale is used, and unsupported languages fall back to the
 * default (English) resources.
 */
object AppLocales {
    private const val PREFS = "locale"
    private const val KEY_TAG = "tag"

    /** Makes [language] the app language; on Android 12 the caller recreates its activity. */
    fun apply(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            val wanted = language.tag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
            if (manager.applicationLocales != wanted) manager.applicationLocales = wanted
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TAG, language.tag).apply()
        }
    }

    /**
     * LG-3: language chosen in the system's per-app language settings (Android 13+), or `null` when
     * unknown (Android 12, or a language Totch does not ship).
     */
    fun systemSelection(context: Context): AppLanguage? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales ?: return null
        if (locales.isEmpty) return AppLanguage.SYSTEM
        val language = locales[0].language
        return AppLanguage.entries.firstOrNull { it.tag == language }
    }

    /** Android 12: [base] with the saved language applied; Android 13+: [base] unchanged. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null) ?: return base
        val config = Configuration(base.resources.configuration).apply { setLocales(LocaleList(Locale.forLanguageTag(tag))) }
        return base.createConfigurationContext(config)
    }
}
