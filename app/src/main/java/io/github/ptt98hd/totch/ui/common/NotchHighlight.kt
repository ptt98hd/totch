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
import io.github.ptt98hd.totch.HighlightArea
import io.github.ptt98hd.totch.core.model.Rect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color

/**
 * Lets a screen replace the highlighted area while editing, e.g. the Notch screen showing the area of
 * unsaved slider drafts (ui spec §2). [override] wins over the saved area while [active].
 */
@Stable
class NotchHighlightState {
    var active by mutableStateOf(false)
    var override by mutableStateOf<HighlightArea?>(null)
}

val LocalNotchHighlight = staticCompositionLocalOf { NotchHighlightState() }

/**
 * Draws the touch area (screen pixels) directly over the activity at the notch (FR-U8): the notch itself
 * (width × height) filled, and its padding as a dashed outline around it (FR-R7). Draws nothing for `null`
 * (landscape) and never consumes touches.
 */
@Composable
fun NotchHighlightOverlay(area: HighlightArea?, state: NotchHighlightState, modifier: Modifier = Modifier) {
    val view = LocalView.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val color = MaterialTheme.colorScheme.primary
    val shown = if (state.active) state.override else area
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
        if (shown == null) return@Canvas
        fun Rect.box() = Offset(left - origin.x, top - origin.y) to Size(width.toFloat(), height.toFloat())
        val stroke = 1.5.dp.toPx()
        if (shown.touch != shown.notch && shown.touch.width > 0 && shown.touch.height > 0) {
            val (topLeft, size) = shown.touch.box()
            drawPill(color.copy(alpha = 0.12f), topLeft, size)
            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
            drawPill(color.copy(alpha = 0.8f), topLeft, size, Stroke(width = stroke, pathEffect = dash))
        }
        if (shown.notch.width > 0 && shown.notch.height > 0) {
            val (topLeft, size) = shown.notch.box()
            drawPill(color.copy(alpha = 0.35f), topLeft, size)
            drawPill(color, topLeft, size, Stroke(width = stroke))
        }
    }
}

private fun DrawScope.drawPill(color: Color, topLeft: Offset, size: Size, style: Stroke? = null) {
    val corner = CornerRadius(minOf(size.width, size.height) / 2)
    if (style == null) {
        drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = corner)
    } else {
        drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = corner, style = style)
    }
}
