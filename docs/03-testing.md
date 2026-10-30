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
| ui (UI-12…UI-15)                   | `app/.../ui/gestures/GesturesViewModelTest`                                             |
| ui (UI-30, UI-31, UI-33, UI-34)    | `app/.../ui/notch/NotchSettingsViewModelTest`                                           |
| ui (UI-32)                         | `app/.../ui/settings/AppSettingsViewModelTest`                                          |
| notch-region (NR-12…NR-14)         | `app/.../engine/RegionTest`                                                             |
| ui (TH-2)                          | `app/.../ui/theme/AmoledTest`                                                           |
| detection-modes (DM-22)            | `app/.../service/BootActionTest`                                                        |
| actions §9 (QT-\*), ui (UI-28)     | `app/.../ui/picker/TileFilterTest`, `ActionPickerViewModelTest`                         |
| update (UP-1…UP-4)                 | `core/.../update/ReleasesTest`                                                          |
| update (UP-5…UP-7)                 | `app/.../ui/settings/AppSettingsViewModelTest`                                          |
| update (UP-9, UP-13)               | `app/.../update/UpdateControllerTest`, `app/.../ui/settings/AppSettingsViewModelTest`   |
| update (UP-10…UP-12)               | `core/.../update/UpdatePolicyTest`                                                      |
| actions §9 (QT-2), §10 (SC-9, OH-1…OH-4) | `core/.../action/ShellCommandsTest`, `OneHandedTest`                              |
| actions §10 (OH-5)                 | `app/.../shizuku/OneHandedRunnerTest`                                                   |
| detection-modes §6 (PC-6, PC-7)    | `core/.../action/PermissionCommandsTest`                                                |
| ui (UI-51, UI-53)                  | `app/.../ui/home/HomeViewModelTest`                                                     |
| actions §11–§13 (AD-12, SC-10…SC-12, SM-1…SM-3) | `core/.../action/ActionDispatcherTest`, `ShellCommandsTest`, `SoundModesTest` |
| settings (ST-29)                   | `core/.../settings/SettingsCodecTest`                                                   |
| detection-modes §6 (PC-8)          | `core/.../action/PermissionCommandsTest`                                                |
| ui (UI-23, UI-24)                  | `app/.../ui/common/ActionCatalogTest`                                                   |
| ui (UI-29)                         | `app/.../ui/picker/ActionPickerViewModelTest`                                           |
| update (UP-14, UP-15)              | `core/.../update/UpdateCommandsTest`                                                    |
| update (UP-16, UP-17)              | `app/.../update/UpdateControllerTest`, `app/.../ui/settings/AppSettingsViewModelTest`   |
| actions §12 (SM-1…SM-3), §15–§17 (AD-13, SP-1, SP-2, SC-13…SC-15, IN-1…IN-5) | `core/.../action/SoundModesTest`, `ActionDispatcherTest`, `SettingsPagesTest`, `ShellCommandsTest`, `IntentFiltersTest` |
| settings (ST-35, ST-36)            | `core/.../settings/SettingsCodecTest`                                                   |
| actions (SP-3, IN-6, CT-1), ui (UI-73…UI-77) | `app/.../ui/picker/ActionPickerViewModelTest`, `IntentGroupingTest`, `ContactFilterTest`, `app/.../ui/common/ActionCatalogTest`, `StringsTest` |
| notch-overlay (OV-\*, NI-\*, IS-\*, IG-\*, TE-\*), settings (ST-37, ST-38) | `core/.../overlay/OverlayVisibilityTest`, `NotificationIndicatorTest`, `IslandControllerTest`, `IslandLayoutTest`, `TouchEffectTest`, `core/.../settings/SettingsCodecTest` |
| notch-overlay (EN-16…EN-18), ui (UI-78…UI-81) | `app/.../engine/GestureEngineTest`, `app/.../ui/notch/NotchSettingsViewModelTest`, `app/.../ui/common/StringsTest` |

## 4. Checklist thủ công trước release

Checklist chi tiết (≈ 90 kịch bản, có ID `MT-*` và lịch sử kết quả): [05-manual-test-checklist.md](05-manual-test-checklist.md).

## 5. CI

GitHub Actions (`.github/workflows/ci.yml`): `./gradlew test assembleRelease lint` trên mỗi push/PR.
