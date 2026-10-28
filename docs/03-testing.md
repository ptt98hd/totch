# 03 — Chiến lược kiểm thử (TDD)

## 1. Quy trình Spec → Test → Code

1. **Spec**: mọi hành vi mới được viết vào `docs/specs/*.md` với ID tiêu chí (vd. `GR-7`).
2. **Red**: viết test JVM tham chiếu ID trong tên test, ví dụ
   ``@Test fun `GR-7 long press is emitted on late UP`()``. Chạy → phải fail.
3. **Green**: viết code tối thiểu để pass.
4. **Refactor**: dọn dẹp khi test vẫn xanh.
5. Spec thay đổi → sửa spec trước, rồi test, rồi code. Không có code hành vi nào thiếu test/ID.

## 2. Tầng kiểm thử

| Tầng                                              | Vị trí          | Công cụ                           | Chạy                               |
| ------------------------------------------------- | --------------- | --------------------------------- | ---------------------------------- |
| Unit lõi                                          | `core/src/test` | JUnit 4, kotlin-test              | `./gradlew :core:test`             |
| Unit app (ViewModel, Engine, Repository với fake) | `app/src/test`  | JUnit 4, coroutines-test, Turbine | `./gradlew :app:testDebugUnitTest` |
| Thủ công trên thiết bị                            | Checklist §4    | Máy thật có notch + Shizuku       | trước mỗi release                  |

Mục tiêu độ phủ: `:core` ≥ 90% dòng (NFR-6).

## 3. Ma trận truy vết

| Spec                               | Test class                                                                              |
| ---------------------------------- | --------------------------------------------------------------------------------------- |
| gesture-recognition (GR-\*)        | `core/.../gesture/GestureRecognizerTest`                                                |
| getevent-input (GD-_, GP-_, CM-\*) | `core/.../input/GeteventDeviceParserTest`, `GeteventParserTest`, `CoordinateMapperTest` |
| notch-region (NR-\*)               | `core/.../region/NotchRegionResolverTest`                                               |
| actions (AD-_, SC-_)               | `core/.../action/ActionDispatcherTest`, `ShellCommandsTest`                             |
| settings (ST-\*)                   | `core/.../settings/SettingsTest`, `SettingsCodecTest`                                   |
| detection-modes (EN-\*)            | `app/.../engine/GestureEngineTest`                                                      |
| ui (UI-1…UI-3)                     | `app/.../ui/home/HomeViewModelTest`                                                     |
| actions §5 (MC-\*)                 | `app/.../action/MediaSessionsTest`                                                      |
| actions §7 (SH-\*)                 | `app/.../ui/picker/ShortcutGroupingTest`                                                |
| ui (UI-10, UI-11, UI-20…UI-22)     | `app/.../ui/picker/ActionPickerViewModelTest`                                           |
| ui (UI-12…UI-16)                   | `app/.../ui/gestures/GesturesViewModelTest`                                             |
| ui (UI-30…UI-32)                   | `app/.../ui/settings/SettingsViewModelTest`                                             |
| ui (TH-2)                          | `app/.../ui/theme/AmoledTest`                                                           |
| detection-modes (DM-22)            | `app/.../service/BootActionTest`                                                        |

## 4. Checklist thủ công trước release

Checklist chi tiết (≈ 90 kịch bản, có ID `MT-*` và lịch sử kết quả): [05-manual-test-checklist.md](05-manual-test-checklist.md).

## 5. CI

GitHub Actions (`.github/workflows/ci.yml`): `./gradlew test assembleRelease lint` trên mỗi push/PR.
