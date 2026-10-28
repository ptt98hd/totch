package io.github.ptt98hd.totch.core.settings

import kotlinx.serialization.json.Json

/** JSON persistence for [Settings]. Decoding never throws: any error yields defaults. */
object SettingsCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(s: Settings): String = json.encodeToString(Settings.serializer(), s)

    fun decode(json: String): Settings =
        runCatching { this.json.decodeFromString(Settings.serializer(), json) }
            .getOrDefault(Settings())
            .sanitized()
}
