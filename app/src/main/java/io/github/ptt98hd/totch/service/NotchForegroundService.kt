package io.github.ptt98hd.totch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.TotchApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Keeps gestures alive while enabled (FR-D5, DM-1, DM-7) and hosts the Shizuku touch pipeline, which idles while Shizuku
 * is not ready (the accessibility overlay is then the touch source, FR-D13); started/stopped by [DefaultAppContainer].
 */
class NotchForegroundService : LifecycleService() {
    private var job: Job? = null
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.platform_channel_name), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.platform_channel_description) },
        )
        val notification = notification(getString(R.string.platform_notification_waiting))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (job == null) {
            val platform = (application as TotchApp).platform
            job = lifecycleScope.launch {
                launch { platform.engineStatus.collect(::updateNotification) }
                platform.pipeline.run()
            }
        }
        return START_STICKY
    }

    private fun updateNotification(status: EngineStatus) {
        val text = when (status) {
            EngineStatus.Running -> getString(R.string.platform_notification_running)
            EngineStatus.NeedsAccess -> getString(R.string.platform_notification_needs_access)
            EngineStatus.Starting -> getString(R.string.platform_notification_waiting)
            EngineStatus.PausedRotation -> getString(R.string.platform_notification_paused)
            is EngineStatus.Error -> status.message
            EngineStatus.Disabled, EngineStatus.Stopping -> return // about to be stopped
        }
        runCatching { notificationManager.notify(NOTIFICATION_ID, notification(text)) }
    }

    private fun notification(text: String): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_totch)
            .setContentTitle(getString(R.string.platform_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(launch)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "TotchService"
        private const val CHANNEL_ID = "gesture_service"
        private const val NOTIFICATION_ID = 1

        /** Starts or stops the service; a start that Android disallows from the background is only logged. */
        fun setRunning(context: Context, running: Boolean) {
            val intent = Intent(context, NotchForegroundService::class.java)
            try {
                if (running) context.startForegroundService(intent) else context.stopService(intent)
            } catch (e: IllegalStateException) { // ForegroundServiceStartNotAllowedException
                Log.w(TAG, "Cannot ${if (running) "start" else "stop"} service now", e)
            }
        }
    }
}
