package io.github.ptt98hd.totch.core.input

object GeteventDeviceParser {
    private val addDevice = Regex("""^\s*add device \d+:\s*(\S+)\s*$""")
    private val name = Regex("""^\s*name:\s*"(.*)"\s*$""")
    private val axis = Regex("""(ABS_MT_POSITION_[XY])\s*:\s*value\s+-?\d+,\s*min\s+(-?\d+),\s*max\s+(-?\d+)""")

    private class Block(val path: String) {
        var name = ""
        var x: AbsRange? = null
        var y: AbsRange? = null
        var direct = false

        fun toInfo(): InputDeviceInfo? {
            return InputDeviceInfo(path, name, x ?: return null, y ?: return null, direct)
        }
    }

    /** Parses `getevent -pl` output; only devices with both MT position axes are returned. Never throws. */
    fun parse(getEventPlOutput: String): List<InputDeviceInfo> {
        val blocks = mutableListOf<Block>()
        for (line in getEventPlOutput.lineSequence()) {
            val header = addDevice.find(line)
            if (header != null) {
                blocks += Block(header.groupValues[1])
                continue
            }
            val block = blocks.lastOrNull() ?: continue
            name.find(line)?.let { block.name = it.groupValues[1] }
            axis.find(line)?.let {
                val (code, min, max) = it.destructured
                val range = AbsRange(min.toIntOrNull() ?: return@let, max.toIntOrNull() ?: return@let)
                if (code.endsWith('X')) block.x = range else block.y = range
            }
            if ("INPUT_PROP_DIRECT" in line) block.direct = true
        }
        return blocks.mapNotNull { it.toInfo() }
    }

    /** Prefers a direct (touchscreen) device, else the first; `null` if none. */
    fun pickTouchscreen(devices: List<InputDeviceInfo>): InputDeviceInfo? =
        devices.firstOrNull { it.direct } ?: devices.firstOrNull()
}
