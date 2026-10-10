package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.AppDnsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppDnsBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val controller = AppDnsController.getInstance(context)
            if (controller.repository.isAutoStartOnBoot()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val defaultId = controller.repository.getDefaultProfileId()
                    val profile = controller.repository.getProfileById(defaultId)
                        ?: controller.repository.getProfileById("cloudflare_standard")
                    profile?.let {
                        CoroutineScope(Dispatchers.Main).launch {
                            controller.startDns(it, source = "Auto Boot")
                        }
                    }
                }
            }
        }
    }
}
