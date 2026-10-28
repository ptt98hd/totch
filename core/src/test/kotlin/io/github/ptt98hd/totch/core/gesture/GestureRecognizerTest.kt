package io.github.ptt98hd.totch.core.gesture

import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.model.Gesture.DOUBLE_TAP
import io.github.ptt98hd.totch.core.model.Gesture.LONG_PRESS
import io.github.ptt98hd.totch.core.model.Gesture.SWIPE_DOWN
import io.github.ptt98hd.totch.core.model.Gesture.SWIPE_LEFT
import io.github.ptt98hd.totch.core.model.Gesture.SWIPE_RIGHT
import io.github.ptt98hd.totch.core.model.Gesture.TAP
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.model.TouchAction
import io.github.ptt98hd.totch.core.model.TouchEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GestureRecognizerTest {
    private val notch = Rect(400, 0, 680, 100)
    private val none = emptyList<Gesture>()

    private fun recognizer(config: GestureConfig = GestureConfig(), region: Rect? = notch) =
        GestureRecognizer(config) { region }

    private fun ev(a: TouchAction, x: Number, y: Number, t: Long, pc: Int = 1) =
        TouchEvent(a, x.toFloat(), y.toFloat(), t, pc)

    private fun GestureRecognizer.down(x: Number, y: Number, t: Long, pc: Int = 1) = onTouch(ev(TouchAction.DOWN, x, y, t, pc))
    private fun GestureRecognizer.move(x: Number, y: Number, t: Long, pc: Int = 1) = onTouch(ev(TouchAction.MOVE, x, y, t, pc))
    private fun GestureRecognizer.up(x: Number, y: Number, t: Long) = onTouch(ev(TouchAction.UP, x, y, t))
    private fun GestureRecognizer.cancel(t: Long) = onTouch(ev(TouchAction.CANCEL, 0, 0, t))

    @Test fun `GR-1 single tap is emitted after double tap timeout`() {
        val r = recognizer()
        assertEquals(none, r.down(540, 50, 0))
        assertEquals(none, r.up(540, 50, 100))
        assertEquals(none, r.onTick(399))
        assertEquals(listOf(TAP), r.onTick(400))
        assertEquals(none, r.onTick(1000))
    }

    @Test fun `GR-2 tap is emitted on UP when double tap disabled`() {
        val r = recognizer(GestureConfig(doubleTapEnabled = false))
        assertEquals(none, r.down(540, 50, 0))
        assertEquals(listOf(TAP), r.up(540, 50, 100))
        assertNull(r.nextDeadlineMs())
        assertEquals(none, r.onTick(1000))
    }

    @Test fun `GR-3 two quick taps emit only DOUBLE_TAP`() {
        val r = recognizer()
        assertEquals(none, r.down(540, 50, 0))
        assertEquals(none, r.up(540, 50, 80))
        assertEquals(none, r.down(540, 50, 200))
        assertEquals(listOf(DOUBLE_TAP), r.up(540, 50, 260))
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-4 late second down flushes TAP first and starts a new gesture`() {
        val r = recognizer()
        r.down(540, 50, 0)
        r.up(540, 50, 80)
        assertEquals(listOf(TAP), r.down(540, 50, 381))
        assertEquals(none, r.up(540, 50, 450))
        assertEquals(listOf(TAP), r.onTick(750))
    }

    @Test fun `GR-5 holding just below threshold emits nothing`() {
        val r = recognizer()
        r.down(540, 50, 0)
        assertEquals(none, r.onTick(499))
    }

    @Test fun `GR-6 long press emitted at threshold and UP emits nothing`() {
        val r = recognizer()
        r.down(540, 50, 0)
        assertEquals(listOf(LONG_PRESS), r.onTick(500))
        assertEquals(none, r.up(540, 50, 700))
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-7 long press emitted on late UP without tick`() {
        val r = recognizer()
        r.down(540, 50, 0)
        assertEquals(listOf(LONG_PRESS), r.up(540, 50, 600))
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-8 swipe down`() {
        val r = recognizer()
        r.down(540, 20, 0)
        assertEquals(none, r.move(545, 50, 50))
        assertEquals(listOf(SWIPE_DOWN), r.up(545, 90, 100))
    }

    @Test fun `GR-9 swipe left`() {
        val r = recognizer()
        r.down(540, 50, 0)
        assertEquals(none, r.move(500, 52, 50))
        assertEquals(listOf(SWIPE_LEFT), r.up(470, 55, 100))
    }

    @Test fun `GR-10 swipe right`() {
        val r = recognizer()
        r.down(540, 50, 0)
        assertEquals(none, r.move(580, 48, 50))
        assertEquals(listOf(SWIPE_RIGHT), r.up(610, 45, 100))
    }

    @Test fun `GR-11 movement beyond slop but below swipe distance emits nothing`() {
        val r = recognizer()
        r.down(540, 50, 0)
        assertEquals(none, r.move(570, 50, 50))
        assertEquals(none, r.up(570, 50, 100))
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-12 swipe up is not supported`() {
        val r = recognizer()
        r.down(540, 90, 0)
        assertEquals(none, r.move(540, 10, 50))
        assertEquals(none, r.up(540, 10, 100))
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-13 movement cancels long press`() {
        val r = recognizer()
        r.down(540, 20, 0)
        assertEquals(none, r.move(545, 60, 100))
        assertEquals(none, r.onTick(800))
        assertEquals(listOf(SWIPE_DOWN), r.up(545, 90, 800))
    }

    @Test fun `GR-14 long second press emits TAP then LONG_PRESS`() {
        val r = recognizer()
        r.down(540, 50, 0)
        r.up(540, 50, 80)
        assertEquals(none, r.down(540, 50, 200))
        assertEquals(none, r.onTick(699))
        assertEquals(listOf(TAP, LONG_PRESS), r.onTick(700))
        assertEquals(none, r.up(540, 50, 900))
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-15 second press turning into swipe emits TAP then swipe`() {
        val r = recognizer()
        r.down(540, 50, 0)
        r.up(540, 50, 80)
        assertEquals(none, r.down(540, 20, 200))
        assertEquals(listOf(TAP), r.move(540, 60, 250))
        assertEquals(listOf(SWIPE_DOWN), r.up(540, 90, 300))
    }

    @Test fun `GR-16 touch starting outside region is ignored`() {
        val r = recognizer()
        assertEquals(none, r.down(100, 50, 0))
        assertEquals(none, r.move(540, 50, 50))
        assertEquals(none, r.up(540, 50, 100))
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-17 down outside region while waiting flushes TAP`() {
        val r = recognizer()
        r.down(540, 50, 0)
        r.up(540, 50, 80)
        assertEquals(listOf(TAP), r.down(100, 50, 150))
        assertEquals(none, r.up(100, 50, 200))
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-18 multi touch cancels until end of stroke`() {
        val r = recognizer()
        r.down(540, 50, 0)
        assertEquals(none, r.move(541, 50, 50, pc = 2))
        assertEquals(none, r.move(540, 50, 60))
        assertEquals(none, r.onTick(600))
        assertEquals(none, r.up(540, 50, 700))
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-19 cancel returns to idle silently`() {
        val r = recognizer()
        r.down(540, 50, 0)
        assertEquals(none, r.cancel(50))
        assertNull(r.nextDeadlineMs())
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-20 swipe may end outside region`() {
        val r = recognizer()
        r.down(540, 50, 0)
        r.move(540, 200, 50)
        assertEquals(listOf(SWIPE_DOWN), r.up(540, 300, 100))
    }

    @Test fun `GR-21 null region ignores everything`() {
        val r = recognizer(region = null)
        assertEquals(none, r.down(540, 50, 0))
        assertEquals(none, r.onTick(600))
        assertEquals(none, r.up(540, 50, 700))
        assertEquals(none, r.down(540, 20, 800))
        assertEquals(none, r.move(540, 90, 850))
        assertEquals(none, r.up(540, 90, 900))
        assertEquals(none, r.onTick(5000))
        assertNull(r.nextDeadlineMs())
    }

    @Test fun `GR-22 next deadline depends on state`() {
        val r = recognizer()
        assertNull(r.nextDeadlineMs())
        r.down(540, 50, 0)
        assertEquals(500, r.nextDeadlineMs())
        r.up(540, 50, 80)
        assertEquals(380, r.nextDeadlineMs())
        r.onTick(380)
        assertNull(r.nextDeadlineMs())
        r.down(540, 20, 1000)
        r.move(540, 80, 1050)
        assertNull(r.nextDeadlineMs())
        r.up(540, 90, 1100)
        r.down(540, 50, 2000)
        r.onTick(2500)
        assertNull(r.nextDeadlineMs())
    }

    @Test fun `GR-23 config change applies at runtime`() {
        val r = recognizer()
        r.config = r.config.copy(longPressMs = 800)
        r.down(540, 50, 0)
        assertEquals(none, r.onTick(600))
        assertEquals(listOf(LONG_PRESS), r.onTick(800))
    }

    @Test fun `GR-24 reset drops pending tap`() {
        val r = recognizer()
        r.down(540, 50, 0)
        r.up(540, 50, 80)
        r.reset()
        assertNull(r.nextDeadlineMs())
        assertEquals(none, r.onTick(5000))
        r.down(540, 50, 6000)
        r.reset()
        assertEquals(none, r.up(540, 50, 6050))
        assertEquals(none, r.onTick(9000))
    }

    @Test fun `GR-25 MOVE and UP without DOWN are ignored`() {
        val r = recognizer()
        assertEquals(none, r.move(540, 50, 0))
        assertEquals(none, r.up(540, 50, 10))
        assertNull(r.nextDeadlineMs())
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-26 DOWN while pressed restarts the stroke`() {
        val r = recognizer()
        r.down(540, 50, 0)
        assertEquals(none, r.down(540, 50, 100))
        assertEquals(600, r.nextDeadlineMs())
        assertEquals(none, r.onTick(550))
        assertEquals(none, r.up(540, 50, 150))
        assertEquals(listOf(TAP), r.onTick(450))
    }

    @Test fun `GR-27 aborting the second press still emits the first TAP`() {
        val cancel = recognizer()
        cancel.down(540, 50, 0); cancel.up(540, 50, 80); cancel.down(540, 50, 200)
        assertEquals(listOf(TAP), cancel.cancel(250))
        assertEquals(none, cancel.onTick(5000))

        val multi = recognizer()
        multi.down(540, 50, 0); multi.up(540, 50, 80); multi.down(540, 50, 200)
        assertEquals(listOf(TAP), multi.move(540, 50, 250, pc = 2))
        assertEquals(none, multi.up(540, 50, 300))
        assertEquals(none, multi.onTick(5000))
    }

    @Test fun `GR-28 UP far from DOWN without MOVE is treated as a swipe`() {
        val r = recognizer()
        r.down(540, 20, 0)
        assertEquals(listOf(SWIPE_DOWN), r.up(540, 90, 100))
        r.down(540, 50, 1000)
        assertEquals(none, r.up(570, 50, 1100))
        assertEquals(none, r.onTick(5000))
    }

    @Test fun `GR-29 second DOWN exactly at doubleTapMs is a new gesture`() {
        val r = recognizer()
        r.down(540, 50, 0)
        r.up(540, 50, 80)
        assertEquals(listOf(TAP), r.down(540, 50, 380))
        assertEquals(none, r.up(540, 50, 400))
        assertEquals(listOf(TAP), r.onTick(700))
    }
}
