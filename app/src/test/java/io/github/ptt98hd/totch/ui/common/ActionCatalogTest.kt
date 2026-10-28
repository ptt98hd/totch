package io.github.ptt98hd.totch.ui.common

import io.github.ptt98hd.totch.core.action.Capability
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.settings.DetectionMode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActionCatalogTest {
    @Test
    fun `local and none actions are available in every mode`() {
        DetectionMode.entries.forEach { mode ->
            assertTrue(ActionCatalog.isAvailable(Action.None, mode))
            assertTrue(ActionCatalog.isAvailable(Action.ToggleTorch, mode))
            assertTrue(ActionCatalog.isAvailable(Action.MediaNext, mode))
        }
    }

    @Test
    fun `system actions are available in both detection modes per the capability matrix`() {
        DetectionMode.entries.forEach { mode ->
            assertTrue(ActionCatalog.isAvailable(Action.Back, mode))
            assertTrue(ActionCatalog.isAvailable(Action.LaunchApp("com.x"), mode))
        }
    }

    @Test
    fun `availability follows the capability of the mode`() {
        assertTrue(ActionCatalog.isAvailable(setOf(Capability.SHIZUKU), DetectionMode.SHIZUKU))
        assertFalse(ActionCatalog.isAvailable(setOf(Capability.SHIZUKU), DetectionMode.ACCESSIBILITY))
        assertFalse(ActionCatalog.isAvailable(setOf(Capability.ACCESSIBILITY), DetectionMode.SHIZUKU))
        assertTrue(ActionCatalog.isAvailable(setOf(Capability.LOCAL), DetectionMode.SHIZUKU))
    }

    @Test
    fun `catalog groups cover every fixed action exactly once`() {
        val all = ActionCatalog.groups.flatMap { it.actions }
        assertEquals(all.size, all.distinct().size)
        assertEquals(14, all.size)
    }
}
