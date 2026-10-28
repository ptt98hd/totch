# 03 — Chiến lược kiểm thử (TDD)

## 1. Quy trình Spec → Test → Code
1. **Spec**: mọi hành vi mới được viết vào `docs/specs/*.md` với ID tiêu chí (vd. `GR-7`).
2. **Red**: viết test JVM tham chiếu ID trong tên test, ví dụ
   ``@Test fun `GR-7 long press is emitted on late UP`()``. Chạy → phải fail.
3. **Green**: viết code tối thiểu để pass.
4. **Refactor**: dọn dẹp khi test vẫn xanh.
5. Spec thay đổi → sửa spec trước, rồi test, rồi code. Không có code hành vi nào thiếu test/ID.

## 2. Tầng kiểm thử

| Tầng | Vị trí | Công cụ | Chạy |
|---|---|---|---|
| Unit lõi | `core/src/test` | JUnit 4, kotlin-test | `./gradlew :core:test` |
| Unit app (ViewModel, Engine, Repository với fake) | `app/src/test` | JUnit 4, coroutines-test, Turbine | `./gradlew :app:testDebugUnitTest` |
| Thủ công trên thiết bị | Checklist §4 | Máy thật có notch + Shizuku | trước mỗi release |

Mục tiêu độ phủ: `:core` ≥ 90% dòng (NFR-6).

## 3. Ma trận truy vết

| Spec | Test class |
|---|---|
| gesture-recognition (GR-*) | `core/.../gesture/GestureRecognizerTest` |
| getevent-input (GD-*, GP-*, CM-*) | `core/.../input/GeteventDeviceParserTest`, `GeteventParserTest`, `CoordinateMapperTest` |
| notch-region (NR-*) | `core/.../region/NotchRegionResolverTest` |
| actions (AD-*, SC-*) | `core/.../action/ActionDispatcherTest`, `ShellCommandsTest` |
| settings (ST-*) | `core/.../settings/SettingsTest`, `SettingsCodecTest` |
| detection-modes (EN-*) | `app/.../engine/GestureEngineTest` |
| ui (UI-10, UI-11) | `app/.../ui/gestures/GesturesViewModelTest`, `AppPickerViewModelTest` |

## 4. Checklist thủ công trước release
- [ ] Accessibility mode: 6 cử chỉ đều kích hoạt đúng hành động trên máy có notch.
- [ ] Shizuku mode: như trên; tắt Shizuku giữa chừng → app không crash, trạng thái "Waiting".
- [ ] Xoay ngang → tạm dừng; xoay dọc → chạy lại.
- [ ] Khởi động lại máy → service tự chạy lại.
- [ ] Đèn pin bật/tắt; media điều khiển được Spotify/YouTube Music/VLC.
- [ ] Mở app đã chọn khi đang ở app khác.
- [ ] Dark mode, tiếng Việt hiển thị đúng.

## 5. CI
GitHub Actions (`.github/workflows/ci.yml`): `./gradlew test assembleRelease lint` trên mỗi push/PR.
