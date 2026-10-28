# 01 — Đặc tả yêu cầu nghiệp vụ (SRS)

> Trạng thái: **Đã chốt v1.0 (MVP)**, v1.1 (§8), v1.2 (§9), v1.3 (§10), **v1.4** (§11) · Cập nhật: 2026-10-28

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
| Detection mode | ~~Cơ chế nhận chạm: `SHIZUKU` hoặc `ACCESSIBILITY`~~ — v1.4: chỉ còn Shizuku (`getevent`), FR-D8 |
| Gesture | Một trong 9 cử chỉ: TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT và (v1.1) SWIPE_DOWN_HOLD, SWIPE_LEFT_HOLD, SWIPE_RIGHT_HOLD |
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
- Lệnh shell tùy chỉnh, Tasker intent, shortcut động / đã ghim của app khác (chỉ launcher mặc định đọc được).
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
| NFR-5 | Build tái lập (reproducible) bằng Gradle wrapper, JDK 21 (bytecode Java 17) |
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

## 8. Phiên bản 1.1

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-G8 | Cử chỉ **vuốt và giữ** (trái / phải / xuống): vuốt đủ xa rồi giữ yên ngón ≥ `longPressMs` | [gesture-recognition §4](specs/gesture-recognition.md) |
| FR-G9 | ~~Tùy chọn **lặp lại khi giữ** cho từng cử chỉ giữ (Long press, vuốt và giữ): hành động lặp theo `repeatIntervalMs` tới khi nhả~~ (v1.3: bỏ, FR-G11) | [gesture-recognition §4](specs/gesture-recognition.md) |
| FR-G10 | Shizuku mode: vô hiệu Swipe down và Swipe down & hold (hệ thống luôn kéo thanh thông báo, R2) | [settings](specs/settings.md) |
| FR-A10 | (sửa lỗi) Rung phản hồi hoạt động ổn định ở cả 2 mode | [actions §6](specs/actions.md) |
| FR-A11 | Hành động **Gọi trợ lý** (trợ lý mặc định của hệ thống) | [actions](specs/actions.md) |
| FR-A12 | Hành động **Mở shortcut** (shortcut tĩnh của app, nhóm theo app) | [actions §7](specs/actions.md) |
| FR-S5 | ~~Reset tách bạch: reset từng mục cài đặt; reset tổng chỉ reset cài đặt; reset gestures riêng ở màn Gestures~~ (v1.2: thay bằng FR-S9) | [settings](specs/settings.md), [ui](specs/ui.md) |
| FR-S6 | Giao diện: theme System / Light / Dark, chế độ AMOLED black | [ui](specs/ui.md) |
| FR-S7 | Ngôn ngữ hiển thị: theo hệ thống (fallback tiếng Anh) hoặc chọn English / Tiếng Việt | [ui](specs/ui.md) |
| FR-S8 | Bật/tắt khởi động cùng máy | [detection-modes §4](specs/detection-modes.md) |
| FR-D7 | Khuyến nghị bỏ tối ưu pin (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) | [detection-modes §4b](specs/detection-modes.md) |
| FR-U7 | Top bar tĩnh hiện tên màn hình; ~~Settings mở từ icon bên trái top bar; bottom bar còn Home, Gestures~~ (v1.2: thay bằng FR-U12, FR-U13) | [ui §1](specs/ui.md) |
| FR-U8 | Highlight vùng notch trực tiếp trên màn hình khi mở app; chỉnh vùng chạm thấy thay đổi ngay (bỏ dialog preview) | [ui §2](specs/ui.md) |
| FR-U9 | Màn chọn hành động dạng tab: Hệ thống & media / Ứng dụng / Shortcut | [ui §3](specs/ui.md) |
| FR-U10 | Giao diện gọn: bỏ các đoạn mô tả không cần thiết; About thành section cuối của Settings | [ui](specs/ui.md) |
| FR-U11 | Icon mới theo ảnh tham khảo, vẫn là dynamic (Monet + themed icon) | [ui](specs/ui.md) |

## 9. Phiên bản 1.2 — sắp xếp lại giao diện

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-U12 | Tách Settings thành 2 màn: **Cài đặt ứng dụng** (giao diện, hệ thống, giới thiệu — mở từ icon Settings ở **bên phải** top bar) và **Notch** (độ nhạy, vị trí, kích thước, phản hồi — tab thứ 3 của bottom bar). Thay FR-U4, FR-U7 | [ui §1, §3](specs/ui.md) |
| FR-U13 | Top bar: chỉ tên màn hình bên trái + icon Settings bên phải (màn con: Back bên trái). Bottom bar: Home, Gestures, Notch | [ui §1](specs/ui.md) |
| FR-U14 | Theme chọn qua dialog như Ngôn ngữ; hai mục này có chevron để nhận biết là mở được | [ui §3](specs/ui.md) |
| FR-S9 | Bỏ mọi nút reset tổng (reset cài đặt, reset gestures) và reset của switch/lựa chọn; chỉ slider có nút reset (bên phải slider). Thay FR-S5 | [ui §3](specs/ui.md) |
| FR-R5 | Bỏ switch "Custom touch area": slider vị trí/kích thước luôn hiển thị, giá trị ban đầu = vùng tự phát hiện; kéo slider → chuyển sang vùng thủ công | [notch-region](specs/notch-region.md) |
| FR-R6 | Vùng thủ công chỉnh được cả **vị trí dọc** (khoảng cách từ cạnh trên, dp) | [notch-region](specs/notch-region.md), [settings](specs/settings.md) |

## 10. Phiên bản 1.3

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-G11 | Bỏ hoàn toàn tính năng **lặp lại khi giữ** (FR-G9): mỗi cử chỉ giữ chỉ chạy hành động **một lần**; bỏ nút Lặp lại và slider Repeat interval | [gesture-recognition §4](specs/gesture-recognition.md), [settings](specs/settings.md) |
| FR-G12 | Mỗi cử chỉ có **switch bật/tắt** riêng; tắt → không chạy gì nhưng **vẫn nhớ** hành động đã gán, bật lại dùng tiếp. Chỉ chọn được hành động khi cử chỉ đang bật | [settings](specs/settings.md), [ui §3](specs/ui.md) |
| FR-U15 | Home: thẻ bật/tắt Notch gestures có icon trạng thái ✓ / ✗ bên trái, nằm trong một box | [ui §3](specs/ui.md) |
| FR-U16 | Chọn hành động trong **bottom sheet** (không còn là màn riêng); dùng **segmented button** thay tab: System / Applications / Shortcuts; tab System có ô tìm kiếm; bỏ mục "Không làm gì" | [ui §3](specs/ui.md) |
| FR-U17 | Đổi tên màn Notch thành **Preferences**; thứ tự section: Phản hồi → Độ nhạy → Vị trí → Kích thước; slider có nấc (bước tròn) | [ui §3](specs/ui.md) |
| FR-A13 | Hành động **Mở bảng âm lượng** (hiện thanh âm lượng hệ thống, không đổi mức âm) | [actions](specs/actions.md) |

## 11. Phiên bản 1.4 — chỉ Shizuku, giao diện mới

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-D8 | **Bỏ chế độ Accessibility**: Shizuku là yêu cầu bắt buộc (đọc `getevent`), thiếu Shizuku thì app không hoạt động. Accessibility chỉ còn là quyền tùy chọn để thực thi hành động (không overlay). Thay FR-D3, phần "chọn chế độ" của FR-U1 | [detection-modes](specs/detection-modes.md) |
| FR-D9 | Các quyền tùy chọn (Trợ năng, Thông báo, Điều khiển media, Pin không giới hạn) bật/tắt bằng switch ở Home; cấp/thu hồi **qua Shizuku** khi được, không được thì mở trang hệ thống tương ứng | [detection-modes §6](specs/detection-modes.md) |
| FR-G13 | Cử chỉ không khả dụng (Vuốt xuống, Vuốt xuống & giữ — FR-G10) luôn hiển thị switch **tắt**, mờ đi; cử chỉ đang tắt cũng mờ | [settings](specs/settings.md), [ui §3](specs/ui.md) |
| FR-U19 | Bottom bar 4 tab: Home, Gestures, **Notch** (đổi tên từ Preferences, icon notch), **Settings** (cài đặt ứng dụng, chuyển từ top bar xuống). Top bar chỉ còn tiêu đề. Thay FR-U12/FR-U13 phần điều hướng | [ui §1](specs/ui.md) |
| FR-U20 | Action picker: vẫn là sheet nhưng **không kéo để ẩn**, có nút đóng bên phải tên cử chỉ; không che top/bottom bar (vẫn bấm được); bỏ tìm kiếm hành động; mỗi nhóm là card bo góc, các dòng ngăn bằng đường kẻ; tab Shortcut là danh sách app mở rộng được (mặc định thu gọn), icon shortcut là mũi tên xuống-rồi-sang-phải | [ui §3](specs/ui.md) |
| FR-U21 | Home: thẻ **Totch** (icon bật / tắt / đang bật-tắt, mô tả ngắn "Gestures are …"); bỏ chọn chế độ; section **Requirement** (Shizuku) rồi **Permissions** (icon quyền, tên, trạng thái cấp, switch) | [ui §3](specs/ui.md) |
| FR-U18 | Slider: nút − / + chỉnh từng đơn vị, kéo bắt theo nấc nhưng không vẽ vạch; Double tap & Long press 100–1000 ms (nấc 50, nút ±10); các slider còn lại nấc 5, nút ±1; nhãn ngắn: Double tap, Long press, Swipe, Horizontal, Vertical, Width, Height, Padding. Thay bảng nấc của FR-U17 | [ui §3](specs/ui.md) |
| FR-R7 | Padding 1–10 dp (vùng nhận chạm dư ra quanh notch rộng × cao); highlight vẽ cả phần padding (viền nét đứt) | [notch-region](specs/notch-region.md), [ui §2](specs/ui.md) |
| FR-U22 | Hiệu ứng chuyển trang (trượt ngang + mờ dần theo hướng tab), sheet trượt lên/xuống | [ui §1](specs/ui.md) |
| FR-S4 | (cập nhật) `doubleTapMs` 100–1000, `longPressMs` 100–1000, `paddingDp` 1–10 | [settings](specs/settings.md) |
