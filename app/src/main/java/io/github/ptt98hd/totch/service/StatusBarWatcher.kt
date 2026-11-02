package io.github.ptt98hd.totch.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Whether the status bar is shown (OV-4), read by [read] while the island shows something: no window above the status
 * bar hears when a full-screen app hides it, so it is polled (notch-overlay §10.2, SB-1…SB-4). Thread-safe.
 */
class StatusBarWatcher(private val scope: CoroutineScope, private val read: () -> Boolean) {
    private val _visible = MutableStateFlow(true)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()
    private var poll: Job? = null

    /** Polls every [POLL_MS] while true. */
    var active: Boolean = false
        set(value) = synchronized(this) {
            if (field == value) return
            field = value
            poll?.cancel()
            poll = if (value) {
                scope.launch {
                    while (isActive) {
                        _visible.value = read()
                        delay(POLL_MS)
                    }
                }
            } else {
                null
            }
        }

    /** Something changed on screen: read now. */
    fun refresh() {
        scope.launch { _visible.value = read() }
    }

    companion object {
        const val POLL_MS = 500L
    }
}
