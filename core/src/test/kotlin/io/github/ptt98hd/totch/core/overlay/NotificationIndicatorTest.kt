package io.github.ptt98hd.totch.core.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationIndicatorTest {
    private val indicator = NotificationIndicator(ownPackage = "io.github.ptt98hd.totch")

    private fun meta(
        key: String = "k1",
        pkg: String = "com.chat",
        time: Long = 1_000,
        ongoing: Boolean = false,
        clearable: Boolean = true,
        groupSummary: Boolean = false,
        alerting: Boolean = true,
        onlyAlertOnce: Boolean = false,
        color: Int? = 0xFF00FF00.toInt(),
    ) = NotificationMeta(key, pkg, time, ongoing, clearable, groupSummary, alerting, onlyAlertOnce, color)

    @Test fun `NI-1 ongoing, persistent, summaries, silent and own notifications are ignored`() {
        listOf(
            meta(key = "a", ongoing = true),
            meta(key = "b", clearable = false),
            meta(key = "c", groupSummary = true),
            meta(key = "d", alerting = false),
            meta(key = "e", pkg = "io.github.ptt98hd.totch"),
        ).forEach(indicator::onPosted)
        assertNull(indicator.state)
    }

    @Test fun `NI-2 a new notification lights the indicator`() {
        indicator.onPosted(meta())
        assertEquals(IndicatorState(1, 0xFF00FF00.toInt()), indicator.state)
    }

    @Test fun `NI-3 updates after seen light again unless only alert once`() {
        indicator.onPosted(meta())
        indicator.onSeen()
        indicator.onPosted(meta(time = 2_000, onlyAlertOnce = true))
        assertNull(indicator.state)
        indicator.onPosted(meta(time = 3_000, onlyAlertOnce = false))
        assertEquals(1, indicator.state?.count)
    }

    @Test fun `NI-4 removing the last unseen notification turns it off`() {
        indicator.onPosted(meta(key = "a"))
        indicator.onPosted(meta(key = "b", time = 2_000))
        indicator.onRemoved("a")
        assertEquals(1, indicator.state?.count)
        indicator.onRemoved("b")
        assertNull(indicator.state)
    }

    @Test fun `NI-5 seen turns it off until the next notification`() {
        indicator.onPosted(meta(key = "a"))
        indicator.onSeen()
        assertNull(indicator.state)
        indicator.onPosted(meta(key = "b", time = 5_000))
        assertEquals(1, indicator.state?.count)
    }

    @Test fun `NI-6 count and color of the newest notification`() {
        indicator.onPosted(meta(key = "a", time = 3_000, color = 1))
        indicator.onPosted(meta(key = "b", time = 1_000, color = 2))
        assertEquals(IndicatorState(2, 1), indicator.state)
        indicator.onPosted(meta(key = "c", time = 4_000, color = null))
        assertEquals(IndicatorState(3, null), indicator.state)
    }

    @Test fun `NI-7 reset treats existing notifications as seen`() {
        indicator.onPosted(meta(key = "old"))
        indicator.reset(listOf(meta(key = "a"), meta(key = "b")))
        assertNull(indicator.state)
        indicator.onRemoved("a")
        indicator.onRemoved("missing")
        assertNull(indicator.state)
        indicator.onPosted(meta(key = "b", time = 9_000, onlyAlertOnce = true))
        assertNull(indicator.state)
    }

    @Test fun `NI-8 a notification updated to ongoing is dropped`() {
        indicator.onPosted(meta())
        indicator.onPosted(meta(time = 2_000, ongoing = true))
        assertNull(indicator.state)
    }

    @Test fun `NI-9 posted returns whether it alerts`() {
        assertEquals(false, indicator.onPosted(meta(key = "x", ongoing = true)))
        assertEquals(true, indicator.onPosted(meta(key = "a", onlyAlertOnce = true)))
        assertEquals(false, indicator.onPosted(meta(key = "a", time = 2_000, onlyAlertOnce = true)))
        assertEquals(true, indicator.onPosted(meta(key = "a", time = 3_000)))
        indicator.onSeen()
        assertEquals(false, indicator.onPosted(meta(key = "a", time = 4_000, onlyAlertOnce = true)))
    }
}
