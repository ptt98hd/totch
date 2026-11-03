package io.github.ptt98hd.totch.core.region

import io.github.ptt98hd.totch.core.model.Rect
import kotlin.math.roundToInt

/** Bounding rects of the display cutout on the top edge, as reported by Android. */
data class Cutout(val boundingRectsTop: List<Rect>)

/**
 * User-defined region in pixels; [centerXPx] is its horizontal center, [topPx] the distance of its top edge from the
 * top of the screen (FR-R10).
 */
data class ManualRegion(val centerXPx: Int, val widthPx: Int, val heightPx: Int, val topPx: Int = 0)

object NotchRegionResolver {
    private const val DEFAULT_WIDTH_FRACTION = 0.2f

    /**
     * Touch region in screen pixels (priority: manual > cutout > default), expanded by [paddingPx]
     * on every side; the top never goes above the screen. `null` when rotation != ROTATION_0 (FR-R4) or the result is empty.
     */
    fun resolve(
        screenWidthPx: Int,
        statusBarHeightPx: Int,
        cutout: Cutout?,
        manual: ManualRegion?,
        paddingPx: Int,
        rotation: Int,
    ): Rect? {
        if (rotation != 0) return null
        val cutoutRects = cutout?.boundingRectsTop.orEmpty()
        val base = when {
            manual != null -> {
                val left = manual.centerXPx.coerceIn(0, screenWidthPx) - manual.widthPx / 2
                Rect(left, manual.topPx, left + manual.widthPx, manual.topPx + manual.heightPx)
            }
            cutoutRects.isNotEmpty() -> cutoutRects.reduce { a, b ->
                Rect(minOf(a.left, b.left), 0, maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
            }
            else -> {
                val width = (screenWidthPx * DEFAULT_WIDTH_FRACTION).roundToInt()
                val left = (screenWidthPx - width) / 2
                Rect(left, 0, left + width, statusBarHeightPx)
            }
        }
        val result = Rect(
            left = (base.left - paddingPx).coerceIn(0, screenWidthPx),
            top = (base.top - paddingPx).coerceAtLeast(0),
            right = (base.right + paddingPx).coerceIn(0, screenWidthPx),
            bottom = base.bottom + paddingPx,
        )
        return result.takeIf { it.width > 0 && it.height > 0 }
    }
}
