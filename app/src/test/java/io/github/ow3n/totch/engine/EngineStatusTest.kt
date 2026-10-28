package io.github.ow3n.totch.engine

import io.github.ow3n.totch.EngineStatus
import io.github.ow3n.totch.core.settings.DetectionMode
import io.github.ow3n.totch.core.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class EngineStatusTest {
    private val shizukuOn = Settings(enabled = true, mode = DetectionMode.SHIZUKU)
    private val a11yOn = Settings(enabled = true, mode = DetectionMode.ACCESSIBILITY)
    private val ready = ShizukuState(running = true, granted = true)

    private fun status(
        settings: Settings,
        shizuku: ShizukuState = ready,
        pipeline: PipelineState = PipelineState.Running,
        a11y: Boolean = true,
        rotation: Int = 0,
    ) = resolveEngineStatus(settings, shizuku, pipeline, a11y, rotation) { "err:$it" }

    @Test
    fun `disabled wins over everything`() {
        assertEquals(EngineStatus.Disabled, status(Settings(enabled = false), ShizukuState(false, false), a11y = false, rotation = 1))
    }

    @Test
    fun `DM-2 shizuku not running is WaitingShizuku`() {
        assertEquals(EngineStatus.WaitingShizuku, status(shizukuOn, ShizukuState(running = false, granted = false)))
    }

    @Test
    fun `DM-3 permission missing is ShizukuPermissionRequired`() {
        assertEquals(EngineStatus.ShizukuPermissionRequired, status(shizukuOn, ShizukuState(running = true, granted = false)))
    }

    @Test
    fun `DM-5 pipeline failure is Error with message`() {
        assertEquals(
            EngineStatus.Error("err:NO_TOUCHSCREEN"),
            status(shizukuOn, pipeline = PipelineState.Failed(PipelineError.NO_TOUCHSCREEN)),
        )
    }

    @Test
    fun `DM-6 connecting or idle pipeline is WaitingShizuku`() {
        assertEquals(EngineStatus.WaitingShizuku, status(shizukuOn, pipeline = PipelineState.Connecting))
        assertEquals(EngineStatus.WaitingShizuku, status(shizukuOn, pipeline = PipelineState.Idle))
    }

    @Test
    fun `shizuku running pipeline is Running and paused when rotated`() {
        assertEquals(EngineStatus.Running, status(shizukuOn))
        assertEquals(EngineStatus.PausedRotation, status(shizukuOn, rotation = 1))
    }

    @Test
    fun `accessibility mode requires the connected service`() {
        assertEquals(EngineStatus.AccessibilityRequired, status(a11yOn, a11y = false))
        assertEquals(EngineStatus.Running, status(a11yOn, shizuku = ShizukuState(false, false), pipeline = PipelineState.Idle))
    }

    @Test
    fun `DM-12 accessibility mode pauses in landscape`() {
        assertEquals(EngineStatus.PausedRotation, status(a11yOn, rotation = 3))
    }
}
