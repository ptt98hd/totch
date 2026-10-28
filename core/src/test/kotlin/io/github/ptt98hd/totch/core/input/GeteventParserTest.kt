package io.github.ptt98hd.totch.core.input

import io.github.ptt98hd.totch.core.model.TouchAction
import io.github.ptt98hd.totch.core.model.TouchEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeteventParserTest {
    // Range == screen size so mapping is identity for in-range values.
    private val mapper = CoordinateMapper(
        InputDeviceInfo("/dev/input/event3", "fts_ts", AbsRange(0, 1080), AbsRange(0, 2340), true), 1080, 2340,
    )
    private val parser = GeteventParser(mapper)
    private val ts = "[   12345.678901]"
    private val t = 12345678L

    private fun abs(code: String, hex: String, prefix: String = "") = "$ts ${prefix}EV_ABS       $code    $hex"
    private val syn = "$ts EV_SYN       SYN_REPORT           00000000"

    private fun frame(vararg lines: String): List<TouchEvent> {
        lines.forEach { assertEquals(emptyList(), parser.feed(it), "non-SYN line must not emit: $it") }
        return parser.feed(syn)
    }

    private fun ev(a: TouchAction, x: Int, y: Int, pc: Int = 1): TouchEvent {
        val (mx, my) = mapper.map(x, y)
        return TouchEvent(a, mx, my, t, pc)
    }

    private fun touchDown() = frame(
        abs("ABS_MT_TRACKING_ID", "00000045"),
        abs("ABS_MT_POSITION_X", "000001c2"),
        abs("ABS_MT_POSITION_Y", "00000020"),
    )

    @Test fun `GP-1 first contact emits DOWN`() {
        assertEquals(listOf(ev(TouchAction.DOWN, 450, 32)), touchDown())
    }

    @Test fun `GP-2 changing X emits MOVE keeping Y`() {
        touchDown()
        assertEquals(listOf(ev(TouchAction.MOVE, 456, 32)), frame(abs("ABS_MT_POSITION_X", "000001c8")))
    }

    @Test fun `GP-3 tracking id -1 emits UP at last position`() {
        touchDown()
        frame(abs("ABS_MT_POSITION_X", "000001c8"))
        assertEquals(listOf(ev(TouchAction.UP, 456, 32)), frame(abs("ABS_MT_TRACKING_ID", "ffffffff")))
    }

    @Test fun `GP-4 unchanged frame emits nothing`() {
        touchDown()
        assertEquals(emptyList(), frame())
        assertEquals(emptyList(), frame(abs("ABS_MT_POSITION_X", "000001c2")))
        assertEquals(emptyList(), frame(abs("ABS_MT_PRESSURE", "00000040")))
    }

    @Test fun `GP-5 second slot emits MOVE with two pointers`() {
        touchDown()
        val out = frame(
            abs("ABS_MT_SLOT", "00000001"),
            abs("ABS_MT_TRACKING_ID", "00000046"),
            abs("ABS_MT_POSITION_X", "00000300"),
            abs("ABS_MT_POSITION_Y", "00000300"),
        )
        assertEquals(listOf(ev(TouchAction.MOVE, 450, 32, pc = 2)), out)
    }

    @Test fun `GP-6 lifting all slots emits exactly one UP with one pointer`() {
        touchDown()
        frame(
            abs("ABS_MT_SLOT", "00000001"),
            abs("ABS_MT_TRACKING_ID", "00000046"),
            abs("ABS_MT_POSITION_X", "00000300"),
            abs("ABS_MT_POSITION_Y", "00000300"),
        )
        val out = frame(
            abs("ABS_MT_SLOT", "00000000"),
            abs("ABS_MT_TRACKING_ID", "ffffffff"),
            abs("ABS_MT_SLOT", "00000001"),
            abs("ABS_MT_TRACKING_ID", "ffffffff"),
        )
        assertEquals(listOf(ev(TouchAction.UP, 450, 32, pc = 1)), out)
    }

    @Test fun `GP-7 irrelevant and garbage lines are ignored`() {
        val junk = listOf(
            "$ts EV_KEY       BTN_TOUCH            DOWN",
            "$ts EV_KEY       BTN_TOUCH            UP",
            abs("ABS_MT_PRESSURE", "00000040"),
            abs("ABS_MT_TOUCH_MAJOR", "00000004"),
            "garbage line ][ zzz",
            "",
            "   ",
            "[ 1.2] EV_ABS ABS_MT_POSITION_X nothex",
            "add device 2: /dev/input/event3",
        )
        junk.forEach { assertTrue(parser.feed(it).isEmpty(), it) }
        assertEquals(listOf(ev(TouchAction.DOWN, 450, 32)), touchDown())
    }

    @Test fun `GP-8 lines with device path prefix are supported`() {
        val p = "/dev/input/event3: "
        listOf(
            abs("ABS_MT_TRACKING_ID", "00000045", p),
            abs("ABS_MT_POSITION_X", "000001c2", p),
            abs("ABS_MT_POSITION_Y", "00000020", p),
        ).forEach { assertEquals(emptyList(), parser.feed(it)) }
        assertEquals(listOf(ev(TouchAction.DOWN, 450, 32)), parser.feed("$ts ${p}EV_SYN       SYN_REPORT           00000000"))
    }

    @Test fun `GP-9 timestamp is converted to milliseconds`() {
        assertEquals(12345678L, touchDown().single().timeMs)
    }
}
