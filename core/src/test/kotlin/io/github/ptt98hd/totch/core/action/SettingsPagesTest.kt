package io.github.ptt98hd.totch.core.action

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsPagesTest {
    @Test fun `SP-1 pages are unique and grouped in group order`() {
        val all = SettingsPages.all
        assertEquals(all.size, all.map { it.intentAction }.toSet().size)
        assertEquals(SettingsGroup.entries, all.map { it.group }.distinct())
        assertEquals(all.map { it.group }, all.map { it.group }.sortedBy { it.ordinal })
    }

    @Test fun `SP-2 every action is a safe intent action`() {
        SettingsPages.all.forEach { assertTrue(IntentActionRegex.matches(it.intentAction), it.intentAction) }
    }
}
