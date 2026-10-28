package io.github.ptt98hd.totch.core.input

import io.github.ptt98hd.totch.core.model.TouchAction
import io.github.ptt98hd.totch.core.model.TouchEvent

/**
 * Converts `getevent -lt <device>` lines (MT protocol type B) into [TouchEvent]s for the primary
 * pointer (lowest active slot). Events are only emitted on `SYN_REPORT`. Never throws.
 */
class GeteventParser(private val mapper: CoordinateMapper) {
    private val lineRegex =
        Regex("""^\s*\[\s*(\d+)\.(\d+)\s*]\s*(?:\S+:\s+)?(EV_\w+)\s+(\w+)\s+([0-9a-fA-F]+)\s*$""")

    private class Slot(var trackingId: Int = -1, var x: Int = 0, var y: Int = 0)

    private val slots = sortedMapOf<Int, Slot>()
    private var currentSlot = 0
    private var lastTimeMs = 0L
    private var lastEmitted: TouchEvent? = null // last DOWN/MOVE while touching, null when up

    fun feed(line: String): List<TouchEvent> {
        val m = lineRegex.find(line) ?: return emptyList()
        val (sec, usec, type, code, hex) = m.destructured
        val value = hex.toLongOrNull(16)?.toInt() ?: return emptyList()
        lastTimeMs = (sec.toLongOrNull() ?: return emptyList()) * 1000 + (usec.toLongOrNull() ?: 0) / 1000
        when (type) {
            "EV_ABS" -> when (code) {
                "ABS_MT_SLOT" -> currentSlot = value
                "ABS_MT_TRACKING_ID" -> slot().trackingId = value
                "ABS_MT_POSITION_X" -> slot().x = value
                "ABS_MT_POSITION_Y" -> slot().y = value
            }
            "EV_SYN" -> if (code == "SYN_REPORT") return listOfNotNull(report())
        }
        return emptyList()
    }

    private fun slot() = slots.getOrPut(currentSlot) { Slot() }

    private fun report(): TouchEvent? {
        val active = slots.values.filter { it.trackingId != -1 }
        val previous = lastEmitted
        if (active.isEmpty()) {
            lastEmitted = null
            return previous?.copy(action = TouchAction.UP, timeMs = lastTimeMs, pointerCount = 1)
        }
        val primary = active.first()
        val (x, y) = mapper.map(primary.x, primary.y)
        val action = if (previous == null) TouchAction.DOWN else TouchAction.MOVE
        if (previous != null && previous.x == x && previous.y == y && previous.pointerCount == active.size) return null
        return TouchEvent(action, x, y, lastTimeMs, active.size).also { lastEmitted = it }
    }
}
