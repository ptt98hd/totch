package io.github.ptt98hd.totch.ui.common

import io.github.ptt98hd.totch.core.action.SettingsGroup
import io.github.ptt98hd.totch.core.action.SettingsPages
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
        assertEquals(29, all.size)
        assertFalse(Action.None in all)
        assertEquals(
            listOf(
                ActionGroupId.NAVIGATION, ActionGroupId.SYSTEM, ActionGroupId.MEDIA,
                ActionGroupId.SOUND, ActionGroupId.DISPLAY, ActionGroupId.CONNECTIVITY,
            ),
            ActionCatalog.groups.map { it.id },
        )
        fun group(id: ActionGroupId) = ActionCatalog.groups.single { it.id == id }.actions
        assertEquals(Action.OneHandedMode, group(ActionGroupId.NAVIGATION).last())
        assertEquals(listOf(Action.VolumeUp, Action.VolumeDown, Action.VolumePanel), group(ActionGroupId.SOUND).take(3))
        assertEquals(ActionCatalog.needsDndAccess, group(ActionGroupId.SOUND).drop(3).toSet())
        assertEquals(
            listOf(Action.ToggleWifi, Action.ToggleBluetooth, Action.ToggleMobileData, Action.ToggleAirplaneMode, Action.ToggleLocation),
            group(ActionGroupId.CONNECTIVITY),
        )
    }

    @Test
    fun `UI-77 no vibrate, sound group and Do Not Disturb set`() {
        assertEquals(
            listOf(
                Action.VolumeUp, Action.VolumeDown, Action.VolumePanel,
                Action.RingerNormal, Action.RingerSilent, Action.CycleRingerMode, Action.ToggleDnd,
            ),
            ActionCatalog.groups.single { it.id == ActionGroupId.SOUND }.actions,
        )
        assertEquals(
            setOf(Action.RingerNormal, Action.RingerSilent, Action.CycleRingerMode, Action.ToggleDnd),
            ActionCatalog.needsDndAccess,
        )
    }

    @Test
    fun `SP-3 every settings page has a label`() {
        SettingsPages.all.forEach { assertTrue(it.intentAction in settingsPageLabels, it.intentAction) }
        SettingsGroup.entries.forEach { assertTrue(it in settingsGroupLabels, "$it") }
    }

    private val labels = mapOf(
        Action.VolumeUp to "Tăng âm lượng",
        Action.VolumeDown to "Giảm âm lượng",
        Action.ToggleWifi to "Bật/tắt Wi-Fi",
        Action.ToggleDnd to "Bật/tắt Không làm phiền",
        Action.Back to "Quay lại",
    )
    private val groups = listOf(
        ActionGroup(ActionGroupId.NAVIGATION, listOf(Action.Back)),
        ActionGroup(ActionGroupId.SOUND, listOf(Action.VolumeUp, Action.VolumeDown, Action.ToggleDnd)),
        ActionGroup(ActionGroupId.CONNECTIVITY, listOf(Action.ToggleWifi)),
    )
    private val groupLabels = mapOf(
        ActionGroupId.NAVIGATION to "Điều hướng",
        ActionGroupId.SOUND to "Âm thanh",
        ActionGroupId.CONNECTIVITY to "Kết nối",
    )

    private fun filter(query: String) = filterCatalog(groups, query, { labels.getValue(it) }, { groupLabels.getValue(it) })

    @Test
    fun `UI-24 search ignores case and Vietnamese diacritics and drops empty groups`() {
        assertEquals(groups, filter("  "))
        assertEquals(
            listOf(ActionGroup(ActionGroupId.SOUND, listOf(Action.VolumeUp, Action.VolumeDown))),
            filter("am luong"),
        )
        assertEquals(listOf(ActionGroup(ActionGroupId.CONNECTIVITY, listOf(Action.ToggleWifi))), filter("WI-FI"))
        assertEquals(listOf(ActionGroup(ActionGroupId.SOUND, listOf(Action.ToggleDnd))), filter("khong lam phien"))
        assertEquals(emptyList(), filter("bluetooth"))
    }

    @Test
    fun `UI-24 a query matching the group name keeps the whole group`() {
        assertEquals(listOf(groups[1]), filter("âm THANH"))
        assertEquals(listOf(groups[0]), filter("dieu huong"))
    }
}
