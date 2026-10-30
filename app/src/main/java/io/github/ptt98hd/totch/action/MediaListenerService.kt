package io.github.ptt98hd.totch.action

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.core.overlay.NotificationMeta

/**
 * Lets the system give Totch `MediaSessionManager.getActiveSessions` (actions spec §5) and feeds the notch overlay
 * (notch-overlay §2, FR-O7) with notification **metadata only**: key, package, time, flags, importance and color —
 * never the title, text or extras. Nothing is stored.
 */
class MediaListenerService : NotificationListenerService() {
    private val platform get() = (application as TotchApp).platform

    override fun onListenerConnected() {
        val active = runCatching { activeNotifications.orEmpty().map { meta(it) } }.getOrDefault(emptyList())
        platform.overlay.onNotificationsReset(active)
        platform.mediaWatcher.start()
    }

    override fun onListenerDisconnected() {
        platform.mediaWatcher.stop()
        platform.overlay.onNotificationsReset(emptyList())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        platform.overlay.onNotificationPosted(meta(sbn, rankingMap))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        platform.overlay.onNotificationRemoved(sbn.key)
    }

    private fun meta(sbn: StatusBarNotification, rankingMap: RankingMap? = currentRanking): NotificationMeta {
        val n = sbn.notification
        val ranking = Ranking()
        val importance = if (rankingMap?.getRanking(sbn.key, ranking) == true) ranking.importance else NotificationManager.IMPORTANCE_DEFAULT
        return NotificationMeta(
            key = sbn.key,
            packageName = sbn.packageName,
            postTimeMs = sbn.postTime,
            ongoing = sbn.isOngoing,
            clearable = sbn.isClearable,
            groupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            alerting = importance >= NotificationManager.IMPORTANCE_DEFAULT,
            onlyAlertOnce = n.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0,
            color = n.color.takeIf { it != Notification.COLOR_DEFAULT },
        )
    }
}
