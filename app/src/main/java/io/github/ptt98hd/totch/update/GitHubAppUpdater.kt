package io.github.ptt98hd.totch.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import io.github.ptt98hd.totch.AppUpdater
import io.github.ptt98hd.totch.BuildConfig
import io.github.ptt98hd.totch.InstallResult
import io.github.ptt98hd.totch.core.update.Release
import io.github.ptt98hd.totch.core.update.Releases
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * Self-update from the GitHub release of the latest tag (update spec §4); network only on request. The verified APK
 * goes to [shizukuInstall] first, then to the system installer (§7).
 */
class GitHubAppUpdater(
    private val context: Context,
    private val shizukuInstall: suspend (File) -> Boolean,
) : AppUpdater {

    override suspend fun latestRelease(): Release? = withContext(Dispatchers.IO) {
        val connection = open(Releases.LATEST_URL).apply { setRequestProperty("Accept", "application/vnd.github+json") }
        try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Releases.parseLatest(connection.inputStream.bufferedReader().use { it.readText() })
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException("HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun downloadAndInstall(release: Release, onProgress: (Float) -> Unit): InstallResult = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
        val apk = File(dir, "totch-${release.version}.apk")
        try {
            download(release, apk, onProgress)
            check(isExpectedPackage(apk)) { "unexpected package" }
            if (shizukuInstall(apk)) {
                InstallResult.SHIZUKU
            } else {
                install(apk)
                InstallResult.SYSTEM_INSTALLER
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) { // network, verification, installer session
            Log.w(TAG, "Update failed", e)
            InstallResult.FAILED
        } finally {
            apk.delete()
        }
    }

    /** UP-8: size and SHA-256 must match the release before anything is installed. */
    private suspend fun download(release: Release, target: File, onProgress: (Float) -> Unit) {
        val connection = open(release.apkUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${connection.responseCode}")
            val digest = MessageDigest.getInstance("SHA-256")
            var read = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        read += n
                        if (read > release.apkSize) throw IOException("larger than expected")
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        onProgress(read.toFloat() / release.apkSize)
                    }
                }
            }
            if (read != release.apkSize) throw IOException("size $read != ${release.apkSize}")
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (release.sha256 != null && hash != release.sha256) throw IOException("sha256 mismatch")
        } finally {
            connection.disconnect()
        }
    }

    /** UP-8: same package and a higher version code; the system itself rejects a different signature. */
    private fun isExpectedPackage(apk: File): Boolean {
        val info = context.packageManager.getPackageArchiveInfo(apk.path, 0) ?: return false
        return info.packageName == context.packageName && info.longVersionCode > BuildConfig.VERSION_CODE
    }

    private fun install(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        try {
            installer.openSession(id).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("totch.apk", 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                // Mutable: the installer adds the status and, if needed, the confirmation intent.
                val status = PendingIntent.getBroadcast(
                    context,
                    id,
                    Intent(context, UpdateInstallReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(status.intentSender)
            }
        } catch (e: Exception) {
            installer.abandonSession(id)
            throw e
        }
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        setRequestProperty("User-Agent", "Totch/${BuildConfig.VERSION_NAME}")
    }

    private companion object {
        const val TAG = "Totch/Update"
        const val TIMEOUT_MS = 15_000
    }
}
