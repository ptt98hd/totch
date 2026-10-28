package io.github.ow3n.totch.core.settings

import io.github.ow3n.totch.core.model.Action
import io.github.ow3n.totch.core.model.Gesture
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsCodecTest {
    @Test fun `ST-4 encode decode round trip`() {
        val samples = listOf(
            Settings(),
            Settings(
                enabled = true,
                mode = DetectionMode.SHIZUKU,
                bindings = mapOf(
                    Gesture.TAP to Action.LaunchApp("com.example"),
                    Gesture.DOUBLE_TAP to Action.None,
                    Gesture.LONG_PRESS to Action.Screenshot,
                    Gesture.SWIPE_DOWN to Action.QuickSettings,
                    Gesture.SWIPE_LEFT to Action.VolumeDown,
                    Gesture.SWIPE_RIGHT to Action.MediaPlayPause,
                ),
                doubleTapMs = 200, longPressMs = 800, swipeMinDistanceDp = 50, paddingDp = 0,
                manualRegion = ManualRegionDp(0.3f, 100, 40),
                haptics = false,
            ),
        )
        samples.forEach { assertEquals(it, SettingsCodec.decode(SettingsCodec.encode(it))) }
    }

    @Test fun `ST-5 malformed input decodes to defaults`() {
        listOf(
            "", "   ", "{", "null", "[]", "42", "not json",
            """{"enabled":"yes"}""",
            """{"bindings":{"TAP":{"type":"teleport"}}}""",
            """{"mode":"ROOT"}""",
        ).forEach { assertEquals(Settings(), SettingsCodec.decode(it), it) }
    }

    @Test fun `ST-6 unknown keys ignored and result sanitized`() {
        val s = SettingsCodec.decode(
            """{"enabled":true,"futureField":{"a":1},"longPressMs":5,"bindings":{"TAP":{"type":"home","extra":1}}}""",
        )
        assertEquals(true, s.enabled)
        assertEquals(300, s.longPressMs)
        assertEquals(Action.Home, s.bindings[Gesture.TAP])
        assertEquals(Action.None, s.bindings[Gesture.LONG_PRESS])
        assertEquals(Gesture.entries.toSet(), s.bindings.keys)
    }

    @Test fun `ST-8 stable serial names`() {
        val json = Json.parseToJsonElement(
            SettingsCodec.encode(Settings(bindings = mapOf(Gesture.TAP to Action.Back, Gesture.LONG_PRESS to Action.LaunchApp("com.a")))),
        ).jsonObject
        val bindings = json.getValue("bindings").jsonObject
        assertEquals(JsonObject(mapOf("type" to kotlinx.serialization.json.JsonPrimitive("back"))), bindings.getValue("TAP"))
        assertEquals("launch_app", bindings.getValue("LONG_PRESS").jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals("com.a", bindings.getValue("LONG_PRESS").jsonObject.getValue("packageName").jsonPrimitive.content)

        val names = mapOf(
            Action.None to "none", Action.Back to "back", Action.Home to "home", Action.Recents to "recents",
            Action.LockScreen to "lock", Action.Screenshot to "screenshot", Action.Notifications to "notifications",
            Action.QuickSettings to "quick_settings", Action.ToggleTorch to "torch", Action.MediaPlayPause to "play_pause",
            Action.MediaNext to "next", Action.MediaPrevious to "previous", Action.VolumeUp to "volume_up",
            Action.VolumeDown to "volume_down",
        )
        names.forEach { (action, name) ->
            val encoded = Json.encodeToString(Action.serializer(), action)
            assertEquals("""{"type":"$name"}""", encoded)
        }
        assertEquals("ACCESSIBILITY", json.getValue("mode").jsonPrimitive.content)
    }
}
