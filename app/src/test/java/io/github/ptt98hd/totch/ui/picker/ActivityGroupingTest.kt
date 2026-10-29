package io.github.ptt98hd.totch.ui.picker

import io.github.ptt98hd.totch.ActivityEntry
import org.junit.Test
import kotlin.test.assertEquals

class ActivityGroupingTest {
    private fun e(pkg: String, app: String, name: String, label: String) = ActivityEntry(pkg, app, name, label)

    private val entries = listOf(
        e("org.zed", "zebra", "org.zed.Main", "zebra"),
        e("com.chrome", "Chrome", "com.chrome.Settings", "Settings"),
        e("com.chrome", "Chrome", "com.chrome.Bookmarks", "Chrome"),
        e("com.chrome", "Chrome", "com.chrome.About", "Chrome"),
        e("com.cam", "camera", "com.cam.Video", "Video"),
    )

    @Test fun `AC-1 grouped by package, groups by app label, activities by label then class`() {
        val groups = groupActivities(entries, "")
        assertEquals(listOf("com.cam", "com.chrome", "org.zed"), groups.map { it.packageName })
        assertEquals(
            listOf("com.chrome.About", "com.chrome.Bookmarks", "com.chrome.Settings"),
            groups[1].activities.map { it.name },
        )
    }

    @Test fun `AC-2 query matches label, class name or app label`() {
        assertEquals(listOf("com.chrome.Bookmarks"), groupActivities(entries, " bookMARK ").flatMap { g -> g.activities.map { it.name } })
        assertEquals(listOf("com.chrome.Settings"), groupActivities(entries, "settings").flatMap { g -> g.activities.map { it.name } })
        assertEquals(listOf("com.cam" to 1), groupActivities(entries, "CAMERA").map { it.packageName to it.activities.size })
        assertEquals(emptyList(), groupActivities(entries, "nothing"))
    }

    @Test fun `AC-3 short class name drops the package prefix`() {
        assertEquals(".ui.Main", shortActivityName("com.a", "com.a.ui.Main"))
        assertEquals("org.lib.Main", shortActivityName("com.a", "org.lib.Main"))
    }
}
