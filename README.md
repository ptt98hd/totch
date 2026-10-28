# Totch

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/io.github.ow3n.totch/)
<!-- Badge is a placeholder until the app is published on F-Droid. -->

**Totch** turns the notch (display cutout / camera hole) of your Android phone into a multi-purpose touch button. Tap, double-tap, long-press or swipe right on the notch to trigger system actions, control media, toggle the flashlight or launch an app.

Free and open source (GPL-3.0-or-later). No ads, no trackers, no `INTERNET` permission.

## Features

- **6 gestures** on the notch area: tap, double tap, long press, swipe down, swipe left, swipe right.
- **Actions** (one per gesture, or "Do nothing"):
  - System: Back, Home, Recents, Lock screen, Screenshot, Notification shade, Quick Settings
  - Flashlight toggle; media Play/Pause, Next, Previous; Volume up/down
  - Launch any installed app
- Adjustable sensitivity, manual notch region, haptic feedback.
- Material 3 with dynamic color and dark theme; English and Vietnamese.
- Automatically disabled in landscape.

## Detection modes

| | Shizuku mode | Accessibility mode |
|---|---|---|
| How | Reads raw touch events (`getevent`) with shell privileges via [Shizuku](https://shizuku.rikka.app/) | Places a small accessibility overlay exactly over the notch |
| Consumes touches | **No** — the system still receives them, so e.g. a swipe down also pulls the notification shade | **Yes**, but only inside the notch area |
| Needs | Shizuku running and permission granted; persistent notification (foreground service) | Accessibility service enabled |

The Accessibility service is used **only** to draw the touch area over the notch and to perform global actions (Back, Home, lock screen, ...). It does not read screen content.

## Requirements

- Android 12 (API 31) or newer, phone with a top display cutout
- Either [Shizuku](https://github.com/RikkaApps/Shizuku) or the Totch Accessibility service enabled

## Building

Requires JDK 17.

```sh
./gradlew assembleRelease   # unsigned release APK in app/build/outputs/apk/release/
./gradlew test lint         # unit tests and lint
```

## Documentation

See [docs/README.md](docs/README.md) (Vietnamese): requirements, architecture, testing and release process.

## License

Copyright (C) 2026 Totch contributors

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. See [LICENSE](LICENSE).
