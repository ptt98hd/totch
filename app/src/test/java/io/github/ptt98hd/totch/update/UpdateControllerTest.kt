package io.github.ptt98hd.totch.update

import io.github.ptt98hd.totch.InstallResult
import io.github.ptt98hd.totch.core.update.Release
import io.github.ptt98hd.totch.core.update.SemVer
import io.github.ptt98hd.totch.ui.FakeAppUpdater
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateControllerTest {
    private val updater = FakeAppUpdater()
    private val release = Release(SemVer(1, 7, 0), "* Notes", "https://github.com/ptt98hd/totch/releases/download/v1.7.0/totch-1.7.0.apk", 10, null)

    private fun controller(version: String = "1.6.1") = UpdateController(updater, version, CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun `UP-9 a silent check only shows the dialog for a newer release`() {
        val c = controller()
        c.check(silent = true)
        assertEquals(UpdateState.Idle, c.state.value.state) // no "Checking…" on the Settings row
        updater.latest.complete(release)
        assertEquals(UpdateState.Available(release), c.state.value.state)
        assertTrue(c.state.value.dialog)

        c.dismissDialog()
        c.check(silent = true) // Available: reopens the dialog without a new request
        assertTrue(c.state.value.dialog)
        assertEquals(1, updater.checks)
    }

    @Test
    fun `UP-9 a silent check keeps the state when there is nothing new or it fails`() {
        val c = controller(version = "1.7.0")
        updater.latest.complete(release)
        c.check(silent = true)
        assertEquals(UpdateState.Idle, c.state.value.state)
        assertFalse(c.state.value.dialog)

        updater.latest = CompletableDeferred<Release?>().apply { completeExceptionally(IOException("offline")) }
        c.check(silent = true)
        assertEquals(UpdateState.Idle, c.state.value.state)

        updater.latest = CompletableDeferred<Release?>().apply { complete(null) }
        c.check() // a manual check still reports the result
        assertEquals(UpdateState.UpToDate, c.state.value.state)
    }

    @Test
    fun `UP-9 a silent check is skipped while a manual check or download runs`() {
        val c = controller()
        c.check()
        c.check(silent = true)
        assertEquals(1, updater.checks)
        assertEquals(UpdateState.Checking, c.state.value.state)
    }

    @Test
    fun `UP-16 the install path decides the Installing state`() {
        val c = controller()
        updater.latest.complete(release)
        c.check()
        c.startUpdate()
        updater.install.complete(InstallResult.SHIZUKU)
        assertEquals(UpdateState.Installing(viaShizuku = true), c.state.value.state)

        updater.latest = CompletableDeferred(release)
        updater.install = CompletableDeferred<InstallResult>().apply { completeExceptionally(IOException("disk")) }
        c.check()
        c.startUpdate()
        assertEquals(UpdateState.Failed, c.state.value.state)
    }

    @Test
    fun `UP-17 a failed Shizuku install is reported`() {
        val c = controller()
        updater.latest.complete(release)
        c.check()
        c.startUpdate()
        updater.install.complete(InstallResult.SHIZUKU)
        c.installFailed()
        assertEquals(UpdateUi(UpdateState.Failed), c.state.value)
    }
}
