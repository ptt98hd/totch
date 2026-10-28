# Spec: Nhận diện cử chỉ (`GestureRecognizer`)

Yêu cầu: FR-G1…FR-G7, FR-S3 · Module: `:core` · Package: `io.github.ow3n.totch.core.gesture`

## 1. Đầu vào / đầu ra
- Đầu vào: chuỗi `TouchEvent` (tọa độ pixel màn hình, `timeMs` đơn điệu tăng) và lời gọi `onTick(nowMs)`.
- Đầu ra: `List<Gesture>` (thường rỗng hoặc 1 phần tử; trường hợp đặc biệt có 2 — xem GR-14).
- `region()` trả `null` → mọi chạm bị bỏ qua.

## 2. Máy trạng thái

| Trạng thái | Ý nghĩa |
|---|---|
| `Idle` | Không có gì |
| `Ignoring` | Nét chạm hiện tại bị bỏ qua (bắt đầu ngoài vùng / multi-touch / đã hủy) đến khi UP/CANCEL |
| `Pressed(down, isSecond)` | Ngón đang chạm, chưa vượt `touchSlop` |
| `Moved(down)` | Đã vượt `touchSlop` — ứng viên vuốt |
| `LongPressed` | Đã phát LONG_PRESS, chờ nhả |
| `WaitingSecondTap(upTime)` | Đã có 1 TAP, chờ lần chạm thứ hai |

**Quy tắc thời gian:** trước khi xử lý mỗi `TouchEvent` tại thời điểm `t`, recognizer xử lý các
timeout đã tới hạn `≤ t` (như thể `onTick(t)` được gọi trước). Do đó kết quả không phụ thuộc
việc `onTick` có được gọi đúng lúc hay không.

Hệ quả: timeout tới hạn khi `t ≥ deadline`, nên DOWN thứ hai đúng tại `up + doubleTapMs` đã là
cử chỉ mới (cửa sổ double tap thực tế là `< doubleTapMs`, xem GR-29).

Khoảng cách dùng Euclid: `hypot(x - downX, y - downY)`; "vượt `touchSlop`" nghĩa là `> touchSlopPx`.

**Phân loại vuốt** (tại UP, dùng `dx = x − downX`, `dy = y − downY`): nếu `|dy| > |dx|` → `SWIPE_DOWN`
khi `dy ≥ swipeMinDistancePx`, ngược lại không phát (vuốt lên không hỗ trợ); nếu `|dx| ≥ |dy|` →
`SWIPE_LEFT`/`SWIPE_RIGHT` theo dấu của `dx` khi `|dx| ≥ swipeMinDistancePx`.

**Hủy nét chạm** (CANCEL, multi-touch, DOWN thiếu UP): nét hiện tại không phát gì, nhưng nếu đó là
lần chạm thứ hai thì TAP đầu tiên (đã hoàn tất) vẫn được phát (GR-27).

## 3. Acceptance criteria

Giả định mặc định trong test: region = `Rect(400, 0, 680, 100)`, `doubleTapMs=300`,
`longPressMs=500`, `touchSlopPx=24`, `swipeMinDistancePx=60`, `doubleTapEnabled=true`.

| ID | Given | When | Then |
|---|---|---|---|
| GR-1 | Idle | DOWN(540,50,t0) , UP(540,50,t0+100), tick(t0+100+300) | phát `[TAP]` tại tick; trước tick, không phát gì |
| GR-2 | `doubleTapEnabled=false` | DOWN, UP sau 100 ms | phát `[TAP]` ngay tại UP (FR-G5) |
| GR-3 | Idle | DOWN t0, UP t0+80, DOWN t0+200, UP t0+260 | phát `[DOUBLE_TAP]` tại UP thứ hai; không phát TAP nào |
| GR-4 | Idle | DOWN t0, UP t0+80, DOWN t0+80+301 … | TAP được phát (do timeout) trước khi nhận DOWN thứ hai; lần chạm 2 là cử chỉ mới |
| GR-5 | Idle | DOWN t0, tick(t0+499) | không phát gì |
| GR-6 | Idle | DOWN t0, tick(t0+500) | phát `[LONG_PRESS]`; UP sau đó không phát gì |
| GR-7 | Idle | DOWN t0, UP t0+600 (không có tick nào ở giữa) | phát `[LONG_PRESS]` tại UP (quy tắc thời gian), không phát TAP |
| GR-8 | Idle | DOWN(540,20), MOVE(545,50), UP(545,90) | `dy=70 ≥ 60` và `dy > |dx|` → `[SWIPE_DOWN]` |
| GR-9 | Idle | DOWN(540,50), UP(470,55) qua MOVE | `[SWIPE_LEFT]` |
| GR-10 | Idle | DOWN(540,50), UP(610,45) qua MOVE | `[SWIPE_RIGHT]` |
| GR-11 | Idle | DOWN(540,50), MOVE(570,50), UP(570,50) | vượt slop nhưng `30 < 60` → không phát gì |
| GR-12 | Idle | DOWN(540,90), MOVE/UP tại (540,10) | vuốt lên không được hỗ trợ → không phát gì |
| GR-13 | Idle | DOWN(540,50), MOVE vượt slop, giữ tới t0+800, UP | không phát LONG_PRESS (di chuyển hủy long press); kết quả theo hướng vuốt |
| GR-14 | WaitingSecondTap | DOWN lần 2, giữ ≥ 500 ms | phát `[TAP, LONG_PRESS]` tại thời điểm đạt ngưỡng |
| GR-15 | WaitingSecondTap | DOWN lần 2, MOVE vượt slop rồi UP với `dy ≥ 60` | phát `[TAP]` khi vượt slop, rồi `[SWIPE_DOWN]` tại UP |
| GR-16 | Idle | DOWN(100,50) (ngoài vùng), MOVE vào vùng, UP | không phát gì (FR-G6) |
| GR-17 | WaitingSecondTap | DOWN ngoài vùng | phát `[TAP]` ngay; nét chạm mới bị bỏ qua |
| GR-18 | Pressed | MOVE với `pointerCount=2` | hủy, không phát gì đến hết nét (FR-G7) |
| GR-19 | Pressed | CANCEL | về Idle, không phát gì |
| GR-20 | Vuốt kết thúc ngoài vùng | DOWN trong vùng (540,50), UP (540,300) | `[SWIPE_DOWN]` — chỉ điểm bắt đầu phải trong vùng |
| GR-21 | `region()` trả `null` | bất kỳ | không phát gì |
| GR-22 | Bất kỳ | `nextDeadlineMs()` | Pressed → `down+longPressMs`; WaitingSecondTap → `up+doubleTapMs`; còn lại `null` |
| GR-23 | Đang chạy | đổi `config.longPressMs=800` rồi DOWN, tick(t0+600) | không phát gì; tick(t0+800) → LONG_PRESS (FR-S3) |
| GR-24 | Bất kỳ | `reset()` | về Idle, xóa mọi TAP đang chờ, không phát gì |
| GR-25 | Idle | MOVE/UP không có DOWN trước | bỏ qua |
| GR-26 | Pressed | DOWN mới (thiếu UP — dữ liệu hỏng) | coi như nét cũ bị hủy, bắt đầu nét mới |
| GR-27 | WaitingSecondTap → DOWN lần 2 | CANCEL, hoặc MOVE với `pointerCount=2` | phát `[TAP]` (của lần chạm 1) ngay lúc hủy; phần còn lại của nét bị bỏ qua |
| GR-28 | Idle | DOWN(540,20), UP(540,90) không có MOVE ở giữa | vị trí UP được dùng để xét slop/vuốt → `[SWIPE_DOWN]` |
| GR-29 | Idle | DOWN t0, UP t0+80, DOWN t0+380 (đúng `up+doubleTapMs`) | phát `[TAP]` tại DOWN thứ hai; lần chạm 2 là cử chỉ mới |
