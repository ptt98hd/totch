# Spec: Tự cập nhật từ GitHub Releases (v1.6)

Yêu cầu: FR-U27, NFR-4 · Module: `:core` (`core.update.Releases`, thuần) + `:app` (`AppUpdater`, `UpdateInstallReceiver`, `AppSettingsViewModel`)

## 1. Vì sao không cần backend

- Mỗi bản phát hành đã có GitHub Release của tag `vX.Y.Z` chứa `totch-X.Y.Z.apk` **đã ký bằng khóa của tác giả**
  (bắt buộc cho reproducible build F-Droid, xem [04](../04-release-fdroid.md)). APK từ F-Droid và từ GitHub cùng chữ ký
  → cài đè lên nhau được, kể cả khi Totch được cài từ F-Droid.
- API công khai, không cần token: `GET https://api.github.com/repos/ptt98hd/totch/releases/latest` (bỏ qua draft /
  prerelease), trả `tag_name`, `body` (= changelog fastlane), `assets[]` có `name`, `size`, `digest` (`sha256:<hex>`),
  `browser_download_url`. Giới hạn 60 request/giờ/IP — đủ vì chỉ gọi khi người dùng bấm.
- Cài: `PackageInstaller` session (quyền `REQUEST_INSTALL_PACKAGES`); hệ thống hiện hộp thoại xác nhận (lần đầu yêu cầu
  cho phép "Cài ứng dụng không rõ nguồn gốc" cho Totch). Android tự từ chối APK khác chữ ký.

## 2. Quyền riêng tư (NFR-4)

- Quyền `INTERNET` chỉ dùng cho tính năng này. **Không** kiểm tra ngầm / định kỳ: chỉ khi người dùng bấm *Check for
  updates* hoặc *Update*.
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

## 5. Trạng thái trên Settings — `AppSettingsViewModel`

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
