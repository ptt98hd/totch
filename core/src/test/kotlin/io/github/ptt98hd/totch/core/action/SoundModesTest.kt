package io.github.ptt98hd.totch.core.action

import kotlin.test.Test
import kotlin.test.assertEquals

class SoundModesTest {
    @Test fun `SM-1 cycle switches between ring and silent`() {
        assertEquals(RingerMode.SILENT, SoundModes.next(RingerMode.NORMAL))
        assertEquals(RingerMode.NORMAL, SoundModes.next(RingerMode.SILENT))
    }

    @Test fun `SM-2 vibrate set by the user goes to ring`() {
        assertEquals(RingerMode.NORMAL, SoundModes.next(RingerMode.VIBRATE))
    }
}
