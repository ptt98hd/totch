package io.github.ptt98hd.totch.core.settings

import io.github.ptt98hd.totch.core.gesture.GestureConfig
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.overlay.IslandActivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsTest {
    /** The pre-1.8 default bindings, used where a test needs bound gestures. */
    private val bound = DefaultBindings + mapOf(
        Gesture.DOUBLE_TAP to Action.LockScreen, Gesture.LONG_PRESS to Action.ToggleTorch,
        Gesture.SWIPE_DOWN to Action.Notifications, Gesture.SWIPE_LEFT to Action.MediaPrevious,
        Gesture.SWIPE_RIGHT to Action.MediaNext,
    )

    /** Switches on exactly the gestures bound to an action, as a pre-1.8.1 configuration behaved (ST-32). */
    private fun Settings.boundOn() = copy(enabledGestures = bindings.filterValues { it != Action.None }.keys)

    @Test fun `ST-1 defaults match FR-S2`() {
        val s = Settings()
        assertEquals(Gesture.entries.associateWith { Action.None }, s.bindings)
        assertFalse(s.enabled)
        assertEquals(24, s.paddingPx)
        assertEquals(null, s.notchRegion)
        assertTrue(s.haptics)
        assertEquals(s, s.sanitized())
    }

    @Test fun `ST-30 every gesture starts not set and off, saved bindings kept`() {
        val s = Settings()
        Gesture.entries.forEach {
            assertEquals(Action.None, s.actionFor(it))
            assertFalse(s.isGestureOn(it, canBlockTouches = true))
        }
        val saved = SettingsCodec.decode("""{"bindings":{"DOUBLE_TAP":{"type":"lock"}},"longPressMs":500}""")
        assertEquals(Action.LockScreen, saved.actionFor(Gesture.DOUBLE_TAP))
    }

    @Test fun `ST-41 sanitized clamps padding and notch region in px`() {
        val low = Settings(paddingPx = -5, notchRegion = NotchRegionPx(-10, 1, 1, -5)).sanitized()
        assertEquals(0, low.paddingPx)
        assertEquals(NotchRegionPx(0, 4, 4, 0), low.notchRegion)
        val high = Settings(paddingPx = 9999, notchRegion = NotchRegionPx(99999, 9999, 9999, 9999)).sanitized()
        assertEquals(60, high.paddingPx)
        assertEquals(NotchRegionPx(10000, 600, 300, 300), high.notchRegion)
        val ok = Settings(paddingPx = 0, notchRegion = NotchRegionPx(540, 41, 41, 30))
        assertEquals(ok, ok.sanitized())
    }

    @Test fun `ST-3 sanitized fills missing gestures with None`() {
        val s = Settings(bindings = mapOf(Gesture.LONG_PRESS to Action.Home)).sanitized()
        assertEquals(Gesture.entries.toSet(), s.bindings.keys)
        assertEquals(Action.Home, s.bindings[Gesture.LONG_PRESS])
        Gesture.entries.filter { it != Gesture.LONG_PRESS }.forEach { assertEquals(Action.None, s.bindings[it]) }
        assertEquals(Action.None, Settings(bindings = emptyMap()).actionFor(Gesture.TAP))
    }

    @Test fun `ST-40 toGestureConfig uses the fixed timings and converts dp`() {
        val s = Settings(bindings = bound).boundOn()
        assertEquals(
            GestureConfig(doubleTapMs = 300, longPressMs = 300, touchSlopPx = 24f, swipeMinDistancePx = 80f, doubleTapEnabled = true),
            s.toGestureConfig(density = 2f),
        )
        val noDouble = s.withGestureOn(Gesture.DOUBLE_TAP, false)
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

    @Test fun `ST-12 swipe down gestures are unavailable`() {
        val s = Settings(
            bindings = bound + mapOf(Gesture.SWIPE_DOWN to Action.Home, Gesture.SWIPE_DOWN_HOLD to Action.VolumeUp),
        ).boundOn()
        assertEquals(Action.None, s.effectiveAction(Gesture.SWIPE_DOWN))
        assertEquals(Action.None, s.effectiveAction(Gesture.SWIPE_DOWN_HOLD))
        assertEquals(Action.Home, s.actionFor(Gesture.SWIPE_DOWN))
        assertEquals(Action.MediaNext, s.effectiveAction(Gesture.SWIPE_RIGHT))
        assertFalse(Gesture.SWIPE_DOWN.isAvailable(canBlockTouches = false))
        assertFalse(Gesture.SWIPE_DOWN_HOLD.isAvailable(canBlockTouches = false))
        assertTrue(Gesture.entries.filter { it !in SwipeDownGestures }.all { it.isAvailable(canBlockTouches = false) })
    }

    @Test fun `ST-22 unavailable gestures are switched off`() {
        val s = Settings(bindings = bound + (Gesture.SWIPE_DOWN to Action.Home)).boundOn()
        assertFalse(s.isGestureOn(Gesture.SWIPE_DOWN))
        assertFalse(s.isGestureOn(Gesture.SWIPE_DOWN_HOLD))
        assertTrue(s.isGestureOn(Gesture.SWIPE_RIGHT))
    }

    @Test fun `ST-13 toGestureConfig derives hold swipes from effective bindings`() {
        val s = Settings(
            bindings = DefaultBindings + mapOf(
                Gesture.SWIPE_LEFT_HOLD to Action.VolumeDown,
                Gesture.SWIPE_DOWN_HOLD to Action.Home,
            ),
        ).boundOn()
        assertEquals(setOf(Gesture.SWIPE_LEFT_HOLD), s.toGestureConfig(1f).holdSwipes)
    }

    @Test fun `ST-19 switched off gestures keep their binding but do nothing`() {
        val s = Settings(bindings = bound + (Gesture.SWIPE_LEFT_HOLD to Action.VolumeDown)).boundOn()
            .withGestureOn(Gesture.DOUBLE_TAP, false)
            .withGestureOn(Gesture.SWIPE_LEFT_HOLD, false)
        assertEquals(Action.None, s.effectiveAction(Gesture.DOUBLE_TAP))
        assertEquals(Action.LockScreen, s.actionFor(Gesture.DOUBLE_TAP))
        assertEquals(Action.VolumeDown, s.actionFor(Gesture.SWIPE_LEFT_HOLD))
        assertEquals(Action.MediaNext, s.effectiveAction(Gesture.SWIPE_RIGHT))
        val config = s.toGestureConfig(1f)
        assertFalse(config.doubleTapEnabled)
        assertEquals(emptySet(), config.holdSwipes)
    }

    @Test fun `ST-20 switching a gesture off and on keeps its action`() {
        val s = Settings(bindings = bound).boundOn()
        assertTrue(s.isGestureOn(Gesture.DOUBLE_TAP))
        assertFalse(s.isGestureOn(Gesture.TAP)) // bound to None, never switched on
        val off = s.withGestureOn(Gesture.DOUBLE_TAP, false)
        assertFalse(off.isGestureOn(Gesture.DOUBLE_TAP))
        assertFalse(Gesture.DOUBLE_TAP in off.enabledGestures)
        assertEquals(s.bindings, off.bindings)
        assertEquals(s, off.withGestureOn(Gesture.DOUBLE_TAP, true))
        val bound = off.withBinding(Gesture.DOUBLE_TAP, Action.Home)
        assertEquals(Action.Home, bound.actionFor(Gesture.DOUBLE_TAP))
        assertTrue(bound.isGestureOn(Gesture.DOUBLE_TAP))
    }

    @Test fun `AD-14 island shows the volume only when it can`() {
        val on = Settings(enabled = true, island = true)
        assertTrue(on.islandShowsVolume(accessibility = true, statusBarVisible = true))
        assertFalse(on.islandShowsVolume(accessibility = false, statusBarVisible = true))
        assertFalse(on.islandShowsVolume(accessibility = true, statusBarVisible = false))
        assertFalse(on.copy(enabled = false).islandShowsVolume(true, true))
        assertFalse(on.copy(island = false).islandShowsVolume(true, true))
        assertFalse(on.copy(islandActivities = setOf(IslandActivity.MEDIA)).islandShowsVolume(true, true))
    }

    @Test fun `ST-24 swipe down gestures are available when touches can be blocked`() {
        val s = Settings(
            bindings = DefaultBindings + mapOf(Gesture.SWIPE_DOWN to Action.Home, Gesture.SWIPE_DOWN_HOLD to Action.VolumeUp),
        ).boundOn()
        assertTrue(Gesture.entries.all { it.isAvailable(canBlockTouches = true) })
        assertEquals(Action.Home, s.effectiveAction(Gesture.SWIPE_DOWN, canBlockTouches = true))
        assertTrue(s.isGestureOn(Gesture.SWIPE_DOWN_HOLD, canBlockTouches = true))
        assertFalse(s.withGestureOn(Gesture.SWIPE_DOWN_HOLD, false).isGestureOn(Gesture.SWIPE_DOWN_HOLD, canBlockTouches = true))
        assertEquals(setOf(Gesture.SWIPE_DOWN_HOLD), s.toGestureConfig(1f, canBlockTouches = true).holdSwipes)
        assertEquals(emptySet(), s.toGestureConfig(1f).holdSwipes)
    }

    @Test fun `ST-25 landscape gestures default off`() {
        assertFalse(Settings().landscapeGestures)
    }

    @Test fun `ST-26 touch blocking is needed only for switched on swipe down gestures`() {
        val on = Settings(enabled = true, bindings = bound).boundOn()
        assertTrue(on.needsTouchBlocking()) // SWIPE_DOWN → Notifications
        assertFalse(on.copy(enabled = false).needsTouchBlocking())
        assertFalse(on.withGestureOn(Gesture.SWIPE_DOWN, false).needsTouchBlocking())
        val holdOnly = on.withGestureOn(Gesture.SWIPE_DOWN, false).withBinding(Gesture.SWIPE_DOWN_HOLD, Action.Home)
        assertTrue(holdOnly.needsTouchBlocking())
        assertFalse(holdOnly.withGestureOn(Gesture.SWIPE_DOWN_HOLD, false).needsTouchBlocking())
    }

    @Test fun `DM-57 notch touches are blocked while any gesture is on`() {
        val s = Settings(enabled = true)
        assertFalse(s.blocksNotchTouches())
        val tap = s.withGestureOn(Gesture.TAP, true) // Not set still listens (FR-G15)
        assertTrue(tap.blocksNotchTouches())
        assertFalse(tap.copy(enabled = false).blocksNotchTouches())
        assertTrue(s.withGestureOn(Gesture.SWIPE_DOWN, true).blocksNotchTouches())
        assertFalse(tap.withGestureOn(Gesture.TAP, false).blocksNotchTouches())
    }

    @Test fun `ST-31 the switch decides whether a gesture listens, Not set does nothing`() {
        val s = Settings(enabled = true)
        assertEquals(emptySet(), s.enabledGestures)
        Gesture.entries.forEach { assertFalse(s.isGestureOn(it, canBlockTouches = true)) }

        val notSet = s.withGestureOn(Gesture.DOUBLE_TAP, true)
            .withGestureOn(Gesture.SWIPE_RIGHT_HOLD, true)
            .withGestureOn(Gesture.SWIPE_DOWN, true)
        assertTrue(notSet.isGestureOn(Gesture.DOUBLE_TAP))
        assertEquals(Action.None, notSet.effectiveAction(Gesture.DOUBLE_TAP))
        val config = notSet.toGestureConfig(1f, canBlockTouches = true)
        assertTrue(config.doubleTapEnabled)
        assertEquals(setOf(Gesture.SWIPE_RIGHT_HOLD), config.holdSwipes)
        assertTrue(notSet.needsTouchBlocking())
        assertFalse(notSet.isGestureOn(Gesture.SWIPE_DOWN)) // still needs the overlay (ST-22)

        assertEquals(s, notSet.withGestureOn(Gesture.DOUBLE_TAP, false)
            .withGestureOn(Gesture.SWIPE_RIGHT_HOLD, false).withGestureOn(Gesture.SWIPE_DOWN, false))
        val cleared = notSet.withBinding(Gesture.TAP, Action.Home).withBinding(Gesture.TAP, Action.None)
        assertTrue(cleared.isGestureOn(Gesture.TAP))
        assertEquals(Action.None, cleared.actionFor(Gesture.TAP))
    }
}
