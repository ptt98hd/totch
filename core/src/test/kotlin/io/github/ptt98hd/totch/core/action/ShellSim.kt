package io.github.ptt98hd.totch.core.action

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Runs a shell script with `sh` against stand-ins for the Android commands it uses (`settings`, `dumpsys`, `getprop`,
 * `input`, `cmd`, `sleep`), so the scripts sent through Shizuku can be checked on the JVM. Every call is logged.
 *
 * WMShell is simulated: writing `one_handed_mode_activated` moves [wmState] only while the one-handed shortcut is set
 * (actions spec §10) and [nativeWorks]; a bottom-edge swipe opens it and a tap closes it while [gestureWorks].
 */
class ShellSim {
    val secure = mutableMapOf<String, String>()
    val props = mutableMapOf("ro.support_one_handed_mode" to "true")
    /** `sCurrentState` printed by the WMShell dump; null = the dump has no one-handed section. */
    var wmState: Int? = 0
    var nativeWorks = true
    var gestureWorks = true
    /** `cmd` calls whose arguments contain one of these fail with exit 1. */
    val failing = mutableListOf<String>()
    var log: List<String> = emptyList()
        private set

    fun calls(name: String): List<String> = log.filter { it.startsWith("$name ") }

    fun run(script: String): Int {
        val dir = Files.createTempDirectory("shellsim").toFile()
        try {
            val bin = File(dir, "bin").apply { mkdirs() }
            val sec = File(dir, "secure").apply { mkdirs() }
            val prop = File(dir, "prop").apply { mkdirs() }
            secure.forEach { (k, v) -> File(sec, k).writeText(v) }
            props.forEach { (k, v) -> File(prop, k).writeText(v) }
            wmState?.let { File(dir, "wm").writeText(it.toString()) }
            if (nativeWorks) File(dir, "native").writeText("")
            if (gestureWorks) File(dir, "gesture").writeText("")
            File(dir, "failing").writeText(failing.joinToString("") { it + "\n" })
            File(dir, "log").writeText("")
            STUBS.forEach { (name, body) ->
                File(bin, name).apply { writeText("#!/bin/sh\nD=\"\$SIM\"\n$body\n"); setExecutable(true) }
            }
            val process = ProcessBuilder("sh", "-c", script).redirectErrorStream(true).also {
                it.environment()["SIM"] = dir.path
                it.environment()["PATH"] = bin.path + File.pathSeparator + System.getenv("PATH")
            }.start()
            process.inputStream.readBytes()
            check(process.waitFor(10, TimeUnit.SECONDS)) { "script timed out" }
            secure.clear()
            sec.listFiles()!!.forEach { secure[it.name] = it.readText() }
            wmState = File(dir, "wm").takeIf { it.exists() }?.readText()?.trim()?.toInt()
            log = File(dir, "log").readLines().filter { it.isNotBlank() }
            return process.exitValue()
        } finally {
            dir.deleteRecursively()
        }
    }

    private companion object {
        val STUBS = mapOf(
            "settings" to """
                echo "settings $*" >> "${'$'}D/log"
                f="${'$'}D/secure/${'$'}3"
                case "${'$'}1" in
                get) if [ -f "${'$'}f" ]; then cat "${'$'}f"; echo; else echo null; fi ;;
                delete) rm -f "${'$'}f" ;;
                put)
                  printf %s "${'$'}4" > "${'$'}f"
                  if [ "${'$'}3" = one_handed_mode_activated ] && [ -f "${'$'}D/wm" ] && [ -f "${'$'}D/native" ]; then
                    sc="$(cat "${'$'}D/secure/accessibility_button_targets" 2>/dev/null):$(cat "${'$'}D/secure/accessibility_shortcut_target_service" 2>/dev/null)"
                    nt="$(cat "${'$'}D/secure/swipe_bottom_to_notification_enabled" 2>/dev/null)"
                    case "${'$'}sc" in *OneHandedMode*)
                      if [ "${'$'}nt" != 1 ]; then if [ "${'$'}4" = 1 ]; then echo 2; else echo 0; fi > "${'$'}D/wm"; fi ;;
                    esac
                  fi ;;
                esac
            """.trimIndent(),
            "dumpsys" to """
                echo "dumpsys $*" >> "${'$'}D/log"
                echo "OneHandedController"
                if [ -f "${'$'}D/wm" ]; then echo "OneHandedState"; echo "  sCurrentState=$(cat "${'$'}D/wm")"; fi
            """.trimIndent(),
            "getprop" to """
                if [ -f "${'$'}D/prop/${'$'}1" ]; then cat "${'$'}D/prop/${'$'}1"; fi
                echo
            """.trimIndent(),
            "input" to """
                echo "input $*" >> "${'$'}D/log"
                [ -f "${'$'}D/gesture" ] && [ -f "${'$'}D/wm" ] || exit 0
                case "${'$'}1" in
                swipe) [ "$(cat "${'$'}D/secure/navigation_mode" 2>/dev/null)" = 2 ] && \
                  [ "$(cat "${'$'}D/secure/one_handed_mode_enabled" 2>/dev/null)" = 1 ] && echo 2 > "${'$'}D/wm" ;;
                tap) echo 0 > "${'$'}D/wm" ;;
                esac
                exit 0
            """.trimIndent(),
            "cmd" to """
                echo "cmd $*" >> "${'$'}D/log"
                while read -r f; do
                  [ -n "${'$'}f" ] && case "$*" in *"${'$'}f"*) exit 1 ;; esac
                done < "${'$'}D/failing"
                exit 0
            """.trimIndent(),
            "sleep" to "exit 0",
        )
    }
}
