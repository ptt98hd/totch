# 04 — Phát hành & F-Droid

> Cập nhật: 2026-10-28

## 1. Đánh số phiên bản

- `versionName` theo SemVer: `MAJOR.MINOR.PATCH`.
- `versionCode = MAJOR*10000 + MINOR*100 + PATCH` (ví dụ `1.2.3` → `10203`).
- Tag git: `vX.Y.Z` (khớp `UpdateCheckMode: Tags` của F-Droid).

## 2. Checklist phát hành

1. Cập nhật `versionName` / `versionCode` trong `app/build.gradle.kts`.
2. Tạo changelog `fastlane/metadata/android/{en-US,vi}/changelogs/<versionCode>.txt`, ≤ 500 ký tự.
3. `./gradlew test lint assembleRelease` xanh (CI cũng chạy lệnh này).
4. Chạy [05-manual-test-checklist.md](05-manual-test-checklist.md) trên máy thật, cả 2 chế độ; ghi kết quả vào phần lịch sử.
5. Commit, tạo tag có chú thích và push:
   ```sh
   git tag -a vX.Y.Z -m "Totch X.Y.Z"
   git push origin main vX.Y.Z
   ```
6. **Bắt buộc** (reproducible builds): build, ký và đăng APK lên GitHub Release của tag:
   ```sh
   scripts/release-apk.sh vX.Y.Z --publish
   ```
   F-Droid tải `totch-X.Y.Z.apk` từ release này để so sánh. Thiếu file → F-Droid không phát hành bản đó.
7. F-Droid tự phát hiện tag mới (`UpdateCheckMode: Tags`, `AutoUpdateMode: Version`) và build trong vài ngày —
   không cần MR mới cho mỗi bản.

## 3. Reproducible builds & ký

App phát hành trên F-Droid **bằng chữ ký của bạn** (đã bật từ 1.0.1):

1. F-Droid build lại APK unsigned từ commit của tag.
2. Tải APK đã ký từ `Binaries:` (GitHub Release), chép chữ ký sang bản build của mình (`apksigcopier`).
3. Nếu chữ ký hợp lệ và khóa khớp `AllowedAPKSigningKeys` → phát hành APK mang chữ ký của bạn.
   → APK từ F-Droid và từ GitHub cài đè lên nhau được.

| Thành phần | Giá trị |
|---|---|
| Keystore | `~/.config/totch/totch-release.jks` (PKCS12, RSA 4096, alias `totch`) |
| Mật khẩu | `~/.config/totch/keystore.properties` (quyền 600) |
| SHA-256 chứng chỉ | `90f255632ccd15e18244b114256e4a2d79e5ae406fa4df06cd2a2ef3ebd5b8ae` |

> ⚠️ **Sao lưu ngay** cả thư mục `~/.config/totch/` vào nơi an toàn (password manager / ổ mã hóa ngoại tuyến).
> Mất keystore = không bao giờ phát hành được bản cập nhật cho người đã cài. **Không bao giờ commit** keystore/mật khẩu.

Các điểm đã xử lý để build tái lập (đã kiểm chứng bằng `fdroid build` cục bộ: *successfully verified*):
- Script build từ **bản clone thật** của tag: AGP nhúng revision git vào `META-INF/version-control-info.textproto`;
  git worktree cho ra `NO_VALID_GIT_FOUND` → khác byte.
- Ký với `apksigner sign --alignment-preserved --v4-signing-enabled false`: mặc định apksigner căn chỉnh lại entry
  → nội dung khác bản unsigned của F-Droid.
- Gradle wrapper ghim `distributionSha256Sum`; JDK 21 (mise, `mise.toml`) cùng phiên bản chính với build server F-Droid
  (`buildserver-trixie`: Debian OpenJDK 21). Không dùng `jvmToolchain(...)`: server chỉ có JDK 21, toolchain 17 sẽ làm build
  lỗi (bài học từ MR đầu tiên, bản 1.0.1).

Kiểm tra tái lập cục bộ (tùy chọn): `fdroid build -v -l io.github.ptt98hd.totch` trong một bản clone fdroiddata,
kết quả phải có dòng `...successfully verified`. Lưu ý: `apksigcopier` lỗi `Unsupported compresslevel` với
Python 3.14 → dùng Python 3.12 (`mise install python@3.12`).

## 4. Gửi app lên F-Droid (lần đầu)

1. **Đẩy mã lên GitHub** `https://github.com/ptt98hd/totch`, tag `vX.Y.Z` và GitHub Release có APK đã ký (mục 2).
2. **Fork** https://gitlab.com/fdroid/fdroiddata (cần tài khoản GitLab).
3. Tạo nhánh mới, chép `metadata/io.github.ptt98hd.totch.yml` từ repo này vào `metadata/` của fork.
4. **Kiểm tra cục bộ** (khuyến nghị) bằng Docker image fdroidserver, đứng trong thư mục fdroiddata:
   ```sh
   docker run --rm -v "$PWD":/repo registry.gitlab.com/fdroid/fdroidserver:buildserver \
     lint io.github.ptt98hd.totch
   docker run --rm -v "$PWD":/repo registry.gitlab.com/fdroid/fdroidserver:buildserver \
     build -v -l io.github.ptt98hd.totch
   docker run --rm -v "$PWD":/repo registry.gitlab.com/fdroid/fdroidserver:buildserver \
     rewritemeta io.github.ptt98hd.totch   # chuẩn hóa định dạng YAML
   ```
   (Hoặc cài `fdroidserver` qua pip và chạy `fdroid lint` / `fdroid build -v -l io.github.ptt98hd.totch`.)
   Xem hướng dẫn mới nhất tại https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md.
5. Commit (`New app: Totch`), push, mở **Merge Request** vào `fdroid/fdroiddata` (template *Default* = new app inclusion) và điền checklist. Trường `commit` phải là **hash đầy đủ**, không dùng tên tag. CI của fdroiddata sẽ chạy lint + build; sửa theo phản hồi của reviewer.
6. Sau khi merge, app thường xuất hiện trên F-Droid sau 1–7 ngày. Lúc đó thay badge placeholder trong `README.md` (đường dẫn đã đúng `f-droid.org/packages/io.github.ptt98hd.totch/`).

### Lưu ý khi review

- **Accessibility:** reviewer sẽ hỏi vì sao dùng AccessibilityService. Trả lời rõ (đã có trong `full_description.txt`): chỉ để vẽ vùng chạm trên notch và thực hiện global action, không đọc nội dung màn hình, dịch vụ là tùy chọn. Đảm bảo `accessibility_service_config` không bật `canRetrieveWindowContent` và có `android:description` giải thích. F-Droid hiện không gắn anti-feature cho Accessibility nếu dùng đúng mục đích.
- **Shizuku:** thư viện `dev.rikka.shizuku:api/provider` là Apache-2.0, có trên Maven Central → tương thích F-Droid; app Shizuku cũng có trên F-Droid. Không cần anti-feature `NonFreeDep`. Nêu trong MR rằng Shizuku là tùy chọn.
- **Notification access** (tùy chọn, cho media): `MediaListenerService` không override `onNotificationPosted`, chỉ dùng `getActiveSessions`. Đã nêu trong `full_description.txt`.
- Không có `INTERNET`, không tracker → không khai báo `AntiFeatures` (`fdroid lint` không chấp nhận danh sách rỗng `[]`). Trước khi gửi, kiểm tra không có dependency Google Play Services / Firebase lọt vào (`./gradlew :app:dependencies`).
- Build server F-Droid (`buildserver-trixie`) có sẵn OpenJDK 21 nên metadata không cần khối `sudo`. Nếu sau này cần JDK khác, thêm:
  ```yaml
      sudo:
        - apt-get update
        - apt-get install -y openjdk-21-jdk-headless
        - update-alternatives --auto java
  ```
- Ảnh: `fastlane/metadata/android/en-US/images/icon.png` (512×512) đã có. Ảnh chụp màn hình (`phoneScreenshots/`) **không bắt buộc** nhưng nên bổ sung sau — F-Droid đọc trực tiếp từ repo theo tag.
