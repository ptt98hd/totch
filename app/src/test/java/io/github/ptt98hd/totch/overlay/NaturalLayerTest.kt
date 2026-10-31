package io.github.ptt98hd.totch.overlay

import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.engine.fromNatural
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class NaturalLayerTest {
    @Test
    fun `RO-1 layer per rotation`() {
        assertEquals(NaturalLayer(0f, 0, 0), naturalLayer(0, windowW = 1080, windowH = 400))
        assertEquals(NaturalLayer(270f, 0, 1080), naturalLayer(1, windowW = 400, windowH = 1080))
        assertEquals(NaturalLayer(180f, 1080, 400), naturalLayer(2, windowW = 1080, windowH = 400))
        assertEquals(NaturalLayer(90f, 400, 0), naturalLayer(3, windowW = 400, windowH = 1080))
    }

    @Test
    fun `RO-1 the layer draws the natural window where fromNatural puts it`() {
        val naturalW = 1080
        val naturalH = 2400
        val frameH = 400
        listOf(0, 1, 2, 3).forEach { rotation ->
            val (w, h) = if (rotation % 2 == 0) naturalW to naturalH else naturalH to naturalW
            val window = fromNatural(Rect(0, 0, naturalW, frameH), rotation, w, h)
            val layer = naturalLayer(rotation, window.width, window.height)
            val notch = Rect(500, 0, 580, 80)
            val expected = fromNatural(notch, rotation, w, h)
            val a = layer.map(notch.left.toFloat(), notch.top.toFloat())
            val b = layer.map(notch.right.toFloat(), notch.bottom.toFloat())
            val drawn = Rect(
                minOf(a.first, b.first).roundToInt() + window.left,
                minOf(a.second, b.second).roundToInt() + window.top,
                maxOf(a.first, b.first).roundToInt() + window.left,
                maxOf(a.second, b.second).roundToInt() + window.top,
            )
            assertEquals(expected, drawn, "rotation $rotation")
        }
    }
}
