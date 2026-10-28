package io.github.ptt98hd.totch.ui.home

import app.cash.turbine.test
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.Requirements
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DetectionMode
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.ui.FakeSettingsRepository
import io.github.ptt98hd.totch.ui.FakeSystemStatus
import io.github.ptt98hd.totch.ui.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    /** Bindings without media actions, so the optional media card (UI-3) stays hidden. */
    private val noMedia = Gesture.entries.associateWith { Action.None }

    private val allStatuses = listOf(
        EngineStatus.Disabled,
        EngineStatus.WaitingShizuku,
        EngineStatus.ShizukuPermissionRequired,
        EngineStatus.AccessibilityRequired,
        EngineStatus.Error("boom"),
        EngineStatus.Running,
        EngineStatus.PausedRotation,
    )

    private fun vm(
        repo: FakeSettingsRepository = FakeSettingsRepository(),
        engine: MutableStateFlow<EngineStatus> = MutableStateFlow(EngineStatus.Disabled),
        sys: FakeSystemStatus = FakeSystemStatus(),
        notifications: Boolean = true,
    ) = HomeViewModel(repo, engine, sys, notificationPermissionRequired = notifications)

    @Test
    fun `UI-1 enabling while requirements are missing still saves enabled=true and shows requirement cards`() = runTest {
        val repo = FakeSettingsRepository(Settings(enabled = false, mode = DetectionMode.ACCESSIBILITY, bindings = noMedia))
        val vm = vm(repo)
        vm.state.test {
            vm.setEnabled(true)
            assertTrue(repo.current.enabled)
            val state = expectMostRecentItem()
            assertTrue(state.enabled)
            assertEquals(
                listOf(
                    RequirementItem(RequirementType.ACCESSIBILITY, satisfied = false),
                    RequirementItem(RequirementType.BATTERY, satisfied = false),
                ),
                state.requirements,
            )
        }
    }

    @Test
    fun `UI-1 the switch can be turned off while permissions are missing`() = runTest {
        val repo = FakeSettingsRepository(Settings(enabled = true))
        vm(repo, MutableStateFlow(EngineStatus.AccessibilityRequired)).setEnabled(false)
        assertFalse(repo.current.enabled)
    }

    @Test
    fun `UI-2 status text maps 1-1 from EngineStatus`() {
        val texts = allStatuses.map { statusText(it) }
        assertEquals(allStatuses.size, texts.map { it.resId }.distinct().size)
        assertEquals(listOf("boom"), statusText(EngineStatus.Error("boom")).args)
        allStatuses.filterNot { it is EngineStatus.Error }.forEach {
            assertTrue(statusText(it).args.isEmpty(), "$it")
        }
    }

    @Test
    fun `UI-2 state follows engine status changes`() = runTest {
        val engine = MutableStateFlow<EngineStatus>(EngineStatus.Disabled)
        val vm = vm(engine = engine)
        vm.state.test {
            assertEquals(EngineStatus.Disabled, expectMostRecentItem().status)
            engine.value = EngineStatus.Running
            assertEquals(EngineStatus.Running, expectMostRecentItem().status)
        }
    }

    @Test
    fun `FR-D4 Shizuku mode lists Shizuku running, permission and notification requirements`() = runTest {
        val repo = FakeSettingsRepository(Settings(mode = DetectionMode.SHIZUKU, bindings = noMedia))
        val sys = FakeSystemStatus(Requirements(shizukuRunning = true, shizukuGranted = false, notificationsGranted = true))
        vm(repo, sys = sys).state.test {
            assertEquals(
                listOf(
                    RequirementItem(RequirementType.SHIZUKU_RUNNING, satisfied = true),
                    RequirementItem(RequirementType.SHIZUKU_PERMISSION, satisfied = false),
                    RequirementItem(RequirementType.NOTIFICATIONS, satisfied = true),
                    RequirementItem(RequirementType.BATTERY, satisfied = false),
                ),
                expectMostRecentItem().requirements,
            )
        }
    }

    @Test
    fun `FR-D4 Shizuku permission cannot be requested while Shizuku is not running`() = runTest {
        val repo = FakeSettingsRepository(Settings(mode = DetectionMode.SHIZUKU, bindings = noMedia))
        vm(repo, notifications = false).state.test {
            val reqs = expectMostRecentItem().requirements
            assertEquals(3, reqs.size)
            assertFalse(reqs.single { it.type == RequirementType.SHIZUKU_PERMISSION }.actionEnabled)
        }
    }

    @Test
    fun `FR-U1 changing mode persists it`() = runTest {
        val repo = FakeSettingsRepository()
        vm(repo).setMode(DetectionMode.SHIZUKU)
        assertEquals(DetectionMode.SHIZUKU, repo.current.mode)
    }

    @Test
    fun `FR-D4 refresh and permission request are forwarded to SystemStatus`() {
        val sys = FakeSystemStatus()
        val vm = vm(sys = sys)
        vm.refresh()
        vm.requestShizukuPermission()
        assertEquals(1, sys.refreshCalls)
        assertEquals(1, sys.permissionRequests)
    }

    @Test
    fun `UI-3 media control card is shown in both modes when a media action is bound`() = runTest {
        DetectionMode.entries.forEach { mode ->
            val bindings = noMedia + (Gesture.SWIPE_RIGHT to Action.MediaNext)
            val repo = FakeSettingsRepository(Settings(mode = mode, bindings = bindings))
            vm(repo, sys = FakeSystemStatus(Requirements(mediaAccessGranted = false))).state.test {
                val media = expectMostRecentItem().requirements.single { it.type == RequirementType.MEDIA_ACCESS }
                assertFalse(media.satisfied, "$mode")
            }
        }
    }

    @Test
    fun `UI-3 media control card reflects the granted state`() = runTest {
        val repo = FakeSettingsRepository(Settings(bindings = noMedia + (Gesture.TAP to Action.MediaPlayPause)))
        vm(repo, sys = FakeSystemStatus(Requirements(mediaAccessGranted = true))).state.test {
            assertTrue(expectMostRecentItem().requirements.single { it.type == RequirementType.MEDIA_ACCESS }.satisfied)
        }
    }

    @Test
    fun `UI-3 media control card is hidden when no media action is bound`() = runTest {
        val repo = FakeSettingsRepository(Settings(bindings = noMedia + (Gesture.TAP to Action.VolumeUp)))
        vm(repo).state.test {
            assertTrue(expectMostRecentItem().requirements.none { it.type == RequirementType.MEDIA_ACCESS })
        }
    }

    @Test
    fun `UI-3 a media action on a gesture disabled in Shizuku mode does not show the card`() = runTest {
        val repo = FakeSettingsRepository(
            Settings(mode = DetectionMode.SHIZUKU, bindings = noMedia + (Gesture.SWIPE_DOWN to Action.MediaPlayPause)),
        )
        vm(repo).state.test {
            assertTrue(expectMostRecentItem().requirements.none { it.type == RequirementType.MEDIA_ACCESS })
        }
    }

    @Test
    fun `UI-4 battery card is listed in both modes and follows the granted state`() = runTest {
        DetectionMode.entries.forEach { mode ->
            val sys = FakeSystemStatus(Requirements(batteryUnrestricted = false))
            vm(FakeSettingsRepository(Settings(mode = mode, bindings = noMedia)), sys = sys).state.test {
                assertFalse(expectMostRecentItem().requirements.single { it.type == RequirementType.BATTERY }.satisfied, "$mode")
                sys.requirements.value = Requirements(batteryUnrestricted = true)
                assertTrue(expectMostRecentItem().requirements.single { it.type == RequirementType.BATTERY }.satisfied, "$mode")
            }
        }
    }
}
