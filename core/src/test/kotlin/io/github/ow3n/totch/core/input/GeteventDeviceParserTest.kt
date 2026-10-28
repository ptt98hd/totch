package io.github.ow3n.totch.core.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeteventDeviceParserTest {
    private val sample = """
        add device 1: /dev/input/event5
          name:     "gpio-keys"
          events:
            KEY (0001): KEY_VOLUMEDOWN        KEY_VOLUMEUP
          input props:
            <none>
        add device 2: /dev/input/event3
          name:     "fts_ts"
          events:
            KEY (0001): KEY_WAKEUP            BTN_TOUCH
            ABS (0003): ABS_MT_SLOT           : value 0, min 0, max 9, fuzz 0, flat 0, resolution 0
                        ABS_MT_TOUCH_MAJOR    : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
                        ABS_MT_POSITION_X     : value 0, min 0, max 1079, fuzz 0, flat 0, resolution 0
                        ABS_MT_POSITION_Y     : value 0, min 0, max 2339, fuzz 0, flat 0, resolution 0
                        ABS_MT_TRACKING_ID    : value 0, min 0, max 65535, fuzz 0, flat 0, resolution 0
          input props:
            INPUT_PROP_DIRECT
    """.trimIndent()

    private fun device(path: String, direct: Boolean) =
        InputDeviceInfo(path, path, AbsRange(0, 100), AbsRange(0, 100), direct)

    @Test fun `GD-1 parses the multitouch device from sample`() {
        assertEquals(
            listOf(InputDeviceInfo("/dev/input/event3", "fts_ts", AbsRange(0, 1079), AbsRange(0, 2339), direct = true)),
            GeteventDeviceParser.parse(sample),
        )
    }

    @Test fun `GD-2 device with only single touch axes is excluded`() {
        val text = """
            add device 1: /dev/input/event7
              name:     "mouse-ish"
              events:
                ABS (0003): ABS_X                 : value 0, min 0, max 1079, fuzz 0, flat 0, resolution 0
                            ABS_Y                 : value 0, min 0, max 2339, fuzz 0, flat 0, resolution 0
              input props:
                INPUT_PROP_DIRECT
        """.trimIndent()
        assertEquals(emptyList(), GeteventDeviceParser.parse(text))
    }

    @Test fun `GD-3 pickTouchscreen prefers direct then first then null`() {
        val a = device("/a", false)
        val b = device("/b", true)
        val c = device("/c", false)
        assertEquals(b, GeteventDeviceParser.pickTouchscreen(listOf(a, b, c)))
        assertEquals(a, GeteventDeviceParser.pickTouchscreen(listOf(a, c)))
        assertNull(GeteventDeviceParser.pickTouchscreen(emptyList()))
    }

    @Test fun `GD-4 empty or garbage input yields empty list`() {
        assertTrue(GeteventDeviceParser.parse("").isEmpty())
        assertTrue(GeteventDeviceParser.parse("garbage\n\u0000 :::: add device\nname: \"x").isEmpty())
        assertTrue(GeteventDeviceParser.parse("add device 1: /dev/input/event1\n ABS_MT_POSITION_X : value x, min ?, max").isEmpty())
    }

    @Test fun `GD-5 quoted name with spaces and special chars is kept without quotes`() {
        val text = sample.replace("\"fts_ts\"", "\"Goodix Capacitive TouchScreen (v2.0) #1\"")
        assertEquals("Goodix Capacitive TouchScreen (v2.0) #1", GeteventDeviceParser.parse(text).single().name)
    }
}
