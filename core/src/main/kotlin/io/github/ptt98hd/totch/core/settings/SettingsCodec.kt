package io.github.ptt98hd.totch.core.settings

import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/** JSON persistence for [Settings]. Decoding never throws: any error yields defaults. */
object SettingsCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(s: Settings): String = json.encodeToString(Settings.serializer(), s)

    fun decode(json: String): Settings =
        runCatching {
            val tree = this.json.parseToJsonElement(json)
            this.json.decodeFromJsonElement(Settings.serializer(), tree).migrated(tree)
        }
            .getOrDefault(Settings())
            .sanitized()

    /**
     * ST-32: up to 1.8.0 a gesture was on when bound to an action and not in `disabledGestures`; 1.8.1 stores the
     * switches themselves in `enabledGestures` (FR-G15).
     */
    private fun Settings.migrated(tree: JsonElement): Settings {
        val obj = tree.jsonObject
        if ("enabledGestures" in obj) return this
        val disabled = obj["disabledGestures"]
            ?.let { json.decodeFromJsonElement<Set<Gesture>>(it) }
            .orEmpty()
        return copy(enabledGestures = Gesture.entries.filterTo(mutableSetOf()) { it !in disabled && actionFor(it) != Action.None })
    }
}
