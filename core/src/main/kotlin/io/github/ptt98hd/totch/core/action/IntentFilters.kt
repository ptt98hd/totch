package io.github.ptt98hd.totch.core.action

import io.github.ptt98hd.totch.core.model.IntentTarget

/** An `<intent-filter>` of a manifest component: its actions and whether it has `<data>`. */
data class ManifestFilter(val actions: List<String>, val hasData: Boolean)

/** Which manifest intents Totch offers (actions spec §16). */
object IntentFilters {
    private const val MAIN = "android.intent.action.MAIN"

    /** IN-1..IN-3: actions of the filters without data, deduplicated; MAIN and system broadcasts are dropped. */
    fun usableActions(target: IntentTarget, filters: List<ManifestFilter>): List<String> =
        filters.asSequence()
            .filter { !it.hasData }
            .flatMap { it.actions }
            .filter { IntentActionRegex.matches(it) }
            .filter {
                when (target) {
                    IntentTarget.ACTIVITY -> it != MAIN
                    IntentTarget.BROADCAST -> !it.startsWith("android.") && !it.startsWith("com.android.")
                }
            }
            .distinct()
            .toList()

    /** IN-4: manifest class names may be relative to the package. */
    fun resolveClassName(packageName: String, name: String): String = when {
        name.startsWith(".") -> packageName + name
        '.' !in name -> "$packageName.$name"
        else -> name
    }

    /** IN-5 */
    fun shortIntentAction(action: String): String = action.substringAfterLast('.')
}
