# Spec: Cấu hình (`Settings`, `SettingsCodec`)

Yêu cầu: FR-S1…FR-S9, FR-G10…FR-G12, FR-R6 · Module: `:core` (model, codec) + `:app` (`SettingsRepository`)

## 1. Mô hình

```kotlin
@Serializable data class ManualRegionDp(
    val centerXFraction: Float = 0.5f, val widthDp: Int = 80, val heightDp: Int = 32,
    val topDp: Int = 0,   // v1.2: vị trí dọc (FR-R6)
)

@Serializable data class Settings(
    val enabled: Boolean = false,
    // v1.4: bỏ `mode` (FR-D8); JSON cũ có "mode" vẫn đọc được (ST-23)
    val bindings: Map<Gesture, Action> = DefaultBindings,
    val doubleTapMs: Long = 300,
    val longPressMs: Long = 500,
    val swipeMinDistanceDp: Int = 40,
    val paddingDp: Int = 8,
    val manualRegion: ManualRegionDp? = null,
    val haptics: Boolean = true,
    // v1.1 (v1.3: bỏ `repeatOnHold`, `repeatIntervalMs` — FR-G11; JSON cũ có các key này vẫn đọc được, ST-6)
    val theme: ThemeMode = ThemeMode.SYSTEM,         // SYSTEM | LIGHT | DARK  (JSON: "system"/"light"/"dark")
    val amoledBlack: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,  // SYSTEM | ENGLISH | VIETNAMESE (JSON: "system"/"en"/"vi")
    val startOnBoot: Boolean = true,
    // v1.3
    val disabledGestures: Set<Gesture> = emptySet(), // cử chỉ người dùng tắt; binding vẫn giữ (FR-G12)
)
val DefaultBindings = mapOf(
    TAP to None, DOUBLE_TAP to LockScreen, LONG_PRESS to ToggleTorch,
    SWIPE_DOWN to Notifications, SWIPE_LEFT to MediaPrevious, SWIPE_RIGHT to MediaNext)
fun Settings.actionFor(g: Gesture): Action = bindings[g] ?: Action.None
const val TOUCH_SLOP_DP = 12
fun Settings.toGestureConfig(density: Float): GestureConfig   // touchSlopPx = TOUCH_SLOP_DP × density; doubleTapEnabled = effectiveAction(DOUBLE_TAP) != None

// v1.3 (FR-G12)
fun Settings.isGestureOn(g: Gesture): Boolean             // v1.4: effectiveAction(g) != None (cử chỉ không khả dụng luôn tắt)
fun Settings.withGestureOn(g: Gesture, on: Boolean): Settings   // thêm/bớt g khỏi disabledGestures, bindings giữ nguyên
fun Settings.withBinding(g: Gesture, action: Action): Settings  // gán action và bật g

// v1.1
val HoldGestures: Set<Gesture>          // LONG_PRESS, SWIPE_DOWN_HOLD, SWIPE_LEFT_HOLD, SWIPE_RIGHT_HOLD
val Gesture.isAvailable: Boolean                          // v1.4: false cho SWIPE_DOWN, SWIPE_DOWN_HOLD (FR-G10)
fun Settings.effectiveAction(g: Gesture): Action          // None nếu !g.isAvailable hoặc g ∈ disabledGestures, ngược lại actionFor(g)
```
Gesture mới thêm ở v1.1 có binding mặc định `None`.

## 2. Tiêu chí

| ID | Tiêu chí |
|---|---|
| ST-1 | `Settings()` mặc định khớp FR-S2 |
| ST-2 | `sanitized()` kẹp (v1.4): doubleTapMs 100–1000, longPressMs 100–1000, swipeMinDistanceDp 16–200, paddingDp 1–10, manualRegion: fraction 0–1 (NaN → 0.5), widthDp 8–400, heightDp 8–100 |
| ST-3 | `sanitized()` bổ sung gesture còn thiếu trong `bindings` bằng `None` (đủ 6 key) |
| ST-4 | `decode(encode(s)) == s` cho mọi settings hợp lệ, kể cả `LaunchApp("com.example")` |
| ST-5 | `decode` của chuỗi rỗng/JSON hỏng/type lạ (kể cả `"type"` action không biết hoặc enum không biết) → `Settings()` mặc định cho **toàn bộ** cấu hình, không ném exception |
| ST-6 | `decode` bỏ qua key không biết (tương thích ngược) và trả về đã `sanitized()` |
| ST-7 | `toGestureConfig(density=2f)`: `swipeMinDistancePx = 80f`, `touchSlopPx = 12dp × density = 24f`, `doubleTapEnabled=false` khi DOUBLE_TAP → None |
| ST-8 | JSON dùng tên ổn định (`@SerialName`), ví dụ action Back = `{"type":"back"}` |
| ST-9 | `Settings()` v1.1: `theme=SYSTEM`, `amoledBlack=false`, `language=SYSTEM`, `startOnBoot=true`; 3 gesture `*_HOLD` → `None` |
| ST-10 | `sanitized()`: `bindings` đủ 9 key (v1.3: bỏ kẹp `repeatIntervalMs` / lọc `repeatOnHold`) |
| ST-11 | JSON của v1.0 (6 binding, không có field mới) → giữ binding cũ, field mới = mặc định |
| ST-12 | `effectiveAction`: `SWIPE_DOWN`, `SWIPE_DOWN_HOLD` trả `None` dù đã gán (v1.4: luôn luôn) |
| ST-13 | `toGestureConfig`: `holdSwipes` = các `*_HOLD` có `effectiveAction ≠ None`; `doubleTapEnabled` theo `effectiveAction` (gồm cả cử chỉ bị tắt, ST-19) |
| ST-14 | ~~`withDefaultPreferences()`~~ — bỏ ở v1.2 (FR-S9: không còn reset tổng) |
| ST-15 | ~~`withDefaultGestures()`~~ — bỏ ở v1.2 (FR-S9) |
| ST-16 | `decode(encode(s)) == s` cho `Assistant`, `LaunchShortcut(...)`, mọi giá trị `ThemeMode`/`AppLanguage`; JSON enum là `"dark"`, `"vi"`… |
| ST-17 | `sanitized()` kẹp `manualRegion.topDp` 0–100; JSON v1.1 (không có `topDp`) → `topDp = 0` |
| ST-18 | JSON v1.2 có `repeatOnHold`, `repeatIntervalMs` → đọc được, binding giữ nguyên, `disabledGestures = ∅` |
| ST-19 | `disabledGestures` chứa g → `effectiveAction(g) = None`, `actionFor(g)` vẫn là action đã gán; `toGestureConfig` coi g như chưa gán (`doubleTapEnabled=false` khi tắt DOUBLE_TAP, `holdSwipes` bỏ g) |
| ST-20 | `isGestureOn(g)` = `g.isAvailable && g ∉ disabledGestures && actionFor(g) ≠ None`; `withGestureOn(g, false)` rồi `withGestureOn(g, true)` → binding không đổi; `withBinding(g, a)` gán `a` và bỏ g khỏi `disabledGestures` |
| ST-22 | `isGestureOn` = false cho `SWIPE_DOWN`, `SWIPE_DOWN_HOLD` dù đã gán |
| ST-23 | JSON có `"mode"` (SHIZUKU / ACCESSIBILITY) vẫn decode, key bị bỏ qua; `paddingDp` 48 cũ → kẹp về 10 |
| ST-21 | `decode(encode(s)) == s` khi có `disabledGestures` và `Action.VolumePanel` |

## 3. `SettingsRepository` (`:app`)
- DataStore Preferences, key `settings_json`.
- `settings: Flow<Settings>`; `suspend fun update(transform: (Settings) -> Settings)` — luôn lưu bản `sanitized()`.
