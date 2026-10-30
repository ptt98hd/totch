package io.github.ptt98hd.totch.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import io.github.ptt98hd.totch.TotchApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** FR-D14: the watchdog job (detection-modes §9). */
object KeepAliveScheduler {
    private const val PERIODIC_ID = 1801
    private const val RESTART_ID = 1802
    const val PERIOD_MS = 15 * 60 * 1000L
    const val RESTART_DELAY_MS = 1_000L

    /** DM-50: periodic and persisted while gestures are on; cancelled with any pending restart when off. */
    fun apply(context: Context, enabled: Boolean) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (!enabled) {
            scheduler.cancel(PERIODIC_ID)
            scheduler.cancel(RESTART_ID)
            return
        }
        // Rescheduling would restart the countdown on every process start.
        if (scheduler.getPendingJob(PERIODIC_ID)?.intervalMillis == PERIOD_MS) return
        scheduler.schedule(
            JobInfo.Builder(PERIODIC_ID, ComponentName(context, KeepAliveJobService::class.java))
                .setPeriodic(PERIOD_MS)
                .setPersisted(true)
                .build(),
        )
    }

    /** DM-52: one restart shortly after the task was swiped away, in case the ROM kills the process with it. */
    fun scheduleRestart(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        scheduler.schedule(
            JobInfo.Builder(RESTART_ID, ComponentName(context, KeepAliveJobService::class.java))
                .setMinimumLatency(RESTART_DELAY_MS)
                .setOverrideDeadline(RESTART_DELAY_MS * 10)
                .build(),
        )
    }
}

/**
 * Wakes the process, which alone restarts the gesture service when on (DM-1), then makes sure it runs and turns the
 * accessibility service back on if Android dropped it (DM-55).
 */
class KeepAliveJobService : JobService() {
    private var job: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val platform = (application as TotchApp).platform
        job = platform.scope.launch {
            try {
                platform.keepAlive()
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        job?.cancel()
        return false
    }
}
