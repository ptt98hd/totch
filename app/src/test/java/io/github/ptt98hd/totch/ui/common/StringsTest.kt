package io.github.ptt98hd.totch.ui.common

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test
import org.w3c.dom.Element
import kotlin.test.assertEquals

/** Labels fixed by the spec, read from the resource files (unit tests run in the module directory). */
class StringsTest {
    private fun strings(dir: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$dir/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }.associate { it.getAttribute("name") to it.textContent }
    }

    private val en = strings("values")
    private val vi = strings("values-vi")

    @Test fun `UI-72 None reads Nothing`() {
        assertEquals("Nothing", en["action_none"])
        assertEquals("Không làm gì", vi["action_none"])
    }

    @Test fun `FR-U56 Do Not Disturb permission row`() {
        assertEquals("Do Not Disturb", en["permission_dnd"])
        assertEquals("Không làm phiền", vi["permission_dnd"])
    }

    @Test fun `UI-71 action type labels and search placeholder`() {
        assertEquals("Applications", en["tab_apps"])
        assertEquals("Quick settings", en["tab_tiles"])
        assertEquals("Cài đặt nhanh", vi["tab_tiles"])
        assertEquals("Search %1\$s", en["picker_search"])
        assertEquals("Tìm %1\$s", vi["picker_search"])
    }

    @Test fun `UI-51 keep when unused row`() {
        assertEquals("Keep when unused", en["permission_keep_unused"])
        assertEquals("Giữ khi ít dùng", vi["permission_keep_unused"])
    }
}
