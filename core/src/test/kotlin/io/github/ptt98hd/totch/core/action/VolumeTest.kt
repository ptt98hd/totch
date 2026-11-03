package io.github.ptt98hd.totch.core.action

import kotlin.test.Test
import kotlin.test.assertEquals

class VolumeTest {
    @Test fun `AD-15 no system panel while the island shows the volume`() {
        assertEquals(0, volumeFlags(islandShowsVolume = true))
        assertEquals(1, volumeFlags(islandShowsVolume = false))
    }
}
