package io.github.ptt98hd.totch.core.settings

import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.overlay.IslandActivity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
            val tree = this.json.parseToJsonElement(json).withoutVibrate()
            this.json.decodeFromJsonElement(Settings.serializer(), tree).migrated(tree).withIndicator(tree)
        }
            .getOrDefault(Settings())
            .sanitized()

    /** ST-35: 1.8.3 dropped the vibrate sound mode; bindings to it become silent. */
    private fun JsonElement.withoutVibrate(): JsonElement = when (this) {
        is JsonObject ->
            if ((this["type"] as? JsonPrimitive)?.content == "ringer_vibrate") {
                JsonObject(mapOf("type" to JsonPrimitive("ringer_silent")))
            } else {
                JsonObject(mapValues { it.value.withoutVibrate() })
            }
        is JsonArray -> JsonArray(map { it.withoutVibrate() })
        else -> this
    }

    /** ST-39: 1.9.1 folded the notification indicator switch into the island's notifications activity (FR-O12). */
    private fun Settings.withIndicator(tree: JsonElement): Settings {
        val indicator = (tree.jsonObject["notificationIndicator"] as? JsonPrimitive)?.content == "true"
        return when {
            !indicator -> this
            island -> copy(islandActivities = islandActivities + IslandActivity.NOTIFICATIONS)
            else -> copy(island = true, islandActivities = setOf(IslandActivity.NOTIFICATIONS))
        }
    }

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
