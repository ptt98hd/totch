package io.github.ptt98hd.totch.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import io.github.ptt98hd.totch.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

/**
 * One-off shell commands as uid shell, independent of the touch pipeline (detection-modes §6): binds its own
 * [TouchUserService] instance (tag "shell") for the duration of [exec], then removes it.
 */
class ShizukuShell(context: Context, private val shizuku: ShizukuManager) {
    private val mutex = Mutex()
    private val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, TouchUserService::class.java.name))
        .tag("shell")
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    /** Runs [commands] in order, stopping at the first failure; false when Shizuku is not ready or any command failed. */
    suspend fun exec(commands: List<String>): Boolean = mutex.withLock {
        shizuku.refresh()
        if (!shizuku.state.value.ready) return false
        val connected = CompletableDeferred<ITouchService?>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                connected.complete(binder?.takeIf { it.pingBinder() }?.let(ITouchService.Stub::asInterface))
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                connected.complete(null)
            }
        }
        try {
            withContext(Dispatchers.Main) { Shizuku.bindUserService(args, connection) }
            val service = withTimeoutOrNull(BIND_TIMEOUT_MS) { connected.await() } ?: return false
            withContext(Dispatchers.IO) { commands.all { service.exec(it) == 0 } }
        } catch (e: Exception) { // RemoteException, IllegalStateException from a dead binder
            Log.w(TAG, "Shell command failed", e)
            false
        } finally {
            withContext(NonCancellable + Dispatchers.Main) { runCatching { Shizuku.unbindUserService(args, connection, true) } }
        }
    }

    private companion object {
        const val TAG = "TotchShell"
        const val BIND_TIMEOUT_MS = 10_000L
    }
}
