package io.github.ow3n.totch.core.input

data class AbsRange(val min: Int, val max: Int)

/** A multi-touch input device as reported by `getevent -pl`; [direct] = has `INPUT_PROP_DIRECT`. */
data class InputDeviceInfo(
    val path: String,
    val name: String,
    val x: AbsRange,
    val y: AbsRange,
    val direct: Boolean,
)
