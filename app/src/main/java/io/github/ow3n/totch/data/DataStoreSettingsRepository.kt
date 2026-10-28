package io.github.ow3n.totch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.ow3n.totch.SettingsRepository
import io.github.ow3n.totch.core.settings.Settings
import io.github.ow3n.totch.core.settings.SettingsCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Settings persisted as JSON under a single key (ADR-3, docs/specs/settings.md §3). */
class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<Settings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.decode() }
        .distinctUntilChanged()

    override suspend fun update(transform: (Settings) -> Settings) {
        store.edit { it[KEY] = SettingsCodec.encode(transform(it.decode()).sanitized()) }
    }

    private fun Preferences.decode(): Settings = this[KEY]?.let(SettingsCodec::decode) ?: Settings().sanitized()

    companion object {
        val KEY = stringPreferencesKey("settings_json")
    }
}
