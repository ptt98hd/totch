package io.github.ptt98hd.totch.update

import io.github.ptt98hd.totch.AppUpdater
import io.github.ptt98hd.totch.core.update.Release
import io.github.ptt98hd.totch.core.update.Releases
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Self-update progress shown on the Check for updates row (update spec §5). */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    /** Handed over to the system installer. */
    data object Installing : UpdateState
    data object Failed : UpdateState
}

data class UpdateUi(val state: UpdateState = UpdateState.Idle, val dialog: Boolean = false)

/**
 * Process-wide update state (update spec §5), so the new-version dialog can open on any screen: from the Settings
 * row, when the app opens (FR-U29) or from the update notification.
 */
class UpdateController(
    private val updater: AppUpdater,
    private val currentVersion: String,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(UpdateUi())
    val state: StateFlow<UpdateUi> = _state.asStateFlow()

    /** UP-6; [silent] (UP-9) leaves the row untouched and only reports a newer release. */
    fun check(silent: Boolean = false) {
        when (_state.value.state) {
            UpdateState.Checking, is UpdateState.Downloading -> return
            is UpdateState.Available -> _state.update { it.copy(dialog = true) }
            UpdateState.Installing -> if (silent) return else start(silent)
            else -> start(silent)
        }
    }

    private fun start(silent: Boolean) {
        val before = _state.value
        if (!silent) _state.value = UpdateUi(UpdateState.Checking)
        scope.launch {
            val result = try {
                val release = updater.latestRelease()
                when {
                    release != null && Releases.isNewer(release, currentVersion) -> UpdateUi(UpdateState.Available(release), dialog = true)
                    silent -> before
                    else -> UpdateUi(UpdateState.UpToDate)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (silent) before else UpdateUi(UpdateState.Failed)
            }
            // A manual check may have started meanwhile; a silent result only lands on an unchanged state.
            if (!silent || _state.value == before) _state.value = result
        }
    }

    fun dismissDialog() = _state.update { it.copy(dialog = false) }

    /** UP-7 */
    fun startUpdate() {
        val release = (_state.value.state as? UpdateState.Available)?.release ?: return
        _state.value = UpdateUi(UpdateState.Downloading(0f))
        scope.launch {
            val ok = try {
                updater.downloadAndInstall(release) { p -> _state.value = UpdateUi(UpdateState.Downloading(p.coerceIn(0f, 1f))) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            _state.value = UpdateUi(if (ok) UpdateState.Installing else UpdateState.Failed)
        }
    }
}
