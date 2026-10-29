# Spec: Hành động & điều phối (`ActionDispatcher`)

Yêu cầu: FR-A1…FR-A19 (v1.6.3) · Module: `:core` (logic) + `:app` (executor)

## 1. Ma trận khả năng — `ActionRequirements.of(action)`

| Action | Capabilities |
|---|---|
| `None` | ∅ |
| `Back`, `Home`, `Recents`, `LockScreen`, `Screenshot`, `Notifications`, `QuickSettings`, `LaunchApp`, `Assistant`, `LaunchShortcut`, `LaunchActivity` | {ACCESSIBILITY, SHIZUKU} |
| `ToggleTorch`, `MediaPlayPause`, `MediaNext`, `MediaPrevious`, `VolumeUp`, `VolumeDown`, `VolumePanel` | {LOCAL} |
| `ClickTile`, `OneHandedMode` (v1.6) | {SHIZUKU} |
| (v1.6.3) `BrightnessUp`, `BrightnessDown`, `ToggleAutoBrightness`, `ToggleAutoRotate`, `ToggleWifi`, `ToggleBluetooth`, `ToggleMobileData`, `ToggleAirplaneMode`, `ToggleLocation` | {SHIZUKU} |
| (v1.6.3) `RingerNormal`, `RingerVibrate`, `RingerSilent`, `CycleRingerMode`, `ToggleDnd` | {LOCAL} |

## 2. Điều phối

`dispatch(action)`:
1. `None` → `Skipped`, không gọi executor nào.
2. Lọc executor có `capability ∈ requirements`, `isAvailable()` và `supports(action)`.
3. Chọn theo ưu tiên **LOCAL > ACCESSIBILITY > SHIZUKU**.
4. Không có ứng viên → `Failed("unavailable")`.
5. `execute` trả `Result.failure(e)` hoặc ném exception → `Failed(e.message ?: "error")`; **không** thử executor khác.
6. Thành công → `Ok`.

| ID | Tiêu chí |
|---|---|
| AD-1 | `None` → `Skipped`, executor giả không bị gọi |
| AD-2 | `Back` với cả ACCESSIBILITY và SHIZUKU khả dụng → chỉ ACCESSIBILITY được gọi |
| AD-3 | `Back` khi ACCESSIBILITY `isAvailable()=false` → SHIZUKU được gọi |
| AD-4 | `ToggleTorch` khi chỉ có SHIZUKU → `Failed("unavailable")` |
| AD-5 | Executor ném exception → `Failed` với message, không lan exception ra ngoài |
| AD-6 | Executor `supports()=false` bị bỏ qua |
| AD-7 | `Assistant`, `LaunchShortcut` có requirements {ACCESSIBILITY, SHIZUKU} |
| AD-8 | `VolumePanel` có requirements {LOCAL} (v1.3, FR-A13) |
| AD-9 | `LaunchActivity` có requirements {ACCESSIBILITY, SHIZUKU} (v1.5, FR-A14) |
| AD-11 | `OneHandedMode` có requirements {SHIZUKU} (v1.6, FR-A16) |
| AD-10 | `ClickTile` có requirements {SHIZUKU} (v1.6, FR-A15): chỉ có Trợ năng → `Failed("unavailable")` |
| AD-12 | (v1.6.3) Hành động màn hình và kết nối có requirements {SHIZUKU}; hành động âm thanh có {LOCAL} |

Action mới (v1.1):
```kotlin
@SerialName("assistant")       data object Assistant : Action                       // FR-A11
@SerialName("launch_shortcut") data class LaunchShortcut(                           // FR-A12
    val packageName: String, val shortcutId: String, val label: String, val intentUri: String) : Action
```
Action mới (v1.3):
```kotlin
@SerialName("volume_panel") data object VolumePanel : Action                         // FR-A13
```

`intentUri` = `Intent.toUri(Intent.URI_INTENT_SCHEME)` của intent cuối trong shortcut tĩnh; `label` lưu lại
lúc chọn để hiển thị mà không cần đọc lại manifest.

## 3. Lệnh shell — `ShellCommands`

| Action | Lệnh |
|---|---|
| Back | `input keyevent 4` |
| Home | `input keyevent 3` |
| Recents | `input keyevent 187` |
| LockScreen | `input keyevent 223` |
| Screenshot | `input keyevent 120` |
| Notifications | `cmd statusbar expand-notifications` |
| QuickSettings | `cmd statusbar expand-settings` |
| Assistant | `input keyevent 219` (KEYCODE_ASSIST → trợ lý mặc định của hệ thống) |
| BrightnessUp / BrightnessDown | `input keyevent 221` / `input keyevent 220` (v1.6.3, §11) |
| ToggleAutoBrightness, ToggleAutoRotate, ToggleWifi, ToggleBluetooth, ToggleMobileData, ToggleAirplaneMode, ToggleLocation | script bật/tắt (v1.6.3, §11, §13) |
| OneHandedMode | `null` từ `of()`; dùng `OneHandedCommands` (v1.6.1, §10) |
| các action LOCAL, None | `null` |
| LaunchApp(pkg) | `null` từ `of()`; dùng `launch(pkg, activity)` |
| LaunchShortcut | `null` từ `of()`; dùng `startIntent(intentUri)` |
| LaunchActivity | `null` từ `of()`; dùng `startActivity(pkg, activity)` (v1.5) |
| ClickTile | `null` từ `of()`; dùng `clickTile(pkg, service)` (v1.6, script v1.6.1 §9) |

`ShellCommands.launch(packageName, activityName): String?` →
`am start -n <pkg>/<activity> -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000`

| ID | Tiêu chí |
|---|---|
| SC-1 | Bảng trên đúng cho mọi action |
| SC-2 | **Chống shell injection**: package phải khớp `^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$`, activity khớp `^[A-Za-z0-9_.$]+$`; sai → `null` |
| SC-3 | `launch("com.foo; rm -rf /", "X")` → `null` |
| SC-4 | `startIntent("intent:#Intent;component=com.a/.B;end")` → `am start 'intent:#Intent;component=com.a/.B;end'` |
| SC-5 | Dấu `'` trong URI được escape thành `'\''` (không thoát được khỏi chuỗi quote) |
| SC-6 | URI không bắt đầu bằng `intent:` hoặc `#Intent;`, hoặc chứa ký tự điều khiển (`\n`, `\r`, `\u0000`…) → `null` |
| SC-8 | (v1.6.1) `clickTile("com.a", "com.a.qs.Tile$Inner")` → script §9 với component `'com.a/com.a.qs.Tile$Inner'` (single-quote); tên sai định dạng (SC-2) → `null` |
| SC-7 | (v1.5) `startActivity("com.a", "com.a.Main$Sub")` → `am start -n 'com.a/com.a.Main$Sub' -f 0x10000000`; tên sai định dạng (SC-2) → `null` |

## 4. Executor phía `:app`

| Executor | Thực thi |
|---|---|
| `LocalExecutor` | Đèn pin: `CameraManager.setTorchMode` trên camera có flash, lưu trạng thái qua `TorchCallback`. Media: xem §5. Âm lượng: `adjustStreamVolume(STREAM_MUSIC, ±1, FLAG_SHOW_UI)`. Bảng âm lượng: `adjustStreamVolume(STREAM_MUSIC, ADJUST_SAME, FLAG_SHOW_UI)` (chỉ hiện thanh, không đổi mức) |
| `AccessibilityExecutor` | LaunchActivity (v1.5): `startActivity(Intent().setClassName(pkg, activity) + NEW_TASK)`. `performGlobalAction(GLOBAL_ACTION_BACK/HOME/RECENTS/LOCK_SCREEN/TAKE_SCREENSHOT/NOTIFICATIONS/QUICK_SETTINGS)`; LaunchApp: `startActivity(launchIntent)` từ service; Assistant: `startActivity(ACTION_ASSIST)`, không có thì `ACTION_VOICE_COMMAND`; LaunchShortcut: `Intent.parseUri(intentUri)` (bỏ selector và cờ grant URI) + `FLAG_ACTIVITY_NEW_TASK`. `isAvailable()` = service đang chạy |
| `ShizukuExecutor` | `ITouchService.exec(ShellCommands…)` (kể cả `ClickTile`, v1.6). `isAvailable()` = Shizuku binder sống + đã cấp quyền + UserService đã bind |

Phản hồi: `haptics` bật → rung ngay khi engine nhận diện cử chỉ có hành động, trước khi dispatch (v1.6.2, FR-A10, §6).
Nếu `Failed` → Toast ngắn
"Không thể thực hiện: <tên hành động>" (FR-A9).

## 5. Điều khiển media (FR-A6, R5)

Thứ tự thử cho `MediaPlayPause` / `MediaNext` / `MediaPrevious`:
1. Có quyền *Notification access* (`MediaListenerService`) → `MediaSessionManager.getActiveSessions` →
   chọn phiên theo `chooseMediaSession` → `transportControls` (`play`/`pause` theo trạng thái, `skipToNext`, `skipToPrevious`).
2. Không có quyền hoặc không có phiên nào; `MediaPlayPause` và Accessibility service đang chạy →
   `performGlobalAction(GLOBAL_ACTION_KEYCODE_HEADSETHOOK)` (hệ thống tự gửi phím, không bị chặn).
3. Còn lại → `AudioManager.dispatchMediaKeyEvent` (DOWN+UP).

`MediaListenerService` (NotificationListenerService) **không** đọc thông báo: không override `onNotificationPosted`,
chỉ tồn tại để hệ thống cho phép `getActiveSessions`.

`chooseMediaSession(sessions: List<MediaSessionInfo>): Int?` — `MediaSessionInfo(packageName, playing)`, danh sách theo thứ tự ưu tiên của hệ thống.

| ID | Tiêu chí |
|---|---|
| MC-1 | Danh sách rỗng → `null` |
| MC-2 | Có phiên đang phát → chỉ số của phiên đang phát **đầu tiên** |
| MC-3 | Không phiên nào đang phát → `0` (phiên ưu tiên cao nhất, thường là phiên vừa dừng) |
| MC-4 | PlayPause trên phiên đang phát → `pause()`, ngược lại → `play()` (`mediaCommand(action, playing)`) |

## 6. Rung phản hồi (FR-A10 — sửa lỗi bản 1.0.x)

Lỗi 1.0.x: `vibrate(createPredefined(EFFECT_TICK))` không kèm thuộc tính → hệ thống xếp vào `USAGE_TOUCH`,
bị tắt theo công tắc "Phản hồi chạm" của hệ thống / ROM và bị bỏ khi process ở nền; `EFFECT_TICK` cũng quá nhẹ
hoặc không được hỗ trợ trên nhiều máy.

| ID | Tiêu chí |
|---|---|
| HF-1 | Hiệu ứng: `EFFECT_CLICK` nếu `areAllEffectsSupported` = YES, ngược lại one-shot 30 ms biên độ mặc định |
| HF-2 | Android 13+: `vibrate(effect, VibrationAttributes.createForUsage(USAGE_HARDWARE_FEEDBACK))` (được phép khi app ở nền, không phụ thuộc công tắc phản hồi chạm); Android 12: `AudioAttributes` `USAGE_ASSISTANCE_SONIFICATION` |
| HF-3 | Mỗi cử chỉ có hành động rung một lần; không có vibrator → bỏ qua, không crash |
| HF-4 | (v1.6.2) Rung ngay khi nhận diện, **không chờ** dispatch xong (đang chạy, chờ trong hàng đợi, hay lỗi đều đã rung) — hành động qua Shizuku có thể chậm, rung sau khi xong cảm giác trễ |

## 7. Shortcut tĩnh của ứng dụng (FR-A12)

Android chỉ cho launcher mặc định đọc shortcut động/đã ghim, nên Totch chỉ hỗ trợ **shortcut tĩnh**:
`meta-data android.app.shortcuts` của activity launcher → XML `<shortcut>` (`shortcutId`, `enabled`,
`shortcutShortLabel`, `shortcutLongLabel`) chứa một hoặc nhiều `<intent>` (`action`, `data`, `targetPackage`,
`targetClass`, `<extra>`, `<categories>`). Lấy intent **cuối**. Bỏ shortcut `enabled=false`, thiếu nhãn, hoặc
activity đích không `exported` / không tồn tại (app khác không mở được). Không cần quyền mới: các package có
activity launcher đã nhìn thấy nhờ `<queries>`.

| ID | Tiêu chí |
|---|---|
| SH-1 | `groupShortcuts(entries, query)`: nhóm theo package, nhóm sắp theo tên app rồi package, shortcut trong nhóm theo nhãn (không phân biệt hoa thường) |
| SH-2 | Query (trim, không phân biệt hoa thường) khớp nhãn shortcut **hoặc** tên app; nhóm không còn shortcut nào bị ẩn; khớp tên app → giữ cả nhóm |

## 8. Activity của ứng dụng (v1.5 — FR-A14)

```kotlin
@SerialName("launch_activity") data class LaunchActivity(
    val packageName: String, val activityName: String, val label: String) : Action   // label lưu để hiển thị
```

Nguồn: `AppActivities.activities()` — với mỗi app có activity launcher (đã thấy nhờ `<queries>`),
`getPackageInfo(pkg, GET_ACTIVITIES)` → giữ activity `exported`, `enabled`, `permission == null` (app khác / shell mở
được). Nhãn = `loadLabel` của activity; hiển thị kèm tên lớp rút gọn (bỏ tiền tố package) vì nhiều activity trùng nhãn app.

`groupActivities(entries, query): List<ActivityGroup>` — như SH-1/SH-2:

| ID | Tiêu chí |
|---|---|
| AC-1 | Nhóm theo package, nhóm sắp theo tên app rồi package; activity trong nhóm theo nhãn rồi tên lớp (không phân biệt hoa thường) |
| AC-2 | Query (trim, không phân biệt hoa thường) khớp nhãn, tên lớp **hoặc** tên app; khớp tên app → giữ cả nhóm; nhóm rỗng bị ẩn |
| AC-3 | `shortActivityName("com.a", "com.a.ui.Main")` = `.ui.Main`; lớp ở package khác giữ nguyên tên đầy đủ |

## 9. Ô Cài đặt nhanh (v1.6 — FR-A15)

```kotlin
@SerialName("click_tile") data class ClickTile(
    val packageName: String, val serviceName: String, val label: String) : Action   // label lưu để hiển thị
```

Nguồn: `QuickSettingsTiles.tiles()` — `queryIntentServices(Intent(TileService.ACTION_QS_TILE))` (cần `<queries>` cho
action này), giữ service `exported`, `enabled`, `permission == BIND_QUICK_SETTINGS_TILE`, bỏ ô của chính Totch. Nhãn =
`loadLabel` của service, kèm tên app.

Thực thi: chỉ Shizuku. `cmd statusbar click-tile <pkg>/<service>` đi qua `StatusBarManagerService` (shell được phép
gọi) tới SystemUI, nhưng SystemUI (cả QS cũ `QSPanelControllerBase` lẫn QS Compose) **chỉ tìm ô trong danh sách đang
có ở bảng Cài đặt nhanh**; không có thì bỏ qua lệnh. Không có cách bấm ô mà không đụng tới bảng: tự bind `TileService`
cần `BIND_QUICK_SETTINGS_TILE` (signature, shell không có), `requestAddTileService` chỉ cho ô của chính app.

v1.6.1 (FR-A15): **tạm thêm → bấm → gỡ**. Shell được gọi cả `add-tile`, `click-tile`, `remove-tile`. Nếu service của ô
chưa bind xong, SystemUI xếp hàng cú bấm và gửi khi bind xong; ô mới thêm ở trạng thái active nên cú bấm đầu không bị bỏ.

`sysui_qs_tiles` lưu ô của app dạng `custom(<ComponentName.flattenToShortString()>)` — tên lớp bắt đầu bằng package
được rút gọn (`com.a/.qs.Tile`), nên script so cả 2 dạng:
```sh
c='<pkg>/<service>'
case ",$(settings get secure sysui_qs_tiles)," in
  *",custom($c),"*|*",custom(<pkg>/<.short>),"*) cmd statusbar click-tile "$c" ;;      # đã có: bấm như cũ
  *) cmd statusbar add-tile "$c" && sleep 0.8 && cmd statusbar click-tile "$c"; r=$?
     sleep 2; cmd statusbar remove-tile "$c"; exit $r ;;                               # chưa có: thêm, bấm, gỡ
esac
```
(nhánh rút gọn chỉ có khi `service` bắt đầu bằng `<pkg>.`). Hai thời gian chờ là ước lượng, cần chỉnh trên máy thật
(lệnh Shizuku tối đa 10 s). Hạn chế: chậm ~3 s với ô chưa có trong bảng; app của ô nhận thêm sự kiện thêm/gỡ ô; nếu
bảng Cài đặt nhanh đang mở, ô chớp hiện rồi biến mất. Thứ tự ô của người dùng giữ nguyên. Ô dựng sẵn của hệ thống
(Wi-Fi, Bluetooth…) không phải `TileService` nên vẫn không có. Picker bỏ gợi ý "Chỉ ô đã thêm vào Cài đặt nhanh…".

`filterTiles(entries, query): List<TileEntry>`:

| ID | Tiêu chí |
|---|---|
| QT-1 | Sắp theo nhãn ô rồi tên app (không phân biệt hoa thường); query (trim, không phân biệt hoa thường) khớp nhãn ô **hoặc** tên app |
| QT-2 | (v1.6.1) `clickTile("com.a", "com.a.qs.Tile")`: so `sysui_qs_tiles` với `custom(com.a/com.a.qs.Tile)` **và** `custom(com.a/.qs.Tile)`; đã có → chỉ `click-tile`; chưa có → `add-tile`, chờ, `click-tile`, chờ, `remove-tile` (luôn gỡ, kể cả khi bấm lỗi), thoát bằng mã của `click-tile`. `clickTile("com.a", "org.b.Tile")` không có dạng rút gọn |

## 10. Chế độ một tay (v1.6 — FR-A16; v1.6.1 viết lại)

`@SerialName("one_handed") data object OneHandedMode : Action` — nhóm System của picker (v1.6.1: bỏ dòng phụ "Cần phím
tắt Chế độ một tay").

### Vì sao bản 1.6.0 không chạy
Android không có global action / keyevent / lệnh shell cho One-handed mode. WMShell (`OneHandedController`) theo dõi
`Settings.Secure.one_handed_mode_activated`: 1 → vào chế độ một tay, 0 → thoát. Nhưng thay đổi bị **bỏ qua** khi chưa
gán phím tắt Chế độ một tay (`accessibility_button_targets` / `accessibility_shortcut_target_service` không chứa
`OneHandedMode`); ngoài ra khi người dùng chọn kiểu "Hiện thông báo" (`swipe_bottom_to_notification_enabled` = 1) thì
lệnh mở thông báo. Bản 1.6.0 chỉ ghi giá trị và báo lỗi khi thiếu phím tắt, nên đa số máy không có tác dụng. Hệ thống
cũng chỉ ghi lại 0 khi thoát nếu có phím tắt, nên đảo theo giá trị đã lưu dễ lệch với trạng thái thật.

### Trạng thái thật
`dumpsys activity service SystemUIService WMShell` in `OneHandedState` → dòng `sCurrentState=<n>` (0 NONE, 1 ENTERING,
2 ACTIVE, 3 EXITING). Script đọc dòng này để biết đang bật hay tắt và để **kiểm tra** lệnh có tác dụng. Không đọc được
(ROM khác, dump đổi định dạng) → coi như không kiểm tra được: chọn hướng theo `one_handed_mode_activated` và coi là thành công.

### Hai cách, có fallback và nhớ cách chạy được
```kotlin
@Serializable enum class OneHandedMethod { @SerialName("system") SYSTEM, @SerialName("gesture") GESTURE }

object OneHandedCommands {
    fun system(): String                                          // OH-1
    fun gesture(widthPx: Int, heightPx: Int, density: Float): String // OH-2
}
object OneHandedFallback {
    fun order(preferred: OneHandedMethod): List<OneHandedMethod>  // OH-3
    suspend fun run(preferred: OneHandedMethod, attempt: suspend (OneHandedMethod) -> Boolean): OneHandedMethod? // OH-4
}
```
Mã thoát của script: 0 thành công (hoặc không kiểm tra được), 2 lệnh không có tác dụng, 3 máy không hỗ trợ
(`ro.support_one_handed_mode` ≠ `true`), 4 không dùng điều hướng bằng cử chỉ (cách GESTURE).

**SYSTEM (native)** — vẫn qua `one_handed_mode_activated`, nhưng tự lo phím tắt:
1. Không hỗ trợ → `exit 3`. Đích `t` = 0 nếu trạng thái là 1/2, ngược lại 1.
2. Chưa có phím tắt → **tạm** thêm `com.android.server.accessibility.OneHandedMode` vào
   `accessibility_shortcut_target_service` (phím tắt giữ phím âm lượng, không hiện gì trên màn hình; giữ target cũ,
   ngăn bằng `:`), chờ 0,3 s cho WMShell đọc lại.
3. Ghi `1-t` rồi `t` (giá trị đổi chắc chắn → observer luôn chạy; lần ghi đầu không có tác dụng vì trùng trạng thái).
4. Chờ tối đa ~1,5 s đến khi trạng thái khớp đích (t=1 → 1/2, t=0 → 0/3); không khớp → mã 2.
5. Trả lại giá trị cũ của `accessibility_shortcut_target_service` nếu đã đổi (`null` → `settings delete`), kể cả khi lỗi.
   `swipe_bottom_to_notification_enabled` = 1 → ghi như bước 3 rồi `exit 0` (hệ thống mở thông báo theo lựa chọn của người dùng).

**GESTURE (fallback)** — giả lập cử chỉ của launcher (Launcher3 `OneHandedModeInputConsumer`, gesture monitor nhận cả
sự kiện tiêm bằng `input`):
1. Không hỗ trợ → `exit 3`; `navigation_mode` ≠ 2 → `exit 4`. `one_handed_mode_enabled` ≠ 1 → bật (cử chỉ cần nó; hệ
   thống cũng tự bật khi dùng phím tắt).
2. Đang bật (1/2) → `input tap x yTop` (chạm vùng trống phía trên để thoát); ngược lại `input swipe x y1 x y2 150`
   (vuốt xuống từ mép dưới, điểm cuối ra ngoài màn hình để vượt ngưỡng kéo ~20 dp).
   x = w/2, y1 = h − round(8·d), y2 = h + round(40·d), yTop = round(0,15·h). Kích thước theo hướng hiện tại; xoay
   ngang thì chế độ một tay không chạy (hệ thống bỏ qua) → mã 2.
3. Kiểm tra như SYSTEM bước 4.

**Chọn cách** (`ShizukuExecutor`): `OneHandedFallback.run(settings.oneHandedMethod) { exec(script(it)) == 0 }`.
Cách đầu tiên thành công được dùng; khác `oneHandedMethod` đang lưu → **lưu lại làm cách chính** cho lần sau. Cả 2 thất
bại → `Failed` → Toast "Không thể thực hiện" (FR-A9).

| ID | Tiêu chí |
|---|---|
| SC-9 | (v1.6.1) `ShellCommands.of(OneHandedMode) == null`; `ShizukuExecutor.supports(OneHandedMode)` vẫn đúng |
| OH-1 | `system()`: kiểm tra `ro.support_one_handed_mode` (`exit 3`); đọc `sCurrentState` từ `dumpsys activity service SystemUIService WMShell`; chỉ khi thiếu `OneHandedMode` trong 2 key phím tắt mới ghi tạm `accessibility_shortcut_target_service` và luôn khôi phục (`trap … EXIT`); ghi `one_handed_mode_activated` 2 lần (`1-t` rồi `t`); kiểm tra trạng thái, không khớp → `exit 2` |
| OH-2 | `gesture(1080, 2400, 2.5f)`: `exit 4` khi `navigation_mode` ≠ 2; bật `one_handed_mode_enabled`; vuốt `input swipe 540 2380 540 2500 150`; đang bật → `input tap 540 360`; kích thước ≤ 0 hoặc density ≤ 0 → vẫn trả script nhưng `exit 2` ngay |
| OH-3 | `order(SYSTEM) = [SYSTEM, GESTURE]`, `order(GESTURE) = [GESTURE, SYSTEM]` |
| OH-4 | `run`: thử theo `order`, dừng ở cách đầu tiên trả true và trả về cách đó (cách sau không bị gọi); cả 2 false → `null`; exception (trừ hủy coroutine) coi như false |
| OH-5 | (`:app`) Executor lưu `oneHandedMethod` khi cách chạy được khác cách đang lưu; không lưu khi giống; cả 2 lỗi → `Result.failure` |

## 11. Màn hình (v1.6.3 — FR-A17)

```kotlin
@SerialName("brightness_up")   data object BrightnessUp : Action
@SerialName("brightness_down") data object BrightnessDown : Action
@SerialName("auto_brightness") data object ToggleAutoBrightness : Action
@SerialName("auto_rotate")     data object ToggleAutoRotate : Action
```

Chỉ Shizuku (ghi `Settings.System` cần `WRITE_SETTINGS`, Totch không xin quyền này).

- Độ sáng: `input keyevent 221` (`KEYCODE_BRIGHTNESS_UP`) / `220` (`DOWN`). `PhoneWindowManager` tự tăng/giảm một nấc
  theo đường cong gamma của hệ thống và hiện thanh độ sáng của SystemUI — giống phím độ sáng phần cứng. Hệ thống **tắt
  độ sáng tự động** khi dùng phím này (hành vi AOSP).
- Bật/tắt dùng script chung `toggle` (§13): độ sáng tự động `system screen_brightness_mode` (1 = tự động), tự động xoay
  `system accelerometer_rotation` (1 = bật); bật/tắt bằng `settings put system <key> 1|0`.

## 12. Chế độ âm thanh (v1.6.3 — FR-A18)

```kotlin
@SerialName("ringer_normal")  data object RingerNormal : Action
@SerialName("ringer_vibrate") data object RingerVibrate : Action
@SerialName("ringer_silent")  data object RingerSilent : Action
@SerialName("ringer_cycle")   data object CycleRingerMode : Action
@SerialName("dnd")            data object ToggleDnd : Action

enum class RingerMode { NORMAL, VIBRATE, SILENT }                       // core.action
object SoundModes { fun next(current: RingerMode, canVibrate: Boolean): RingerMode }
```

`LocalExecutor`: `AudioManager.setRingerMode`, `NotificationManager.setInterruptionFilter`. Android chặn mọi thay đổi có
thể bật/tắt Không làm phiền (im lặng, hoặc đổi chế độ khi đang Không làm phiền) nếu app chưa có **quyền truy cập Không
làm phiền** → cần quyền đó (Home › Permissions, detection-modes §6). Shell không có lệnh đổi chế độ chuông ổn định trên
Android 12, nên không có đường Shizuku. Chưa có quyền → `Failed` → toast (FR-A9); picker hiện dòng phụ "Cần quyền
Không làm phiền" dưới các hành động này.

- `RingerNormal` / `RingerVibrate` / `RingerSilent`: `setRingerMode(NORMAL|VIBRATE|SILENT)`.
- `CycleRingerMode`: `setRingerMode(next(hiện tại, hasVibrator))`.
- `ToggleDnd`: bộ lọc hiện tại `INTERRUPTION_FILTER_ALL` (hoặc `UNKNOWN`) → `INTERRUPTION_FILTER_PRIORITY`; còn lại →
  `INTERRUPTION_FILTER_ALL`.
- Hệ thống không hiện gì khi đổi chế độ; rung phản hồi của cử chỉ là xác nhận.

| ID | Tiêu chí |
|---|---|
| SM-1 | `next(NORMAL, true) = VIBRATE`, `next(VIBRATE, true) = SILENT`, `next(SILENT, true) = NORMAL` |
| SM-2 | Không có bộ rung: `next(NORMAL, false) = SILENT`, `next(VIBRATE, false) = SILENT`, `next(SILENT, false) = NORMAL` |
| SM-3 | `LocalExecutor.supports` đúng cho 5 hành động âm thanh; `ShellCommands.of` trả `null` cho chúng |

## 13. Kết nối (v1.6.3 — FR-A19)

```kotlin
@SerialName("wifi")        data object ToggleWifi : Action
@SerialName("bluetooth")   data object ToggleBluetooth : Action
@SerialName("mobile_data") data object ToggleMobileData : Action
@SerialName("airplane")    data object ToggleAirplaneMode : Action
@SerialName("location")    data object ToggleLocation : Action
```

Chỉ Shizuku. Mọi hành động bật/tắt (cả §11) dùng một dạng script: đọc trạng thái từ `settings`, đang bật → lệnh tắt,
ngược lại → lệnh bật; mã thoát là mã của lệnh bật/tắt.
```sh
case "$(settings get <ns> <key>)" in <on1>|<on2>…) <lệnh tắt> ;; *) <lệnh bật> ;; esac
```

| Action | Trạng thái (`ns key` → giá trị "bật") | Bật | Tắt |
|---|---|---|---|
| ToggleAutoBrightness | `system screen_brightness_mode` → 1 | `settings put system screen_brightness_mode 1` | `… 0` |
| ToggleAutoRotate | `system accelerometer_rotation` → 1 | `settings put system accelerometer_rotation 1` | `… 0` |
| ToggleWifi | `global wifi_on` → 1, 2 (2 = bật khi đang ở chế độ máy bay) | `svc wifi enable` | `svc wifi disable` |
| ToggleBluetooth | `global bluetooth_on` → 1, 2 | `cmd bluetooth_manager enable \|\| svc bluetooth enable` | tương tự `disable` |
| ToggleMobileData | `global mobile_data` → 1 | `svc data enable` | `svc data disable` |
| ToggleAirplaneMode | `global airplane_mode_on` → 1 | `cmd connectivity airplane-mode enable` | `… disable` |
| ToggleLocation | `secure location_mode` → 1, 2, 3 | `cmd location set-location-enabled true` | `… false` |

`cmd bluetooth_manager` chỉ có từ Android 13; Android 12 rơi về `svc bluetooth`. Hệ thống không hiện gì khi bật/tắt;
rung phản hồi của cử chỉ là xác nhận.

| ID | Tiêu chí |
|---|---|
| SC-10 | `of(BrightnessUp) = "input keyevent 221"`, `of(BrightnessDown) = "input keyevent 220"` |
| SC-11 | Script bật/tắt (§11, §13) chạy trên `ShellSim`: giá trị "bật" → chỉ lệnh tắt; giá trị khác hoặc `null` → chỉ lệnh bật; mã thoát = mã của lệnh đó |
| SC-12 | `ToggleWifi` coi `wifi_on` = 2 là bật, = 3 là tắt; `ToggleBluetooth` lỗi `cmd bluetooth_manager` → chạy `svc bluetooth` |
