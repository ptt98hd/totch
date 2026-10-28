# Spec: Cấu hình (`Settings`, `SettingsCodec`)

Yêu cầu: FR-S1…FR-S4 · Module: `:core` (model, codec) + `:app` (`SettingsRepository`)

## 1. Mô hình

```kotlin
@Serializable data class ManualRegionDp(val centerXFraction: Float = 0.5f, val widthDp: Int = 80, val heightDp: Int = 32)

@Serializable data class Settings(
    val enabled: Boolean = false,
    val mode: DetectionMode = DetectionMode.ACCESSIBILITY,
    val bindings: Map<Gesture, Action> = DefaultBindings,
    val doubleTapMs: Long = 300,
    val longPressMs: Long = 500,
    val swipeMinDistanceDp: Int = 40,
    val paddingDp: Int = 8,
    val manualRegion: ManualRegionDp? = null,
    val haptics: Boolean = true,
)
val DefaultBindings = mapOf(
    TAP to None, DOUBLE_TAP to LockScreen, LONG_PRESS to ToggleTorch,
    SWIPE_DOWN to Notifications, SWIPE_LEFT to MediaPrevious, SWIPE_RIGHT to MediaNext)
fun Settings.actionFor(g: Gesture): Action = bindings[g] ?: Action.None
const val TOUCH_SLOP_DP = 12
fun Settings.toGestureConfig(density: Float): GestureConfig   // touchSlopPx = TOUCH_SLOP_DP × density; doubleTapEnabled = actionFor(DOUBLE_TAP) != None
```

## 2. Tiêu chí

| ID | Tiêu chí |
|---|---|
| ST-1 | `Settings()` mặc định khớp FR-S2 |
| ST-2 | `sanitized()` kẹp: doubleTapMs 150–600, longPressMs 300–1500, swipeMinDistanceDp 16–200, paddingDp 0–48, manualRegion: fraction 0–1 (NaN → 0.5), widthDp 8–400, heightDp 8–100 |
| ST-3 | `sanitized()` bổ sung gesture còn thiếu trong `bindings` bằng `None` (đủ 6 key) |
| ST-4 | `decode(encode(s)) == s` cho mọi settings hợp lệ, kể cả `LaunchApp("com.example")` |
| ST-5 | `decode` của chuỗi rỗng/JSON hỏng/type lạ (kể cả `"type"` action không biết hoặc enum không biết) → `Settings()` mặc định cho **toàn bộ** cấu hình, không ném exception |
| ST-6 | `decode` bỏ qua key không biết (tương thích ngược) và trả về đã `sanitized()` |
| ST-7 | `toGestureConfig(density=2f)`: `swipeMinDistancePx = 80f`, `touchSlopPx = 12dp × density = 24f`, `doubleTapEnabled=false` khi DOUBLE_TAP → None |
| ST-8 | JSON dùng tên ổn định (`@SerialName`), ví dụ action Back = `{"type":"back"}` |

## 3. `SettingsRepository` (`:app`)
- DataStore Preferences, key `settings_json`.
- `settings: Flow<Settings>`; `suspend fun update(transform: (Settings) -> Settings)` — luôn lưu bản `sanitized()`.
