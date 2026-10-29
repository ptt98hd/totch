package io.github.ptt98hd.totch.core.action

import kotlin.test.Test
import kotlin.test.assertEquals

class SoundModesTest {
    @Test fun `SM-1 cycle ring, vibrate, silent`() {
        assertEquals(RingerMode.VIBRATE, SoundModes.next(RingerMode.NORMAL, canVibrate = true))
        assertEquals(RingerMode.SILENT, SoundModes.next(RingerMode.VIBRATE, canVibrate = true))
        assertEquals(RingerMode.NORMAL, SoundModes.next(RingerMode.SILENT, canVibrate = true))
    }

    @Test fun `SM-2 without a vibrator vibrate is skipped`() {
        assertEquals(RingerMode.SILENT, SoundModes.next(RingerMode.NORMAL, canVibrate = false))
        assertEquals(RingerMode.SILENT, SoundModes.next(RingerMode.VIBRATE, canVibrate = false))
        assertEquals(RingerMode.NORMAL, SoundModes.next(RingerMode.SILENT, canVibrate = false))
    }
}
