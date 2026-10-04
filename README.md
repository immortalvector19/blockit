# BlockIt 🛡️
### A Free, 100% On-Device App & Website Blocker for Android

BlockIt is an uncompromising, private personal-use app and website blocker for Android. Engineered to outperform commercial blockers like BlockSite without ads, paywalls, accounts, tracking, or background telemetry.

---

## ⚡ Core Philosophy & Guarantees

- **Zero Internet Permission**: The Android manifest contains **no** `android.permission.INTERNET`. BlockIt cannot send a single byte off your device.
- **100% On-Device**: All DNS filtering, accessibility inspection, schedule calculations, and usage insights execute locally on your processor.
- **No Monetization / No Slop**: No subscriptions, no ads, no in-app purchases, no Google Analytics, no Firebase telemetry.
- **Built for Sideloading**: Designed for personal use without compromising effectiveness for Play Store policy restrictions.

---

## 🚀 Features

1. **Profile & Blocklist Management**
   - Create focused profiles (e.g. *Deep Work*, *Sleep*, *Study*).
   - Multi-select installed apps to block.
   - Domain matching engine supporting exact domains (`reddit.com`), wildcards (`*.youtube.com`), and subdomains (`m.facebook.com`).

2. **Foreground App Blocking & Calm Compose Overlay**
   - Sub-millisecond foreground app interception via `AccessibilityService`.
   - Mounts a calm Compose Material 3 full-screen overlay directly via `WindowManager` without killing processes.
   - Secondary `UsageStatsManager` polling fallback if accessibility is ever temporarily restarted.

3. **On-Device Website & DNS Filtering**
   - **Primary Layer**: Local split-tunnel `VpnService` intercepting UDP port 53 DNS queries on localhost. Synthesizes instant `NXDOMAIN` (Name Error) responses for blocked domains while forwarding allowed queries via protected sockets.
   - **Secondary Layer**: Real-time browser URL extraction across 7 major browser engines (Chrome, Firefox, Brave, Samsung Internet, Edge, Opera, DuckDuckGo) to counter Android 9+ Private DNS / DoT bypasses.

4. **Recurring Schedules & Quick Focus Sessions**
   - Weekly schedules with customizable day-of-week bitmasks and 24-hour time ranges.
   - Quick one-off Pomodoro focus sessions (15m, 25m, 45m, 60m).
   - Powered by `AlarmManager.setExactAndAllowWhileIdle()` to survive Android Doze mode.
   - Automatic re-arming on reboot (`RECEIVE_BOOT_COMPLETED`) and periodic WorkManager health watchdogs.

5. **Anti-Bypass Strict Mode**
   - **Timed Cooldowns**: Enforce 30–120 second waiting periods before disabling a rule or ending a session early.
   - **Typed Challenges**: Require typing a custom reflective pledge character-by-character to unlock settings.
   - **Device Admin Protection**: Activates Android Device Administration (`DevicePolicyManager`) to prevent uninstallation from the launcher or settings during locked sessions.
   - **Settings Tampering Guard**: Detects attempts to Force Stop or revoke permissions in system settings and returns to the home screen.

6. **Local Usage Insights & Time Reclaimed**
   - Metrics on distraction attempts blocked today, this week, and all-time.
   - Estimated focus time reclaimed (~5 minutes preserved per distraction prevented).
   - Breakdown of top blocked apps and domains with on-device audit log clearing.

7. **Local Backup & Restore + Setup Guide**
   - Full configuration export/import to local JSON files via the Android Storage Access Framework (SAF).
   - Interactive permission onboarding checklist.
   - OEM background battery killer bypass guide for Samsung (One UI), Xiaomi (HyperOS/MIUI), OnePlus (OxygenOS), and Huawei (EMUI).

---

## 🏗️ Architecture & Tech Stack

- **Language & Runtime**: Kotlin 1.9, Java 17, Android Min SDK 26 (Android 8.0), Target SDK 34 (Android 14).
- **UI Framework**: Jetpack Compose, Material 3, Single-Activity architecture (`MainActivity`).
- **State & Concurrency**: Kotlin Coroutines, StateFlow, Unidirectional Data Flow (UDF).
- **Persistence**: Room Database (SQLite with foreign-key cascade deletions) & DataStore Preferences.
- **Dependency Injection**: Clean manual dependency injection (`AppContainer`) for zero annotation-processing overhead and instant clean builds.
- **Test Suite**: JUnit 4 unit tests covering domain pattern matching, schedule evaluation, DNS packet parsing, and JSON serialization.

---

## 🛠️ Building & Sideloading

### Prerequisites
- JDK 17
- Android SDK 34 (Installed via Android Studio or command-line tools)

### Build Debug APK
To run all unit tests and assemble the signed debug APK:

```bash
# Windows
.\gradlew.bat testDebugUnitTest assembleDebug

# Linux / macOS
./gradlew testDebugUnitTest assembleDebug
```

The signed debug APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB
Connect your phone with USB Debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Quick Permissions Grant via ADB (Optional)
If you prefer configuring permissions instantly via terminal rather than tapping through the setup guide:

```bash
# Grant Draw Over Other Apps (Overlay)
adb shell appops set com.blockit.debug SYSTEM_ALERT_WINDOW allow

# Grant Usage Stats Access
adb shell appops set com.blockit.debug GET_USAGE_STATS allow

# Whitelist from Battery Optimization (Doze exemption)
adb shell dumpsys deviceidle whitelist +com.blockit.debug

# Enable BlockIt Accessibility Service
adb shell settings put secure enabled_accessibility_services com.blockit.debug/com.blockit.service.accessibility.BlockAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

---

## 📖 In-Depth Documentation

For technical details on how each blocking layer functions, its packet-level mechanics, and known limits:
- See [docs/BLOCKING_LAYERS.md](docs/BLOCKING_LAYERS.md)
