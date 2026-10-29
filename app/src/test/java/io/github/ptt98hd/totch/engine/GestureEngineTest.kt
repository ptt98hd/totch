package io.github.ptt98hd.totch.engine

import io.github.ptt98hd.totch.core.action.DispatchResult
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.model.TouchAction
import io.github.ptt98hd.totch.core.model.TouchEvent
import io.github.ptt98hd.totch.ui.LegacyBindings
import io.github.ptt98hd.totch.core.settings.Settings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GestureEngineTest {
    /** Bound gestures and the 500 ms long press these tests were written for. */
    private val base = Settings(enabled = true, bindings = LegacyBindings, longPressMs = 500)
    private val region = Rect(400, 0, 600, 80)
    private val dispatched = mutableListOf<Action>()
    private var result: DispatchResult = DispatchResult.Ok
    private var haptics = 0
    private val failures = mutableListOf<Action>()

    private val feedback = object : EngineFeedback {
        override fun onRecognized() { haptics++ }
        override fun onFailed(action: Action, reason: String) { failures += action }
    }

    private fun TestScope.engine(
        settings: Settings = base,
        scope: CoroutineScope = backgroundScope,
        clock: () -> Long = { testScheduler.currentTime },
    ) = GestureEngine(
        scope = scope,
        clock = clock,
        density = 2f,
        dispatch = { dispatched += it; result },
        feedback = feedback,
    ).apply {
        updateSettings(settings)
        region = this@GestureEngineTest.region
    }

    private fun Settings.bind(vararg pairs: Pair<Gesture, Action>) =
        copy(bindings = bindings + pairs.toMap())

    private fun down(t: Long, x: Float = 500f, y: Float = 40f) = TouchEvent(TouchAction.DOWN, x, y, t)
    private fun up(t: Long, x: Float = 500f, y: Float = 40f) = TouchEvent(TouchAction.UP, x, y, t)

    @Test
    fun `EN-1 TAP bound to Home dispatches Home`() = runTest {
        val e = engine(base.bind(Gesture.TAP to Action.Home, Gesture.DOUBLE_TAP to Action.None))
        e.onTouch(down(0))
        e.onTouch(up(50))
        runCurrent()
        assertEquals(listOf<Action>(Action.Home), dispatched)
        assertEquals(1, haptics)
    }

    @Test
    fun `EN-1 TAP is dispatched after the double-tap timeout when DOUBLE_TAP is bound`() = runTest {
        val e = engine(base.bind(Gesture.TAP to Action.Home))
        e.onTouch(down(0))
        e.onTouch(up(50))
        advanceTimeBy(300)
        runCurrent()
        assertTrue(dispatched.isEmpty(), "must wait doubleTapMs (300) after UP")
        advanceTimeBy(50)
        runCurrent()
        assertEquals(listOf<Action>(Action.Home), dispatched)
    }

    @Test
    fun `EN-2 gesture bound to None does not dispatch nor vibrate`() = runTest {
        val e = engine(base.bind(Gesture.TAP to Action.None, Gesture.DOUBLE_TAP to Action.None))
        e.onTouch(down(0))
        e.onTouch(up(50))
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(dispatched.isEmpty())
        assertEquals(0, haptics)
    }

    @Test
    fun `EN-3 settings change updates recognizer config immediately`() = runTest {
        val e = engine()
        e.updateSettings(Settings(enabled = true, longPressMs = 1000, doubleTapMs = 400, swipeMinDistanceDp = 50))
        assertEquals(1000, e.config.longPressMs)
        assertEquals(400, e.config.doubleTapMs)
        assertEquals(100f, e.config.swipeMinDistancePx)
        e.updateSettings(base.bind(Gesture.DOUBLE_TAP to Action.None))
        assertEquals(false, e.config.doubleTapEnabled)
    }

    @Test
    fun `EN-4 LONG_PRESS fires after 500 ms of virtual time without a new event`() = runTest {
        val e = engine()
        e.onTouch(down(0))
        advanceTimeBy(499)
        runCurrent()
        assertTrue(dispatched.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf<Action>(Action.ToggleTorch), dispatched)
    }

    @Test
    fun `EN-4 a later event reschedules the single timeout`() = runTest {
        val e = engine()
        e.onTouch(down(0))
        advanceTimeBy(200)
        e.onTouch(up(200)) // tap ends: long press must not fire any more
        advanceTimeBy(10_000)
        runCurrent()
        assertTrue(Action.ToggleTorch !in dispatched)
        assertEquals(0, e.pendingTimers)
    }

    @Test
    fun `EN-5 event timestamps from another clock are re-based`() = runTest {
        val offset = 1_700_000_000_000L // e.g. CLOCK_REALTIME vs uptime
        val e = engine()
        e.onTouch(down(testScheduler.currentTime + offset))
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf<Action>(Action.ToggleTorch), dispatched)
    }

    @Test
    fun `EN-6 failed dispatch reports failure after vibrating`() = runTest {
        result = DispatchResult.Failed("unavailable")
        val e = engine()
        e.onTouch(down(0))
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf<Action>(Action.ToggleTorch), failures)
        assertEquals(1, haptics)
    }

    @Test
    fun `EN-6 no haptics when disabled in settings`() = runTest {
        val e = engine(base.copy(haptics = false))
        e.onTouch(down(0))
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf<Action>(Action.ToggleTorch), dispatched)
        assertEquals(0, haptics)
    }

    @Test
    fun `EN-7 touches outside the region or with no region are ignored`() = runTest {
        val e = engine()
        e.onTouch(down(0, x = 10f))
        advanceTimeBy(1_000)
        e.onTouch(up(1_000, x = 10f))
        e.region = null
        e.onTouch(down(2_000))
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(dispatched.isEmpty())
    }

    @Test
    fun `EN-8 gestures are dispatched in order`() = runTest {
        val e = engine(base.bind(Gesture.TAP to Action.Home, Gesture.DOUBLE_TAP to Action.None))
        e.onTouch(down(0))
        e.onTouch(up(10))
        e.onTouch(down(20))
        e.onTouch(TouchEvent(TouchAction.MOVE, 800f, 40f, 30))
        e.onTouch(up(40, x = 800f))
        runCurrent()
        assertEquals(listOf(Action.Home, Action.MediaNext), dispatched)
    }

    private fun move(t: Long, x: Float, y: Float = 40f) = TouchEvent(TouchAction.MOVE, x, y, t)

    @Test
    fun `EN-9 swipe down is never dispatched`() = runTest {
        val e = engine(base.bind(Gesture.SWIPE_DOWN to Action.Notifications))
        e.onTouch(down(0, y = 10f))
        e.onTouch(move(50, 500f, 200f))
        e.onTouch(up(100, 500f, 200f))
        advanceTimeBy(1000)
        runCurrent()
        assertTrue(dispatched.isEmpty())
        assertEquals(0, haptics)
    }

    @Test
    fun `EN-10 swipe right and hold fires once`() = runTest {
        val e = engine(base.bind(Gesture.SWIPE_RIGHT_HOLD to Action.VolumeUp))
        e.onTouch(down(0, x = 420f))
        e.onTouch(move(50, 560f))
        advanceTimeBy(50 + 500 + 2000)
        runCurrent()
        assertEquals(listOf<Action>(Action.VolumeUp), dispatched)
        assertEquals(1, haptics)
        e.onTouch(up(testScheduler.currentTime, 560f))
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(1, dispatched.size)
    }

    @Test
    fun `EN-11 a switched off gesture dispatches nothing`() = runTest {
        val e = engine(base.copy(disabledGestures = setOf(Gesture.SWIPE_RIGHT)))
        e.onTouch(down(0, x = 420f))
        e.onTouch(move(50, 560f))
        e.onTouch(up(100, 560f))
        advanceTimeBy(1000)
        runCurrent()
        assertTrue(dispatched.isEmpty())
    }

    @Test
    fun `EN-12 swipe down is dispatched while touches can be blocked`() = runTest {
        val e = engine(base.bind(Gesture.SWIPE_DOWN to Action.Notifications))
        e.canBlockTouches = true
        e.onTouch(down(0, y = 10f))
        e.onTouch(move(50, 500f, 200f))
        e.onTouch(up(100, 500f, 200f))
        runCurrent()
        assertEquals(listOf<Action>(Action.Notifications), dispatched)
        e.canBlockTouches = false
        e.onTouch(down(1000, y = 10f))
        e.onTouch(move(1050, 500f, 200f))
        e.onTouch(up(1100, 500f, 200f))
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, dispatched.size)
    }

    @Test
    fun `EN-13 vibrates on recognition without waiting for a slow dispatch`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val e = GestureEngine(
            scope = backgroundScope,
            clock = { testScheduler.currentTime },
            density = 2f,
            dispatch = { dispatched += it; gate.await(); DispatchResult.Ok },
            feedback = feedback,
        ).apply {
            updateSettings(base.bind(Gesture.TAP to Action.Home, Gesture.DOUBLE_TAP to Action.None))
            region = this@GestureEngineTest.region
        }
        e.onTouch(down(0))
        e.onTouch(up(50))
        runCurrent()
        assertEquals(listOf<Action>(Action.Home), dispatched)
        assertEquals(1, haptics, "vibrates while the action is still running")
        e.onTouch(down(100))
        e.onTouch(up(150))
        runCurrent()
        assertEquals(1, dispatched.size, "second action waits in the queue")
        assertEquals(2, haptics, "queued gesture vibrates immediately too")
        gate.complete(Unit)
        runCurrent()
        assertEquals(2, dispatched.size)
        assertEquals(2, haptics)
    }
}
