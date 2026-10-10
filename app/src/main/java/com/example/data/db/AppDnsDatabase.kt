package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppLog
import com.example.data.model.AppRule
import com.example.data.model.DnsProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DnsProfile::class, AppRule::class, AppLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDnsDatabase : RoomDatabase() {
    abstract fun dnsProfileDao(): DnsProfileDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun appLogDao(): AppLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDnsDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDnsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDnsDatabase::class.java,
                    "app_dns_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultProfiles(database.dnsProfileDao())
                    }
                }
            }
        }

        suspend fun populateDefaultProfiles(dao: DnsProfileDao) {
            val defaults = listOf(
                DnsProfile(
                    id = "cloudflare_standard",
                    name = "Cloudflare 1.1.1.1",
                    description = "Ultra-fast, privacy-first public DNS resolver",
                    primaryIpv4 = "1.1.1.1",
                    secondaryIpv4 = "1.0.0.1",
                    primaryIpv6 = "2606:4700:4700::1111",
                    dohUrl = "https://cloudflare-dns.com/dns-query",
                    dotHost = "one.one.one.one",
                    category = "Privacy",
                    isPreset = true,
                    isFavorite = true,
                    accentColorHex = "#F6821F"
                ),
                DnsProfile(
                    id = "cloudflare_security",
                    name = "Cloudflare Security",
                    description = "1.1.1.2 - Blocks known malware and malicious domains",
                    primaryIpv4 = "1.1.1.2",
                    secondaryIpv4 = "1.0.0.2",
                    primaryIpv6 = "2606:4700:4700::1112",
                    dohUrl = "https://security.cloudflare-dns.com/dns-query",
                    dotHost = "security.cloudflare-dns.com",
                    category = "Security",
                    isPreset = true,
                    isFavorite = true,
                    accentColorHex = "#E65100"
                ),
                DnsProfile(
                    id = "cloudflare_family",
                    name = "Cloudflare Family",
                    description = "1.1.1.3 - Blocks malware, phishing, and adult content",
                    primaryIpv4 = "1.1.1.3",
                    secondaryIpv4 = "1.0.0.3",
                    primaryIpv6 = "2606:4700:4700::1113",
                    dohUrl = "https://family.cloudflare-dns.com/dns-query",
                    dotHost = "family.cloudflare-dns.com",
                    category = "Family",
                    isPreset = true,
                    accentColorHex = "#FB8C00"
                ),
                DnsProfile(
                    id = "adguard_dns",
                    name = "AdGuard DNS",
                    description = "Blocks advertisements, tracking domains, and analytics",
                    primaryIpv4 = "94.140.14.14",
                    secondaryIpv4 = "94.140.15.15",
                    primaryIpv6 = "2a10:50c0::ad1:ff",
                    dohUrl = "https://dns.adguard-dns.com/dns-query",
                    dotHost = "dns.adguard-dns.com",
                    category = "Ad-Blocking",
                    isPreset = true,
                    isFavorite = true,
                    accentColorHex = "#68BC71"
                ),
                DnsProfile(
                    id = "adguard_family",
                    name = "AdGuard Family",
                    description = "Ad-blocking plus SafeSearch enforcement and adult content block",
                    primaryIpv4 = "94.140.14.15",
                    secondaryIpv4 = "94.140.15.16",
                    primaryIpv6 = "2a10:50c0::ad2:ff",
                    dohUrl = "https://family.adguard-dns.com/dns-query",
                    dotHost = "family.adguard-dns.com",
                    category = "Family",
                    isPreset = true,
                    accentColorHex = "#43A047"
                ),
                DnsProfile(
                    id = "quad9_dns",
                    name = "Quad9 Security",
                    description = "9.9.9.9 - Threat intelligence blocking & zero data logging",
                    primaryIpv4 = "9.9.9.9",
                    secondaryIpv4 = "149.112.112.112",
                    primaryIpv6 = "2620:fe::fe",
                    dohUrl = "https://dns.quad9.net/dns-query",
                    dotHost = "dns.quad9.net",
                    category = "Security",
                    isPreset = true,
                    isFavorite = true,
                    accentColorHex = "#2B6CB0"
                ),
                DnsProfile(
                    id = "mullvad_dns",
                    name = "Mullvad AdBlock",
                    description = "194.242.2.3 - Strict zero-log Swedish privacy & ad-blocking",
                    primaryIpv4 = "194.242.2.3",
                    secondaryIpv4 = "194.242.2.4",
                    primaryIpv6 = "2a07:e340::3",
                    dohUrl = "https://adblock.doh.mullvad.net/dns-query",
                    dotHost = "adblock.doh.mullvad.net",
                    category = "Privacy",
                    isPreset = true,
                    accentColorHex = "#D39E00"
                ),
                DnsProfile(
                    id = "nextdns_standard",
                    name = "NextDNS Anycast",
                    description = "Modern cloud DNS with customized privacy rules",
                    primaryIpv4 = "45.90.28.0",
                    secondaryIpv4 = "45.90.30.0",
                    primaryIpv6 = "2a07:a8c0::",
                    dohUrl = "https://dns.nextdns.io/dns-query",
                    dotHost = "dns.nextdns.io",
                    category = "Privacy",
                    isPreset = true,
                    accentColorHex = "#00B4D8"
                ),
                DnsProfile(
                    id = "google_public_dns",
                    name = "Google Public DNS",
                    description = "8.8.8.8 - High-performance geo-distributed worldwide DNS",
                    primaryIpv4 = "8.8.8.8",
                    secondaryIpv4 = "8.8.4.4",
                    primaryIpv6 = "2001:4860:4860::8888",
                    dohUrl = "https://dns.google/dns-query",
                    dotHost = "dns.google",
                    category = "High Speed",
                    isPreset = true,
                    accentColorHex = "#4285F4"
                ),
                DnsProfile(
                    id = "controld_dns",
                    name = "Control D Free",
                    description = "76.76.2.0 - Uncensored, lightning-fast modern resolver",
                    primaryIpv4 = "76.76.2.0",
                    secondaryIpv4 = "76.76.10.0",
                    primaryIpv6 = "2606:1a40::",
                    dohUrl = "https://freedns.controld.com/p0",
                    dotHost = "p0.freedns.controld.com",
                    category = "Privacy",
                    isPreset = true,
                    accentColorHex = "#8B5CF6"
                )
            )
            dao.insertProfiles(defaults)
        }
    }
}
