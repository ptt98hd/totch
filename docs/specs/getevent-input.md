# Spec: Đọc input thô qua `getevent` (Shizuku mode)

Yêu cầu: FR-D1, FR-D2 · Module: `:core` · Package: `io.github.ow3n.totch.core.input`

## 1. Tìm thiết bị cảm ứng — `GeteventDeviceParser`

Đầu vào: output của `getevent -pl`. Ví dụ:
```
add device 1: /dev/input/event5
  name:     "gpio-keys"
  events:
    KEY (0001): KEY_VOLUMEDOWN        KEY_VOLUMEUP
  input props:
    <none>
add device 2: /dev/input/event3
  name:     "fts_ts"
  events:
    KEY (0001): KEY_WAKEUP            BTN_TOUCH
    ABS (0003): ABS_MT_SLOT           : value 0, min 0, max 9, fuzz 0, flat 0, resolution 0
                ABS_MT_TOUCH_MAJOR    : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
                ABS_MT_POSITION_X     : value 0, min 0, max 1079, fuzz 0, flat 0, resolution 0
                ABS_MT_POSITION_Y     : value 0, min 0, max 2339, fuzz 0, flat 0, resolution 0
                ABS_MT_TRACKING_ID    : value 0, min 0, max 65535, fuzz 0, flat 0, resolution 0
  input props:
    INPUT_PROP_DIRECT
```

`InputDeviceInfo(path, name, x: AbsRange, y: AbsRange, direct: Boolean)` — chỉ thiết bị có **cả**
`ABS_MT_POSITION_X` và `ABS_MT_POSITION_Y` mới được trả về.

| ID | Tiêu chí |
|---|---|
| GD-1 | Ví dụ trên → đúng 1 thiết bị: `/dev/input/event3`, name `fts_ts`, x 0..1079, y 0..2339, direct=true |
| GD-2 | Thiết bị chỉ có ABS_X/ABS_Y (không MT) → bị loại |
| GD-3 | `pickTouchscreen` ưu tiên thiết bị `direct=true`; nếu không có, lấy thiết bị đầu tiên; danh sách rỗng → `null` |
| GD-4 | Chuỗi rỗng / rác → danh sách rỗng, không ném exception |
| GD-5 | Tên có ký tự đặc biệt/khoảng trắng trong dấu nháy được giữ nguyên (bỏ dấu nháy) |

## 2. Parse luồng sự kiện — `GeteventParser`

Đầu vào: từng dòng của `getevent -lt <path>`. Hai dạng phải hỗ trợ:
```
[   12345.678901] EV_ABS       ABS_MT_POSITION_X    000001c2
[   12345.678901] /dev/input/event3: EV_ABS       ABS_MT_POSITION_X    000001c2
```
- Timestamp `[ sec.usec ]` → `timeMs = sec*1000 + usec/1000`.
- Giá trị ABS là hex 8 ký tự; `ffffffff` của `ABS_MT_TRACKING_ID` nghĩa là nhấc ngón (−1).
- Giao thức MT type B: `ABS_MT_SLOT` chọn slot hiện tại (mặc định slot 0 nếu thiết bị không gửi SLOT).
- Mỗi slot lưu `trackingId`, `x`, `y`. Sự kiện chỉ phát khi gặp `EV_SYN SYN_REPORT`.
- **Primary** = slot đang hoạt động có chỉ số nhỏ nhất. `pointerCount` = số slot đang hoạt động.

| ID | Tiêu chí |
|---|---|
| GP-1 | Khung: TRACKING_ID 0x45, X 0x1c2, Y 0x20, SYN → `DOWN(map(450,32), t)` pointerCount=1 |
| GP-2 | Khung tiếp theo chỉ đổi X → `MOVE` với X mới, Y giữ nguyên |
| GP-3 | Khung TRACKING_ID ffffffff, SYN → `UP` tại vị trí cuối cùng |
| GP-4 | Khung không thay đổi vị trí và số ngón → không phát gì |
| GP-5 | Slot 1 bắt đầu khi slot 0 đang chạm → `MOVE` với `pointerCount=2` |
| GP-6 | Tất cả slot nhấc → đúng một `UP`, `pointerCount` = 1 |
| GP-7 | Dòng `EV_KEY BTN_TOUCH DOWN/UP`, `ABS_MT_PRESSURE`, `ABS_MT_TOUCH_MAJOR`, dòng rác, dòng rỗng → bị bỏ qua, không exception |
| GP-8 | Hỗ trợ cả hai định dạng có/không có tiền tố đường dẫn thiết bị |
| GP-9 | Timestamp `[   12345.678901]` → `timeMs = 12345678` |

## 3. Quy đổi tọa độ — `CoordinateMapper`

`px = (raw − min) / (max − min) × screenSize`, sau đó kẹp vào `[0, screenSize − 1]`.
`screenWidthPx/HeightPx` là kích thước **tự nhiên** (hướng ROTATION_0).

| ID | Tiêu chí |
|---|---|
| CM-1 | Range 0..1079, màn hình 1080 → raw 0 → 0, raw 1079 → 1079 |
| CM-2 | Range 0..4095 (độ phân giải cảm ứng cao hơn), màn hình 1080 → raw 2048 ≈ 540 (±1) |
| CM-3 | Raw ngoài range → được kẹp |
| CM-4 | `max == min` → trả 0, không chia cho 0 |
