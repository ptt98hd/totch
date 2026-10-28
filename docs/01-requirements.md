# 01 — Đặc tả yêu cầu nghiệp vụ (SRS)

> Trạng thái: **Đã chốt v1.0 (MVP)** · Cập nhật: 2026-10-28

## 1. Tầm nhìn

**Totch** biến vùng "notch" (tai thỏ / đục lỗ camera) — vốn là vùng chết trên màn hình — thành
một nút cảm ứng đa năng. Người dùng chạm, chạm đúp, nhấn giữ hoặc vuốt ngay tại notch để
kích hoạt hành động hệ thống, điều khiển media, bật đèn pin hay mở ứng dụng.

App là **native Android (Kotlin + Jetpack Compose, Material 3 / Material You)**, mã nguồn mở
(GPL-3.0-or-later), phát hành qua **F-Droid**, không quảng cáo, không thu thập dữ liệu, không cần Internet.

## 2. Người dùng mục tiêu

| Persona | Mô tả | Nhu cầu chính |
|---|---|---|
| P1 — Power user | Đã dùng Shizuku, quen ADB | Không muốn bật Accessibility, cần độ trễ thấp |
| P2 — Người dùng phổ thông | Không biết Shizuku | Cài xong bật một quyền là dùng được |
| P3 — Người dùng một tay | Điện thoại lớn | Mở thông báo, về Home mà không phải với tay |

## 3. Thuật ngữ

| Thuật ngữ | Định nghĩa |
|---|---|
| Notch region | Hình chữ nhật trên màn hình nơi nhận cử chỉ, mặc định = bounding rect của display cutout ở cạnh trên, mở rộng thêm `padding` |
| Detection mode | Cơ chế nhận chạm: `SHIZUKU` (đọc `getevent`) hoặc `ACCESSIBILITY` (overlay) |
| Gesture | Một trong 6 cử chỉ: TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT |
| Action | Hành động được gán cho một gesture |
| Binding | Cặp (Gesture → Action) |

## 4. Phạm vi MVP

### 4.1 Trong phạm vi
- Nhận 6 cử chỉ tại vùng notch.
- 2 chế độ nhận chạm, người dùng tự chọn.
- Gán hành động cho từng cử chỉ (mỗi cử chỉ tối đa 1 hành động, có thể "Không làm gì").
- Nhóm hành động: hệ thống cơ bản, đèn pin & media, mở ứng dụng.
- Tùy chỉnh độ nhạy, vùng notch thủ công, rung phản hồi.
- Giao diện Material 3, dynamic color, dark theme, tiếng Anh + tiếng Việt.

### 4.2 Ngoài phạm vi (để sau v1)
- Lệnh shell tùy chỉnh, Tasker intent, shortcut của app khác.
- Hành động theo từng ứng dụng đang chạy (per-app profile).
- Nhận cử chỉ ở chế độ ngang (landscape) — MVP **tự tắt** khi xoay ngang.
- Cử chỉ nhiều ngón, vuốt lên.
- Sao lưu / khôi phục cấu hình.

## 5. Yêu cầu chức năng

Mỗi yêu cầu có ID; spec chi tiết và acceptance criteria nằm trong [`specs/`](specs/). Test phải
tham chiếu ID tương ứng.

### 5.1 Nhận cử chỉ — [specs/gesture-recognition.md](specs/gesture-recognition.md)
| ID | Yêu cầu |
|---|---|
| FR-G1 | Nhận diện **TAP**: chạm và nhả trong vùng notch, không di chuyển quá `touchSlop`, thời gian giữ < `longPressMs` |
| FR-G2 | Nhận diện **DOUBLE_TAP**: hai TAP liên tiếp, khoảng cách giữa lần nhả 1 và lần chạm 2 ≤ `doubleTapMs` |
| FR-G3 | Nhận diện **LONG_PRESS**: giữ ≥ `longPressMs` không di chuyển quá `touchSlop`; phát ngay khi đạt ngưỡng (không đợi nhả) |
| FR-G4 | Nhận diện **SWIPE_DOWN / LEFT / RIGHT**: điểm bắt đầu trong vùng notch, độ dời ≥ `swipeMinDistance` theo hướng chính |
| FR-G5 | Nếu DOUBLE_TAP **không** được gán hành động, TAP được phát ngay khi nhả (không chờ `doubleTapMs`) |
| FR-G6 | Chạm bắt đầu ngoài vùng notch bị bỏ qua hoàn toàn |
| FR-G7 | Chạm đa điểm (≥2 ngón) hủy cử chỉ đang nhận |

### 5.2 Vùng notch — [specs/notch-region.md](specs/notch-region.md)
| ID | Yêu cầu |
|---|---|
| FR-R1 | Tự phát hiện vùng notch từ `DisplayCutout` ở cạnh trên |
| FR-R2 | Nếu máy không có cutout, dùng vùng mặc định: chính giữa cạnh trên, rộng 20% màn hình, cao bằng status bar |
| FR-R3 | Người dùng có thể ghi đè vùng thủ công (offset X, độ rộng, độ cao) và thêm `padding` |
| FR-R4 | Tự tắt nhận cử chỉ khi màn hình không ở hướng dọc chuẩn (ROTATION_0) |

### 5.3 Chế độ nhận chạm — [specs/detection-modes.md](specs/detection-modes.md)
| ID | Yêu cầu |
|---|---|
| FR-D1 | **Shizuku mode**: đọc sự kiện chạm thô qua `getevent -lt` chạy với quyền shell (Shizuku UserService); không chặn chạm của hệ thống |
| FR-D2 | Tự tìm thiết bị input cảm ứng (có `ABS_MT_POSITION_X/Y`) và quy đổi tọa độ thô → pixel màn hình |
| FR-D3 | **Accessibility mode**: đặt overlay `TYPE_ACCESSIBILITY_OVERLAY` đúng bằng vùng notch, nhận và **tiêu thụ** chạm |
| FR-D4 | Hiển thị trạng thái từng điều kiện (Shizuku chạy? đã cấp quyền? Accessibility bật? Notification access khi có gán hành động media?) và nút dẫn tới nơi cấp quyền |
| FR-D5 | Shizuku mode chạy trong foreground service có thông báo thường trực (yêu cầu hệ thống) |
| FR-D6 | Tự khởi động lại sau reboot nếu người dùng đã bật (Shizuku mode: khi Shizuku sẵn sàng) |

### 5.4 Hành động — [specs/actions.md](specs/actions.md)
| ID | Hành động | Cần |
|---|---|---|
| FR-A1 | Không làm gì (NONE) | – |
| FR-A2 | Back, Home, Recents | Shizuku hoặc Accessibility |
| FR-A3 | Khóa màn hình, Chụp màn hình | Shizuku hoặc Accessibility |
| FR-A4 | Mở thanh thông báo, Mở Quick Settings | Shizuku hoặc Accessibility |
| FR-A5 | Bật/tắt đèn pin | Không cần quyền đặc biệt (CameraManager) |
| FR-A6 | Play/Pause, Bài tiếp, Bài trước | Khuyến nghị quyền *Notification access* (điều khiển trực tiếp `MediaController`); không có quyền → gửi phím media (không ổn định ở Accessibility mode — lỗi MT-M3 của bản 1.0.0) |
| FR-A7 | Tăng / Giảm âm lượng (hiện thanh âm lượng hệ thống) | Không cần quyền đặc biệt |
| FR-A8 | Mở một ứng dụng đã cài do người dùng chọn | Shizuku hoặc Accessibility (vượt giới hạn mở Activity từ nền) |
| FR-A9 | Nếu không có backend nào thực thi được hành động, hiển thị toast lỗi ngắn, không crash |
| FR-A10 | Rung nhẹ khi nhận diện cử chỉ có gán hành động (tắt được) |

### 5.5 Cấu hình — [specs/settings.md](specs/settings.md)
| ID | Yêu cầu |
|---|---|
| FR-S1 | Lưu bindings + cài đặt bền vững (DataStore), có giá trị mặc định hợp lý |
| FR-S2 | Bindings mặc định: TAP → NONE, DOUBLE_TAP → Khóa màn hình, LONG_PRESS → Đèn pin, SWIPE_DOWN → Thông báo, SWIPE_LEFT → Bài trước, SWIPE_RIGHT → Bài tiếp |
| FR-S3 | Thay đổi cấu hình áp dụng ngay, không cần khởi động lại service |
| FR-S4 | Giới hạn giá trị: `doubleTapMs` 150–600, `longPressMs` 300–1500, `swipeMinDistanceDp` 16–200, `paddingDp` 0–48 |

### 5.6 Giao diện — [specs/ui.md](specs/ui.md)
| ID | Yêu cầu |
|---|---|
| FR-U1 | Màn hình **Home**: công tắc bật/tắt chính, chọn chế độ, các thẻ trạng thái quyền |
| FR-U2 | Màn hình **Gestures**: danh sách 6 cử chỉ, mỗi dòng hiện hành động đang gán; chạm → bottom sheet chọn hành động |
| FR-U3 | **App picker**: danh sách app có launcher, tìm kiếm theo tên |
| FR-U4 | Màn hình **Settings**: độ nhạy, vùng notch, rung, xem trước vùng notch (highlight tạm thời trên màn hình) |
| FR-U5 | Màn hình **About**: phiên bản, license, link mã nguồn |
| FR-U6 | Material 3, dynamic color (Android 12+), dark/light theo hệ thống, edge-to-edge |

## 6. Yêu cầu phi chức năng

| ID | Yêu cầu |
|---|---|
| NFR-1 | minSdk 31 (Android 12), targetSdk 36, compileSdk 37 |
| NFR-2 | Độ trễ từ lúc nhả tay đến lúc thực thi hành động ≤ 100 ms (không tính chờ double-tap) |
| NFR-3 | CPU khi rảnh ≈ 0: không polling; chỉ xử lý khi có sự kiện input |
| NFR-4 | Không có quyền INTERNET, không thư viện độc quyền (yêu cầu F-Droid), không tracking |
| NFR-5 | Build tái lập (reproducible) bằng Gradle wrapper, JDK 17 |
| NFR-6 | Logic lõi (nhận cử chỉ, parse getevent, vùng notch, bindings) là Kotlin thuần, phủ unit test ≥ 90% dòng |
| NFR-7 | Ngôn ngữ: English (mặc định), Tiếng Việt |
| NFR-8 | Không crash khi Shizuku chết giữa chừng; tự chuyển trạng thái "chờ Shizuku" |

## 7. Giả định & rủi ro

| # | Nội dung | Giảm thiểu |
|---|---|---|
| R1 | Một số ROM chặn shell đọc `/dev/input` | Phát hiện lỗi, gợi ý dùng Accessibility mode |
| R2 | Shizuku mode không chặn được chạm: vuốt xuống ở notch vẫn kéo thanh thông báo của hệ thống | Ghi rõ trong UI; khuyến nghị gán SWIPE_DOWN = Thông báo ở mode này |
| R5 | Android bỏ qua phím media từ app không có foreground service (quan sát thực tế ở Accessibility mode) | Ưu tiên `MediaController` qua Notification access; Play/Pause dự phòng bằng `GLOBAL_ACTION_KEYCODE_HEADSETHOOK` |
| R3 | F-Droid soi kỹ app dùng AccessibilityService | Khai báo rõ mục đích trong mô tả, `isAccessibilityTool=false`, không đọc nội dung cửa sổ |
| R4 | Tọa độ thô có thể bị xoay/đảo trục so với màn hình | Spec quy đổi có xử lý; MVP chỉ chạy ở ROTATION_0 |
