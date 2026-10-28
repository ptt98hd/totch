package io.github.ptt98hd.totch.shizuku

import android.content.pm.PackageManager
import io.github.ptt98hd.totch.engine.ShizukuState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/** Tracks Shizuku availability and permission via its listeners (no polling). */
class ShizukuManager {
    private val _state = MutableStateFlow(ShizukuState())
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val onBinderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val onBinderDead = Shizuku.OnBinderDeadListener { _state.value = ShizukuState() }
    private val onPermissionResult = Shizuku.OnRequestPermissionResultListener { code, _ ->
        if (code == REQUEST_CODE) refresh()
    }

    init {
        Shizuku.addBinderDeadListener(onBinderDead)
        Shizuku.addRequestPermissionResultListener(onPermissionResult)
        Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
    }

    fun refresh() {
        val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val granted = running && runCatching {
            !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        _state.value = ShizukuState(running, granted)
    }

    /** Shows Shizuku's permission dialog; no-op if Shizuku is not running, already granted or unsupported. */
    fun requestPermission() {
        refresh()
        val s = _state.value
        if (!s.running || s.granted) return
        runCatching { if (!Shizuku.isPreV11()) Shizuku.requestPermission(REQUEST_CODE) }
    }

    private companion object {
        const val REQUEST_CODE = 0x7070
    }
}
