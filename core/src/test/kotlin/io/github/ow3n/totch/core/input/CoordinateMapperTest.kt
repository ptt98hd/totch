package io.github.ow3n.totch.core.input

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoordinateMapperTest {
    private fun mapper(x: AbsRange, y: AbsRange = AbsRange(0, 2339)) =
        CoordinateMapper(InputDeviceInfo("/dev/input/event3", "ts", x, y, true), 1080, 2340)

    @Test fun `CM-1 native range maps to edges`() {
        val m = mapper(AbsRange(0, 1079))
        assertEquals(0f, m.map(0, 0).first)
        assertEquals(1079f, m.map(1079, 0).first)
    }

    @Test fun `CM-2 higher resolution range is scaled`() {
        val (x, _) = mapper(AbsRange(0, 4095)).map(2048, 0)
        assertTrue(abs(x - 540f) <= 1f, "x=$x")
    }

    @Test fun `CM-3 out of range raw values are clamped`() {
        val m = mapper(AbsRange(0, 1079))
        assertEquals(0f to 0f, m.map(-50, -10))
        assertEquals(1079f to 2339f, m.map(5000, 9000))
    }

    @Test fun `CM-4 degenerate range returns zero`() {
        val m = mapper(AbsRange(5, 5), AbsRange(7, 7))
        assertEquals(0f to 0f, m.map(5, 7))
        assertEquals(0f to 0f, m.map(100, 100))
    }
}
