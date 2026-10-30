package io.github.ptt98hd.totch.shizuku

import android.content.Intent
import android.content.pm.PackageManager
import io.github.ptt98hd.totch.core.action.ActionExecutor
import io.github.ptt98hd.totch.core.action.Capability
import io.github.ptt98hd.totch.core.action.OneHandedCommands
import io.github.ptt98hd.totch.core.action.OneHandedMethod
import io.github.ptt98hd.totch.core.action.ShellCommands
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.IntentTarget
import io.github.ptt98hd.totch.engine.DisplaySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Runs system actions as uid shell through the bound [TouchUserService] (actions spec §4). */
class ShizukuExecutor(
    private val service: StateFlow<ITouchService?>,
    private val packageManager: PackageManager,
    private val display: () -> DisplaySnapshot,
    oneHandedMethod: () -> OneHandedMethod,
    rememberOneHandedMethod: suspend (OneHandedMethod) -> Unit,
) : ActionExecutor {
    override val capability = Capability.SHIZUKU

    /** Actions spec §10: native toggle or launcher gesture, whichever works, remembered (OH-5). */
    private val oneHanded = OneHandedRunner(oneHandedMethod, rememberOneHandedMethod) { method ->
        val d = display()
        exec(
            when (method) {
                OneHandedMethod.SYSTEM -> OneHandedCommands.system()
                OneHandedMethod.GESTURE -> OneHandedCommands.gesture(d.widthPx, d.heightPx, d.density)
            },
        ) == 0
    }

    /** Bound UserService implies Shizuku binder alive and permission granted. */
    override fun isAvailable(): Boolean = service.value?.asBinder()?.isBinderAlive == true

    override fun supports(action: Action): Boolean =
        action is Action.LaunchApp || action is Action.LaunchShortcut || action is Action.LaunchActivity ||
            action is Action.ClickTile || action == Action.OneHandedMode || action is Action.OpenSettings ||
            action is Action.OpenContact || (action is Action.SendIntent && action.target == IntentTarget.ACTIVITY) ||
            ShellCommands.of(action) != null

    override suspend fun execute(action: Action): Result<Unit> = withContext(Dispatchers.IO) {
        if (action == Action.OneHandedMode) return@withContext oneHanded.run()
        runCatching {
            val command = when (action) {
                is Action.LaunchApp -> launchCommand(action.packageName)
                is Action.LaunchShortcut -> ShellCommands.startIntent(action.intentUri)
                is Action.LaunchActivity -> ShellCommands.startActivity(action.packageName, action.activityName)
                is Action.ClickTile -> ShellCommands.clickTile(action.packageName, action.serviceName)
                is Action.OpenSettings -> ShellCommands.openSettings(action.intentAction)
                is Action.OpenContact -> ShellCommands.openContact(action.lookupUri)
                is Action.SendIntent -> ShellCommands.sendIntent(action.packageName, action.className, action.intentAction)
                else -> ShellCommands.of(action)
            } ?: error("unsupported")
            val exit = exec(command)
            check(exit == 0) { "exit $exit" }
        }
    }

    private fun exec(command: String): Int = (service.value ?: error("unavailable")).exec(command)

    private fun launchCommand(packageName: String): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)
        val activity = packageManager.queryIntentActivities(intent, 0).firstOrNull()?.activityInfo?.name
            ?: error("not installed")
        return ShellCommands.launch(packageName, activity)
    }
}
