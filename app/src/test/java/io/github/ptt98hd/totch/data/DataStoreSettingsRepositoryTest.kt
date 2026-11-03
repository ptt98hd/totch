package io.github.ptt98hd.totch.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.LegacyDisplay
import io.github.ptt98hd.totch.core.settings.NotchRegionPx
import io.github.ptt98hd.totch.core.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals

class DataStoreSettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { tmp.root.resolve("settings.preferences_pb") }
    }
    private val repo by lazy { DataStoreSettingsRepository(store) }

    @After fun tearDown() = scope.cancel()

    @Test
    fun `FR-S1 empty store emits defaults`() = runBlocking {
        assertEquals(Settings(), repo.settings.first())
    }

    @Test
    fun `FR-S4 update always stores sanitized settings`() = runBlocking {
        repo.update { it.copy(enabled = true, paddingPx = 1000) }
        val stored = repo.settings.first()
        assertEquals(true, stored.enabled)
        assertEquals(60, stored.paddingPx)
        assertEquals(stored, stored.sanitized())
    }

    @Test
    fun `update round-trips LaunchApp bindings`() = runBlocking {
        repo.update { it.copy(bindings = it.bindings + (Gesture.TAP to Action.LaunchApp("com.example"))) }
        assertEquals(Action.LaunchApp("com.example"), repo.settings.first().bindings[Gesture.TAP])
    }

    @Test
    fun `ST-5 corrupt json falls back to defaults`() = runBlocking {
        store.edit { it[DataStoreSettingsRepository.KEY] = "{not json" }
        assertEquals(Settings(), repo.settings.first())
    }

    @Test
    fun `ST-43 dp values of 1_9_5 are read in px and written with the new keys`() = runBlocking {
        store.edit { it[DataStoreSettingsRepository.KEY] = """{"paddingDp":8,"manualRegion":{"centerXFraction":0.5,"widthDp":30,"heightDp":12,"topDp":4}}""" }
        val legacyRepo = DataStoreSettingsRepository(store) { LegacyDisplay(2.75f, 1080) }
        val read = legacyRepo.settings.first()
        assertEquals(22, read.paddingPx)
        assertEquals(NotchRegionPx(540, 83, 33, 11), read.notchRegion)
        legacyRepo.update { it.copy(enabled = true) }
        // once written, a different display no longer changes anything
        val later = DataStoreSettingsRepository(store) { LegacyDisplay(4f, 1440) }.settings.first()
        assertEquals(read.copy(enabled = true), later)
    }
}
