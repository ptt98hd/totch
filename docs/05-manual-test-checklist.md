# 05 — Checklist kiểm thử thủ công

> Dùng cho mỗi bản release. Ký hiệu cột **KQ**: `[v]` đạt · `[x]` lỗi (ghi chú lại hiện tượng) · `[-]` không áp dụng / bỏ qua.
> Khi báo lỗi, ghi ID (vd. `MT-M3`) kèm máy, phiên bản Android, ROM và (nếu được) log:
> `adb logcat | grep -E 'Totch|AndroidRuntime'`

## 0. Thông tin lần test

| Mục | Giá trị |
|---|---|
| Phiên bản app | 1.4.0 (10400) |
| Máy / ROM / Android | |
| Kiểu notch (giọt nước / đục lỗ giữa / đục lỗ góc / không có) | |
| Phiên bản Shizuku | |
| Người test / ngày | |

## 1. Cài đặt & lần mở đầu

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-I1 | Cài mới, mở app | Home gọn (không có đoạn mô tả dài); công tắc chính tắt; không crash | [ ] |
| MT-I2 | Bật công tắc khi chưa cấp quyền nào | Công tắc vẫn bật; thẻ yêu cầu quyền hiện ✗ kèm nút hành động; status báo thiếu quyền | [ ] |
| MT-I3 | Chuyển qua lại 2 chế độ bằng segmented button | Danh sách thẻ yêu cầu đổi theo chế độ; lựa chọn được lưu sau khi đóng/mở app | [ ] |
| MT-I4 | Cập nhật đè từ 1.0.x lên 1.1.0 | Cài đặt và binding cũ được giữ nguyên; 3 cử chỉ "vuốt và giữ" mới = "Không làm gì" | [ ] |

## 2. Chế độ Trợ năng (Accessibility) — ~~bỏ ở v1.4~~ (xem §10)

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-A1 | Bấm "Open settings" ở thẻ Accessibility → bật dịch vụ "Totch notch gestures" → quay lại app | Thẻ chuyển ✓; status "Running" | [ ] |
| MT-A2 | Chạm 1 lần vào notch (TAP gán Home) | Về màn hình chính; rung nhẹ nếu bật haptics | [ ] |
| MT-A3 | Chạm đúp (gán Khóa màn hình) | Màn hình khóa; **không** kích hoạt TAP trước đó | [ ] |
| MT-A4 | Nhấn giữ ~0,5 s (gán Đèn pin) | Đèn pin bật ngay khi đủ thời gian, không cần nhả tay | [ ] |
| MT-A5 | Vuốt xuống từ notch | Mở thanh thông báo (hành động gán) | [ ] |
| MT-A6 | Vuốt trái / vuốt phải từ notch | Hành động gán tương ứng chạy đúng chiều | [ ] |
| MT-A7 | Vuốt lên từ notch | Không có gì xảy ra | [ ] |
| MT-A8 | Chạm ngay **cạnh** notch nhưng ngoài vùng (cách > padding) | App bên dưới/status bar nhận chạm bình thường; Totch không phản ứng | [ ] |
| MT-A9 | Chạm 2 ngón cùng lúc lên vùng notch | Không phát cử chỉ nào | [ ] |
| MT-A10 | Gán TAP = "Không làm gì", DOUBLE_TAP = Back; chạm 1 lần | Không có gì xảy ra; chạm đúp → Back | [ ] |
| MT-A11 | Gán DOUBLE_TAP = "Không làm gì", TAP = Home; chạm 1 lần | Home chạy **ngay** khi nhả tay (không chờ ~300 ms) | [ ] |
| MT-A12 | Mở video toàn màn hình (YouTube) và chạm vào notch | Cử chỉ vẫn hoạt động (vùng chạm vẫn ở trên cùng); ghi lại nếu gây khó chịu khi xem | [ ] |
| MT-A13 | Tắt dịch vụ Trợ năng trong Cài đặt hệ thống khi app đang chạy | Status chuyển "cần Accessibility"; không crash; bật lại → chạy lại | [ ] |
| MT-A14 | Tắt công tắc chính | Vùng chạm biến mất ngay; chạm vào status bar ở notch hoạt động như bình thường | [ ] |

## 3. Chế độ Shizuku

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-S1 | Chưa cài Shizuku → bấm nút ở thẻ "Shizuku running" | Mở trang web Shizuku | [ ] |
| MT-S2 | Shizuku đã cài nhưng chưa chạy | Status "Waiting for Shizuku"; nút cấp quyền bị vô hiệu | [ ] |
| MT-S3 | Khởi động Shizuku → bấm "Grant" → **Từ chối** | Thẻ quyền vẫn ✗; không crash; bấm lại vẫn hỏi được | [ ] |
| MT-S4 | Bấm "Grant" → **Cho phép** | Thẻ ✓; status "Running"; có thông báo thường trực | [ ] |
| MT-S5 | (Android 13+) Từ chối quyền thông báo | Service vẫn chạy; thẻ Notifications ✗; bấm lại mở trang cài đặt thông báo của app | [ ] |
| MT-S6 | Lặp lại MT-A2…MT-A7 ở chế độ Shizuku | Tất cả đúng như chế độ Trợ năng | [ ] |
| MT-S7 | Vuốt xuống ở notch | Chỉ thanh thông báo hệ thống kéo xuống; Totch **không** chạy hành động nào (FR-G10). Màn Gestures: 2 dòng vuốt xuống bị mờ | [ ] |
| MT-S8 | Độ trễ: chạm (TAP gán Home) và cảm nhận | Ghi lại: tức thì / chậm nhẹ (< 0,5 s) / chậm rõ (> 0,5 s) | [ ] |
| MT-S9 | Dừng Shizuku (Stop) khi app đang chạy | Status "Waiting for Shizuku"; không crash | [ ] |
| MT-S10 | Khởi động lại Shizuku sau MT-S9 | Tự chạy lại, **không** cần mở app | [ ] |
| MT-S11 | Thu hồi quyền Totch trong app Shizuku | Status "cần quyền Shizuku"; cấp lại → chạy lại | [ ] |
| MT-S12 | Chạm vào thông báo thường trực | Mở app Totch | [ ] |
| MT-S13 | Chuyển sang chế độ Trợ năng | Thông báo thường trực biến mất; Shizuku pipeline dừng | [ ] |

## 4. Hành động (chạy ở **cả 2 chế độ**, ghi KQ dạng `A/S`)

| ID | Hành động | Kỳ vọng | KQ A/S |
|---|---|---|---|
| MT-X1 | Back | Quay lại màn hình trước | [ ]/[ ] |
| MT-X2 | Home | Về launcher | [ ]/[ ] |
| MT-X3 | Recents | Mở màn hình đa nhiệm | [ ]/[ ] |
| MT-X4 | Khóa màn hình | Tắt màn hình/khóa; mở khóa bằng vân tay/khuôn mặt vẫn được (Smart Lock không bị mất) | [ ]/[ ] |
| MT-X5 | Chụp màn hình | Ảnh chụp được lưu, có hoạt ảnh chụp | [ ]/[ ] |
| MT-X6 | Mở thanh thông báo | Thanh thông báo kéo xuống | [ ]/[ ] |
| MT-X7 | Mở Quick Settings | Bảng Quick Settings mở rộng | [ ]/[ ] |
| MT-X8 | Đèn pin (bật rồi tắt) | Bật/tắt luân phiên đúng trạng thái | [ ]/[ ] |
| MT-X9 | Đèn pin khi đã bật từ Quick Settings | Lần chạm tiếp theo **tắt** đèn (đồng bộ trạng thái) | [ ]/[ ] |
| MT-X10 | Đèn pin khi app Camera đang mở | Hiện toast "Couldn't perform…"; không crash | [ ]/[ ] |
| MT-X11 | Tăng / Giảm âm lượng | Âm lượng media đổi 1 nấc, thanh âm lượng hệ thống hiện | [ ]/[ ] |
| MT-X12 | Mở app đã chọn khi đang ở **app khác** | App được mở lên trước | [ ]/[ ] |
| MT-X13 | Mở app đã chọn khi đang ở launcher | App được mở | [ ]/[ ] |
| MT-X14 | Mở app khi app đó đang chạy nền | App được đưa lên trước (không tạo bản sao) | [ ]/[ ] |
| MT-X15 | Gỡ app đã gán rồi kích hoạt cử chỉ | Toast lỗi; màn Gestures hiện "Not installed" | [ ]/[ ] |
| MT-X16 | Gọi trợ lý | Trợ lý mặc định (Gemini / Google Assistant / …) mở lên | [ ]/[ ] |
| MT-X17 | Shortcut tĩnh (vd. Chrome "New incognito tab", Camera "Selfie") khi đang ở app khác | Shortcut mở đúng màn hình | [ ]/[ ] |
| MT-X18 | Gỡ app có shortcut đã gán rồi kích hoạt | Toast lỗi; màn Gestures hiện "Not installed" | [ ]/[ ] |

## 4b. Vuốt và giữ (v1.1; lặp lại đã bỏ ở v1.3)

Chuẩn bị: Vuốt phải & giữ = Tăng âm lượng, Vuốt trái & giữ = Giảm âm lượng, Nhấn giữ = Đèn pin.

| ID | Kịch bản | Kỳ vọng | KQ A/S |
|---|---|---|---|
| MT-H1 | Vuốt phải rồi giữ yên ~0,5 s | Âm lượng tăng 1 nấc; nhả tay không có gì thêm | [ ]/[ ] |
| MT-H2 | Vuốt phải rồi giữ 3 s | Âm lượng chỉ tăng **1** nấc (không lặp, FR-G11) | [ ]/[ ] |
| MT-H3 | Vuốt phải nhanh rồi nhả ngay | Chạy hành động của "Vuốt sang phải" (không phải bản giữ) | [ ]/[ ] |
| MT-H4 | Gestures: không còn nút Lặp lại; Preferences: không còn slider Repeat interval | Đúng | [ ] |

## 5. Media (lỗi 1.0.0: không hoạt động ở chế độ Trợ năng)

Chuẩn bị: gán TAP = Play/Pause, SWIPE_RIGHT = Bài tiếp, SWIPE_LEFT = Bài trước.

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-M1 | Home hiện thẻ "Media control" khi có binding media; bỏ hết binding media | Thẻ hiện / ẩn tương ứng | [ ] |
| MT-M2 | Bấm "Open settings" ở thẻ → bật quyền truy cập thông báo cho "Totch media control" | Thẻ chuyển ✓ khi quay lại app | [ ] |
| MT-M3 | **Trợ năng + có quyền thông báo**: Spotify đang phát → TAP / vuốt phải / vuốt trái | Dừng-phát / bài tiếp / bài trước | [ ] |
| MT-M4 | Như MT-M3 với YouTube Music | Như trên | [ ] |
| MT-M5 | Như MT-M3 với VLC (file nhạc trong máy) | Như trên | [ ] |
| MT-M6 | **Trợ năng, KHÔNG có quyền thông báo**: Play/Pause | Dừng-phát vẫn hoạt động (dự phòng phím headset) | [ ] |
| MT-M7 | Trợ năng, không có quyền thông báo: Bài tiếp/Bài trước | Ghi lại có hoạt động hay không (cách dự phòng cuối, có thể không chạy trên một số máy) | [ ] |
| MT-M8 | **Shizuku** + có quyền / không có quyền: lặp lại MT-M3 | Hoạt động trong cả 2 trường hợp | [ ] |
| MT-M9 | Nhạc đang **dừng** (đã phát trước đó) → Play/Pause | Tiếp tục phát trên trình phát gần nhất | [ ] |
| MT-M10 | Hai trình phát cùng có phiên (vd. Spotify dừng, YouTube đang phát) → Play/Pause | Tác động lên trình **đang phát** | [ ] |
| MT-M11 | Không có trình phát nào từng mở → Play/Pause | Không crash (có thể không có gì xảy ra) | [ ] |

## 6. Vùng notch & cài đặt

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-R1 | Mở app (cả 2 chế độ) | Vùng chạm được tô màu trực tiếp trên notch ở mọi màn hình | [ ] |
| MT-R2 | Xoay ngang khi đang mở app | Không còn highlight | [ ] |
| MT-R3 | Kéo slider padding lên 48 dp (đang kéo, chưa nhả) | Highlight to ra theo ngón tay; cạnh trên vẫn sát mép | [ ] |
| MT-R4 | Tab Notch: slider vị trí/kích thước hiện sẵn với giá trị của notch thật; kéo vị trí ngang về 10% | Highlight dời sang trái ngay khi kéo; cử chỉ chỉ nhận ở vị trí mới | [ ] |
| MT-R4b | Tab Notch: kéo vị trí dọc lên 40 dp (cả 2 chế độ) | Highlight dời xuống theo; cử chỉ chỉ nhận ở vùng mới, chạm sát mép trên không còn nhận | [ ]/[ ] |
| MT-R4c | Bấm ↺ ở mọi slider vị trí/kích thước đã đổi | Vùng quay về đúng notch tự phát hiện; mọi nút ↺ của vùng chạm mờ đi | [ ] |
| MT-R5 | Máy không có notch (hoặc giả lập) | Vùng mặc định ở giữa cạnh trên, rộng ~20% | [ ] |
| MT-R6 | Xoay ngang (app hỗ trợ xoay, vd. YouTube) | Status "Paused"; chạm vùng cũ không kích hoạt gì | [ ] |
| MT-R7 | Xoay dọc lại | Hoạt động lại không cần mở app | [ ] |
| MT-R8 | Double-tap window = 600 ms rồi chạm đúp chậm | Vẫn nhận double tap; với 150 ms thì chạm đúp chậm thành 2 TAP | [ ] |
| MT-R9 | Long-press = 1500 ms | Phải giữ đủ 1,5 s mới kích hoạt | [ ] |
| MT-R10 | Swipe distance = 200 dp, vuốt ngắn | Vuốt ngắn không kích hoạt; vuốt dài thì có | [ ] |
| MT-R11 | Bật haptics (cả 2 chế độ, kể cả khi "Phản hồi chạm" của hệ thống tắt) / tắt haptics | Bật: rung rõ mỗi lần hành động chạy (lỗi 1.0.x); tắt: không rung | [ ]/[ ] |
| MT-R12 | Đổi cài đặt khi đang chạy | Có hiệu lực ngay, không cần tắt/bật | [ ] |
| MT-R13 | Top bar Gestures / Notch / Cài đặt | Không còn nút reset tổng nào; switch và lựa chọn không có nút ↺ | [ ] |
| MT-R15 | Đổi 1 slider (vd. Long-press) rồi bấm nút ↺ bên phải slider đó | Chỉ mục đó về mặc định; nút ↺ mờ đi | [ ] |
| MT-R14 | Force stop app trong Cài đặt hệ thống rồi mở lại | Cài đặt còn nguyên | [ ] |

## 7. Khởi động lại máy

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-B1 | Chế độ Trợ năng, đang bật → reboot | Sau khi mở khóa, cử chỉ hoạt động mà không mở app | [ ] |
| MT-B2 | Chế độ Shizuku, Shizuku khởi động bằng root/ADB không dây sau boot | Thông báo "Waiting for Shizuku" → tự chuyển "Running" khi Shizuku sẵn sàng | [ ] |
| MT-B3 | Chế độ Shizuku, Shizuku **không** tự khởi động | Thông báo "Waiting for Shizuku"; khởi động Shizuku thủ công → tự chạy | [ ] |
| MT-B4 | Công tắc chính tắt → reboot | Không có service/thông báo nào chạy | [ ] |
| MT-B5 | Tắt "Start on boot", công tắc chính bật → reboot (cả 2 chế độ) | Sau boot không nhận cử chỉ; mở app thấy công tắc chính tắt | [ ]/[ ] |

## 8. Giao diện

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-U1 | Theme = System, bật Dark mode hệ thống | Mọi màn hình (Home, Gestures, Action picker, Settings, dialog) đổi tối; chữ đọc rõ | [ ] |
| MT-U1b | Theme = Light / Dark (khác hệ thống) | App theo lựa chọn; icon status bar đọc rõ | [ ] |
| MT-U1c | Dark + AMOLED black | Nền đen tuyệt đối; thẻ/vùng chứa vẫn phân biệt được | [ ] |
| MT-U2 | Ngôn ngữ = System, đổi ngôn ngữ hệ thống sang Tiếng Việt / tiếng Pháp | Tiếng Việt: toàn bộ chữ tiếng Việt, kể cả toast; tiếng Pháp: tiếng Anh | [ ] |
| MT-U2b | Settings → Ngôn ngữ = Tiếng Việt khi máy đang tiếng Anh (Android 12 và 13+) | App đổi ngay sang tiếng Việt; giữ sau khi mở lại | [ ] |
| MT-U2c | (Android 13+) Đổi ngôn ngữ của Totch trong Cài đặt hệ thống → Ứng dụng | Mở lại app: mục Ngôn ngữ trong Settings khớp | [ ] |
| MT-U3 | Đổi hình nền (màu khác hẳn) | Màu app đổi theo (Material You) | [ ] |
| MT-U4 | Sau MT-U3, xem icon ở launcher | Icon đổi màu theo hình nền mới (có thể cần vài giây/khởi động lại launcher) | [ ] |
| MT-U5 | Bật "Themed icons" (Android 13+) | Icon hiển thị dạng đơn sắc theo theme | [ ] |
| MT-U6 | Cỡ chữ hệ thống lớn nhất | Không vỡ layout; slider và nút dùng được | [ ] |
| MT-U7 | Action picker → tab Ứng dụng: tìm "spo" / "SPO" | Kết quả giống nhau (không phân biệt hoa thường) | [ ] |
| MT-U10 | Top bar | Cố định khi cuộn, tên màn hình bên trái; Home/Gestures/Preferences có icon Settings bên phải; Cài đặt có nút Back | [ ] |
| MT-U13 | Bottom bar | 3 mục Home, Gestures, Preferences; ẩn ở Cài đặt | [ ] |
| MT-U14 | Cài đặt → Chủ đề / Ngôn ngữ | Dòng có chevron ›; bấm mở dialog chọn, chọn xong áp dụng ngay | [ ] |
| MT-U11 | Action picker (bottom sheet) | Segmented button System / Applications / Shortcuts; mở đúng mục của hành động đang gán; cả 3 mục tìm kiếm được; không có "Không làm gì" | [ ] |
| MT-U15 | Home: thẻ Notch gestures | Icon ✓ (đang chạy) / ✗ trong box bên trái | [ ] |
| MT-U16 | Gestures: tắt switch của Double tap đang gán Khóa màn hình, bật lại | Khi tắt: không bấm chọn được, chạm đúp không khóa máy; bật lại: vẫn là Khóa màn hình | [ ] |
| MT-U17 | Gestures: bật switch của cử chỉ chưa gán gì | Mở sheet chọn hành động; đóng sheet không chọn → switch vẫn tắt | [ ] |
| MT-U18 | Preferences | Thứ tự Phản hồi → Độ nhạy → Vị trí → Kích thước; slider nhảy theo nấc tròn | [ ] |
| MT-A13 | Gán một cử chỉ = Bảng âm lượng (cả 2 chế độ) | Thanh âm lượng hệ thống hiện ra, mức âm không đổi | [ ]/[ ] |
| MT-U12 | Icon mới ở launcher, đổi hình nền, bật Themed icons | Icon theo thiết kế mới, đổi màu theo hình nền; themed icon đơn sắc rõ | [ ] |
| MT-U8 | Icon thông báo thường trực (Shizuku) | Icon đơn sắc rõ ràng, không phải khối trắng | [ ] |
| MT-U9 | TalkBack: duyệt Home và Gestures | Các nút/thẻ đọc được nhãn có nghĩa | [ ] |

## 9. Ổn định & pin

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-P1 | Bật Battery saver | Cử chỉ vẫn hoạt động | [ ] |
| MT-P5 | Thẻ "Battery" ở Home → bấm → Cho phép | Hộp thoại hệ thống hiện; quay lại thẻ chuyển ✓ | [ ] |
| MT-P2 | Để máy qua đêm với app bật (mỗi chế độ 1 đêm) | Totch không nằm trong top tiêu thụ pin; sáng ra vẫn hoạt động | [ ] |
| MT-P3 | Dùng bình thường 1 ngày | Không có crash, không có toast lỗi lạ | [ ] |
| MT-P4 | Gỡ cài đặt Shizuku khi đang ở chế độ Shizuku | Không crash; status "Waiting for Shizuku" | [ ] |

## 10. v1.4 — chỉ Shizuku, Home / picker / slider mới

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-V1 | Cập nhật đè từ 1.3 đang ở Accessibility mode | App mở bình thường; không còn chọn chế độ; binding giữ nguyên; padding cũ > 10 về 10 | [ ] |
| MT-V2 | Home: bật Totch khi Shizuku chưa chạy | Thẻ Totch icon ✗ nền đỏ, "Gestures are waiting for Shizuku"; dòng Shizuku "Not running", chạm → mở app Shizuku | [ ] |
| MT-V3 | Khởi động Shizuku, cấp quyền | Dòng Shizuku "Running, permission granted"; thẻ Totch hiện icon xoay "turning on…" rồi ✓ "Gestures are on" | [ ] |
| MT-V4 | Tắt Totch | Icon xoay "turning off…" rồi ✗ "Gestures are off"; thông báo foreground biến mất | [ ] |
| MT-V5 | Permissions: bật/tắt Trợ năng, Media control, Pin khi Shizuku sẵn sàng | Switch đổi ngay, không mở trang hệ thống; kiểm tra lại trong Cài đặt hệ thống đúng trạng thái | [ ] |
| MT-V6 | Permissions: bật Notifications (13+) khi Shizuku sẵn sàng / tắt Notifications | Bật: cấp ngay; Tắt: mở trang thông báo của app | [ ] |
| MT-V7 | Permissions khi Shizuku chưa sẵn sàng | Bật/tắt mở trang hệ thống tương ứng | [ ] |
| MT-V8 | Gestures: Swipe down / Swipe down & hold | Mờ, switch tắt + không bấm được, nhãn "Unavailable…" | [ ] |
| MT-V9 | Gestures: tắt một cử chỉ | Dòng mờ đi (icon, tên, mô tả) | [ ] |
| MT-V10 | Mở picker | Sheet trượt lên giữa top bar và bottom bar; kéo xuống không đóng; nút ✕ và Back đóng; bấm tab ở bottom bar khi sheet mở vẫn chuyển tab | [ ] |
| MT-V11 | Picker tab System | Không có ô tìm kiếm; 2 card System / Media bo góc, dòng ngăn bằng đường kẻ, có radio | [ ] |
| MT-V12 | Picker tab Shortcuts | Danh sách app đều thu gọn; chạm app → shortcut hiện ngay bên dưới (icon mũi tên xuống-rồi-sang-phải); tìm kiếm → app khớp tự mở rộng | [ ] |
| MT-V13 | Tab Notch: nút − / + của Double tap | Đổi đúng 10 ms; kéo slider nhảy theo nấc 50, không có chấm vạch; thang 100–1000 | [ ] |
| MT-V14 | Tab Notch: Width / Padding | − / + đổi 1 dp; padding 1–10; highlight vẽ notch đậm + viền nét đứt phần padding, cập nhật ngay | [ ] |
| MT-V15 | Bottom bar | 4 tab Home, Gestures, Notch (icon notch), Settings; top bar không có nút; chuyển tab có hiệu ứng trượt theo hướng | [ ] |

## 11. v1.5 — activities, chặn chạm bằng Trợ năng, xoay ngang

| ID | Bước | Kỳ vọng | Kết quả |
|---|---|---|---|
| MT-A1 | Bật Trợ năng, gán Swipe down → Thông báo, vuốt xuống tại notch | Dòng Swipe down bấm được; vuốt xuống tại notch **không** kéo thanh thông báo của hệ thống mà chạy hành động đúng 1 lần; vuốt xuống ngoài notch vẫn kéo thanh thông báo | [ ] |
| MT-A2 | Tắt Trợ năng | Swipe down / Swipe down & hold mờ lại, nhãn "Requires Accessibility"; vuốt xuống ở notch kéo thanh thông báo như cũ | [ ] |
| MT-A3 | Tắt switch Thông báo ở Home (13+, Shizuku sẵn sàng) | Hộp thoại xác nhận; đồng ý → Totch đóng rồi tự mở lại sau ~1 s, quyền Thông báo "Not granted", cử chỉ vẫn chạy | [ ] |
| MT-A4 | Picker tab Activities | App thu gọn; mở rộng → activity (nhãn + tên lớp); chọn một activity → cử chỉ mở đúng activity (cả khi chỉ có Shizuku, và khi có Trợ năng) | [ ] |
| MT-A5 | Settings | System đầu tiên (Haptic feedback, Gestures in landscape, Start on boot), rồi Appearance, About; mọi dòng có icon, có đường kẻ giữa các dòng; tab Notch không còn Feedback | [ ] |
| MT-A6 | Bật Gestures in landscape, xoay ngang (cả 2 chiều), chạm/vuốt tại notch | Status "Gestures are on"; chạm notch chạy hành động; "vuốt xuống" = vuốt ra xa notch. Tắt tùy chọn → "paused in landscape" | [ ] |
| MT-A7 | Settings (cả en và vi) | System: Start on boot → Haptic feedback → Gestures in landscape; "Amoled black" có mô tả 1 dòng; mọi mô tả trong About 1 dòng, cuối dòng link là › | [ ] |
| MT-A8 | Mở picker | Sheet chạm sát mép dưới top bar; hàng chip cuộn ngang có icon; mở picker của cử chỉ gán activity → chip Activities được chọn và nằm trong tầm nhìn | [ ] |

## Lịch sử kết quả

### 1.0.0 (10000)
| Hạng mục | KQ | Ghi chú |
|---|---|---|
| Accessibility mode: 6 cử chỉ | [v] | |
| Shizuku mode: 6 cử chỉ; tắt Shizuku giữa chừng | [v] | |
| Xoay ngang/dọc | [v] | |
| Khởi động lại máy | [ ] | chưa test |
| Đèn pin | [v] | |
| Media Spotify/YouTube Music/VLC | [x] | Không điều khiển được ở Accessibility mode → sửa ở 1.0.1 (actions spec §5), kiểm lại bằng MT-M3…M7 |
| Mở app khi đang ở app khác | [ ] | chưa test |
| Dark mode, tiếng Việt | [ ] | chưa test |
