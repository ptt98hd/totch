package io.github.ptt98hd.totch.ui.home

import app.cash.turbine.test
import io.github.ptt98hd.totch.EngineStatus
import io.github.ptt98hd.totch.Permission
import io.github.ptt98hd.totch.Requirements
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

    private val allStatuses = listOf(
        EngineStatus.Disabled,
        EngineStatus.NeedsAccess,
        EngineStatus.Starting,
        EngineStatus.Stopping,
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
    fun `UI-1 enabling while Shizuku is missing still saves enabled=true`() = runTest {
        val repo = FakeSettingsRepository(Settings(enabled = false))
        val vm = vm(repo)
        vm.state.test {
            vm.setEnabled(true)
            assertTrue(repo.current.enabled)
            val state = expectMostRecentItem()
            assertTrue(state.enabled)
            assertEquals(ShizukuStatus.NOT_RUNNING, state.shizuku)
        }
    }

    @Test
    fun `UI-1 the switch can be turned off while Shizuku is missing`() = runTest {
        val repo = FakeSettingsRepository(Settings(enabled = true))
        vm(repo, MutableStateFlow(EngineStatus.NeedsAccess)).setEnabled(false)
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
    fun `UI-50 Shizuku requirement follows running and granted`() = runTest {
        val sys = FakeSystemStatus(Requirements(shizukuRunning = false))
        vm(sys = sys).state.test {
            assertEquals(ShizukuStatus.NOT_RUNNING, expectMostRecentItem().shizuku)
            sys.requirements.value = Requirements(shizukuRunning = true, shizukuGranted = false)
            assertEquals(ShizukuStatus.PERMISSION_REQUIRED, expectMostRecentItem().shizuku)
            sys.requirements.value = Requirements(shizukuRunning = true, shizukuGranted = true)
            assertEquals(ShizukuStatus.READY, expectMostRecentItem().shizuku)
        }
    }

    @Test
    fun `UI-51 permissions are listed in order with their granted state`() = runTest {
        val sys = FakeSystemStatus(
            Requirements(
                accessibilityEnabled = true, notificationsGranted = false, mediaAccessGranted = true,
                batteryUnrestricted = false, installUpdatesAllowed = true, dndAccessGranted = true, keepWhenUnused = true,
            ),
        )
        vm(sys = sys).state.test {
            assertEquals(
                listOf(
                    PermissionItem(Permission.ACCESSIBILITY, granted = true),
                    PermissionItem(Permission.NOTIFICATIONS, granted = false),
                    PermissionItem(Permission.MEDIA_ACCESS, granted = true),
                    PermissionItem(Permission.DND_ACCESS, granted = true),
                    PermissionItem(Permission.BATTERY, granted = false),
                    PermissionItem(Permission.KEEP_WHEN_UNUSED, granted = true),
                    PermissionItem(Permission.INSTALL_UPDATES, granted = true),
                ),
                expectMostRecentItem().permissions,
            )
            sys.requirements.value = Requirements(batteryUnrestricted = true)
            assertTrue(expectMostRecentItem().permissions.single { it.permission == Permission.BATTERY }.granted)
        }
    }

    @Test
    fun `UI-51 notifications are not listed before Android 13`() = runTest {
        vm(notifications = false).state.test {
            assertTrue(expectMostRecentItem().permissions.none { it.permission == Permission.NOTIFICATIONS })
        }
    }

    @Test
    fun `UI-52 switching a permission goes through Shizuku first, then falls back to system settings`() = runTest {
        val sys = FakeSystemStatus()
        val vm = vm(sys = sys)
        val fallbacks = mutableListOf<Permission>()

        sys.shizukuResult = true
        vm.setPermission(Permission.BATTERY, true, fallbacks::add)
        assertEquals(listOf(Permission.BATTERY to true), sys.permissionCalls)
        assertTrue(fallbacks.isEmpty())

        sys.shizukuResult = false
        vm.setPermission(Permission.ACCESSIBILITY, false, fallbacks::add)
        assertEquals(Permission.ACCESSIBILITY to false, sys.permissionCalls.last())
        assertEquals(listOf(Permission.ACCESSIBILITY), fallbacks)
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
    fun `UI-42 master icon is on, off or pending`() {
        allStatuses.forEach { status ->
            val expected = when (status) {
                EngineStatus.Running -> MasterIcon.ON
                EngineStatus.Starting, EngineStatus.Stopping -> MasterIcon.PENDING
                else -> MasterIcon.OFF
            }
            assertEquals(expected, masterIcon(status), "$status")
        }
    }

    @Test
    fun `UI-53 only switching notifications or install updates off asks for confirmation`() {
        assertTrue(needsConfirmation(Permission.NOTIFICATIONS, granted = false))
        assertTrue(needsConfirmation(Permission.INSTALL_UPDATES, granted = false))
        assertFalse(needsConfirmation(Permission.INSTALL_UPDATES, granted = true))
        assertFalse(needsConfirmation(Permission.NOTIFICATIONS, granted = true))
        assertFalse(needsConfirmation(Permission.ACCESSIBILITY, granted = false))
        assertFalse(needsConfirmation(Permission.BATTERY, granted = false))
    }
}
