package io.github.ow3n.totch.action

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.annotation.StringRes
import io.github.ow3n.totch.InstalledApps
import io.github.ow3n.totch.R
import io.github.ow3n.totch.core.model.Action
import io.github.ow3n.totch.engine.EngineFeedback

/** Haptic tick on success (FR-A10), short toast on failure (FR-A9). */
class AndroidFeedback(private val context: Context, private val apps: InstalledApps) : EngineFeedback {
    private val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    private val main = Handler(Looper.getMainLooper())

    override fun onPerformed() {
        runCatching { vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)) }
    }

    override fun onFailed(action: Action, reason: String) {
        val name = when (action) {
            is Action.LaunchApp -> apps.labelOf(action.packageName) ?: action.packageName
            else -> context.getString(labelOf(action))
        }
        main.post { Toast.makeText(context, context.getString(R.string.platform_action_failed, name), Toast.LENGTH_SHORT).show() }
    }

    @StringRes
    private fun labelOf(action: Action): Int = when (action) {
        Action.None -> R.string.platform_action_none
        Action.Back -> R.string.platform_action_back
        Action.Home -> R.string.platform_action_home
        Action.Recents -> R.string.platform_action_recents
        Action.LockScreen -> R.string.platform_action_lock
        Action.Screenshot -> R.string.platform_action_screenshot
        Action.Notifications -> R.string.platform_action_notifications
        Action.QuickSettings -> R.string.platform_action_quick_settings
        Action.ToggleTorch -> R.string.platform_action_torch
        Action.MediaPlayPause -> R.string.platform_action_play_pause
        Action.MediaNext -> R.string.platform_action_next
        Action.MediaPrevious -> R.string.platform_action_previous
        Action.VolumeUp -> R.string.platform_action_volume_up
        Action.VolumeDown -> R.string.platform_action_volume_down
        is Action.LaunchApp -> R.string.platform_action_launch_app
    }
}
