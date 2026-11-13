package com.shishir.appdns.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shishir.appdns.core.PrivateDns
import com.shishir.appdns.ui.AppViewModel
import com.shishir.appdns.ui.components.PopIn
import com.shishir.appdns.ui.components.Pressable
import com.shishir.appdns.ui.components.ShieldMark
import com.shishir.appdns.ui.components.StaggerIn
import com.shishir.appdns.ui.theme.MonoStyle

@Composable
fun SetupScreen(vm: AppViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val status by vm.status.collectAsState()
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val adbCommand = PrivateDns.adbCommand(context.packageName)

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refreshStatus() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StaggerIn(0) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Setup guide", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Two permissions make AppDNS work. Both survive reboots — the app watches them and alerts you if anything ever turns off.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Step 1 — Accessibility
        StaggerIn(1) {
            StepCard(1, "Enable the app watcher", status.accessibility) {
                Text(
                    "Open Accessibility settings, find AppDNS in the list of downloaded apps/services, and switch it on. This lets AppDNS see which app is in front — nothing else.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                MonoButton("Open Accessibility settings") {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }

        // Step 2 — ADB grant
        StaggerIn(2) {
            StepCard(2, "Grant secure-settings access", status.secureSettings) {
                Text(
                    "Android only lets system tools change Private DNS. One ADB command grants AppDNS that ability — no root needed.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        adbCommand,
                        style = MonoStyle,
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Pressable(onClick = {
                        clipboard.setText(AnnotatedString(adbCommand))
                        copied = true
                    }, pressScale = 0.8f) {
                        Box(
                            Modifier.clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(8.dp)
                        ) {
                            if (copied) {
                                Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
                if (copied) {
                    Text("Copied to clipboard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                InstructionLine("1", "On a PC: install Android platform-tools, enable USB debugging on the phone (Developer options), connect via USB.")
                InstructionLine("2", "Run the command above in a terminal. Done — the grant is permanent.")
                InstructionLine("3", "No PC? Use wireless debugging with an on-device ADB app such as LADB or Shizuku, and run the same command.")
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Oppo / OnePlus / Realme (ColorOS)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        "The command fails with a SecurityException? Enable Developer options → “Disable permission monitoring” (sign in to your Oppo account if asked), restart the phone, then run the command again.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                MonoButton("I ran it — check again") { vm.refreshStatus() }
            }
        }

        // Step 3 — Notifications
        if (Build.VERSION.SDK_INT >= 33) {
            StaggerIn(3) {
                StepCard(3, "Allow alerts", status.notifications, optional = true) {
                    Text(
                        "Lets AppDNS warn you immediately if a permission is ever lost (for example after a system update).",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    MonoButton("Allow notifications") {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

        // Step 4 — Battery
        StaggerIn(4) {
            StepCard(4, "Unrestricted battery", status.batteryExempt, optional = true) {
                Text(
                    "Stops the system from freezing AppDNS in the background, so switching stays instant even after hours of idle.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                MonoButton("Open battery settings") {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                .setData(Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
        }

        // Completion
        if (status.coreReady) {
            StaggerIn(5) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShieldMark(true, Modifier.size(64.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        "You're all set",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Assign DNS profiles to your apps and AppDNS handles the rest.",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Start using AppDNS", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    done: Boolean,
    optional: Boolean = false,
    content: @Composable () -> Unit
) {
    val badgeScale by animateFloatAsState(
        if (done) 1f else 0.92f,
        spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "badge"
    )
    val badgeBg by animateColorAsState(
        if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
        tween(250), label = "badgeBg"
    )
    val badgeFg by animateColorAsState(
        if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        tween(250), label = "badgeFg"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .graphicsLayer { scaleX = badgeScale; scaleY = badgeScale }
                    .clip(CircleShape)
                    .background(badgeBg)
                    .border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = badgeFg)
                } else {
                    Text(number.toString(), color = badgeFg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (optional && !done) {
                Text(
                    "OPTIONAL",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun InstructionLine(number: String, text: String) {
    Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(number, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonoButton(label: String, onClick: () -> Unit) {
    Pressable(
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}