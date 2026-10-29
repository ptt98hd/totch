package io.github.ptt98hd.totch.ui.common

import io.github.ptt98hd.totch.core.model.Action
import java.text.Normalizer

enum class ActionGroupId { NAVIGATION, SYSTEM, MEDIA, SOUND, DISPLAY, CONNECTIVITY }

data class ActionGroup(val id: ActionGroupId, val actions: List<Action>)

/** Fixed actions of the picker's "System" tab, grouped; "None" is not offered (UI-23). */
object ActionCatalog {
    val groups: List<ActionGroup> = listOf(
        ActionGroup(
            ActionGroupId.NAVIGATION,
            listOf(Action.Back, Action.Home, Action.Recents, Action.Notifications, Action.QuickSettings, Action.OneHandedMode),
        ),
        ActionGroup(ActionGroupId.SYSTEM, listOf(Action.LockScreen, Action.Screenshot, Action.Assistant, Action.ToggleTorch)),
        ActionGroup(ActionGroupId.MEDIA, listOf(Action.MediaPlayPause, Action.MediaPrevious, Action.MediaNext)),
        ActionGroup(
            ActionGroupId.SOUND,
            listOf(
                Action.VolumeUp, Action.VolumeDown, Action.VolumePanel,
                Action.RingerNormal, Action.RingerVibrate, Action.RingerSilent, Action.CycleRingerMode, Action.ToggleDnd,
            ),
        ),
        ActionGroup(
            ActionGroupId.DISPLAY,
            listOf(Action.BrightnessUp, Action.BrightnessDown, Action.ToggleAutoBrightness, Action.ToggleAutoRotate),
        ),
        ActionGroup(
            ActionGroupId.CONNECTIVITY,
            listOf(
                Action.ToggleWifi, Action.ToggleBluetooth, Action.ToggleMobileData, Action.ToggleAirplaneMode,
                Action.ToggleLocation,
            ),
        ),
    )

    /** Sound mode actions, which need Do Not Disturb access (FR-A18, UI-29). */
    val needsDndAccess: Set<Action> =
        setOf(Action.RingerNormal, Action.RingerVibrate, Action.RingerSilent, Action.CycleRingerMode, Action.ToggleDnd)
}

private val marks = Regex("\\p{Mn}+")

/** Lower case without Vietnamese diacritics (`đ` → `d`), so "am luong" finds "Âm lượng". */
internal fun searchKey(text: String): String =
    marks.replace(Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD), "").replace('đ', 'd')

/** UI-24: a query matching the group name keeps the whole group; otherwise only matching actions; empty groups go. */
fun filterCatalog(
    groups: List<ActionGroup>,
    query: String,
    actionLabel: (Action) -> String,
    groupLabel: (ActionGroupId) -> String,
): List<ActionGroup> {
    val q = searchKey(query)
    if (q.isEmpty()) return groups
    return groups.mapNotNull { group ->
        val actions = if (q in searchKey(groupLabel(group.id))) {
            group.actions
        } else {
            group.actions.filter { q in searchKey(actionLabel(it)) }
        }
        actions.takeIf { it.isNotEmpty() }?.let { group.copy(actions = it) }
    }
}
