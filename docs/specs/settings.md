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
    val longPressMs: Long = 300,      // v1.8: 300 ms (FR-S10)
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
    // v1.5
    val landscapeGestures: Boolean = false,           // FR-R8
    // v1.8.1 (FR-G15): thay `disabledGestures` — cử chỉ bật (nhận sự kiện), kể cả khi binding = None
    val enabledGestures: Set<Gesture> = emptySet(),
    val navLabels: Boolean = true,                     // FR-U49
)
val DefaultBindings = Gesture.entries.associateWith { None }   // v1.8: mọi cử chỉ chưa đặt (FR-S2 sửa)
// trước v1.8: TAP → None, DOUBLE_TAP → LockScreen, LONG_PRESS → ToggleTorch, SWIPE_DOWN → Notifications,
//             SWIPE_LEFT → MediaPrevious, SWIPE_RIGHT → MediaNext
fun Settings.actionFor(g: Gesture): Action = bindings[g] ?: Action.None
const val TOUCH_SLOP_DP = 12
fun Settings.toGestureConfig(density: Float): GestureConfig   // touchSlopPx = TOUCH_SLOP_DP × density; doubleTapEnabled = effectiveAction(DOUBLE_TAP) != None

// v1.3 (FR-G12)
fun Settings.isGestureOn(g: Gesture): Boolean             // v1.4: effectiveAction(g) != None (cử chỉ không khả dụng luôn tắt)
fun Settings.withGestureOn(g: Gesture, on: Boolean): Settings   // thêm/bớt g khỏi disabledGestures, bindings giữ nguyên
fun Settings.withBinding(g: Gesture, action: Action): Settings  // gán action và bật g

// v1.1
val HoldGestures: Set<Gesture>          // LONG_PRESS, SWIPE_DOWN_HOLD, SWIPE_LEFT_HOLD, SWIPE_RIGHT_HOLD
// v1.5 (FR-G14): thay `val Gesture.isAvailable`
val SwipeDownGestures: Set<Gesture>                       // SWIPE_DOWN, SWIPE_DOWN_HOLD
fun Gesture.isAvailable(canBlockTouches: Boolean): Boolean // false cho SwipeDownGestures khi !canBlockTouches
fun Settings.effectiveAction(g: Gesture, canBlockTouches: Boolean = false): Action
                                                          // None nếu !g.isAvailable(…) hoặc g ∈ disabledGestures, ngược lại actionFor(g)
fun Settings.isGestureOn(g: Gesture, canBlockTouches: Boolean = false): Boolean
fun Settings.toGestureConfig(density: Float, canBlockTouches: Boolean = false): GestureConfig
fun Settings.needsTouchBlocking(): Boolean                // DM-15
```

**v1.8.1 (FR-G15)** — switch và Not set tách bạch:
```kotlin
fun Settings.isGestureOn(g, canBlockTouches) = g.isAvailable(canBlockTouches) && g ∈ enabledGestures   // không xét binding
fun Settings.effectiveAction(g, canBlockTouches) = if (isGestureOn(g, …)) actionFor(g) else None
fun Settings.toGestureConfig(…)   // doubleTapEnabled = isGestureOn(DOUBLE_TAP); holdSwipes = *_HOLD đang bật (kể cả None)
fun Settings.needsTouchBlocking() = enabled && SwipeDownGestures.any { isGestureOn(it, true) }   // kể cả None
fun Settings.withGestureOn(g, on)  // thêm / bớt g trong enabledGestures
fun Settings.withBinding(g, a)     // gán a (kể cả None) và thêm g vào enabledGestures
```
`SettingsCodec.decode`: JSON **không có** key `enabledGestures` (≤ 1.8.0) → `enabledGestures` = { g | g ∉ `disabledGestures`
cũ ∧ `bindings[g] ≠ None` } (giữ đúng hành vi cũ); `disabledGestures` không còn được ghi.
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
| ST-24 | (v1.5) `canBlockTouches=true`: `effectiveAction(SWIPE_DOWN, true)` = action đã gán, `isGestureOn(SWIPE_DOWN_HOLD, true)` đúng khi đã gán và không tắt; `toGestureConfig(d, true).holdSwipes` chứa `SWIPE_DOWN_HOLD` khi đã gán. Mặc định (`false`) giữ nguyên ST-12, ST-22 |
| ST-25 | `Settings().landscapeGestures == false`; JSON v1.4 (không có key) → `false`; `decode(encode(s)) == s` với `landscapeGestures=true` và `LaunchActivity` |
| ST-26 | `needsTouchBlocking`: false khi `!enabled`, hoặc cả 2 cử chỉ vuốt xuống chưa gán / bị tắt; true khi `enabled` và `SWIPE_DOWN` (hoặc `SWIPE_DOWN_HOLD`) đã gán và bật (DM-15) |
| ST-27 | (v1.6) `decode(encode(s)) == s` với `ClickTile(pkg, service, label)` và `OneHandedMode` (`"one_handed"`); JSON `{"type":"click_tile",…}` |
| ST-28 | (v1.6.1) Field mới: `updateOnLaunch = false`, `updateInterval = NEVER` (update §6), `oneHandedMethod = SYSTEM` (actions §10, do executor ghi, không có trên UI). JSON v1.6.0 (không có key) → mặc định; `decode(encode(s)) == s` với `updateOnLaunch=true`, `updateInterval=WEEKLY`, `oneHandedMethod=GESTURE`; JSON `"oneHandedMethod":"gesture"`, `"updateInterval":"weekly"`; giá trị enum lạ → mặc định toàn bộ (ST-5) |
| ST-29 | (v1.6.3) Hành động mới round-trip với `type` ổn định: `brightness_up`, `brightness_down`, `auto_brightness`, `auto_rotate`, `ringer_normal`, `ringer_vibrate`, `ringer_silent`, `ringer_cycle`, `dnd`, `wifi`, `bluetooth`, `mobile_data`, `airplane`, `location` |
| ST-31 | (v1.8.1, FR-G15) `Settings().enabledGestures = ∅` (mọi cử chỉ tắt, chưa đặt). `isGestureOn(g)` = `g.isAvailable && g ∈ enabledGestures` bất kể binding; g bật + `None` → `effectiveAction = None` nhưng `toGestureConfig` coi g như đang nhận (DOUBLE_TAP → `doubleTapEnabled`, `*_HOLD` → `holdSwipes`), `needsTouchBlocking` đúng khi SWIPE_DOWN bật dù `None`; `withGestureOn(g,false)` rồi `true` → về như cũ; `withBinding(g, None)` → g bật. Thay ST-13, ST-19, ST-20, ST-26 phần "đã gán" |
| ST-32 | (v1.8.1) JSON 1.8.0 không có `enabledGestures`: `{"bindings":{"TAP":home,"DOUBLE_TAP":lock,"LONG_PRESS":torch},"disabledGestures":["LONG_PRESS"]}` → `enabledGestures = {TAP, DOUBLE_TAP}`; không có `disabledGestures` → mọi cử chỉ đã gán bật. Có `enabledGestures` → dùng nguyên giá trị. `decode(encode(s)) == s` |
| ST-33 | (v1.8.1, FR-U49) `Settings().navLabels == true`; JSON cũ không có key → `true`; round-trip `navLabels=false` |
| ST-30 | (v1.8, FR-S2 sửa, FR-S10) `Settings().bindings` = `None` cho **cả 9** cử chỉ, `isGestureOn(g)` = false với mọi g; `Settings().longPressMs == 300`; JSON đã lưu (có `bindings`, `longPressMs`) giữ nguyên giá trị cũ |

## 3. `SettingsRepository` (`:app`)
- DataStore Preferences, key `settings_json`.
- `settings: Flow<Settings>`; `suspend fun update(transform: (Settings) -> Settings)` — luôn lưu bản `sanitized()`.
