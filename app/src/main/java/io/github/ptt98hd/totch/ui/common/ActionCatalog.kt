package io.github.ptt98hd.totch.ui.common

import io.github.ptt98hd.totch.core.model.Action

enum class ActionGroupId { SYSTEM, MEDIA }

data class ActionGroup(val id: ActionGroupId, val actions: List<Action>)

/** Fixed actions of the picker's "System" tab, grouped; "None" is not offered (UI-23). */
object ActionCatalog {
    val groups: List<ActionGroup> = listOf(
        ActionGroup(
            ActionGroupId.SYSTEM,
            listOf(
                Action.Back, Action.Home, Action.Recents, Action.LockScreen,
                Action.Screenshot, Action.Notifications, Action.QuickSettings, Action.Assistant, Action.ToggleTorch,
            ),
        ),
        ActionGroup(
            ActionGroupId.MEDIA,
            listOf(
                Action.MediaPlayPause, Action.MediaPrevious, Action.MediaNext,
                Action.VolumeUp, Action.VolumeDown, Action.VolumePanel,
            ),
        ),
    )
}
