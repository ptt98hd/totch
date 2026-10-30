package io.github.ptt98hd.totch.core.overlay

import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TouchEffectTest {
    @Test fun `TE-1 effect per gesture`() {
        val expected = mapOf(
            Gesture.TAP to TouchEffect.Pulse,
            Gesture.DOUBLE_TAP to TouchEffect.DoublePulse,
            Gesture.LONG_PRESS to TouchEffect.Glow,
            Gesture.SWIPE_DOWN to TouchEffect.Streak(0, 1),
            Gesture.SWIPE_DOWN_HOLD to TouchEffect.Streak(0, 1),
            Gesture.SWIPE_LEFT to TouchEffect.Streak(-1, 0),
            Gesture.SWIPE_LEFT_HOLD to TouchEffect.Streak(-1, 0),
            Gesture.SWIPE_RIGHT to TouchEffect.Streak(1, 0),
            Gesture.SWIPE_RIGHT_HOLD to TouchEffect.Streak(1, 0),
        )
        assertEquals(Gesture.entries.toSet(), expected.keys)
        expected.forEach { (g, e) -> assertEquals(e, touchEffectFor(g), g.name) }
    }

    @Test fun `TE-2 press progress`() {
        assertEquals(0f, pressProgress(100, 50, 300))
        assertEquals(0.5f, pressProgress(100, 250, 300))
        assertEquals(1f, pressProgress(100, 1_000, 300))
        assertEquals(1f, pressProgress(100, 100, 0))
    }
}

class OverlayVisibilityTest {
    private val on = Settings(enabled = true, notificationIndicator = true)

    private fun visible(s: Settings = on, a11y: Boolean = true, rotation: Int = 0, statusBar: Boolean = true) =
        overlayVisible(s, a11y, rotation, statusBar)

    @Test fun `OV-1 Totch switched off`() = assertFalse(visible(on.copy(enabled = false)))

    @Test fun `OV-2 no accessibility`() = assertFalse(visible(a11y = false))

    @Test fun `OV-3 rotated`() = assertFalse(visible(rotation = 1))

    @Test fun `OV-4 full screen app`() = assertFalse(visible(statusBar = false))

    @Test fun `OV-5 any feature on`() {
        assertTrue(visible())
        assertTrue(visible(Settings(enabled = true, island = true)))
        assertTrue(visible(Settings(enabled = true, touchEffects = true)))
        assertFalse(visible(Settings(enabled = true)))
    }
}
