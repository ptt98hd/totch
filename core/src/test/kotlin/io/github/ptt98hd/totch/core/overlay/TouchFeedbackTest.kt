package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.overlay.TouchFeedbackController.Companion.RESULT_MS
import io.github.ptt98hd.totch.core.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TouchFeedbackTest {
    private val fb = TouchFeedbackController()

    @Test fun `TF-1 starts idle`() {
        assertEquals(TouchFeedback.Idle, fb.state)
        assertNull(fb.nextDeadlineMs())
    }

    @Test fun `TF-2 press then move`() {
        fb.onPress(10f, 20f, 100)
        assertEquals(TouchFeedback.Pressing(10f, 20f, 100, 10f, 20f), fb.state)
        fb.onMove(40f, 60f)
        assertEquals(TouchFeedback.Pressing(10f, 20f, 100, 40f, 60f), fb.state)
        assertNull(fb.nextDeadlineMs())
    }

    @Test fun `TF-2 move without a press is ignored`() {
        fb.onMove(40f, 60f)
        assertEquals(TouchFeedback.Idle, fb.state)
    }

    @Test fun `TF-3 release waits for a gesture, then goes idle`() {
        fb.onPress(10f, 20f, 100)
        fb.onRelease(200, waitMs = 400)
        assertEquals(TouchFeedback.Waiting, fb.state)
        assertEquals(600, fb.nextDeadlineMs())
        fb.onTick(599)
        assertEquals(TouchFeedback.Waiting, fb.state)
        fb.onTick(600)
        assertEquals(TouchFeedback.Idle, fb.state)
        assertNull(fb.nextDeadlineMs())
        fb.onRelease(700, waitMs = 400)
        assertEquals(TouchFeedback.Idle, fb.state)
    }

    @Test fun `TF-4 a gesture shows its result for a while`() {
        fb.onPress(10f, 20f, 100)
        fb.onRelease(200, waitMs = 400)
        fb.onGesture(Gesture.TAP, Action.Home, 500)
        assertEquals(TouchFeedback.Result(Gesture.TAP, Action.Home), fb.state)
        assertEquals(500 + RESULT_MS, fb.nextDeadlineMs())
        fb.onTick(500 + RESULT_MS)
        assertEquals(TouchFeedback.Idle, fb.state)
    }

    @Test fun `TF-4 a held gesture keeps its result after the finger moves or lifts`() {
        fb.onPress(10f, 20f, 100)
        fb.onGesture(Gesture.LONG_PRESS, Action.None, 400)
        fb.onMove(30f, 20f)
        fb.onRelease(600, waitMs = 400)
        assertEquals(TouchFeedback.Result(Gesture.LONG_PRESS, Action.None), fb.state)
        assertEquals(400 + RESULT_MS, fb.nextDeadlineMs())
    }

    @Test fun `TF-5 a new press replaces waiting or a result`() {
        fb.onPress(10f, 20f, 100)
        fb.onRelease(200, waitMs = 400)
        fb.onPress(11f, 21f, 300)
        assertEquals(TouchFeedback.Pressing(11f, 21f, 300, 11f, 21f), fb.state)
        assertNull(fb.nextDeadlineMs())
        fb.onGesture(Gesture.DOUBLE_TAP, Action.LockScreen, 350)
        fb.onPress(12f, 22f, 400)
        assertEquals(TouchFeedback.Pressing(12f, 22f, 400, 12f, 22f), fb.state)
        assertNull(fb.nextDeadlineMs())
    }

    @Test fun `TF-6 stretch follows the finger and the hold time`() {
        val p = TouchFeedback.Pressing(0f, 0f, 0, 30f, 40f)
        // A quick touch (< L/3) does not stretch by time.
        assertEquals(50f, pressStretch(p, nowMs = 100, longPressMs = 300, maxPx = 200f))
        // Halfway through the rest of the hold: (200 - 100) / 200 = 0.5 of max.
        assertEquals(100f, pressStretch(p.copy(x = 0f, y = 0f), nowMs = 200, longPressMs = 300, maxPx = 200f))
        assertEquals(200f, pressStretch(p.copy(x = 0f, y = 0f), nowMs = 900, longPressMs = 300, maxPx = 200f))
        assertEquals(200f, pressStretch(p.copy(x = 500f), nowMs = 0, longPressMs = 300, maxPx = 200f))
        assertEquals(200f, pressStretch(p.copy(x = 0f, y = 0f), nowMs = 0, longPressMs = 0, maxPx = 200f))
    }

    @Test fun `TF-7 stretched pill`() {
        val notch = Rect(500, 0, 580, 80)
        val m = IslandMetrics(screenWidthPx = 1080, density = 2f)
        assertEquals(80, IslandLayout.feedbackWing(notch, m)) // IG-20
        assertEquals(Rect(388, 0, 692, 80), IslandLayout.stretched(notch, m, 112))
        assertEquals(notch, IslandLayout.stretched(notch, m, 0))
        assertEquals(notch, IslandLayout.stretched(notch, m, -5))
        assertEquals(Rect(16, 0, 1064, 80), IslandLayout.stretched(notch, m, 900))
    }

    @Test fun `TE-2 press progress`() {
        assertEquals(0f, pressProgress(100, 50, 300))
        assertEquals(0.5f, pressProgress(100, 250, 300))
        assertEquals(1f, pressProgress(100, 1_000, 300))
        assertEquals(1f, pressProgress(100, 100, 0))
    }
}

class OverlayVisibilityTest {
    private val on = Settings(enabled = true, island = true)

    @Test fun `OV-1 Totch switched off`() = assertFalse(overlayVisible(on.copy(enabled = false), accessibility = true))

    @Test fun `OV-2 no accessibility`() = assertFalse(overlayVisible(on, accessibility = false))

    @Test fun `OV-4 full screen app hides island activities only`() {
        val media = IslandContent.Media(MediaInfo("com.music", "Song", null, playing = true), expanded = false)
        assertEquals(IslandContent.Hidden, islandShown(media, statusBarVisible = false))
        assertEquals(media, islandShown(media, statusBarVisible = true))
    }

    @Test fun `OV-5 any feature on`() {
        assertTrue(overlayVisible(on, accessibility = true))
        assertTrue(overlayVisible(Settings(enabled = true, touchEffects = true), accessibility = true))
        assertFalse(overlayVisible(Settings(enabled = true), accessibility = true))
    }
}
