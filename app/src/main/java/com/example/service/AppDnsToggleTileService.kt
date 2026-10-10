package com.example.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.R
import com.example.system.PrivateDnsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppDnsToggleTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val controller = AppDnsController.getInstance(this)
        controller.refreshSystemDnsState()

        val mode = controller.currentSystemDnsMode.value
        val isCurrentlyActive = mode == PrivateDnsManager.MODE_HOSTNAME || mode == PrivateDnsManager.MODE_OPPORTUNISTIC

        if (isCurrentlyActive) {
            controller.stopDns(source = "Quick Settings Tile")
        } else {
            val activeProfile = controller.activeProfile.value
            if (activeProfile != null) {
                controller.startDns(activeProfile, source = "Quick Settings Tile")
            } else {
                scope.launch(Dispatchers.IO) {
                    val defaultId = controller.repository.getDefaultProfileId()
                    val profile = controller.repository.getProfileById(defaultId)
                        ?: controller.repository.getProfileById("cloudflare_standard")
                    profile?.let {
                        launch(Dispatchers.Main) {
                            controller.startDns(it, source = "Quick Settings Tile")
                        }
                    }
                }
            }
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val controller = AppDnsController.getInstance(this)
        controller.refreshSystemDnsState()

        val mode = controller.currentSystemDnsMode.value
        val specifier = controller.currentSystemSpecifier.value
        val isActive = mode == PrivateDnsManager.MODE_HOSTNAME || mode == PrivateDnsManager.MODE_OPPORTUNISTIC

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_dns_tile)
        tile.label = "Private DNS"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (mode) {
                PrivateDnsManager.MODE_HOSTNAME -> specifier?.take(18) ?: "Custom Hostname"
                PrivateDnsManager.MODE_OPPORTUNISTIC -> "Automatic"
                else -> "Off"
            }
        }
        tile.updateTile()
    }
}
