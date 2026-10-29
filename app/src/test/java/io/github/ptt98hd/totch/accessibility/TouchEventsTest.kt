package io.github.ptt98hd.totch.accessibility

import android.view.MotionEvent
import io.github.ptt98hd.totch.core.model.TouchAction
import io.github.ptt98hd.totch.core.model.TouchEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TouchEventsTest {
    @Test
    fun `DM-44 single-pointer actions map one to one`() {
        assertEquals(TouchEvent(TouchAction.DOWN, 10f, 20f, 100), touchEventOf(MotionEvent.ACTION_DOWN, 10f, 20f, 100, 1))
        assertEquals(TouchEvent(TouchAction.MOVE, 11f, 30f, 110), touchEventOf(MotionEvent.ACTION_MOVE, 11f, 30f, 110, 1))
        assertEquals(TouchEvent(TouchAction.UP, 11f, 40f, 120), touchEventOf(MotionEvent.ACTION_UP, 11f, 40f, 120, 1))
        assertEquals(TouchEvent(TouchAction.CANCEL, 0f, 0f, 130), touchEventOf(MotionEvent.ACTION_CANCEL, 0f, 0f, 130, 1))
    }

    @Test
    fun `DM-44 a second finger is a move with its pointer count`() {
        assertEquals(
            TouchEvent(TouchAction.MOVE, 10f, 20f, 100, pointerCount = 2),
            touchEventOf(MotionEvent.ACTION_POINTER_DOWN, 10f, 20f, 100, 2),
        )
        assertEquals(
            TouchEvent(TouchAction.MOVE, 10f, 20f, 105, pointerCount = 2),
            touchEventOf(MotionEvent.ACTION_POINTER_UP, 10f, 20f, 105, 2),
        )
    }

    @Test
    fun `DM-44 other actions are ignored`() {
        assertNull(touchEventOf(MotionEvent.ACTION_HOVER_MOVE, 1f, 1f, 1, 1))
        assertNull(touchEventOf(MotionEvent.ACTION_OUTSIDE, 1f, 1f, 1, 1))
    }
}
