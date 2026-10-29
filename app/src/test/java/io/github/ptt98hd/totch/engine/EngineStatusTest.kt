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
        accessibility: Boolean = false,
    ) = resolveEngineStatus(settings, shizuku, pipeline, rotation, accessibility) { "err:$it" }

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
    fun `DM-41 neither Shizuku nor Accessibility is NeedsAccess`() {
        assertEquals(EngineStatus.NeedsAccess, status(on, ShizukuState(running = false, granted = false), PipelineState.Idle))
        assertEquals(EngineStatus.NeedsAccess, status(on, ShizukuState(running = true, granted = false), PipelineState.Idle))
    }

    @Test
    fun `DM-42 Accessibility alone runs, but only in portrait`() {
        val none = ShizukuState(running = false, granted = false)
        assertEquals(EngineStatus.Running, status(on, none, PipelineState.Idle, accessibility = true))
        assertEquals(EngineStatus.PausedRotation, status(on, none, PipelineState.Idle, rotation = 1, accessibility = true))
        assertEquals(
            EngineStatus.PausedRotation,
            status(on.copy(landscapeGestures = true), none, PipelineState.Idle, rotation = 1, accessibility = true),
        )
    }

    @Test
    fun `DM-43 Accessibility takes over when the Shizuku pipeline fails`() {
        val failed = PipelineState.Failed(PipelineError.NO_TOUCHSCREEN)
        assertEquals(EngineStatus.Running, status(on, pipeline = failed, accessibility = true))
        assertEquals(EngineStatus.Error("err:NO_TOUCHSCREEN"), status(on, pipeline = failed, accessibility = false))
    }

    @Test
    fun `DM-40 touch source prefers Shizuku, then Accessibility`() {
        val none = ShizukuState(running = false, granted = false)
        val failed = PipelineState.Failed(PipelineError.STREAM_ENDED)
        assertEquals(TouchSource.SHIZUKU, touchSource(ready, PipelineState.Running, accessibility = true))
        assertEquals(TouchSource.SHIZUKU, touchSource(ready, PipelineState.Connecting, accessibility = false))
        assertEquals(TouchSource.ACCESSIBILITY, touchSource(ready, failed, accessibility = true))
        assertEquals(TouchSource.SHIZUKU, touchSource(ready, failed, accessibility = false))
        assertEquals(TouchSource.ACCESSIBILITY, touchSource(none, PipelineState.Idle, accessibility = true))
        assertEquals(TouchSource.NONE, touchSource(none, PipelineState.Idle, accessibility = false))
        assertEquals(TouchSource.NONE, touchSource(ShizukuState(running = true, granted = false), PipelineState.Idle, false))
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
