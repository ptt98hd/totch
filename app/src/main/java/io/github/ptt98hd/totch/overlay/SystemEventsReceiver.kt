package io.github.ptt98hd.totch.overlay

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import io.github.ptt98hd.totch.core.overlay.IslandEvent
import io.github.ptt98hd.totch.core.overlay.SoundMode

/**
 * Charging and sound events for the island (FR-O4, FR-O5), registered only while one of them is shown.
 * `VOLUME_CHANGED_ACTION` is not a public API; devices that do not send it simply show no volume pill.
 */
class SystemEventsReceiver(private val context: Context, private val hub: OverlayHub) : BroadcastReceiver() {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val battery = context.getSystemService(BatteryManager::class.java)
    private var registered = false

    fun setRegistered(on: Boolean) {
        if (on == registered) return
        registered = on
        if (on) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
                addAction(VOLUME_CHANGED_ACTION)
            }
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            runCatching { context.unregisterReceiver(this) }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (isInitialStickyBroadcast) return // the ringer mode is sticky: no pill on registration
        val event = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED ->
                IslandEvent.Charging(battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100))
            AudioManager.RINGER_MODE_CHANGED_ACTION -> IslandEvent.Sound(
                when (intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, audio.ringerMode)) {
                    AudioManager.RINGER_MODE_SILENT -> SoundMode.SILENT
                    AudioManager.RINGER_MODE_VIBRATE -> SoundMode.VIBRATE
                    else -> SoundMode.RING
                },
            )
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> IslandEvent.Sound(
                if (notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) SoundMode.DND_OFF else SoundMode.DND_ON,
            )
            VOLUME_CHANGED_ACTION -> volume(intent)
            else -> null
        } ?: return
        hub.onIslandEvent(event)
    }

    private fun volume(intent: Intent): IslandEvent? {
        val stream = intent.getIntExtra(EXTRA_STREAM, -1)
        if (stream != AudioManager.STREAM_MUSIC && stream != AudioManager.STREAM_RING) return null
        val value = intent.getIntExtra(EXTRA_VALUE, -1)
        if (value < 0 || value == intent.getIntExtra(EXTRA_PREV_VALUE, -1)) return null
        val max = audio.getStreamMaxVolume(stream).takeIf { it > 0 } ?: return null
        return IslandEvent.Volume(value.toFloat() / max)
    }

    private companion object {
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        const val EXTRA_STREAM = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        const val EXTRA_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
        const val EXTRA_PREV_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE"
    }
}
