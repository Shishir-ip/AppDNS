package com.example.system

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PrivateDnsManager {

    const val PRIVATE_DNS_MODE = "private_dns_mode"
    const val PRIVATE_DNS_SPECIFIER = "private_dns_specifier"
    const val PRIVATE_DNS_DEFAULT_MODE = "private_dns_default_mode"

    const val MODE_OFF = "off"
    const val MODE_OPPORTUNISTIC = "opportunistic"
    const val MODE_HOSTNAME = "hostname"

    fun hasSecureSettingsPermission(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun getPrivateDnsMode(context: Context): String {
        return try {
            Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE) ?: MODE_OFF
        } catch (e: Exception) {
            MODE_OFF
        }
    }

    fun getPrivateDnsSpecifier(context: Context): String? {
        return try {
            Settings.Global.getString(context.contentResolver, PRIVATE_DNS_SPECIFIER)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sets Private DNS to a designated hostname (e.g., dns.adguard-dns.com or one.one.one.one)
     */
    fun setHostname(context: Context, hostname: String): Boolean {
        return try {
            val cr: ContentResolver = context.contentResolver
            val currentMode = getPrivateDnsMode(context)
            val currentSpec = getPrivateDnsSpecifier(context)
            if (currentMode == MODE_HOSTNAME && currentSpec == hostname) {
                return true
            }
            val modeSuccess = Settings.Global.putString(cr, PRIVATE_DNS_MODE, MODE_HOSTNAME)
            val specSuccess = Settings.Global.putString(cr, PRIVATE_DNS_SPECIFIER, hostname)
            modeSuccess && specSuccess
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Turns off Android Private DNS
     */
    fun turnOff(context: Context): Boolean {
        return try {
            val cr: ContentResolver = context.contentResolver
            val currentMode = getPrivateDnsMode(context)
            if (currentMode == MODE_OFF) {
                return true
            }
            Settings.Global.putString(cr, PRIVATE_DNS_MODE, MODE_OFF)
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reverts Android Private DNS to Automatic (Opportunistic) mode
     */
    fun setOpportunistic(context: Context): Boolean {
        return try {
            val cr: ContentResolver = context.contentResolver
            val currentMode = getPrivateDnsMode(context)
            if (currentMode == MODE_OPPORTUNISTIC) {
                return true
            }
            Settings.Global.putString(cr, PRIVATE_DNS_MODE, MODE_OPPORTUNISTIC)
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Attempts to grant WRITE_SECURE_SETTINGS automatically using Root (su)
     */
    suspend fun grantWithRoot(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(
                arrayOf("su", "-c", "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS")
            )
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    fun getAdbCommand(packageName: String): String {
        return "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
    }
}
