# 02 — Thiết kế hệ thống

> Trạng thái: **Đã chốt v1.0**, cập nhật cho v1.1 · **v1.4:** bỏ `AccessibilityTouchSource` (chỉ còn Shizuku nhận chạm), Accessibility chỉ còn là executor tùy chọn; quyền tùy chọn cấp qua `ShizukuShell` + `PermissionCommands` (detection-modes §6) · **v1.7:** Shizuku thành tùy chọn — nguồn chạm `TouchSource` = Shizuku (`getevent`) hoặc overlay của `NotchAccessibilityService` chuyển `MotionEvent` cho engine (detection-modes §8) · Liên quan: [01-requirements.md](01-requirements.md)

## 1. Tổng quan

```mermaid
flowchart LR
  subgraph Input["Nguồn chạm (TouchSource)"]
    SZ["ShizukuTouchSource<br/>getevent -lt (uid shell)"]
    AC["AccessibilityTouchSource<br/>overlay trên notch"]
  end
  SZ -->|"raw line → GeteventParser"| TE((TouchEvent))
  AC -->|"MotionEvent"| TE
  TE --> RF["NotchRegion.contains()"]
  RF --> GR["GestureRecognizer<br/>(state machine)"]
  GR -->|Gesture| BD["Bindings lookup"]
  BD -->|Action| DP["ActionDispatcher"]
  DP --> E1["ShizukuExecutor<br/>(input keyevent, cmd statusbar, am start)"]
  DP --> E2["AccessibilityExecutor<br/>(performGlobalAction)"]
  DP --> E3["LocalExecutor<br/>(đèn pin, media, âm lượng)"]
  UI["Compose UI (M3)"] <--> ST[("DataStore<br/>Settings + Bindings")]
  ST --> GR
  ST --> BD
```

Nguyên tắc:
1. **Lõi thuần Kotlin** (`:core`, JVM library, không phụ thuộc Android) chứa toàn bộ logic quyết định → test nhanh bằng JUnit, phục vụ TDD.
2. **`:app`** chỉ là "adapter": nối Android API (Shizuku, Accessibility, CameraManager, DataStore, Compose) vào các interface của lõi.
3. Không có trạng thái toàn cục; mọi thời gian đi qua tham số `timeMs` để test được.

## 2. Module & package

```
totch/
├── core/                         # Kotlin JVM library — KHÔNG import android.*
│   └── src/main/kotlin/io/github/ptt98hd/totch/core/
│       ├── model/                # TouchEvent, Gesture, Action, Rect, Settings, Bindings
│       ├── gesture/              # GestureRecognizer, GestureConfig
│       ├── input/                # GeteventParser, InputDeviceInfo, CoordinateMapper
│       ├── region/               # NotchRegionResolver
│       ├── action/               # ActionDispatcher, ActionExecutor, ShellCommands
│       ├── overlay/              # v1.9: NotificationIndicator, IslandController, IslandLayout, TouchEffect
│       └── settings/             # SettingsCodec (JSON), validation/clamp
├── app/                          # Android application
│   └── src/main/java/io/github/ptt98hd/totch/
│       ├── TotchApp.kt
│       ├── data/                 # SettingsRepository (DataStore)
│       ├── engine/               # GestureEngine: nối TouchSource → recognizer → dispatcher
│       ├── shizuku/              # ShizukuManager, TouchUserService (AIDL), ShizukuExecutor
│       ├── accessibility/        # NotchAccessibilityService, AccessibilityExecutor
│       ├── service/              # NotchForegroundService, BootReceiver
│       ├── action/               # LocalExecutor (torch, media, volume), AppLauncher
│       ├── overlay/              # v1.9: OverlayController, NotchOverlayWindow (Compose), nguồn media / pin / âm thanh
│       └── ui/                   # theme/, home/, gestures/, notch/, settings/ (app settings + about), navigation
└── docs/
```

## 3. Hợp đồng API của `:core` (nguồn chân lý cho test)

```kotlin
package io.github.ptt98hd.totch.core.model

data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    fun contains(x: Float, y: Float): Boolean   // left <= x < right && top <= y < bottom
    fun inset(by: Int): Rect                    // âm = mở rộng
}

enum class TouchAction { DOWN, MOVE, UP, CANCEL }
data class TouchEvent(val action: TouchAction, val x: Float, val y: Float,
                      val timeMs: Long, val pointerCount: Int = 1)

enum class Gesture { TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT,
                    SWIPE_DOWN_HOLD, SWIPE_LEFT_HOLD, SWIPE_RIGHT_HOLD }   // *_HOLD: v1.1

@Serializable sealed interface Action {
    @Serializable @SerialName("none")          data object None : Action
    @Serializable @SerialName("back")          data object Back : Action
    @Serializable @SerialName("home")          data object Home : Action
    @Serializable @SerialName("recents")       data object Recents : Action
    @Serializable @SerialName("lock")          data object LockScreen : Action
    @Serializable @SerialName("screenshot")    data object Screenshot : Action
    @Serializable @SerialName("notifications") data object Notifications : Action
    @Serializable @SerialName("quick_settings")data object QuickSettings : Action
    @Serializable @SerialName("torch")         data object ToggleTorch : Action
    @Serializable @SerialName("play_pause")    data object MediaPlayPause : Action
    @Serializable @SerialName("next")          data object MediaNext : Action
    @Serializable @SerialName("previous")      data object MediaPrevious : Action
    @Serializable @SerialName("volume_up")     data object VolumeUp : Action
    @Serializable @SerialName("volume_down")   data object VolumeDown : Action
    @Serializable @SerialName("launch_app")    data class LaunchApp(val packageName: String) : Action
    @Serializable @SerialName("assistant")     data object Assistant : Action                       // v1.1
    @Serializable @SerialName("launch_shortcut") data class LaunchShortcut(val packageName: String,  // v1.1
        val shortcutId: String, val label: String, val intentUri: String) : Action
}
```

```kotlin
package io.github.ptt98hd.totch.core.gesture

data class GestureConfig(
    val doubleTapMs: Long = 300, val longPressMs: Long = 500,
    val touchSlopPx: Float = 24f, val swipeMinDistancePx: Float = 60f,
    val doubleTapEnabled: Boolean = true,   // false => FR-G5: TAP phát ngay khi UP
    val holdSwipes: Set<Gesture> = emptySet(),  // v1.1 (v1.3: bỏ repeating / repeatIntervalMs)
)

class GestureRecognizer(config: GestureConfig, private val region: () -> Rect?) {
    var config: GestureConfig                  // thay được lúc chạy (FR-S3)
    fun onTouch(event: TouchEvent): List<Gesture>
    fun onTick(nowMs: Long): List<Gesture>      // gọi theo deadline để xử lý timeout
    fun nextDeadlineMs(): Long?                 // thời điểm cần gọi onTick kế tiếp, null = không cần
    fun reset()
}
```

```kotlin
package io.github.ptt98hd.totch.core.input

data class AbsRange(val min: Int, val max: Int)
data class InputDeviceInfo(val path: String, val name: String, val x: AbsRange, val y: AbsRange, val direct: Boolean)

object GeteventDeviceParser { fun parse(getEventPlOutput: String): List<InputDeviceInfo>  // từ `getevent -pl`
                              fun pickTouchscreen(devices: List<InputDeviceInfo>): InputDeviceInfo? }

class GeteventParser(private val mapper: CoordinateMapper) {
    fun feed(line: String): List<TouchEvent>    // từng dòng của `getevent -lt <device>`
}

class CoordinateMapper(device: InputDeviceInfo, screenWidthPx: Int, screenHeightPx: Int) {
    fun map(rawX: Int, rawY: Int): Pair<Float, Float>
}
```

```kotlin
package io.github.ptt98hd.totch.core.region

data class Cutout(val boundingRectsTop: List<Rect>)  // rects ở cạnh trên do Android cung cấp
data class ManualRegion(val centerXFraction: Float, val widthPx: Int, val heightPx: Int)

object NotchRegionResolver {
    fun resolve(screenWidthPx: Int, statusBarHeightPx: Int, cutout: Cutout?,
                manual: ManualRegion?, paddingPx: Int, rotation: Int): Rect?   // null = tắt (FR-R4)
}
```

```kotlin
package io.github.ptt98hd.totch.core.action

enum class Capability { SHIZUKU, ACCESSIBILITY, LOCAL }

interface ActionExecutor {
    val capability: Capability
    fun isAvailable(): Boolean
    fun supports(action: Action): Boolean
    suspend fun execute(action: Action): Result<Unit>
}

object ActionRequirements { fun of(action: Action): Set<Capability> }  // tập backend có thể chạy

class ActionDispatcher(executors: List<ActionExecutor>) {
    // Ưu tiên: LOCAL > ACCESSIBILITY > SHIZUKU. None → Skipped, không gọi executor.
    suspend fun dispatch(action: Action): DispatchResult
}
sealed interface DispatchResult { data object Ok; data object Skipped; data class Failed(val reason: String) }

object ShellCommands { fun of(action: Action): String?                          // lệnh shell cho ShizukuExecutor
                       fun launch(packageName: String, activityName: String): String?    // null nếu tên không hợp lệ
                       fun startIntent(intentUri: String): String? }                  // v1.1, SC-4…SC-6
```

```kotlin
package io.github.ptt98hd.totch.core.settings

enum class DetectionMode { SHIZUKU, ACCESSIBILITY }

@Serializable data class Settings(
    val enabled: Boolean = false,
    val mode: DetectionMode = DetectionMode.ACCESSIBILITY,
    val bindings: Map<Gesture, Action> = DefaultBindings,
    val doubleTapMs: Long = 300, val longPressMs: Long = 500,
    val swipeMinDistanceDp: Int = 40, val paddingDp: Int = 8,
    val manualRegion: ManualRegionDp? = null,
    val haptics: Boolean = true,
    // v1.1
    val theme: ThemeMode = ThemeMode.SYSTEM, val amoledBlack: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM, val startOnBoot: Boolean = true,
    // v1.3 (FR-G12) disabledGestures → v1.8.1 (FR-G15) enabledGestures: switch = nhận sự kiện, kể cả khi binding = None
    val enabledGestures: Set<Gesture> = emptySet(), val navLabels: Boolean = true,
    val accessibilityWanted: Boolean = false,          // v1.8.2 (FR-D15): do service Trợ năng ghi, để tự bật lại
) { fun sanitized(): Settings }   // clamp theo FR-S4
// effectiveAction: xem specs/settings.md

object SettingsCodec { fun encode(s: Settings): String; fun decode(json: String): Settings } // lỗi → mặc định
```

## 4. Luồng chính

### 4.1 Shizuku mode
```mermaid
sequenceDiagram
  participant FS as NotchForegroundService
  participant SM as ShizukuManager
  participant US as TouchUserService (uid 2000)
  participant EN as GestureEngine
  FS->>SM: bind UserService
  SM->>US: start
  US->>US: getevent -pl → chọn touchscreen
  US-->>FS: InputDeviceInfo
  US->>US: spawn getevent -lt /dev/input/eventN
  loop mỗi dòng
    US-->>EN: onLine(line) (AIDL oneway callback)
    EN->>EN: GeteventParser.feed → recognizer.onTouch
  end
  EN->>EN: Gesture → binding → ActionDispatcher
```
- `TouchUserService` chạy trong process của Shizuku với uid shell; giao tiếp qua AIDL
  `ITouchService { void start(ILineCallback cb); void stop(); int exec(String cmd); int writeFile(ParcelFileDescriptor, String path); destroy() }`
  (`writeFile` từ v1.6.3: chép APK cập nhật cho `pm install`, update §7) và
  `oneway ILineCallback { onDevice(path, name, minX, maxX, minY, maxY); onLines(String[] frame); onError(int) }`
  (mỗi lần gọi `onLines` chứa các dòng của một khung, tới `SYN_REPORT`).
- `ShizukuExecutor.execute` gọi `exec("input keyevent 4")` …
- Shizuku binder chết → ~~`WAITING_SHIZUKU`~~ (v1.7: nguồn chạm chuyển sang Trợ năng nếu đang bật, không thì `NeedsAccess`), lắng nghe `OnBinderReceivedListener` để tự kết nối lại (NFR-8).

### 4.2 Accessibility mode
- `NotchAccessibilityService.onServiceConnected` thêm một `View` trong suốt kiểu
  `TYPE_ACCESSIBILITY_OVERLAY`, đặt đúng `Rect` vùng notch, `FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN`,
  `layoutInDisplayCutoutMode = ALWAYS`.
- `onTouchEvent(MotionEvent)` → chuyển thành `TouchEvent` tọa độ màn hình (`rawX/rawY`) → engine.
- Khi xoay màn hình / settings thay đổi → tính lại `Rect`, cập nhật `LayoutParams`; `null` → ẩn overlay.
- Service không khai báo `canRetrieveWindowContent`, không lắng nghe event nào (`eventTypes` rỗng).

### 4.3 Hẹn giờ cho recognizer
`GestureEngine` sau mỗi `onTouch/onTick` đọc `nextDeadlineMs()` và đặt lại một coroutine `delay`
duy nhất trên `Dispatchers.Default` → không polling (NFR-3).

## 5. Lưu trữ
- Một key DataStore Preferences `settings_json` chứa `SettingsCodec.encode(settings)`.
- `SettingsRepository.settings: Flow<Settings>` (đã `sanitized()`), `suspend fun update(transform)`.

## 6. Quyền & thành phần manifest
| Thành phần | Mục đích |
|---|---|
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Service Shizuku mode |
| `POST_NOTIFICATIONS` | Thông báo thường trực |
| `RECEIVE_BOOT_COMPLETED` | FR-D6, FR-S8 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | FR-D7 (khuyến nghị, v1.1) |
| `android:localeConfig` (`xml/locales_config`) | FR-S7: ngôn ngữ theo app trong cài đặt hệ thống (Android 13+) |
| `VIBRATE` | FR-A10 |
| `QUERY_ALL_PACKAGES` → **không dùng**; thay bằng `<queries>` intent MAIN/LAUNCHER | App picker |
| `BIND_ACCESSIBILITY_SERVICE` (service) | Accessibility mode |
| Shizuku `ShizukuProvider` | Theo yêu cầu thư viện Shizuku |
| `MediaListenerService` (`BIND_NOTIFICATION_LISTENER_SERVICE`, tùy chọn) | Điều khiển media qua `MediaController` (actions spec §5); không đọc thông báo |
| `INTERNET` (v1.6) | FR-U27: chỉ khi người dùng bấm kiểm tra / cập nhật, chỉ tới GitHub (NFR-4); v1.6.1: thêm kiểm tra khi mở app / định kỳ do người dùng bật (FR-U29) |
| `REQUEST_INSTALL_PACKAGES` (v1.6) | FR-U27: cài APK cập nhật bằng `PackageInstaller` (hệ thống hỏi xác nhận); v1.6.1: bật/tắt ở Home › Permissions qua `appops` (FR-D11) |
| `BIND_JOB_SERVICE` cho `UpdateCheckJobService` (v1.6.1) | FR-U29: kiểm tra cập nhật định kỳ (`setPersisted` dùng `RECEIVE_BOOT_COMPLETED` đã có) |
| `<queries>` intent `QS_TILE` (v1.6) | FR-A15: liệt kê ô Cài đặt nhanh |

## 7. Stack & phiên bản
| Thành phần | Phiên bản |
|---|---|
| JDK | Temurin 21 (qua `mise.toml`, cùng bản chính với build server F-Droid); bytecode Java 17 |
| Gradle / AGP | wrapper 9.8.2 / AGP 9.4.1 (Kotlin tích hợp sẵn của AGP 9) · compileSdk 37 |
| Kotlin | 2.4.21, kotlinx-serialization 1.12, coroutines 1.11 |
| UI | Compose BOM mới nhất, Material 3, Navigation Compose |
| Shizuku | `dev.rikka.shizuku:api` + `provider` 13.1.5 (Apache-2.0, hợp lệ với F-Droid) |
| Test | JUnit 4, kotlin-test, kotlinx-coroutines-test, Turbine |

## 8. ADR (quyết định kiến trúc)
- **ADR-1 Hai chế độ nhận chạm** — status bar nằm trên overlay thường nên chỉ có 2 cách khả thi
  không cần root: đọc input thô (Shizuku) hoặc overlay accessibility. Mỗi cách có đánh đổi
  (R2/R3) → để người dùng chọn.
- **ADR-2 `:core` thuần JVM** — cho phép TDD nhanh (< 5 s), không cần emulator.
- **ADR-3 Settings lưu JSON trong 1 key** — schema đơn giản, migrate dễ, giải mã lỗi thì về mặc định.
- **ADR-4 minSdk 31** — dynamic color luôn có; `WindowMetrics`/`DisplayCutout` ổn định.
- **ADR-5 Lặp khi giữ nằm trong recognizer** (v1.1) — lặp lại là deadline của state `Held`, nên vẫn chỉ có 1 timer
  (NFR-3), test được bằng `onTick` và engine không cần biết gì thêm.
- **ADR-6 Chỉ shortcut tĩnh** (v1.1) — `LauncherApps.getShortcuts`/`startShortcut` chỉ dành cho launcher mặc định;
  shortcut tĩnh đọc được từ manifest của app (qua `<queries>` sẵn có) và mở bằng intent của chính nó.
- **ADR-7 Highlight notch trong Activity** (v1.1) — vẽ trong cửa sổ của app (edge-to-edge, cutout `shortEdges`)
  thay vì overlay: chạy được ở cả 2 mode, không cần quyền, cập nhật theo bản nháp khi kéo slider.
- **ADR-8 Lớp hiển thị trong overlay Trợ năng** (v1.9) — chỉ `TYPE_ACCESSIBILITY_OVERLAY` nằm trên status bar; cửa sổ
  vẽ `FLAG_NOT_TOUCHABLE` (Compose, trong service) tách khỏi cửa sổ chạm `NotchView` để vẽ không ảnh hưởng nhận chạm.
  Quyết định (island hiện gì, bao lâu, hình học, hit-test, viền thông báo) nằm trong `:core/overlay` thuần, có test; `:app`
  chỉ nối nguồn dữ liệu Android và vẽ ([notch-overlay](specs/notch-overlay.md)).
