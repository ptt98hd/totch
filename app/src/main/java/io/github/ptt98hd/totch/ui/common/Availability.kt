package io.github.ptt98hd.totch.ui.common

import androidx.annotation.StringRes
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.Requirements
import io.github.ptt98hd.totch.core.action.ActionRequirements
import io.github.ptt98hd.totch.core.action.Capability
import io.github.ptt98hd.totch.core.model.Action

/** What a gesture or action still lacks to run (FR-U44). */
enum class Need { ACCESS, ACCESSIBILITY, SHIZUKU, DND }

@get:StringRes
val Need.labelRes: Int
    get() = when (this) {
        Need.ACCESS -> R.string.needs_access
        Need.ACCESSIBILITY -> R.string.gesture_unavailable
        Need.SHIZUKU -> R.string.action_needs_shizuku
        Need.DND -> R.string.action_needs_dnd
    }

/** Backends and permissions that decide whether gestures and actions can run (FR-U44, ui §3 "Khả dụng"). */
data class Capabilities(
    val accessibility: Boolean = true,
    /** Shizuku is running and granted. */
    val shizuku: Boolean = true,
    val dndAccess: Boolean = true,
) {
    /** Touches can be read at all, through the accessibility overlay or Shizuku (FR-D13). */
    val touchSource: Boolean get() = accessibility || shizuku

    /** UI-62: null when [action] can run. */
    fun need(action: Action): Need? {
        val required = ActionRequirements.of(action)
        return when {
            Capability.LOCAL in required -> Need.DND.takeIf { !dndAccess && action in ActionCatalog.needsDndAccess }
            required.isEmpty() -> null
            Capability.ACCESSIBILITY in required && accessibility -> null
            Capability.SHIZUKU in required && shizuku -> null
            required == setOf(Capability.SHIZUKU) -> Need.SHIZUKU
            else -> Need.ACCESS
        }
    }
}

fun Requirements.capabilities() = Capabilities(
    accessibility = accessibilityEnabled,
    shizuku = shizukuRunning && shizukuGranted,
    dndAccess = dndAccessGranted,
)
