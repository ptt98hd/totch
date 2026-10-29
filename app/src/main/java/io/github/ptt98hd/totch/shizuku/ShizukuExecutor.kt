package io.github.ptt98hd.totch.shizuku

import android.content.Intent
import android.content.pm.PackageManager
import io.github.ptt98hd.totch.core.action.ActionExecutor
import io.github.ptt98hd.totch.core.action.Capability
import io.github.ptt98hd.totch.core.action.ShellCommands
import io.github.ptt98hd.totch.core.model.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Runs system actions as uid shell through the bound [TouchUserService] (actions spec §4). */
class ShizukuExecutor(
    private val service: StateFlow<ITouchService?>,
    private val packageManager: PackageManager,
) : ActionExecutor {
    override val capability = Capability.SHIZUKU

    /** Bound UserService implies Shizuku binder alive and permission granted. */
    override fun isAvailable(): Boolean = service.value?.asBinder()?.isBinderAlive == true

    override fun supports(action: Action): Boolean =
        action is Action.LaunchApp || action is Action.LaunchShortcut || action is Action.LaunchActivity ||
            ShellCommands.of(action) != null

    override suspend fun execute(action: Action): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val command = when (action) {
                is Action.LaunchApp -> launchCommand(action.packageName)
                is Action.LaunchShortcut -> ShellCommands.startIntent(action.intentUri)
                is Action.LaunchActivity -> ShellCommands.startActivity(action.packageName, action.activityName)
                else -> ShellCommands.of(action)
            } ?: error("unsupported")
            val service = service.value ?: error("unavailable")
            val exit = service.exec(command)
            check(exit == 0) { "exit $exit" }
        }
    }

    private fun launchCommand(packageName: String): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)
        val activity = packageManager.queryIntentActivities(intent, 0).firstOrNull()?.activityInfo?.name
            ?: error("not installed")
        return ShellCommands.launch(packageName, activity)
    }
}
