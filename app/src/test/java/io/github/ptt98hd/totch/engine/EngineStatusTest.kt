package io.github.ptt98hd.totch.engine

import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.core.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class EngineStatusTest {
    private val on = Settings(enabled = true)
    private val off = Settings(enabled = false)
    private val ready = ShizukuState(running = true, granted = true)

    private fun status(
        settings: Settings,
        shizuku: ShizukuState = ready,
        pipeline: PipelineState = PipelineState.Running,
        rotation: Int = 0,
    ) = resolveEngineStatus(settings, shizuku, pipeline, rotation) { "err:$it" }

    @Test
    fun `disabled wins when the pipeline is stopped`() {
        assertEquals(EngineStatus.Disabled, status(off, ShizukuState(false, false), PipelineState.Idle, rotation = 1))
        assertEquals(EngineStatus.Disabled, status(off, pipeline = PipelineState.Failed(PipelineError.STREAM_ENDED)))
    }

    @Test
    fun `DM-30 disabled with a live pipeline is Stopping`() {
        assertEquals(EngineStatus.Stopping, status(off, pipeline = PipelineState.Running))
        assertEquals(EngineStatus.Stopping, status(off, pipeline = PipelineState.Connecting))
    }

    @Test
    fun `DM-2 shizuku not running is WaitingShizuku`() {
        assertEquals(EngineStatus.WaitingShizuku, status(on, ShizukuState(running = false, granted = false)))
    }

    @Test
    fun `DM-3 permission missing is ShizukuPermissionRequired`() {
        assertEquals(EngineStatus.ShizukuPermissionRequired, status(on, ShizukuState(running = true, granted = false)))
    }

    @Test
    fun `DM-5 pipeline failure is Error with message`() {
        assertEquals(
            EngineStatus.Error("err:NO_TOUCHSCREEN"),
            status(on, pipeline = PipelineState.Failed(PipelineError.NO_TOUCHSCREEN)),
        )
    }

    @Test
    fun `DM-30 connecting or idle pipeline with Shizuku ready is Starting`() {
        assertEquals(EngineStatus.Starting, status(on, pipeline = PipelineState.Connecting))
        assertEquals(EngineStatus.Starting, status(on, pipeline = PipelineState.Idle))
    }

    @Test
    fun `running pipeline is Running and paused when rotated`() {
        assertEquals(EngineStatus.Running, status(on))
        assertEquals(EngineStatus.PausedRotation, status(on, rotation = 1))
    }

    @Test
    fun `DM-31 landscape gestures keep running when rotated`() {
        assertEquals(EngineStatus.Running, status(on.copy(landscapeGestures = true), rotation = 1))
        assertEquals(EngineStatus.Running, status(on.copy(landscapeGestures = true), rotation = 3))
    }
}
