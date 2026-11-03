package io.github.ptt98hd.totch.ui.common

import io.github.ptt98hd.totch.core.model.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class NotchOutlineTest {
    @Test fun `UI-84 the notch outline lies outside the notch, the touch outline inside the touch area`() {
        assertEquals(Outline(98f, -2f, 142f, 42f, 22f), outlineOutside(Rect(100, 0, 140, 40), 4f))
        assertEquals(Outline(92f, 2f, 148f, 48f, 23f), outlineInside(Rect(90, 0, 150, 50), 4f))
        assertEquals(Outline(4f, 4f, 4f, 4f, 0f), outlineInside(Rect(0, 0, 2, 2), 8f))
    }
}
