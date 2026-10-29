package io.github.ptt98hd.totch.action

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import io.github.ptt98hd.totch.InstalledApps
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.engine.EngineFeedback

/** Haptic click on success (FR-A10, actions spec §6), short toast on failure (FR-A9). */
class AndroidFeedback(private val context: Context, private val apps: InstalledApps) : EngineFeedback {
    private val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        ?.takeIf { it.hasVibrator() }
    private val main = Handler(Looper.getMainLooper())

    /** HF-1: a predefined click where the hardware supports it, else a short one-shot. */
    private val effect: VibrationEffect? by lazy {
        val v = vibrator ?: return@lazy null
        if (v.areAllEffectsSupported(VibrationEffect.EFFECT_CLICK) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } else {
            VibrationEffect.createOneShot(ONE_SHOT_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    }

    /**
     * HF-2: hardware-feedback usage is allowed from the background and does not depend on the
     * system "touch feedback" switch (an attribute-less vibration is classified as touch feedback).
     */
    override fun onPerformed() {
        val v = vibrator ?: return
        val e = effect ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                v.vibrate(e, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(e, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
            }
        }.onFailure { Log.w(TAG, "vibrate failed", it) }
    }

    override fun onFailed(action: Action, reason: String) {
        val name = when (action) {
            is Action.LaunchApp -> apps.labelOf(action.packageName) ?: action.packageName
            is Action.LaunchShortcut -> action.label
            is Action.LaunchActivity -> action.label
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
        Action.VolumePanel -> R.string.platform_action_volume_panel
        is Action.LaunchApp -> R.string.platform_action_launch_app
        Action.Assistant -> R.string.platform_action_assistant
        is Action.LaunchShortcut -> R.string.platform_action_shortcut
        is Action.LaunchActivity -> R.string.platform_action_activity
    }

    private companion object {
        const val TAG = "Totch/Feedback"
        const val ONE_SHOT_MS = 30L
    }
}
