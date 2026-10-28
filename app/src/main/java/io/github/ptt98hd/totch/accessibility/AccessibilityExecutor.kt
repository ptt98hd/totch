package io.github.ptt98hd.totch.accessibility

import android.accessibilityservice.AccessibilityService
import io.github.ptt98hd.totch.core.action.ActionExecutor
import io.github.ptt98hd.totch.core.action.Capability
import io.github.ptt98hd.totch.core.model.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** System actions via `performGlobalAction`; apps, shortcuts and the assistant are started from the service (actions spec §4). */
class AccessibilityExecutor(private val bridge: AccessibilityBridge) : ActionExecutor {
    override val capability = Capability.ACCESSIBILITY

    override fun isAvailable(): Boolean = bridge.service.value != null

    override fun supports(action: Action): Boolean =
        action is Action.LaunchApp || action is Action.LaunchShortcut || action == Action.Assistant || globalAction(action) != null

    override suspend fun execute(action: Action): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            val service = bridge.service.value ?: error("unavailable")
            when (action) {
                is Action.LaunchApp -> service.launchApp(action.packageName)
                is Action.LaunchShortcut -> service.launchShortcut(action.intentUri)
                Action.Assistant -> service.openAssistant()
                else -> {
                    val global = globalAction(action) ?: error("unsupported")
                    check(service.performGlobalAction(global)) { "rejected" }
                }
            }
        }
    }

    private fun globalAction(action: Action): Int? = when (action) {
        Action.Back -> AccessibilityService.GLOBAL_ACTION_BACK
        Action.Home -> AccessibilityService.GLOBAL_ACTION_HOME
        Action.Recents -> AccessibilityService.GLOBAL_ACTION_RECENTS
        Action.LockScreen -> AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
        Action.Screenshot -> AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
        Action.Notifications -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
        Action.QuickSettings -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
        else -> null
    }
}
