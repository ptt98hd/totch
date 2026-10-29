package io.github.ptt98hd.totch.core.settings

import io.github.ptt98hd.totch.core.action.OneHandedMethod
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SettingsCodecTest {
    @Test fun `ST-4 encode decode round trip`() {
        val samples = listOf(
            Settings(),
            Settings(
                enabled = true,
                bindings = DefaultBindings + mapOf(
                    Gesture.TAP to Action.LaunchApp("com.example"),
                    Gesture.DOUBLE_TAP to Action.None,
                    Gesture.LONG_PRESS to Action.Screenshot,
                    Gesture.SWIPE_DOWN to Action.QuickSettings,
                    Gesture.SWIPE_LEFT to Action.VolumeDown,
                    Gesture.SWIPE_RIGHT to Action.MediaPlayPause,
                ),
                doubleTapMs = 200, longPressMs = 800, swipeMinDistanceDp = 50, paddingDp = 1,
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
            """{"theme":"sepia"}""",
        ).forEach { assertEquals(Settings(), SettingsCodec.decode(it), it) }
    }

    @Test fun `ST-6 unknown keys ignored and result sanitized`() {
        val s = SettingsCodec.decode(
            """{"enabled":true,"futureField":{"a":1},"longPressMs":5,"bindings":{"TAP":{"type":"home","extra":1}}}""",
        )
        assertEquals(true, s.enabled)
        assertEquals(100, s.longPressMs)
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
        assertFalse("mode" in json)
    }

    @Test fun `ST-23 json with the removed mode key still decodes`() {
        listOf("SHIZUKU", "ACCESSIBILITY").forEach { mode ->
            val s = SettingsCodec.decode("""{"enabled":true,"mode":"$mode","paddingDp":48}""")
            assertEquals(Settings(enabled = true, paddingDp = 10), s)
        }
    }

    @Test fun `ST-11 v1_0 json keeps bindings and defaults new fields`() {
        val s = SettingsCodec.decode(
            """{"enabled":true,"mode":"SHIZUKU","bindings":{"TAP":{"type":"home"},"DOUBLE_TAP":{"type":"lock"},""" +
                """"LONG_PRESS":{"type":"torch"},"SWIPE_DOWN":{"type":"notifications"},"SWIPE_LEFT":{"type":"previous"},""" +
                """"SWIPE_RIGHT":{"type":"next"}},"doubleTapMs":300,"longPressMs":500,"swipeMinDistanceDp":40,""" +
                """"paddingDp":8,"manualRegion":null,"haptics":true}""",
        )
        assertEquals(true, s.enabled)
        assertEquals(Action.Home, s.bindings[Gesture.TAP])
        assertEquals(Action.None, s.bindings[Gesture.SWIPE_RIGHT_HOLD])
        assertEquals(
            Settings().copy(enabled = true, bindings = DefaultBindings + (Gesture.TAP to Action.Home)),
            s,
        )
    }

    @Test fun `ST-16 new actions and enums round trip with stable names`() {
        val shortcut = Action.LaunchShortcut("com.a", "compose", "New message", "intent:#Intent;component=com.a/.B;end")
        val s = Settings(
            bindings = DefaultBindings + mapOf(Gesture.TAP to Action.Assistant, Gesture.SWIPE_LEFT_HOLD to shortcut),
            theme = ThemeMode.DARK, amoledBlack = true, language = AppLanguage.VIETNAMESE, startOnBoot = false,
        )
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        ThemeMode.entries.forEach { t -> assertEquals(t, SettingsCodec.decode(SettingsCodec.encode(Settings(theme = t))).theme) }
        AppLanguage.entries.forEach { l -> assertEquals(l, SettingsCodec.decode(SettingsCodec.encode(Settings(language = l))).language) }

        val json = Json.parseToJsonElement(SettingsCodec.encode(s)).jsonObject
        assertEquals("dark", json.getValue("theme").jsonPrimitive.content)
        assertEquals("vi", json.getValue("language").jsonPrimitive.content)
        val bindings = json.getValue("bindings").jsonObject
        assertEquals("""{"type":"assistant"}""", bindings.getValue("TAP").toString())
        assertEquals("launch_shortcut", bindings.getValue("SWIPE_LEFT_HOLD").jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals("system", Json.parseToJsonElement(SettingsCodec.encode(Settings())).jsonObject.getValue("language").jsonPrimitive.content)
        assertEquals("en", Json.parseToJsonElement(SettingsCodec.encode(Settings(language = AppLanguage.ENGLISH))).jsonObject.getValue("language").jsonPrimitive.content)
    }

    @Test fun `ST-17 v1_1 manual region without topDp decodes with topDp 0`() {
        val s = SettingsCodec.decode("""{"manualRegion":{"centerXFraction":0.3,"widthDp":100,"heightDp":40}}""")
        assertEquals(ManualRegionDp(0.3f, 100, 40, topDp = 0), s.manualRegion)
        val offset = Settings(manualRegion = ManualRegionDp(0.3f, 100, 40, topDp = 12))
        assertEquals(offset, SettingsCodec.decode(SettingsCodec.encode(offset)))
    }

    @Test fun `ST-18 v1_2 JSON with repeat fields still decodes`() {
        val s = SettingsCodec.decode(
            """{"bindings":{"TAP":{"type":"home"}},"repeatOnHold":["LONG_PRESS"],"repeatIntervalMs":500}""",
        )
        assertEquals(Action.Home, s.bindings[Gesture.TAP])
        assertEquals(emptySet(), s.disabledGestures)
    }

    @Test fun `ST-21 disabled gestures and volume panel round trip`() {
        val s = Settings(
            bindings = DefaultBindings + (Gesture.TAP to Action.VolumePanel),
            disabledGestures = setOf(Gesture.TAP, Gesture.SWIPE_DOWN),
        )
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        val json = Json.parseToJsonElement(SettingsCodec.encode(s)).jsonObject
        assertEquals("""{"type":"volume_panel"}""", json.getValue("bindings").jsonObject.getValue("TAP").toString())
    }

    @Test fun `ST-25 landscape gestures and activities round trip`() {
        assertFalse(SettingsCodec.decode("""{"bindings":{"TAP":{"type":"home"}}}""").landscapeGestures)
        val s = Settings(
            landscapeGestures = true,
            bindings = DefaultBindings + (Gesture.TAP to Action.LaunchActivity("com.a", "com.a.Main\$Sub", "Main")),
        )
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        val json = Json.parseToJsonElement(SettingsCodec.encode(s)).jsonObject
        assertEquals("launch_activity", json.getValue("bindings").jsonObject.getValue("TAP").jsonObject.getValue("type").jsonPrimitive.content)
    }

    @Test fun `ST-27 quick settings tiles round trip`() {
        val s = Settings(
            bindings = DefaultBindings + (Gesture.TAP to Action.ClickTile("com.a", "com.a.qs.Tile", "Tile")) +
                (Gesture.LONG_PRESS to Action.OneHandedMode),
        )
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        val json = Json.parseToJsonElement(SettingsCodec.encode(s)).jsonObject
        assertEquals("click_tile", json.getValue("bindings").jsonObject.getValue("TAP").jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals("one_handed", json.getValue("bindings").jsonObject.getValue("LONG_PRESS").jsonObject.getValue("type").jsonPrimitive.content)
    }

    @Test fun `ST-28 update and one-handed fields`() {
        val old = SettingsCodec.decode("""{"bindings":{"TAP":{"type":"one_handed"}}}""")
        assertFalse(old.updateOnLaunch)
        assertEquals(UpdateInterval.NEVER, old.updateInterval)
        assertEquals(OneHandedMethod.SYSTEM, old.oneHandedMethod)
        assertEquals(Settings().copy(bindings = old.bindings), old)

        val s = Settings(updateOnLaunch = true, updateInterval = UpdateInterval.WEEKLY, oneHandedMethod = OneHandedMethod.GESTURE)
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        val json = Json.parseToJsonElement(SettingsCodec.encode(s)).jsonObject
        assertEquals("weekly", json.getValue("updateInterval").jsonPrimitive.content)
        assertEquals("gesture", json.getValue("oneHandedMethod").jsonPrimitive.content)
        assertEquals(Settings(), SettingsCodec.decode("""{"enabled":true,"updateInterval":"hourly"}"""))
    }

    @Test fun `ST-29 v1_6_3 actions round trip with stable type names`() {
        val names = mapOf(
            Action.BrightnessUp to "brightness_up",
            Action.BrightnessDown to "brightness_down",
            Action.ToggleAutoBrightness to "auto_brightness",
            Action.ToggleAutoRotate to "auto_rotate",
            Action.RingerNormal to "ringer_normal",
            Action.RingerVibrate to "ringer_vibrate",
            Action.RingerSilent to "ringer_silent",
            Action.CycleRingerMode to "ringer_cycle",
            Action.ToggleDnd to "dnd",
            Action.ToggleWifi to "wifi",
            Action.ToggleBluetooth to "bluetooth",
            Action.ToggleMobileData to "mobile_data",
            Action.ToggleAirplaneMode to "airplane",
            Action.ToggleLocation to "location",
        )
        names.forEach { (action, name) ->
            val s = Settings(bindings = DefaultBindings + (Gesture.TAP to action))
            assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)), name)
            val json = Json.parseToJsonElement(SettingsCodec.encode(s)).jsonObject
            assertEquals("""{"type":"$name"}""", json.getValue("bindings").jsonObject.getValue("TAP").toString())
        }
    }
}
