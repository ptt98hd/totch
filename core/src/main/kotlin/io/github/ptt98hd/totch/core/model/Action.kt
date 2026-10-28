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
    @Serializable @SerialName("launch_app") data class LaunchApp(val packageName: String) : Action
}
