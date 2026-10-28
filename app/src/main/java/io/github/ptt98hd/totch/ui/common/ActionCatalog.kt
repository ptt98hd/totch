package io.github.ptt98hd.totch.ui.common

import io.github.ptt98hd.totch.core.action.ActionRequirements
import io.github.ptt98hd.totch.core.action.Capability
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.settings.DetectionMode

enum class ActionGroupId { SYSTEM, MEDIA, APPS }

data class ActionGroup(val id: ActionGroupId, val actions: List<Action>)

/** Actions offered in the gesture bottom sheet, grouped (docs/specs/ui.md, Gestures). */
object ActionCatalog {
    val groups: List<ActionGroup> = listOf(
        ActionGroup(
            ActionGroupId.SYSTEM,
            listOf(
                Action.None, Action.Back, Action.Home, Action.Recents, Action.LockScreen,
                Action.Screenshot, Action.Notifications, Action.QuickSettings, Action.ToggleTorch,
            ),
        ),
        ActionGroup(
            ActionGroupId.MEDIA,
            listOf(Action.MediaPlayPause, Action.MediaPrevious, Action.MediaNext, Action.VolumeUp, Action.VolumeDown),
        ),
        // The APPS group holds the "Open app…" entry, which navigates to the app picker instead.
        ActionGroup(ActionGroupId.APPS, emptyList()),
    )

    /** Whether [action] can run in [mode] according to the capability matrix (docs/specs/actions.md §1). */
    fun isAvailable(action: Action, mode: DetectionMode): Boolean = isAvailable(ActionRequirements.of(action), mode)

    fun isAvailable(requirements: Set<Capability>, mode: DetectionMode): Boolean {
        if (requirements.isEmpty() || Capability.LOCAL in requirements) return true
        return when (mode) {
            DetectionMode.ACCESSIBILITY -> Capability.ACCESSIBILITY in requirements
            DetectionMode.SHIZUKU -> Capability.SHIZUKU in requirements
        }
    }
}
