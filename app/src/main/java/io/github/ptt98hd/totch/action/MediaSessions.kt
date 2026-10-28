package io.github.ptt98hd.totch.action

import io.github.ptt98hd.totch.core.model.Action

/** Snapshot of an active media session, in the system's priority order (actions spec §5). */
data class MediaSessionInfo(val packageName: String, val playing: Boolean)

enum class MediaCommand { PLAY, PAUSE, NEXT, PREVIOUS }

/** MC-1..MC-3: first playing session, else the highest-priority one. */
fun chooseMediaSession(sessions: List<MediaSessionInfo>): Int? =
    sessions.indexOfFirst { it.playing }.takeIf { it >= 0 } ?: sessions.indices.firstOrNull()

/** MC-4 */
fun mediaCommand(action: Action, playing: Boolean): MediaCommand? = when (action) {
    Action.MediaPlayPause -> if (playing) MediaCommand.PAUSE else MediaCommand.PLAY
    Action.MediaNext -> MediaCommand.NEXT
    Action.MediaPrevious -> MediaCommand.PREVIOUS
    else -> null
}
