package io.github.ptt98hd.totch.ui.picker

import io.github.ptt98hd.totch.ContactEntry
import org.junit.Test
import kotlin.test.assertEquals

class ContactFilterTest {
    private val entries = listOf(
        ContactEntry("u/3", "bình"),
        ContactEntry("u/2", "Ân"),
        ContactEntry("u/1", "Ân"),
        ContactEntry("u/4", "Anh Đức"),
    )

    @Test fun `CT-1 sorted by name then lookup URI`() {
        assertEquals(listOf("u/1", "u/2", "u/4", "u/3"), filterContacts(entries, "").map { it.lookupUri })
    }

    @Test fun `CT-1 query ignores case and Vietnamese marks`() {
        assertEquals(listOf("u/1", "u/2", "u/4"), filterContacts(entries, " AN").map { it.lookupUri })
        assertEquals(listOf("u/4"), filterContacts(entries, "duc").map { it.lookupUri })
        assertEquals(emptyList(), filterContacts(entries, "zzz"))
    }
}
