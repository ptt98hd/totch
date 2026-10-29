package io.github.ptt98hd.totch.core.action

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/** How the one-handed mode is toggled (actions spec §10); the one that last worked is tried first. */
@Serializable
enum class OneHandedMethod {
    /** `one_handed_mode_activated`, with the one-handed shortcut set temporarily when missing. */
    @SerialName("system") SYSTEM,
    /** The launcher's swipe down on the bottom edge, injected with `input`. */
    @SerialName("gesture") GESTURE,
}

/**
 * Scripts run as uid shell. Exit codes: 0 done (or not verifiable), 2 no effect, 3 unsupported device,
 * 4 no gesture navigation.
 */
object OneHandedCommands {
    /** Common start: support check, `st` = WMShell `OneHandedState` (empty when unknown), target `t`, check `ok`. */
    private val prelude = """
        [ "$(getprop ro.support_one_handed_mode)" = true ] || exit 3
        st() { dumpsys activity service SystemUIService WMShell 2>/dev/null | sed -n 's/^ *sCurrentState=\([0-9]\).*$/\1/p' | head -n 1; }
        s=$(st)
        case "${'$'}s" in
          1|2) t=0 ;;
          0|3) t=1 ;;
          *) if [ "$(settings get secure one_handed_mode_activated)" = 1 ]; then t=0; else t=1; fi ;;
        esac
        ok() {
          [ -n "${'$'}s" ] || return 0
          for i in 1 2 3; do sleep 0.5; case "${'$'}t$(st)" in 11|12|00|03) return 0 ;; esac; done
          return 2
        }
    """.trimIndent()

    /** OH-1 */
    fun system(): String = prelude + "\n" + """
        k=accessibility_shortcut_target_service
        o=$(settings get secure ${'$'}k)
        a=
        restore() {
          [ -n "${'$'}a" ] || return 0
          if [ "${'$'}o" = null ] || [ -z "${'$'}o" ]; then settings delete secure ${'$'}k; else settings put secure ${'$'}k "${'$'}o"; fi
        }
        trap restore EXIT
        case "$(settings get secure accessibility_button_targets):${'$'}o" in
          *OneHandedMode*) ;;
          *) a=1
             n=$SHORTCUT_TARGET
             case "${'$'}o" in null|'') ;; *) n="${'$'}o:${'$'}n" ;; esac
             settings put secure ${'$'}k "${'$'}n"
             sleep 0.3 ;;
        esac
        settings put secure one_handed_mode_activated $((1-t))
        settings put secure one_handed_mode_activated ${'$'}t
        [ "$(settings get secure swipe_bottom_to_notification_enabled)" = 1 ] && exit 0
        ok
    """.trimIndent()

    /** OH-2: sizes in pixels of the display in its current rotation. */
    fun gesture(widthPx: Int, heightPx: Int, density: Float): String {
        if (widthPx <= 0 || heightPx <= 0 || density <= 0f) return "exit 2"
        val x = widthPx / 2
        val from = heightPx - (8 * density).roundToInt()
        val to = heightPx + (40 * density).roundToInt()
        val top = (heightPx * 0.15f).roundToInt()
        return prelude + "\n" + """
            [ "$(settings get secure navigation_mode)" = 2 ] || exit 4
            [ "$(settings get secure one_handed_mode_enabled)" = 1 ] || { settings put secure one_handed_mode_enabled 1; sleep 0.5; }
            if [ "${'$'}t" = 0 ]; then input tap $x $top; else input swipe $x $from $x $to 150; fi
            ok
        """.trimIndent()
    }

    private const val SHORTCUT_TARGET = "com.android.server.accessibility.OneHandedMode"
}

object OneHandedFallback {
    /** OH-3 */
    fun order(preferred: OneHandedMethod): List<OneHandedMethod> = listOf(preferred) + (OneHandedMethod.entries - preferred)

    /** OH-4: the first method for which [attempt] succeeds, or null when none does. */
    suspend fun run(preferred: OneHandedMethod, attempt: suspend (OneHandedMethod) -> Boolean): OneHandedMethod? =
        order(preferred).firstOrNull { method ->
            try {
                attempt(method)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        }
}
