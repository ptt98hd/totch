package io.github.ptt98hd.totch.action

import android.service.notification.NotificationListenerService

/**
 * Exists only so the system lets Totch call `MediaSessionManager.getActiveSessions` (actions spec §5).
 * It deliberately does not override `onNotificationPosted`: no notification content is read.
 */
class MediaListenerService : NotificationListenerService()
