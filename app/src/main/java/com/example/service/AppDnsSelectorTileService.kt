package com.example.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.R
import com.example.system.PrivateDnsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AppDnsSelectorTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val controller = AppDnsController.getInstance(this)

        scope.launch(Dispatchers.IO) {
            val profiles = controller.repository.allProfiles.firstOrNull() ?: emptyList()
            if (profiles.isNotEmpty()) {
                val current = controller.activeProfile.value
                val currentIndex = profiles.indexOfFirst { it.id == current?.id }
                val nextIndex = if (currentIndex in 0 until profiles.size - 1) currentIndex + 1 else 0
                val nextProfile = profiles[nextIndex]

                launch(Dispatchers.Main) {
                    controller.switchProfile(nextProfile, source = "Tile Switcher")
                    updateTileState()
                }
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val controller = AppDnsController.getInstance(this)
        controller.refreshSystemDnsState()

        val profile = controller.activeProfile.value
        val mode = controller.currentSystemDnsMode.value
        val isActive = mode == PrivateDnsManager.MODE_HOSTNAME

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_dns_selector)
        tile.label = profile?.name?.take(16) ?: "DNS Profile"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isActive) (profile?.dotHost?.take(18) ?: "Active") else "Tap to apply"
        }
        tile.updateTile()
    }
}
