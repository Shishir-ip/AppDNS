package com.shishir.appdns.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Runs after reboot / app update and on a gentle periodic alarm.
 * If the accessibility service survived the reboot the system binds it
 * automatically; if not, the watchdog raises the alert notification.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_CHECK -> {
                Watchdog.sync(context)
                schedulePeriodicCheck(context)
            }
        }
    }

    companion object {
        const val ACTION_CHECK = "com.shishir.appdns.CHECK"
        private const val INTERVAL_MS = 30 * 60 * 1000L

        fun schedulePeriodicCheck(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val pi = PendingIntent.getBroadcast(
                    context, 0,
                    Intent(context, BootReceiver::class.java).setAction(ACTION_CHECK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + INTERVAL_MS,
                    INTERVAL_MS,
                    pi
                )
            } catch (_: Exception) {
            }
        }
    }
}