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
 * (width × height) filled with an outline just outside it, and its padding up to a dashed outline (FR-R7, FR-U65).
 * Draws nothing for `null` (landscape) and never consumes touches.
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
        val stroke = 1.5.dp.toPx()
        // FR-U65: the solid outline sits just outside the notch, the dashed one just inside the touch area.
        if (shown.touch != shown.notch && shown.touch.width > 0 && shown.touch.height > 0) {
            val inside = outlineInside(shown.touch, stroke)
            drawPill(color.copy(alpha = 0.12f), shown.touch.toOutline(), origin)
            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
            drawPill(color.copy(alpha = 0.8f), inside, origin, Stroke(width = stroke, pathEffect = dash))
        }
        if (shown.notch.width > 0 && shown.notch.height > 0) {
            drawPill(color.copy(alpha = 0.35f), shown.notch.toOutline(), origin)
            drawPill(color, outlineOutside(shown.notch, stroke), origin, Stroke(width = stroke))
        }
    }
}

/** A rounded rect in screen pixels, as drawn by the highlight (UI-84). */
data class Outline(val left: Float, val top: Float, val right: Float, val bottom: Float, val corner: Float)

private fun Rect.toOutline() = Outline(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), minOf(width, height) / 2f)

/** UI-84: the path of a [stroke] wide line whose inner edge is the edge of [r]. */
fun outlineOutside(r: Rect, stroke: Float): Outline {
    val half = stroke / 2
    val o = r.toOutline()
    return Outline(o.left - half, o.top - half, o.right + half, o.bottom + half, o.corner + half)
}

/** UI-84: the path of a [stroke] wide line whose outer edge is the edge of [r]; an empty rect when it does not fit. */
fun outlineInside(r: Rect, stroke: Float): Outline {
    val half = stroke / 2
    val o = r.toOutline()
    val w = (o.right - o.left - stroke).coerceAtLeast(0f)
    val h = (o.bottom - o.top - stroke).coerceAtLeast(0f)
    val left = o.left + half
    val top = o.top + half
    return Outline(left, top, left + w, top + h, (o.corner - half).coerceIn(0f, minOf(w, h) / 2))
}

private fun DrawScope.drawPill(color: Color, o: Outline, origin: Offset, style: Stroke? = null) {
    val topLeft = Offset(o.left - origin.x, o.top - origin.y)
    val size = Size(o.right - o.left, o.bottom - o.top)
    val corner = CornerRadius(o.corner)
    if (style == null) {
        drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = corner)
    } else {
        drawRoundRect(color = color, topLeft = topLeft, size = size, cornerRadius = corner, style = style)
    }
}
