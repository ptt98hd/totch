package io.github.ptt98hd.totch.core.settings

import io.github.ptt98hd.totch.core.gesture.GestureConfig
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsTest {
    @Test fun `ST-1 defaults match FR-S2`() {
        val s = Settings()
        assertEquals(
            mapOf(
                Gesture.TAP to Action.None,
                Gesture.DOUBLE_TAP to Action.LockScreen,
                Gesture.LONG_PRESS to Action.ToggleTorch,
                Gesture.SWIPE_DOWN to Action.Notifications,
                Gesture.SWIPE_LEFT to Action.MediaPrevious,
                Gesture.SWIPE_RIGHT to Action.MediaNext,
                Gesture.SWIPE_DOWN_HOLD to Action.None,
                Gesture.SWIPE_LEFT_HOLD to Action.None,
                Gesture.SWIPE_RIGHT_HOLD to Action.None,
            ),
            s.bindings,
        )
        assertFalse(s.enabled)
        assertEquals(DetectionMode.ACCESSIBILITY, s.mode)
        assertEquals(300, s.doubleTapMs)
        assertEquals(500, s.longPressMs)
        assertEquals(40, s.swipeMinDistanceDp)
        assertEquals(8, s.paddingDp)
        assertEquals(null, s.manualRegion)
        assertTrue(s.haptics)
        assertEquals(s, s.sanitized())
    }

    @Test fun `ST-2 sanitized clamps numeric values`() {
        val low = Settings(
            doubleTapMs = 1, longPressMs = 1, swipeMinDistanceDp = 1, paddingDp = -5,
            manualRegion = ManualRegionDp(centerXFraction = -0.5f, widthDp = 1, heightDp = 1),
        ).sanitized()
        assertEquals(150, low.doubleTapMs)
        assertEquals(300, low.longPressMs)
        assertEquals(16, low.swipeMinDistanceDp)
        assertEquals(0, low.paddingDp)
        assertEquals(ManualRegionDp(0f, 8, 8), low.manualRegion)

        val high = Settings(
            doubleTapMs = 9999, longPressMs = 9999, swipeMinDistanceDp = 9999, paddingDp = 9999,
            manualRegion = ManualRegionDp(centerXFraction = 7f, widthDp = 9999, heightDp = 9999),
        ).sanitized()
        assertEquals(600, high.doubleTapMs)
        assertEquals(1500, high.longPressMs)
        assertEquals(200, high.swipeMinDistanceDp)
        assertEquals(48, high.paddingDp)
        assertEquals(ManualRegionDp(1f, 400, 100), high.manualRegion)

        assertEquals(0.5f, Settings(manualRegion = ManualRegionDp(centerXFraction = Float.NaN)).sanitized().manualRegion?.centerXFraction)
    }

    @Test fun `ST-3 sanitized fills missing gestures with None`() {
        val s = Settings(bindings = mapOf(Gesture.LONG_PRESS to Action.Home)).sanitized()
        assertEquals(Gesture.entries.toSet(), s.bindings.keys)
        assertEquals(Action.Home, s.bindings[Gesture.LONG_PRESS])
        Gesture.entries.filter { it != Gesture.LONG_PRESS }.forEach { assertEquals(Action.None, s.bindings[it]) }
        assertEquals(Action.None, Settings(bindings = emptyMap()).actionFor(Gesture.TAP))
    }

    @Test fun `ST-7 toGestureConfig converts dp and derives doubleTapEnabled`() {
        val s = Settings(doubleTapMs = 250, longPressMs = 700)
        assertEquals(
            GestureConfig(doubleTapMs = 250, longPressMs = 700, touchSlopPx = 24f, swipeMinDistancePx = 80f, doubleTapEnabled = true),
            s.toGestureConfig(density = 2f),
        )
        val noDouble = s.copy(bindings = s.bindings + (Gesture.DOUBLE_TAP to Action.None))
        assertFalse(noDouble.toGestureConfig(2f).doubleTapEnabled)
        assertEquals(80f, noDouble.toGestureConfig(2f).swipeMinDistancePx)
        assertEquals(24f, noDouble.toGestureConfig(2f).touchSlopPx)
    }

    @Test fun `ST-9 v1_1 defaults`() {
        val s = Settings()
        assertEquals(ThemeMode.SYSTEM, s.theme)
        assertFalse(s.amoledBlack)
        assertEquals(AppLanguage.SYSTEM, s.language)
        assertTrue(s.startOnBoot)
        assertEquals(Gesture.entries.size, s.bindings.size)
        assertEquals(
            setOf(Gesture.LONG_PRESS, Gesture.SWIPE_DOWN_HOLD, Gesture.SWIPE_LEFT_HOLD, Gesture.SWIPE_RIGHT_HOLD),
            HoldGestures,
        )
    }

    @Test fun `ST-10 sanitized completes bindings`() {
        val s = Settings(bindings = mapOf(Gesture.TAP to Action.Home)).sanitized()
        assertEquals(9, s.bindings.size)
        assertEquals(Action.Home, s.bindings[Gesture.TAP])
    }

    @Test fun `ST-12 swipe down gestures are unavailable in Shizuku mode`() {
        val bound = Settings(
            bindings = DefaultBindings + mapOf(Gesture.SWIPE_DOWN to Action.Home, Gesture.SWIPE_DOWN_HOLD to Action.VolumeUp),
        )
        val shizuku = bound.copy(mode = DetectionMode.SHIZUKU)
        assertEquals(Action.None, shizuku.effectiveAction(Gesture.SWIPE_DOWN))
        assertEquals(Action.None, shizuku.effectiveAction(Gesture.SWIPE_DOWN_HOLD))
        assertEquals(Action.MediaNext, shizuku.effectiveAction(Gesture.SWIPE_RIGHT))
        assertFalse(Gesture.SWIPE_DOWN.isAvailableIn(DetectionMode.SHIZUKU))
        assertTrue(Gesture.SWIPE_DOWN.isAvailableIn(DetectionMode.ACCESSIBILITY))
        val a11y = bound.copy(mode = DetectionMode.ACCESSIBILITY)
        assertEquals(Action.Home, a11y.effectiveAction(Gesture.SWIPE_DOWN))
        assertEquals(Action.VolumeUp, a11y.effectiveAction(Gesture.SWIPE_DOWN_HOLD))
    }

    @Test fun `ST-13 toGestureConfig derives hold swipes from effective bindings`() {
        val s = Settings(
            bindings = DefaultBindings + mapOf(
                Gesture.SWIPE_LEFT_HOLD to Action.VolumeDown,
                Gesture.SWIPE_DOWN_HOLD to Action.Home,
            ),
        )
        assertEquals(setOf(Gesture.SWIPE_LEFT_HOLD, Gesture.SWIPE_DOWN_HOLD), s.toGestureConfig(1f).holdSwipes)
        assertEquals(setOf(Gesture.SWIPE_LEFT_HOLD), s.copy(mode = DetectionMode.SHIZUKU).toGestureConfig(1f).holdSwipes)
    }

    @Test fun `ST-19 disabled gestures keep their binding but do nothing`() {
        val s = Settings(
            bindings = DefaultBindings + (Gesture.SWIPE_LEFT_HOLD to Action.VolumeDown),
            disabledGestures = setOf(Gesture.DOUBLE_TAP, Gesture.SWIPE_LEFT_HOLD),
        )
        assertEquals(Action.None, s.effectiveAction(Gesture.DOUBLE_TAP))
        assertEquals(Action.LockScreen, s.actionFor(Gesture.DOUBLE_TAP))
        assertEquals(Action.VolumeDown, s.actionFor(Gesture.SWIPE_LEFT_HOLD))
        assertEquals(Action.MediaNext, s.effectiveAction(Gesture.SWIPE_RIGHT))
        val config = s.toGestureConfig(1f)
        assertFalse(config.doubleTapEnabled)
        assertEquals(emptySet(), config.holdSwipes)
    }

    @Test fun `ST-20 switching a gesture off and on keeps its action`() {
        val s = Settings()
        assertTrue(s.isGestureOn(Gesture.DOUBLE_TAP))
        assertFalse(s.isGestureOn(Gesture.TAP)) // bound to None
        val off = s.withGestureOn(Gesture.DOUBLE_TAP, false)
        assertFalse(off.isGestureOn(Gesture.DOUBLE_TAP))
        assertEquals(setOf(Gesture.DOUBLE_TAP), off.disabledGestures)
        assertEquals(s.bindings, off.bindings)
        assertEquals(s, off.withGestureOn(Gesture.DOUBLE_TAP, true))
        val bound = off.withBinding(Gesture.DOUBLE_TAP, Action.Home)
        assertEquals(Action.Home, bound.actionFor(Gesture.DOUBLE_TAP))
        assertTrue(bound.isGestureOn(Gesture.DOUBLE_TAP))
        assertEquals(emptySet(), bound.disabledGestures)
    }

    @Test fun `ST-17 manual region top is clamped`() {
        assertEquals(0, Settings(manualRegion = ManualRegionDp(topDp = -5)).sanitized().manualRegion?.topDp)
        assertEquals(100, Settings(manualRegion = ManualRegionDp(topDp = 500)).sanitized().manualRegion?.topDp)
        assertEquals(24, Settings(manualRegion = ManualRegionDp(topDp = 24)).sanitized().manualRegion?.topDp)
    }
}
