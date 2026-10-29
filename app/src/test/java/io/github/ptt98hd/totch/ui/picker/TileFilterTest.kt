package io.github.ptt98hd.totch.ui.picker

import io.github.ptt98hd.totch.TileEntry
import org.junit.Test
import kotlin.test.assertEquals

class TileFilterTest {
    private val entries = listOf(
        TileEntry("org.zed", "zebra", "org.zed.Tile", "zebra"),
        TileEntry("com.notes", "Notes", "com.notes.QuickNote", "quick note"),
        TileEntry("com.b", "Alarm", "com.b.Timer", "Quick note"),
        TileEntry("com.c", "Caffeine", "com.c.Tile", "Awake"),
    )

    @Test fun `QT-1 sorted by tile label then app label`() {
        assertEquals(
            listOf("com.c.Tile", "com.b.Timer", "com.notes.QuickNote", "org.zed.Tile"),
            filterTiles(entries, "").map { it.name },
        )
    }

    @Test fun `QT-1 query matches tile label or app label`() {
        assertEquals(listOf("com.b.Timer", "com.notes.QuickNote"), filterTiles(entries, " QUICK ").map { it.name })
        assertEquals(listOf("com.c.Tile"), filterTiles(entries, "caff").map { it.name })
        assertEquals(emptyList(), filterTiles(entries, "zzz"))
    }
}
