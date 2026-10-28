package io.github.ptt98hd.totch.action

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import io.github.ptt98hd.totch.core.action.ActionExecutor
import io.github.ptt98hd.totch.core.action.Capability
import io.github.ptt98hd.totch.core.model.Action
import java.util.concurrent.ConcurrentHashMap

/** Torch, media keys and volume: no special permission needed (FR-A5..A7). */
class LocalExecutor(context: Context) : ActionExecutor {
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val torchState = ConcurrentHashMap<String, Boolean>()

    /** Back-facing camera with a flash unit, else any camera with a flash. */
    private val torchCameraId: String? by lazy {
        val ids = runCatching { cameraManager.cameraIdList.toList() }.getOrDefault(emptyList())
        val withFlash = ids.filter { id ->
            runCatching {
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }.getOrDefault(false)
        }
        val back = withFlash.firstOrNull { id ->
            runCatching {
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            }.getOrDefault(false)
        }
        (back ?: withFlash.firstOrNull())?.also {
            cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            torchState[cameraId] = enabled
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            torchState[cameraId] = false
        }
    }

    /** Resolves the flash camera and starts tracking torch state; call once off the main thread. */
    fun warmUp() {
        torchCameraId
    }

    override val capability = Capability.LOCAL

    override fun isAvailable() = true

    override fun supports(action: Action): Boolean = when (action) {
        Action.ToggleTorch -> torchCameraId != null
        Action.MediaPlayPause, Action.MediaNext, Action.MediaPrevious, Action.VolumeUp, Action.VolumeDown -> true
        else -> false
    }

    override suspend fun execute(action: Action): Result<Unit> = runCatching {
        when (action) {
            Action.ToggleTorch -> {
                val id = torchCameraId ?: error("no flash")
                val on = !(torchState[id] ?: false)
                cameraManager.setTorchMode(id, on)
                torchState[id] = on
            }
            Action.MediaPlayPause -> mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            Action.MediaNext -> mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            Action.MediaPrevious -> mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            Action.VolumeUp -> volume(AudioManager.ADJUST_RAISE)
            Action.VolumeDown -> volume(AudioManager.ADJUST_LOWER)
            else -> error("unsupported")
        }
    }

    private fun mediaKey(code: Int) {
        val now = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
    }

    private fun volume(direction: Int) =
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
}
