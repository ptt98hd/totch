package io.github.ptt98hd.totch.ui.common

import io.github.ptt98hd.totch.core.model.Action
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActionCatalogTest {
    @Test
    fun `UI-23 catalog groups cover every fixed action except None exactly once`() {
        val all = ActionCatalog.groups.flatMap { it.actions }
        assertEquals(all.size, all.distinct().size)
        assertEquals(15, all.size)
        assertFalse(Action.None in all)
        assertTrue(Action.Assistant in ActionCatalog.groups.single { it.id == ActionGroupId.SYSTEM }.actions)
        val media = ActionCatalog.groups.single { it.id == ActionGroupId.MEDIA }.actions
        assertEquals(Action.VolumePanel, media[media.indexOf(Action.VolumeDown) + 1])
    }
}
