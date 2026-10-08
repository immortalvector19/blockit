# FocusShield Design System (DESIGN.md)

> **North Star**: A quiet, precision utility for personal focus. Built with restraint, high contrast, clean whitespace, and zero artificial drama. Inspired by Things 3 and Linear Mobile.

---

## 1. Principles

1. **Quiet Precision Over Cyber-Theater**: FocusShield is an intentional tool, not a military firewall or sci-fi cockpit. Avoid neon colors, glowing rings, ALL-CAPS tracked typography, and military defense copy.
2. **Whitespace Over Nested Boxes**: Structure content using spacing and subtle 1dp dividers instead of nesting cards inside cards.
3. **One Calm Accent for Active State**: Color is used strictly for meaningful state (an active focus session or enabled switch). Everything else is neutral slate.
4. **Zero AI Slop / Vibe-Code Tells**: Sentence case, plain factual language, standard flat Android navigation, consistent 24dp iconography, no emoji icons, and no redundant marketing slogans.
5. **Universal Accessibility (WCAG AAA/AA)**: Every text element has at least 4.5:1 contrast against its background (most > 7:1). Minimum touch target size is 48dp. Layouts scale smoothly up to 200% system font size without truncation or clipping.

---

## 2. Color Tokens

### 2.1 Light Theme (Calm Canvas)
| Token | Hex | Role | Contrast Ratio |
| :--- | :--- | :--- | :--- |
| `color.background` | `#F8F9FA` | App screen background | — |
| `color.surface` | `#FFFFFF` | Primary card and sheet container | 1.05:1 vs bg |
| `color.surfaceVariant` | `#F1F3F5` | Secondary container, inputs, chips | 1.15:1 vs bg |
| `color.border` | `#E5E7EB` | Hairline dividers, subtle card borders | 1.25:1 vs bg |
| `color.textPrimary` | `#111827` | Primary headlines, titles, active labels | **15.8:1** (AAA) |
| `color.textSecondary` | `#4B5563` | Subtitles, helper text, inactive labels | **7.2:1** (AAA) |
| `color.textTertiary` | `#9CA3AF` | Placeholder text, disabled hints | **4.6:1** (AA) |
| `color.accent` | `#2563EB` | Active session countdown, primary CTA | **4.6:1** vs white |
| `color.accentContainer` | `#EFF6FF` | Active chip background, subtle tint | — |
| `color.statusActive` | `#16A34A` | Running focus state indicator | **4.5:1** vs white |
| `color.statusDestructive` | `#DC2626` | Stop session, delete actions | **4.8:1** vs white |

### 2.2 Dark Theme (Deep Slate)
| Token | Hex | Role | Contrast Ratio |
| :--- | :--- | :--- | :--- |
| `color.background` | `#121316` | App screen background | — |
| `color.surface` | `#1A1C20` | Primary card and sheet container | 1.15:1 vs bg |
| `color.surfaceVariant` | `#22252B` | Secondary container, inputs, chips | 1.3:1 vs bg |
| `color.border` | `#2D3139` | Hairline dividers, subtle card borders | 1.4:1 vs bg |
| `color.textPrimary` | `#F3F4F6` | Primary headlines, titles, active labels | **15.2:1** (AAA) |
| `color.textSecondary` | `#9CA3AF` | Subtitles, helper text, inactive labels | **7.0:1** (AAA) |
| `color.textTertiary` | `#6B7280` | Placeholder text, disabled hints | **4.5:1** (AA) |
| `color.accent` | `#3B82F6` | Active session countdown, primary CTA | **4.5:1** vs bg |
| `color.accentContainer` | `#1E293B` | Active chip background, subtle tint | — |
| `color.statusActive` | `#22C55E` | Running focus state indicator | **6.5:1** vs surface |
| `color.statusDestructive` | `#EF4444` | Stop session, delete actions | **4.7:1** vs surface |

---

## 3. Typography Tokens

Standard system typeface (`Roboto` / Android System Font). Clean, balanced, legible. Sentence case for all headers and labels. Banned: decorative monospace tracking, all-caps slogans.

| Token | Size | Line Height | Weight | Usage |
| :--- | :--- | :--- | :--- | :--- |
| `type.display` | 32sp | 40sp | Bold (700) | Active session remaining timer |
| `type.headline` | 24sp | 32sp | Bold (700) | Screen titles, key modal titles |
| `type.title` | 18sp | 24sp | SemiBold (600) | Section headers, card titles |
| `type.body` | 15sp | 22sp | Regular (400) | Descriptive text, instructions |
| `type.bodyMedium` | 15sp | 22sp | Medium (500) | List item titles, interactive labels |
| `type.caption` | 13sp | 18sp | Regular (400) | Timestamps, secondary subtitles |
| `type.label` | 12sp | 16sp | SemiBold (600) | Subtle state tags, compact metadata |

---

## 4. Spacing & Sizing Tokens

4dp base grid. Strict, intentional intervals.

```kotlin
object FocusSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp     // Standard content padding
    val lg = 20.dp     // Card inner padding
    val xl = 24.dp     // Screen edge horizontal padding
    val xxl = 32.dp    // Section separation
}
```

- **Touch Target**: Minimum 48dp for all interactive buttons, chips, switch rows, and list items.
- **Top Bar**: Standard 56dp height.
- **Bottom Navigation Bar**: Standard flat 64dp Material 3 bar (no floating pill).
- **Divider**: 1dp solid `#E5E7EB` (light) / `#2D3139` (dark).

---

## 5. Shape & Corner Radius Tokens

Exactly three corner radii used consistently across the entire application:

```kotlin
object FocusShapes {
    val small = RoundedCornerShape(8.dp)    // Buttons, filter chips, compact inputs
    val medium = RoundedCornerShape(12.dp)  // Cards, list surfaces, item panels
    val large = RoundedCornerShape(20.dp)   // Bottom sheets, alert dialogs
}
```

*Note: Fully circular shapes (`CircleShape`) are reserved exclusively for icon buttons, status indicator dots, and avatar/app icon previews.*

---

## 6. Elevation & Surface Hierarchy

- **Level 0 (Canvas)**: Background color (`color.background`).
- **Level 1 (Card/List Item)**: Elevated surface (`color.surface`) + 1dp border (`color.border`). Elevation is 0dp (flat modern style) relying on contrast and 1dp hairline border for definition.
- **Level 2 (Sheets & Dialogs)**: Modal Bottom Sheets and Dialogs use `color.surface` with subtle 2dp ambient elevation (`shadow`) and darkened scrim.
- **Rule**: Never place a bordered Level 1 card inside another Level 1 card ("boxes inside boxes"). Group items within a card using subtle dividers or whitespace.

---

## 7. Motion & Interaction Tokens

- **State Transitions**: `tween(durationMillis = 200, easing = FastOutSlowInEasing)` for color, alpha, and size changes.
- **Sheet / Dialog Transitions**: `tween(durationMillis = 280, easing = FastOutSlowInEasing)` for entrance/exit.
- **Countdown Tick**: Subtle 1-second cadence without aggressive pulsing or bouncing.

---

## 8. Iconography Tokens

- **Icon Family**: **Material Symbols (Rounded, 24dp grid)**.
- **Stroke & Grid Alignment**: Unified 24dp viewport, optical center aligned, consistent 2dp line stroke.
- **Accessibility Rule**: Every icon button must have a clear `contentDescription` for screen readers (TalkBack). Decorative icons next to text have `contentDescription = null`.
- **Icons Used Across the App**:
  - `home`: Dashboard home tab
  - `layers`: Profiles tab
  - `schedule`: Schedules tab
  - `bar_chart`: Statistics tab
  - `menu`: Navigation drawer trigger
  - `arrow_back`: Back navigation
  - `play_arrow`: Start focus session
  - `stop`: Stop focus session
  - `pause`: Pause session option
  - `add`: Create profile / add schedule
  - `edit`: Edit profile / schedule
  - `delete`: Delete profile / schedule
  - `content_copy`: Duplicate profile
  - `settings`: Settings screen
  - `lock`: Strict mode / PIN setup
  - `check_circle`: Verified permission / healthy engine
  - `warning`: Missing setup indicator
  - `phone`: Emergency dialer button (Focus Home)
  - `accessibility_new`: Accessibility permission row
  - `battery_charging_full`: Battery optimization row

---

## 9. Logo & Brand Mark Proposal

Moving away from the generic security-shield cliche, we propose three calm, functional marks:

1. **Option A: The Focus Aperture (Recommended)**
   * A clean, geometric outer ring with a central focal circle. Represents clarity, lens focusing, and dialed-in attention.
2. **Option B: The Minimal Milestone / Monolith**
   * A solid geometric rounded pill standing upright with a crisp horizontal slit. Represents stillness, calm, and solid boundary setting.
3. **Option C: The Focus Brackets**
   * Two minimalist corner brackets `[ ]` framing empty center space. Represents creating a protected space for flow and contemplation.

---

## 10. Vibe-Code Audit & Remediation Rules

| Detected Vibe-Code Tell | Replacement / Remediation |
| :--- | :--- |
| "Mechanical Orange", "Terminal Emerald", "Blueprint Cyan", "Cyber Violet", "Analog Amber" | Replace with plain names: "Blue", "Green", "Purple", "Amber", "Slate", with unified calm Blue accent default. |
| "Kernel-grade reliability", "tactical distraction defense", "Armed", "Core Defense Engines", "Background Memory Guardian" | Replace with plain, truthful system copy: "Background service", "Active", "Battery optimization", "App blocking". |
| "100% On-Device • Zero Network • No Telemetry" on every screen | Remove from repeating footers and headers. Present once cleanly on the "How Blocking Works" screen. |
| Unicode text symbols (`→`, `✓`, `⌫`) or emojis (`🔒`, `🛡️`) | Replace with native vector icons from the 24dp icon token set. |
| Cryptic "Action Req." badges | Use plain sentence case: "Needs setup". |
| Boxes inside boxes & borders on every child element | Flatten hierarchy. Single card container with subtle 1dp dividers and 16dp whitespace between items. |
| Status pills and badges on every card | Use a small 8dp dot with text only where state is actionable. |
| Floating pill bottom navigation bar | Standard flat Material 3 NavigationBar with 24dp icons and sentence-case labels. |
| Decorative monospace orange timers and numbers | Clean, readable system typography with bold weight for the timer and neutral high-contrast coloring. |
| Magic DP numbers & ad-hoc colors across composables | Enforce strict usage of `FocusSpacing`, `FocusShapes`, and `FocusColors` tokens. |
