package io.github.ow3n.totch.ui.home

import app.cash.turbine.test
import io.github.ow3n.totch.EngineStatus
import io.github.ow3n.totch.Requirements
import io.github.ow3n.totch.core.settings.DetectionMode
import io.github.ow3n.totch.core.settings.Settings
import io.github.ow3n.totch.ui.FakeSettingsRepository
import io.github.ow3n.totch.ui.FakeSystemStatus
import io.github.ow3n.totch.ui.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

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
        val repo = FakeSettingsRepository(Settings(enabled = false, mode = DetectionMode.ACCESSIBILITY))
        val vm = vm(repo)
        vm.state.test {
            vm.setEnabled(true)
            assertTrue(repo.current.enabled)
            val state = expectMostRecentItem()
            assertTrue(state.enabled)
            assertEquals(listOf(RequirementItem(RequirementType.ACCESSIBILITY, satisfied = false)), state.requirements)
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
        val repo = FakeSettingsRepository(Settings(mode = DetectionMode.SHIZUKU))
        val sys = FakeSystemStatus(Requirements(shizukuRunning = true, shizukuGranted = false, notificationsGranted = true))
        vm(repo, sys = sys).state.test {
            assertEquals(
                listOf(
                    RequirementItem(RequirementType.SHIZUKU_RUNNING, satisfied = true),
                    RequirementItem(RequirementType.SHIZUKU_PERMISSION, satisfied = false),
                    RequirementItem(RequirementType.NOTIFICATIONS, satisfied = true),
                ),
                expectMostRecentItem().requirements,
            )
        }
    }

    @Test
    fun `FR-D4 Shizuku permission cannot be requested while Shizuku is not running`() = runTest {
        val repo = FakeSettingsRepository(Settings(mode = DetectionMode.SHIZUKU))
        vm(repo, notifications = false).state.test {
            val reqs = expectMostRecentItem().requirements
            assertEquals(2, reqs.size)
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
    fun `onboarding is shown only while no detection permission is granted`() = runTest {
        val sys = FakeSystemStatus()
        vm(sys = sys).state.test {
            assertTrue(expectMostRecentItem().showOnboarding)
            sys.requirements.value = Requirements(accessibilityEnabled = true)
            assertFalse(expectMostRecentItem().showOnboarding)
        }
    }
}
