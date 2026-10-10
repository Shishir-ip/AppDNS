# AppDNS - Native Android Private DNS Switcher & Per-App Privacy Router

[![Android Platform](https://img.shields.io/badge/Platform-Android_9.0%2B_(API_28%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![VPN Required](https://img.shields.io/badge/VPN_Required-NO_(Zero_VPN)-success?style=for-the-badge)](#zero-vpn-architecture)
[![Privacy](https://img.shields.io/badge/Tracking-0%25_On--Device-brightgreen?style=for-the-badge)](#permissions--privacy-exactly-what-appdns-accesses)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=for-the-badge)](LICENSE)

> **The Android utility that modifies Android's native Private DNS (DNS-over-TLS) settings per application. Automatically switches Private DNS hostnames upon launching designated apps and turns off or restores baseline DNS immediately when leaving—without running a VPN service.**

---

## 🔍 SEO & Discovery Keywords
*Android private dns switcher, modify private dns settings per app, auto switch private dns android, no vpn dns switcher, adguard dns per app, cloudflare family dns switcher, nextdns android switcher, write secure settings private dns, dns-over-tls per app routing, quick settings private dns tile, event-driven private dns automation.*

---

## 🌟 Why Zero VPN Matters

Traditional Android DNS switcher apps require running a local `VpnService`. This introduces several practical drawbacks:
1. **Blocks your VPN slot**: Android allows only **one** active VPN interface at a time. A VPN-based DNS tool prevents you from running WireGuard, Tailscale, OpenVPN, or corporate VPNs.
2. **CPU and memory overhead**: Continuous packet interception and loopback tunneling in user space.
3. **VPN key icon & warnings**: Persistent VPN status indicators in the notification bar.

**AppDNS takes a different approach:**
It writes directly to Android's native system configuration:
- `Settings.Global.PRIVATE_DNS_MODE` (`"hostname"`, `"off"`, or `"opportunistic"`)
- `Settings.Global.PRIVATE_DNS_SPECIFIER` (`"dns.adguard-dns.com"`, `"one.one.one.one"`, `"family.cloudflare-dns.com"`, etc.)

Android's own kernel and network stack manage the encrypted DNS-over-TLS (DoT) connection natively at hardware speeds with **no VPN slot conflicts and low CPU overhead.**

---

## 🔍 How Foreground App Detection Works

AppDNS uses an **event-driven** architecture rather than active polling:

- **Zero Polling**: Instead of running a continuous background loop to check running processes, AppDNS relies on Android's `AccessibilityService` window state callback (`AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`). The system notifies AppDNS only when the active window changes.
- **350 ms Debounce**: When a new foreground package is detected, a 350 ms timer (`DEBOUNCE_DELAY_MS`) ensures the window remains stable before applying rules or firing exit actions. Transient UI flutters (such as rapid task switching or momentary splashes) do not trigger unnecessary DNS changes.
- **Neutral System Windows**: System UI elements (`com.android.systemui`), input method editors/keyboards (`inputmethod`), and runtime permission dialogs (`permissioncontroller`, `packageinstaller`) are recognized as neutral. When these overlays appear, AppDNS maintains the current DNS state and preserves the underlying foreground app context.

---

## 🔒 Permissions & Privacy: Exactly What AppDNS Accesses

AppDNS is designed to be transparent about what it touches on your device. Below is a detailed breakdown of all requested permissions and data handling.

### 1. Accessibility Service (`AppDnsAccessibilityService`)
- **Why it is needed**: Android does not provide a standard callback when apps open and close. The accessibility service listens exclusively for `TYPE_WINDOW_STATE_CHANGED` events to determine which application is currently in the foreground and apply your configured DNS profile.
- **What it reads**: The service reads **only the package name** (`event.packageName`) of the focused window.
- **What it cannot read**: In [`accessibility_service_config.xml`](app/src/main/res/xml/accessibility_service_config.xml), `canRetrieveWindowContent` is explicitly set to `false`. The service **cannot read screen content, text fields, passwords, keystrokes, or view trees**.

### 2. `WRITE_SECURE_SETTINGS`
- **Why it is needed**: Android protects `Settings.Global` to prevent unauthorized apps from changing system settings. AppDNS requires `WRITE_SECURE_SETTINGS` to change `private_dns_mode` and `private_dns_specifier`.
- **How it is granted**: Granted once during setup via ADB, Shizuku, or Root (see [Setup](#-1-minute-initial-setup)).

### 3. Manifest Permissions Table

| Permission | Protection Level | Why AppDNS Uses It |
| :--- | :--- | :--- |
| `android.permission.INTERNET` | Normal | Used strictly for the optional socket latency test tool (`DnsPingTester`) to ping user-selected DNS hostnames. |
| `android.permission.ACCESS_NETWORK_STATE` | Normal | Checks network connectivity status before running latency ping checks. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal | Allows `AppDnsBootReceiver` to restore your default Private DNS profile after a device reboot. |
| `android.permission.POST_NOTIFICATIONS` | Runtime (Android 13+) | Allows displaying Quick Settings status updates or service alerts if enabled. |
| `android.permission.WRITE_SECURE_SETTINGS` | Signature / Privileged | Core permission to write native Private DNS hostname and mode settings. |
| `android.permission.WRITE_SETTINGS` | Signature / System | Included as a legacy compatibility fallback for older Android versions. |
| `android.permission.PACKAGE_USAGE_STATS` | Special App Access | Declared as an optional alternative fallback for foreground app detection. |
| `android.permission.QUERY_ALL_PACKAGES` | Normal (Android 11+) | Allows the App Rules screen to list installed apps so you can pick which apps to configure. |
| `BIND_ACCESSIBILITY_SERVICE` | Signature | Required for the accessibility service to receive window state change events. |

### 4. What is Stored Locally
All application data is saved strictly on-device in a local Room SQLite database (`AppDnsDatabase`):
- **DNS Profiles** (`dns_profiles`): Saved DNS hostnames, descriptions, and test latency numbers.
- **App Rules** (`app_rules`): Mappings between package names, assigned DNS profile IDs, and exit actions.
- **Event History** (`app_logs`): A rolling log (last 40 entries) recording when a rule switched DNS (timestamp, app name, package name, target DNS profile). **No URLs, visited domains, or browsing telemetry are ever collected or stored.**

### 5. Network Activity
- AppDNS contains **no analytics SDKs, no tracking libraries, and no telemetry services**.
- The only network calls in the codebase are in [`DnsPingTester.kt`](app/src/main/java/com/example/network/DnsPingTester.kt), which opens a direct TCP socket handshake (`Socket.connect`) to measure latency to the specific DNS server address you chose to test.

### 6. Open Source & Verification
You can inspect the source code directly to verify all behavior:
- Accessibility configuration: [`accessibility_service_config.xml`](app/src/main/res/xml/accessibility_service_config.xml)
- Accessibility event listener: [`AppDnsAccessibilityService.kt`](app/src/main/java/com/example/service/AppDnsAccessibilityService.kt)
- Focus management & debounce logic: [`AppDnsController.kt`](app/src/main/java/com/example/service/AppDnsController.kt)
- System Private DNS read/write logic: [`PrivateDnsManager.kt`](app/src/main/java/com/example/system/PrivateDnsManager.kt)
- Local event log storage: [`AppLogDao.kt`](app/src/main/java/com/example/data/db/AppLogDao.kt)

### 7. How to Revoke Access
- **Turn off Accessibility**: Open Android Settings > Accessibility > AppDNS, and switch the toggle to **Off**.
- **Revoke `WRITE_SECURE_SETTINGS`**: Connect your phone to a computer and run:
  ```bash
  adb shell pm revoke com.aistudio.appdns.pvtkqr android.permission.WRITE_SECURE_SETTINGS
  ```
- **Uninstalling**: Uninstalling AppDNS immediately purges all local databases and settings.

---

## ⚠️ Known Limitations

1. **System-Wide Setting (Not Per-App Tunnel Isolation)**:
   - Android's native Private DNS is a global system setting. When AppDNS switches the hostname for the active foreground app, the entire device uses that DNS provider while that app is in front.
   - Background sync and notifications from other apps will resolve through whichever DNS provider is active at that moment. Unlike a full VPN split-tunnel, this does not isolate background traffic per socket.
2. **Android 9+ and DoT Only**:
   - Android's native Private DNS feature requires Android 9 (API 28) or higher and supports **DNS-over-TLS (DoT)** hostnames. It does not support raw DNS-over-HTTPS (DoH) URLs natively.
3. **Device / OEM Behavior**:
   - Certain aggressive OEM skins (e.g. heavily customized battery savers) may delay accessibility callbacks or background execution. If you experience delays on a specific device model, please open a GitHub issue with your device model and Android version.

---

## ✨ Core Features

### 1. 🎯 Per-App Automatic Private DNS Switching
- **App-Specific Hostnames**: Assign designated DoT providers to individual apps (e.g. `dns.adguard-dns.com` for browsers, `dns.quad9.net` for banking, `one.one.one.one` for general use).
- **Event-Driven Detection**: Uses Android window state change callbacks (`TYPE_WINDOW_STATE_CHANGED`) to trigger setting updates with low battery impact.
- **Configurable Exit Actions**:
  - *Turn off Private DNS completely* (`private_dns_mode = off`)
  - *Revert to system default profile* (`private_dns_mode = hostname`)
  - *Revert to automatic opportunistic mode* (`private_dns_mode = opportunistic`)
  - *Keep active*

### 2. ⚡ Quick Settings Notification Drawer Tiles
- **Private DNS Toggle Tile (`AppDnsToggleTileService`)**: One-tap toggle directly in your quick settings pull-down. Subtitle dynamically displays the active hostname or status.
- **DNS Profile Cycler Tile (`AppDnsSelectorTileService`)**: Cycle through your saved DoT providers without opening the app.

### 3. 🛡️ Pre-Loaded Verified Private DNS (DoT) Library
Includes verified hostnames out of the box:
- **AdGuard DNS**: `dns.adguard-dns.com` (Ad and tracker blocking)
- **AdGuard Family**: `family.adguard-dns.com` (Ad-blocking + adult filter)
- **Cloudflare 1.1.1.1**: `one.one.one.one` (Fast privacy resolver)
- **Cloudflare Security**: `security.cloudflare-dns.com` (Malware blocking)
- **Cloudflare Family**: `family.cloudflare-dns.com` (Malware + adult filter)
- **Quad9 Security**: `dns.quad9.net` (Threat intelligence & zero logs)
- **NextDNS**: `dns.nextdns.io` (Custom cloud filtering; enter your custom ID: `<id>.dns.nextdns.io`)
- **Mullvad AdBlock**: `adblock.doh.mullvad.net` (Strict no-log privacy)
- **Google Public DNS**: `dns.google` (Geo-distributed resolver)
- **Control D**: `p0.freedns.controld.com` (Uncensored DoT)
- **Custom DoT Hostnames**: Add unlimited custom DoT server hostnames.

### 4. ⏱️ Integrated Socket Latency Ping Tool
- Test live connection times directly to resolvers in milliseconds over TCP.

### 5. 🔒 100% On-Device Privacy
- No analytics, no tracking, no remote logging.
- DNS-over-TLS encryption is handled directly by Android OS.

---

## 📲 1-Minute Initial Setup

Because modifying Android's system `Settings.Global` is a protected feature, AppDNS needs the `WRITE_SECURE_SETTINGS` permission once during setup:

### Method A: Via Computer (ADB)
Connect your phone with USB debugging enabled and run:
```bash
adb shell pm grant com.aistudio.appdns.pvtkqr android.permission.WRITE_SECURE_SETTINGS
```

### Method B: Via Shizuku (No PC Required)
1. Start **Shizuku** on your device using Wireless Debugging.
2. Use terminal/command execution to grant `WRITE_SECURE_SETTINGS` for AppDNS.

### Method C: Root (1-Tap Inside App)
If your phone is rooted with Magisk or KernelSU, tap the **1-Tap Root Grant** button on the setup banner.

### Step 2: Enable Auto-Switch Service
1. Open AppDNS **Settings**.
2. Tap **Enable** next to *Auto-Switch Accessibility Service*.
3. Toggle AppDNS to **ON**.

---

## 🛠️ Architecture & Tech Stack

```
AppDNS Architecture (Direct Android Settings Management)
├── Core System Engine
│   ├── PrivateDnsManager          # Reads/writes Settings.Global.private_dns_mode & private_dns_specifier
│   ├── AppDnsController           # Coordinates app focus events with debounce & neutral window filters
│   ├── AppDnsAccessibilityService # Window state change listener (canRetrieveWindowContent=false)
│   ├── AppDnsToggleTileService    # Quick Settings toggle for system Private DNS
│   ├── AppDnsSelectorTileService  # Quick Settings DoT profile cycler
│   └── AppDnsBootReceiver         # Restores preferred DNS on device reboot
├── Data Layer (Room Local SQLite)
│   ├── AppDnsDatabase             # Local Room DB for profiles, rules, and event triggers
│   ├── DnsProfileDao              # Storage for DoT hostnames, categories, and latency
│   ├── AppRuleDao                 # Per-package rule configuration & exit actions
│   └── AppLogDao                  # Local trigger event history (no browsing telemetry)
└── UI Layer (Jetpack Compose M3)
    ├── DashboardScreen            # Live system Private DNS status & setup wizard
    ├── AppRulesScreen             # App search, per-app hostname assignment & sheet
    ├── DnsProfilesScreen          # Pre-loaded DoT providers & custom hostname editor
    └── SettingsScreen             # Permission check, fallback selector, tile guides
```

---

## ❓ Frequently Asked Questions (FAQ)

### Does this use a VPN?
**No.** AppDNS does not create a VPN connection or run a local proxy. It modifies Android's built-in Private DNS setting directly. Your VPN slot remains completely free for WireGuard, Tailscale, or work VPNs.

### Does it drain battery?
AppDNS is event-driven and does not run continuous polling loops in the background. Android OS natively handles the DoT connection. When switching apps, AppDNS applies the setting and goes idle.

### Can I add my own NextDNS or personal DoT server?
**Yes.** Go to the **DNS Library** tab, tap `+`, and enter your hostname (for example: `xyz123.dns.nextdns.io`).

---

## 📄 License
Licensed under the [Apache License, Version 2.0](LICENSE).
