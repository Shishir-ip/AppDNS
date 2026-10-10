package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.db.AppLogDao
import com.example.data.db.AppRuleDao
import com.example.data.db.DnsProfileDao
import com.example.data.model.AppLog
import com.example.data.model.AppRule
import com.example.data.model.DnsProfile
import com.example.data.model.InstalledAppInfo
import com.example.network.DnsPingTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DnsRepository(
    private val context: Context,
    private val dnsProfileDao: DnsProfileDao,
    private val appRuleDao: AppRuleDao,
    private val appLogDao: AppLogDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_dns_settings", Context.MODE_PRIVATE)

    val allProfiles: Flow<List<DnsProfile>> = dnsProfileDao.getAllProfiles()
    val allRules: Flow<List<AppRule>> = appRuleDao.getAllRules()
    val recentLogs: Flow<List<AppLog>> = appLogDao.getRecentLogs(50)

    suspend fun getProfileById(id: String): DnsProfile? = dnsProfileDao.getProfileById(id)

    suspend fun saveProfile(profile: DnsProfile) = dnsProfileDao.insertProfile(profile)

    suspend fun deleteProfile(profile: DnsProfile) = dnsProfileDao.deleteProfile(profile)

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) =
        dnsProfileDao.setFavorite(id, isFavorite)

    suspend fun saveRule(rule: AppRule) = appRuleDao.insertRule(rule)

    suspend fun deleteRule(packageName: String) = appRuleDao.deleteRule(packageName)

    suspend fun setRuleEnabled(packageName: String, isEnabled: Boolean) =
        appRuleDao.setRuleEnabled(packageName, isEnabled)

    suspend fun getRuleForPackage(packageName: String): AppRule? =
        appRuleDao.getRuleForPackage(packageName)

    suspend fun addLog(packageName: String, appName: String, event: String, profileName: String) {
        appLogDao.insertLog(
            AppLog(
                packageName = packageName,
                appName = appName,
                event = event,
                profileName = profileName
            )
        )
    }

    suspend fun clearLogs() = appLogDao.clearAllLogs()

    suspend fun testProfileLatency(profile: DnsProfile): Long {
        val latency = DnsPingTester.pingDnsServer(profile.primaryIpv4)
        if (latency >= 0) {
            dnsProfileDao.updateLatency(profile.id, latency)
        }
        return latency
    }

    suspend fun testAllProfilesLatency(profiles: List<DnsProfile>) {
        withContext(Dispatchers.IO) {
            profiles.forEach { profile ->
                val latency = DnsPingTester.pingDnsServer(profile.primaryIpv4)
                dnsProfileDao.updateLatency(profile.id, latency)
            }
        }
    }

    /**
     * Retrieves all installed apps with launchable intent, mapped with existing rules.
     */
    suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val seenPackages = mutableSetOf<String>()
        val appList = mutableListOf<InstalledAppInfo>()
        val existingRules = appRuleDao.getAllRulesList().associateBy { it.packageName }

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val appName = info.loadLabel(pm).toString()
            val isSystemApp = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                false
            }
            val icon = try {
                info.loadIcon(pm)
            } catch (e: Exception) {
                null
            }

            appList.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appName = appName,
                    isSystemApp = isSystemApp,
                    rule = existingRules[pkg],
                    icon = icon
                )
            )
        }

        appList.sortedBy { it.appName.lowercase() }
    }

    // --- Settings / Preferences ---
    fun isAutoSwitchEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_SWITCH, true)
    fun setAutoSwitchEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_AUTO_SWITCH, enabled).apply()

    fun getDefaultProfileId(): String =
        prefs.getString(KEY_DEFAULT_PROFILE, "cloudflare_standard") ?: "cloudflare_standard"

    fun setDefaultProfileId(id: String) =
        prefs.edit().putString(KEY_DEFAULT_PROFILE, id).apply()

    fun isAutoStartOnBoot(): Boolean = prefs.getBoolean(KEY_BOOT_START, true)
    fun setAutoStartOnBoot(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_BOOT_START, enabled).apply()

    fun isSplitTunnelMode(): Boolean = prefs.getBoolean(KEY_SPLIT_TUNNEL, false)
    fun setSplitTunnelMode(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_SPLIT_TUNNEL, enabled).apply()

    fun getFallbackAction(): String =
        prefs.getString(KEY_FALLBACK_ACTION, AppRule.ACTION_REVERT_DEFAULT)
            ?: AppRule.ACTION_REVERT_DEFAULT

    fun setFallbackAction(action: String) =
        prefs.edit().putString(KEY_FALLBACK_ACTION, action).apply()

    companion object {
        const val KEY_AUTO_SWITCH = "key_auto_switch"
        const val KEY_DEFAULT_PROFILE = "key_default_profile"
        const val KEY_BOOT_START = "key_boot_start"
        const val KEY_SPLIT_TUNNEL = "key_split_tunnel"
        const val KEY_FALLBACK_ACTION = "key_fallback_action"
    }
}
