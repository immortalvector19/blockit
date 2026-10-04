# BlockIt: Technical Architecture & Blocking Layers Deep-Dive

BlockIt is an uncompromising, fully on-device personal application and website blocker for Android. Unlike commercial blockers, BlockIt has **zero internet permission** (`android.permission.INTERNET` is deliberately omitted), zero telemetry, no accounts, and no paywalls.

Because BlockIt is designed for personal sideloading, it leverages the most potent, low-level mechanisms Android provides without compromising for Google Play Store policy restrictions.

---

## 1. Architectural Overview & Defense-in-Depth

BlockIt operates on a multi-layer defense-in-depth model:

```
[User Interaction]
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: Foreground App Detection & Compose System Overlay   │
│ ├─ Primary: AccessibilityService (TYPE_WINDOW_STATE_CHANGED)│
│ ├─ Fallback: UsageStatsManager (interval polling)           │
│ └─ UI: WindowManager ComposeView System Alert Overlay       │
└─────────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 2: On-Device DNS Packet Filtering (VpnService)        │
│ ├─ Split-tunnel: Intercepts UDP port 53 traffic only        │
│ ├─ In-memory RFC 1035 Packet Parser & Synthesizer           │
│ ├─ Blocked domains -> Immediate Synthetic NXDOMAIN / 0.0.0.0│
│ └─ Allowed domains -> Forwarded via protect() socket to ISP │
└─────────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 3: Secondary Browser URL Node Inspection              │
│ ├─ Accessibility Node Extraction for 7 browser engines      │
│ └─ Counters Private DNS / DoT (Port 853) bypasses           │
└─────────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 4: Resilient Time Boundaries & Watchdogs              │
│ ├─ Exact Alarms: AlarmManager.setExactAndAllowWhileIdle     │
│ ├─ Doze Survival & Reboot Re-arm (RECEIVE_BOOT_COMPLETED)   │
│ └─ Periodic Fallback: WorkManager Periodic Watchdog         │
└─────────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 5: Anti-Bypass Strict Mode & Friction                 │
│ ├─ Cognitive Friction: Timed Cooldowns & Typed Passages     │
│ ├─ Tamper Guard: Accessibility intercepts Settings kill     │
│ └─ Uninstall Defense: Device Admin (DevicePolicyManager)    │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Layer 1: App Blocking & WindowManager Overlay

### Mechanism
- **Foreground Interception**: `BlockAccessibilityService` listens for `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOWS_CHANGED`. When an application switches foreground windows, the package name is extracted within milliseconds (<16ms frame budget).
- **Evaluation**: The package name is checked against `RuleEvaluator`, which maintains an in-memory cached bitmask/set of active rules from enabled profiles, active recurring schedules, and focus sessions.
- **Overlay Presentation**: When a block rule triggers, `OverlayController` mounts a Compose `ComposeView` directly into the Android `WindowManager` using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
- **OverlayLifecycleOwner**: Compose requires `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner`. Because a `WindowManager` view runs outside an `Activity`, `OverlayController` binds a custom `OverlayLifecycleOwner` to provide Compose with complete lifecycle and animation support.
- **Home Fallback**: If the user dismisses the overlay or attempts to navigate around it, `BlockAccessibilityService` performs `performGlobalAction(GLOBAL_ACTION_HOME)` to return to the Android launcher.
- **UsageStats Fallback**: If the accessibility service is momentarily disconnected by an aggressive OEM task killer, `UsageStatsDetector` polls `UsageStatsManager.queryUsageStats()` to detect foreground switches and launch `BlockOverlayActivity`.

### Limits & Failure Modes
- **App Cloning / Dual Messenger**: Sideloaded parallel apps (e.g., Samsung Dual Messenger) use package suffixes like `com.instagram.android:2`. BlockIt matches against base package identifiers.
- **Split Screen / Multi-Window**: In multi-window mode, the non-focused window may remain visible. BlockIt monitors both active window hierarchies to ensure the overlay occludes distracting apps in split-screen mode.

---

## 3. Layer 2: On-Device DNS Filtering (VpnService)

### Mechanism
- **Split-Tunnel Local TUN**: `LocalDnsVpnService` registers a virtual network interface (`VpnService.Builder`) configured with:
  - Address: `10.10.0.1/32`
  - Route: `0.0.0.0/0`
  - DNS Server: `10.10.0.1` (directs all system DNS queries into the TUN interface)
- **Zero Remote Traffic**: Only UDP port 53 packets are inspected. Non-DNS TCP/UDP payloads are never captured or rerouted off the device.
- **RFC 1035 Parser & Builder**:
  - `DnsPacketParser` decodes the IPv4 header, UDP header, DNS transaction ID, flags, and Question section (labels and QNAME).
  - Domain names are evaluated against active profile patterns using `DomainMatcher`, which supports root domains, wildcards (`*.reddit.com`), and multi-level subdomains.
- **Synthetic NXDOMAIN**: If blocked, `DnsPacketBuilder` synthesizes an immediate DNS Response with `RCODE = 3 (Name Error / NXDOMAIN)` and writes it back to the TUN descriptor. The browser or client app immediately terminates the connection attempt without establishing a TCP socket.
- **Protected Upstream Forwarding**: Allowed queries are forwarded to the system network DNS server via a standard UDP `DatagramSocket` protected by `VpnService.protect(socket)`. This prevents VPN routing loops without requiring internet permissions for BlockIt.

### Limits & Failure Modes
- **Hardcoded IP Addresses**: If a user enters a raw IPv4/IPv6 address (e.g. `http://192.168.1.1`), DNS is bypassed. This is addressed by Layer 3.
- **Private DNS / DNS-over-TLS (DoT)**: Android 9+ enables opportunistic Private DNS by default, sending encrypted DNS queries to port 853 (TCP/TLS). Android's OS resolver bypasses port 53 TUN interfaces when Private DNS is active. This is solved by Layer 3.

---

## 4. Layer 3: Secondary Browser URL Extraction

### Mechanism
- **Accessibility View Tree Traversal**: `BrowserUrlDetector` inspects the node hierarchy when any of the top 7 Android browser engines are in the foreground:
  - Google Chrome: `com.android.chrome` (View ID: `url_bar`)
  - Mozilla Firefox: `org.mozilla.firefox` (View ID: `mozac_browser_toolbar_url_view`)
  - Brave Browser: `com.brave.browser` (View ID: `url_bar`)
  - Samsung Internet: `com.sec.android.app.sbrowser` (View ID: `location_bar_edit_text`)
  - Microsoft Edge: `com.microsoft.emmx` (View ID: `url_bar`)
  - Opera: `com.opera.browser` (View ID: `url_field`)
  - DuckDuckGo: `com.duckduckgo.mobile.android` (View ID: `omnibar_text_input`)
- **Address Sanitization**: Strips URL schemes (`https://`, `http://`), ports, and path components to normalize domain names for `DomainMatcher`.
- **Immediate Overlay Mount**: If a URL matches an active blocked domain, the full-screen block overlay is mounted immediately and the browser window is sent back or kicked to the home screen.

### Limits & Tradeoffs
- **Custom / Obscure Browsers**: Niche browsers with custom view IDs that do not expose standard URL text nodes may evade accessibility URL extraction. Layer 2 (VpnService) catches them provided Private DNS is disabled or set to automatic.

---

## 5. Layer 4: Recurring Schedules & Focus Sessions

### Mechanism
- **Exact Alarms**: `ScheduleManager` calculates the precise timestamp of the next boundary (either the start or end of an active schedule window across all enabled profiles).
- **Doze Survival**: Configures alarms using `AlarmManager.setExactAndAllowWhileIdle()`. When the alarm fires, `AlarmReceiver` wakes the CPU, updates profile states in Room, re-computes `RuleEvaluator` active sets, and schedules the subsequent boundary.
- **Reboot Resilience**: `BootReceiver` listens for `android.intent.action.BOOT_COMPLETED` and `ACTION_LOCKED_BOOT_COMPLETED`. Upon reboot, it immediately loads active focus sessions and schedules from Room, re-arms exact alarms, and starts background services.
- **WorkManager Watchdog**: `ServiceWatchdogWorker` runs periodic 15-minute background jobs to verify service health and ensure alarms have not been dropped by OS power management.

---

## 6. Layer 5: Strict Mode & Anti-Bypass Defenses

### Mechanism
- **Friction Challenges**:
  - `ChallengeType.COOLDOWN`: Imposes a mandatory 30–120 second waiting countdown before an active profile or focus session can be toggled off or modified.
  - `ChallengeType.TYPED_PASSAGE`: Requires the user to type a reflective pledge (e.g., "I choose focus and clarity over momentary distraction") character-for-character before actions proceed.
- **Device Admin (Anti-Uninstall)**:
  - `BlockItDeviceAdminReceiver` implements `DeviceAdminReceiver`.
  - When enabled in Strict Mode, Android's system package installer forbids uninstallation from the home screen or settings while Device Admin is active.
- **Settings Tampering Guard**:
  - `BlockAccessibilityService` detects when the user opens system Settings pages attempting to Force Stop BlockIt or revoke accessibility/admin permissions.
  - Intercepts clicks on "Force Stop" or "Deactivate" and triggers the friction challenge or returns to Home.

---

## 7. OEM Battery Restrictions & Workarounds

Device manufacturers often implement proprietary aggressive task killers that terminate background services when the screen locks.

| OEM | System Killer Mechanism | Recommended User Setting |
| :--- | :--- | :--- |
| **Samsung (One UI)** | Sleeping Apps & Auto-Optimize | Settings > Apps > BlockIt > Battery > **Unrestricted**. Background usage limits > **Never auto sleeping apps** > Add BlockIt. |
| **Xiaomi / POCO (HyperOS / MIUI)** | Autostart & MIUI Battery Saver | App Info > **Autostart: ON**. Battery saver > **No restrictions**. Lock BlockIt card in Recent Apps. |
| **OnePlus / OPPO / Realme** | Background Freezing | Settings > Apps > BlockIt > Battery > Enable **Allow background activity** & **Allow auto-launch**. |
| **Huawei / Honor** | App Launch Management | Settings > Battery > App launch > Set BlockIt to **Manage manually** (enable all 3 toggles). |
| **Google Pixel / Stock** | Doze Bucket Allocation | Settings > Apps > BlockIt > App battery usage > **Unrestricted**. |
