# Spec: Vùng notch (`NotchRegionResolver`)

Yêu cầu: FR-R1…FR-R4 · Module: `:core` · Package: `io.github.ow3n.totch.core.region`

`resolve(screenWidthPx, statusBarHeightPx, cutout, manual, paddingPx, rotation): Rect?`

`rotation` theo hằng số `Surface.ROTATION_*` (0, 1, 2, 3).

Thứ tự ưu tiên: **manual > cutout > mặc định**. Sau khi có rect gốc, mở rộng `paddingPx`
sang trái, phải và dưới (cạnh trên luôn = 0), rồi kẹp trái/phải vào `[0, screenWidthPx]`.

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

Ghi chú: manual → `left = round(fraction × screenWidthPx − widthPx/2)`, `right = left + widthPx`;
mặc định → `width = round(screenWidthPx × 0.2)`, `left = (screenWidthPx − width) / 2`. Cạnh trên của rect cutout luôn được đặt về 0.
