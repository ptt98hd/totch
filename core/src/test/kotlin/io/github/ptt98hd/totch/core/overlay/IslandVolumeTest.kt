package io.github.ptt98hd.totch.core.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class IslandVolumeTest {
    @Test fun `VR-1 the volume ring sweeps with the level`() {
        assertEquals(180f, volumeSweep(0.5f))
        assertEquals(0f, volumeSweep(-1f))
        assertEquals(360f, volumeSweep(2f))
    }
}
