package com.shishir.appdns.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shishir.appdns.BuildConfig
import com.shishir.appdns.core.UpdateManager
import com.shishir.appdns.data.ExitBehavior
import com.shishir.appdns.data.ThemeMode
import com.shishir.appdns.ui.AppViewModel
import com.shishir.appdns.ui.UpdateState
import com.shishir.appdns.ui.components.Pressable
import com.shishir.appdns.ui.components.RubberSwitch
import com.shishir.appdns.ui.components.ShieldMark
import com.shishir.appdns.ui.components.StaggerIn
import com.shishir.appdns.ui.components.UpdateDialog
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    snackbar: SnackbarHostState,
    onSetup: () -> Unit,
    onProfiles: () -> Unit
) {
    val context = LocalContext.current
    val snap by vm.store.snapshot.collectAsStateWithLifecycle()
    val status by vm.status.collectAsState()
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    val prefs = snap.prefs
    val updateState by vm.update.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(updateState) {
        if (updateState is UpdateState.Ready) showUpdateDialog = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StaggerIn(0) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        // Appearance
        StaggerIn(1) {
            SettingsGroup("APPEARANCE") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Theme", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    ThemeSegmented(
                        selected = prefs.theme,
                        onSelect = { vm.store.setPrefs { p -> p.copy(theme = it) } }
                    )
                }
            }
        }

        // Behavior
        StaggerIn(2) {
            SettingsGroup("BEHAVIOR") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("When you leave a protected app", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    ExitOption("Turn Private DNS off", "Matches the classic AppDNS behavior", prefs.exitBehavior == ExitBehavior.OFF) {
                        vm.store.setPrefs { it.copy(exitBehavior = ExitBehavior.OFF) }
                        vm.engine.reapply()
                    }
                    ExitOption("Restore default profile", "Falls back to your default DNS", prefs.exitBehavior == ExitBehavior.DEFAULT) {
                        vm.store.setPrefs { it.copy(exitBehavior = ExitBehavior.DEFAULT) }
                        vm.engine.reapply()
                    }
                    ExitOption("Keep current DNS", "Leaves the last profile active", prefs.exitBehavior == ExitBehavior.KEEP) {
                        vm.store.setPrefs { it.copy(exitBehavior = ExitBehavior.KEEP) }
                        vm.engine.reapply()
                    }
                }
                SettingRow(
                    "Default profile",
                    snap.profilesById[prefs.defaultProfileId]?.name ?: "None",
                    onClick = onProfiles
                )
                SwitchRow("Haptic feedback", "Subtle ticks on toggles and saves", prefs.haptics) {
                    vm.store.setPrefs { p -> p.copy(haptics = it) }
                }
                SwitchRow("Switch notifications", "Silent notice on every DNS change", prefs.switchNotifications) {
                    vm.store.setPrefs { p -> p.copy(switchNotifications = it) }
                }
            }
        }

        // System
        StaggerIn(3) {
            SettingsGroup("SYSTEM") {
                SettingRow(
                    "Setup guide",
                    if (status.coreReady) "All permissions granted" else "Action needed",
                    onClick = onSetup
                )
                SettingRow(
                    "Battery optimization",
                    if (status.batteryExempt) "Unrestricted" else "Optimized (may delay switching)",
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    .setData(Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Column {
                        Text("Quick settings tile", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Add the AppDNS shield tile from your quick settings editor for one-tap pause/resume.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Updates & source
        StaggerIn(4) {
            SettingsGroup("UPDATES & SOURCE") {
                SettingRow(
                    "Check for updates",
                    "Current version ${BuildConfig.VERSION_NAME}",
                    onClick = {
                        showUpdateDialog = true
                        vm.checkForUpdates()
                    }
                )
                SettingRow(
                    "Source code",
                    "github.com/Shishir-ip/AppDNS",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.REPO_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
            }
        }

        // Reset
        StaggerIn(5) {
            Pressable(
                onClick = { confirmReset = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "Reset app data",
                    Modifier.padding(16.dp),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        // About
        StaggerIn(6) {
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShieldMark(false, Modifier.size(40.dp))
                Text("AppDNS", fontWeight = FontWeight.Bold)
                Text(
                    "v${BuildConfig.VERSION_NAME} · Per-app Private DNS for Android",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Designed & built by Shishir",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showUpdateDialog && updateState !is UpdateState.Idle) {
        UpdateDialog(
            state = updateState,
            onDismiss = {
                showUpdateDialog = false
                vm.dismissUpdate()
            },
            onRetry = { vm.checkForUpdates() },
            onDownload = { vm.downloadUpdate(it) }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text("Reset AppDNS?") },
            text = { Text("All rules, custom profiles and settings will be erased. Permissions are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetApp()
                    confirmReset = false
                    scope.launch { snackbar.showSnackbar("App data reset") }
                }) { Text("Reset", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) { content() }
    }
}

@Composable
private fun ThemeSegmented(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.background).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(ThemeMode.SYSTEM to "Auto", ThemeMode.LIGHT to "White", ThemeMode.DARK to "Black").forEach { (mode, label) ->
            val isSel = selected == mode
            val bg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                tween(220), label = "segBg"
            )
            val fg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(220), label = "segFg"
            )
            Pressable(onClick = { onSelect(mode) }, modifier = Modifier.clip(CircleShape).background(bg)) {
                Text(
                    label,
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ExitOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant,
        tween(200), label = "exitBg"
    )
    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(18.dp).clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(Modifier.size(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RubberSwitch(checked = checked, onCheckedChange = onChange)
    }
}