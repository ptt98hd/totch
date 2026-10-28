package io.github.ptt98hd.totch.ui.picker

import io.github.ptt98hd.totch.ShortcutEntry
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShortcutGroupingTest {
    private fun e(pkg: String, app: String, label: String) = ShortcutEntry(pkg, app, "id-$label", label, "intent:#Intent;end")

    private val entries = listOf(
        e("org.zed", "zebra", "Stripes"),
        e("com.chrome", "Chrome", "New tab"),
        e("com.chrome", "Chrome", "incognito tab"),
        e("com.cam", "camera", "Selfie"),
        e("com.cam", "camera", "Video"),
        e("com.other", "Chrome", "Beta"),
    )

    @Test fun `SH-1 grouped by package, groups by app label then package, shortcuts by label`() {
        val groups = groupShortcuts(entries, "")
        assertEquals(listOf("com.cam", "com.chrome", "com.other", "org.zed"), groups.map { it.packageName })
        assertEquals(listOf("incognito tab", "New tab"), groups[1].shortcuts.map { it.label })
        assertEquals("camera", groups[0].appLabel)
    }

    @Test fun `SH-2 query matches shortcut label or app label, case-insensitive`() {
        assertEquals(
            listOf("com.chrome" to listOf("incognito tab", "New tab")),
            groupShortcuts(entries, "  TAB ").map { it.packageName to it.shortcuts.map { s -> s.label } },
        )
        val byApp = groupShortcuts(entries, "CAMERA")
        assertEquals(listOf("Selfie", "Video"), byApp.single().shortcuts.map { it.label })
        assertTrue(groupShortcuts(entries, "xyz").isEmpty())
    }
}
