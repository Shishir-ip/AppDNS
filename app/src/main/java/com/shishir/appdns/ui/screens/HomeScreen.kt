package com.shishir.appdns.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shishir.appdns.core.PrivateDns
import com.shishir.appdns.ui.AppViewModel
import com.shishir.appdns.ui.components.Pressable
import com.shishir.appdns.ui.components.RubberSwitch
import com.shishir.appdns.ui.components.ShieldMark
import com.shishir.appdns.ui.components.StaggerIn
import com.shishir.appdns.ui.theme.MonoStyle

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onSetup: () -> Unit,
    onApps: () -> Unit,
    onProfiles: () -> Unit
) {
    val snap by vm.store.snapshot.collectAsStateWithLifecycle()
    val status by vm.status.collectAsState()
    val sysMode by vm.engine.sysMode.collectAsState()
    val sysHost by vm.engine.sysHost.collectAsState()
    val currentLabel by vm.engine.currentLabel.collectAsState()
    val switchCount by vm.engine.switchCount.collectAsState()
    val accConnected by vm.engine.accessibilityConnected.collectAsState()

    val protectionOn = snap.prefs.autoSwitch
    val ready = status.coreReady
    val shieldActive = protectionOn && ready && sysMode != PrivateDns.MODE_OFF

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        StaggerIn(0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShieldMark(active = false, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text("AppDNS", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("v3.0", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Permission alert banner
        if (!ready) {
            StaggerIn(1) { AlertBanner(onSetup) }
        }

        // Status card
        StaggerIn(2) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShieldMark(shieldActive, Modifier.size(116.dp))
                Spacer(Modifier.height(14.dp))
                Text(
                    text = when {
                        !ready -> "Not protecting"
                        !protectionOn -> "Paused"
                        sysMode == PrivateDns.MODE_HOST -> "Protected"
                        sysMode == PrivateDns.MODE_AUTO -> "Automatic"
                        else -> "Idle"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        !ready -> "Finish setup to activate"
                        !protectionOn -> "Per-app switching is off"
                        sysMode == PrivateDns.MODE_HOST -> sysHost ?: ""
                        sysMode == PrivateDns.MODE_AUTO -> "System automatic mode"
                        else -> "Private DNS is off"
                    },
                    style = MonoStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Per-app protection", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (protectionOn) "Switches DNS with your apps" else "Tap to resume",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RubberSwitch(
                        checked = protectionOn,
                        onCheckedChange = { vm.engine.setAutoSwitch(it) },
                        enabled = ready
                    )
                }
            }
        }

        // Live activity
        StaggerIn(3) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "RIGHT NOW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                InfoRow("Foreground app", currentLabel ?: "Waiting for events…")
                InfoRow(
                    "Private DNS",
                    when (sysMode) {
                        PrivateDns.MODE_HOST -> sysHost ?: "hostname"
                        PrivateDns.MODE_AUTO -> "Automatic"
                        else -> "Off"
                    }
                )
                InfoRow("Switches this session", switchCount.toString())
                InfoRow("App watcher", if (accConnected) "Connected" else "Not connected")
            }
        }

        // Quick actions
        StaggerIn(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("App rules", "${snap.rules.size} configured", Icons.Rounded.Apps, onApps, Modifier.weight(1f))
                QuickAction("DNS profiles", "${snap.profiles.size} available", Icons.Rounded.Dns, onProfiles, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AlertBanner(onSetup: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "alert")
    val glow by transition.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow"
    )
    val borderColor by animateColorAsState(
        MaterialTheme.colorScheme.onBackground.copy(alpha = glow),
        label = "alertBorder"
    )
    Pressable(
        onClick = onSetup,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Warning, null, Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("Setup required", fontWeight = FontWeight.Bold)
                Text(
                    "A permission is missing — tap to fix it",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Rounded.ChevronRight, null)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, style = MonoStyle)
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Pressable(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(22.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}