# Spec: Hành động & điều phối (`ActionDispatcher`)

Yêu cầu: FR-A1…FR-A16 · Module: `:core` (logic) + `:app` (executor)

## 1. Ma trận khả năng — `ActionRequirements.of(action)`

| Action | Capabilities |
|---|---|
| `None` | ∅ |
| `Back`, `Home`, `Recents`, `LockScreen`, `Screenshot`, `Notifications`, `QuickSettings`, `LaunchApp`, `Assistant`, `LaunchShortcut`, `LaunchActivity` | {ACCESSIBILITY, SHIZUKU} |
| `ToggleTorch`, `MediaPlayPause`, `MediaNext`, `MediaPrevious`, `VolumeUp`, `VolumeDown`, `VolumePanel` | {LOCAL} |
| `ClickTile`, `OneHandedMode` (v1.6) | {SHIZUKU} |

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
| OneHandedMode | script bật/tắt `one_handed_mode_activated` (§10) |
| các action LOCAL, None | `null` |
| LaunchApp(pkg) | `null` từ `of()`; dùng `launch(pkg, activity)` |
| LaunchShortcut | `null` từ `of()`; dùng `startIntent(intentUri)` |
| LaunchActivity | `null` từ `of()`; dùng `startActivity(pkg, activity)` (v1.5) |
| ClickTile | `null` từ `of()`; dùng `clickTile(pkg, service)` (v1.6) |

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
| SC-8 | (v1.6) `clickTile("com.a", "com.a.qs.Tile$Inner")` → `cmd statusbar click-tile 'com.a/com.a.qs.Tile$Inner'`; tên sai định dạng (SC-2) → `null` |
| SC-7 | (v1.5) `startActivity("com.a", "com.a.Main$Sub")` → `am start -n 'com.a/com.a.Main$Sub' -f 0x10000000`; tên sai định dạng (SC-2) → `null` |

## 4. Executor phía `:app`

| Executor | Thực thi |
|---|---|
| `LocalExecutor` | Đèn pin: `CameraManager.setTorchMode` trên camera có flash, lưu trạng thái qua `TorchCallback`. Media: xem §5. Âm lượng: `adjustStreamVolume(STREAM_MUSIC, ±1, FLAG_SHOW_UI)`. Bảng âm lượng: `adjustStreamVolume(STREAM_MUSIC, ADJUST_SAME, FLAG_SHOW_UI)` (chỉ hiện thanh, không đổi mức) |
| `AccessibilityExecutor` | LaunchActivity (v1.5): `startActivity(Intent().setClassName(pkg, activity) + NEW_TASK)`. `performGlobalAction(GLOBAL_ACTION_BACK/HOME/RECENTS/LOCK_SCREEN/TAKE_SCREENSHOT/NOTIFICATIONS/QUICK_SETTINGS)`; LaunchApp: `startActivity(launchIntent)` từ service; Assistant: `startActivity(ACTION_ASSIST)`, không có thì `ACTION_VOICE_COMMAND`; LaunchShortcut: `Intent.parseUri(intentUri)` (bỏ selector và cờ grant URI) + `FLAG_ACTIVITY_NEW_TASK`. `isAvailable()` = service đang chạy |
| `ShizukuExecutor` | `ITouchService.exec(ShellCommands…)` (kể cả `ClickTile`, v1.6). `isAvailable()` = Shizuku binder sống + đã cấp quyền + UserService đã bind |

Phản hồi: nếu `Ok` và `haptics` bật → rung (FR-A10, §6). Nếu `Failed` → Toast ngắn
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
| HF-3 | Mỗi lần dispatch `Ok` rung một lần; không có vibrator → bỏ qua, không crash |

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

Thực thi: chỉ Shizuku — `cmd statusbar click-tile <pkg>/<service>` (SystemUI tìm ô trong bảng Cài đặt nhanh và bấm như
người dùng: bật/tắt, mở dialog hoặc activity của ô). Ô chưa được thêm vào bảng → hệ thống bỏ qua lệnh (không báo lỗi);
picker hiển thị gợi ý "Chỉ ô đã thêm vào Cài đặt nhanh mới phản hồi". Không tự thêm ô vào bảng của người dùng.

`filterTiles(entries, query): List<TileEntry>`:

| ID | Tiêu chí |
|---|---|
| QT-1 | Sắp theo nhãn ô rồi tên app (không phân biệt hoa thường); query (trim, không phân biệt hoa thường) khớp nhãn ô **hoặc** tên app |

## 10. Chế độ một tay (v1.6 — FR-A16)

`@SerialName("one_handed") data object OneHandedMode : Action` — nhóm System của picker, dòng phụ "Cần phím tắt Chế độ một tay".

Android không có global action / keyevent / lệnh shell cho One-handed mode. WMShell (`OneHandedController`) theo dõi
`Settings.Secure.one_handed_mode_activated`: đổi thành 1 → vào chế độ một tay, 0 → thoát; khi thoát bằng chạm ngoài /
hết giờ hệ thống tự ghi lại 0. Nhưng thay đổi bị **bỏ qua** nếu phím tắt chưa bật, tức `accessibility_button_targets`
và `accessibility_shortcut_target_service` đều không chứa `OneHandedMode` (`OneHandedSettingsUtil.getShortcutEnabled`).
Nếu `one_handed_mode_enabled` = 0 hệ thống tự bật khi nhận lệnh. Máy không hỗ trợ (`ro.support_one_handed_mode`) hoặc
ROM thay chế độ một tay riêng (Samsung) → không có tác dụng.

Lệnh (`sh -c` qua Shizuku), thoát 1 khi chưa có phím tắt để hiện Toast lỗi (FR-A9):
```sh
case "$(settings get secure accessibility_button_targets):$(settings get secure accessibility_shortcut_target_service)" in
  *OneHandedMode*) ;; *) exit 1 ;; esac
if [ "$(settings get secure one_handed_mode_activated)" = 1 ]; then v=0; else v=1; fi
settings put secure one_handed_mode_activated $v
```

| ID | Tiêu chí |
|---|---|
| SC-9 | `ShellCommands.of(OneHandedMode)` kiểm tra phím tắt (`*OneHandedMode*` trong 2 key trên, không có → `exit 1`) rồi đảo `one_handed_mode_activated` (1 → 0, khác → 1) |
