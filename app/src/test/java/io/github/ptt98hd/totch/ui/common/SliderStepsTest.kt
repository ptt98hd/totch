package io.github.ptt98hd.totch.ui.common

import org.junit.Test
import kotlin.test.assertEquals

class SliderStepsTest {
    @Test
    fun `UI-35 dragging snaps to multiples of the step, kept inside the range`() {
        assertEquals(300f, snapToStep(320f, 100f..1000f, 50f))
        assertEquals(350f, snapToStep(330f, 100f..1000f, 50f))
        assertEquals(100f, snapToStep(3f, 100f..1000f, 50f))
        assertEquals(400f, snapToStep(9999f, 10f..400f, 5f))
        assertEquals(15f, snapToStep(13f, 10f..100f, 5f))
        // Padding 1–10 with step 5: the ends stay reachable.
        assertEquals(1f, snapToStep(1.4f, 1f..10f, 5f))
        assertEquals(5f, snapToStep(4f, 1f..10f, 5f))
        assertEquals(10f, snapToStep(9f, 1f..10f, 5f))
    }

    @Test
    fun `UI-36 the buttons move by one increment, kept inside the range`() {
        assertEquals(310f, stepBy(300f, 10f, 100f..1000f))
        assertEquals(290f, stepBy(300f, -10f, 100f..1000f))
        assertEquals(1000f, stepBy(995f, 10f, 100f..1000f))
        assertEquals(1f, stepBy(1f, -1f, 1f..10f))
        // An off-step value (e.g. a detected width of 87 dp) moves by exactly one unit.
        assertEquals(88f, stepBy(87f, 1f, 10f..400f))
    }
}
