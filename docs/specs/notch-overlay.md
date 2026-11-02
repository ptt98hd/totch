# Spec: Lớp hiển thị quanh notch (v1.9 — FR-O1…FR-O8; v1.9.1 — FR-O9…FR-O11, FR-U63; v1.9.3 — FR-O13…FR-O15, FR-R9; v1.9.4 — FR-O16…FR-O18; v1.9.5 — FR-O19, FR-O20)

Yêu cầu: FR-O1…FR-O8 ([01 §22](../01-requirements.md)) · Module: `:core` (`io.github.ptt98hd.totch.core.overlay`: logic
thuần) + `:app` (`overlay/`: overlay, nguồn dữ liệu Android)

Ba tính năng vẽ **quanh notch, trên status bar**: viền báo thông báo (notification indicator), **Dynamic Island** và
**hiệu ứng chạm**. Mỗi tính năng có công tắc riêng, mặc định **tắt**.

## 1. Điều kiện hiển thị (OV-*)

Chỉ overlay `TYPE_ACCESSIBILITY_OVERLAY` của `NotchAccessibilityService` vẽ được **trên** status bar (overlay
`TYPE_APPLICATION_OVERLAY` nằm dưới) → cả ba tính năng **cần Trợ năng**. Viền thông báo, island media và island thông
báo còn cần **quyền truy cập thông báo** (`MediaListenerService`, dòng Home › Permissions đổi tên thành
**Media & notifications** / **Media và thông báo**, FR-O7).

~~`overlayVisible(settings, accessibility, rotation, statusBarVisible)`~~ v1.9.3: `overlayVisible(settings, accessibility):
Boolean` (`:core`) — không còn phụ thuộc hướng xoay (§8) và status bar (OV-4 chỉ ẩn phần island):

| ID | Given | Then |
|---|---|---|
| OV-1 | `enabled = false` (công tắc Totch tắt) | `false` |
| OV-2 | Trợ năng chưa kết nối | `false` |
| OV-3 | ~~`rotation ≠ 0` → `false`~~ v1.9.3 (FR-O15): bỏ — vẽ ở mọi hướng, tại notch vật lý (§8) | |
| OV-4 | status bar đang ẩn (app toàn màn hình: video, game; v1.9.4: đọc theo §10.2, SB-*). v1.9.3: `islandShown(content, statusBarVisible)` | ~~`false`~~ v1.9.3: island **hoạt động** (media, sạc, thông báo, âm thanh) và viền thông báo không vẽ — `islandShown` trả `Hidden`; hiệu ứng chạm (§4) **vẫn vẽ** vì do người dùng chạm. Status bar hiện → nội dung giữ nguyên |
| OV-5 | `enabled`, Trợ năng, và **ít nhất một** trong `island`, `touchEffects` bật (v1.9.1: bỏ `notificationIndicator`, FR-O12) | `true`; cả hai tắt → `false` |

Hình học dùng **vùng notch không padding** (`HighlightArea.notch`, notch-region §FR-R7) — vùng người dùng đã căn bằng
slider Vị trí / Kích thước, nên viền và island bám đúng camera.

Cửa sổ:
- **Cửa sổ vẽ**: rộng toàn màn hình, từ cạnh trên, cao = `IslandLayout.EXPANDED` bottom + 24 dp (chỗ cho glow),
  `FLAG_NOT_TOUCHABLE` (không nhận chạm), cutout `ALWAYS`. Nội dung vẽ bằng Compose. v1.9.3: "rộng toàn màn hình, từ
  cạnh trên" là trong **hệ tọa độ tự nhiên**; khi xoay, cửa sổ là dải dọc theo cạnh có notch (§8).
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
| Âm thanh (FR-O5) | `RINGER_MODE_CHANGED_ACTION`, `ACTION_INTERRUPTION_FILTER_CHANGED`, `android.media.VOLUME_CHANGED_ACTION` (luồng media / chuông) | trái: icon chế độ (chuông / rung / im lặng / trăng), phải: nhãn ngắn; âm lượng: icon loa + ~~thanh tiến độ~~ v1.9.4: vòng tròn (§10.3) | — |

Media mở rộng: island rộng gần hết màn hình, ảnh bìa 56 dp, tên bài + nghệ sĩ (một dòng, "…"), hàng nút
**Trước · Phát/Dừng · Tiếp** (điều khiển `MediaController.transportControls` của phiên đang hiện).

### 3.5 Chuyển động vòng sáng → giãn → thu (v1.9.1, FR-O11, IM-*)

Island (và nhịp **bung** của Báo thông báo) đi theo trình tự, **vòng sáng** (glow ring như §2, không nhịp thở) bao
quanh viên đen **suốt** từ đầu tới cuối:
1. `RING_IN` — vòng sáng hiện dần quanh **vùng notch** (viên = notch), `RING_IN_MS` = 220 ms.
2. `MORPH` — viên giãn ra hai bên tới hình đích (lò xo như §3); vòng sáng bám theo viên.
3. Hết nội dung: `MORPH` về notch (thu lại), rồi `RING_OUT` — vòng sáng tắt dần, `RING_OUT_MS` = 280 ms; v1.9.5: viên
   đen cùng vòng sáng thu nhỏ cả ngang lẫn dọc và mờ dần rồi biến mất (§11.1).

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
    var expandable: Boolean                      // v1.9.3: false khi xoay — media không mở rộng (IS-15)
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
| IS-15 | (v1.9.3, FR-O15) `expandable = false` (màn đang xoay, §8) | `onTap` khi `Media` thu gọn → `NONE`, không mở rộng; đang mở rộng mà đặt `expandable = false` → thu gọn ngay. Mặc định `true` |

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

## 4. Hiệu ứng chạm (FR-O6; v1.9.3 — FR-O14, TF-*)

~~v1.9–1.9.2: gợn sóng tại điểm chạm, viền sáng theo tiến độ giữ, nháy, vệt sáng theo hướng vuốt, viên đen co giãn /
lệch theo hướng vuốt (TE-1).~~ v1.9.3 (FR-O14): tối giản — chỉ **viên đen quanh notch + vòng sáng**, viên giãn
**ngang** (dọc theo cạnh có notch) và khi cử chỉ chạy thì hiện **icon cử chỉ** (trái) và **icon hành động** (phải):

| Cử chỉ | Chạm xuống | Đang chạm | Cử chỉ được nhận (hành động chạy) |
|---|---|---|---|
| Chạm / chạm đúp | vòng sáng quanh notch | — (nhả tay: vòng sáng giữ quanh notch, chờ kết quả) | viên giãn ra, trái icon cử chỉ, phải icon hành động |
| Nhấn giữ | vòng sáng | viên giãn ngang dần theo thời gian giữ | như trên (lúc đủ `longPressMs`, ngón tay có thể vẫn đang giữ) |
| Vuốt / vuốt giữ | vòng sáng | viên giãn ngang theo **khoảng cách ngón tay** đã đi | như trên; vuốt giữ chỉ hiện icon khi hành động chạy |

Không có cử chỉ (vuốt quá ngắn, cử chỉ tắt) → hết hạn chờ thì viên thu về notch và vòng sáng tắt (như IM-3). Kết quả hiện
`RESULT_MS` = 1200 ms rồi thu lại. Trong lúc có phản hồi chạm, viên **thay** nội dung island (icon island ẩn); hết phản
hồi → island trở lại như §3.5 (vòng sáng còn sáng → chỉ `MORPH`).

Vẽ: viên đen + vòng sáng hai lớp như FR-O10 (quầng tối `system_accent1_900` alpha 0.55 rộng hơn 3 dp mỗi bên, lõi sáng
`system_accent1_100`); icon trắng. Vòng sáng hiện dần (`RING_IN_MS`) **cùng lúc** với việc giãn theo ngón tay (không đợi).
Khi đang chạm, hình viên bám mục tiêu mỗi frame (nội suy 35 % mỗi frame); khi có kết quả / thu lại dùng lò xo như §3.
Chỉ khi chạm **bắt đầu trong vùng chạm**; hiệu ứng không đổi việc nhận cử chỉ.

Engine báo cho lớp hiển thị qua `EngineObserver` (`:app`, `engine/`) — tọa độ **tự nhiên** (như `getevent`, §8):
```kotlin
interface EngineObserver {
    fun onPress(x: Float, y: Float, timeMs: Long)
    fun onMove(x: Float, y: Float)                 // v1.9.3
    fun onRelease()
    fun onGesture(g: Gesture, action: Action)      // v1.9.3: kèm hành động thực tế (Action.None = Không làm gì)
}
```

```kotlin
sealed interface TouchFeedback {
    data object Idle
    data class Pressing(val downX: Float, val downY: Float, val downMs: Long, val x: Float, val y: Float)
    data object Waiting                                   // đã nhả, chờ cử chỉ (chạm đơn chờ chạm đúp)
    data class Result(val gesture: Gesture, val action: Action)
}
class TouchFeedbackController {                           // thuần, thời gian qua tham số (NFR-3)
    val state: TouchFeedback
    fun onPress(x: Float, y: Float, nowMs: Long)
    fun onMove(x: Float, y: Float)
    fun onRelease(nowMs: Long, waitMs: Long)              // waitMs = doubleTapMs + 100
    fun onGesture(g: Gesture, action: Action, nowMs: Long)
    fun onTick(nowMs: Long)
    fun nextDeadlineMs(): Long?
    companion object { const val RESULT_MS = 1200L; const val WAIT_MARGIN_MS = 100L }
}
fun pressStretch(p: TouchFeedback.Pressing, nowMs: Long, longPressMs: Long, maxPx: Float): Float
fun pressProgress(downMs: Long, nowMs: Long, longPressMs: Long): Float   // TE-2, giữ nguyên
object IslandLayout {
    fun feedbackWing(notch: Rect, m: IslandMetrics): Int                 // h + dp(16), = wing của IG-2
    fun stretched(notch: Rect, m: IslandMetrics, stretchPx: Int): Rect
}
```

| ID | Given | Then |
|---|---|---|
| TE-1 | ~~`touchEffectFor`~~ | v1.9.3: bỏ (thay bằng TF-*) |
| TE-2 | `pressProgress` | `(now − down) / longPressMs` kẹp `[0, 1]`; `longPressMs ≤ 0` → 1 |
| TF-1 | controller mới | `Idle`, `nextDeadlineMs() = null` |
| TF-2 | `onPress(x, y, t)` | `Pressing(x, y, t, x, y)`; `onMove(x2, y2)` → chỉ đổi `x`, `y`; `onMove` khi không `Pressing` → không đổi |
| TF-3 | `onRelease(t, waitMs)` khi `Pressing` | `Waiting`, `nextDeadlineMs = t + waitMs`; `onTick` trước mốc không đổi, tại mốc → `Idle`, mốc `null`. `onRelease` khi `Idle` / `Result` → không đổi |
| TF-4 | `onGesture(g, a, t)` (khi `Pressing`, `Waiting` hay `Idle`) | `Result(g, a)`, mốc `t + RESULT_MS`; tới mốc → `Idle`. Sau đó `onMove` / `onRelease` (ngón tay vẫn giữ sau nhấn giữ) không đổi `Result` |
| TF-5 | `onPress` khi `Waiting` hoặc `Result` | `Pressing` mới (chạm mới thay thế), không còn mốc |
| TF-6 | `pressStretch` | `drag = hypot(x − downX, y − downY)`; `hold` = `((now − down) − L/3) / (2L/3)` kẹp `[0,1]` × `maxPx` (`L = longPressMs`; `L ≤ 0` → `maxPx`) — chạm nhanh (< L/3) không giãn; kết quả `max(drag, hold)` kẹp `[0, maxPx]` |
| TF-7 | `stretched(notch, m, s)` | `(notch.left − s, notch.top, notch.right + s, notch.bottom)`, trái/phải kẹp trong `[dp(8), screenWidthPx − dp(8)]`; `s ≤ 0` → `notch`. `feedbackWing` = `notch.height + dp(16)`; ví dụ IG-6: `stretched(notch, wing = 112)` = `(388,0,692,80)` |
| EN-16 | engine, chạm xuống **trong** vùng, 1 ngón | `onPress(x, y, t)` một lần; chạm ngoài vùng / ≥ 2 ngón → không gọi |
| EN-17 | sau `onPress`, UP hoặc CANCEL | `onRelease()` đúng một lần |
| EN-18 | cử chỉ đang bật được nhận diện (kể cả Nothing, hoặc rung tắt) | `onGesture(g, action)` với `action = effectiveAction(g)` (`Action.None` khi Nothing); cử chỉ tắt → không gọi |
| EN-19 | (v1.9.3) sau `onPress`, MOVE 1 ngón | `onMove(x, y)`; trước `onPress` / sau `onRelease` / ≥ 2 ngón → không gọi |

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
3. **Touch effects** / **Hiệu ứng chạm** — ~~"Gợn sóng và co giãn khi chạm notch"~~ v1.9.3: "Glow and icons on touch" / "Vòng sáng và icon khi chạm"

Thiếu điều kiện → switch vẫn bật/tắt được (để chuẩn bị trước) nhưng mô tả đổi thành lý do, màu lỗi:
"Cần Trợ năng" (cả ba); "Cần quyền Media và thông báo" (Báo thông báo, island Media, island Thông báo). v1.9.1 (FR-U60):
mô tả rút gọn — "Viền sáng khi có thông báo", "Gợn sóng và co giãn khi chạm", en "Needs Media & notifications".

| ID | Tiêu chí |
|---|---|
| UI-78 | `NotchSettingsViewModel`: ~~`setNotificationIndicator`~~ (bỏ ở v1.9.1), `setIsland`, `setIslandActivity(a, on)`, `setTouchEffects` lưu đúng field, field khác giữ nguyên |
| UI-79 | `setIslandActivity(a, false)` khi `a` là hoạt động cuối cùng còn bật → `island = false`, `islandActivities` giữ nguyên `{a}` (không bao giờ lưu tập rỗng); `setIsland(true)` khi `islandActivities` rỗng (dữ liệu cũ) → bật lại cả 4 |
| UI-80 | `NotchSettingsUiState.overlayIssue(feature)`: Trợ năng chưa bật → `ACCESSIBILITY` (ưu tiên); có Trợ năng nhưng thiếu quyền thông báo, với ~~Báo thông báo /~~ Media / Thông báo → `NOTIFICATION_ACCESS`; Sạc / Âm thanh / Hiệu ứng chạm không cần quyền thông báo → `null` |
| UI-81 | Home › Permissions: dòng `MEDIA_ACCESS` (luôn hiện) đổi nhãn thành **Media & notifications** / **Media và thông báo**; nhãn service listener trong cài đặt hệ thống: **Totch media & notifications** / **Totch media và thông báo** (kiểm bằng `StringsTest`) |

## 7. Đọc lại trạng thái màn hình (v1.9.3 — FR-O13, DR-*)

Bản ≤ 1.9.2 chỉ đọc `Display` trong `DisplayManager.DisplayListener.onDisplayChanged`. Có máy gửi callback trước khi
thông tin xoay / kích thước cập nhật (hoặc không gửi khi app toàn màn hình tự xoay rồi thoát) → giữ hướng cũ: island vẽ
ở chỗ không có notch, engine báo sai `PausedRotation`. Sửa: `DisplayMonitor` dùng `DisplayRefresher` (thuần, test được):

```kotlin
class DisplayRefresher(scope: CoroutineScope, read: () -> DisplaySnapshot) {
    val state: StateFlow<DisplaySnapshot>       // distinct
    fun refresh()                               // đọc ngay, rồi đọc lại sau SETTLE_MS = [250, 1000]
}
```
Tín hiệu gọi `refresh()`: `DisplayListener` (mọi sự kiện của màn mặc định), broadcast `Intent.ACTION_CONFIGURATION_CHANGED`
(đăng ký động, trong process có foreground service), `NotchAccessibilityService.onConfigurationChanged`, và listener insets
của cửa sổ vẽ (status bar ẩn / hiện, xoay). Sau mỗi lần cập nhật cửa sổ vẽ, `requestApplyInsets()` để trạng thái status
bar (OV-4) cũng được đọc lại.

| ID | Given | Then |
|---|---|---|
| DR-1 | tạo `DisplayRefresher` | `state` = `read()` ngay |
| DR-2 | `refresh()` | đọc ngay; đọc lại tại +250 ms và +1000 ms (giá trị đổi giữa chừng → `state` cập nhật), sau đó không đọc nữa |
| DR-3 | `refresh()` lần hai khi lần đầu còn chờ | lịch cũ bị hủy, tính lại từ lần gọi mới (không đọc thừa) |
| DR-4 | (thủ công MT-O17) xem video toàn màn hình ngang rồi thoát / xoay qua lại nhanh | island và trạng thái engine theo đúng hướng hiện tại |

## 8. Xoay màn hình (v1.9.3 — FR-O15, FR-R9, RO-*)

Mọi thứ của lớp hiển thị (vùng notch, island, hiệu ứng chạm, hit test) tính trong **hệ tọa độ tự nhiên** (ROTATION_0 —
notch ở cạnh trên), giống `getevent` và `engineRegion` (notch-region §FR-R8): `OverlayFrame.notch` =
`highlightArea(settings, display.natural()).notch`, `IslandMetrics.screenWidthPx` = chiều rộng tự nhiên. Chỉ khi đặt
cửa sổ mới đổi sang hướng hiện tại:

- `fromNatural(rect, rotation, widthPx, heightPx)` (`widthPx`/`heightPx` của hướng hiện tại) — nghịch đảo của `toNatural`
  (NR-18); `pointToNatural(x, y, rotation, widthPx, heightPx)` — đổi một điểm (NR-19).
- **Cửa sổ vẽ**: rect tự nhiên `(0, 0, naturalWidth, frame.heightPx)` → `fromNatural`. Nội dung Compose đo ở kích thước
  tự nhiên rồi đặt bằng một layer xoay: `naturalLayer(rotation, windowW, windowH) = NaturalLayer(degrees, x, y)` — điểm tự
  nhiên `p` vẽ tại `rotate(p, degrees) + (x, y)` (xoay quanh góc trái trên, chiều kim đồng hồ):

  | rotation | degrees | (x, y) |
  |---|---|---|
  | 0 | 0 | (0, 0) |
  | 1 | 270 | (0, windowH) |
  | 2 | 180 | (windowW, windowH) |
  | 3 | 90 | (windowW, 0) |

  Icon / chữ trong viên xoay ngược `−degrees` để luôn **đứng thẳng**. Media không mở rộng khi xoay (IS-15).
- **Cửa sổ chạm** (`NotchView`): hợp (tự nhiên) của vùng chạm engine và viên island bấm được → `fromNatural`; mỗi
  `MotionEvent` đổi `rawX/rawY` bằng `pointToNatural` rồi mới chia cho engine / hit test island. Khi xoay, view đặt
  `systemGestureExclusionRects` = toàn view.

| ID | Given | Then |
|---|---|---|
| NR-18 | `fromNatural` | nghịch đảo `toNatural` ở cả 4 hướng; ví dụ rotation 1, màn 2400×1080, `(500,0,580,80)` → `(0,500,80,580)`; rotation 3 → `(2320,500,2400,580)`; rotation 2, màn 1080×2400 → `(500,2320,580,2400)`; rotation 0 → giữ nguyên |
| NR-19 | `pointToNatural` | rotation 1, màn 2400×1080: `(40, 540)` → `(540, 40)`; rotation 3: `(2360, 540)` → `(540, 40)`; rotation 2, màn 1080×2400: `(540, 2360)` → `(540, 40)`; rotation 0 → giữ nguyên |
| RO-1 | `naturalLayer` | đúng bảng trên; với mọi hướng, ảnh của rect tự nhiên `(0,0,W,H)` qua layer = `fromNatural(...)` dời về gốc cửa sổ |
| RO-2 | (thủ công MT-O18) xoay 90° / 270° với island media đang hiện; chạm / vuốt notch | viên nằm dọc theo cạnh có camera, icon đứng thẳng; vuốt ra xa camera = vuốt xuống |

## 9. Ngoài phạm vi / giới hạn
- ~~Không vẽ ở hướng ngang~~ (v1.9.3: vẽ ở mọi hướng, §8), trên màn hình khóa vẫn vẽ (overlay Trợ năng hiện trên keyguard).
- Phát hiện "đã xem" dựa trên sự kiện cửa sổ của System UI: một số ROM không gửi → viền tắt khi thông báo bị gỡ.
- v1.9.4: máy có trạng thái insets thô không phản ánh status bar ẩn → island vẫn hiện khi toàn màn hình (như 1.9.3).
- `VOLUME_CHANGED_ACTION` không phải API công khai; máy không gửi → island âm lượng không hiện (chế độ âm vẫn chạy).
- Không đọc nội dung thông báo (FR-O7): island thông báo chỉ có icon app.

## 10. v1.9.4 (FR-O16…FR-O18)

### 10.1 Viền gọn (FR-O16, IG-7)
Bản ≤ 1.9.3: viên đen đúng bằng vùng notch và mờ dần cùng vòng sáng (`alpha = ring`), hai nét mềm của vòng sáng (10 dp,
6 dp) nằm giữa đường viền nên lấn vào trong viên → mép trong nhòe, vùng notch hiệu chỉnh lệch một chút là thấy viền
camera thật. Sửa:
- Viên đen vẽ theo `IslandLayout.cover(shape, m)` — rộng hơn hình viên `BLEED = dp(2)` ở trái, phải, đáy (cạnh trên
  giữ nguyên); hit test, vị trí icon vẫn theo hình viên.
- Viên **đặc** (`alpha = 1`) mỗi khi vòng sáng > 0 hoặc đang có phản hồi chạm.
- Vòng sáng (cả quầng tối của phản hồi chạm) chỉ vẽ **ngoài** viên: clip `Difference` theo hình bo góc của `cover`; nét sắc
  2.5 dp đặt sát mép (`cover` nới ra 1.25 dp), mép trong của vòng sáng trùng mép viên.

| ID | Given | Then |
|---|---|---|
| IG-7 | `cover(rect, m)` | `(rect.left − dp(2), rect.top, rect.right + dp(2), rect.bottom + dp(2))`, trái/phải kẹp trong `[0, screenWidthPx]`; ví dụ IG-6: `cover(notch)` = `(496,0,584,84)`, `cover((0,0,80,80))` = `(0,0,84,84)` |

### 10.2 Nhận biết toàn màn hình (FR-O17, SB-*)
Bản 1.9.3 đọc `WindowInsets.isVisible(statusBars())` của cửa sổ vẽ. Cửa sổ `TYPE_ACCESSIBILITY_OVERLAY` nằm **trên**
status bar nên không nhận nguồn insets của nó; loại insets không có nguồn được coi là hiện → luôn `true`, island media
vẫn hiện khi xem video toàn màn hình. Sửa: `NotchAccessibilityService` đọc
`WindowManager.currentWindowMetrics.windowInsets.isVisible(statusBars())` (trạng thái insets thô của màn hình; lỗi →
`true`) qua `StatusBarWatcher` (thuần, test được như `DisplayRefresher`):

```kotlin
class StatusBarWatcher(scope: CoroutineScope, read: () -> Boolean) {
    val visible: StateFlow<Boolean>
    var active: Boolean                 // island có nội dung (≠ Hidden) — chỉ lúc đó mới cần biết
    fun refresh()                       // tín hiệu: insets cửa sổ vẽ đổi, đọc lại màn hình (§7)
    companion object { const val POLL_MS = 500L }
}
```
`statusBarVisible` của hub = cửa sổ vẽ báo hiện **và** `StatusBarWatcher.visible`.

| ID | Given | Then |
|---|---|---|
| SB-1 | tạo watcher | `visible = true`, chưa đọc |
| SB-2 | `active = true` | đọc ngay, rồi mỗi `POLL_MS`; giá trị đổi → `visible` cập nhật |
| SB-3 | `active = false` | ngừng đọc, giữ giá trị cuối |
| SB-4 | `refresh()` | đọc ngay (kể cả khi `active = false`) |
| SB-5 | (thủ công MT-O22) YouTube toàn màn hình khi đang phát nhạc / video có media session | island media ẩn trong ≤ 0.5 s; thoát toàn màn hình → hiện lại |

### 10.3 Âm lượng dạng vòng (FR-O18, VR-1)
Island `Volume`: trái icon loa, phải **vòng tròn** đường kính = cỡ icon: vòng nền trắng alpha 0.25, cung trắng từ 12 giờ
theo chiều kim đồng hồ, nét 3 dp, đầu tròn. Cung đổi mượt (tween 150 ms) khi âm lượng đổi liên tiếp (IS-14).

| ID | Given | Then |
|---|---|---|
| VR-1 | `volumeSweep(fraction)` (`:core`) | `360 × fraction` kẹp `[0, 360]`; `0.5` → `180`, `-1` → `0`, `2` → `360` |

## 11. v1.9.5 (FR-O19, FR-O20)

### 11.1 Biến mất mượt (FR-O19, IM-6)
Bản ≤ 1.9.4: hết nội dung → viên thu về notch, vòng sáng tắt dần, còn viên đen giữ nguyên cỡ notch rồi **biến mất đột
ngột** khi vòng sáng về 0. Sửa: trong `RING_OUT`, cùng lúc với vòng sáng, giá trị `whole` chạy `1 → 0`
(tween `RING_OUT_MS`); viên đen (`cover`) và vòng sáng vẽ **thu nhỏ quanh tâm** của `cover` theo
`vanishScale(whole)` cả hai chiều (bo góc thu theo), viên đen có `alpha = whole`. `whole` về 1 khi island hiện lại
(`RING_IN`: vòng sáng đang tắt hẳn → đặt ngay 1, đang tắt dở → chạy về 1 cùng vòng sáng) và khi chạm (phản hồi chạm luôn
vẽ đủ cỡ, đặc). Chuyển động hiện ra giữ nguyên.

```kotlin
object IslandMotion {
    const val VANISH_MIN_SCALE = 0.3f
    fun vanishScale(whole: Float): Float   // whole: 1 = đủ cỡ … 0 = đã biến mất
}
```

| ID | Given | Then |
|---|---|---|
| IM-6 | `vanishScale(whole)` | `VANISH_MIN_SCALE + (1 − VANISH_MIN_SCALE) × whole`, `whole` kẹp `[0, 1]`; `1` → `1`, `0` → `0.3`, `0.5` → `0.65`, `-1` → `0.3`, `2` → `1` |
| IM-7 | (thủ công MT-O25) island đang hiện rồi hết nội dung | viên thu về notch, rồi thu nhỏ cả ngang lẫn dọc, mờ dần và biến mất — không tắt đột ngột |

### 11.2 Nội dung căn giữa theo chiều dọc (FR-O20, IG-8)
Bản 1.9.4: icon / chữ của island thu gọn và của kết quả cử chỉ căn giữa theo hình viên (`notch.top … notch.bottom`), nhưng
phần đen thấy được là `cover` — dài hơn `BLEED` ở đáy (IG-7) → nội dung lệch lên `BLEED / 2`. Sửa: ô nội dung (hai cánh
trái / phải) lấy chiều dọc theo `IslandLayout.band(shape, m)`, căn giữa theo phần đen thấy được; chữ (`PillText`) bỏ
khoảng đệm font và cắt line height (`LineHeightStyle(Center, Trim.Both)`) để glyph nằm đúng tâm ô.

| ID | Given | Then |
|---|---|---|
| IG-8 | `band(shape, m)` | `(shape.left, shape.top, shape.right, shape.bottom + dp(2))` — cùng chiều dọc với `cover(shape, m)`; ví dụ IG-6: `band(notch)` = `(500,0,580,84)` |
| IG-9 | (thủ công MT-O26) island sạc, âm thanh, âm lượng, thông báo, media thu gọn; kết quả cử chỉ | icon và chữ nằm giữa viên đen theo chiều dọc |
