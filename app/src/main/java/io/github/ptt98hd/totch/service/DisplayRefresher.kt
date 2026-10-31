package io.github.ptt98hd.totch.service

import io.github.ptt98hd.totch.engine.DisplaySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Display state read on every hint that it changed, and again a little later: a callback may come before the system
 * has updated the rotation or size (notch-overlay §7, DR-1…DR-3). Thread-safe.
 */
class DisplayRefresher(private val scope: CoroutineScope, private val read: () -> DisplaySnapshot) {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<DisplaySnapshot> = _state.asStateFlow()
    private var job: Job? = null

    /** Reads now, then again after each of [SETTLE_MS]; a new call restarts the schedule. */
    fun refresh() = synchronized(this) {
        job?.cancel()
        job = scope.launch {
            _state.value = read()
            var waited = 0L
            for (at in SETTLE_MS) {
                delay(at - waited)
                waited = at
                _state.value = read()
            }
        }
    }

    companion object {
        val SETTLE_MS = listOf(250L, 1_000L)
    }
}
