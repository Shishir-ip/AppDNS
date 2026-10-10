# AppDNS - Native Android Private DNS Switcher & Per-App Privacy Router

[![Android Platform](https://img.shields.io/badge/Platform-Android_9.0%2B_(API_28%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![VPN Required](https://img.shields.io/badge/VPN_Required-NO_(Zero_VPN)-success?style=for-the-badge)](#zero-vpn-architecture)
[![Privacy](https://img.shields.io/badge/Tracking-0%25_On--Device-brightgreen?style=for-the-badge)](#privacy--security-guarantee)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=for-the-badge)](LICENSE)

> **The modern Android utility that directly modifies Android's native Private DNS (DNS-over-TLS) settings per application. Automatically switches Private DNS hostnames upon launching designated apps and turns off or restores baseline DNS immediately when leaving—without any VPN!**

---

## 🔍 SEO & Discovery Keywords
*Android private dns switcher, modify private dns settings per app, auto switch private dns android, no vpn dns switcher, adguard dns per app, cloudflare family dns switcher, nextdns android switcher, write secure settings private dns, dns-over-tls per app routing, quick settings private dns tile, battery efficient private dns automation.*

---

## 🌟 Why Zero VPN Matters

Traditional Android DNS switcher apps require running a local `VpnService`. This introduces several major drawbacks:
1. **Blocks your VPN slot**: Android allows only **one** active VPN interface at a time. A VPN-based DNS tool prevents you from using WireGuard, Tailscale, or work VPNs.
2. **Battery and CPU drain**: Constant packet interception and loopback tunneling in user space.
3. **VPN key icon & warnings**: Constant VPN status indicators in the notification bar.

**AppDNS is completely different:**
It writes directly to Android's native system configuration:
- `Settings.Global.PRIVATE_DNS_MODE` (`"hostname"`, `"off"`, or `"opportunistic"`)
- `Settings.Global.PRIVATE_DNS_SPECIFIER` (`"dns.adguard-dns.com"`, `"one.one.one.one"`, `"family.cloudflare-dns.com"`, etc.)

Android's own kernel and netd daemon manage the encrypted DNS-over-TLS connection natively at hardware line speeds with **zero overhead, zero VPN conflicts, and zero battery drain.**

---

## ✨ Core Features

### 1. 🎯 Per-App Automatic Private DNS Switching
- **App-Specific Hostnames**: Assign designated DoT providers to individual apps (e.g. `dns.adguard-dns.com` for browsers, `dns.quad9.net` for banking, `one.one.one.one` for gaming).
- **Zero-Battery Event Detection**: Uses Android's native accessibility window state callbacks (`TYPE_WINDOW_STATE_CHANGED`) to trigger instant setting updates without continuous background polling.
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
- **Cloudflare 1.1.1.1**: `one.one.one.one` (Ultra-fast privacy resolver)
- **Cloudflare Security**: `security.cloudflare-dns.com` (Malware blocking)
- **Cloudflare Family**: `family.cloudflare-dns.com` (Malware + adult filter)
- **Quad9 Security**: `dns.quad9.net` (Threat intelligence & zero logs)
- **NextDNS**: `dns.nextdns.io` (Custom cloud filtering; enter your custom ID: `<id>.dns.nextdns.io`)
- **Mullvad AdBlock**: `adblock.doh.mullvad.net` (Strict no-log privacy)
- **Google Public DNS**: `dns.google` (Worldwide geo-distributed DNS)
- **Control D**: `p0.freedns.controld.com` (Uncensored lightning-fast DoT)
- **Custom DoT Hostnames**: Add unlimited custom DoT server hostnames.

### 4. ⏱️ Integrated Socket Latency Ping Tool
- Test live connection times directly to resolvers in milliseconds.

### 5. 🔒 100% On-Device Privacy Guarantee
- No analytics, no tracking, no remote logging.
- System Private DNS encryption is handled directly by Android OS.

---

## 📲 1-Minute Initial Setup

Because modifying Android's system `Settings.Global` is a protected feature, AppDNS needs the `WRITE_SECURE_SETTINGS` permission once during setup:

### Method A: Via Computer (ADB)
Connect your phone with USB debugging enabled and run:
```bash
adb shell pm grant com.aistudio.appdns.pvtkqr android.permission.WRITE_SECURE_SETTINGS
```

### Method B: Via Shizuku (No PC Required!)
1. Start **Shizuku** on your device using Wireless Debugging.
2. Grant terminal permission to set `WRITE_SECURE_SETTINGS` for AppDNS.

### Method C: Root (1-Tap Inside App)
If your phone is rooted with Magisk or KernelSU, simply tap the **1-Tap Root** button on the setup banner!

### Step 2: Enable Auto-Switch Service
1. Open AppDNS **Settings**.
2. Tap **Enable** next to *Auto-Switch Accessibility Service*.
3. Toggle AppDNS to **ON**.

You're done! Now whenever you launch a configured app, Android's system Private DNS switches automatically.

---

## 🛠️ Architecture & Tech Stack

```
AppDNS Architecture (Direct Android Settings Management)
├── Core System Engine
│   ├── PrivateDnsManager          # Reads/writes Settings.Global.private_dns_mode & private_dns_specifier
│   ├── AppDnsController           # Coordinates app focus events with system settings
│   ├── AppDnsAccessibilityService # Zero-battery window state change listener
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
**No!** AppDNS does not create a VPN connection. It modifies Android's built-in Private DNS setting directly. Your VPN slot remains completely free for WireGuard, Tailscale, or other VPN apps.

### Does it drain battery?
**No.** Android OS itself maintains the encrypted DoT connection natively. AppDNS only executes for ~2ms when you actively switch between apps.

### Can I add my own NextDNS or personal DoT server?
**Yes.** Go to the **DNS Library** tab, tap `+`, and enter your hostname (for example: `xyz123.dns.nextdns.io`).

---

## 📄 License
Licensed under the [Apache License, Version 2.0](LICENSE).
