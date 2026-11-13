package com.shishir.appdns.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shishir.appdns.R
import com.shishir.appdns.ui.MainActivity

/**
 * Watches every permission the app depends on. Reboots can silently disable
 * the accessibility service on some OEMs; WRITE_SECURE_SETTINGS can be revoked
 * by system updates or `pm revoke`. Whenever something is missing we raise a
 * heads-up notification and the in-app banner so the user can fix it.
 */
object Watchdog {

    const val CHANNEL_ALERTS = "alerts"
    const val CHANNEL_SWITCHES = "switches"
    private const val NOTIF_ID = 1001

    data class Status(
        val secureSettings: Boolean,
        val accessibility: Boolean,
        val notifications: Boolean,
        val batteryExempt: Boolean
    ) {
        val coreReady: Boolean get() = secureSettings && accessibility
    }

    fun check(context: Context): Status {
        val secure = PrivateDns.hasPermission(context)
        val acc = isAccessibilityEnabled(context)
        val notif = if (Build.VERSION.SDK_INT >= 33) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        val battery = try {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)
        } catch (e: Exception) { false }
        return Status(secure, acc, notif, battery)
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, AppDnsAccessibilityService::class.java)
        val flat = expected.flattenToString()
        val enabled = try {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Exception) { 0 }
        if (enabled != 1) return false
        val services = try {
            Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
        } catch (e: Exception) { return false }
        return services.split(':').any { it.equals(flat, ignoreCase = true) }
    }

    /** Checks state and raises/clears the alert notification accordingly. */
    fun sync(context: Context) {
        val status = check(context)
        DnsEngine.get(context).let { engine ->
            // keep engine flows fresh even if the service is dead
            engine.refresh()
        }
        if (status.coreReady) {
            NotificationManagerCompat.from(context).cancel(NOTIF_ID)
            return
        }
        val missing = buildList {
            if (!status.accessibility) add("Accessibility service is off")
            if (!status.secureSettings) add("Secure-settings permission missing")
        }.joinToString(" · ")

        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_SETUP, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("AppDNS is not protecting you")
            .setContentText(missing)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$missing.\nTap to open the setup guide and fix it in under a minute."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — the in-app banner covers this case.
        }
    }

    fun notifySwitch(context: Context, mode: String, host: String?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannels(context)
        val text = when (mode) {
            PrivateDns.MODE_HOST -> "Private DNS → ${host ?: ""}"
            PrivateDns.MODE_AUTO -> "Private DNS → Automatic"
            else -> "Private DNS → Off"
        }
        val n = NotificationCompat.Builder(context, CHANNEL_SWITCHES)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("AppDNS switched")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setTimeoutAfter(4000)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(2002, n)
        } catch (_: SecurityException) {
        }
    }

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Permission alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Warns when AppDNS loses a permission it needs"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SWITCHES, "DNS switches", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Optional notice whenever the DNS profile changes"
                setShowBadge(false)
            }
        )
    }
}