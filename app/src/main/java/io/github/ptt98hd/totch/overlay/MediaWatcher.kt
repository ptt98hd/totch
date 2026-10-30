package io.github.ptt98hd.totch.overlay

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.ptt98hd.totch.action.MediaListenerService
import io.github.ptt98hd.totch.action.MediaSessionInfo
import io.github.ptt98hd.totch.action.chooseMediaSession
import io.github.ptt98hd.totch.core.overlay.IslandControl
import io.github.ptt98hd.totch.core.overlay.MediaInfo

/**
 * Feeds the island with the media session Totch would control (MC-1…MC-3, FR-O2) and runs its buttons. Needs the
 * notification listener to be enabled; started when it connects, stopped when it disconnects. Main thread only.
 */
class MediaWatcher(context: Context, private val hub: OverlayHub) {
    private val sessions = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, MediaListenerService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var controllers: List<MediaController> = emptyList()
    private var current: MediaController? = null
    private var started = false

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { update(it.orEmpty()) }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = pick()
        override fun onMetadataChanged(metadata: MediaMetadata?) = pick()
        override fun onSessionDestroyed() = update(controllers - current)
    }

    fun start() {
        if (started) return
        runCatching {
            sessions.addOnActiveSessionsChangedListener(sessionsChanged, component, handler)
            started = true
            update(sessions.getActiveSessions(component))
        }.onFailure { Log.w(TAG, "media sessions unavailable", it) }
        hub.mediaControl = ::control
    }

    fun stop() {
        if (started) runCatching { sessions.removeOnActiveSessionsChangedListener(sessionsChanged) }
        started = false
        update(emptyList())
    }

    private fun update(list: List<MediaController?>) {
        controllers.forEach { it.unregisterCallback(callback) }
        controllers = list.filterNotNull()
        controllers.forEach { it.registerCallback(callback, handler) }
        pick()
    }

    private fun MediaController.isPlaying() = playbackState?.state == PlaybackState.STATE_PLAYING

    private fun pick() {
        val index = chooseMediaSession(controllers.map { MediaSessionInfo(it.packageName, it.isPlaying()) })
        val c = index?.let(controllers::get)
        current = c
        val metadata = c?.metadata
        hub.mediaArt.value = metadata?.art()
        hub.onMedia(
            c?.let {
                MediaInfo(
                    packageName = it.packageName,
                    title = metadata?.text(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, MediaMetadata.METADATA_KEY_TITLE),
                    artist = metadata?.text(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, MediaMetadata.METADATA_KEY_ARTIST),
                    playing = it.isPlaying(),
                )
            },
        )
    }

    private fun control(control: IslandControl) {
        val c = current ?: return
        val controls = c.transportControls
        when (control) {
            IslandControl.PREVIOUS -> controls.skipToPrevious()
            IslandControl.NEXT -> controls.skipToNext()
            IslandControl.PLAY_PAUSE -> if (c.isPlaying()) controls.pause() else controls.play()
        }
    }

    private fun MediaMetadata.text(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { getText(it)?.toString()?.takeIf(String::isNotBlank) }

    private fun MediaMetadata.art(): Bitmap? = listOf(
        MediaMetadata.METADATA_KEY_ART,
        MediaMetadata.METADATA_KEY_ALBUM_ART,
        MediaMetadata.METADATA_KEY_DISPLAY_ICON,
    ).firstNotNullOfOrNull { getBitmap(it) }

    private companion object {
        const val TAG = "TotchMedia"
    }
}
