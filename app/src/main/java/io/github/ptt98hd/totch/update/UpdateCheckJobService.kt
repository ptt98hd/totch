package io.github.ptt98hd.totch.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import io.github.ptt98hd.totch.BuildConfig
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.core.settings.UpdateInterval
import io.github.ptt98hd.totch.core.update.Release
import io.github.ptt98hd.totch.core.update.UpdatePolicy
import io.github.ptt98hd.totch.ui.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Schedules or cancels the periodic update check for [UpdateInterval] (update spec §6, UP-11). */
object UpdateScheduler {
    private const val JOB_ID = 1601

    fun apply(context: Context, interval: UpdateInterval) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        val period = UpdatePolicy.periodMillis(interval)
        if (period == null) {
            scheduler.cancel(JOB_ID)
            return
        }
        // Rescheduling the same period would restart its countdown on every app start.
        if (scheduler.getPendingJob(JOB_ID)?.intervalMillis == period) return
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, UpdateCheckJobService::class.java))
            .setPeriodic(period)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .build()
        scheduler.schedule(job)
    }
}

/** Background check: notifies each newer release once; tapping opens the update dialog (update spec §6). */
class UpdateCheckJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val updater = (application as TotchApp).container.appUpdater
        running = scope.launch {
            val retry = try {
                val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
                val release = updater.latestRelease()
                if (UpdatePolicy.shouldNotify(release, BuildConfig.VERSION_NAME, prefs.getString(KEY_NOTIFIED, null))) {
                    notify(release!!)
                    prefs.edit { putString(KEY_NOTIFIED, release.version.toString()) }
                }
                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) { // network: let JobScheduler retry with backoff
                Log.w(TAG, "Update check failed", e)
                true
            }
            jobFinished(params, retry)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notify(release: Release) {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.update_channel_name), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_SHOW_UPDATE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_totch)
            .setContentTitle(getString(R.string.update_notification_title, release.version.toString()))
            .setContentText(getString(R.string.update_notification_text))
            .setContentIntent(PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val TAG = "TotchUpdate"
        const val CHANNEL_ID = "updates"
        const val NOTIFICATION_ID = 2
        const val PREFS = "update"
        const val KEY_NOTIFIED = "last_notified"
    }
}
