package io.github.ow3n.totch.core.input

/** Maps raw ABS values to natural-orientation (ROTATION_0) screen pixels, clamped to the screen. */
class CoordinateMapper(device: InputDeviceInfo, screenWidthPx: Int, screenHeightPx: Int) {
    private val xAxis = Axis(device.x, screenWidthPx)
    private val yAxis = Axis(device.y, screenHeightPx)

    fun map(rawX: Int, rawY: Int): Pair<Float, Float> = xAxis.map(rawX) to yAxis.map(rawY)

    private class Axis(private val range: AbsRange, private val size: Int) {
        fun map(raw: Int): Float {
            val span = range.max - range.min
            if (span == 0 || size <= 0) return 0f
            val px = (raw - range.min).toFloat() / span * size
            return px.coerceIn(0f, (size - 1).toFloat())
        }
    }
}
