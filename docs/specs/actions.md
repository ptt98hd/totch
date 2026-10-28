# Spec: Hành động & điều phối (`ActionDispatcher`)

Yêu cầu: FR-A1…FR-A10 · Module: `:core` (logic) + `:app` (executor)

## 1. Ma trận khả năng — `ActionRequirements.of(action)`

| Action | Capabilities |
|---|---|
| `None` | ∅ |
| `Back`, `Home`, `Recents`, `LockScreen`, `Screenshot`, `Notifications`, `QuickSettings`, `LaunchApp` | {ACCESSIBILITY, SHIZUKU} |
| `ToggleTorch`, `MediaPlayPause`, `MediaNext`, `MediaPrevious`, `VolumeUp`, `VolumeDown` | {LOCAL} |

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
| các action LOCAL, None | `null` |
| LaunchApp(pkg) | `null` từ `of()`; dùng `launch(pkg, activity)` |

`ShellCommands.launch(packageName, activityName): String?` →
`am start -n <pkg>/<activity> -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000`

| ID | Tiêu chí |
|---|---|
| SC-1 | Bảng trên đúng cho mọi action |
| SC-2 | **Chống shell injection**: package phải khớp `^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$`, activity khớp `^[A-Za-z0-9_.$]+$`; sai → `null` |
| SC-3 | `launch("com.foo; rm -rf /", "X")` → `null` |

## 4. Executor phía `:app`

| Executor | Thực thi |
|---|---|
| `LocalExecutor` | Đèn pin: `CameraManager.setTorchMode` trên camera có flash, lưu trạng thái qua `TorchCallback`. Media: `AudioManager.dispatchMediaKeyEvent` (DOWN+UP). Âm lượng: `adjustStreamVolume(STREAM_MUSIC, ±1, FLAG_SHOW_UI)` |
| `AccessibilityExecutor` | `performGlobalAction(GLOBAL_ACTION_BACK/HOME/RECENTS/LOCK_SCREEN/TAKE_SCREENSHOT/NOTIFICATIONS/QUICK_SETTINGS)`; LaunchApp: `startActivity(launchIntent)` từ service. `isAvailable()` = service đang chạy |
| `ShizukuExecutor` | `ITouchService.exec(ShellCommands…)`. `isAvailable()` = Shizuku binder sống + đã cấp quyền + UserService đã bind |

Phản hồi: nếu `Ok` và `haptics` bật → rung `EFFECT_TICK` (FR-A10). Nếu `Failed` → Toast ngắn
"Không thể thực hiện: <tên hành động>" (FR-A9).
