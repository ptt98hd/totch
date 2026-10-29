# Totch

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/io.github.ptt98hd.totch/)
<!-- Badge is a placeholder until the app is published on F-Droid. -->

**Totch** turns the notch (display cutout / camera hole) of your Android phone into a multi-purpose touch button. Tap, double-tap, long-press or swipe right on the notch to trigger system actions, control media, toggle the flashlight or launch an app.

Free and open source (GPL-3.0-or-later). No ads, no trackers. The network is used only for updates from GitHub Releases: when you tap **Check for updates**, or by the automatic checks you turn on yourself (off by default).

## Features

- **9 gestures** on the notch area: tap, double tap, long press, swipe down / left / right, and swipe down / left / right **& hold**. Each gesture has its own on/off switch that keeps the assigned action.
- **Actions** (one per gesture):
  - System: Back, Home, Recents, Lock screen, Screenshot, Notification shade, Quick Settings, Assistant
  - Flashlight toggle; media Play/Pause, Next, Previous; Volume up/down; show the volume panel
  - Sound mode: ring, vibrate, silent, cycle through them; toggle Do Not Disturb
  - Display: brightness up/down, toggle auto brightness, toggle auto-rotate
  - Connections: toggle Wi-Fi, Bluetooth, mobile data, airplane mode, location
  - Launch any installed app, one of its static app shortcuts, or one of its exported activities
  - Tap an app's Quick Settings tile (a tile that is not in Quick Settings is added for the tap and removed again)
  - Toggle Android's one-handed mode (no shortcut needed; falls back to the launcher's swipe gesture and remembers what works)
- Adjustable sensitivity, manual notch size, position and padding shown live on screen, haptic feedback, start on boot.
- Optionally keeps working in landscape, on the physical notch.
- Material 3 with dynamic color; light / dark / AMOLED black theme; English and Vietnamese (or follow the system).

## How it works

Totch reads raw touch events (`getevent`) with shell privileges through [Shizuku](https://shizuku.rikka.app/), in a foreground service with a persistent notification. **Shizuku is required**: without it Totch does nothing. Touches are not consumed (the system still receives them), so the swipe-down gestures need the optional Accessibility service: it places an invisible overlay on the notch that swallows touches there, otherwise swiping down would also pull the notification shade.

Every other permission is optional and can be switched on or off from the Home screen; Totch grants it through Shizuku when it can, otherwise it opens the matching system page:

- **Accessibility** — to perform global actions (Back, Home, lock screen, ...), open apps from the background, and block touches on the notch for the swipe-down gestures (an empty, invisible overlay). It does not read screen content.
- **Notifications** (Android 13+) — for the foreground service notification. Android closes an app when one of its permissions is revoked, so switching it off restarts Totch.
- **Media control** (notification access) — only to find the active media session and send play/pause/skip commands (more reliable than media key events). It never reads notifications.
- **Do Not Disturb access** — needed by the sound mode actions; Android blocks ringer and Do Not Disturb changes without it.
- **Unrestricted battery** — keeps the gesture service from being killed.
- **Install updates** (install unknown apps) — lets the self-update install the new APK. Switching it off also restarts Totch.

Self-update (Settings › Update › Check for updates) reads the latest [GitHub release](https://github.com/ptt98hd/totch/releases), downloads its signed APK, checks its SHA-256 and installs it through Shizuku (no confirmation, Totch reopens by itself), or hands it to the system installer when Shizuku is not running. Optionally it also checks when the app opens, or daily / weekly / monthly in the background with a notification; both are off by default. The APK is signed with the same key as the F-Droid build, so either can update the other.

## Requirements

- Android 12 (API 31) or newer, phone with a top display cutout
- [Shizuku](https://github.com/RikkaApps/Shizuku) running, with permission granted to Totch

## Building

Requires JDK 21 (same as the F-Droid build server).

```sh
./gradlew assembleRelease   # unsigned release APK in app/build/outputs/apk/release/
./gradlew test lint         # unit tests and lint
```

## Documentation

See [docs/README.md](docs/README.md) (Vietnamese): requirements, architecture, testing and release process.

## License

Copyright (C) 2026 Totch contributors

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. See [LICENSE](LICENSE).
