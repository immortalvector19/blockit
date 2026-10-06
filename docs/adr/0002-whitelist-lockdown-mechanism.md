# ADR 0002: Whitelist Mode Lockdown & Kiosk Enforcement Mechanism

## Status
Accepted

## Context
FocusShield provides a Whitelist Mode where a user specifies an allowlist of permitted apps (e.g. Phone, Notes, Calculator), while all other applications and distracting launcher surfaces on the device are locked out.

In earlier builds, Whitelist Mode behaved like a reactive Blacklist with an overlay, and did not immediately seize control of the device when started. The user could press Home or Recents to access their normal launcher, browse unpermitted apps, and the lockdown only enforced after the screen was turned off and on.

We must evaluate three potential architectural mechanisms available on standard Android to enforce the Whitelist Lockdown:
1. **Accessibility Redirect to a dedicated Focus Home screen**
2. **Temporary Home/Launcher Role (`RoleManager.ROLE_HOME` / Default Launcher)**
3. **Screen Pinning (`Activity.startLockTask()` / App Pinning)**

---

## Evaluation of Candidate Approaches

### Candidate 1: Accessibility Redirect + Dedicated Focus Home Activity (Chosen)
- **Mechanism**:
  - FocusShield implements a dedicated, single-task `FocusHomeActivity`.
  - When a Whitelist focus session starts, `FocusHomeActivity` is immediately launched to the foreground.
  - `BlockAccessibilityService` continuously listens to `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOWS_CHANGED`.
  - If any unpermitted package, system launcher (`Intent.CATEGORY_HOME`), or Recents navigation is detected during an active whitelist session, the Accessibility Service immediately redirects back to `FocusHomeActivity` via `startActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP`.
- **Official Android Documentation Grounding**:
  - `AccessibilityService` has system-level visibility over active window transitions and hierarchy events across all apps without requiring Device Administrator or Device Owner.
  - `AccessibilityService.performGlobalAction()` and `Context.startActivity()` are standard APIs supported from API 21 through API 35+.
- **Advantages**:
  - **Zero additional user friction**: Accessibility and Overlay permissions are already granted during engine setup. No extra permission prompts or system dialogs appear when starting a session.
  - **Works on 100% of standard consumer Android devices**: Does not require Device Owner (DPC), Knox, or root.
  - **Seamless third-party app launching**: Permitted apps (e.g. Anikku, Bolt, Calculator) can be launched freely from Focus Home without breaking the sandbox.
- **Limitations & Mitigations**:
  - *Transient window visibility*: When an unpermitted app is clicked from an external notification or widget, the app may display for ~50–100ms before the accessibility event fires and redirects to Focus Home.
  - *Mitigation*: Focus Home covers the launcher so external apps cannot be clicked from the home screen, and the notification shade / settings tampering can be guarded by Accessibility.

---

### Candidate 2: Temporary Home / Launcher Role (`RoleManager.ROLE_HOME`)
- **Mechanism**:
  - FocusShield declares an `<intent-filter>` for `Intent.ACTION_MAIN` and `Intent.CATEGORY_HOME`.
  - On starting a Whitelist session, FocusShield requests the default home role via `RoleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)` (Android 10+) or prompts the user via `Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS`.
  - When the session ends, the app prompts the user to switch back to their original launcher.
- **Official Android Documentation Grounding**:
  - Android security policy strictly prohibits applications from silently changing or acquiring `ROLE_HOME` without user interaction.
- **Why Rejected**:
  - **Severe User Friction**: On *every single session start*, Android displays a full-screen system prompt: *"Set FocusShield as default Home app?"*.
  - On *every session end*, the user must go through another system dialog or settings screen to restore Nova/OneUI/Pixel launcher.
  - If a device reboots or an unhandled crash occurs in Strict/Locked mode, the user could be trapped without their primary launcher.

---

### Candidate 3: Screen Pinning (`Activity.startLockTask()` / App Pinning)
- **Mechanism**:
  - FocusShield calls `Activity.startLockTask()` when entering Whitelist mode.
  - Android locks the display to FocusShield, disabling the status bar and home navigation gestures.
- **Official Android Documentation Grounding**:
  - Android documentation specifies that for personal, unmanaged apps (apps that are not Device Owner / DPC):
    1. The user must manually confirm a system dialog to pin the screen.
    2. Any user can exit pinning at any time by holding Back + Recents (or swiping up and holding).
    3. **Fatal constraint for multi-app Whitelists**: While an unmanaged app is pinned, Android restricts activity transitions outside the pinned app's task. If the user taps a permitted third-party app (e.g. Calculator, Slack, Phone), Android refuses to launch the external activity until the screen is unpinned.
- **Why Rejected**:
  - Breaks launching permitted third-party apps in consumer mode.
  - Easily bypassed by holding the system unpin buttons.

---

## Decision

We adopt **Candidate 1: Accessibility Redirect + Dedicated Focus Home Activity**.

1. **Immediate Lockdown**: The instant a Whitelist session starts, FocusShield synchronously updates `RuleEvaluator` and immediately brings `FocusHomeActivity` to the foreground (`< 1 second`).
2. **Kiosk Home UI**: `FocusHomeActivity` displays:
   - Only permitted apps for the active whitelist profile.
   - A prominent **Phone / Emergency Call** button opening the system dialer.
   - Monospace countdown with progress ring and Stop friction.
3. **Accessibility Enforcement**:
   - `SystemAppResolver` distinguishes between critical system components (Dialer, Keyboards, SystemUI) and Home Launchers.
   - In Whitelist Mode, third-party and OEM home launchers are **not** exempt; visiting them triggers an immediate redirect back to `FocusHomeActivity`.
   - In-call UI (`com.android.incallui`, `TelecomManager.defaultDialerPackage`) and user-configured "always allowed" apps are always permitted.
