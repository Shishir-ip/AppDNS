package com.shishir.appdns

import android.app.Application
import com.shishir.appdns.core.BootReceiver
import com.shishir.appdns.core.DnsEngine
import com.shishir.appdns.core.Watchdog

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DnsEngine.init(this)
        Watchdog.ensureChannels(this)
        BootReceiver.schedulePeriodicCheck(this)
    }
}