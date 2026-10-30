package io.github.ptt98hd.totch.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Action bound to a gesture. Serialized polymorphically with discriminator `"type"`; serial names are stable. */
@Serializable
sealed interface Action {
    @Serializable @SerialName("none") data object None : Action
    @Serializable @SerialName("back") data object Back : Action
    @Serializable @SerialName("home") data object Home : Action
    @Serializable @SerialName("recents") data object Recents : Action
    @Serializable @SerialName("lock") data object LockScreen : Action
    @Serializable @SerialName("screenshot") data object Screenshot : Action
    @Serializable @SerialName("notifications") data object Notifications : Action
    @Serializable @SerialName("quick_settings") data object QuickSettings : Action
    @Serializable @SerialName("torch") data object ToggleTorch : Action
    @Serializable @SerialName("play_pause") data object MediaPlayPause : Action
    @Serializable @SerialName("next") data object MediaNext : Action
    @Serializable @SerialName("previous") data object MediaPrevious : Action
    @Serializable @SerialName("volume_up") data object VolumeUp : Action
    @Serializable @SerialName("volume_down") data object VolumeDown : Action
    /** Shows the system volume panel without changing the volume (FR-A13). */
    @Serializable @SerialName("volume_panel") data object VolumePanel : Action
    @Serializable @SerialName("launch_app") data class LaunchApp(val packageName: String) : Action
    @Serializable @SerialName("assistant") data object Assistant : Action
    /** Toggles Android's one-handed mode (FR-A16). */
    @Serializable @SerialName("one_handed") data object OneHandedMode : Action

    // v1.6.3 display actions (FR-A17), through Shizuku.
    @Serializable @SerialName("brightness_up") data object BrightnessUp : Action
    @Serializable @SerialName("brightness_down") data object BrightnessDown : Action
    @Serializable @SerialName("auto_brightness") data object ToggleAutoBrightness : Action
    @Serializable @SerialName("auto_rotate") data object ToggleAutoRotate : Action

    // v1.6.3 sound modes (FR-A18), local; need Do Not Disturb access. 1.8.3 dropped "ringer_vibrate" (ST-35).
    @Serializable @SerialName("ringer_normal") data object RingerNormal : Action
    @Serializable @SerialName("ringer_silent") data object RingerSilent : Action
    @Serializable @SerialName("ringer_cycle") data object CycleRingerMode : Action
    @Serializable @SerialName("dnd") data object ToggleDnd : Action

    // v1.6.3 connections (FR-A19), through Shizuku.
    @Serializable @SerialName("wifi") data object ToggleWifi : Action
    @Serializable @SerialName("bluetooth") data object ToggleBluetooth : Action
    @Serializable @SerialName("mobile_data") data object ToggleMobileData : Action
    @Serializable @SerialName("airplane") data object ToggleAirplaneMode : Action
    @Serializable @SerialName("location") data object ToggleLocation : Action

    /** Static app shortcut; [label] is kept for display, [intentUri] is `Intent.toUri(URI_INTENT_SCHEME)`. */
    @Serializable @SerialName("launch_shortcut") data class LaunchShortcut(
        val packageName: String,
        val shortcutId: String,
        val label: String,
        val intentUri: String,
    ) : Action

    /** Exported activity of another app (FR-A14); [label] is kept for display. */
    @Serializable @SerialName("launch_activity") data class LaunchActivity(
        val packageName: String,
        val activityName: String,
        val label: String,
    ) : Action

    /** Quick Settings tile of an app, a `TileService` (FR-A15); [label] is kept for display. */
    @Serializable @SerialName("click_tile") data class ClickTile(
        val packageName: String,
        val serviceName: String,
        val label: String,
    ) : Action

    /** A system Settings page started by its intent action (FR-A21, actions spec §15). */
    @Serializable @SerialName("open_settings") data class OpenSettings(val intentAction: String) : Action

    /** An intent another app declares in its manifest, sent to [className] (FR-A22, actions spec §16). */
    @Serializable @SerialName("send_intent") data class SendIntent(
        val packageName: String,
        val className: String,
        val intentAction: String,
        val target: IntentTarget,
    ) : Action

    /** A contact opened in the default contacts app (FR-A23); [name] is kept for display. */
    @Serializable @SerialName("open_contact") data class OpenContact(
        val lookupUri: String,
        val name: String,
    ) : Action
}

/** Whether a [Action.SendIntent] starts an activity or sends a broadcast. */
@Serializable
enum class IntentTarget {
    @SerialName("activity") ACTIVITY,
    @SerialName("broadcast") BROADCAST,
}
