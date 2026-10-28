# Spec: Giao diện (Material 3)

Yêu cầu: FR-U1…FR-U14, FR-S6…FR-S9, FR-R5, FR-R6 · Module: `:app` · Package: `io.github.ptt98hd.totch.ui`

## 1. Nguyên tắc
- Jetpack Compose + Material 3, `dynamicLightColorScheme/dynamicDarkColorScheme`, edge-to-edge.
- Một Activity (`MainActivity`), Navigation Compose.
- **Top bar tĩnh** (`TopAppBar` pinned, không co giãn khi cuộn) trên mọi màn hình, tiêu đề = tên màn hình hiện tại
  ở bên trái.
  - Home, Gestures, Notch: chỉ có tiêu đề + icon **Settings ở bên phải** → mở **Cài đặt ứng dụng**. Không có nút nào khác.
  - Cài đặt ứng dụng, Action picker: nút **Back** ở bên trái, không có action bên phải.
- `NavigationBar` dưới cùng có 3 mục: **Home**, **Gestures**, **Notch** (icon Tune); ẩn ở Cài đặt ứng dụng / Action picker.
- Mỗi màn hình có `ViewModel` riêng, expose `StateFlow<UiState>`; Composable stateless nhận `state` + lambda → test được.
- Giao diện gọn: không có đoạn mô tả giải thích dài (bỏ mô tả chế độ, lời dẫn màn Gestures, thẻ onboarding,
  ghi chú R2, summary của các switch hiển nhiên).
- Chuỗi trong `strings.xml` (en) và `values-vi/strings.xml`.

| ID | Tiêu chí |
|---|---|
| UI-40 | Top bar pinned, tiêu đề = tên màn hình bên trái; Home/Gestures/Notch: icon Settings bên phải; Cài đặt ứng dụng/Picker: Back bên trái |
| UI-41 | Bottom bar có Home, Gestures, Notch theo thứ tự; ẩn khi ở Cài đặt ứng dụng / Action picker |

## 2. Highlight vùng notch trực tiếp (FR-U8)
- Khi app đang mở (mọi màn hình, hướng dọc), vùng chạm hiện tại được tô trực tiếp trên màn hình tại đúng vị
  trí notch: nền `primary` alpha 35% + viền `primary`, bo tròn. Ở Shizuku mode và Accessibility mode như nhau.
- Màn Notch: khi kéo slider vùng chạm (vị trí ngang/dọc, rộng, cao, padding), highlight
  cập nhật **ngay trong lúc kéo** theo giá trị nháp (`TouchArea.regionFor(draft)`), không cần lưu.
- Bỏ nút / dialog "Preview touch area" và overlay preview qua accessibility.

## 3. Màn hình

### Home (FR-U1, FR-D4, FR-D7)
```
┌──────────────────────────────────┐
│ Home                           ⚙ │  ← top bar tĩnh
│ ┌──────────────────────────────┐ │
│ │ ● Notch gestures       [ ON ]│ │
│ │   Status text (EngineStatus) │ │
│ └──────────────────────────────┘ │
│ Detection mode                   │
│ [ Accessibility | Shizuku ]      │
│ Requirements                     │
│ ✓/✗ Shizuku running / permission │  (Shizuku mode)
│ ✓/✗ Accessibility                │  (Accessibility mode)
│ ✓/✗ Notifications                │  (Shizuku mode, Android 13+)
│ ✓/✗ Media control                │  (khi có gesture gán media)
│ ✓/✗ Battery unrestricted         │  (khuyến nghị, cả 2 mode)
├──────────────────────────────────┤
│  Home  |  Gestures  |  Notch     │
└──────────────────────────────────┘
```
| ID | Tiêu chí |
|---|---|
| UI-1 | Switch tắt được khi thiếu quyền; bật khi thiếu quyền vẫn lưu `enabled=true` và hiển thị thẻ yêu cầu |
| UI-2 | Status text ánh xạ 1-1 từ `EngineStatus` |
| UI-3 | Thẻ "Media control" chỉ hiện khi có binding **hiệu lực** (`effectiveAction`) là action media |
| UI-4 | Thẻ "Battery" luôn hiện (cả 2 mode), satisfied = `Requirements.batteryUnrestricted` |

### Gestures (FR-U2, FR-G8…G10)
Các cử chỉ chia nhóm, mỗi nhóm một `SectionHeader` + `Card`:

| Nhóm | Cử chỉ |
|---|---|
| Chạm (`TAP_GROUP`) | Tap, Double tap |
| Nhấn giữ (`PRESS`) | Long press |
| Vuốt (`SWIPE`) | Swipe left, Swipe right, Swipe down |
| Vuốt và giữ (`SWIPE_HOLD`) | Swipe left & hold, Swipe right & hold, Swipe down & hold |

- Mỗi dòng: icon cử chỉ, tên, hành động đang gán; chạm → mở **Action picker**.
- Cử chỉ giữ (Long press, `*_HOLD`) có nút bật/tắt **Lặp lại** (icon Repeat) ở cuối dòng; tắt được khi
  hành động là `None`.
- Shizuku mode: Swipe down, Swipe down & hold bị vô hiệu (mờ, không bấm được) với nhãn "Không khả dụng ở chế độ Shizuku".
- Không có nút Reset gestures (FR-S9).

| ID | Tiêu chí |
|---|---|
| UI-12 | LaunchApp hiển thị tên app (đã gỡ: package + "Not installed"); LaunchShortcut hiển thị "<app>: <nhãn>" (đã gỡ app: thêm "Not installed") |
| UI-13 | `GesturesUiState.groups` đúng 4 nhóm theo bảng trên, đúng thứ tự |
| UI-14 | Mode SHIZUKU → dòng `SWIPE_DOWN`, `SWIPE_DOWN_HOLD` có `enabled=false`; mode ACCESSIBILITY → mọi dòng `enabled=true` |
| UI-15 | Chỉ dòng thuộc `HoldGestures` có `repeatable=true`; `setRepeat(g, on)` cập nhật `repeatOnHold` |

### Action picker (FR-U9, FR-A11, FR-A12)
Màn hình đầy đủ `ActionPickerDestination(gesture)`, tiêu đề = tên cử chỉ, có `PrimaryTabRow` 3 tab:

| Tab | Nội dung |
|---|---|
| Hệ thống & media | Nhóm System (None, Back, Home, Recents, Lock, Screenshot, Notifications, Quick settings, Assistant, Torch) và Media (Play/pause, Previous, Next, Volume up/down). Hành động không chạy được ở mode hiện tại vẫn chọn được, có nhãn cảnh báo |
| Ứng dụng | Ô tìm kiếm + danh sách app (icon, tên) |
| Shortcut | Ô tìm kiếm + shortcut tĩnh nhóm theo app (header: icon + tên app) |

Mục đang chọn có radio được chọn. Chọn → lưu binding và quay lại.

| ID | Tiêu chí |
|---|---|
| UI-10 | Chọn hành động (bất kỳ tab) → `SettingsRepository.update` với binding mới, `finished=true` |
| UI-11 | Tab Ứng dụng lọc không phân biệt hoa thường theo tên/package, sắp xếp theo tên |
| UI-20 | Tab ban đầu = tab chứa hành động hiện tại (LaunchApp → Ứng dụng, LaunchShortcut → Shortcut, còn lại → Hệ thống & media) |
| UI-21 | Tab Shortcut dùng `groupShortcuts` (SH-1, SH-2); chọn → `LaunchShortcut(pkg, id, label, intentUri)` |
| UI-22 | App và shortcut chỉ được tải lần đầu mở tab tương ứng (hoặc tab ban đầu) |

### Notch (FR-U12, FR-S9, FR-R5, FR-R6) — tab thứ 3
Các section, theo thứ tự:
1. **Độ nhạy**: Double-tap window, Long-press duration, Swipe distance, Repeat interval.
2. **Vị trí**: Vị trí ngang (%, 0–100), Vị trí dọc (dp, 0–100, tính từ cạnh trên).
3. **Kích thước**: Rộng (dp, 8–400), Cao (dp, 8–100), Mở rộng vùng chạm / padding (dp, 0–48).
4. **Phản hồi**: Haptic feedback (switch).

- Không có switch "Custom touch area": slider vị trí/kích thước **luôn hiển thị**. Giá trị hiển thị
  `area = manualRegion ?: detectedRegion ?: ManualRegionDp()` (`detectedRegion` = vùng tự phát hiện, notch-region NR-12).
  Kéo một slider → lưu `manualRegion = area.copy(<field> = giá trị mới)`.
- Chỉ **slider** có nút reset (icon ↺), đặt **bên phải thanh slider**, chỉ bật khi khác mặc định. Switch không có reset.
  Mặc định của slider vùng chạm = giá trị của `detectedRegion`; reset field cuối cùng khác mặc định → `manualRegion = null`
  (quay về tự phát hiện).
- Highlight trực tiếp (§2).

| ID | Tiêu chí |
|---|---|
| UI-30 | `NotchSetting` gồm đúng các slider: DOUBLE_TAP, LONG_PRESS, SWIPE_DISTANCE, REPEAT_INTERVAL, AREA_X, AREA_Y, AREA_WIDTH, AREA_HEIGHT, PADDING. `reset(item, s, auto)` đặt đúng field đó về mặc định (`Settings()` hoặc `auto` cho vùng chạm), field khác giữ nguyên; `isDefault(item, s, auto)` đúng khi bằng mặc định |
| UI-31 | Reset slider vùng chạm khi mọi field khác đã bằng `auto` → `manualRegion = null`; khi `manualRegion = null` mọi slider vùng chạm `isDefault` |
| UI-33 | `NotchSettingsUiState.area` = `manualRegion ?: detectedRegion ?: ManualRegionDp()`; `setArea(region)` lưu `manualRegion` (mọi slider, kể cả vị trí dọc) |
| UI-34 | Setter độ nhạy / padding / haptics lưu đúng field (kẹp bởi repository) |

### Cài đặt ứng dụng (FR-U12, FR-U14, FR-S6…FR-S8) — mở từ icon Settings bên phải top bar
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
