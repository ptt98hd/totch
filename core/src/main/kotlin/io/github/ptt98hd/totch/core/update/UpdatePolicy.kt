package io.github.ptt98hd.totch.core.update

import io.github.ptt98hd.totch.core.settings.UpdateInterval

/** Background update checks (update spec §6). */
object UpdatePolicy {
    private const val DAY_MS = 86_400_000L

    /** UP-11: JobScheduler period, null when checks are off. */
    fun periodMillis(interval: UpdateInterval): Long? = interval.days.takeIf { it > 0 }?.let { it * DAY_MS }

    /** UP-12: a newer release that has not been notified yet. */
    fun shouldNotify(release: Release?, currentVersionName: String, lastNotified: String?): Boolean =
        release != null && Releases.isNewer(release, currentVersionName) && release.version.toString() != lastNotified
}
