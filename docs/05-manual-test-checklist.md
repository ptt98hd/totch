# 05 — Checklist kiểm thử thủ công

> Dùng cho mỗi bản release. Ký hiệu cột **KQ**: `[v]` đạt · `[x]` lỗi (ghi chú lại hiện tượng) · `[-]` không áp dụng / bỏ qua.
> Khi báo lỗi, ghi ID (vd. `MT-M3`) kèm máy, phiên bản Android, ROM và (nếu được) log:
> `adb logcat | grep -E 'Totch|AndroidRuntime'`

## 0. Thông tin lần test

| Mục | Giá trị |
|---|---|
| Phiên bản app | 1.0.1 (10001) |
| Máy / ROM / Android | |
| Kiểu notch (giọt nước / đục lỗ giữa / đục lỗ góc / không có) | |
| Phiên bản Shizuku | |
| Người test / ngày | |

## 1. Cài đặt & lần mở đầu

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-I1 | Cài mới, mở app | Home hiện thẻ giới thiệu 2 chế độ; công tắc chính tắt; không crash | [ ] |
| MT-I2 | Bật công tắc khi chưa cấp quyền nào | Công tắc vẫn bật; thẻ yêu cầu quyền hiện ✗ kèm nút hành động; status báo thiếu quyền | [ ] |
| MT-I3 | Chuyển qua lại 2 chế độ bằng segmented button | Danh sách thẻ yêu cầu đổi theo chế độ; lựa chọn được lưu sau khi đóng/mở app | [ ] |
| MT-I4 | Cập nhật đè từ 1.0.0 lên 1.0.1 | Cài đặt và binding cũ được giữ nguyên | [ ] |

## 2. Chế độ Trợ năng (Accessibility)

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
| MT-S7 | Vuốt xuống ở notch | Totch chạy hành động **và** thanh thông báo hệ thống cũng kéo xuống (hạn chế đã biết R2) | [ ] |
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
| MT-R1 | Settings → "Preview touch area" (chế độ Trợ năng) | Vùng tô màu xuất hiện đúng trên notch ~3 s | [ ] |
| MT-R2 | Preview ở chế độ Shizuku | Hiện hình minh họa vùng chạm trong app | [ ] |
| MT-R3 | Tăng padding lên 48 dp rồi preview | Vùng to ra 2 bên và phía dưới; cạnh trên vẫn sát mép | [ ] |
| MT-R4 | Bật "Custom touch area", kéo vị trí ngang về 10% | Vùng chạm dời sang trái; cử chỉ chỉ nhận ở vị trí mới | [ ] |
| MT-R5 | Máy không có notch (hoặc giả lập) | Vùng mặc định ở giữa cạnh trên, rộng ~20% | [ ] |
| MT-R6 | Xoay ngang (app hỗ trợ xoay, vd. YouTube) | Status "Paused"; chạm vùng cũ không kích hoạt gì | [ ] |
| MT-R7 | Xoay dọc lại | Hoạt động lại không cần mở app | [ ] |
| MT-R8 | Double-tap window = 600 ms rồi chạm đúp chậm | Vẫn nhận double tap; với 150 ms thì chạm đúp chậm thành 2 TAP | [ ] |
| MT-R9 | Long-press = 1500 ms | Phải giữ đủ 1,5 s mới kích hoạt | [ ] |
| MT-R10 | Swipe distance = 200 dp, vuốt ngắn | Vuốt ngắn không kích hoạt; vuốt dài thì có | [ ] |
| MT-R11 | Tắt haptics | Không rung khi kích hoạt | [ ] |
| MT-R12 | Đổi cài đặt khi đang chạy | Có hiệu lực ngay, không cần tắt/bật | [ ] |
| MT-R13 | "Reset to defaults" → Hủy, rồi → Đồng ý | Hủy: không đổi gì; Đồng ý: về mặc định nhưng giữ công tắc và chế độ | [ ] |
| MT-R14 | Force stop app trong Cài đặt hệ thống rồi mở lại | Cài đặt còn nguyên | [ ] |

## 7. Khởi động lại máy

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-B1 | Chế độ Trợ năng, đang bật → reboot | Sau khi mở khóa, cử chỉ hoạt động mà không mở app | [ ] |
| MT-B2 | Chế độ Shizuku, Shizuku khởi động bằng root/ADB không dây sau boot | Thông báo "Waiting for Shizuku" → tự chuyển "Running" khi Shizuku sẵn sàng | [ ] |
| MT-B3 | Chế độ Shizuku, Shizuku **không** tự khởi động | Thông báo "Waiting for Shizuku"; khởi động Shizuku thủ công → tự chạy | [ ] |
| MT-B4 | Công tắc chính tắt → reboot | Không có service/thông báo nào chạy | [ ] |

## 8. Giao diện

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-U1 | Bật Dark mode hệ thống | Mọi màn hình (Home, Gestures, App picker, Settings, About, bottom sheet, dialog) đổi tối; chữ đọc rõ | [ ] |
| MT-U2 | Đổi ngôn ngữ hệ thống sang Tiếng Việt | Toàn bộ chữ là tiếng Việt, không bị cắt chữ; toast lỗi cũng tiếng Việt | [ ] |
| MT-U3 | Đổi hình nền (màu khác hẳn) | Màu app đổi theo (Material You) | [ ] |
| MT-U4 | Sau MT-U3, xem icon ở launcher | Icon đổi màu theo hình nền mới (có thể cần vài giây/khởi động lại launcher) | [ ] |
| MT-U5 | Bật "Themed icons" (Android 13+) | Icon hiển thị dạng đơn sắc theo theme | [ ] |
| MT-U6 | Cỡ chữ hệ thống lớn nhất | Không vỡ layout; slider và nút dùng được | [ ] |
| MT-U7 | App picker: tìm "spo" / "SPO" | Kết quả giống nhau (không phân biệt hoa thường) | [ ] |
| MT-U8 | Icon thông báo thường trực (Shizuku) | Icon đơn sắc rõ ràng, không phải khối trắng | [ ] |
| MT-U9 | TalkBack: duyệt Home và Gestures | Các nút/thẻ đọc được nhãn có nghĩa | [ ] |

## 9. Ổn định & pin

| ID | Kịch bản | Kỳ vọng | KQ |
|---|---|---|---|
| MT-P1 | Bật Battery saver | Cử chỉ vẫn hoạt động | [ ] |
| MT-P2 | Để máy qua đêm với app bật (mỗi chế độ 1 đêm) | Totch không nằm trong top tiêu thụ pin; sáng ra vẫn hoạt động | [ ] |
| MT-P3 | Dùng bình thường 1 ngày | Không có crash, không có toast lỗi lạ | [ ] |
| MT-P4 | Gỡ cài đặt Shizuku khi đang ở chế độ Shizuku | Không crash; status "Waiting for Shizuku" | [ ] |

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
