package com.shishir.appdns.data

data class DnsProfile(
    val id: String,
    val name: String,
    val host: String,
    val builtIn: Boolean = false
)

data class AppRule(
    val pkg: String,
    val profileId: String,
    val enabled: Boolean = true
)

object ExitBehavior {
    const val OFF = 0
    const val DEFAULT = 1
    const val KEEP = 2
}

object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}

data class Prefs(
    val autoSwitch: Boolean = true,
    val theme: Int = ThemeMode.SYSTEM,
    val haptics: Boolean = true,
    val exitBehavior: Int = ExitBehavior.OFF,
    val defaultProfileId: String = "cloudflare",
    val switchNotifications: Boolean = false
)

data class Snapshot(
    val profiles: List<DnsProfile>,
    val rules: List<AppRule>,
    val prefs: Prefs
) {
    val profilesById: Map<String, DnsProfile> = profiles.associateBy { it.id }
    val rulesByPkg: Map<String, AppRule> = rules.associateBy { it.pkg }
}

object Defaults {
    val PROFILES = listOf(
        DnsProfile("cloudflare", "Cloudflare", "one.one.one.one", builtIn = true),
        DnsProfile("adguard", "AdGuard", "dns.adguard-dns.com", builtIn = true),
        DnsProfile("google", "Google", "dns.google", builtIn = true),
        DnsProfile("quad9", "Quad9", "dns.quad9.net", builtIn = true)
    )
}