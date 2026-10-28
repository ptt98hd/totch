package io.github.ptt98hd.totch.action

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import io.github.ptt98hd.totch.accessibility.AccessibilityBridge
import io.github.ptt98hd.totch.core.action.ActionExecutor
import io.github.ptt98hd.totch.core.action.Capability
import io.github.ptt98hd.totch.core.model.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Torch, media keys and volume: no special permission needed (FR-A5..A7). */
class LocalExecutor(private val context: Context, private val accessibility: AccessibilityBridge) : ActionExecutor {
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
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
        Action.MediaPlayPause, Action.MediaNext, Action.MediaPrevious,
        Action.VolumeUp, Action.VolumeDown, Action.VolumePanel -> true
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
            Action.MediaPlayPause, Action.MediaNext, Action.MediaPrevious -> media(action)
            Action.VolumeUp -> volume(AudioManager.ADJUST_RAISE)
            Action.VolumeDown -> volume(AudioManager.ADJUST_LOWER)
            Action.VolumePanel -> volume(AudioManager.ADJUST_SAME) // only shows the panel (FR-A13)
            else -> error("unsupported")
        }
    }

    /** Actions spec §5: session controller, then headset hook via accessibility, then a media key. */
    private suspend fun media(action: Action) {
        if (controlSession(action)) return
        if (action == Action.MediaPlayPause && headsetHook()) return
        mediaKey(
            when (action) {
                Action.MediaNext -> KeyEvent.KEYCODE_MEDIA_NEXT
                Action.MediaPrevious -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            },
        )
    }

    private fun controlSession(action: Action): Boolean {
        if (context.packageName !in NotificationManagerCompat.getEnabledListenerPackages(context)) return false
        val sessions: List<MediaController> =
            runCatching { sessionManager.getActiveSessions(listenerComponent) }.getOrElse {
                Log.w(TAG, "getActiveSessions failed", it)
                return false
            }
        val playing = sessions.map { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val index = chooseMediaSession(sessions.mapIndexed { i, c -> MediaSessionInfo(c.packageName, playing[i]) })
            ?: return false
        val controls = sessions[index].transportControls
        when (mediaCommand(action, playing[index])) {
            MediaCommand.PLAY -> controls.play()
            MediaCommand.PAUSE -> controls.pause()
            MediaCommand.NEXT -> controls.skipToNext()
            MediaCommand.PREVIOUS -> controls.skipToPrevious()
            null -> return false
        }
        Log.d(TAG, "$action -> session ${sessions[index].packageName}")
        return true
    }

    private suspend fun headsetHook(): Boolean = withContext(Dispatchers.Main) {
        accessibility.service.value
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_KEYCODE_HEADSETHOOK) == true
    }

    private fun mediaKey(code: Int) {
        Log.d(TAG, "media key $code via AudioManager")
        val now = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
    }

    private fun volume(direction: Int) =
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)

    private companion object {
        const val TAG = "Totch/LocalExecutor"
    }
}
