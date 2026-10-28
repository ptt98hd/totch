package io.github.ptt98hd.totch.ui.common

import io.github.ptt98hd.totch.core.action.ActionRequirements
import io.github.ptt98hd.totch.core.action.Capability
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.settings.DetectionMode

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

    /** UI-24: groups whose actions' [label] contains [query] (trimmed, case-insensitive); empty groups are dropped. */
    fun search(query: String, label: (Action) -> String): List<ActionGroup> {
        val q = query.trim()
        if (q.isEmpty()) return groups
        return groups
            .map { group -> group.copy(actions = group.actions.filter { label(it).contains(q, ignoreCase = true) }) }
            .filter { it.actions.isNotEmpty() }
    }

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
