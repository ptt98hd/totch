# Spec: Chế độ nhận chạm & vòng đời service

> **v1.4 (FR-D8):** chỉ còn Shizuku. Bỏ `DetectionMode`, `Settings.mode` và toàn bộ Accessibility mode (§3, DM-10…DM-14,
> DM-21 chỉ còn giá trị lịch sử). `NotchAccessibilityService` không còn overlay, chỉ là quyền tùy chọn để thực thi
> hành động (§6). JSON cũ có `"mode"` vẫn đọc được (key bị bỏ qua, ST-23).

Yêu cầu: FR-D1…FR-D7, FR-S8, FR-G10, NFR-3, NFR-8 · Module: `:app`

## 1. Trạng thái engine

```kotlin
sealed interface EngineStatus {
    data object Disabled                      // settings.enabled = false
    data object WaitingShizuku                // Shizuku chưa chạy / binder chết
    data object ShizukuPermissionRequired
    data object Starting                      // v1.4: Shizuku sẵn sàng, pipeline đang kết nối
    data object Stopping                      // v1.4: đã tắt nhưng pipeline chưa dừng hẳn
    data class Error(val message: String)     // vd. không tìm thấy touchscreen, getevent lỗi
    data object Running
    data object PausedRotation                // FR-R4
}
```
Trạng thái được expose qua `StateFlow<EngineStatus>` (singleton `GestureEngine` trong `TotchApp`) để UI hiển thị.

## 2. Shizuku mode
| ID | Tiêu chí (kiểm thử thủ công trên máy thật / kiểm thử JVM với fake) |
|---|---|
| DM-1 | `enabled` → `NotchForegroundService` khởi động với thông báo kênh "Gesture service" (low importance) |
| DM-2 | Shizuku chưa chạy → `WaitingShizuku`; khi `Shizuku.addBinderReceivedListenerSticky` báo sẵn sàng → tự tiếp tục |
| DM-3 | Chưa cấp quyền → `ShizukuPermissionRequired`; UI có nút gọi `Shizuku.requestPermission` |
| DM-4 | UserService chạy `getevent -pl`, chọn touchscreen (GD-3), rồi `getevent -lt <path>`; mỗi dòng đẩy qua callback AIDL `oneway` |
| DM-5 | Không có touchscreen hoặc process `getevent` thoát → `Error` |
| DM-6 | Binder chết giữa chừng → `WaitingShizuku`, không crash (NFR-8) |
| DM-7 | Tắt switch → dừng `getevent`, unbind UserService, dừng foreground service |

## 3. Accessibility mode
| ID | Tiêu chí |
|---|---|
| DM-10 | `mode == ACCESSIBILITY` và service đã bật → overlay hiển thị đúng `Rect` vùng notch |
| DM-11 | Settings thay đổi (padding, vùng thủ công) → overlay cập nhật vị trí trong ≤ 1 khung hình, không tạo lại service |
| DM-12 | Xoay ngang → overlay bị gỡ (`PausedRotation`); xoay dọc → thêm lại |
| DM-13 | `enabled=false` hoặc đổi sang SHIZUKU → overlay bị gỡ, service vẫn chạy nhưng không làm gì |
| DM-14 | Config XML: không khai báo `accessibilityEventTypes` (không nhận event nào), `canRetrieveWindowContent=false`, `isAccessibilityTool=false`, có `description` giải thích rõ |

## 4. Khởi động cùng máy (FR-D6)
| ID | Tiêu chí |
|---|---|
| DM-20 | `BOOT_COMPLETED` + `enabled` → start foreground service (vào `WaitingShizuku` nếu Shizuku chưa sẵn) |
| ~~DM-21~~ | Bỏ ở v1.4 (không còn Accessibility mode) |
| DM-22 | `startOnBoot=false` (FR-S8): `BOOT_COMPLETED` → lưu `enabled=false` (service không chạy) cho tới khi người dùng bật lại. `startOnBoot=true` → như DM-20/21. Quyết định là hàm thuần `bootAction(settings)` → `START_SERVICE` / `DISABLE` / `NONE` |

## 4b. Tối ưu pin (FR-D7)
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Home có thẻ khuyến nghị "Không giới hạn pin" (cả 2 mode) khi
`PowerManager.isIgnoringBatteryOptimizations` = false; nút mở hộp thoại hệ thống
`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Không bắt buộc để chạy.

## 5. Engine (kiểm thử JVM được)
`GestureEngine` nhận `TouchEvent`, giữ một `GestureRecognizer`, lập lịch đúng 1 coroutine cho
`nextDeadlineMs()` (NFR-3), tra binding và gọi `ActionDispatcher`.

| ID | Tiêu chí |
|---|---|
| EN-1 | TAP với binding `Home` → dispatcher nhận `Home` |
| EN-2 | Gesture có binding `None` → dispatcher không được gọi, không rung |
| EN-3 | Settings đổi → `recognizer.config` cập nhật ngay (FR-S3) |
| EN-4 | DOWN rồi chờ (virtual time) 500 ms → LONG_PRESS được dispatch không cần sự kiện mới |
| EN-5 | Timestamp sự kiện lệch đồng hồ của engine > 10 s (nguồn dùng clock khác) → tự quy đổi theo độ lệch, timeout vẫn đúng |
| EN-6 | Dispatch `Ok` + `haptics` bật → rung; `Failed` → báo lỗi (toast), không rung |
| EN-7 | Chạm ngoài vùng hoặc `region == null` → không dispatch |
| EN-8 | Nhiều gesture liên tiếp được dispatch tuần tự, đúng thứ tự |
| EN-9 | `SWIPE_DOWN` gán `Notifications` → vuốt xuống không dispatch gì (FR-G10, dùng `effectiveAction`) |
| EN-10 | `SWIPE_RIGHT_HOLD` gán `VolumeUp`: vuốt phải rồi giữ 500 + 2000 ms (virtual time) → `VolumeUp` dispatch đúng **1** lần (v1.3: không lặp, FR-G11) |
| EN-11 | `SWIPE_RIGHT` gán `MediaNext` nhưng nằm trong `disabledGestures` → vuốt phải không dispatch gì (FR-G12) |

Đồng hồ: engine dùng `SystemClock.uptimeMillis()` (CLOCK_MONOTONIC). `MotionEvent.eventTime` dùng cùng
đồng hồ; `getevent` gọi `EVIOCSCLOCKID(CLOCK_MONOTONIC)` nên timestamp `-t` cũng vậy. EN-5 là lưới an toàn.

Trạng thái (`resolveEngineStatus(settings, shizuku, pipeline, rotation)`, test `EngineStatusTest`):
`!enabled` → pipeline Running/Connecting ? `Stopping` : `Disabled`; Shizuku chưa chạy `WaitingShizuku` → chưa cấp
quyền `ShizukuPermissionRequired` → pipeline lỗi `Error` → xoay `PausedRotation` → pipeline chạy `Running`, còn lại
`Starting`.

| ID | Tiêu chí |
|---|---|
| DM-30 | `enabled=false` + pipeline Running/Connecting → `Stopping`; `enabled=true`, Shizuku sẵn sàng, pipeline Idle/Connecting → `Starting` |

## 6. Quyền tùy chọn qua Shizuku (v1.4 — FR-D9)
`SystemStatus.setPermission(permission, granted): Boolean` chạy lệnh shell bằng `ShizukuShell` — bind một
`TouchUserService` riêng (tag `shell`) chỉ trong lúc chạy lệnh, rồi unbind (remove). Lệnh là hàm thuần trong `:core`
(`PermissionCommands`). Trả `false` khi Shizuku chưa sẵn sàng hoặc lệnh lỗi → UI mở trang hệ thống.

| Quyền | Bật | Tắt | Dùng để |
|---|---|---|---|
| Trợ năng | ghi `enabled_accessibility_services` (thêm service, giữ service khác) + `accessibility_enabled 1` | ghi lại danh sách không có service (rỗng → `settings delete`) | Thực thi global action, mở app/shortcut/trợ lý từ nền (ưu tiên hơn Shizuku) |
| Thông báo (13+) | `pm grant <pkg> POST_NOTIFICATIONS` | không qua Shizuku (`pm revoke` giết app) → trang thông báo của app | Thông báo foreground service |
| Điều khiển media | `cmd notification allow_listener <component>` | `disallow_listener` | `MediaController` (FR-A6) |
| Pin không giới hạn | `cmd deviceidle whitelist +<pkg>` | `whitelist -<pkg>` | Giữ service sống (FR-D7) |

| ID | Tiêu chí |
|---|---|
| PC-1 | `accessibility(current, c, true)` giữ các service khác, không thêm trùng (so sánh dạng đầy đủ của tên rút gọn `pkg/.Cls`), thêm `accessibility_enabled 1`; `false` bỏ c, danh sách rỗng → `settings delete secure enabled_accessibility_services`; `"null"` coi như rỗng |
| PC-2 | `notificationListener(c, allowed)` = `cmd notification allow_listener|disallow_listener 'c'` |
| PC-3 | `batteryUnrestricted(pkg, on)` = `cmd deviceidle whitelist +'pkg'` / `-'pkg'` |
| PC-4 | `grantNotifications(pkg)` = `pm grant 'pkg' android.permission.POST_NOTIFICATIONS`; mọi giá trị được single-quote cho shell |
