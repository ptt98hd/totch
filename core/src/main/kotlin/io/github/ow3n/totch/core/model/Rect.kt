package io.github.ow3n.totch.core.model

/** Integer screen rectangle in pixels; [right] and [bottom] are exclusive. */
data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun contains(x: Float, y: Float): Boolean = x >= left && x < right && y >= top && y < bottom

    /** Shrinks every edge by [by] pixels; a negative value expands. */
    fun inset(by: Int): Rect = Rect(left + by, top + by, right - by, bottom - by)
}
