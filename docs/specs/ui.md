# Spec: Giao diện (Material 3)

Yêu cầu: FR-U1…FR-U11, FR-S5…FR-S8 · Module: `:app` · Package: `io.github.ptt98hd.totch.ui`

## 1. Nguyên tắc
- Jetpack Compose + Material 3, `dynamicLightColorScheme/dynamicDarkColorScheme`, edge-to-edge.
- Một Activity (`MainActivity`), Navigation Compose.
- **Top bar tĩnh** (`TopAppBar` pinned, không co giãn khi cuộn) trên mọi màn hình, tiêu đề = tên màn hình hiện tại.
  - Home, Gestures: icon **Settings ở bên trái** (navigation icon) → mở Settings.
  - Settings, Action picker: nút **Back** ở bên trái.
  - Hành động riêng của màn hình nằm bên phải (vd. Reset).
- `NavigationBar` dưới cùng chỉ có **Home** và **Gestures**; ẩn ở Settings / Action picker.
- Mỗi màn hình có `ViewModel` riêng, expose `StateFlow<UiState>`; Composable stateless nhận `state` + lambda → test được.
- Giao diện gọn: không có đoạn mô tả giải thích dài (bỏ mô tả chế độ, lời dẫn màn Gestures, thẻ onboarding,
  ghi chú R2, summary của các switch hiển nhiên).
- Chuỗi trong `strings.xml` (en) và `values-vi/strings.xml`.

| ID | Tiêu chí |
|---|---|
| UI-40 | Top bar pinned, tiêu đề = tên màn hình; Home/Gestures: icon Settings bên trái; Settings/Picker: Back bên trái |
| UI-41 | Bottom bar chỉ có Home, Gestures; ẩn khi ở Settings / Action picker |

## 2. Highlight vùng notch trực tiếp (FR-U8)
- Khi app đang mở (mọi màn hình, hướng dọc), vùng chạm hiện tại được tô trực tiếp trên màn hình tại đúng vị
  trí notch: nền `primary` alpha 35% + viền `primary`, bo tròn. Ở Shizuku mode và Accessibility mode như nhau.
- Settings → nhóm Touch area: khi kéo slider (padding, vị trí, rộng, cao) hoặc bật/tắt custom area, highlight
  cập nhật **ngay trong lúc kéo** theo giá trị nháp (`TouchArea.regionFor(draft)`), không cần lưu.
- Bỏ nút / dialog "Preview touch area" và overlay preview qua accessibility.

## 3. Màn hình

### Home (FR-U1, FR-D4, FR-D7)
```
┌──────────────────────────────────┐
│ ⚙  Home                          │  ← top bar tĩnh
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
│  Home  |  Gestures               │
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
- Top bar có nút **Reset gestures** (dialog xác nhận) → `withDefaultGestures()`.

| ID | Tiêu chí |
|---|---|
| UI-12 | LaunchApp hiển thị tên app (đã gỡ: package + "Not installed"); LaunchShortcut hiển thị "<app>: <nhãn>" (đã gỡ app: thêm "Not installed") |
| UI-13 | `GesturesUiState.groups` đúng 4 nhóm theo bảng trên, đúng thứ tự |
| UI-14 | Mode SHIZUKU → dòng `SWIPE_DOWN`, `SWIPE_DOWN_HOLD` có `enabled=false`; mode ACCESSIBILITY → mọi dòng `enabled=true` |
| UI-15 | Chỉ dòng thuộc `HoldGestures` có `repeatable=true`; `setRepeat(g, on)` cập nhật `repeatOnHold` |
| UI-16 | `requestResetGestures` → dialog; xác nhận → `withDefaultGestures()` (cài đặt khác giữ nguyên); hủy → không đổi |

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

### Settings (FR-U4, FR-U5, FR-S5…FR-S8)
Các section, theo thứ tự:
1. **Độ nhạy**: Double-tap window, Long-press duration, Swipe distance, Repeat interval.
2. **Vùng chạm**: Padding, switch Custom touch area → vị trí ngang (%), rộng (dp), cao (dp). Highlight trực tiếp (§2).
3. **Phản hồi**: Haptic feedback.
4. **Giao diện**: Theme (System / Light / Dark — segmented), AMOLED black (switch), Ngôn ngữ (System / English / Tiếng Việt — dialog chọn).
5. **Hệ thống**: Start on boot.
6. **Giới thiệu** (cuối cùng): phiên bản, license, mã nguồn, credit Shizuku.

- Mỗi mục có nút **reset riêng** (icon ↺) chỉ bật khi giá trị khác mặc định.
- Top bar có nút **Reset settings** (dialog xác nhận) → `withDefaultPreferences()`: **không** đụng tới gestures.

| ID | Tiêu chí |
|---|---|
| UI-30 | `reset(item)` đặt đúng field đó về giá trị của `Settings()`; field khác giữ nguyên. `isDefault(item)` đúng khi bằng mặc định |
| UI-31 | Reset tổng → `withDefaultPreferences()`; bindings và `repeatOnHold` giữ nguyên |
| UI-32 | Setter theme / amoled / language / startOnBoot / repeatInterval lưu đúng field |

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
