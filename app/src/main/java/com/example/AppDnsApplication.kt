package com.example

import android.app.Application
import com.example.data.db.AppDnsDatabase
import com.example.service.AppDnsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AppDnsApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Ensure database defaults are populated if empty
        applicationScope.launch {
            val db = AppDnsDatabase.getDatabase(this@AppDnsApplication, applicationScope)
            val currentProfiles = db.dnsProfileDao().getAllProfiles().firstOrNull()
            if (currentProfiles.isNullOrEmpty()) {
                AppDnsDatabase.populateDefaultProfiles(db.dnsProfileDao())
            }
        }

        // Initialize controller singleton
        AppDnsController.getInstance(this)
    }
}
