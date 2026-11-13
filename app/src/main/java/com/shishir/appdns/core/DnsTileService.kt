package com.shishir.appdns.core

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.shishir.appdns.R

/** Quick-settings shield: one tap toggles per-app protection. */
class DnsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val engine = DnsEngine.get(this)
        engine.setAutoSwitch(!engine.store.current.prefs.autoSwitch)
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val engine = DnsEngine.get(this)
        val on = engine.store.current.prefs.autoSwitch
        val ready = PrivateDns.hasPermission(this)
        tile.state = when {
            !ready -> Tile.STATE_UNAVAILABLE
            on -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = "AppDNS"
        tile.contentDescription = if (on) "AppDNS protection on" else "AppDNS protection off"
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = when {
                !ready -> "Setup needed"
                !on -> "Paused"
                engine.sysMode.value == PrivateDns.MODE_HOST -> engine.sysHost.value ?: "Active"
                engine.sysMode.value == PrivateDns.MODE_AUTO -> "Automatic"
                else -> "Off"
            }
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_shield)
        tile.updateTile()
    }
}