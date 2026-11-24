# Workstation Net Meter

A lightweight, free and privacy-friendly Android app that shows your **live internet
speed in the Android status bar**, updated every second, using a dynamically rendered
notification small icon.

- **Package:** `com.workstation.netmeter`
- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Min Android:** 8.0 (API 26)
- **Target device:** Samsung Galaxy A03s, Android 13 (One UI Core 5.1, ARM64, 720×1600)
- **Ads / analytics / tracking:** none

---

## Features

### Live status-bar speed indicator
- A foreground service reads `TrafficStats.getTotalRxBytes()` / `getTotalTxBytes()`
  once per second and computes real throughput.
- A monochrome bitmap is rendered with `Canvas`/`Paint` and set as the notification
  **small icon** through `NotificationCompat` + `IconCompat`, so Android draws the
  changing speed directly in the status-bar notification icon area.
- Download speed is always shown (`↓850B`, `↓45K`, `↓223K`, `↓1.2M`); when
  *Show Up/Down Speed* is enabled the compact combined representation is rendered
  (upload on the top line, download on the bottom line).
- The expanded notification always shows both values, for example:

  ```
  ↓ Download: 223 KB/s
  ↑ Upload: 12 KB/s
  ```

- The icon auto-scales its unit for legibility; the unit chosen in **Settings →
  Speed Units** is applied consistently to the dashboard and the notification text.

### Foreground monitoring service
- Start/stop from the dashboard; keeps running when the app is minimised.
- Low-importance, silent notification channel (no sound, no vibration, no badge).
- Android 13+ `POST_NOTIFICATIONS` runtime permission handling.
- Android 14+ foreground-service type `specialUse` with the matching permission and
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` declaration.
- `START_STICKY` + optional **Start on Boot** (`BOOT_COMPLETED` receiver).
- No wake locks, no root, no VPN, no accessibility tricks.

### Settings
| Setting | Description |
| --- | --- |
| Hide Lock Screen Notification | Uses `VISIBILITY_PRIVATE` to hide notification details on the lock screen |
| Notification only when connected | Uses `NET_CAPABILITY_VALIDATED` (real internet validation, not just "connected") |
| Show Up/Down Speed | Default **ON**; when off, only download speed is shown |
| Notification Tap Action | Open app (default) / Stop monitoring / Do nothing |
| Speed Units | B/s, KB/s, MB/s, bps, Kbps, Mbps (default B/s) |
| Monthly Data Usage Limit | Configurable in MB; `0` disables the limit |
| Starting Hour of Day | Daily usage reset boundary (also used for the monthly period start) |
| Language | English, हिन्दी, Hinglish |
| Notification Accent | System Default (live rendered icon) / Classic Icon |
| Theme | System Default / Dark / Light |
| Advanced | Network & Wi-Fi information, sampling interval, start on boot, battery optimisation, Usage Access, diagnostics (last recorded error) |

### Data usage
- Usage is accumulated from the device traffic counters and stored with Jetpack
  DataStore (no Room database is needed for this data volume).
- Daily and monthly rollover honour the configured starting hour.
- The counters are **total device traffic (all networks)**. Mobile-only figures
  require the special **Usage Access** app-op and are shown separately through
  `NetworkStatsManager`; when it is not granted the app says so instead of
  presenting total traffic as mobile traffic.

### Privacy
- No ads, no ad SDK, no analytics, no crash reporters, no third-party services.
- All data stays on the device. No account, no network calls.
- Location permission is only used to read the Wi-Fi SSID on Android versions that
  require it; it is never requested just to measure speed.

---

## Required permissions

| Permission | Why |
| --- | --- |
| `INTERNET` | Not used to measure speed (counters are local); declared for completeness |
| `ACCESS_NETWORK_STATE` | Detect Wi-Fi / mobile / Ethernet and validated internet |
| `ACCESS_WIFI_STATE` | Read Wi-Fi signal strength and link speed |
| `POST_NOTIFICATIONS` | **Required on Android 13+** for the status-bar indicator |
| `FOREGROUND_SERVICE` | Run the always-on monitor |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ foreground service type |
| `RECEIVE_BOOT_COMPLETED` | Optional "Start on Boot" |
| `ACCESS_FINE_LOCATION` | Wi-Fi SSID only (best effort; optional) |
| `PACKAGE_USAGE_STATS` | Optional mobile-data breakdown via Usage Access (special access) |

`POST_NOTIFICATIONS` is the important one: without it the foreground notification
cannot be shown, so no status-bar indicator appears. The app detects this state and
shows an explanation instead of pretending that monitoring is active.

---

## Project structure

```
app/src/main/java/com/workstation/netmeter/
├── MainActivity.kt                 # Compose host, locale + theme
├── NetMeterApp.kt                  # process-wide singletons
├── network/
│   ├── TrafficStatsReader.kt       # abstraction over TrafficStats
│   ├── SpeedCalculator.kt          # pure throughput math
│   ├── NetworkStateMonitor.kt      # ConnectivityManager / NetworkCallback
│   └── SpeedSample.kt
├── service/
│   ├── SpeedMonitorService.kt      # foreground 1 Hz sampling loop
│   ├── SpeedServiceController.kt   # start/stop helper
│   ├── SpeedStateHolder.kt         # shared StateFlows
│   └── BootReceiver.kt
├── notification/
│   ├── SpeedIconRenderer.kt        # monochrome bitmap renderer
│   └── SpeedNotificationManager.kt # channel + notification building
├── settings/
│   ├── AppSettings.kt              # preferences + enums
│   ├── SettingsRepository.kt       # DataStore-backed preferences
│   ├── LocaleManager.kt            # per-app language
│   └── DataStoreExt.kt
├── usage/
│   ├── DataUsageManager.kt         # usage accumulation + rollover
│   ├── UsageSchedule.kt            # pure reset-boundary math
│   ├── DataUsage.kt
│   └── MobileUsageReader.kt        # NetworkStatsManager (Usage Access)
├── ui/
│   ├── theme/Theme.kt
│   ├── dashboard/                  # DashboardScreen + DashboardViewModel
│   ├── settings/                   # SettingsScreen + SettingsViewModel
│   └── components/                 # reusable setting rows
└── util/                           # SpeedUnit, SpeedFormatter, Permissions

app/src/test/java/com/workstation/netmeter/   # JVM unit tests
```

---

## Status

| Item | Value |
| --- | --- |
| Repository | https://github.com/Akashop56/WORKSTATION-NET-METER |
| CI | https://github.com/Akashop56/WORKSTATION-NET-METER/actions (`Android CI`) |
| Release | https://github.com/Akashop56/WORKSTATION-NET-METER/releases/tag/v1.0.0 |
| APK | `app-debug.apk` (9.5 MB, debug-signed) |
| Unit tests (CI) | `tests=29 failures=0 errors=0 skipped=0` |

APK metadata verified by CI (`aapt2 dump badging` / `apksigner verify`):

```
package: name='com.workstation.netmeter' versionCode='1' versionName='1.0.0'
minSdkVersion:'26'   targetSdkVersion:'34'   compileSdkVersion:'34'
native-code: 'arm64-v8a' 'armeabi-v7a' 'x86' 'x86_64'
launchable-activity: name='com.workstation.netmeter.MainActivity'
signature: V2, CN=Android Debug (automatic debug keystore)
```

## Build

Requirements: JDK 17 and the Android SDK (platform 34).

```bash
./gradlew assembleDebug       # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest   # JVM unit tests
./gradlew lintDebug           # Android lint
```

No signing configuration is required for the debug build — it is signed with the
automatic debug keystore.

## GitHub Actions

`.github/workflows/android.yml` runs on every push to `main` (and pull requests):

1. Checkout
2. Set up JDK 17 (Temurin)
3. Set up the Android SDK
4. `./gradlew testDebugUnitTest`
5. `./gradlew lintDebug`
6. `./gradlew assembleDebug`
7. Upload `workstation-net-meter-debug` artifact (the APK)

`.github/workflows/release.yml` builds the debug APK and attaches it to a GitHub
Release; run it manually from the Actions tab with a tag such as `v1.0.0`.

## Monitoring instructions

1. Open the app and press **Start Monitoring**.
2. On Android 13+, grant the notification permission when prompted.
3. The status bar now shows the live speed, and the notification shade shows both
   download and upload values.
4. Press **Stop Monitoring** to remove the indicator.

### Samsung One UI battery guidance

One UI may stop background apps to save battery. To keep the indicator alive:

- Open **Settings → Apps → Workstation Net Meter → Battery** and set
  **Allow background activity** / **Unrestricted** (disable *Put app to sleep*).
- Or open the app's **Settings → Advanced → Battery optimization** row and exempt
  the app from battery optimization.
- Avoid the system "Deep sleeping apps" list for this app.

## Technical notes & known constraints

- **Traffic counters are device-wide and cumulative since boot.** They cannot
  attribute traffic to a single app, and a mobile-only breakdown requires the
  special Usage Access permission.
- **Speed is measured from counter deltas**, so the first sample after starting is
  always `0`. Counter resets (reboot, unsupported counters) are detected and
  re-baselined instead of producing negative or fake speeds.
- Units are decimal (1 KB = 1000 B, 1 Kb = 1000 b). Byte-based and bit-based
  families are offered separately.
- `TrafficStats` reports `UNSUPPORTED` (-1) on devices without the counters; the
  app then shows `0 B/s` instead of inventing a value.
- The compact status-bar icon auto-scales its own unit (B/K/M) so the text stays
  legible at icon size; the unit chosen in settings is used for the numeric text
  in the dashboard and the expanded notification.
- On Android 14 the service uses the `specialUse` foreground-service type to avoid
  the 6-hour limit that applies to `dataSync` services.
- Only the debug APK is produced by CI; release signing is not configured.

## Testing

Unit tests cover download/upload calculation, sampling intervals, zero traffic,
negative counter deltas, counter resets, unsupported TrafficStats values,
overflow safety, unit conversion, speed formatting, and daily/monthly usage
boundaries. These are **automated JVM tests** (`./gradlew testDebugUnitTest`);
they are not a substitute for physical-device testing. The status-bar indicator
itself was implemented against the documented Android notification APIs but was
not exercised on a physical Samsung device in an automated run.
