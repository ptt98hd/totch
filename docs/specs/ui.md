# Spec: Giao diện (Material 3)

Yêu cầu: FR-U1…FR-U42, FR-A20, FR-A15, FR-G12…FR-G14, FR-A14, FR-D10, FR-R8, FR-S6…FR-S9, FR-R5…FR-R7, FR-D9 · Module: `:app` · Package: `io.github.ptt98hd.totch.ui`

## 1. Nguyên tắc
- Jetpack Compose + Material 3, `dynamicLightColorScheme/dynamicDarkColorScheme`, edge-to-edge.
- Một Activity (`MainActivity`), Navigation Compose.
- **Top bar tĩnh** (`TopAppBar` pinned, không co giãn khi cuộn) trên mọi màn hình: tiêu đề = tên màn hình ở bên trái
  (v1.4: bỏ icon Settings và nút Back). v1.7 (FR-U40, FR-U41): bên phải có 2 nút icon, theo thứ tự **Check for updates**
  (icon `SystemUpdate`, chấm báo khi có bản mới — update §8) rồi **Buy me a coffee** (icon ly cà phê, ngoài cùng bên
  phải). Hai nút lấy hành động từ `LocalAppActions` (do `MainActivity` cung cấp) nên màn hình không phải truyền tay.
- `NavigationBar` dưới cùng có 4 mục (v1.4, FR-U19): **Home**, **Gestures**, **Notch** (icon điện thoại có notch,
  vector riêng `NotchIcons`), **Settings** (cài đặt ứng dụng). Luôn hiển thị. v1.8.1 (FR-U49): nhãn dưới icon hiện khi
  `Settings.navLabels` (mặc định bật); tắt → chỉ icon (`label = null`).
- **Switch** (v1.8.1, FR-U54): mọi switch dùng `TotchSwitch` — `Switch` của Material 3 với `thumbContent` là icon
  `Check` (bật) / `Close` (tắt), cỡ `SwitchDefaults.IconSize`.
- **Liên kết** (v1.8.1, FR-U52): `MainActivity` cung cấp `LocalUriHandler` mở `http(s)` bằng Custom Tabs
  (`CustomTabsIntent`, màu thanh theo theme); lỗi (không có trình duyệt hỗ trợ) → `ACTION_VIEW` như cũ. Mọi chỗ mở link
  (About, Buy me a coffee) đi qua handler này.
- Chuyển tab có animation (FR-U22): trang mới trượt vào 1/5 chiều rộng + fade theo hướng tab (sang phải nếu tab
  đích nằm bên phải), trang cũ trượt ngược lại; 300 ms.
- Action picker là **sheet** trên màn Gestures (FR-U20), không phải destination riêng.
- Mỗi màn hình có `ViewModel` riêng, expose `StateFlow<UiState>`; Composable stateless nhận `state` + lambda → test được.
- Giao diện gọn: không có đoạn mô tả giải thích dài.
- **Dòng danh sách đồng nhất** (v1.7, FR-U39): mọi dòng (`ListItem`) trong app — Home, Gestures, picker, Settings —
  dùng chung `TotchListItem`: cao tối thiểu `RowHeight` = 72 dp (bằng nhau dù có hay không có mô tả), icon trái 40 dp,
  tiêu đề và mô tả `maxLines = 1` + `TextOverflow.Ellipsis`. Thanh trượt (màn Notch) và dòng trong hộp thoại chọn không
  phải dòng danh sách.
- Chuỗi trong `strings.xml` (en) và `values-vi/strings.xml`.

| ID | Tiêu chí |
|---|---|
| UI-40 | Top bar pinned, chỉ có tiêu đề = tên màn hình bên trái |
| UI-41 | Bottom bar có Home, Gestures, Notch, Settings theo thứ tự; luôn hiển thị |
| UI-43 | Chuyển tab: hướng trượt = dấu của (chỉ số tab đích − chỉ số tab nguồn) |
| UI-44 | (v1.7) Top bar: bên phải đúng 2 nút — Check for updates (mở hộp thoại cập nhật, update §8) rồi Buy me a coffee (mở hộp thoại ủng hộ) |
| UI-45 | (v1.7) Mọi dòng danh sách cao ≥ 72 dp, tiêu đề và mô tả một dòng (kiểm thủ công MT-U40) |
| UI-70 | (v1.8.1, FR-U49) `navLabels = false` → bottom bar không có nhãn; `true` → có nhãn (kiểm thủ công MT-U50) |

### Buy me a coffee (v1.7 — FR-U40, prototype)
v1.8.1 (FR-U48): hộp thoại vẽ bằng `TotchDialog` (bên dưới); nút chính **Buy me a coffee** đầy đủ chiều rộng có icon,
**Maybe later** là `TextButton` bên dưới; link mở trong app (FR-U52).

Hộp thoại ủng hộ dùng chung cho nút top bar, dòng ở Home và Settings: icon ly cà phê, tiêu đề "Buy me a coffee", một
đoạn ngắn "Totch is free and open source, with no ads. If it helps you, a coffee keeps it going." / "Totch miễn phí, mã
nguồn mở, không quảng cáo. Nếu thấy hữu ích, một ly cà phê giúp Totch tiếp tục phát triển.", nút **Buy me a coffee**
(mở `donate_url` trong trình duyệt) và **Maybe later**. Đây là prototype: URL là chuỗi `donate_url` (không dịch) trong
`strings.xml`, đổi được mà không sửa code; chưa có cổng thanh toán trong app (F-Droid: không thư viện độc quyền).

**`TotchDialog`** (v1.8.1, FR-U48 — khung chung của hộp thoại ủng hộ và cập nhật): `BasicAlertDialog` + `Surface` bo 28 dp,
nền `surfaceContainerHigh`; trên cùng là icon 32 dp trong vòng tròn 72 dp nền `primaryContainer` (tùy chọn có
`CircularProgressIndicator` vòng quanh khi đang bận), tiêu đề `headlineSmall` căn giữa, nội dung, rồi các nút xếp dọc
đầy đủ chiều rộng (nút chính `Button`, nút phụ `TextButton`).

Dòng *Buy me a coffee* (Home, Settings): icon ly cà phê (box tonal), tiêu đề "Buy me a coffee", mô tả "Support Totch's
development" / "Ủng hộ phát triển Totch", chevron (›).

## 2. Highlight vùng notch trực tiếp (FR-U8)
- Khi app đang mở (mọi màn hình, hướng dọc), vùng chạm hiện tại được tô trực tiếp trên màn hình tại đúng vị
  trí notch: nền `primary` alpha 35% + viền `primary`, bo tròn.
- Màn Notch: khi kéo slider vùng chạm (vị trí ngang/dọc, rộng, cao, padding) hoặc bấm − / +, highlight
  cập nhật **ngay** theo giá trị nháp (`TouchArea.areaFor(draft)`).
- v1.4 (FR-R7): `HighlightArea(touch, notch)` — `notch` = vùng rộng × cao (không padding) tô nền `primary` 35% + viền
  liền; `touch` = vùng có padding, vẽ nền `primary` 12% + **viền nét đứt** quanh notch. Engine vẫn nhận chạm trong cả
  `touch`.
- Bỏ nút / dialog "Preview touch area" và overlay preview qua accessibility.

## 3. Màn hình

### Home (FR-U1, FR-D4, FR-D9, FR-U21, FR-U42) — v1.4, v1.7
```
┌──────────────────────────────────┐
│ Home                             │  ← top bar tĩnh, chỉ tiêu đề
│ ┌──────────────────────────────┐ │
│ │ ┌──┐ Totch            [ ON ] │ │
│ │ │✓ │ Gestures are on         │ │
│ │ └──┘                         │ │
│ └──────────────────────────────┘ │
│ Permissions                      │
│ [♿] Accessibility          [on]  │  Granted / Not granted
│ [🔔] Notifications         [off] │  (Android 13+)
│ [♪] Media control          [off] │
│ [⊖] Do Not Disturb         [off] │  v1.6.3; v1.8.2: bỏ "access"
│ [🔋] Unrestricted battery  [on]  │
│ [⏸] Keep when unused       [on]  │  v1.8.2
│ [⤓] Install updates        [off] │  v1.6.1
│ Advanced                         │  v1.7: đổi tên từ Requirement, chuyển xuống dưới
│ [▣] Shizuku                [on]  │  Running, permission granted / Not running · optional
│ [⟳] Auto-start                ›  │  v1.8.2, chỉ khi máy có trang của hãng
│ Support                          │  v1.7
│ [☕] Buy me a coffee          ›  │
├──────────────────────────────────┤
│ Home | Gestures | Notch | Settings│
└──────────────────────────────────┘
```
- Thẻ Totch: tiêu đề = tên app; bên trái là **box** bo góc (48 dp) chứa 1 trong 3 icon (UI-42): `Check` (bật,
  box `primary`), `Close` (tắt; box `error` khi đã bật mà có lỗi / thiếu Shizuku, ngược lại `surfaceVariant`),
  `Sync` xoay liên tục (đang bật / đang tắt, box `tertiary`). Mô tả ngắn "Gestures are …" theo `EngineStatus`.
- **Advanced** / **Nâng cao** (v1.7, FR-U42 — trước đây là **Requirement**, nằm trên Permissions): 1 dòng Shizuku (icon,
  tên, trạng thái, switch = sẵn sàng). Shizuku là tùy chọn (FR-D13) nên dòng không còn tô màu lỗi khi chưa sẵn sàng; mô
  tả khi chưa chạy / chưa cài: "Not running · optional" / "Chưa chạy · tùy chọn", "Not installed · optional" / "Chưa
  cài đặt · tùy chọn". Chạm: chưa cấp quyền → hộp thoại
  quyền Shizuku; còn lại → mở app Shizuku (hoặc trang web khi chưa cài).
  v1.8.2 (FR-U59): dưới Shizuku là dòng **Auto-start** / **Tự khởi chạy** (icon `RestartAlt`, mô tả "Let Totch run in
  the background" / "Cho Totch chạy nền", chevron) — chỉ hiện khi `OemSettings` tìm được trang của hãng
  ([detection-modes §9](detection-modes.md)); bấm → mở trang đó.
- **Permissions**: mỗi dòng như thẻ Totch nhưng icon là **biểu tượng quyền** (box tonal), tên quyền, mô tả
  "Granted" / "Not granted", switch. Bật/tắt → `SystemStatus.setPermission` (qua Shizuku); trả về false → mở trang
  hệ thống (Notifications khi chưa hỏi lần nào → hộp thoại runtime).
  v1.8.2: tên quyền Không làm phiền là **Do Not Disturb** / **Không làm phiền** (FR-U56); thêm **Keep when unused** /
  **Giữ khi ít dùng** (icon `PauseCircle`, FR-D16) sau Pin không giới hạn.

| ID | Tiêu chí |
|---|---|
| UI-1 | Switch tắt được khi thiếu Shizuku; bật khi thiếu Shizuku vẫn lưu `enabled=true` |
| UI-2 | Status text ánh xạ 1-1 từ `EngineStatus` |
| UI-42 | `masterIcon(status)`: `Running` → ON; `Starting`, `Stopping` → PENDING; còn lại → OFF |
| UI-50 | `HomeUiState.shizuku`: chưa chạy → NOT_RUNNING; chạy, chưa cấp quyền → PERMISSION_REQUIRED; còn lại READY |
| UI-51 | `permissions` = Accessibility, Notifications (chỉ Android 13+), Media control, Do Not Disturb (v1.6.3), Battery, Keep when unused (v1.8.2, `Requirements.keepWhenUnused`), Install updates (v1.6.1), theo thứ tự, `granted` từ `Requirements` |
| UI-52 | `setPermission(p, on)` gọi `SystemStatus.setPermission(p, on)`; kết quả false → gọi fallback (mở trang hệ thống) |
| UI-54 | (v1.7) Thứ tự Home: thẻ Totch, Permissions, Advanced (Shizuku), Support (Buy me a coffee) |
| UI-55 | (v1.7) Thẻ Totch khi `NeedsAccess`: "Turn on Accessibility or Shizuku" / "Bật Trợ năng hoặc Shizuku" (UI-2). v1.8: "Needs Accessibility or Shizuku" / "Cần Trợ năng hoặc Shizuku" |
| UI-56 | (v1.8, FR-U45) Mô tả trạng thái thẻ Totch một dòng (`maxLines = 1`, "…"); chuỗi ngắn: Gestures are off / on, Turning on… / off…, Paused in landscape, Stopped: %s — vi: Cử chỉ đang tắt / bật, Đang bật… / Đang tắt…, Tạm dừng khi xoay ngang, Đã dừng: %s |
| UI-53 | (v1.5, FR-D10) Tắt switch Thông báo → hộp thoại xác nhận "Totch sẽ khởi động lại"; chỉ khi đồng ý mới gọi `setPermission(NOTIFICATIONS, false)`. v1.6.1: tắt **Install updates** cũng hỏi như vậy (FR-D11); bật / các quyền khác không hỏi |
| ~~UI-3, UI-4~~ | Bỏ ở v1.4: quyền media / pin luôn nằm trong Permissions |

### Gestures (FR-U2, FR-G8, FR-G10, FR-G12)
Các cử chỉ chia nhóm, mỗi nhóm một `SectionHeader` + `Card`:

| Nhóm | Cử chỉ |
|---|---|
| Chạm (`TAP_GROUP`) | Tap, Double tap |
| Nhấn giữ (`PRESS`) | Long press |
| Vuốt (`SWIPE`) | Swipe left, Swipe right, Swipe down |
| Vuốt và giữ (`SWIPE_HOLD`) | Swipe left & hold, Swipe right & hold, Swipe down & hold |

- Mỗi dòng: icon cử chỉ, tên, hành động đang gán, **switch** ở cuối dòng (v1.3, FR-G12). v1.4 (FR-G13): dòng có
  switch tắt (kể cả cử chỉ không khả dụng) bị **làm mờ** (icon, tên, mô tả alpha 50%). Không còn hiển thị
  "Không làm gì": cử chỉ chưa gán / đã tắt chỉ có switch tắt. v1.8 (FR-U43): cử chỉ chưa gán hiện mô tả "Not set" /
  "Chưa đặt". v1.8.2 (FR-G16): đổi thành "Nothing" / "Không làm gì".
  - Switch bật (`on = isGestureOn(g)`): chạm dòng → mở **Action picker** (bottom sheet); dòng hiện tên hành động.
  - Switch tắt: chạm dòng **không** mở picker; nếu có hành động đã nhớ, dòng hiện tên đó mờ đi.
  - ~~Bật switch: có hành động đã nhớ → bỏ g khỏi `disabledGestures`; chưa có (`None`) → mở picker, chỉ bật khi
    đã chọn hành động (đóng sheet không chọn → vẫn tắt). Tắt switch → thêm g vào `disabledGestures`, binding giữ nguyên.~~
  - v1.8.1 (FR-G15): switch = cử chỉ **nhận sự kiện**. Bật switch → thêm g vào `enabledGestures` (kể cả khi chưa đặt:
    dòng sáng, mô tả "Not set", cử chỉ nhận sự kiện nhưng không làm gì); tắt → bỏ g khỏi `enabledGestures`, binding giữ
    nguyên. Switch không bao giờ mở picker.
- v1.3: bỏ nút **Lặp lại** (FR-G11).
- Swipe down, Swipe down & hold bị vô hiệu (mờ, không bấm được, switch **tắt** + disabled) với nhãn "Cần quyền Trợ năng
  (bật ở Home)" khi Trợ năng **chưa** bật; Trợ năng đang bật → dùng như mọi cử chỉ khác (v1.5, FR-G14).
- v1.8 (FR-U44): không có Trợ năng lẫn Shizuku → **mọi** cử chỉ vô hiệu như trên, nhãn "Requires Accessibility or
  Shizuku" / "Cần Trợ năng hoặc Shizuku". Cử chỉ có hành động đã gán mà hành động không khả dụng (`Capabilities.need`,
  bảng dưới) → switch **tắt** + disabled, mô tả "<hành động> · <lý do>" màu lỗi; dòng vẫn chạm được để mở picker chọn
  hành động khác.
- ~~Picker mở ra như sheet **trong vùng nội dung** của màn Gestures (giữa top bar và bottom bar), trượt lên từ dưới; mép
  trên của sheet chạm sát mép dưới top bar (v1.5, FR-U25).~~ v1.7 (FR-U37): màn Gestures là một `BottomSheetScaffold`
  (top bar trong slot `topBar`, danh sách là `content`, picker là `sheetContent`); sheet mở ra cao **toàn màn Gestures**
  — che cả top bar, dừng ở mép trên bottom bar.
- Không có nút Reset gestures (FR-S9).

| ID | Tiêu chí |
|---|---|
| UI-12 | LaunchApp hiển thị tên app (đã gỡ: package + "Not installed"); LaunchShortcut hiển thị "<app>: <nhãn>" (đã gỡ app: thêm "Not installed") |
| UI-13 | `GesturesUiState.groups` đúng 4 nhóm theo bảng trên, đúng thứ tự |
| UI-14 | Trợ năng chưa bật: dòng `SWIPE_DOWN`, `SWIPE_DOWN_HOLD` có `enabled=false` và `on=false` (dù đã gán); mọi dòng khác `enabled=true`. Trợ năng đang bật (`Requirements.accessibilityEnabled`): mọi dòng `enabled=true`, `on = isGestureOn(g, true)` |
| UI-15 | ~~Nút Lặp lại cho cử chỉ giữ~~ — bỏ ở v1.3 (FR-G11) |
| UI-16 | `GestureRow.on = isGestureOn(g)`; `setOn(g, false)` thêm g vào `disabledGestures`, binding giữ nguyên; `setOn(g, true)` với binding ≠ None bỏ g khỏi `disabledGestures`. v1.8.1: thay bằng UI-64 |
| UI-64 | (v1.8.1, FR-G15) `setOn(g, true)` → g ∈ `enabledGestures`, binding giữ nguyên (kể cả `None`: `on=true`, `picking` không đổi); `setOn(g, false)` → g ∉ `enabledGestures`, binding giữ nguyên |
| UI-17 | ~~`setOn(g, true)` với binding `None` → không lưu gì, `picking = g`~~ (v1.8.1: UI-64); `openPicker(g)` chỉ đặt `picking` khi `on` (v1.8: hoặc khi `enabled` và hành động không khả dụng, UI-61); `closePicker()` → `picking = null` |
| UI-60 | (v1.8, FR-U44) `Capabilities(accessibility=false, shizuku=false)` → mọi dòng `enabled=false`, `on=false`, `need=ACCESS`. Chỉ Shizuku → vuốt xuống `need=ACCESSIBILITY`, các dòng khác `enabled=true` |
| UI-61 | (v1.8, FR-U44) Dòng có hành động không khả dụng (ví dụ `ToggleWifi` khi Shizuku chưa sẵn sàng, `RingerSilent` khi thiếu quyền Không làm phiền): `enabled=true`, `on=false`, `need` = lý do; `setOn(g, true)` không lưu gì; `openPicker(g)` đặt `picking = g` |

### Action picker (FR-U9, FR-U16, FR-U20, FR-A11…FR-A13)
v1.7 (FR-U37): sheet **native** của `BottomSheetScaffold` (Material 3), thay `Surface` + `AnimatedVisibility` tự vẽ:
- `rememberStandardBottomSheetState(initialValue = Hidden, skipHiddenState = false)`, `confirmValueChange` từ chối
  `PartiallyExpanded` (chỉ có Hidden / Expanded), `sheetPeekHeight = 0`, `sheetSwipeEnabled = false`,
  `sheetDragHandle = null`; mở bằng `expand()`, đóng bằng `hide()` (animation của Material).
- Sheet cao toàn màn Gestures (che top bar; nội dung sheet chừa `WindowInsets.statusBars` ở trên); bottom bar của app
  nằm ngoài scaffold nên luôn hiện và bấm được (chuyển tab → đóng picker).
- `picking` (UI-17) là nguồn sự thật: `picking ≠ null` → `expand()`; `null` → `hide()` rồi mới bỏ nội dung picker (view
  model của picker sống đúng bằng một lần mở).

~~Sheet (`Surface` bo 2 góc trên 28 dp) chiếm vùng nội dung của màn Gestures — **không** che top bar và bottom bar, hai
thanh này vẫn bấm được (chuyển tab → đóng picker).~~ **Không** kéo xuống để ẩn; hàng tiêu đề = tên cử chỉ + nút **đóng**
(✕) bên phải; nút Back hệ thống cũng đóng. v1.8.1 (FR-U47): bên dưới là **ô chọn tab** (`ExposedDropdownMenuBox` +
`OutlinedTextField` chỉ đọc, đầy đủ chiều rộng trừ gutter, icon tab đang chọn ở đầu, mũi tên ở cuối; menu liệt kê các tab
theo thứ tự, mỗi mục có icon + tên, mục đang chọn được tô) thay hàng chip. ~~Bên dưới là **hàng chip cuộn ngang** (`LazyRow` của `FilterChip`, mỗi chip có
icon + nhãn, chip đang chọn được tô màu và luôn được cuộn vào tầm nhìn — v1.5, FR-U25; thay segmented button để thêm
mục mới như Intents, Settings… mà nhãn không bị hẹp lại).~~ Nội dung đổi mục có fade:

| Mục | Nội dung |
|---|---|
| Basic (v1.8.1, FR-U46: trước là System; en **Basic**, vi **Cơ bản**, icon `Category`) | v1.6.3 (FR-U34, FR-U35): ô tìm kiếm như tab Applications, rồi 6 nhóm (bảng dưới). ~~Không có "Không làm gì".~~ v1.8 (FR-U43): dòng **Not set** / **Chưa đặt** (icon `Block`, một card riêng không tiêu đề) ở đầu danh sách, ẩn khi query không khớp tên của nó. Hành động âm thanh (trừ âm lượng) có dòng phụ "Requires Do Not Disturb access" khi chưa có quyền (FR-A18) |
| Applications | Ô tìm kiếm + danh sách app (icon, tên, radio) |

**Ô tìm kiếm + loại hành động** (v1.8.2, FR-U58 — thay ô chọn tab của FR-U47 và ô tìm kiếm riêng của từng tab):
một ô duy nhất dưới hàng tiêu đề, cao 48 dp, bo tròn hoàn toàn, nền `surfaceContainerHigh`, đầy đủ chiều rộng trừ gutter.
```
( [▦▾]  Search applications                 [✕] | [⌄] )
```
- **Trái**: icon của loại đang chọn (`IconButton`, mô tả "Action type: <loại>") — bấm → `DropdownMenu` rộng bằng ô, mỗi
  mục: icon + tên loại + `RadioButton` bên phải; mục đang chọn nền `secondaryContainer`, chữ / icon `onSecondaryContainer`.
  Chọn → `selectTab`, menu đóng.
- **Giữa**: ô nhập, placeholder `picker_search` = "Search %1$s" / "Tìm %1$s" với tên loại viết thường theo locale
  ("Search applications", "Tìm cài đặt nhanh"); `ImeAction.Search` ẩn bàn phím.
- **Phải**: query không rỗng → nút **xóa** (✕); rỗng → **mũi tên thả xuống** (xoay khi mở menu), bấm cũng mở menu.
- Query là của loại đang chọn (`ActionPickerUiState.query`); mỗi loại giữ query riêng khi đổi qua lại.
- Nhãn loại (en / vi): Basic / Cơ bản, **Applications** / Ứng dụng, Shortcuts / Shortcut, Activities / Activity,
  **Quick settings** / **Cài đặt nhanh** (trước: Apps, Tiles / Ô nhanh).

~~Ô tìm kiếm (v1.7, FR-U38):~~ (v1.7, FR-U38 — thay ở v1.8.2:) `CompactSearchField` cao 44 dp (thay `SearchBar` 56 dp), bo tròn hoàn toàn, nền
`surfaceContainerHigh`, icon kính lúp 20 dp, nút xóa khi có chữ, bàn phím `ImeAction.Search` (bấm → ẩn bàn phím).

Hành động chỉ chạy qua Shizuku (v1.7, FR-A20 — actions §14) có dòng phụ "Requires Shizuku" / "Cần Shizuku" khi Shizuku
chưa sẵn sàng; ở tab Tiles dòng phụ thành "<tên app> · Requires Shizuku".

Khả dụng (v1.8, FR-U44 — `Capabilities` từ `Requirements`: `accessibility`, `shizuku` = running && granted,
`dndAccess`). `Capabilities.need(action)`:

| Hành động (`ActionRequirements.of`) | Không khả dụng khi | `Need` | Lý do (en / vi) |
|---|---|---|---|
| `None` | không bao giờ | — | — |
| LOCAL, trong `ActionCatalog.needsDndAccess` | `!dndAccess` | `DND` | Requires Do Not Disturb access / Cần quyền Không làm phiền |
| LOCAL khác | không bao giờ | — | — |
| {ACCESSIBILITY, SHIZUKU} (điều hướng, khóa, chụp, mở app / shortcut / activity…) | `!accessibility && !shizuku` | `ACCESS` | Requires Accessibility or Shizuku / Cần Trợ năng hoặc Shizuku |
| {SHIZUKU} (FR-A20) | `!shizuku` | `SHIZUKU` | Requires Shizuku / Cần Shizuku |

Dòng không khả dụng ở mọi tab: **vô hiệu** (mờ 38%, radio disabled, không chọn được), dòng phụ = lý do (tab Tiles:
"<tên app> · <lý do>"). `select(action)` bỏ qua hành động không khả dụng.
| Shortcuts | Ô tìm kiếm + danh sách **app** như tab Applications; chạm app → mở rộng/thu gọn, shortcut của app hiện ngay bên dưới (thụt vào, icon `SubdirectoryArrowRight`, nhãn, radio). Mặc định thu gọn tất cả; khi đang tìm kiếm, mọi app khớp đều mở rộng |
| Activities | (v1.5, FR-A14) Giống hệt Shortcuts nhưng dòng con là activity: icon activity, nhãn + dòng phụ tên lớp rút gọn, radio. Trạng thái mở rộng riêng (`expandedActivities`) |
| Tiles | (v1.6, FR-A15) Ô tìm kiếm + gợi ý một dòng "Chỉ ô đã thêm vào Cài đặt nhanh mới phản hồi" + danh sách phẳng các ô (icon app, nhãn ô + dòng phụ tên app, radio), một card (UI-26) |

Nhóm của tab System (v1.6.3, `ActionCatalog.groups`, theo thứ tự):

| Nhóm (en / vi) | Hành động |
|---|---|
| Navigation / Điều hướng | Back, Home, Recents, Notifications, Quick settings, One-handed mode |
| System / Hệ thống | Lock screen, Screenshot, Assistant, Torch |
| Media / Media | Play/pause, Previous, Next |
| Sound / Âm thanh | Volume up, Volume down, Volume panel, Ring, Vibrate, Silent, Cycle sound mode, Do Not Disturb |
| Display / Màn hình | Brightness up, Brightness down, Auto brightness, Auto-rotate |
| Connections / Kết nối | Wi-Fi, Bluetooth, Mobile data, Airplane mode, Location |

Tìm kiếm (`filterCatalog(groups, query, actionLabel, groupLabel)`): so khớp sau khi chuẩn hóa (trim, chữ thường, bỏ dấu
tiếng Việt — NFD bỏ dấu kết hợp, `đ` → `d`); query khớp **tên nhóm** → giữ cả nhóm; ngược lại giữ các hành động có tên
khớp; nhóm rỗng bị ẩn; không có kết quả → "No actions match "…"".

Mỗi nhóm (System, Media, danh sách app, danh sách app + shortcut) hiển thị như nhóm cử chỉ: **một card bo góc**, các
dòng (icon, tên, radio) ngăn cách bằng đường kẻ ngang (UI-26). Chọn → lưu binding, **bật** cử chỉ (`withBinding`) và
đóng sheet.

| ID | Tiêu chí |
|---|---|
| UI-10 | Chọn hành động (bất kỳ tab) → `SettingsRepository.update` với binding mới và g bị bỏ khỏi `disabledGestures`, `finished=true` |
| UI-11 | Tab Ứng dụng lọc không phân biệt hoa thường theo tên/package, sắp xếp theo tên |
| UI-20 | Tab ban đầu = tab chứa hành động hiện tại (LaunchApp → Applications, LaunchShortcut → Shortcuts, LaunchActivity → Activities, ClickTile → Tiles, còn lại → System) |
| UI-21 | Tab Shortcut dùng `groupShortcuts` (SH-1, SH-2); chọn → `LaunchShortcut(pkg, id, label, intentUri)` |
| UI-22 | App và shortcut chỉ được tải lần đầu mở tab tương ứng (hoặc tab ban đầu) |
| UI-23 | `ActionCatalog.groups` không chứa `None`, mỗi hành động cố định xuất hiện đúng một lần; v1.6.3: đúng 6 nhóm theo thứ tự NAVIGATION, SYSTEM, MEDIA, SOUND, DISPLAY, CONNECTIVITY với nội dung như bảng trên |
| UI-24 | (v1.6.3, FR-U34) `filterCatalog`: query rỗng → nguyên danh sách; khớp tên hành động không phân biệt hoa thường / dấu (`"am luong"` khớp "Tăng âm lượng", `"WI-FI"` khớp "Wi-Fi"); khớp tên nhóm → cả nhóm; nhóm không còn hành động bị bỏ; thứ tự nhóm và hành động giữ nguyên |
| UI-29 | (v1.6.3) `ActionPickerUiState.systemQuery`, `setSystemQuery`; `needsDndAccess(action)` đúng cho RingerNormal/Vibrate/Silent/CycleRingerMode/ToggleDnd khi `Requirements.dndAccessGranted = false` |
| UI-25 | `expanded` ban đầu rỗng; `toggleExpanded(pkg)` thêm/bớt pkg; `isExpanded(pkg)` = đang tìm kiếm (query không rỗng) hoặc pkg ∈ `expanded` |
| UI-27 | (v1.5) Tab Activities dùng `groupActivities` (AC-1, AC-2), tải lần đầu mở tab (như UI-22); `toggleActivityApp(pkg)`/`isActivityAppExpanded(pkg)` như UI-25; chọn → `LaunchActivity(pkg, activity, label)` |
| UI-28 | (v1.6) Tab Tiles dùng `filterTiles` (QT-1), tải lần đầu mở tab (như UI-22); chọn → `ClickTile(pkg, service, label)` |
| UI-29b | (v1.7) `ActionPickerUiState.shizukuReady` = `Requirements.shizukuRunning && shizukuGranted`; `needsShizuku` theo AR-10. v1.8: thay bằng `capabilities` + `need(action)` (UI-62) |
| UI-62 | (v1.8, FR-U44) `Capabilities.need` theo bảng Khả dụng; `ActionPickerUiState.need(action)` dùng `Requirements` hiện tại; `select` một hành động có `need ≠ null` → không lưu, `finished=false` |
| UI-63 | (v1.8, FR-U43) `select(None)` → binding `None`, `finished=true`, cử chỉ tắt (`isGestureOn = false`). v1.8.1: thay bằng UI-65 |
| UI-65 | (v1.8.1, FR-G15) `select(None)` → binding `None`, `finished=true`, cử chỉ **vẫn bật** (`isGestureOn = true`) |
| UI-66 | (v1.8.1, FR-U46) `PickerTab.entries` = BASIC, APPS, SHORTCUTS, ACTIVITIES, TILES; `initialTab` của hành động cố định / `None` = BASIC; `basicQuery`, `setBasicQuery` (thay `systemQuery`, UI-29) |
| UI-71 | (v1.8.2, FR-U58) `ActionPickerUiState.query` = query của `tab` (BASIC → `basicQuery`, APPS → `appQuery`, …); `setQuery(v)` ghi vào query của tab hiện tại, query của tab khác giữ nguyên |
| UI-72 | (v1.8.2, FR-G16) Nhãn `None`: "Nothing" / "Không làm gì"; dòng Nothing của tab Basic vẫn ẩn khi query không khớp tên mới |
| UI-26 | Mỗi nhóm là một card bo góc (`Modifier.groupItem(index, count)`: bo góc trên ở dòng đầu, góc dưới ở dòng cuối, đường kẻ trên mỗi dòng trừ dòng đầu) |

### Notch (FR-U12, FR-U17, FR-U18, FR-S9, FR-R5…FR-R7) — tab thứ 3
v1.4: đổi tên màn **Preferences** thành **Notch** (tiêu đề và nhãn tab). Các section, theo thứ tự (v1.5: Haptic feedback
chuyển sang Settings, bỏ section Phản hồi):
1. **Độ nhạy**: Double tap, Long press, Swipe.
2. **Vị trí**: Horizontal (%), Vertical (dp, tính từ cạnh trên).
3. **Kích thước**: Width (dp), Height (dp), Padding (dp).

v1.6 (FR-U28): các slider trong một section ngăn bằng `RowDivider` (thụt lề hai bên như Settings).

v1.8.2 (FR-U57): `TotchSlider` dùng **track mặc định của Material 3** (`SliderDefaults.Track`: cao 16 dp, khe 6 dp quanh
nút kéo, chấm dừng ở cuối) và nút kéo **hình tròn** 24 dp màu `primary` (20 dp khi đang kéo / nhấn, như nút kéo M3 thu
hẹp khi kéo), không bóng. Thay mô tả v1.8.1 dưới đây.

~~v1.8.1 (FR-U53):~~ thanh trượt là `TotchSlider` — `Slider` của Material 3 với nút kéo **hình tròn** 20 dp màu `primary`
(thay thanh dọc), track không có khe quanh nút kéo; mọi thanh trượt trong app dùng component này.

Mỗi slider (FR-U18): hàng 1 là nhãn ngắn + giá trị; hàng 2 là nút **−**, thanh slider, nút **+**, nút reset. Kéo
slider bắt vào nấc (bội số của bước, UI-35) nhưng **không vẽ vạch**; nút − / + đổi đúng một đơn vị (UI-36), lưu ngay,
tắt khi đã ở đầu / cuối thang.

| Slider | Thang | Nấc (kéo) | − / + |
|---|---|---|---|
| Double tap | 100–1000 ms | 50 | 10 |
| Long press | 100–1000 ms | 50 | 10 |
| Swipe | 20–200 dp | 5 | 1 |
| Horizontal | 0–100 % | 5 | 1 |
| Vertical | 0–100 dp | 5 | 1 |
| Width | 10–400 dp | 5 | 1 |
| Height | 10–100 dp | 5 | 1 |
| Padding | 1–10 dp | 5 | 1 |

Giá trị đã lưu lệch nấc (vd. vùng tự phát hiện rộng 87 dp) vẫn hiển thị đúng; nút − / + đổi đúng 1 đơn vị từ giá trị
đó. `sanitized()` giữ giới hạn ST-2.

- Không có switch "Custom touch area": slider vị trí/kích thước **luôn hiển thị**. Giá trị hiển thị
  `area = manualRegion ?: detectedRegion ?: ManualRegionDp()` (`detectedRegion` = vùng tự phát hiện, notch-region NR-12).
  Kéo một slider → lưu `manualRegion = area.copy(<field> = giá trị mới)`.
- Chỉ **slider** có nút reset (icon ↺), đặt **bên phải thanh slider**, chỉ bật khi khác mặc định. Switch không có reset.
  Mặc định của slider vùng chạm = giá trị của `detectedRegion`; reset field cuối cùng khác mặc định → `manualRegion = null`
  (quay về tự phát hiện).
- Highlight trực tiếp (§2).

| ID | Tiêu chí |
|---|---|
| UI-30 | `NotchSetting` gồm đúng các slider (v1.3: bỏ REPEAT_INTERVAL): DOUBLE_TAP, LONG_PRESS, SWIPE_DISTANCE, AREA_X, AREA_Y, AREA_WIDTH, AREA_HEIGHT, PADDING. `reset(item, s, auto)` đặt đúng field đó về mặc định (`Settings()` hoặc `auto` cho vùng chạm), field khác giữ nguyên; `isDefault(item, s, auto)` đúng khi bằng mặc định |
| UI-31 | Reset slider vùng chạm khi mọi field khác đã bằng `auto` → `manualRegion = null`; khi `manualRegion = null` mọi slider vùng chạm `isDefault` |
| UI-33 | `NotchSettingsUiState.area` = `manualRegion ?: detectedRegion ?: ManualRegionDp()`; `setArea(region)` lưu `manualRegion` (mọi slider, kể cả vị trí dọc) |
| UI-34 | Setter độ nhạy / padding lưu đúng field (kẹp bởi repository) |
| UI-35 | `snapToStep(v, range, step)` = `round(v/step)·step`, kẹp trong `range` (hai đầu thang luôn chọn được, vd. padding 1, 5, 10) |
| UI-36 | `stepBy(v, delta, range)` = `v + delta` kẹp trong `range` |

### Cài đặt ứng dụng (FR-U12, FR-U14, FR-S6…FR-S8, FR-U23) — tab thứ 4 "Settings" (v1.4)
Các section, theo thứ tự (v1.5):
1. **Hệ thống**: Start on boot (icon nguồn), Haptic feedback (icon rung), Gestures in landscape (icon xoay màn hình —
   FR-R8) — đều là switch (thứ tự FR-U24).
   v1.6 (FR-U26): mỗi dòng có mô tả ngắn một dòng:

   | Dòng | en | vi |
   |---|---|---|
   | Start on boot | Turn gestures on after restart | Bật cử chỉ sau khi khởi động lại |
   | Haptic feedback | Vibrate when a gesture runs | Rung khi thực hiện cử chỉ |
   | Gestures in landscape | Keep the notch active when rotated | Vẫn nhận chạm ở notch khi xoay |
2. **Giao diện**: Theme (icon bảng màu; System / Light / Dark — **dialog chọn** như Ngôn ngữ), Amoled black (icon độ
   tương phản, switch, mô tả "Pure black dark theme"), Ngôn ngữ (icon ngôn ngữ; System / English / Tiếng Việt — dialog
   chọn). Theme và Ngôn ngữ có **chevron** (›) ở cuối dòng. v1.8.1 (FR-U49): dòng cuối **Show navigation labels** /
   **Hiện nhãn thanh điều hướng** (icon `Label`, switch, mô tả "Text under the bottom bar icons" / "Chữ dưới icon thanh
   điều hướng").
3. **Cập nhật** (v1.6.1, FR-U29 — [update §6](update.md)): *Check on app launch* (icon tên lửa/khởi chạy, switch, mô
   tả "Look for a new version when Totch opens" / "Tìm bản mới mỗi khi mở Totch" — v1.8.1, FR-U51: **Check on launch** /
   **Kiểm tra khi mở**, mô tả "When Totch opens" / "Mỗi khi mở Totch"), *Automatic checks* (icon đồng hồ cập nhật,
   giá trị Off / Daily / Weekly / Monthly, chevron, dialog chọn), *Check for updates* (icon cập nhật hệ thống, mô tả theo
   trạng thái — chuyển từ Giới thiệu).
4. **Ủng hộ** (v1.7, FR-U40): một dòng *Buy me a coffee* (§1 — Buy me a coffee).
5. **Giới thiệu** (cuối cùng): phiên bản, license, mã nguồn (`github.com/ptt98hd/totch`), credit ("Shizuku by
   RikkaApps"). Dòng mở link có **chevron** (›) ở cuối.

- Mọi mô tả (dòng phụ) chỉ một dòng (`maxLines = 1`, cắt bằng "…" nếu vẫn quá dài).

- Mọi dòng có icon trong box tonal bên trái (`TonalIcon`, như Giới thiệu); các dòng trong một section ngăn bằng
  `HorizontalDivider` (thụt lề hai bên như Giới thiệu).
- Không có nút reset nào (FR-S9).

| ID | Tiêu chí |
|---|---|
| UI-32 | Setter theme / amoled / language / startOnBoot / haptics / landscapeGestures lưu đúng field |
| UI-37 | (v1.6) Dòng Check for updates hiển thị mô tả theo `UpdateState` (UP-6); bấm khi `Available` → hộp thoại tên bản + ghi chú phát hành, nút **Update** / **Later** (v1.6.1: hộp thoại ở `MainActivity`, update §5) |
| UI-67 | (v1.8.1, FR-U49) `setNavLabels(v)` lưu `navLabels` |
| UI-38 | (v1.6.1) Thứ tự section: System, Appearance, Update, About; Update có đúng 3 dòng như trên, About không còn Check for updates. v1.7: System, Appearance, Update, **Support**, About |

### Giao diện & ngôn ngữ (FR-S6, FR-S7)
| ID | Tiêu chí |
|---|---|
| TH-1 | `ThemeMode.SYSTEM` theo `isSystemInDarkTheme()`, LIGHT/DARK cố định; icon status bar / navigation bar đổi theo |
| TH-2 | `amoledBlack` chỉ có tác dụng ở dark: `background`, `surface`, `surfaceContainer*` → đen thuần / gần đen (`amoled(scheme)` là hàm thuần) |
| LG-1 | Lựa chọn: System, English, Tiếng Việt. System + ngôn ngữ máy không được hỗ trợ → tiếng Anh (resource mặc định) |
| LG-2 | Android 13+: áp dụng bằng `LocaleManager.setApplicationLocales` (có `locales_config.xml` → hiện trong cài đặt ngôn ngữ app của hệ thống); Android 12: bọc `Context` trong `attachBaseContext` của Activity/Application + `recreate()` |
| LG-3 | Android 13+: nếu người dùng đổi ngôn ngữ app trong cài đặt hệ thống, lần mở app sau `language` trong Settings được đồng bộ theo hệ thống |

### Icon (v1.6.3 — FR-U33; v1.6.2 — FR-U32, thay FR-U11)
Adaptive icon **lỗ đen**, nguồn duy nhất là `scripts/icon/gen.py` (sinh 3 drawable XML + SVG xem trước +
`fastlane/.../images/icon.png` qua ImageMagick). Hình học (viewport 108):
- Vòng sáng thấu kính hấp dẫn: vành khuyên tâm **(54, 54)**, bán kính ngoài 20.5, trong 14.5; chân trời sự kiện là
  hình tròn tâm (54, 54) bán kính 14.5.
- Đĩa bồi tụ: hình **con thoi** (lens) **nằm ngang**, đối xứng qua y = 54, từ x = 21.6 đến 86.4, dày 5.2 ở giữa (hai
  cung bán kính lớn). Vòng sáng bị cắt quanh đĩa với khe 1.2 (cung đồng tâm với mép đĩa) nên đĩa tách rõ khỏi vòng.
- Không xoay. ~~Vài ngôi sao 4 cánh ở nền~~ v1.8.1 (FR-U55): **4 ngôi sao 4 cánh giống hệt nhau** (bán kính mũi 2.6),
  tâm tại (54 ± 19, 54 ± 19) — mỗi góc một ngôi sao, đối xứng qua x = 54 và y = 54 — nằm trong vùng an toàn 66 dp.

| ID | Tiêu chí |
|---|---|
| IC-1 | Mọi vòng tròn tâm (54, 54); đĩa đối xứng qua y = 54 và x = 54; không có `rotation` trong drawable |
| IC-2 | Màu Monet (Android 12+, minSdk 31): nền `@android:color/system_accent1_800`, sao `system_accent1_200`, vòng sáng `system_accent1_200`, đĩa `system_accent1_100`, chân trời `system_neutral1_1000` → đổi theo hình nền (launcher vẽ lại khi bảng màu đổi) |
| IC-3 | Layer `monochrome` = vòng sáng + đĩa (cùng hình, alpha) cho themed icon Android 13+; `ic_stat_totch` cùng hình |
| IC-5 | (v1.8.1, FR-U55) `STARS` gồm đúng 4 ngôi sao cùng hình (`star(x, y)`), tâm (35, 35), (73, 35), (35, 73), (73, 73) |
| IC-4 | `icon.png` 512×512 cho F-Droid dùng bảng màu xem trước cố định (tím) |

### Splash screen (v1.6.1 — FR-U31)
Splash mặc định của Android 12+ (launcher icon trên nền cửa sổ, không có icon động riêng). `MainActivity` giữ splash
đến khi đọc xong settings (`OnPreDrawListener` trả false — trước đây nền cửa sổ trống hiện trong lúc này), rồi splash
mờ dần + phóng nhẹ (`setOnExitAnimationListener`, 250 ms).
