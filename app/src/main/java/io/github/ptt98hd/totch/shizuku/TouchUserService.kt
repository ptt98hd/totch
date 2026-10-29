package io.github.ptt98hd.totch.shizuku

import android.os.ParcelFileDescriptor
import io.github.ptt98hd.totch.core.input.GeteventDeviceParser
import io.github.ptt98hd.totch.core.update.UpdateCommands
import io.github.ptt98hd.totch.engine.PipelineError
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Shizuku UserService: runs in a separate process as uid shell (2000), which may read `/dev/input`.
 * Instantiated by Shizuku via reflection (kept in proguard-rules.pro). Plain threads only: this
 * process has no Android main looper.
 */
class TouchUserService : ITouchService.Stub() {
    private val lock = Any()
    private var stream: Stream? = null

    /** Shizuku requires the process to exit here. */
    override fun destroy() {
        stop()
        exitProcess(0)
    }

    override fun start(callback: ILineCallback) = synchronized(lock) {
        stream?.close()
        stream = Stream(callback).also { it.start() }
    }

    override fun stop() = synchronized(lock) {
        stream?.close()
        stream = null
    }

    override fun exec(command: String): Int = try {
        val process = ProcessBuilder("sh", "-c", command)
            .redirectErrorStream(true)
            .redirectOutput(DEV_NULL)
            .start()
        if (process.waitFor(EXEC_TIMEOUT_S, TimeUnit.SECONDS)) process.exitValue() else -1.also { process.destroy() }
    } catch (_: Exception) {
        -1
    }

    override fun writeFile(source: ParcelFileDescriptor, path: String): Int = try {
        if (!UpdateCommands.isSafeApkPath(path)) {
            -1
        } else {
            // Owned by shell like the `pm` that reads it, so no wider permissions are needed.
            ParcelFileDescriptor.AutoCloseInputStream(source).use { input -> File(path).outputStream().use(input::copyTo) }
            0
        }
    } catch (_: Exception) {
        -1
    }

    /** `getevent -pl` → touchscreen → `getevent -lt <path>`, forwarded one input frame per binder call. */
    private class Stream(private val callback: ILineCallback) : Thread("totch-getevent") {
        @Volatile private var closed = false
        @Volatile private var process: Process? = null

        override fun run() {
            val error = try {
                pump()
            } catch (_: Exception) {
                PipelineError.SERVICE_FAILED // includes DeadObjectException: the app is gone
            } finally {
                process?.destroy()
            }
            if (!closed && error != null) runCatching { callback.onError(error.ordinal) }
        }

        private fun pump(): PipelineError? {
            val devices = GeteventDeviceParser.parse(readAll("getevent", "-pl"))
            val device = GeteventDeviceParser.pickTouchscreen(devices) ?: return PipelineError.NO_TOUCHSCREEN
            callback.onDevice(device.path, device.name, device.x.min, device.x.max, device.y.min, device.y.max)
            // getevent disables stdout buffering itself and reports CLOCK_MONOTONIC timestamps.
            val p = ProcessBuilder("getevent", "-lt", device.path).redirectError(DEV_NULL).start()
            process = p
            if (closed) return null
            val batch = ArrayList<String>(8)
            p.inputStream.bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    batch += line
                    if ("SYN_REPORT" in line || batch.size >= MAX_BATCH) {
                        callback.onLines(batch.toTypedArray())
                        batch.clear()
                    }
                }
            }
            return if (closed) null else PipelineError.STREAM_ENDED
        }

        private fun readAll(vararg command: String): String {
            val p = ProcessBuilder(*command).redirectError(DEV_NULL).start()
            process = p
            // `-p` makes getevent exit after printing; its output (a few KiB) fits in the pipe buffer.
            if (!p.waitFor(EXEC_TIMEOUT_S, TimeUnit.SECONDS)) p.destroy()
            return p.inputStream.bufferedReader().use { it.readText() }
        }

        fun close() {
            closed = true
            process?.destroy()
        }
    }

    private companion object {
        val DEV_NULL = File("/dev/null")
        const val EXEC_TIMEOUT_S = 10L
        const val MAX_BATCH = 64
    }
}
