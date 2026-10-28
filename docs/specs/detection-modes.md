# Spec: Chế độ nhận chạm & vòng đời service

Yêu cầu: FR-D1…FR-D6, NFR-3, NFR-8 · Module: `:app`

## 1. Trạng thái engine

```kotlin
sealed interface EngineStatus {
    data object Disabled                      // settings.enabled = false
    data object WaitingShizuku                // Shizuku chưa chạy / binder chết
    data object ShizukuPermissionRequired
    data object AccessibilityRequired         // mode ACCESSIBILITY nhưng service chưa bật
    data class Error(val message: String)     // vd. không tìm thấy touchscreen, getevent lỗi
    data object Running
    data object PausedRotation                // FR-R4
}
```
Trạng thái được expose qua `StateFlow<EngineStatus>` (singleton `GestureEngine` trong `TotchApp`) để UI hiển thị.

## 2. Shizuku mode
| ID | Tiêu chí (kiểm thử thủ công trên máy thật / kiểm thử JVM với fake) |
|---|---|
| DM-1 | `enabled && mode == SHIZUKU` → `NotchForegroundService` khởi động với thông báo kênh "Gesture service" (low importance) |
| DM-2 | Shizuku chưa chạy → `WaitingShizuku`; khi `Shizuku.addBinderReceivedListenerSticky` báo sẵn sàng → tự tiếp tục |
| DM-3 | Chưa cấp quyền → `ShizukuPermissionRequired`; UI có nút gọi `Shizuku.requestPermission` |
| DM-4 | UserService chạy `getevent -pl`, chọn touchscreen (GD-3), rồi `getevent -lt <path>`; mỗi dòng đẩy qua callback AIDL `oneway` |
| DM-5 | Không có touchscreen hoặc process `getevent` thoát → `Error`, kèm gợi ý dùng Accessibility mode |
| DM-6 | Binder chết giữa chừng → `WaitingShizuku`, không crash (NFR-8) |
| DM-7 | Tắt switch → dừng `getevent`, unbind UserService, dừng foreground service |

## 3. Accessibility mode
| ID | Tiêu chí |
|---|---|
| DM-10 | `mode == ACCESSIBILITY` và service đã bật → overlay hiển thị đúng `Rect` vùng notch |
| DM-11 | Settings thay đổi (padding, vùng thủ công) → overlay cập nhật vị trí trong ≤ 1 khung hình, không tạo lại service |
| DM-12 | Xoay ngang → overlay bị gỡ (`PausedRotation`); xoay dọc → thêm lại |
| DM-13 | `enabled=false` hoặc đổi sang SHIZUKU → overlay bị gỡ, service vẫn chạy nhưng không làm gì |
| DM-14 | Config XML: `accessibilityEventTypes` rỗng(=0), `canRetrieveWindowContent=false`, `isAccessibilityTool=false`, có `description` giải thích rõ |

## 4. Khởi động cùng máy (FR-D6)
| ID | Tiêu chí |
|---|---|
| DM-20 | `BOOT_COMPLETED` + `enabled && mode == SHIZUKU` → start foreground service (vào `WaitingShizuku` nếu Shizuku chưa sẵn) |
| DM-21 | Accessibility mode tự hoạt động lại sau boot vì hệ thống tự bind service |

## 5. Engine (kiểm thử JVM được)
`GestureEngine` nhận `TouchEvent`, giữ một `GestureRecognizer`, lập lịch đúng 1 coroutine cho
`nextDeadlineMs()` (NFR-3), tra binding và gọi `ActionDispatcher`.

| ID | Tiêu chí |
|---|---|
| EN-1 | TAP với binding `Home` → dispatcher nhận `Home` |
| EN-2 | Gesture có binding `None` → dispatcher không được gọi, không rung |
| EN-3 | Settings đổi → `recognizer.config` cập nhật ngay (FR-S3) |
| EN-4 | DOWN rồi chờ (virtual time) 500 ms → LONG_PRESS được dispatch không cần sự kiện mới |
