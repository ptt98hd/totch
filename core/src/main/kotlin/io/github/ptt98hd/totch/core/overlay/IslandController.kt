package io.github.ptt98hd.totch.core.overlay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What the Dynamic Island may show (FR-O2…FR-O5); each can be switched off. */
@Serializable
enum class IslandActivity {
    @SerialName("media") MEDIA,
    @SerialName("notifications") NOTIFICATIONS,
    @SerialName("charging") CHARGING,
    @SerialName("sound") SOUND,
}

data class MediaInfo(val packageName: String, val title: String?, val artist: String?, val playing: Boolean)

enum class SoundMode { RING, VIBRATE, SILENT, DND_ON, DND_OFF }

/** Short-lived events shown for a while, then the island goes back to media or hides (IS-5). */
sealed interface IslandEvent {
    /** Just plugged in; [level] 0–100. */
    data class Charging(val level: Int) : IslandEvent
    /** A new notification that counts (NI-1); only its package is known (FR-O7). */
    data class Notification(val packageName: String) : IslandEvent
    data class Sound(val mode: SoundMode) : IslandEvent
    /** Media or ring volume changed; [fraction] 0–1. */
    data class Volume(val fraction: Float) : IslandEvent
}

sealed interface IslandContent {
    data object Hidden : IslandContent
    data class Media(val info: MediaInfo, val expanded: Boolean) : IslandContent
    data class Charging(val level: Int) : IslandContent
    data class Notification(val packageName: String) : IslandContent
    data class Sound(val mode: SoundMode) : IslandContent
    data class Volume(val fraction: Float) : IslandContent
}

enum class IslandTap { NONE, EXPANDED, COLLAPSED, OPEN_NOTIFICATIONS }

/**
 * Decides what the island shows (notch-overlay §3.2, IS-1…IS-14). Pure: time comes in as parameters and
 * [nextDeadlineMs] tells when [onTick] is due, like the gesture recognizer (NFR-3). Not thread-safe.
 */
class IslandController(activities: Set<IslandActivity>) {
    var activities: Set<IslandActivity> = activities

    private var media: MediaInfo? = null
    /** Media was seen playing, so it is shown (also while paused, until [lingerUntil]). */
    private var mediaShown = false
    private var lingerUntil: Long? = null
    private var expanded = false
    private var expandedUntil: Long? = null
    private var transient: IslandContent? = null
    private var transientUntil: Long? = null

    val content: IslandContent
        get() {
            transient?.takeIf { it.activity() in activities }?.let { return it }
            val m = media
            if (m != null && mediaShown && IslandActivity.MEDIA in activities) return IslandContent.Media(m, expanded)
            return IslandContent.Hidden
        }

    fun setMedia(media: MediaInfo?, nowMs: Long) {
        this.media = media
        when {
            media == null -> {
                mediaShown = false
                lingerUntil = null
                collapse()
            }
            media.playing -> {
                mediaShown = true
                lingerUntil = null
            }
            mediaShown && lingerUntil == null -> lingerUntil = nowMs + MEDIA_LINGER_MS // IS-3
        }
    }

    fun onEvent(event: IslandEvent, nowMs: Long) {
        val shown = when (event) {
            is IslandEvent.Charging -> IslandContent.Charging(event.level)
            is IslandEvent.Notification -> IslandContent.Notification(event.packageName)
            is IslandEvent.Sound -> IslandContent.Sound(event.mode)
            is IslandEvent.Volume -> IslandContent.Volume(event.fraction)
        }
        if (shown.activity() !in activities) return // IS-12
        transient = shown
        transientUntil = nowMs + durationOf(shown)
        collapse() // IS-7
    }

    fun onTap(nowMs: Long): IslandTap = when (val c = content) {
        is IslandContent.Media -> if (c.expanded) {
            collapse()
            IslandTap.COLLAPSED
        } else {
            expanded = true
            expandedUntil = nowMs + EXPANDED_MS
            IslandTap.EXPANDED
        }
        is IslandContent.Notification -> {
            clearTransient()
            IslandTap.OPEN_NOTIFICATIONS
        }
        else -> IslandTap.NONE
    }

    /** IS-9: a media control was used; keeps the island expanded. */
    fun onInteraction(nowMs: Long) {
        if (expanded) expandedUntil = nowMs + EXPANDED_MS
    }

    fun onTick(nowMs: Long) {
        transientUntil?.let { if (nowMs >= it) clearTransient() }
        lingerUntil?.let {
            if (nowMs >= it) {
                mediaShown = false
                lingerUntil = null
                collapse()
            }
        }
        expandedUntil?.let { if (nowMs >= it) collapse() }
    }

    fun nextDeadlineMs(): Long? = listOfNotNull(transientUntil, lingerUntil, expandedUntil).minOrNull()

    private fun collapse() {
        expanded = false
        expandedUntil = null
    }

    private fun clearTransient() {
        transient = null
        transientUntil = null
    }

    private fun IslandContent.activity(): IslandActivity = when (this) {
        is IslandContent.Media, IslandContent.Hidden -> IslandActivity.MEDIA
        is IslandContent.Notification -> IslandActivity.NOTIFICATIONS
        is IslandContent.Charging -> IslandActivity.CHARGING
        is IslandContent.Sound, is IslandContent.Volume -> IslandActivity.SOUND
    }

    private fun durationOf(c: IslandContent): Long = when (c) {
        is IslandContent.Charging -> CHARGING_MS
        is IslandContent.Notification -> NOTIFICATION_MS
        is IslandContent.Volume -> VOLUME_MS
        else -> SOUND_MS
    }

    companion object {
        const val CHARGING_MS = 3_000L
        const val NOTIFICATION_MS = 3_000L
        const val SOUND_MS = 2_000L
        const val VOLUME_MS = 1_500L
        const val EXPANDED_MS = 6_000L
        const val MEDIA_LINGER_MS = 5_000L
    }
}
