# Spec: Lớp hiển thị quanh notch (v1.9 — FR-O1…FR-O8; v1.9.1 — FR-O9…FR-O11, FR-U63)

Yêu cầu: FR-O1…FR-O8 ([01 §22](../01-requirements.md)) · Module: `:core` (`io.github.ptt98hd.totch.core.overlay`: logic
thuần) + `:app` (`overlay/`: overlay, nguồn dữ liệu Android)

Ba tính năng vẽ **quanh notch, trên status bar**: viền báo thông báo (notification indicator), **Dynamic Island** và
**hiệu ứng chạm**. Mỗi tính năng có công tắc riêng, mặc định **tắt**.

## 1. Điều kiện hiển thị (OV-*)

Chỉ overlay `TYPE_ACCESSIBILITY_OVERLAY` của `NotchAccessibilityService` vẽ được **trên** status bar (overlay
`TYPE_APPLICATION_OVERLAY` nằm dưới) → cả ba tính năng **cần Trợ năng**. Viền thông báo, island media và island thông
báo còn cần **quyền truy cập thông báo** (`MediaListenerService`, dòng Home › Permissions đổi tên thành
**Media & notifications** / **Media và thông báo**, FR-O7).

`overlayVisible(settings, accessibility, rotation, statusBarVisible): Boolean` (`:core`):

| ID | Given | Then |
|---|---|---|
| OV-1 | `enabled = false` (công tắc Totch tắt) | `false` |
| OV-2 | Trợ năng chưa kết nối | `false` |
| OV-3 | `rotation ≠ 0` | `false` (vùng notch chỉ có ở hướng dọc, như highlight) |
| OV-4 | status bar đang ẩn (app toàn màn hình: video, game) | `false` — không đè lên nội dung toàn màn hình |
| OV-5 | `enabled`, Trợ năng, `rotation = 0`, status bar hiện, và **ít nhất một** trong `island`, `touchEffects` bật (v1.9.1: bỏ `notificationIndicator`, FR-O12) | `true`; cả hai tắt → `false` |

Hình học dùng **vùng notch không padding** (`HighlightArea.notch`, notch-region §FR-R7) — vùng người dùng đã căn bằng
slider Vị trí / Kích thước, nên viền và island bám đúng camera.

Cửa sổ:
- **Cửa sổ vẽ**: rộng toàn màn hình, từ cạnh trên, cao = `IslandLayout.EXPANDED` bottom + 24 dp (chỗ cho glow),
  `FLAG_NOT_TOUCHABLE` (không nhận chạm), cutout `ALWAYS`. Nội dung vẽ bằng Compose.
- **Cửa sổ chạm** (`NotchView`, detection-modes §8): rect = vùng chạm notch (có padding), khi island đang hiện nội dung
  **bấm được** (media, thông báo) thì mở rộng thành hợp của vùng chạm và rect island. Chạm bắt đầu trong vùng chạm notch →
  cử chỉ như cũ (chuyển cho engine khi nguồn chạm = Trợ năng); bắt đầu ngoài → chạm vào island (§3.4). Khi nguồn chạm là
  Shizuku và không cần chặn chạm, cửa sổ chỉ tồn tại lúc island bấm được (chạm tại notch lúc đó bị nuốt, nhưng cử chỉ vẫn
  nhận qua `getevent`).

**Không lưu ảnh** (v1.9.1, FR-O9). Bản 1.9.0 để sót icon / viên đen / viền tại notch sau khi island hoặc hiệu ứng chạm
kết thúc: nội dung island đặt bằng `offset` nằm **ngoài** bounds của layer `AnimatedContent` (layer nhỏ ở góc trái trên),
nên khi layer mờ dần, HWUI chỉ vẽ lại vùng bounds (damage) → điểm ảnh cũ trên buffer của cửa sổ trong suốt còn nguyên.
Quy tắc:
- Mọi thứ trong cửa sổ vẽ nằm **trong** bounds của node chứa nó: `AnimatedContent` và lớp chứa nội dung island
  `fillMaxSize()`.
- Khi lớp vẽ chuyển sang **rảnh** (không island, không vòng sáng, không viền thông báo, không hiệu ứng đang chạy), sau
  frame cuối cửa sổ được `invalidate()` **toàn bộ** một lần → buffer được xóa sạch.
- v1.9.2: animation của một hiệu ứng **không** chạy trong effect bị khởi động lại bởi chính chạm đó. Bản 1.9.0–1.9.1
  chạy gợn sóng trong `LaunchedEffect(press)`: nhả tay (`press = null`) khởi động lại effect, hủy animation trước khi
  gợn sóng tự gỡ khỏi danh sách → vòng tròn đứng yên mãi tại điểm chạm. Gợn sóng chạy trong scope của composition
  (`rememberCoroutineScope`) và luôn gỡ trong `finally`.

## 2. Viền báo thông báo (FR-O1, NI-*)

v1.9.1 (FR-O12): không còn công tắc riêng — viền chạy khi **island bật và có hoạt động `NOTIFICATIONS`**
(`IslandActivity.NOTIFICATIONS in activeIslandActivities()`). Thông báo mới → island Thông báo (§3) bung ra rồi thu
lại, vòng sáng ở lại quanh notch và nhịp thở tới khi xem.

Còn thông báo **chưa xem** → viền quanh notch phát sáng, **nhịp thở** (alpha 0.35 ↔ 1, chu kỳ 2 s), màu theo app của
thông báo mới nhất (màu nhấn `Notification.color`), không có màu → màu nhấn Monet (`system_accent1_200`). Tắt khi người
dùng **xem** (mở thanh thông báo) hoặc khi mọi thông báo chưa xem bị gỡ.

**Chỉ đọc metadata** (FR-O7): key, gói, thời điểm, cờ (`ongoing`, `clearable`, nhóm, `onlyAlertOnce`), mức quan trọng
(`Ranking.importance`) và màu. **Không** đọc tiêu đề, nội dung, `extras`. Không lưu, không gửi đi.

```kotlin
data class NotificationMeta(
    val key: String, val packageName: String, val postTimeMs: Long,
    val ongoing: Boolean, val clearable: Boolean, val groupSummary: Boolean,
    val alerting: Boolean,        // importance ≥ IMPORTANCE_DEFAULT (không phải thông báo im lặng)
    val onlyAlertOnce: Boolean,
    val color: Int?,              // null = COLOR_DEFAULT
)
data class IndicatorState(val count: Int, val color: Int?)

class NotificationIndicator(ownPackage: String) {
    val state: IndicatorState?                        // null = không sáng
    fun reset(active: List<NotificationMeta>)         // listener vừa kết nối
    fun onPosted(meta: NotificationMeta): Boolean     // mới hoặc cập nhật (cùng key); true = thông báo này "báo" (NI-9)
    fun onRemoved(key: String)
    fun onSeen()                                      // đã xem thanh thông báo
}
```

"Đã xem" (`onSeen`): `NotchAccessibilityService` nhận `TYPE_WINDOW_STATE_CHANGED` **chỉ của `com.android.systemui`**
(thanh thông báo / Cài đặt nhanh mở ra; `packageNames` lọc sẵn trong cấu hình, vẫn `canRetrieveWindowContent=false`),
hoặc Totch chạy hành động Thông báo / Cài đặt nhanh, hoặc người dùng bấm island thông báo.

| ID | Given | Then |
|---|---|---|
| NI-1 | thông báo `ongoing`, hoặc không `clearable`, hoặc là group summary, hoặc không `alerting`, hoặc của chính Totch | bỏ qua, `state` không đổi |
| NI-2 | `onPosted` một thông báo hợp lệ mới | `state = IndicatorState(1, color)` |
| NI-3 | cập nhật (cùng key) sau khi đã xem: `onlyAlertOnce = true` → vẫn đã xem; `false` → chưa xem lại | |
| NI-4 | `onRemoved(key)` | không còn đếm; hết thông báo chưa xem → `null` |
| NI-5 | `onSeen()` | `null`; thông báo mới sau đó → sáng lại |
| NI-6 | nhiều thông báo chưa xem | `count` = số lượng, `color` = màu của thông báo có `postTimeMs` lớn nhất (`null` nếu nó không có màu) |
| NI-7 | `reset(active)` | `null`; các thông báo đang có coi như đã xem (không sáng vì thông báo cũ khi khởi động); `onRemoved` của chúng không lỗi |
| NI-8 | thông báo hợp lệ bị cập nhật thành không hợp lệ (vd. `ongoing`) | bỏ khỏi danh sách chưa xem |
| NI-9 | giá trị trả về của `onPosted` | `true` khi thông báo hợp lệ và là key mới hoặc `onlyAlertOnce = false` (kể cả khi đang chưa xem — tin nhắn mới cùng cuộc trò chuyện); ngược lại `false`. `true` → `IslandEvent.Notification` |

## 3. Dynamic Island (FR-O2…FR-O5)

Viên thuốc màu **đen** bao quanh notch, giãn ra hai bên để hiện hoạt động. Không có hoạt động → co về đúng vùng notch và
ẩn. Đổi trạng thái bằng animation lò xo (spring, `dampingRatio` 0.7).

### 3.1 Hoạt động

```kotlin
@Serializable enum class IslandActivity { MEDIA, NOTIFICATIONS, CHARGING, SOUND }   // JSON "media", "notifications", "charging", "sound"

data class MediaInfo(val packageName: String, val title: String?, val artist: String?, val playing: Boolean)
enum class SoundMode { RING, VIBRATE, SILENT, DND_ON, DND_OFF }

sealed interface IslandEvent {
    data class Charging(val level: Int) : IslandEvent              // vừa cắm sạc, level 0–100
    data class Notification(val packageName: String) : IslandEvent // thông báo hợp lệ mới (NI-1), chỉ biết gói
    data class Sound(val mode: SoundMode) : IslandEvent
    data class Volume(val fraction: Float) : IslandEvent           // âm lượng media / chuông thay đổi, 0–1
}

sealed interface IslandContent {
    data object Hidden
    data class Media(val info: MediaInfo, val expanded: Boolean)
    data class Charging(val level: Int)
    data class Notification(val packageName: String)
    data class Sound(val mode: SoundMode)
    data class Volume(val fraction: Float)
}
```

| Hoạt động | Nguồn (`:app`) | Thu gọn | Bấm |
|---|---|---|---|
| Media (FR-O2) | `MediaSessionManager` + `OnActiveSessionsChangedListener`, `MediaController.Callback` (phiên ưu tiên như MC-1…MC-3) | trái: ảnh bìa (bo tròn; không có → icon app), phải: sóng nhạc 3 vạch (đứng yên khi dừng) | mở rộng |
| Thông báo (FR-O3) | `MediaListenerService.onNotificationPosted` (chỉ metadata) | trái: icon app, phải: chấm màu của thông báo | mở thanh thông báo, đánh dấu đã xem |
| Sạc (FR-O4) | `ACTION_POWER_CONNECTED` + mức pin từ `BATTERY_CHANGED` (sticky) | trái: icon tia sét xanh lá, phải: `54%` | — |
| Âm thanh (FR-O5) | `RINGER_MODE_CHANGED_ACTION`, `ACTION_INTERRUPTION_FILTER_CHANGED`, `android.media.VOLUME_CHANGED_ACTION` (luồng media / chuông) | trái: icon chế độ (chuông / rung / im lặng / trăng), phải: nhãn ngắn; âm lượng: icon loa + thanh tiến độ | — |

Media mở rộng: island rộng gần hết màn hình, ảnh bìa 56 dp, tên bài + nghệ sĩ (một dòng, "…"), hàng nút
**Trước · Phát/Dừng · Tiếp** (điều khiển `MediaController.transportControls` của phiên đang hiện).

### 3.5 Chuyển động vòng sáng → giãn → thu (v1.9.1, FR-O11, IM-*)

Island (và nhịp **bung** của Báo thông báo) đi theo trình tự, **vòng sáng** (glow ring như §2, không nhịp thở) bao
quanh viên đen **suốt** từ đầu tới cuối:
1. `RING_IN` — vòng sáng hiện dần quanh **vùng notch** (viên = notch), `RING_IN_MS` = 220 ms.
2. `MORPH` — viên giãn ra hai bên tới hình đích (lò xo như §3); vòng sáng bám theo viên.
3. Hết nội dung: `MORPH` về notch (thu lại), rồi `RING_OUT` — vòng sáng tắt dần, `RING_OUT_MS` = 280 ms.

Đổi nội dung khi đang hiện → chỉ `MORPH` (vòng sáng giữ nguyên). Nội dung của viên (icon, chữ) hiện sau khi giãn và mờ
đi trước khi thu. Màu vòng sáng: island thông báo → màu của thông báo (như §2); sạc → xanh lá; khác → màu
nhấn Monet.

~~Nhịp bung của Báo thông báo (flare)~~ — bỏ ở FR-O12: viền là một phần của island Thông báo, island đã tự bung ra.

```kotlin
enum class IslandStep { RING_IN, MORPH, RING_OUT }
object IslandMotion {
    const val RING_IN_MS = 220; const val RING_OUT_MS = 280
    fun steps(ringLit: Boolean, shown: Boolean): List<IslandStep>          // ringLit: vòng sáng đang sáng đủ
    fun target(content: IslandContent, notch: Rect, m: IslandMetrics): Rect?  // null = không có gì để hiện
}
```

| ID | Given | Then |
|---|---|---|
| IM-1 | `steps(ringLit = false, shown = true)` | `[RING_IN, MORPH]` — vòng sáng trước, giãn sau |
| IM-2 | `steps(ringLit = true, shown = true)` | `[MORPH]` |
| IM-3 | `steps(_, shown = false)` | `[MORPH, RING_OUT]` — thu về notch trước, tắt vòng sáng sau |
| IM-4 | `target(content ≠ Hidden, …)` | `IslandLayout.pill(content, …)` |
| IM-5 | `target(Hidden, …)` | `null` |

### 3.2 `IslandController` (`:core`, IS-*)

Thuần, thời gian qua tham số như `GestureRecognizer` (một timer, NFR-3).

```kotlin
class IslandController(activities: Set<IslandActivity>) {
    var activities: Set<IslandActivity>          // đổi lúc chạy; ∅ = island tắt
    val content: IslandContent
    fun setMedia(media: MediaInfo?, nowMs: Long)
    fun onEvent(event: IslandEvent, nowMs: Long)
    fun onTap(nowMs: Long): IslandTap            // bấm vào thân island (ngoài nút điều khiển)
    fun onInteraction(nowMs: Long)               // bấm nút điều khiển media: giữ mở rộng
    fun onTick(nowMs: Long)
    fun nextDeadlineMs(): Long?
}
enum class IslandTap { NONE, EXPANDED, COLLAPSED, OPEN_NOTIFICATIONS }
```

Thời lượng: sạc 3000 ms, thông báo 3000 ms, chế độ âm 2000 ms, âm lượng 1500 ms, media mở rộng tự thu gọn sau 6000 ms
không tương tác, media dừng còn hiện 5000 ms (`MEDIA_LINGER_MS`).

| ID | Given | Then |
|---|---|---|
| IS-1 | không media, không sự kiện | `Hidden`; `nextDeadlineMs() = null` |
| IS-2 | `setMedia(playing = true)` | `Media(info, expanded = false)` |
| IS-3 | media chuyển sang dừng (`playing = false`) | vẫn `Media` (sóng nhạc đứng yên) tới `MEDIA_LINGER_MS` rồi `Hidden`; phát lại trước hạn → giữ, hủy hạn. `setMedia(paused)` khi trước đó chưa có media → `Hidden` ngay |
| IS-4 | `setMedia(null)` | về `Hidden` (hoặc sự kiện tạm đang hiện) |
| IS-5 | sự kiện tạm (sạc / thông báo / âm / âm lượng) | hiện nội dung tương ứng đúng thời lượng, rồi trở lại nội dung nền (media hoặc `Hidden`) |
| IS-6 | sự kiện tạm mới khi sự kiện tạm khác đang hiện | sự kiện mới thay thế, thời lượng tính lại từ lúc nhận |
| IS-7 | sự kiện tạm khi media đang mở rộng | sự kiện tạm hiện đè; hết hạn → media **thu gọn** |
| IS-8 | `onTap` khi `Media(expanded=false)` | `Media(expanded=true)`, trả `EXPANDED`; khi đang mở rộng → thu gọn, `COLLAPSED` |
| IS-9 | media mở rộng, 6000 ms không tương tác | thu gọn; `onInteraction` đặt lại mốc 6000 ms |
| IS-10 | `onTap` khi `Notification` | trả `OPEN_NOTIFICATIONS`, bỏ sự kiện tạm, về nội dung nền |
| IS-11 | `onTap` khi `Hidden`, `Charging`, `Sound`, `Volume` | `NONE`, không đổi |
| IS-12 | hoạt động không có trong `activities` | sự kiện của nó bị bỏ qua; `MEDIA` không có → media không hiện (đang hiện → `Hidden`); `activities = ∅` → luôn `Hidden` |
| IS-13 | `nextDeadlineMs()` | mốc sớm nhất trong: hết sự kiện tạm, tự thu gọn, hết `MEDIA_LINGER_MS`; `onTick` trước mốc không đổi gì |
| IS-14 | `Volume` liên tiếp | mỗi lần cập nhật `fraction` và gia hạn 1500 ms (không nhấp nháy) |

### 3.3 `IslandLayout` (`:core`, IG-*)

```kotlin
data class IslandMetrics(val screenWidthPx: Int, val density: Float)
object IslandLayout {
    fun pill(content: IslandContent, notch: Rect, m: IslandMetrics): Rect
    fun controls(pill: Rect, m: IslandMetrics): Map<IslandControl, Rect>   // chỉ cho media mở rộng
    fun hit(content: IslandContent, notch: Rect, m: IslandMetrics, x: Float, y: Float): IslandHit
}
enum class IslandControl { PREVIOUS, PLAY_PAUSE, NEXT }
sealed interface IslandHit { data object Outside; data object Body; data class Control(val control: IslandControl) }
```

`h = notch.height`, `cx = (notch.left + notch.right) / 2`, `dp(n) = round(n × density)`.

| ID | Given | Then |
|---|---|---|
| IG-1 | `Hidden` | `pill = notch` |
| IG-2 | nội dung thu gọn (media thu gọn, thông báo, sạc, âm lượng; `Sound` có nhãn chữ nên `wing = h + dp(40)`) | `top = notch.top`, `bottom = notch.bottom`, mỗi bên rộng thêm `wing = h + dp(16)`: `left = notch.left − wing`, `right = notch.right + wing`, rồi kẹp trong `[dp(8), screenWidthPx − dp(8)]` (giữ nguyên tâm khi không chạm mép) |
| IG-3 | media mở rộng | `(dp(12), notch.top, screenWidthPx − dp(12), notch.bottom + dp(136))` |
| IG-4 | `controls(pill)` | 3 ô vuông `dp(48)` nằm dưới cùng của pill (đáy cách `pill.bottom` `dp(12)`), giữa ô `PLAY_PAUSE` = `pill` tâm ngang, `PREVIOUS` / `NEXT` cách tâm `± dp(72)` |
| IG-5 | `hit` | trong một ô control (chỉ khi media mở rộng) → `Control`; trong `pill` (nội dung ≠ `Hidden`) → `Body`; còn lại → `Outside` |
| IG-6 | ví dụ: notch `(500,0,580,80)`, màn 1080, density 2 | thu gọn `(388,0,692,80)` (`Sound`: `(340,0,740,80)`); mở rộng `(24,0,1056,352)`; `PLAY_PAUSE = (492,232,588,328)`, `PREVIOUS = (348,232,444,328)` |

### 3.4 Chạm vào island
`NotchView` khi chạm bắt đầu **ngoài** vùng chạm notch: nhả trong `touchSlop` → `IslandLayout.hit` tại điểm nhả:
`Control` → lệnh media (Trước / Phát-Dừng theo trạng thái / Tiếp) + `onInteraction`; `Body` → `onTap`
(`OPEN_NOTIFICATIONS` → `GLOBAL_ACTION_NOTIFICATIONS` + `NotificationIndicator.onSeen()`); kéo xa quá `touchSlop` → bỏ.
Bấm vào island có **rung nhẹ** nếu bật Rung phản hồi.

## 4. Hiệu ứng chạm (FR-O6, TE-*)

Khi chạm vào notch (vùng chạm có padding):
- **Gợn sóng + viền sáng**: lúc chạm xuống, một vòng sáng lan ra từ điểm chạm (400 ms, mờ dần); viền quanh notch sáng dần
  theo **tiến độ nhấn giữ** (`pressProgress`), đạt đủ khi tới `longPressMs`. Cử chỉ được nhận diện → TAP / DOUBLE_TAP: nháy
  viền (DOUBLE_TAP nháy 2 lần); LONG_PRESS: viền sáng đầy, rồi tắt khi nhả; vuốt: **vệt sáng** chạy theo hướng vuốt.
- **Co giãn**: viên đen quanh notch (island, kể cả khi `Hidden` thì là chính vùng notch) phình **+12 %** khi chạm xuống và
  nảy về (spring) khi nhả; vuốt → lệch `dp(6)` theo hướng vuốt rồi nảy về.

~~Màu hiệu ứng = màu nhấn Monet (`system_accent1_200`).~~ v1.9.1 (FR-O10): mỗi nét (gợn sóng, viền sáng, vệt sáng) vẽ
**hai lớp**: **quầng tối** `system_accent1_900` alpha 0.55, rộng hơn lõi 3 dp mỗi bên, rồi **lõi sáng**
`system_accent1_100` — lõi nổi trên nền tối, quầng nổi trên nền sáng (status bar sáng / tối tùy app, không theo theme hệ
thống). Gợn sóng: lõi 3 dp alpha 0.95 → 0; vệt sáng: lõi 5 dp. Chỉ khi chạm **bắt đầu trong vùng chạm**; hiệu ứng không đổi việc
nhận cử chỉ.

Engine báo cho lớp hiển thị qua `EngineObserver` (`:app`, `engine/`):
```kotlin
interface EngineObserver { fun onPress(x: Float, y: Float, timeMs: Long); fun onRelease(); fun onGesture(g: Gesture) }
```

```kotlin
sealed interface TouchEffect {
    data object Pulse; data object DoublePulse; data object Glow
    data class Streak(val dx: Int, val dy: Int)       // hướng đơn vị: (−1,0) trái, (1,0) phải, (0,1) xuống
}
fun touchEffectFor(g: Gesture): TouchEffect
fun pressProgress(downMs: Long, nowMs: Long, longPressMs: Long): Float
```

| ID | Given | Then |
|---|---|---|
| TE-1 | `touchEffectFor` | TAP → `Pulse`, DOUBLE_TAP → `DoublePulse`, LONG_PRESS → `Glow`, SWIPE_DOWN / SWIPE_DOWN_HOLD → `Streak(0,1)`, SWIPE_LEFT(_HOLD) → `Streak(−1,0)`, SWIPE_RIGHT(_HOLD) → `Streak(1,0)` |
| TE-2 | `pressProgress` | `(now − down) / longPressMs` kẹp `[0, 1]`; `longPressMs ≤ 0` → 1 |
| EN-16 | engine, chạm xuống **trong** vùng, 1 ngón | `onPress(x, y, t)` một lần; chạm ngoài vùng / ≥ 2 ngón → không gọi |
| EN-17 | sau `onPress`, UP hoặc CANCEL | `onRelease()` đúng một lần |
| EN-18 | cử chỉ đang bật được nhận diện (kể cả Nothing, hoặc rung tắt) | `onGesture(g)`; cử chỉ tắt → không gọi |

## 5. Cài đặt (ST-37, ST-38)

```kotlin
// Settings, v1.9 (v1.9.1, FR-O12: bỏ notificationIndicator — viền thuộc hoạt động NOTIFICATIONS)
val island: Boolean = false,                                   // FR-O2…FR-O5
val islandActivities: Set<IslandActivity> = IslandActivity.entries.toSet(),
val touchEffects: Boolean = false,                             // FR-O6
fun Settings.activeIslandActivities(): Set<IslandActivity> = if (island) islandActivities else emptySet()
```

| ID | Tiêu chí |
|---|---|
| ST-37 | `Settings()`: `island`, `touchEffects` `false`, `islandActivities` = cả 4; JSON 1.8.3 (không có key) → mặc định; round-trip với `island=true`, `islandActivities={MEDIA, SOUND}`, `touchEffects=true`; JSON enum `"media"`, `"sound"` |
| ST-38 | `activeIslandActivities()` = `islandActivities` khi `island`, ngược lại `∅` |
| ST-39 | (v1.9.1, FR-O12) JSON 1.9.0 có `"notificationIndicator": true`: `island = false` → `island = true`, `islandActivities = {NOTIFICATIONS}`; `island = true` → thêm `NOTIFICATIONS` vào `islandActivities`. `false` / không có key → không đổi. JSON mới không còn key này |

## 6. Giao diện (UI-78…UI-81)

Màn **Notch**: section mới **Hiệu ứng** / **Effects** ở **đầu** (trên Độ nhạy):
1. ~~**Notification indicator** / **Báo thông báo**~~ — bỏ ở v1.9.1 (FR-O12); dòng **Thông báo mới** trong hộp thoại
   island có mô tả "Ring glows until seen" / "Viền sáng tới khi xem"
2. **Dynamic Island** — "Hiện hoạt động quanh notch". ~~Khi bật, ngay dưới là 4 dòng con (switch, thụt lề)~~ v1.9.1
   (FR-U63): dòng chỉ có switch; khi island **bật**, chạm dòng (ngoài switch) → hộp thoại **Dynamic Island** với 4 dòng
   switch: **Media đang phát**, **Thông báo mới**, **Sạc pin**, **Âm thanh** (chế độ âm & âm lượng), mỗi dòng có lý do
   thiếu điều kiện như dưới; nút **Xong**. Island tắt → chạm dòng = bật switch. Tắt hoạt động cuối cùng (UI-79) → island
   tắt, hộp thoại đóng.
3. **Touch effects** / **Hiệu ứng chạm** — "Gợn sóng và co giãn khi chạm notch"

Thiếu điều kiện → switch vẫn bật/tắt được (để chuẩn bị trước) nhưng mô tả đổi thành lý do, màu lỗi:
"Cần Trợ năng" (cả ba); "Cần quyền Media và thông báo" (Báo thông báo, island Media, island Thông báo). v1.9.1 (FR-U60):
mô tả rút gọn — "Viền sáng khi có thông báo", "Gợn sóng và co giãn khi chạm", en "Needs Media & notifications".

| ID | Tiêu chí |
|---|---|
| UI-78 | `NotchSettingsViewModel`: ~~`setNotificationIndicator`~~ (bỏ ở v1.9.1), `setIsland`, `setIslandActivity(a, on)`, `setTouchEffects` lưu đúng field, field khác giữ nguyên |
| UI-79 | `setIslandActivity(a, false)` khi `a` là hoạt động cuối cùng còn bật → `island = false`, `islandActivities` giữ nguyên `{a}` (không bao giờ lưu tập rỗng); `setIsland(true)` khi `islandActivities` rỗng (dữ liệu cũ) → bật lại cả 4 |
| UI-80 | `NotchSettingsUiState.overlayIssue(feature)`: Trợ năng chưa bật → `ACCESSIBILITY` (ưu tiên); có Trợ năng nhưng thiếu quyền thông báo, với ~~Báo thông báo /~~ Media / Thông báo → `NOTIFICATION_ACCESS`; Sạc / Âm thanh / Hiệu ứng chạm không cần quyền thông báo → `null` |
| UI-81 | Home › Permissions: dòng `MEDIA_ACCESS` (luôn hiện) đổi nhãn thành **Media & notifications** / **Media và thông báo**; nhãn service listener trong cài đặt hệ thống: **Totch media & notifications** / **Totch media và thông báo** (kiểm bằng `StringsTest`) |

## 7. Ngoài phạm vi / giới hạn
- Không vẽ ở hướng ngang, trên màn hình khóa vẫn vẽ (overlay Trợ năng hiện trên keyguard).
- Phát hiện "đã xem" dựa trên sự kiện cửa sổ của System UI: một số ROM không gửi → viền tắt khi thông báo bị gỡ.
- `VOLUME_CHANGED_ACTION` không phải API công khai; máy không gửi → island âm lượng không hiện (chế độ âm vẫn chạy).
- Không đọc nội dung thông báo (FR-O7): island thông báo chỉ có icon app.
