# Spec: Giao diện (Material 3)

Yêu cầu: FR-U1…FR-U22, FR-G12, FR-G13, FR-S6…FR-S9, FR-R5…FR-R7, FR-D9 · Module: `:app` · Package: `io.github.ptt98hd.totch.ui`

## 1. Nguyên tắc
- Jetpack Compose + Material 3, `dynamicLightColorScheme/dynamicDarkColorScheme`, edge-to-edge.
- Một Activity (`MainActivity`), Navigation Compose.
- **Top bar tĩnh** (`TopAppBar` pinned, không co giãn khi cuộn) trên mọi màn hình, **chỉ có tiêu đề** = tên màn hình
  ở bên trái (v1.4: bỏ icon Settings và nút Back).
- `NavigationBar` dưới cùng có 4 mục (v1.4, FR-U19): **Home**, **Gestures**, **Notch** (icon điện thoại có notch,
  vector riêng `NotchIcons`), **Settings** (cài đặt ứng dụng). Luôn hiển thị.
- Chuyển tab có animation (FR-U22): trang mới trượt vào 1/5 chiều rộng + fade theo hướng tab (sang phải nếu tab
  đích nằm bên phải), trang cũ trượt ngược lại; 300 ms.
- Action picker là **sheet** trên màn Gestures (FR-U20), không phải destination riêng.
- Mỗi màn hình có `ViewModel` riêng, expose `StateFlow<UiState>`; Composable stateless nhận `state` + lambda → test được.
- Giao diện gọn: không có đoạn mô tả giải thích dài.
- Chuỗi trong `strings.xml` (en) và `values-vi/strings.xml`.

| ID | Tiêu chí |
|---|---|
| UI-40 | Top bar pinned, chỉ có tiêu đề = tên màn hình bên trái |
| UI-41 | Bottom bar có Home, Gestures, Notch, Settings theo thứ tự; luôn hiển thị |
| UI-43 | Chuyển tab: hướng trượt = dấu của (chỉ số tab đích − chỉ số tab nguồn) |

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

### Home (FR-U1, FR-D4, FR-D9, FR-U21) — v1.4
```
┌──────────────────────────────────┐
│ Home                             │  ← top bar tĩnh, chỉ tiêu đề
│ ┌──────────────────────────────┐ │
│ │ ┌──┐ Totch            [ ON ] │ │
│ │ │✓ │ Gestures are on         │ │
│ │ └──┘                         │ │
│ └──────────────────────────────┘ │
│ Requirement                      │
│ [▣] Shizuku                [on]  │  Running, permission granted / Not running / …
│ Permissions                      │
│ [♿] Accessibility          [on]  │  Granted / Not granted
│ [🔔] Notifications         [off] │  (Android 13+)
│ [♪] Media control          [off] │
│ [🔋] Unrestricted battery  [on]  │
├──────────────────────────────────┤
│ Home | Gestures | Notch | Settings│
└──────────────────────────────────┘
```
- Thẻ Totch: tiêu đề = tên app; bên trái là **box** bo góc (48 dp) chứa 1 trong 3 icon (UI-42): `Check` (bật,
  box `primary`), `Close` (tắt; box `error` khi đã bật mà có lỗi / thiếu Shizuku, ngược lại `surfaceVariant`),
  `Sync` xoay liên tục (đang bật / đang tắt, box `tertiary`). Mô tả ngắn "Gestures are …" theo `EngineStatus`.
- **Requirement**: 1 dòng Shizuku (icon, tên, trạng thái, switch = sẵn sàng). Chạm: chưa cấp quyền → hộp thoại
  quyền Shizuku; còn lại → mở app Shizuku (hoặc trang web khi chưa cài).
- **Permissions**: mỗi dòng như thẻ Totch nhưng icon là **biểu tượng quyền** (box tonal), tên quyền, mô tả
  "Granted" / "Not granted", switch. Bật/tắt → `SystemStatus.setPermission` (qua Shizuku); trả về false → mở trang
  hệ thống (Notifications khi chưa hỏi lần nào → hộp thoại runtime).

| ID | Tiêu chí |
|---|---|
| UI-1 | Switch tắt được khi thiếu Shizuku; bật khi thiếu Shizuku vẫn lưu `enabled=true` |
| UI-2 | Status text ánh xạ 1-1 từ `EngineStatus` |
| UI-42 | `masterIcon(status)`: `Running` → ON; `Starting`, `Stopping` → PENDING; còn lại → OFF |
| UI-50 | `HomeUiState.shizuku`: chưa chạy → NOT_RUNNING; chạy, chưa cấp quyền → PERMISSION_REQUIRED; còn lại READY |
| UI-51 | `permissions` = Accessibility, Notifications (chỉ Android 13+), Media control, Battery, theo thứ tự, `granted` từ `Requirements` |
| UI-52 | `setPermission(p, on)` gọi `SystemStatus.setPermission(p, on)`; kết quả false → gọi fallback (mở trang hệ thống) |
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
  "Không làm gì": cử chỉ chưa gán / đã tắt chỉ có switch tắt.
  - Switch bật (`on = isGestureOn(g)`): chạm dòng → mở **Action picker** (bottom sheet); dòng hiện tên hành động.
  - Switch tắt: chạm dòng **không** mở picker; nếu có hành động đã nhớ, dòng hiện tên đó mờ đi.
  - Bật switch: có hành động đã nhớ → bỏ g khỏi `disabledGestures`; chưa có (`None`) → mở picker, chỉ bật khi
    đã chọn hành động (đóng sheet không chọn → vẫn tắt). Tắt switch → thêm g vào `disabledGestures`, binding giữ nguyên.
- v1.3: bỏ nút **Lặp lại** (FR-G11).
- Swipe down, Swipe down & hold luôn bị vô hiệu (mờ, không bấm được, switch **tắt** + disabled) với nhãn "Không khả
  dụng: hệ thống luôn kéo thanh thông báo".
- Picker mở ra như sheet **trong vùng nội dung** của màn Gestures (giữa top bar và bottom bar), trượt lên từ dưới.
- Không có nút Reset gestures (FR-S9).

| ID | Tiêu chí |
|---|---|
| UI-12 | LaunchApp hiển thị tên app (đã gỡ: package + "Not installed"); LaunchShortcut hiển thị "<app>: <nhãn>" (đã gỡ app: thêm "Not installed") |
| UI-13 | `GesturesUiState.groups` đúng 4 nhóm theo bảng trên, đúng thứ tự |
| UI-14 | Dòng `SWIPE_DOWN`, `SWIPE_DOWN_HOLD` có `enabled=false` và `on=false` (dù đã gán); mọi dòng khác `enabled=true` |
| UI-15 | ~~Nút Lặp lại cho cử chỉ giữ~~ — bỏ ở v1.3 (FR-G11) |
| UI-16 | `GestureRow.on = isGestureOn(g)`; `setOn(g, false)` thêm g vào `disabledGestures`, binding giữ nguyên; `setOn(g, true)` với binding ≠ None bỏ g khỏi `disabledGestures` |
| UI-17 | `setOn(g, true)` với binding `None` → không lưu gì, `picking = g`; `openPicker(g)` chỉ đặt `picking` khi `on`; `closePicker()` → `picking = null` |

### Action picker (FR-U9, FR-U16, FR-U20, FR-A11…FR-A13)
Sheet (`Surface` bo 2 góc trên 28 dp) chiếm vùng nội dung của màn Gestures — **không** che top bar và bottom bar, hai
thanh này vẫn bấm được (chuyển tab → đóng picker). **Không** kéo xuống để ẩn; hàng tiêu đề = tên cử chỉ + nút **đóng**
(✕) bên phải; nút Back hệ thống cũng đóng. Bên dưới là `SingleChoiceSegmentedButtonRow` 3 mục, nội dung đổi tab có
fade:

| Mục | Nội dung |
|---|---|
| System | Nhóm System (Back, Home, Recents, Lock, Screenshot, Notifications, Quick settings, Assistant, Torch) và Media (Play/pause, Previous, Next, Volume up/down, Volume panel). **Không** có ô tìm kiếm (v1.4), không có "Không làm gì" |
| Applications | Ô tìm kiếm + danh sách app (icon, tên, radio) |
| Shortcuts | Ô tìm kiếm + danh sách **app** như tab Applications; chạm app → mở rộng/thu gọn, shortcut của app hiện ngay bên dưới (thụt vào, icon `SubdirectoryArrowRight`, nhãn, radio). Mặc định thu gọn tất cả; khi đang tìm kiếm, mọi app khớp đều mở rộng |

Mỗi nhóm (System, Media, danh sách app, danh sách app + shortcut) hiển thị như nhóm cử chỉ: **một card bo góc**, các
dòng (icon, tên, radio) ngăn cách bằng đường kẻ ngang (UI-26). Chọn → lưu binding, **bật** cử chỉ (`withBinding`) và
đóng sheet.

| ID | Tiêu chí |
|---|---|
| UI-10 | Chọn hành động (bất kỳ tab) → `SettingsRepository.update` với binding mới và g bị bỏ khỏi `disabledGestures`, `finished=true` |
| UI-11 | Tab Ứng dụng lọc không phân biệt hoa thường theo tên/package, sắp xếp theo tên |
| UI-20 | Tab ban đầu = tab chứa hành động hiện tại (LaunchApp → Applications, LaunchShortcut → Shortcuts, còn lại → System) |
| UI-21 | Tab Shortcut dùng `groupShortcuts` (SH-1, SH-2); chọn → `LaunchShortcut(pkg, id, label, intentUri)` |
| UI-22 | App và shortcut chỉ được tải lần đầu mở tab tương ứng (hoặc tab ban đầu) |
| UI-23 | `ActionCatalog.groups` không chứa `None`; nhóm Media có `VolumePanel` sau `VolumeDown` |
| ~~UI-24~~ | Bỏ ở v1.4: không còn tìm kiếm hành động |
| UI-25 | `expanded` ban đầu rỗng; `toggleExpanded(pkg)` thêm/bớt pkg; `isExpanded(pkg)` = đang tìm kiếm (query không rỗng) hoặc pkg ∈ `expanded` |
| UI-26 | Mỗi nhóm là một card bo góc (`Modifier.groupItem(index, count)`: bo góc trên ở dòng đầu, góc dưới ở dòng cuối, đường kẻ trên mỗi dòng trừ dòng đầu) |

### Notch (FR-U12, FR-U17, FR-U18, FR-S9, FR-R5…FR-R7) — tab thứ 3
v1.4: đổi tên màn **Preferences** thành **Notch** (tiêu đề và nhãn tab). Các section, theo thứ tự:
1. **Phản hồi**: Haptic feedback (switch).
2. **Độ nhạy**: Double tap, Long press, Swipe.
3. **Vị trí**: Horizontal (%), Vertical (dp, tính từ cạnh trên).
4. **Kích thước**: Width (dp), Height (dp), Padding (dp).

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
| UI-34 | Setter độ nhạy / padding / haptics lưu đúng field (kẹp bởi repository) |
| UI-35 | `snapToStep(v, range, step)` = `round(v/step)·step`, kẹp trong `range` (hai đầu thang luôn chọn được, vd. padding 1, 5, 10) |
| UI-36 | `stepBy(v, delta, range)` = `v + delta` kẹp trong `range` |

### Cài đặt ứng dụng (FR-U12, FR-U14, FR-S6…FR-S8) — tab thứ 4 "Settings" (v1.4)
Các section, theo thứ tự:
1. **Giao diện**: Theme (System / Light / Dark — **dialog chọn** như Ngôn ngữ), AMOLED black (switch),
   Ngôn ngữ (System / English / Tiếng Việt — dialog chọn). Theme và Ngôn ngữ có **chevron** (›) ở cuối dòng.
2. **Hệ thống**: Start on boot.
3. **Giới thiệu** (cuối cùng): phiên bản, license, mã nguồn, credit Shizuku.

- Không có nút reset nào (FR-S9).

| ID | Tiêu chí |
|---|---|
| UI-32 | Setter theme / amoled / language / startOnBoot lưu đúng field |

### Giao diện & ngôn ngữ (FR-S6, FR-S7)
| ID | Tiêu chí |
|---|---|
| TH-1 | `ThemeMode.SYSTEM` theo `isSystemInDarkTheme()`, LIGHT/DARK cố định; icon status bar / navigation bar đổi theo |
| TH-2 | `amoledBlack` chỉ có tác dụng ở dark: `background`, `surface`, `surfaceContainer*` → đen thuần / gần đen (`amoled(scheme)` là hàm thuần) |
| LG-1 | Lựa chọn: System, English, Tiếng Việt. System + ngôn ngữ máy không được hỗ trợ → tiếng Anh (resource mặc định) |
| LG-2 | Android 13+: áp dụng bằng `LocaleManager.setApplicationLocales` (có `locales_config.xml` → hiện trong cài đặt ngôn ngữ app của hệ thống); Android 12: bọc `Context` trong `attachBaseContext` của Activity/Application + `recreate()` |
| LG-3 | Android 13+: nếu người dùng đổi ngôn ngữ app trong cài đặt hệ thống, lần mở app sau `language` trong Settings được đồng bộ theo hệ thống |

### Icon (FR-U11)
Adaptive icon theo ảnh tham khảo: màn hình điện thoại bo góc, notch dạng viên thuốc, các dải sóng toả xuống
từ notch, vòng chạm và bàn tay chỉ. Màu Monet (`system_accent*`) → đổi theo hình nền; có layer `monochrome`
cho themed icon Android 13+. Icon thông báo giữ dạng đơn sắc.
