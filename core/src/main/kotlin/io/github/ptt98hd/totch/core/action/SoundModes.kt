package io.github.ptt98hd.totch.core.action

enum class RingerMode { NORMAL, VIBRATE, SILENT }

/** Sound mode cycle of [io.github.ptt98hd.totch.core.model.Action.CycleRingerMode] (actions spec §12). */
object SoundModes {
    /** SM-1, SM-2: ring → vibrate → silent → ring; vibrate is skipped without a vibrator. */
    fun next(current: RingerMode, canVibrate: Boolean): RingerMode = when (current) {
        RingerMode.NORMAL -> if (canVibrate) RingerMode.VIBRATE else RingerMode.SILENT
        RingerMode.VIBRATE -> RingerMode.SILENT
        RingerMode.SILENT -> RingerMode.NORMAL
    }
}
