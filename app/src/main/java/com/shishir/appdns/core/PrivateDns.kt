package com.shishir.appdns.core

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Thin wrapper over Android's native Private DNS settings.
 * All writes are single binder calls (~1-3 ms) — safe to run on the
 * accessibility event thread for instant switching.
 */
object PrivateDns {

    const val MODE_OFF = "off"
    const val MODE_AUTO = "opportunistic"
    const val MODE_HOST = "hostname"

    private const val KEY_MODE = "private_dns_mode"
    private const val KEY_SPECIFIER = "private_dns_specifier"

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun mode(cr: ContentResolver): String = try {
        Settings.Global.getString(cr, KEY_MODE) ?: MODE_OFF
    } catch (e: Exception) {
        MODE_OFF
    }

    fun specifier(cr: ContentResolver): String? = try {
        Settings.Global.getString(cr, KEY_SPECIFIER)
    } catch (e: Exception) {
        null
    }

    /**
     * Applies a DNS state. Specifier is written BEFORE the mode so the system
     * never observes hostname-mode pointing at a stale provider.
     */
    fun apply(cr: ContentResolver, mode: String, host: String?): Boolean {
        return try {
            if (mode == MODE_HOST && !host.isNullOrBlank()) {
                Settings.Global.putString(cr, KEY_SPECIFIER, host)
            }
            Settings.Global.putString(cr, KEY_MODE, mode)
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun adbCommand(packageName: String): String =
        "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
}