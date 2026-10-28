# Spec: Vùng notch (`NotchRegionResolver`)

Yêu cầu: FR-R1…FR-R6 · Module: `:core` · Package: `io.github.ptt98hd.totch.core.region`

`resolve(screenWidthPx, statusBarHeightPx, cutout, manual, paddingPx, rotation): Rect?`

`rotation` theo hằng số `Surface.ROTATION_*` (0, 1, 2, 3).

`ManualRegion(centerXFraction, widthPx, heightPx, topPx = 0)` — `topPx` là vị trí dọc (khoảng cách từ cạnh trên, FR-R6).

Thứ tự ưu tiên: **manual > cutout > mặc định**. Sau khi có rect gốc, mở rộng `paddingPx`
sang trái, phải, dưới và trên, rồi kẹp trái/phải vào `[0, screenWidthPx]`, cạnh trên vào `≥ 0`
(rect gốc cutout/mặc định có top = 0 nên cạnh trên của chúng luôn = 0).

| ID | Given | Then |
|---|---|---|
| NR-1 | rotation ≠ 0 | `null` (FR-R4) |
| NR-2 | cutout có 1 rect `(500,0,580,80)`, padding 0 | `(500,0,580,80)` |
| NR-3 | như NR-2, padding 10 | `(490,0,590,90)` |
| NR-4 | cutout có 2 rect `(100,0,150,60)` và `(900,0,960,70)` | hợp của chúng `(100,0,960,70)` |
| NR-5 | cutout `null` hoặc danh sách rỗng, màn 1080, status bar 90 | mặc định rộng 20% ở giữa: `(432,0,648,90)` |
| NR-6 | manual `(centerXFraction=0.5, width=200, height=60)`, màn 1000 | `(400,0,600,60)` — bỏ qua cutout |
| NR-7 | padding đẩy rect ra ngoài màn (vd. cutout `(0,0,50,50)`, padding 20) | trái kẹp về 0: `(0,0,70,70)` |
| NR-8 | rect kết quả có width ≤ 0 hoặc height ≤ 0 | `null` |
| NR-9 | manual centerXFraction ngoài [0,1] | được kẹp vào [0,1] trước khi tính |
| NR-10 | manual `(0.5, width=200, height=60, topPx=40)`, màn 1000, padding 0 | `(400,40,600,100)` |
| NR-11 | như NR-10, padding 10 | `(390,30,610,110)`; padding 50 → cạnh trên kẹp về 0: `(350,0,650,150)` |

Ghi chú: manual → `left = round(fraction × screenWidthPx − widthPx/2)`, `right = left + widthPx`, `top = topPx`, `bottom = topPx + heightPx`;
mặc định → `width = round(screenWidthPx × 0.2)`, `left = (screenWidthPx − width) / 2`. Cạnh trên của rect cutout luôn được đặt về 0.

## Hiển thị vùng chạm trong app (v1.1 — FR-U8)
`TouchArea.regionFor(settings)` = `notchRegion(settings, display hiện tại)` (cùng hàm engine dùng), để UI
vẽ vùng của **bản nháp** settings khi người dùng đang kéo slider. Highlight vẽ trong cửa sổ của
`MainActivity` (edge-to-edge, `layoutInDisplayCutoutMode=shortEdges`), quy đổi tọa độ màn hình → cửa sổ
bằng vị trí cửa sổ trên màn hình. Không cần overlay hay quyền mới; ở landscape (`null`) không vẽ.

## Vùng tự phát hiện dạng dp (v1.2 — FR-R5)
`detectedRegionDp(display): ManualRegionDp?` (`:app`, `engine/EngineState.kt`) = vùng `resolve(manual = null, padding = 0)`
quy về dp: `centerXFraction = (left + right) / 2 / widthPx`, `widthDp = round(width / density)`,
`heightDp = round(height / density)`, `topDp = 0`. `null` khi xoay ngang. `TouchArea.detectedRegion` expose giá trị này
(StateFlow theo display) để màn Notch hiển thị slider khi chưa có vùng thủ công.

| ID | Given | Then |
|---|---|---|
| NR-12 | cutout `(500,0,580,80)`, màn 1080, density 2 | `ManualRegionDp(centerXFraction = 540/1080 = 0.5, widthDp = 40, heightDp = 40, topDp = 0)` |
| NR-13 | không cutout, màn 1080, status bar 90, density 2 | `ManualRegionDp(0.5, widthDp = 108, heightDp = 45, topDp = 0)` |
| NR-14 | rotation ≠ 0 | `null` |

## Padding & highlight (v1.4 — FR-R7)
`paddingDp` 1–10: phần dư ra quanh vùng notch (rộng × cao) để dễ chạm trúng. Về lý thuyết notch là vùng rộng × cao; thực tế
engine nhận chạm trong toàn bộ vùng có padding (`notchRegion`). Highlight dùng `highlightArea(settings, display)` =
`HighlightArea(touch = notchRegion(settings), notch = notchRegion(settings.copy(paddingDp = 0)))` để vẽ riêng phần padding
(viền nét đứt) và phần notch (nền đậm, viền liền). `TouchArea.area` / `areaFor(draft)` thay cho `region` / `regionFor`.
