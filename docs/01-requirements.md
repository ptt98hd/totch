# 01 — Đặc tả yêu cầu nghiệp vụ (SRS)

> Trạng thái: **Đã chốt v1.0 (MVP)**, v1.1 (§8), v1.2 (§9), v1.3 (§10), v1.4 (§11), v1.5 (§12), v1.6.x (§13–16), v1.7 (§17), v1.8 (§18), v1.8.1 (§19), v1.8.2 (§20), v1.8.3 (§21), **v1.9** (§22) · Cập nhật: 2026-10-30

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
| Detection mode | ~~Cơ chế nhận chạm: `SHIZUKU` hoặc `ACCESSIBILITY`~~ — v1.4: chỉ còn Shizuku (`getevent`), FR-D8. v1.7: **nguồn chạm** (`TouchSource`) tự chọn — Shizuku nếu sẵn sàng, không thì Trợ năng (FR-D13) |
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
| NFR-4 | Không thư viện độc quyền (yêu cầu F-Droid), không tracking. v1.6: có quyền INTERNET **chỉ** cho FR-U27 — mạng chỉ dùng khi người dùng bấm kiểm tra / cập nhật, chỉ tới GitHub (`api.github.com`, `github.com` và CDN tải file của GitHub). v1.6.1: thêm kiểm tra khi mở app / định kỳ do người dùng **tự bật** (mặc định tắt, FR-U29) — [update §2](specs/update.md) |
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
| FR-U11 | Icon mới theo ảnh tham khảo, vẫn là dynamic (Monet + themed icon) (v1.6.2: thay bằng FR-U32) | [ui](specs/ui.md) |

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

## 12. Phiên bản 1.5

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-D10 | Tắt quyền **Thông báo** qua Shizuku: Android luôn giết app khi thu hồi quyền runtime (kể cả khi tắt trong Cài đặt), nên Totch hỏi xác nhận rồi chạy `pm revoke` **tách rời** (process shell riêng) và mở lại Totch ngay sau đó | [detection-modes §6](specs/detection-modes.md) |
| FR-A14 | Hành động **Mở activity**: activity được export (không yêu cầu quyền) của app có launcher; tab **Activities** trong picker hiển thị như tab Shortcut (nhóm theo app, mở rộng/thu gọn, tìm kiếm) | [actions §8](specs/actions.md), [ui §3](specs/ui.md) |
| FR-G14 | Vuốt xuống / Vuốt xuống & giữ dùng được khi **Trợ năng** đang bật: service đặt overlay trong suốt trên vùng notch để chặn chạm (hệ thống không kéo thanh thông báo), cử chỉ vẫn nhận qua Shizuku. Thay FR-G10/FR-G13 khi có Trợ năng | [detection-modes §7](specs/detection-modes.md), [settings](specs/settings.md) |
| FR-R8 | Tùy chọn **Nhận cử chỉ khi xoay ngang** (mặc định tắt): bật → khi xoay, vùng chạm vẫn là notch vật lý (tính theo hướng dọc tự nhiên), engine tiếp tục chạy. Thay FR-R4 khi bật | [notch-region](specs/notch-region.md), [detection-modes §1](specs/detection-modes.md) |
| FR-U23 | Màn **Settings**: section **System** lên đầu gồm Haptic feedback (chuyển từ tab Notch), Nhận cử chỉ khi xoay ngang, Khởi động cùng máy; mọi dòng có icon; các dòng trong section ngăn bằng đường kẻ ngang. Tab Notch bỏ section Phản hồi | [ui §3](specs/ui.md) |
| FR-U24 | Settings: System xếp Start on boot → Haptic feedback → Gestures in landscape; đổi "AMOLED black" thành "Amoled black" và thêm mô tả ngắn (1 dòng); mô tả trong About ngắn gọn (1 dòng), icon cuối dòng mở link là chevron (›) | [ui §3](specs/ui.md) |
| FR-U25 | Action picker: sheet chạm sát mép dưới top bar; các mục (System, Apps, Shortcuts, Activities, sau này thêm Intents, Settings…) là **hàng chip cuộn ngang** có icon thay cho segmented button — thêm mục không làm hẹp nhãn | [ui §3](specs/ui.md) |

## 13. Phiên bản 1.6

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-U26 | Settings: mỗi dòng của section **System** có mô tả ngắn **một dòng** (không xuống dòng) | [ui §3](specs/ui.md) |
| FR-A15 | Hành động **Bấm ô Cài đặt nhanh**: chạm một ô (tile) Quick Settings của app (`TileService`) qua Shizuku (`cmd statusbar click-tile`); tab **Tiles** trong picker liệt kê các ô. Chỉ ô đã được thêm vào bảng Cài đặt nhanh mới phản hồi; ô hệ thống dựng sẵn (Wi-Fi, Bluetooth…) không phải `TileService` nên không có | [actions §9](specs/actions.md), [ui §3](specs/ui.md) |
| FR-U27 | **Tự cập nhật** không cần backend: Settings › About có dòng **Check for updates** → đọc GitHub Release mới nhất, có bản mới → hộp thoại ghi chú phát hành → tải APK đã ký, kiểm SHA-256, cài bằng `PackageInstaller` (hệ thống hỏi xác nhận). Không tự kiểm tra ngầm | [update](specs/update.md), [ui §3](specs/ui.md) |
| FR-A16 | Hành động **Chế độ một tay** (bật/tắt One-handed mode của Android) qua Shizuku. Android chỉ nhận lệnh khi người dùng đã gán **phím tắt Chế độ một tay** (nút Trợ năng hoặc giữ phím âm lượng); chưa gán → hành động báo lỗi | [actions §10](specs/actions.md) |
| FR-U28 | Màn **Notch**: các slider trong mỗi section ngăn cách bằng đường kẻ ngang (như Settings, FR-U23) | [ui §3](specs/ui.md) |

## 14. Phiên bản 1.6.1

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-A15 (sửa) | Bấm ô Cài đặt nhanh **không cần** thêm ô vào bảng: ô chưa có thì tạm thêm, bấm, rồi gỡ (`cmd statusbar add-tile` / `click-tile` / `remove-tile` qua Shizuku); ô đã có bấm như cũ | [actions §9](specs/actions.md) |
| FR-A16 (sửa) | Chế độ một tay chạy được **không cần** người dùng gán phím tắt: cách native (`one_handed_mode_activated`, tự gán tạm phím tắt rồi trả lại, kiểm tra trạng thái thật qua dump WMShell) và cách fallback (giả lập cử chỉ vuốt xuống ở mép dưới). Cách nào chạy được thì lưu làm cách chính; cả hai lỗi mới báo lỗi | [actions §10](specs/actions.md) |
| FR-U29 | Settings có section **Update**: switch *Kiểm tra khi mở app*, chọn *Kiểm tra định kỳ* (Tắt / Hằng ngày / Hằng tuần / Hằng tháng — chạy nền bằng JobScheduler, có bản mới thì gửi thông báo, mỗi bản một lần), dòng *Kiểm tra cập nhật* thủ công (chuyển từ About). Có bản mới khi mở app → hộp thoại cập nhật ở bất kỳ màn hình nào | [update §5–6](specs/update.md), [ui §3](specs/ui.md) |
| FR-U31 | Splash screen: giữ splash của hệ thống (icon app) đến khi đọc xong cài đặt rồi mờ dần, không còn khung trống | [ui — Splash](specs/ui.md) |
| FR-D11 | Home › Permissions thêm **Install updates** (quyền "Cài ứng dụng không rõ nguồn gốc"): bật/tắt qua Shizuku (`appops`), tắt thì hỏi xác nhận vì Android khởi động lại app; không có Shizuku → mở trang cài đặt của hệ thống | [detection-modes §6](specs/detection-modes.md), [ui §3](specs/ui.md) |

## 15. Phiên bản 1.6.2

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-A10 (sửa) | Rung phản hồi ngay khi **nhận diện** cử chỉ có gán hành động, không chờ hành động chạy xong (hành động qua Shizuku có thể mất vài trăm ms). Hành động lỗi vẫn báo toast | [actions §6](specs/actions.md), [detection-modes §5](specs/detection-modes.md) |
| FR-U32 | Icon mới: **lỗ đen** (đĩa bồi tụ vàng cam trên nền không gian tím than, màu cố định — thay icon Monet FR-U11; v1.6.3: hình và màu sửa bởi FR-U33); có layer `monochrome` cho themed icon; icon thông báo và icon F-Droid dùng cùng hình | [ui — Icon](specs/ui.md) |

## 16. Phiên bản 1.6.3

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-U33 | Sửa icon lỗ đen: tâm các vòng tròn (vòng sáng, chân trời sự kiện) đúng **chính giữa** (54, 54); đĩa bồi tụ **nằm ngang hoàn toàn** (bỏ nghiêng −10°), giữ dạng con thoi; màu lấy theo **Monet** (`@android:color/system_*`) nên đổi theo hình nền. Thay phần màu cố định của FR-U32 | [ui — Icon](specs/ui.md) |
| FR-U34 | Tab **System** của picker có **ô tìm kiếm** như các tab khác (khớp tên hành động hoặc tên nhóm, không phân biệt hoa thường và dấu tiếng Việt) | [ui §3](specs/ui.md) |
| FR-U35 | Chia lại nhóm của tab System: **Điều hướng**, **Hệ thống**, **Media**, **Âm thanh**, **Màn hình**, **Kết nối** | [ui §3](specs/ui.md) |
| FR-A17 | Hành động **màn hình** qua Shizuku: Tăng / Giảm độ sáng (phím độ sáng của hệ thống, hiện thanh độ sáng), Bật/tắt độ sáng tự động, Bật/tắt tự động xoay | [actions §11](specs/actions.md) |
| FR-A18 | Hành động **chế độ âm thanh**: Đổ chuông, Rung, Im lặng, Đổi chế độ âm thanh (lần lượt Đổ chuông → Rung → Im lặng), Bật/tắt Không làm phiền. Cần quyền **Không làm phiền** (Home › Permissions, bật qua Shizuku được) | [actions §12](specs/actions.md), [detection-modes §6](specs/detection-modes.md) |
| FR-A19 | Hành động **kết nối** qua Shizuku: bật/tắt Wi-Fi, Bluetooth, Dữ liệu di động, Chế độ máy bay, Vị trí | [actions §13](specs/actions.md) |
| FR-D12 | Home › Permissions thêm **Do Not Disturb access** (quyền truy cập Không làm phiền): bật/tắt qua Shizuku (`cmd notification allow_dnd`), không có Shizuku → mở trang cài đặt của hệ thống | [detection-modes §6](specs/detection-modes.md), [ui §3](specs/ui.md) |
| FR-U36 | Cài bản cập nhật **qua Shizuku** khi Shizuku sẵn sàng: `pm install` dưới uid shell, không cần hộp thoại xác nhận hay quyền "Cài ứng dụng không rõ nguồn gốc", cài xong tự mở lại Totch. Không có Shizuku / lỗi khi chuyển giao → dùng `PackageInstaller` như cũ | [update §7](specs/update.md) |

## 17. Phiên bản 1.7 — Shizuku tùy chọn

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-D13 | **Shizuku là tùy chọn**: không có Shizuku, Totch nhận chạm qua **Trợ năng** (overlay `TYPE_ACCESSIBILITY_OVERLAY` trên vùng notch, nhận và tiêu thụ chạm). Có Shizuku → đọc `getevent` như cũ và mở rộng: hành động qua shell, bật/tắt quyền ngay trong app, cài cập nhật không cần xác nhận. Không có cả hai → trạng thái "Bật Trợ năng hoặc Shizuku". Thay FR-D8 | [detection-modes §8](specs/detection-modes.md) |
| FR-A20 | Hành động chỉ chạy được qua Shizuku (Bấm ô Cài đặt nhanh, Chế độ một tay, độ sáng, tự xoay, kết nối) có dòng phụ "Cần Shizuku" trong picker khi Shizuku chưa sẵn sàng | [actions §14](specs/actions.md), [ui §3](specs/ui.md) |
| FR-U37 | Action picker dùng **`BottomSheetScaffold`** (sheet native của Material 3) thay sheet tự vẽ: cao **toàn màn hình** (được che top bar), chỉ giữ **bottom bar** (vẫn bấm được); không kéo để ẩn, không có thanh kéo. Thay phần "không che top bar" của FR-U20 và "sát mép dưới top bar" của FR-U25 | [ui §3](specs/ui.md) |
| FR-U38 | Ô tìm kiếm trong picker **gọn hơn** (cao 44 dp thay vì 56 dp của `SearchBar`) | [ui §3](specs/ui.md) |
| FR-U39 | Mọi dòng danh sách trong app **cùng chiều cao** (tối thiểu 72 dp, có hay không có mô tả); tiêu đề và mô tả luôn **một dòng** (cắt bằng "…"). Thay FR-U24/FR-U26 phần một dòng (nay áp dụng toàn app) | [ui §1](specs/ui.md) |
| FR-U40 | **Buy me a coffee** (prototype): nút ly cà phê ở **bên phải** top bar mọi màn hình, dòng *Buy me a coffee* ở Home (section **Ủng hộ**, cuối) và Settings (section **Ủng hộ**, trước Giới thiệu); bấm → hộp thoại ủng hộ → mở trang ủng hộ trong trình duyệt | [ui §1, §3](specs/ui.md) |
| FR-U41 | Nút **Kiểm tra cập nhật** ở top bar (bên trái nút cà phê, có chấm báo khi đã biết có bản mới): mở hộp thoại cập nhật — đang kiểm tra → bản mới nhất / có bản mới (ghi chú) → tải (tiến độ) → cài (qua Shizuku nếu sẵn sàng, không thì trình cài đặt hệ thống) / lỗi (thử lại) | [update §8](specs/update.md), [ui §1](specs/ui.md) |
| FR-U42 | Home: section **Requirement** đổi tên thành **Nâng cao** (Advanced) và chuyển xuống **dưới** Permissions; dòng Shizuku ghi rõ là tùy chọn. Thay phần thứ tự của FR-U21 | [ui §3](specs/ui.md) |

## 18. Phiên bản 1.8

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-S2 (sửa) | **Mọi cử chỉ** mặc định **chưa đặt** (`None`): lần đầu mở app, mọi cử chỉ đều tắt. Cấu hình đã lưu của người dùng cũ giữ nguyên | [settings](specs/settings.md) |
| FR-S10 | `longPressMs` mặc định **300 ms** (thay 500 ms). Giá trị đã lưu giữ nguyên | [settings](specs/settings.md) |
| FR-U43 | Picker: tab **System** có lựa chọn **Not set** / **Chưa đặt** ở đầu danh sách (trên các nhóm); chọn → binding `None`, cử chỉ tắt. Dòng cử chỉ chưa gán hiển thị "Not set" / "Chưa đặt" | [ui §3](specs/ui.md) |
| FR-U44 | Hành động và cử chỉ **không khả dụng thì bị vô hiệu** (mờ, không chọn / bật được), kèm lý do một dòng: hành động hệ thống / mở app, shortcut, activity cần Trợ năng hoặc Shizuku; hành động chỉ chạy qua Shizuku cần Shizuku; chế độ âm thanh cần quyền Không làm phiền. Cử chỉ: không có Trợ năng lẫn Shizuku → mọi cử chỉ bị vô hiệu; vuốt xuống thiếu Trợ năng như FR-G14; cử chỉ có hành động đã gán không khả dụng → switch tắt + vô hiệu, mô tả "<hành động> · <lý do>" màu lỗi, vẫn chạm được để đổi hành động. Thay FR-A20 (gợi ý "Cần Shizuku" nay đi kèm vô hiệu) | [ui §3](specs/ui.md) |
| FR-U45 | Home: mô tả trạng thái ở thẻ Totch ngắn gọn, luôn **một dòng** (cắt bằng "…") | [ui §3](specs/ui.md) |

## 19. Phiên bản 1.8.1

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-G15 | **Switch** và **Not set** tách bạch: switch quyết định cử chỉ có **nhận sự kiện** hay không (bật → engine nhận diện cử chỉ: chờ chạm đúp, giữ khi vuốt-giữ, overlay chặn vuốt xuống); **Not set** là cử chỉ đang bật, nhận sự kiện nhưng **không làm gì** (không rung). Bật switch của cử chỉ chưa đặt chỉ bật nó (không mở picker); chọn "Not set" trong picker giữ cử chỉ bật. Mặc định: mọi cử chỉ tắt và chưa đặt. Cấu hình đã lưu (≤ 1.8.0) giữ nguyên hành vi: cử chỉ bật ⇔ không bị tắt và đã gán hành động. Thay FR-G12 phần "chỉ bật khi có hành động" và FR-U43 phần "cử chỉ tắt" | [settings](specs/settings.md), [ui §3](specs/ui.md) |
| FR-U46 | Picker: tab **System** đổi tên thành **Basic** / **Cơ bản**, icon `Category` | [ui §3](specs/ui.md) |
| FR-U47 | Picker: chọn tab bằng **ô chọn** (`ExposedDropdownMenuBox`, icon + tên tab đang chọn, menu thả xuống liệt kê các tab có icon) thay hàng chip. Thay phần chip của FR-U25 | [ui §3](specs/ui.md) |
| FR-U48 | Hộp thoại **Buy me a coffee** và **cập nhật** đẹp hơn: dùng chung khung `TotchDialog` (icon lớn trong vòng tròn tonal, tiêu đề giữa, nút chính dạng nút đầy đủ chiều rộng); hộp thoại cập nhật có chip phiên bản "hiện tại → mới", ghi chú phát hành trong khối nền riêng cuộn được, tiến độ tải có phần trăm | [ui §1](specs/ui.md), [update §8](specs/update.md) |
| FR-U49 | Settings › Giao diện: switch **Show navigation labels** / **Hiện nhãn thanh điều hướng** (mặc định bật); tắt → bottom bar chỉ còn icon | [settings](specs/settings.md), [ui §1, §3](specs/ui.md) |
| FR-U50 | Dòng **Check for updates** ở Settings mở **cùng hộp thoại cập nhật** với nút ở top bar (đang kiểm tra → kết quả). Thay UP-6 phần "chỉ mở dialog khi có bản mới" | [update §8](specs/update.md) |
| FR-U51 | Settings: "Check on app launch" đổi thành **Check on launch** / **Kiểm tra khi mở**, mô tả ngắn hơn | [ui §3](specs/ui.md) |
| FR-U52 | Mọi liên kết (mã nguồn, Shizuku, trang ủng hộ) mở **trong app** bằng Custom Tabs (`androidx.browser`, mã nguồn mở) thay vì chuyển sang trình duyệt ngoài; máy không có trình duyệt hỗ trợ → mở như cũ | [ui §1](specs/ui.md) |
| FR-U53 | Mọi thanh trượt trong app dùng **nút kéo hình tròn** (thay thanh dọc của Material 3 Expressive), cùng một component | [ui §3](specs/ui.md) |
| FR-U55 | Icon: các ngôi sao nền **giống hệt nhau** (cùng kích thước, cùng hình 4 cánh), mỗi góc một ngôi sao, **đối xứng** qua hai trục giữa của icon. Sửa phần sao của FR-U33 | [ui — Icon](specs/ui.md) |
| FR-U54 | Mọi switch trong app có **icon trong nút gạt**: ✓ khi bật, ✕ khi tắt, cùng một component | [ui §1](specs/ui.md) |

## 20. Phiên bản 1.8.2

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-G16 | **Not set** / **Chưa đặt** đổi tên thành **Nothing** / **Không làm gì**. Cử chỉ đang bật với hành động Nothing khi được nhận diện vẫn **rung phản hồi** (nếu bật Rung phản hồi và máy có rung), chỉ là không thực hiện gì. Sửa FR-G15 phần "không rung" | [detection-modes §5](specs/detection-modes.md), [actions §6](specs/actions.md), [ui §3](specs/ui.md) |
| FR-U56 | Home › Permissions: dòng "Do Not Disturb access" / "Quyền Không làm phiền" đổi tên thành **Do Not Disturb** / **Không làm phiền** | [ui §3](specs/ui.md) |
| FR-U57 | Thanh trượt: giữ **giao diện Material 3** (track dày, khe hở quanh nút kéo, chấm dừng ở cuối track) nhưng nút kéo là **hình tròn**. Sửa FR-U53 phần "track mảnh không khe" | [ui §3](specs/ui.md) |
| FR-U58 | Picker: **gộp ô tìm kiếm và ô chọn loại hành động** thành một ô: icon bên trái là icon của loại đang chọn, bấm → menu thả xuống chọn loại khác (mỗi mục: icon + tên + radio, mục đang chọn được tô); placeholder "Search <loại>" / "Tìm <loại>"; bên phải là nút xóa khi có chữ, ngược lại là mũi tên thả xuống (bấm cũng mở menu). Nhãn **Apps** → **Applications**, **Tiles** → **Quick settings** / **Cài đặt nhanh**. Thay FR-U47 và ô tìm kiếm riêng của FR-U38 | [ui §3](specs/ui.md) |
| FR-D14 | **Khó bị tắt nhất có thể**: ngoài foreground service + Trợ năng + bỏ tối ưu pin, thêm job **watchdog** định kỳ (khởi động lại service nếu bị giết, sống qua reboot), tự chạy lại sau khi **cập nhật app** và sau "quick boot" của một số hãng, hẹn khởi động lại khi app bị **vuốt khỏi Gần đây** | [detection-modes §9](specs/detection-modes.md) |
| FR-D15 | **Tự bật lại Trợ năng**: khi có Shizuku, Totch tự cấp cho mình `WRITE_SECURE_SETTINGS` (giữ vĩnh viễn, không cần Shizuku sau reboot). Nếu Trợ năng bị hệ thống / ROM tắt (vd. sau khi app bị buộc dừng) mà người dùng **không** tự tắt, Totch bật lại khi chạy lại; nút Trợ năng ở Home cũng bật/tắt trực tiếp bằng quyền này khi không có Shizuku | [detection-modes §9](specs/detection-modes.md), [settings](specs/settings.md) |
| FR-D16 | Home › Permissions thêm **Keep when unused** / **Giữ khi ít dùng** (miễn "Tạm dừng hoạt động khi không dùng" — app hibernation): trạng thái từ hệ thống, bật/tắt qua Shizuku (`appops`), không có Shizuku → mở trang của app trong Cài đặt | [detection-modes §6](specs/detection-modes.md), [ui §3](specs/ui.md) |
| FR-D17 | Bật **Pin không giới hạn** qua Shizuku đồng thời gỡ hạn chế chạy nền (`RUN_ANY_IN_BACKGROUND allow`) | [detection-modes §6](specs/detection-modes.md) |
| FR-U59 | Home › Nâng cao: dòng **Auto-start** / **Tự khởi chạy** — chỉ hiện trên máy có trang quản lý tự khởi chạy / chạy nền của hãng (Xiaomi, Huawei, Honor, OPPO/realme, OnePlus, vivo, Samsung, Asus…); bấm → mở trang đó | [detection-modes §9](specs/detection-modes.md), [ui §3](specs/ui.md) |

## 21. Phiên bản 1.8.3

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-A18 (sửa) | Bỏ hành động **Rung** (chế độ âm thanh rung). **Đổi chế độ âm thanh** chỉ chuyển qua lại **Đổ chuông ↔ Im lặng** (đang Rung — do người dùng tự đặt — → Đổ chuông). Đổi tên: "Sound mode: ring / silent" → **Ring mode** / **Silent mode** ("Chế độ đổ chuông" / "Chế độ im lặng"). Cử chỉ đang gán Rung (bản cũ) được chuyển sang Im lặng | [actions §12](specs/actions.md), [settings](specs/settings.md) |
| FR-A21 | Hành động **Mở trang Cài đặt**: một trang Cài đặt hệ thống trong danh sách tuyển chọn (Wi-Fi, Bluetooth, Màn hình, Âm thanh, Pin, Ứng dụng, Tùy chọn nhà phát triển, các bảng nổi Internet / Âm lượng…), ẩn trang máy không có. Loại **Settings** / **Cài đặt** trong picker, nhóm như tab Cơ bản, tìm kiếm được. Chạy qua Trợ năng hoặc Shizuku | [actions §15](specs/actions.md), [ui §3](specs/ui.md) |
| FR-A22 | Hành động **Gửi intent**: liệt kê intent mà app (có launcher) khai báo trong manifest — `intent-filter` không có `<data>` của activity (trừ `MAIN`) và broadcast receiver (trừ broadcast của hệ thống) được export, không yêu cầu quyền. Loại **Intents** trong picker, hiển thị như Shortcuts (nhóm theo app). Activity chạy qua Trợ năng hoặc Shizuku; broadcast do Totch gửi trực tiếp | [actions §16](specs/actions.md), [ui §3](specs/ui.md) |
| FR-A23 | Hành động **Mở liên hệ**: chọn một liên hệ ngay trong app (loại **Contacts** / **Danh bạ** trong picker, cần quyền **Danh bạ** — xin khi mở loại này), khi cử chỉ chạy → mở liên hệ đó trong app Danh bạ mặc định của hệ thống. Chạy qua Trợ năng hoặc Shizuku | [actions §17](specs/actions.md), [ui §3](specs/ui.md) |

## 22. Phiên bản 1.9 — Lớp hiển thị quanh notch

| ID | Yêu cầu | Spec |
|---|---|---|
| FR-O1 | **Báo thông báo** (notification indicator): còn thông báo chưa xem (không tính thông báo thường trực / im lặng) → **viền quanh notch phát sáng nhịp thở** theo màu của app; tắt khi mở thanh thông báo hoặc thông báo bị gỡ | [notch-overlay §2](specs/notch-overlay.md) |
| FR-O2 | **Dynamic Island — media**: đang phát nhạc / video → viên đen quanh notch giãn ra hai bên (ảnh bìa, sóng nhạc); bấm → mở rộng: tên bài, nghệ sĩ, nút Trước / Phát-Dừng / Tiếp | [notch-overlay §3](specs/notch-overlay.md) |
| FR-O3 | **Dynamic Island — thông báo mới**: bung ra 3 s với icon app (chỉ metadata, FR-O7); bấm → mở thanh thông báo | [notch-overlay §3](specs/notch-overlay.md) |
| FR-O4 | **Dynamic Island — sạc pin**: cắm sạc → bung ra 3 s với icon sạc và % pin | [notch-overlay §3](specs/notch-overlay.md) |
| FR-O5 | **Dynamic Island — âm thanh**: đổi chế độ chuông / rung / im lặng, bật/tắt Không làm phiền, đổi âm lượng → viên thuốc nhỏ với icon và nhãn / thanh âm lượng | [notch-overlay §3](specs/notch-overlay.md) |
| FR-O6 | **Hiệu ứng chạm**: chạm notch → gợn sóng sáng + viền sáng theo tiến độ nhấn giữ, nháy khi nhận cử chỉ, vệt sáng theo hướng vuốt; viên đen quanh notch co giãn (spring) | [notch-overlay §4](specs/notch-overlay.md) |
| FR-O7 | Ba tính năng trên có công tắc riêng ở màn **Notch** › **Hiệu ứng**, mặc định tắt; cần **Trợ năng** (overlay vẽ trên status bar). Báo thông báo, island media / thông báo cần quyền truy cập thông báo — dòng Home › Permissions "Media control" đổi thành **Media & notifications**. Totch chỉ đọc **metadata** thông báo (gói, thời điểm, cờ, mức quan trọng, màu), không đọc tiêu đề / nội dung, không lưu. Không vẽ khi xoay ngang hoặc app toàn màn hình | [notch-overlay §1, §5, §6](specs/notch-overlay.md) |
| FR-O8 | Trợ năng nhận sự kiện `TYPE_WINDOW_STATE_CHANGED` **chỉ của System UI** để biết thanh thông báo đã mở (FR-O1); vẫn không đọc nội dung cửa sổ | [notch-overlay §2](specs/notch-overlay.md) |
