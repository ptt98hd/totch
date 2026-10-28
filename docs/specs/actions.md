# Spec: Hành động & điều phối (`ActionDispatcher`)

Yêu cầu: FR-A1…FR-A13 · Module: `:core` (logic) + `:app` (executor)

## 1. Ma trận khả năng — `ActionRequirements.of(action)`

| Action | Capabilities |
|---|---|
| `None` | ∅ |
| `Back`, `Home`, `Recents`, `LockScreen`, `Screenshot`, `Notifications`, `QuickSettings`, `LaunchApp`, `Assistant`, `LaunchShortcut` | {ACCESSIBILITY, SHIZUKU} |
| `ToggleTorch`, `MediaPlayPause`, `MediaNext`, `MediaPrevious`, `VolumeUp`, `VolumeDown`, `VolumePanel` | {LOCAL} |

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
| các action LOCAL, None | `null` |
| LaunchApp(pkg) | `null` từ `of()`; dùng `launch(pkg, activity)` |
| LaunchShortcut | `null` từ `of()`; dùng `startIntent(intentUri)` |

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

## 4. Executor phía `:app`

| Executor | Thực thi |
|---|---|
| `LocalExecutor` | Đèn pin: `CameraManager.setTorchMode` trên camera có flash, lưu trạng thái qua `TorchCallback`. Media: xem §5. Âm lượng: `adjustStreamVolume(STREAM_MUSIC, ±1, FLAG_SHOW_UI)`. Bảng âm lượng: `adjustStreamVolume(STREAM_MUSIC, ADJUST_SAME, FLAG_SHOW_UI)` (chỉ hiện thanh, không đổi mức) |
| `AccessibilityExecutor` | `performGlobalAction(GLOBAL_ACTION_BACK/HOME/RECENTS/LOCK_SCREEN/TAKE_SCREENSHOT/NOTIFICATIONS/QUICK_SETTINGS)`; LaunchApp: `startActivity(launchIntent)` từ service; Assistant: `startActivity(ACTION_ASSIST)`, không có thì `ACTION_VOICE_COMMAND`; LaunchShortcut: `Intent.parseUri(intentUri)` (bỏ selector và cờ grant URI) + `FLAG_ACTIVITY_NEW_TASK`. `isAvailable()` = service đang chạy |
| `ShizukuExecutor` | `ITouchService.exec(ShellCommands…)`. `isAvailable()` = Shizuku binder sống + đã cấp quyền + UserService đã bind |

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
