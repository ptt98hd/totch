#!/usr/bin/env bash
# Build, sign and publish the release APK for a tag (reproducible builds, see docs/04-release-fdroid.md).
#
#   scripts/release-apk.sh v1.0.1            # build + sign into dist/
#   scripts/release-apk.sh v1.0.1 --publish  # ... and upload to the GitHub release
#
# Signing config is read from $TOTCH_KEYSTORE_PROPERTIES (default ~/.config/totch/keystore.properties),
# never from the repository.
set -euo pipefail

tag="${1:?usage: $0 <tag> [--publish]}"
publish="${2:-}"
version="${tag#v}"
root="$(cd "$(dirname "$0")/.." && pwd)"
props="${TOTCH_KEYSTORE_PROPERTIES:-$HOME/.config/totch/keystore.properties}"
sdk="${ANDROID_HOME:-$HOME/Android/Sdk}"
build_tools="$(ls -d "$sdk"/build-tools/* | sort -V | tail -1)"
out="$root/dist/totch-$version.apk"
export JAVA_HOME="$(cd "$root" && mise where java)"
export PATH="$JAVA_HOME/bin:$PATH"

prop() { grep "^$1=" "$props" | cut -d= -f2-; }

# Build from a clean clone of the tag so the APK matches what F-Droid builds. A real clone (not a
# worktree) matters: AGP embeds the git revision in META-INF/version-control-info.textproto.
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
git clone -q "$root" "$work/src"
git -C "$work/src" -c advice.detachedHead=false checkout -q "$tag"
echo "sdk.dir=$sdk" > "$work/src/local.properties"
(cd "$work/src" && ./gradlew --no-configuration-cache -q :app:assembleRelease)

mkdir -p "$root/dist"
unsigned="$work/src/app/build/outputs/apk/release/app-release-unsigned.apk"
cp "$unsigned" "$root/dist/totch-$version-unsigned.apk" # for `apksigcopier compare` against F-Droid's build
"$build_tools/apksigner" sign --alignment-preserved --v4-signing-enabled false \
    --ks "$(prop storeFile)" --ks-key-alias "$(prop keyAlias)" \
    --ks-pass "pass:$(prop storePassword)" --key-pass "pass:$(prop keyPassword)" \
    --out "$out" "$unsigned"
"$build_tools/apksigner" verify --print-certs "$out" | grep -i "SHA-256"
sha256sum "$out"

if [[ "$publish" == "--publish" ]]; then
    notes="$root/fastlane/metadata/android/en-US/changelogs/$(grep -oP 'versionCode = \K\d+' "$work/src/app/build.gradle.kts").txt"
    if gh release view "$tag" >/dev/null 2>&1; then
        gh release upload "$tag" "$out" --clobber
    else
        gh release create "$tag" "$out" --title "Totch $version" --notes-file "$notes"
    fi
fi
