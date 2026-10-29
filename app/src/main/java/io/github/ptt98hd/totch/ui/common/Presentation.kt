package io.github.ptt98hd.totch.ui.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Assistant
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material.icons.outlined.SwipeDown
import androidx.compose.material.icons.outlined.SwipeDownAlt
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material.icons.outlined.SwipeLeftAlt
import androidx.compose.material.icons.outlined.SwipeRight
import androidx.compose.material.icons.outlined.SwipeRightAlt
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.outlined.WebAsset
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture

@get:StringRes
val Gesture.labelRes: Int
    get() = when (this) {
        Gesture.TAP -> R.string.gesture_tap
        Gesture.DOUBLE_TAP -> R.string.gesture_double_tap
        Gesture.LONG_PRESS -> R.string.gesture_long_press
        Gesture.SWIPE_DOWN -> R.string.gesture_swipe_down
        Gesture.SWIPE_LEFT -> R.string.gesture_swipe_left
        Gesture.SWIPE_RIGHT -> R.string.gesture_swipe_right
        Gesture.SWIPE_DOWN_HOLD -> R.string.gesture_swipe_down_hold
        Gesture.SWIPE_LEFT_HOLD -> R.string.gesture_swipe_left_hold
        Gesture.SWIPE_RIGHT_HOLD -> R.string.gesture_swipe_right_hold
    }

val Gesture.icon: ImageVector
    get() = when (this) {
        Gesture.TAP -> Icons.Outlined.TouchApp
        Gesture.DOUBLE_TAP -> Icons.Outlined.AdsClick
        Gesture.LONG_PRESS -> Icons.Outlined.PanTool
        Gesture.SWIPE_DOWN -> Icons.Outlined.SwipeDown
        Gesture.SWIPE_LEFT -> Icons.Outlined.SwipeLeft
        Gesture.SWIPE_RIGHT -> Icons.Outlined.SwipeRight
        Gesture.SWIPE_DOWN_HOLD -> Icons.Outlined.SwipeDownAlt
        Gesture.SWIPE_LEFT_HOLD -> Icons.Outlined.SwipeLeftAlt
        Gesture.SWIPE_RIGHT_HOLD -> Icons.Outlined.SwipeRightAlt
    }

/** Label of a fixed action; [Action.LaunchApp] maps to the generic "Open app…" entry. */
@get:StringRes
val Action.labelRes: Int
    get() = when (this) {
        Action.None -> R.string.action_none
        Action.Back -> R.string.action_back
        Action.Home -> R.string.action_home
        Action.Recents -> R.string.action_recents
        Action.LockScreen -> R.string.action_lock
        Action.Screenshot -> R.string.action_screenshot
        Action.Notifications -> R.string.action_notifications
        Action.QuickSettings -> R.string.action_quick_settings
        Action.ToggleTorch -> R.string.action_torch
        Action.MediaPlayPause -> R.string.action_play_pause
        Action.MediaNext -> R.string.action_next
        Action.MediaPrevious -> R.string.action_previous
        Action.VolumeUp -> R.string.action_volume_up
        Action.VolumeDown -> R.string.action_volume_down
        Action.VolumePanel -> R.string.action_volume_panel
        is Action.LaunchApp -> R.string.action_open_app
        Action.Assistant -> R.string.action_assistant
        Action.OneHandedMode -> R.string.action_one_handed
        is Action.LaunchShortcut -> R.string.action_open_shortcut
        is Action.LaunchActivity -> R.string.action_open_activity
        is Action.ClickTile -> R.string.action_click_tile
    }

val Action.icon: ImageVector
    get() = when (this) {
        Action.None -> Icons.Outlined.Block
        Action.Back -> Icons.AutoMirrored.Outlined.ArrowBack
        Action.Home -> Icons.Outlined.Home
        Action.Recents -> Icons.Outlined.ViewCarousel
        Action.LockScreen -> Icons.Outlined.Lock
        Action.Screenshot -> Icons.Outlined.Screenshot
        Action.Notifications -> Icons.Outlined.Notifications
        Action.QuickSettings -> Icons.Outlined.Tune
        Action.ToggleTorch -> Icons.Outlined.FlashlightOn
        Action.MediaPlayPause -> Icons.Outlined.PlayCircle
        Action.MediaNext -> Icons.Outlined.SkipNext
        Action.MediaPrevious -> Icons.Outlined.SkipPrevious
        Action.VolumeUp -> Icons.AutoMirrored.Outlined.VolumeUp
        Action.VolumeDown -> Icons.AutoMirrored.Outlined.VolumeDown
        Action.VolumePanel -> Icons.Outlined.Speaker
        is Action.LaunchApp -> Icons.Outlined.Apps
        Action.Assistant -> Icons.Outlined.Assistant
        Action.OneHandedMode -> Icons.Outlined.VerticalAlignBottom
        is Action.LaunchShortcut -> Icons.Outlined.SubdirectoryArrowRight
        is Action.LaunchActivity -> Icons.Outlined.WebAsset
        is Action.ClickTile -> Icons.Outlined.GridView
    }

@StringRes
fun ActionGroupId.labelRes(): Int = when (this) {
    ActionGroupId.SYSTEM -> R.string.group_system
    ActionGroupId.MEDIA -> R.string.group_media
}

/** Human-readable action name; LaunchApp/LaunchShortcut/LaunchActivity/ClickTile show the app label (UI-12) or the package name. */
@Composable
fun actionDisplayName(action: Action, appLabel: String?): String = when (action) {
    is Action.LaunchApp -> stringResource(R.string.action_launch_app, appLabel ?: action.packageName)
    is Action.LaunchShortcut -> stringResource(R.string.action_launch_shortcut, appLabel ?: action.packageName, action.label)
    is Action.LaunchActivity -> stringResource(R.string.action_launch_shortcut, appLabel ?: action.packageName, action.label)
    is Action.ClickTile -> stringResource(R.string.action_launch_shortcut, appLabel ?: action.packageName, action.label)
    else -> stringResource(action.labelRes)
}
