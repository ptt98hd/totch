package io.github.ow3n.totch.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import io.github.ow3n.totch.core.model.Action
import io.github.ow3n.totch.core.model.Gesture
import io.github.ow3n.totch.core.settings.Settings
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
        repo.update { it.copy(enabled = true, doubleTapMs = 10, paddingDp = 1000) }
        val stored = repo.settings.first()
        assertEquals(true, stored.enabled)
        assertEquals(150, stored.doubleTapMs)
        assertEquals(48, stored.paddingDp)
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
}
