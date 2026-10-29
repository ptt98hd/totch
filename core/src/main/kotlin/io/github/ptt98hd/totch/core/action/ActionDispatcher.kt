package io.github.ptt98hd.totch.core.action

import io.github.ptt98hd.totch.core.model.Action
import kotlin.coroutines.cancellation.CancellationException

enum class Capability { SHIZUKU, ACCESSIBILITY, LOCAL }

interface ActionExecutor {
    val capability: Capability
    fun isAvailable(): Boolean
    fun supports(action: Action): Boolean
    suspend fun execute(action: Action): Result<Unit>
}

/** Which backends are able to perform an action (docs/specs/actions.md §1). */
object ActionRequirements {
    private val system = setOf(Capability.ACCESSIBILITY, Capability.SHIZUKU)
    private val local = setOf(Capability.LOCAL)
    private val shizuku = setOf(Capability.SHIZUKU)

    fun of(action: Action): Set<Capability> = when (action) {
        Action.None -> emptySet()
        Action.Back, Action.Home, Action.Recents, Action.LockScreen, Action.Screenshot,
        Action.Notifications, Action.QuickSettings, is Action.LaunchApp, Action.Assistant,
        is Action.LaunchShortcut, is Action.LaunchActivity -> system
        Action.ToggleTorch, Action.MediaPlayPause, Action.MediaNext, Action.MediaPrevious,
        Action.VolumeUp, Action.VolumeDown, Action.VolumePanel -> local
        is Action.ClickTile, Action.OneHandedMode -> shizuku
    }
}

sealed interface DispatchResult {
    data object Ok : DispatchResult
    data object Skipped : DispatchResult
    data class Failed(val reason: String) : DispatchResult
}

/** Runs an action on the single best executor: LOCAL > ACCESSIBILITY > SHIZUKU. No fallback on failure. */
class ActionDispatcher(private val executors: List<ActionExecutor>) {
    private val priority = listOf(Capability.LOCAL, Capability.ACCESSIBILITY, Capability.SHIZUKU)

    suspend fun dispatch(action: Action): DispatchResult {
        if (action == Action.None) return DispatchResult.Skipped
        val required = ActionRequirements.of(action)
        val executor = executors
            .filter { it.capability in required && it.isAvailable() && it.supports(action) }
            .minByOrNull { priority.indexOf(it.capability) }
            ?: return DispatchResult.Failed("unavailable")
        val result = try {
            executor.execute(action)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        return result.fold(
            onSuccess = { DispatchResult.Ok },
            onFailure = { DispatchResult.Failed(it.message ?: "error") },
        )
    }
}
