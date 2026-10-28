package io.github.ptt98hd.totch.ui.common

import org.junit.Test
import kotlin.test.assertEquals

class SliderStepsTest {
    @Test
    fun `UI-35 values snap to multiples of the step from the start of the range`() {
        assertEquals(300f, snapToStep(320f, 150f..600f, 50f))
        assertEquals(350f, snapToStep(330f, 150f..600f, 50f))
        assertEquals(20f, snapToStep(3f, 20f..200f, 10f))
        assertEquals(400f, snapToStep(9999f, 10f..400f, 10f))
        assertEquals(15f, snapToStep(13f, 10f..100f, 5f))
    }

    @Test
    fun `UI-35 tick marks only when the scale has at most 20 steps`() {
        assertEquals(8, tickCount(150f..600f, 50f))
        assertEquals(19, tickCount(0f..100f, 5f))
        assertEquals(0, tickCount(10f..400f, 10f))
        assertEquals(11, tickCount(0f..48f, 4f))
    }
}
