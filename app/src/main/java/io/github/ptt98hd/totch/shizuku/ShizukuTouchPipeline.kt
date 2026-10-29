package io.github.ptt98hd.totch.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import io.github.ptt98hd.totch.BuildConfig
import io.github.ptt98hd.totch.core.input.AbsRange
import io.github.ptt98hd.totch.core.input.CoordinateMapper
import io.github.ptt98hd.totch.core.input.GeteventParser
import io.github.ptt98hd.totch.core.input.InputDeviceInfo
import io.github.ptt98hd.totch.core.model.TouchEvent
import io.github.ptt98hd.totch.engine.DisplaySnapshot
import io.github.ptt98hd.totch.engine.PipelineError
import io.github.ptt98hd.totch.engine.PipelineState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

/**
 * Shizuku mode input (FR-D1, DM-2..DM-7): binds [TouchUserService] while Shizuku is ready, streams
 * `getevent` frames through [GeteventParser] into [onTouch]. Coordinates are mapped to the natural
 * (ROTATION_0) display size; the engine only runs in ROTATION_0 anyway (FR-R4).
 */
class ShizukuTouchPipeline(
    context: Context,
    private val shizuku: ShizukuManager,
    private val display: StateFlow<DisplaySnapshot>,
    private val onTouch: (TouchEvent) -> Unit,
    private val onReset: () -> Unit,
) {
    private val _state = MutableStateFlow<PipelineState>(PipelineState.Idle)
    val state: StateFlow<PipelineState> = _state.asStateFlow()

    private val _service = MutableStateFlow<ITouchService?>(null)
    /** Bound UserService (also used by [ShizukuExecutor]), `null` when not connected. */
    val service: StateFlow<ITouchService?> = _service.asStateFlow()

    private val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, TouchUserService::class.java.name))
        .daemon(false)
        .processNameSuffix("touch")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    /** Runs until cancelled; call from the main thread (Shizuku binds on it). */
    suspend fun run() {
        try {
            shizuku.state.map { it.ready }.distinctUntilChanged().collectLatest { ready ->
                if (!ready) {
                    _state.value = PipelineState.Idle // Accessibility may take over, otherwise NeedsAccess (DM-6, DM-41)
                    return@collectLatest
                }
                _state.value = PipelineState.Connecting
                userService()
                    .catch { fail(PipelineError.SERVICE_FAILED, it) }
                    .collectLatest { service ->
                        if (service == null) fail(PipelineError.SERVICE_FAILED) else stream(service)
                    }
            }
        } finally {
            _state.value = PipelineState.Idle
            _service.value = null
        }
    }

    /** Emits the service when connected and `null` when its process dies (DM-5); unbinds on cancel (DM-7). */
    private fun userService(): Flow<ITouchService?> = callbackFlow {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                trySend(binder?.takeIf { it.pingBinder() }?.let(ITouchService.Stub::asInterface))
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                trySend(null)
            }
        }
        Shizuku.bindUserService(args, connection)
        awaitClose { runCatching { Shizuku.unbindUserService(args, connection, true) } }
    }

    private suspend fun stream(service: ITouchService) = coroutineScope {
        val callback = LineCallback()
        _service.value = service
        try {
            launch { display.map { it.naturalSize }.distinctUntilChanged().collect { callback.screen = it } }
            service.start(callback)
            fail(callback.failure.filterNotNull().first())
            awaitCancellation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) { // RemoteException: service process died mid-call
            fail(PipelineError.SERVICE_FAILED, e)
        } finally {
            callback.active = false
            _service.value = null
            runCatching { service.stop() }
            onReset()
        }
    }

    private fun fail(error: PipelineError, cause: Throwable? = null) {
        Log.w(TAG, "Shizuku pipeline failed: $error", cause)
        _state.value = PipelineState.Failed(error)
    }

    /** Receives frames on a binder thread; oneway calls from one process are delivered in order. */
    private inner class LineCallback : ILineCallback.Stub() {
        @Volatile var active = true
        val failure = MutableStateFlow<PipelineError?>(null)
        @Volatile private var device: InputDeviceInfo? = null
        @Volatile private var parser: GeteventParser? = null

        @Volatile var screen: Pair<Int, Int>? = null
            set(value) {
                field = value
                rebuild()
            }

        override fun onDevice(path: String, name: String, minX: Int, maxX: Int, minY: Int, maxY: Int) {
            if (!active) return
            device = InputDeviceInfo(path, name, AbsRange(minX, maxX), AbsRange(minY, maxY), direct = true)
            rebuild()
            Log.i(TAG, "Touchscreen $path ($name) x=$minX..$maxX y=$minY..$maxY")
            _state.value = PipelineState.Running
        }

        override fun onLines(lines: Array<out String>?) {
            if (!active) return
            val p = parser ?: return
            lines?.forEach { line -> p.feed(line).forEach(onTouch) }
        }

        override fun onError(error: Int) {
            if (active) failure.value = PipelineError.entries.getOrElse(error) { PipelineError.SERVICE_FAILED }
        }

        private fun rebuild() {
            val d = device ?: return
            val (w, h) = screen ?: return
            parser = GeteventParser(CoordinateMapper(d, w, h))
        }
    }

    private companion object {
        const val TAG = "TotchShizuku"
    }
}
