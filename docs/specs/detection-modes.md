# Spec: Chế độ nhận chạm & vòng đời service

> **v1.4 (FR-D8):** chỉ còn Shizuku. Bỏ `DetectionMode`, `Settings.mode` và toàn bộ Accessibility mode (§3, DM-10…DM-14,
> DM-21 chỉ còn giá trị lịch sử). `NotchAccessibilityService` không còn overlay, chỉ là quyền tùy chọn để thực thi
> hành động (§6). JSON cũ có `"mode"` vẫn đọc được (key bị bỏ qua, ST-23).

> **v1.7 (FR-D13):** Shizuku là **tùy chọn**. Không có Shizuku, Trợ năng nhận chạm bằng overlay (như Accessibility mode
> cũ, §3) — xem §8. `WaitingShizuku`, `ShizukuPermissionRequired` được thay bằng `NeedsAccess`.

> **v1.5:** quyền Thông báo tắt được qua Shizuku (FR-D10, PC-5); overlay chặn chạm khi có Trợ năng (FR-G14, §7);
> nhận cử chỉ khi xoay ngang (FR-R8, DM-31).

Yêu cầu: FR-D1…FR-D13, FR-S8, FR-G10, FR-G14, FR-R8, NFR-3, NFR-8 · Module: `:app`

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
| EN-6 | `haptics` bật → rung khi cử chỉ có hành động được nhận diện (v1.6.2: không chờ dispatch, kể cả khi `Failed`); `Failed` → báo lỗi (toast); `haptics` tắt → không rung |
| EN-13 | (v1.6.2, HF-4) Dispatch còn đang chạy (treo) → cử chỉ đã rung; cử chỉ thứ hai trong lúc đó cũng rung ngay dù hành động còn trong hàng đợi |
| EN-7 | Chạm ngoài vùng hoặc `region == null` → không dispatch |
| EN-8 | Nhiều gesture liên tiếp được dispatch tuần tự, đúng thứ tự |
| EN-9 | `SWIPE_DOWN` gán `Notifications` → vuốt xuống không dispatch gì (FR-G10, dùng `effectiveAction`) |
| EN-10 | `SWIPE_RIGHT_HOLD` gán `VolumeUp`: vuốt phải rồi giữ 500 + 2000 ms (virtual time) → `VolumeUp` dispatch đúng **1** lần (v1.3: không lặp, FR-G11) |
| EN-11 | `SWIPE_RIGHT` gán `MediaNext` nhưng nằm trong `disabledGestures` → vuốt phải không dispatch gì (FR-G12) |
| EN-12 | (v1.5, FR-G14) `engine.canBlockTouches = true` → `SWIPE_DOWN` gán `Notifications` được dispatch; đặt lại `false` → không dispatch (config cập nhật ngay) |

Đồng hồ: engine dùng `SystemClock.uptimeMillis()` (CLOCK_MONOTONIC). `MotionEvent.eventTime` dùng cùng
đồng hồ; `getevent` gọi `EVIOCSCLOCKID(CLOCK_MONOTONIC)` nên timestamp `-t` cũng vậy. EN-5 là lưới an toàn.

Trạng thái (`resolveEngineStatus(settings, shizuku, pipeline, rotation)`, test `EngineStatusTest`):
`!enabled` → pipeline Running/Connecting ? `Stopping` : `Disabled`; Shizuku chưa chạy `WaitingShizuku` → chưa cấp
quyền `ShizukuPermissionRequired` → pipeline lỗi `Error` → xoay (và `!landscapeGestures`, v1.5) `PausedRotation` →
pipeline chạy `Running`, còn lại `Starting`.

| ID | Tiêu chí |
|---|---|
| DM-30 | `enabled=false` + pipeline Running/Connecting → `Stopping`; `enabled=true`, Shizuku sẵn sàng, pipeline Idle/Connecting → `Starting` |
| DM-31 | (v1.5, FR-R8) `landscapeGestures=true`, rotation ≠ 0, pipeline Running → `Running` (không `PausedRotation`) |

Vùng của engine (v1.5): `engineRegion(settings, display)` = `notchRegion(settings, display)` khi rotation = 0; khi xoay và
`landscapeGestures` → `notchRegion(settings, display.natural())` (vùng notch vật lý theo hướng dọc tự nhiên — cùng hệ
tọa độ với `getevent`, xem notch-region §v1.5); ngược lại `null`. Hướng vuốt tính theo hướng dọc tự nhiên: "vuốt xuống"
= vuốt ra xa notch.

## 6. Quyền tùy chọn qua Shizuku (v1.4 — FR-D9)
`SystemStatus.setPermission(permission, granted): Boolean` chạy lệnh shell bằng `ShizukuShell` — bind một
`TouchUserService` riêng (tag `shell`) chỉ trong lúc chạy lệnh, rồi unbind (remove). Lệnh là hàm thuần trong `:core`
(`PermissionCommands`). Trả `false` khi Shizuku chưa sẵn sàng hoặc lệnh lỗi → UI mở trang hệ thống.

| Quyền | Bật | Tắt | Dùng để |
|---|---|---|---|
| Trợ năng | ghi `enabled_accessibility_services` (thêm service, giữ service khác) + `accessibility_enabled 1` | ghi lại danh sách không có service (rỗng → `settings delete`) | Thực thi global action, mở app/shortcut/trợ lý từ nền (ưu tiên hơn Shizuku) |
| Thông báo (13+) | `pm grant <pkg> POST_NOTIFICATIONS` | v1.5: xác nhận → `pm revoke` tách rời rồi mở lại Totch (PC-5) | Thông báo foreground service |
| Điều khiển media | `cmd notification allow_listener <component>` | `disallow_listener` | `MediaController` (FR-A6) |
| Không làm phiền (v1.6.3) | `cmd notification allow_dnd <pkg>` | `disallow_dnd` | Đổi chế độ chuông / Không làm phiền (FR-A18, FR-D12) |
| Pin không giới hạn | `cmd deviceidle whitelist +<pkg>` | `whitelist -<pkg>` | Giữ service sống (FR-D7) |
| Cài bản cập nhật (v1.6.1) | `appops set <pkg> REQUEST_INSTALL_PACKAGES allow` | xác nhận → `appops set … deny` tách rời rồi mở lại Totch (PC-7) | Cài APK tự cập nhật không cần hỏi quyền "Cài ứng dụng không rõ nguồn gốc" (FR-U27, FR-D11) |

| ID | Tiêu chí |
|---|---|
| PC-1 | `accessibility(current, c, true)` giữ các service khác, không thêm trùng (so sánh dạng đầy đủ của tên rút gọn `pkg/.Cls`), thêm `accessibility_enabled 1`; `false` bỏ c, danh sách rỗng → `settings delete secure enabled_accessibility_services`; `"null"` coi như rỗng |
| PC-2 | `notificationListener(c, allowed)` = `cmd notification allow_listener|disallow_listener 'c'` |
| PC-3 | `batteryUnrestricted(pkg, on)` = `cmd deviceidle whitelist +'pkg'` / `-'pkg'` |
| PC-4 | `grantNotifications(pkg)` = `pm grant 'pkg' android.permission.POST_NOTIFICATIONS`; mọi giá trị được single-quote cho shell |
| PC-6 | (v1.6.1) `allowInstallPackages(pkg)` = `appops set 'pkg' REQUEST_INSTALL_PACKAGES allow` |
| PC-7 | (v1.6.1) `denyInstallPackagesAndRestart(pkg, activity)`: như PC-5 với lệnh `appops set <pkg> REQUEST_INSTALL_PACKAGES deny`; tên sai định dạng → `null` |
| PC-8 | (v1.6.3) `dndAccess(pkg, allowed)` = `cmd notification allow_dnd 'pkg'` / `disallow_dnd 'pkg'` |
| PC-5 | `revokeNotificationsAndRestart(pkg, activity)` = `setsid sh -c '<script>' </dev/null >/dev/null 2>&1 &` với script = `sleep 1; pm revoke <pkg> android.permission.POST_NOTIFICATIONS; am start -n <pkg>/<activity>`; package/activity sai định dạng (SC-2) → `null` |

**Vì sao thu hồi Thông báo lại giết app (FR-D10).** Android giết process của app mỗi khi một quyền runtime bị thu hồi —
kể cả khi người dùng tắt Thông báo trong Cài đặt; không có cách thu hồi `POST_NOTIFICATIONS` mà không giết app qua shell.
Vì vậy: Home hiện hộp thoại xác nhận "Totch sẽ khởi động lại"; đồng ý → `setPermission(NOTIFICATIONS, false)` chạy lệnh
PC-5. Lệnh chạy trong session riêng (`setsid`, nền) nên sống sót khi Shizuku gỡ UserService; `sleep 1` để lời gọi
binder trả về trước khi app bị giết. Sau khi mở lại, foreground service tự chạy nếu `enabled` (DM-1).

**Cài bản cập nhật (v1.6.1 — FR-D11).** Trạng thái = `PackageManager.canRequestPackageInstalls()`. Thu hồi quyền đặc biệt
này cũng làm hệ thống giết app (ActivityManager theo dõi app-op `REQUEST_INSTALL_PACKAGES`), nên dùng cùng cách với
Thông báo: hỏi xác nhận rồi chạy lệnh tách rời và mở lại Totch. Không có Shizuku → mở
`Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` (`package:<pkg>`).

**Không làm phiền (v1.6.3 — FR-D12).** Manifest khai báo `ACCESS_NOTIFICATION_POLICY`; trạng thái =
`NotificationManager.isNotificationPolicyAccessGranted`. Thu hồi không giết app nên không hỏi xác nhận. Không có Shizuku
→ mở `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`.

## 7. Chặn chạm bằng Trợ năng (v1.5 — FR-G14)

Shizuku chỉ **đọc** chạm, không chặn được nên vuốt xuống tại notch luôn kéo thanh thông báo (R2). Khi Trợ năng đang bật,
`NotchAccessibilityService` đặt một view trong suốt `TYPE_ACCESSIBILITY_OVERLAY` đúng bằng vùng notch (như overlay của
Accessibility mode cũ, DM-10/11): view **nuốt** mọi chạm (status bar không nhận được) nhưng **không** chuyển chạm cho
engine — engine vẫn nhận qua `getevent` nên không bị phát cử chỉ hai lần.

- `canBlockTouches` = Accessibility service đang kết nối. Khi true, `SWIPE_DOWN`, `SWIPE_DOWN_HOLD` khả dụng
  (`Gesture.isAvailable(canBlockTouches)`, settings ST-24).
- Overlay hiện khi: `enabled` ∧ rotation = 0 ∧ vùng notch ≠ null ∧ có ít nhất một cử chỉ vuốt xuống đang bật
  (`needsTouchBlocking(settings)`, ST-26). Ngoài ra gỡ overlay (không chặn chạm vô ích). Ở landscape không cần chặn: vuốt
  ra xa notch không phải là kéo thanh thông báo.
- Vùng overlay cập nhật theo settings / display không tạo lại service (như DM-11).

| ID | Tiêu chí |
|---|---|
| DM-15 | `needsTouchBlocking(s)` = `s.enabled` ∧ `isGestureOn(SWIPE_DOWN, true) ∨ isGestureOn(SWIPE_DOWN_HOLD, true)` |
| DM-16 | Overlay nuốt chạm (`onTouchEvent` trả true) và không gọi engine (kiểm thủ công MT-A1) |

## 8. Nguồn chạm: Shizuku hoặc Trợ năng (v1.7 — FR-D13)

Totch chạy được với **một trong hai**: Shizuku (đọc `getevent`, §2) hoặc Trợ năng (overlay nhận chạm). Shizuku vẫn được
ưu tiên khi sẵn sàng: không chiếm chạm ngoài trường hợp chặn vuốt xuống (§7) và mở thêm tính năng (hành động shell —
actions §14, bật/tắt quyền trong app — §6, cài cập nhật im lặng — update §7).

```kotlin
enum class TouchSource { NONE, SHIZUKU, ACCESSIBILITY }

/** DM-40 */
fun touchSource(shizuku: ShizukuState, pipeline: PipelineState, accessibility: Boolean): TouchSource
```

| Shizuku sẵn sàng | Pipeline | Trợ năng | Nguồn |
|---|---|---|---|
| có | không `Failed` | bất kỳ | `SHIZUKU` |
| có | `Failed` | có | `ACCESSIBILITY` (dự phòng khi `getevent` lỗi, vd. ROM chặn `/dev/input` — R1) |
| có | `Failed` | không | `SHIZUKU` (để hiện `Error`) |
| không | bất kỳ | có | `ACCESSIBILITY` |
| không | bất kỳ | không | `NONE` |

**Overlay của Trợ năng** (mở rộng §7): `NotchAccessibilityService` đặt overlay trên vùng notch khi engine `Running` và
(nguồn = `ACCESSIBILITY` **hoặc** `needsTouchBlocking`). Overlay luôn nuốt chạm; khi nguồn = `ACCESSIBILITY` nó còn
**chuyển** chạm cho engine (`GestureEngine.onTouch`) — tọa độ màn hình `rawX/rawY`, thời gian `eventTime`
(`uptimeMillis`, cùng đồng hồ với engine). Chuỗi chạm bắt đầu trong overlay vẫn được giao cho overlay khi ngón tay ra
ngoài vùng, nên vuốt hoạt động. Khi nguồn = `SHIZUKU`, overlay không chuyển (tránh nhận cử chỉ hai lần). Đổi nguồn →
`engine.reset()`.

Nguồn `ACCESSIBILITY` chỉ chạy ở hướng dọc (vùng overlay = `region` theo hướng hiện tại, `null` khi xoay):
`landscapeGestures` (FR-R8) chỉ có tác dụng với Shizuku.

Ánh xạ `MotionEvent` → `TouchEvent` (`touchEventOf(actionMasked, rawX, rawY, eventTime, pointerCount)`, hàm thuần):
`ACTION_DOWN` → DOWN, `ACTION_MOVE` → MOVE, `ACTION_UP` → UP, `ACTION_CANCEL` → CANCEL, `ACTION_POINTER_DOWN` /
`ACTION_POINTER_UP` → MOVE với `pointerCount` của sự kiện (recognizer hủy khi ≥ 2, FR-G7), còn lại → `null`.

Trạng thái (`resolveEngineStatus(settings, shizuku, pipeline, rotation, accessibility)`):
`!enabled` → như cũ (`Stopping` / `Disabled`); nguồn `NONE` → `NeedsAccess`; nguồn `SHIZUKU` → pipeline lỗi `Error` →
xoay (và `!landscapeGestures`) `PausedRotation` → pipeline chạy `Running`, còn lại `Starting`; nguồn `ACCESSIBILITY` →
xoay `PausedRotation`, còn lại `Running`.

Foreground service (DM-1) vẫn chạy khi `enabled` ở cả hai nguồn (giữ process sống, thông báo trạng thái). Thông báo khi
`NeedsAccess`: "Turn on Accessibility or Shizuku" / "Bật Trợ năng hoặc Shizuku".

| ID | Tiêu chí |
|---|---|
| DM-40 | `touchSource` đúng bảng trên |
| DM-41 | `enabled`, Shizuku chưa chạy / chưa cấp quyền, Trợ năng tắt → `NeedsAccess` |
| DM-42 | `enabled`, Shizuku chưa sẵn sàng, Trợ năng bật → `Running` (pipeline `Idle`); xoay → `PausedRotation` kể cả khi `landscapeGestures` |
| DM-43 | Shizuku sẵn sàng, pipeline `Failed`, Trợ năng bật → `Running` (dự phòng); Trợ năng tắt → `Error` |
| DM-44 | `touchEventOf` ánh xạ đúng như trên; `ACTION_POINTER_DOWN` giữ `pointerCount` = 2; hành động khác (vd. `ACTION_HOVER_MOVE`) → `null` |
| DM-45 | (thủ công) Không có Shizuku, bật Trợ năng: chạm / chạm đúp / vuốt tại notch chạy đúng hành động, chạm ở notch không tới app bên dưới |

DM-2, DM-3 (chờ Shizuku / thiếu quyền Shizuku) không còn là trạng thái engine: Shizuku chỉ hiện ở Home › Nâng cao.
