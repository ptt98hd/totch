package io.github.ptt98hd.totch.engine

import io.github.ptt98hd.totch.core.action.DispatchResult
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.model.TouchAction
import io.github.ptt98hd.totch.core.model.TouchEvent
import io.github.ptt98hd.totch.core.settings.DefaultBindings
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GestureEngineTest {
    private val region = Rect(400, 0, 600, 80)
    private val dispatched = mutableListOf<Action>()
    private var result: DispatchResult = DispatchResult.Ok
    private var haptics = 0
    private val failures = mutableListOf<Action>()

    private val feedback = object : EngineFeedback {
        override fun onPerformed() { haptics++ }
        override fun onFailed(action: Action, reason: String) { failures += action }
    }

    private fun TestScope.engine(
        settings: Settings = Settings(enabled = true),
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
        copy(bindings = DefaultBindings + pairs.toMap())

    private fun down(t: Long, x: Float = 500f, y: Float = 40f) = TouchEvent(TouchAction.DOWN, x, y, t)
    private fun up(t: Long, x: Float = 500f, y: Float = 40f) = TouchEvent(TouchAction.UP, x, y, t)

    @Test
    fun `EN-1 TAP bound to Home dispatches Home`() = runTest {
        val e = engine(Settings(enabled = true).bind(Gesture.TAP to Action.Home, Gesture.DOUBLE_TAP to Action.None))
        e.onTouch(down(0))
        e.onTouch(up(50))
        runCurrent()
        assertEquals(listOf<Action>(Action.Home), dispatched)
        assertEquals(1, haptics)
    }

    @Test
    fun `EN-1 TAP is dispatched after the double-tap timeout when DOUBLE_TAP is bound`() = runTest {
        val e = engine(Settings(enabled = true).bind(Gesture.TAP to Action.Home))
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
        val e = engine(Settings(enabled = true).bind(Gesture.TAP to Action.None, Gesture.DOUBLE_TAP to Action.None))
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
        e.updateSettings(Settings(enabled = true).bind(Gesture.DOUBLE_TAP to Action.None))
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
    fun `EN-6 failed dispatch reports failure and does not vibrate`() = runTest {
        result = DispatchResult.Failed("unavailable")
        val e = engine()
        e.onTouch(down(0))
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf<Action>(Action.ToggleTorch), failures)
        assertEquals(0, haptics)
    }

    @Test
    fun `EN-6 no haptics when disabled in settings`() = runTest {
        val e = engine(Settings(enabled = true, haptics = false))
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
        val e = engine(Settings(enabled = true).bind(Gesture.TAP to Action.Home, Gesture.DOUBLE_TAP to Action.None))
        e.onTouch(down(0))
        e.onTouch(up(10))
        e.onTouch(down(20))
        e.onTouch(TouchEvent(TouchAction.MOVE, 500f, 200f, 30))
        e.onTouch(up(40, y = 200f))
        runCurrent()
        assertEquals(listOf(Action.Home, Action.Notifications), dispatched)
    }

    private fun move(t: Long, x: Float, y: Float = 40f) = TouchEvent(TouchAction.MOVE, x, y, t)

    @Test
    fun `EN-9 swipe down is not dispatched in Shizuku mode`() = runTest {
        val e = engine(Settings(enabled = true, mode = DetectionMode.SHIZUKU).bind(Gesture.SWIPE_DOWN to Action.Notifications))
        e.onTouch(down(0, y = 10f))
        e.onTouch(move(50, 500f, 200f))
        e.onTouch(up(100, 500f, 200f))
        advanceTimeBy(1000)
        runCurrent()
        assertTrue(dispatched.isEmpty())
        assertEquals(0, haptics)
    }

    @Test
    fun `EN-10 swipe right and hold repeats until release`() = runTest {
        val e = engine(
            Settings(enabled = true, repeatOnHold = setOf(Gesture.SWIPE_RIGHT_HOLD), repeatIntervalMs = 300)
                .bind(Gesture.SWIPE_RIGHT_HOLD to Action.VolumeUp),
        )
        e.onTouch(down(0, x = 420f))
        e.onTouch(move(50, 560f))
        advanceTimeBy(50 + 500 + 2 * 300)
        runCurrent()
        assertEquals(List<Action>(3) { Action.VolumeUp }, dispatched)
        assertEquals(3, haptics)
        e.onTouch(up(testScheduler.currentTime, 560f))
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(3, dispatched.size)
    }
}
