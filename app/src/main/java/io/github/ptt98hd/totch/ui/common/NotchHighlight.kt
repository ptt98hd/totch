package io.github.ptt98hd.totch.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import io.github.ptt98hd.totch.core.model.Rect

/**
 * Lets a screen replace the highlighted region while editing, e.g. Settings showing the region of
 * unsaved slider drafts (ui spec §2). [override] wins over the saved region while [active].
 */
@Stable
class NotchHighlightState {
    var active by mutableStateOf(false)
    var override by mutableStateOf<Rect?>(null)
}

val LocalNotchHighlight = staticCompositionLocalOf { NotchHighlightState() }

/**
 * Draws the touch area (screen pixels) directly over the activity at the notch (FR-U8). Draws
 * nothing for `null` (landscape) and never consumes touches.
 */
@Composable
fun NotchHighlightOverlay(region: Rect?, state: NotchHighlightState, modifier: Modifier = Modifier) {
    val view = LocalView.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val color = MaterialTheme.colorScheme.primary
    val rect = if (state.active) state.override else region
    Canvas(
        modifier.fillMaxSize().onGloballyPositioned { coordinates ->
            // Window origin on screen + this canvas' position in the window.
            val viewOnScreen = IntArray(2).also(view::getLocationOnScreen)
            val viewInWindow = IntArray(2).also(view::getLocationInWindow)
            val inWindow = coordinates.positionInWindow()
            origin = Offset(
                viewOnScreen[0] - viewInWindow[0] + inWindow.x,
                viewOnScreen[1] - viewInWindow[1] + inWindow.y,
            )
        },
    ) {
        if (rect == null || rect.width <= 0 || rect.height <= 0) return@Canvas
        val topLeft = Offset(rect.left - origin.x, rect.top - origin.y)
        val size = Size(rect.width.toFloat(), rect.height.toFloat())
        val corner = CornerRadius(minOf(size.width, size.height) / 2)
        drawRoundRect(color = color.copy(alpha = 0.35f), topLeft = topLeft, size = size, cornerRadius = corner)
        drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = corner, style = Stroke(width = 1.5.dp.toPx()))
    }
}
