# Spec: Tự cập nhật từ GitHub Releases (v1.6, v1.6.1, v1.6.3)

Yêu cầu: FR-U27, FR-U29, NFR-4 · Module: `:core` (`core.update.Releases`, `UpdatePolicy`, thuần) + `:app` (`AppUpdater`,
`UpdateController`, `UpdateCheckJobService`, `UpdateInstallReceiver`, `AppSettingsViewModel`)

## 1. Vì sao không cần backend

- Mỗi bản phát hành đã có GitHub Release của tag `vX.Y.Z` chứa `totch-X.Y.Z.apk` **đã ký bằng khóa của tác giả**
  (bắt buộc cho reproducible build F-Droid, xem [04](../04-release-fdroid.md)). APK từ F-Droid và từ GitHub cùng chữ ký
  → cài đè lên nhau được, kể cả khi Totch được cài từ F-Droid.
- API công khai, không cần token: `GET https://api.github.com/repos/ptt98hd/totch/releases/latest` (bỏ qua draft /
  prerelease), trả `tag_name`, `body` (= changelog fastlane), `assets[]` có `name`, `size`, `digest` (`sha256:<hex>`),
  `browser_download_url`. Giới hạn 60 request/giờ/IP — đủ: bấm tay, mỗi lần mở app (nếu bật) và tối đa 1 lần/ngày ở nền.
- Cài: `PackageInstaller` session (quyền `REQUEST_INSTALL_PACKAGES`); hệ thống hiện hộp thoại xác nhận (lần đầu yêu cầu
  cho phép "Cài ứng dụng không rõ nguồn gốc" cho Totch). Android tự từ chối APK khác chữ ký.

## 2. Quyền riêng tư (NFR-4)

- Quyền `INTERNET` chỉ dùng cho tính năng này. Mặc định **không** có kết nối nào tự động: chỉ khi người dùng bấm *Check
  for updates* hoặc *Update*. v1.6.1 (FR-U29): người dùng có thể **tự bật** kiểm tra khi mở app và kiểm tra định kỳ ở
  nền (§6) — cả hai mặc định tắt.
- Chỉ kết nối GitHub: `api.github.com`, `github.com/ptt98hd/totch/releases/download/…` và CDN mà GitHub chuyển hướng tới.
  Không gửi dữ liệu nào ngoài header `User-Agent: Totch/<version>`.

## 3. Phân tích release — `Releases` (`:core`)

```kotlin
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer>
data class Release(val version: SemVer, val notes: String, val apkUrl: String, val apkSize: Long, val sha256: String?)

object Releases {
    const val LATEST_URL = "https://api.github.com/repos/ptt98hd/totch/releases/latest"
    const val DOWNLOAD_PREFIX = "https://github.com/ptt98hd/totch/releases/download/"
    fun parseVersion(value: String): SemVer?        // "v1.6.0" | "1.6.0"
    fun parseLatest(json: String): Release?
    fun isNewer(release: Release, currentVersionName: String): Boolean
}
```

| ID | Tiêu chí |
|---|---|
| UP-1 | `parseVersion`: `"v1.6.0"`, `"1.6.0"` → `SemVer(1,6,0)`; `"1.6"`, `"v1.6.0-beta"`, `"x"`, `""` → `null`; so sánh theo số (`1.10.0 > 1.9.0`) |
| UP-2 | `parseLatest` chọn đúng asset `totch-<X.Y.Z>.apk` của tag (bỏ `totch-X.Y.Z-unsigned.apk` và asset khác); `sha256` = hex chữ thường từ `digest` `"sha256:…"`, thiếu / khác dạng → `null`; `notes` = `body` (null → `""`) |
| UP-3 | `parseLatest` → `null` khi: JSON hỏng, thiếu `tag_name` hoặc tag sai dạng, `draft` / `prerelease` = true, không có asset APK đúng tên, `browser_download_url` không bắt đầu bằng `DOWNLOAD_PREFIX`, `size` ≤ 0. Key lạ bị bỏ qua |
| UP-4 | `isNewer`: đúng khi `release.version > parseVersion(current)`; bằng hoặc cũ hơn → false; `current` không parse được (bản dev) → true |

## 4. Tải & cài — `AppUpdater` (`:app`)

```kotlin
interface AppUpdater {
    /** Latest usable release; throws on network failure, null when none (UP-3). */
    suspend fun latestRelease(): Release?
    /** Downloads, verifies (UP-8) and hands the APK to the system installer; progress 0..1; false on any failure. */
    suspend fun downloadAndInstall(release: Release, onProgress: (Float) -> Unit): Boolean
}
```
`GitHubAppUpdater`: `HttpURLConnection`, timeout 15 s, header `Accept: application/vnd.github+json`. Tải vào
`cacheDir/updates/` (xóa file cũ trước).

UP-8 (thủ công, MT-U*): trước khi cài, APK phải: đúng `apkSize`; SHA-256 khớp `sha256` (nếu có); `getPackageArchiveInfo`
cho `packageName` = Totch và `versionCode` > bản đang chạy. Sai bất kỳ điều kiện nào → xóa file, `false`.

`PackageInstaller`: `MODE_FULL_INSTALL`, `setAppPackageName`, `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` (hệ thống
vẫn hỏi nếu cần). Kết quả về `UpdateInstallReceiver` (không export): `STATUS_PENDING_USER_ACTION` → mở
`EXTRA_INTENT` (`NEW_TASK`); lỗi khác (trừ `STATUS_SUCCESS`) → Toast "Update failed".

## 5. Trạng thái — `UpdateController` (v1.6.1, dùng chung)

v1.6: trạng thái nằm trong `AppSettingsViewModel`. v1.6.1: chuyển vào `UpdateController` (một instance trong
`AppContainer.updates`, scope của process) để hộp thoại bản mới hiện được ở **mọi màn hình** (kiểm tra khi mở app, mở từ
thông báo). `AppSettingsViewModel` chỉ chuyển tiếp (`onUpdateRowClick` → `check()`, `startUpdate`, `dismissUpdateDialog`).
Hộp thoại vẽ ở `MainActivity` (trên `TotchNavHost`), không còn trong màn Settings.

```kotlin
sealed interface UpdateState {
    data object Idle; data object Checking; data object UpToDate
    data class Available(val release: Release)
    data class Downloading(val progress: Float)
    data object Installing          // đã chuyển cho trình cài đặt hệ thống
    data object Failed
}
// AppSettingsUiState: update: UpdateState = Idle, updateDialog: Boolean = false
```

| Trạng thái | Mô tả dòng Check for updates (en / vi) |
|---|---|
| Idle | Get the latest version from GitHub / Tải bản mới nhất từ GitHub |
| Checking | Checking… / Đang kiểm tra… |
| UpToDate | You have the latest version / Bạn đang dùng bản mới nhất |
| Available | Version X.Y.Z is available / Đã có bản X.Y.Z |
| Downloading | Downloading… N% / Đang tải… N% |
| Installing | Confirm to install / Xác nhận để cài đặt |
| Failed | Couldn't update, tap to retry / Không cập nhật được, chạm để thử lại |

| ID | Tiêu chí |
|---|---|
| UP-5 | Trạng thái ban đầu `Idle`, `updateDialog = false`; không gọi mạng khi mở màn hình |
| UP-6 | `onUpdateRowClick()`: `Idle`/`UpToDate`/`Failed`/`Installing` → `Checking` → bản mới hơn (UP-4) → `Available` + `updateDialog = true`; không mới hơn hoặc `null` → `UpToDate`; ném exception → `Failed`. `Available` → chỉ mở lại dialog. `Checking`/`Downloading` → bỏ qua |
| UP-7 | `startUpdate()` (từ `Available`): đóng dialog → `Downloading(p)` theo tiến độ → `true` → `Installing`, `false`/exception → `Failed`. `dismissUpdateDialog()` đóng dialog, giữ `Available` |
| UP-9 | (v1.6.1) `check(silent = true)` (khi mở app, mở từ thông báo cũng dùng): chỉ khi `Idle`/`UpToDate`/`Failed`; trong lúc kiểm tra trạng thái **không đổi** (dòng Settings không hiện "Checking…"); bản mới → `Available` + dialog; không mới / `null` / exception → trạng thái giữ nguyên, không dialog. `Available` → mở lại dialog |

## 6. Kiểm tra tự động (v1.6.1 — FR-U29)

Settings có section **Update** (ui §3) với 3 dòng:

| Dòng | Kiểu | Field |
|---|---|---|
| Check on app launch / Kiểm tra khi mở app | switch | `updateOnLaunch: Boolean = false` |
| Automatic checks / Kiểm tra định kỳ | chọn (dialog): Off / Daily / Weekly / Monthly — Tắt / Hằng ngày / Hằng tuần / Hằng tháng | `updateInterval: UpdateInterval = NEVER` |
| Check for updates / Kiểm tra cập nhật | bấm (UP-6), chuyển từ About sang | — |

```kotlin
@Serializable enum class UpdateInterval(val days: Int) {     // core.settings
    @SerialName("never") NEVER(0), @SerialName("daily") DAILY(1),
    @SerialName("weekly") WEEKLY(7), @SerialName("monthly") MONTHLY(30) }

object UpdatePolicy {                                         // core.update
    /** Chu kỳ JobScheduler (ms), null = không lập lịch. */
    fun periodMillis(interval: UpdateInterval): Long?
    /** Thông báo khi có bản mới hơn chưa từng thông báo. */
    fun shouldNotify(release: Release?, currentVersionName: String, lastNotified: String?): Boolean
}
```

**Khi mở app**: `MainActivity.onCreate` lần đầu (không phải recreate, `savedInstanceState == null`) và
`updateOnLaunch` → `updates.check(silent = true)` (UP-9).

**Định kỳ ở nền**: `UpdateScheduler` theo dõi `updateInterval` (từ `DefaultAppContainer`): `NEVER` → `JobScheduler.cancel`;
khác → `schedule` job `UpdateCheckJobService` định kỳ `periodMillis`, cần mạng (`NETWORK_TYPE_ANY`), `setPersisted(true)`
(sống qua khởi động lại; quyền `RECEIVE_BOOT_COMPLETED` đã có). Không dùng WorkManager (thêm phụ thuộc). Job: gọi
`latestRelease()`; `shouldNotify` → thông báo kênh **Updates** ("Totch X.Y.Z is available" / "Đã có Totch X.Y.Z", chạm →
mở `MainActivity` với action `io.github.ptt98hd.totch.action.SHOW_UPDATE` → `updates.check(silent = true)` → dialog)
và lưu `lastNotified` (SharedPreferences `update`), để mỗi bản chỉ báo một lần. Lỗi mạng → `jobFinished(params, true)`
(hệ thống thử lại theo backoff). Thiếu quyền thông báo → không báo, vẫn lưu.

| ID | Tiêu chí |
|---|---|
| UP-10 | `UpdateInterval.days`: NEVER 0, DAILY 1, WEEKLY 7, MONTHLY 30; JSON `"never"`, `"daily"`, `"weekly"`, `"monthly"` |
| UP-11 | `periodMillis(NEVER) = null`; `periodMillis(WEEKLY) = 7 × 86 400 000` |
| UP-12 | `shouldNotify`: `null` → false; không mới hơn (UP-4) → false; `release.version.toString() == lastNotified` → false; bản mới hơn, `lastNotified` khác / `null` → true |
| UP-13 | `AppSettingsViewModel.setUpdateOnLaunch`, `setUpdateInterval` lưu đúng field |

## 7. Cài qua Shizuku (v1.6.3 — FR-U36)

Shell (uid 2000) có `INSTALL_PACKAGES`, nên `pm install` cài đè Totch **không cần** xác nhận của người dùng và không
cần quyền "Cài ứng dụng không rõ nguồn gốc". Android vẫn từ chối APK khác chữ ký.

Luồng trong `GitHubAppUpdater.downloadAndInstall` sau khi tải và kiểm tra (UP-8):
1. Shizuku sẵn sàng → `ShellInstaller.install(apk)`: bind `TouchUserService` tag `shell` (như `ShizukuShell`), gọi
   `writeFile(ParcelFileDescriptor, path)` để UserService chép APK (app không đọc được file của shell và ngược lại) vào
   `/data/local/tmp/totch-update.apk`, rồi `exec(UpdateCommands.shellInstall(...))`.
2. Lệnh chạy **tách rời** (`setsid`, như PC-5) vì cài đè giết process của Totch và Shizuku gỡ UserService theo:
   ```sh
   setsid sh -c 'sleep 1; pm install -r <apk>; r=$?; rm -f <apk>;
     if [ $r = 0 ]; then am start -n <pkg>/<activity>; else am start -n <pkg>/<activity> -a <pkg>.action.UPDATE_FAILED; fi' \
     </dev/null >/dev/null 2>&1 &
   ```
   Cài xong → Totch được mở lại (bản mới). Lỗi → mở Totch với action `UPDATE_FAILED` → `UpdateController.installFailed()`
   → `Failed` + toast "Update failed".
3. Bước 1 trả `false` (Shizuku không sẵn sàng, bind / chép / exec lỗi) → `PackageInstaller` như §4.

`downloadAndInstall` trả `true` khi đã chuyển giao (Shizuku hoặc `PackageInstaller`) → `Installing`. Dòng Settings ở
trạng thái `Installing` hiện "Installing…" khi qua Shizuku ("Confirm to install" khi qua hệ thống) — `AppUpdater`
báo cách cài qua kết quả `InstallResult`:

```kotlin
enum class InstallResult { FAILED, SYSTEM_INSTALLER, SHIZUKU }   // thay Boolean
data class Installing(val viaShizuku: Boolean) : UpdateState    // thay data object Installing
```

| ID | Tiêu chí |
|---|---|
| UP-14 | `UpdateCommands.shellInstall(pkg, activity, path)` = `setsid sh -c '<script>' </dev/null >/dev/null 2>&1 &` với script trên; package / activity sai định dạng (SC-2) hoặc path không phải `/data/local/tmp/<tên an toàn>.apk` → `null` |
| UP-15 | Script (chạy trên `ShellSim`): `pm install` thành công → `rm` file rồi `am start -n pkg/activity`; lỗi → `rm` rồi `am start` kèm `-a pkg.action.UPDATE_FAILED` |
| UP-16 | `startUpdate`: `SHIZUKU` → `Installing(viaShizuku = true)`, `SYSTEM_INSTALLER` → `Installing(false)`, `FAILED` / exception → `Failed` |
| UP-17 | `installFailed()` → `Failed`, đóng dialog |
| UP-18 | `TouchUserService.writeFile` chỉ ghi vào path được UP-14 chấp nhận (file thuộc shell, cùng uid với `pm`, không cần đổi quyền); lỗi → -1 |
