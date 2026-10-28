# Spec: Giao diện (Material 3)

Yêu cầu: FR-U1…FR-U6 · Module: `:app` · Package: `io.github.ptt98hd.totch.ui`

## 1. Nguyên tắc
- Jetpack Compose + Material 3, `dynamicLightColorScheme/dynamicDarkColorScheme`, edge-to-edge.
- Một Activity (`MainActivity`), Navigation Compose, `NavigationBar` dưới cùng với 3 tab:
  **Home**, **Gestures**, **Settings**. About mở từ icon trên `TopAppBar` của Settings.
- Mỗi màn hình có `ViewModel` riêng, expose `StateFlow<UiState>`; Composable stateless nhận `state` + lambda → test được.
- Chuỗi trong `strings.xml` (en) và `values-vi/strings.xml`.

## 2. Màn hình

### Home (FR-U1, FR-D4)
```
┌──────────────────────────────────┐
│ Totch                            │
│ ┌──────────────────────────────┐ │
│ │ ● Gestures active      [ ON ]│ │  ← Card lớn, màu primaryContainer khi Running
│ │   Status text (EngineStatus) │ │
│ └──────────────────────────────┘ │
│ Detection mode                   │
│ [ Accessibility | Shizuku ]      │  ← SingleChoiceSegmentedButtonRow
│ ┌ Requirement cards ───────────┐ │
│ │ ✓/✗ Shizuku running   [Open] │ │  (chỉ hiện ở Shizuku mode)
│ │ ✓/✗ Shizuku permission[Grant]│ │
│ │ ✓/✗ Accessibility     [Open] │ │  (chỉ hiện ở Accessibility mode)
│ │ ✓/✗ Notifications     [Grant]│ │  (Shizuku mode, Android 13+)
│ │ ✓/✗ Media control     [Open] │ │  (cả 2 mode, khi có gesture gán Play/Pause/Next/Previous)
│ └──────────────────────────────┘ │
│ ⓘ Note về R2 khi ở Shizuku mode  │
└──────────────────────────────────┘
```
| ID | Tiêu chí |
|---|---|
| UI-1 | Switch tắt được khi thiếu quyền; bật khi thiếu quyền vẫn lưu `enabled=true` và hiển thị thẻ yêu cầu |
| UI-2 | Status text ánh xạ 1-1 từ `EngineStatus` |
| UI-3 | Thẻ "Media control" (khuyến nghị) chỉ hiện khi có binding là action media; nút mở trang Notification access chi tiết của `MediaListenerService` |

### Gestures (FR-U2, FR-U3)
- `LazyColumn` 6 `ListItem`: icon cử chỉ, tên cử chỉ, `supportingContent` = tên hành động đang gán.
- Chạm → `ModalBottomSheet` liệt kê hành động theo nhóm (Hệ thống / Media / Ứng dụng); hành động
  không khả dụng ở mode hiện tại vẫn chọn được nhưng có nhãn cảnh báo.
- "Open app…" → màn hình App picker: `SearchBar` + danh sách (icon, tên); chọn → lưu `LaunchApp(pkg)`.

| ID | Tiêu chí |
|---|---|
| UI-10 | Chọn hành động → `SettingsRepository.update` với binding mới, sheet đóng |
| UI-11 | App picker lọc không phân biệt hoa thường, sắp xếp theo tên |
| UI-12 | LaunchApp hiển thị tên app (nếu app đã gỡ: hiển thị package + nhãn "Not installed") |

### Settings (FR-U4)
- Slider: Double-tap window (ms), Long-press duration (ms), Swipe distance (dp), Touch area padding (dp).
- Switch: Haptic feedback.
- Switch "Custom touch area" → hiện 3 slider: vị trí ngang (%), rộng (dp), cao (dp).
- Nút "Preview touch area" → hiển thị vùng notch tô màu `primary` 40% alpha trong 3 giây
  (Accessibility mode dùng overlay; Shizuku mode: hiển thị hình vẽ minh họa trong app).
- Nút "Reset to defaults" (có dialog xác nhận).

### About (FR-U5)
- Tên, phiên bản (`BuildConfig.VERSION_NAME`), license GPL-3.0-or-later, link mã nguồn GitHub, credit Shizuku.

## 3. Onboarding lần đầu (khuyến nghị, nằm trong Home)
Nếu chưa có quyền nào: card giải thích ngắn 2 mode + nút chọn.
