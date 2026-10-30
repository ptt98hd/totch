package io.github.ptt98hd.totch.core.action

/** [VIBRATE] is only read: the user may have chosen it in the system (1.8.3 has no vibrate action). */
enum class RingerMode { NORMAL, VIBRATE, SILENT }

/** Sound mode cycle of [io.github.ptt98hd.totch.core.model.Action.CycleRingerMode] (actions spec §12). */
object SoundModes {
    /** SM-1, SM-2: ring ↔ silent; vibrate goes to ring. */
    fun next(current: RingerMode): RingerMode = when (current) {
        RingerMode.NORMAL -> RingerMode.SILENT
        RingerMode.VIBRATE, RingerMode.SILENT -> RingerMode.NORMAL
    }
}
