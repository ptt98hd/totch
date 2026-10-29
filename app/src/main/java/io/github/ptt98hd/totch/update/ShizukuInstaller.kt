package io.github.ptt98hd.totch.update

import android.content.Context
import android.os.ParcelFileDescriptor
import io.github.ptt98hd.totch.core.update.UpdateCommands
import io.github.ptt98hd.totch.shizuku.ShizukuShell
import io.github.ptt98hd.totch.ui.MainActivity
import java.io.File

/**
 * Update spec §7 (FR-U36): the shell user service copies the APK where `pm` can read it, then a detached `pm install`
 * replaces Totch and reopens it. False when Shizuku is not ready or the hand-over failed.
 */
class ShizukuInstaller(private val context: Context, private val shell: ShizukuShell) {
    suspend fun install(apk: File): Boolean {
        val command = UpdateCommands.shellInstall(context.packageName, MainActivity::class.java.name, UpdateCommands.APK_PATH)
            ?: return false
        return shell.withService { service ->
            val copied = ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY).use {
                service.writeFile(it, UpdateCommands.APK_PATH) == 0
            }
            copied && service.exec(command) == 0
        }
    }
}
