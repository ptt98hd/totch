package io.github.ptt98hd.totch.ui.settings

import io.github.ptt98hd.totch.InstallResult
import io.github.ptt98hd.totch.core.settings.AppLanguage
import io.github.ptt98hd.totch.core.settings.ThemeMode
import io.github.ptt98hd.totch.core.settings.UpdateInterval
import io.github.ptt98hd.totch.core.update.Release
import io.github.ptt98hd.totch.core.update.SemVer
import io.github.ptt98hd.totch.ui.FakeAppUpdater
import io.github.ptt98hd.totch.ui.FakeSettingsRepository
import io.github.ptt98hd.totch.ui.MainDispatcherRule
import io.github.ptt98hd.totch.update.UpdateController
import io.github.ptt98hd.totch.update.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val updater = FakeAppUpdater()
    private val release = Release(SemVer(1, 6, 0), "* Notes", "https://github.com/ptt98hd/totch/releases/download/v1.6.0/totch-1.6.0.apk", 10, null)

    private fun vm(repo: FakeSettingsRepository = FakeSettingsRepository(), version: String = "1.5.0") =
        AppSettingsViewModel(repo, UpdateController(updater, version, CoroutineScope(main.dispatcher)))

    @Test
    fun `UI-32 appearance, language and boot setters persist`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = vm(repo)
        vm.setTheme(ThemeMode.LIGHT)
        vm.setAmoledBlack(true)
        vm.setLanguage(AppLanguage.ENGLISH)
        vm.setStartOnBoot(false)
        vm.setHaptics(false)
        vm.setLandscapeGestures(true)
        with(repo.current) {
            assertFalse(haptics)
            assertTrue(landscapeGestures)
            assertEquals(ThemeMode.LIGHT, theme)
            assertTrue(amoledBlack)
            assertEquals(AppLanguage.ENGLISH, language)
            assertFalse(startOnBoot)
        }
    }

    @Test
    fun `UP-13 update settings persist`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = vm(repo)
        vm.setUpdateOnLaunch(true)
        vm.setUpdateInterval(UpdateInterval.MONTHLY)
        assertTrue(repo.current.updateOnLaunch)
        assertEquals(UpdateInterval.MONTHLY, repo.current.updateInterval)
    }

    @Test
    fun `UP-5 starts idle without touching the network`() {
        val vm = vm()
        assertEquals(UpdateState.Idle, vm.state.value.update)
        assertFalse(vm.state.value.updateDialog)
        assertEquals(0, updater.checks)
    }

    @Test
    fun `UP-6 check reports available, up to date or failure`() {
        val vm = vm()
        vm.onUpdateRowClick()
        assertEquals(UpdateState.Checking, vm.state.value.update)
        vm.onUpdateRowClick() // ignored while checking
        assertEquals(1, updater.checks)
        updater.latest.complete(release)
        assertEquals(UpdateState.Available(release), vm.state.value.update)
        assertTrue(vm.state.value.updateDialog)
        vm.dismissUpdateDialog()
        assertFalse(vm.state.value.updateDialog)
        vm.onUpdateRowClick() // Available: only reopens the dialog
        assertTrue(vm.state.value.updateDialog)
        assertEquals(1, updater.checks)

        val current = vm(version = "1.6.0")
        updater.latest = CompletableDeferred(release)
        current.onUpdateRowClick()
        assertEquals(UpdateState.UpToDate, current.state.value.update)
        assertFalse(current.state.value.updateDialog)

        updater.latest = CompletableDeferred<Release?>().apply { complete(null) }
        current.onUpdateRowClick()
        assertEquals(UpdateState.UpToDate, current.state.value.update)

        updater.latest = CompletableDeferred<Release?>().apply { completeExceptionally(IOException("offline")) }
        current.onUpdateRowClick()
        assertEquals(UpdateState.Failed, current.state.value.update)
    }

    @Test
    fun `UP-7 update downloads with progress then hands over to the installer`() {
        val vm = vm()
        updater.latest.complete(release)
        vm.onUpdateRowClick()
        vm.startUpdate()
        assertTrue(vm.state.value.updateDialog) // UP-20: stays open to show the progress
        assertEquals(UpdateState.Downloading(0f), vm.state.value.update)
        updater.progress(0.5f)
        assertEquals(UpdateState.Downloading(0.5f), vm.state.value.update)
        vm.onUpdateRowClick() // ignored while downloading
        assertEquals(1, updater.checks)
        updater.install.complete(InstallResult.SYSTEM_INSTALLER)
        assertEquals(UpdateState.Installing(viaShizuku = false), vm.state.value.update)

        updater.latest = CompletableDeferred(release)
        updater.install = CompletableDeferred(InstallResult.FAILED)
        vm.onUpdateRowClick()
        vm.startUpdate()
        assertEquals(UpdateState.Failed, vm.state.value.update)
    }
}
