# 04 — Phát hành & F-Droid

> Cập nhật: 2026-10-28

## 1. Đánh số phiên bản

- `versionName` theo SemVer: `MAJOR.MINOR.PATCH`.
- `versionCode = MAJOR*10000 + MINOR*100 + PATCH` (ví dụ `1.2.3` → `10203`).
- Tag git: `vX.Y.Z` (khớp `UpdateCheckMode: Tags` của F-Droid).

## 2. Checklist phát hành

1. Cập nhật `versionName` / `versionCode` trong `app/build.gradle.kts`.
2. Tạo changelog `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (và `vi/`), ≤ 500 ký tự.
3. `./gradlew test lint assembleRelease` xanh (CI cũng chạy lệnh này).
4. Chạy checklist test thủ công trong [03-testing.md](03-testing.md) trên thiết bị thật, cả 2 chế độ (Shizuku & Accessibility).
5. Commit, tạo tag có chú thích và push:
   ```sh
   git tag -a v1.0.1 -m "Totch 1.0.1"
   git push origin main --tags
   ```
6. (Tùy chọn) Tạo GitHub Release, đính kèm APK tự ký (mục 5).
7. F-Droid tự phát hiện tag mới (`AutoUpdateMode: Version`) và build trong vòng vài ngày — không cần MR mới cho mỗi bản.

## 3. Build tái lập & ký

- Repo **không** chứa cấu hình ký (`signingConfig`) hay keystore. `assembleRelease` tạo APK *unsigned*.
- F-Droid build từ mã nguồn và **tự ký** bằng khóa của F-Droid. Do đó APK từ F-Droid và APK trên GitHub Releases có chữ ký khác nhau → người dùng không cài đè chéo được.
- Sau này có thể bật **reproducible builds**: F-Droid build lại, so sánh với APK bạn ký, nếu trùng byte (trừ chữ ký) thì phát hành APK mang chữ ký của bạn. Cần: thêm `Binaries:` + `AllowedAPKSigningKeys:` vào metadata, cố định JDK/AGP, tránh timestamp/đường dẫn tuyệt đối trong build. Chưa làm ở v1.0.0.
- Gradle wrapper đã ghim `distributionSha256Sum` để F-Droid (và CI) xác minh bản phân phối Gradle.

## 4. Gửi app lên F-Droid (lần đầu)

1. **Đẩy mã lên GitHub** `https://github.com/ow3n/totch` (xác nhận lại owner; nếu khác, sửa URL trong `README.md` và `metadata/io.github.ow3n.totch.yml`). Đảm bảo có `LICENSE`, tag `v1.0.0` trỏ đúng commit có `versionCode = 1`.
2. **Fork** https://gitlab.com/fdroid/fdroiddata (cần tài khoản GitLab).
3. Tạo nhánh mới, chép `metadata/io.github.ow3n.totch.yml` từ repo này vào `metadata/` của fork.
4. **Kiểm tra cục bộ** (khuyến nghị) bằng Docker image fdroidserver, đứng trong thư mục fdroiddata:
   ```sh
   docker run --rm -v "$PWD":/repo registry.gitlab.com/fdroid/fdroidserver:buildserver \
     lint io.github.ow3n.totch
   docker run --rm -v "$PWD":/repo registry.gitlab.com/fdroid/fdroidserver:buildserver \
     build -v -l io.github.ow3n.totch
   docker run --rm -v "$PWD":/repo registry.gitlab.com/fdroid/fdroidserver:buildserver \
     rewritemeta io.github.ow3n.totch   # chuẩn hóa định dạng YAML
   ```
   (Hoặc cài `fdroidserver` qua pip và chạy `fdroid lint` / `fdroid build -v -l io.github.ow3n.totch`.)
   Xem hướng dẫn mới nhất tại https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md.
5. Commit (`New app: Totch`), push, mở **Merge Request** vào `fdroid/fdroiddata`, chọn template *App inclusion* và điền checklist. CI của fdroiddata sẽ chạy lint + build; sửa theo phản hồi của reviewer.
6. Sau khi merge, app thường xuất hiện trên F-Droid sau 1–7 ngày. Lúc đó thay badge placeholder trong `README.md` (đường dẫn đã đúng `f-droid.org/packages/io.github.ow3n.totch/`).

### Lưu ý khi review

- **Accessibility:** reviewer sẽ hỏi vì sao dùng AccessibilityService. Trả lời rõ (đã có trong `full_description.txt`): chỉ để vẽ vùng chạm trên notch và thực hiện global action, không đọc nội dung màn hình, dịch vụ là tùy chọn. Đảm bảo `accessibility_service_config` không bật `canRetrieveWindowContent` và có `android:description` giải thích. F-Droid hiện không gắn anti-feature cho Accessibility nếu dùng đúng mục đích.
- **Shizuku:** thư viện `dev.rikka.shizuku:api/provider` là Apache-2.0, có trên Maven Central → tương thích F-Droid; app Shizuku cũng có trên F-Droid. Không cần anti-feature `NonFreeDep`. Nêu trong MR rằng Shizuku là tùy chọn.
- Không có `INTERNET`, không tracker → `AntiFeatures` để trống. Trước khi gửi, kiểm tra không có dependency Google Play Services / Firebase lọt vào (`./gradlew :app:dependencies`).
- Build server F-Droid đã có JDK 17 nên metadata không cần khối `sudo`. Nếu sau này cần JDK khác, thêm:
  ```yaml
      sudo:
        - apt-get update
        - apt-get install -y openjdk-21-jdk-headless
        - update-alternatives --auto java
  ```
- Ảnh: thêm `fastlane/metadata/android/en-US/images/icon.png` (512×512) và ảnh chụp vào `phoneScreenshots/` — F-Droid đọc trực tiếp từ repo theo tag.

## 5. Tự ký APK cho GitHub Releases

Tạo keystore **một lần**, lưu ngoài repo và sao lưu an toàn (mất là không phát hành bản cập nhật cùng chữ ký được):

```sh
keytool -genkeypair -v -keystore ~/keys/totch-release.jks -alias totch \
  -keyalg RSA -keysize 4096 -validity 10000
```

Build và ký:

```sh
./gradlew assembleRelease
BT=$ANDROID_HOME/build-tools/<phiên-bản>
$BT/zipalign -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk /tmp/totch-aligned.apk
$BT/apksigner sign --ks ~/keys/totch-release.jks --ks-key-alias totch \
  --out totch-1.0.0.apk /tmp/totch-aligned.apk
$BT/apksigner verify --print-certs totch-1.0.0.apk
```

**Không bao giờ commit keystore hay mật khẩu.** Nên thêm `*.jks`, `*.keystore`, `keystore.properties` vào `.gitignore`.
Ghi lại SHA-256 chứng chỉ (từ `apksigner verify --print-certs`) để công bố trong README / dùng cho reproducible builds sau này.
