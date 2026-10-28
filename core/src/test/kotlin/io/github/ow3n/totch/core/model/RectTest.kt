package io.github.ow3n.totch.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RectTest {
    @Test fun `contains is half open`() {
        val r = Rect(10, 0, 20, 5)
        assertTrue(r.contains(10f, 0f))
        assertTrue(r.contains(19.9f, 4.9f))
        assertFalse(r.contains(20f, 2f))
        assertFalse(r.contains(15f, 5f))
        assertFalse(r.contains(9.9f, 2f))
    }

    @Test fun `size and inset`() {
        val r = Rect(10, 0, 20, 5)
        assertEquals(10, r.width)
        assertEquals(5, r.height)
        assertEquals(Rect(12, 2, 18, 3), r.inset(2))
        assertEquals(Rect(8, -2, 22, 7), r.inset(-2))
    }
}
