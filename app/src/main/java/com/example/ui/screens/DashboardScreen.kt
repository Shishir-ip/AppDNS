package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLog
import com.example.data.model.DnsProfile
import com.example.system.PrivateDnsManager
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.AppDnsViewModel
import com.example.ui.viewmodel.BatchPreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Pre-allocated reusable gradient brushes to avoid heap allocations per composition
private val ActiveCardBrush = Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.6f), NeonGreen.copy(alpha = 0.4f)))
private val InactiveCardBrush = Brush.linearGradient(listOf(DarkBorder, DarkBorder))
private val AmberCyanBrush = Brush.horizontalGradient(listOf(AlertAmber, CyberCyan))

@Composable
fun DashboardScreen(
    viewModel: AppDnsViewModel,
    onNavigateToRules: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDnsActive by viewModel.isDnsActive.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val triggerSource by viewModel.triggerSource.collectAsStateWithLifecycle()
    val foregroundAppName by viewModel.currentForegroundAppName.collectAsStateWithLifecycle()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsStateWithLifecycle()
    val hasSecureSettingsPermission by viewModel.hasSecureSettingsPermission.collectAsStateWithLifecycle()
    val currentSystemDnsMode by viewModel.currentSystemDnsMode.collectAsStateWithLifecycle()
    val currentSystemSpecifier by viewModel.currentSystemSpecifier.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val allRules by viewModel.allRules.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Permission Setup Card if WRITE_SECURE_SETTINGS not yet granted
        if (!hasSecureSettingsPermission) {
            item(key = "setup_card") {
                SecureSettingsSetupCard(
                    adbCommand = viewModel.getAdbCommand(),
                    onCopyCommand = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ADB Command", viewModel.getAdbCommand())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Command copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    onAttemptRoot = {
                        viewModel.attemptRootGrant { success ->
                            if (success) {
                                Toast.makeText(context, "Permission granted via Root!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Root grant not available. Use ADB command.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onVerifyPermission = {
                        viewModel.refreshSystemState()
                        if (viewModel.hasSecureSettingsPermission.value) {
                            Toast.makeText(context, "Permission verified!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Permission not detected yet. Run the command first.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // 2. Accessibility warning banner if not connected
        if (!isAccessibilityConnected) {
            item(key = "accessibility_banner") {
                ServiceSetupBanner(
                    onEnableClicked = {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            onNavigateToSettings()
                        }
                    }
                )
            }
        }

        // 3. Hero Status Card (Modifies Android System Settings Directly)
        item(key = "hero_card") {
            HeroStatusCard(
                isDnsActive = isDnsActive,
                activeProfile = activeProfile,
                systemDnsMode = currentSystemDnsMode,
                systemSpecifier = currentSystemSpecifier,
                triggerSource = triggerSource,
                hasPermission = hasSecureSettingsPermission,
                onToggleClicked = { viewModel.toggleDns() },
                onNavigateToSettings = onNavigateToSettings
            )
        }

        // 4. Quick Stats & Foreground App Card
        item(key = "foreground_card") {
            ForegroundAppCard(
                foregroundAppName = foregroundAppName,
                rulesCount = allRules.size,
                onManageRulesClicked = onNavigateToRules
            )
        }

        // 5. Quick Profile Carousel
        item(key = "quick_profiles_section") {
            QuickProfileSection(
                profiles = allProfiles,
                activeProfileId = activeProfile?.id,
                isDnsActive = isDnsActive,
                onProfileSelected = { viewModel.selectProfile(it) },
                onViewAllClicked = onNavigateToProfiles
            )
        }

        // 6. One-Tap Recommended Presets
        item(key = "presets_card") {
            BatchPresetsCard(
                onApplyPreset = { preset -> viewModel.applyBatchPreset(preset) },
                onNavigateToRules = onNavigateToRules
            )
        }

        // 7. Recent Rule Trigger Activity Feed
        item(key = "activity_logs_section") {
            ActivityLogSection(
                logs = recentLogs,
                onClearLogs = { viewModel.clearLogs() }
            )
        }
    }
}

@Composable
private fun SecureSettingsSetupCard(
    adbCommand: String,
    onCopyCommand: () -> Unit,
    onAttemptRoot: () -> Unit,
    onVerifyPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("secure_settings_setup_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = AmberCyanBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AlertAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Permission Required",
                        tint = AlertAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "System Private DNS Permission Needed",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Zero VPN required. Directly manages Android settings.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "To allow AppDNS to modify Private DNS in Android Settings without using a VPN, grant WRITE_SECURE_SETTINGS once via ADB or Root:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Command code box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .clickable { onCopyCommand() }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = adbCommand,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCopyCommand,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy ADB Command",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAttemptRoot,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("1-Tap Root", fontSize = 11.sp)
                }

                Button(
                    onClick = onVerifyPermission,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("Verify", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ServiceSetupBanner(
    onEnableClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("service_setup_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = AmberCyanBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AlertAmber.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Automation",
                    tint = AlertAmber,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Auto-Switching Service",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Allows instant app launch detection to change Private DNS in settings automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onEnableClicked,
                modifier = Modifier.testTag("enable_accessibility_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HeroStatusCard(
    isDnsActive: Boolean,
    activeProfile: DnsProfile?,
    systemDnsMode: String,
    systemSpecifier: String?,
    triggerSource: String,
    hasPermission: Boolean,
    onToggleClicked: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Pulse animation: rendered with graphicsLayer to isolate drawing phase & prevent recomposition loops at 120Hz/144Hz
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val targetStatusColor = if (isDnsActive) NeonGreen else MaterialTheme.colorScheme.outline
    val animatedStatusColor by animateColorAsState(targetStatusColor, label = "statusColor")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_status_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (isDnsActive) ActiveCardBrush else InactiveCardBrush
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(animatedStatusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(animatedStatusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val statusTitle = when (systemDnsMode) {
                    PrivateDnsManager.MODE_HOSTNAME -> "PRIVATE DNS (CUSTOM HOSTNAME)"
                    PrivateDnsManager.MODE_OPPORTUNISTIC -> "PRIVATE DNS (AUTOMATIC)"
                    else -> "PRIVATE DNS (OFF)"
                }
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = animatedStatusColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Big Power Toggle Button with hardware graphicsLayer pulse animation (zero recomposition overhead)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                if (isDnsActive) {
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.12f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (isDnsActive) CyberCyan else MaterialTheme.colorScheme.surfaceVariant,
                                    if (isDnsActive) NeonGreen else DarkNavyCard
                                )
                            )
                        )
                        .border(
                            2.dp,
                            if (isDnsActive) CyberCyan else DarkBorder,
                            CircleShape
                        )
                        .clickable {
                            if (hasPermission) {
                                onToggleClicked()
                            } else {
                                onNavigateToSettings()
                            }
                        }
                        .testTag("dashboard_power_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle Private DNS",
                        tint = if (isDnsActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Active Hostname
            val activeHostname = when (systemDnsMode) {
                PrivateDnsManager.MODE_HOSTNAME -> systemSpecifier ?: activeProfile?.dotHost ?: "Custom Hostname"
                PrivateDnsManager.MODE_OPPORTUNISTIC -> "System Automatic"
                else -> "Private DNS Off"
            }

            Text(
                text = activeHostname,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isDnsActive) "Trigger: $triggerSource" else "Tap power button to set Private DNS",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Metrics row (Hostname Specifier, Mode, Method)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem(
                    label = "System Mode",
                    value = systemDnsMode.uppercase(),
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(DarkBorder)
                )

                MetricItem(
                    label = "DoT Hostname",
                    value = (systemSpecifier ?: activeProfile?.dotHost ?: "None"),
                    modifier = Modifier.weight(1.5f)
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(DarkBorder)
                )

                MetricItem(
                    label = "Routing",
                    value = "Android Native",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ForegroundAppCard(
    foregroundAppName: String?,
    rulesCount: Int,
    onManageRulesClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("foreground_app_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Foreground Detection",
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Foreground App Detection",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = foregroundAppName ?: "Launcher / Idle",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$rulesCount per-app rules active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onManageRulesClicked,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Rules", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun QuickProfileSection(
    profiles: List<DnsProfile>,
    activeProfileId: String?,
    isDnsActive: Boolean,
    onProfileSelected: (DnsProfile) -> Unit,
    onViewAllClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick DNS Switcher",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage All",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = CyberCyan,
                modifier = Modifier
                    .clickable { onViewAllClicked() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(profiles.take(6), key = { it.id }) { profile ->
                val isSelected = profile.id == activeProfileId && isDnsActive
                QuickProfilePill(
                    profile = profile,
                    isSelected = isSelected,
                    onSelected = { onProfileSelected(profile) }
                )
            }
        }
    }
}

@Composable
private fun QuickProfilePill(
    profile: DnsProfile,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val borderColor = if (isSelected) CyberCyan else DarkBorder
    val bgColor = if (isSelected) CyberCyan.copy(alpha = 0.15f) else DarkNavyCard

    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onSelected() }
            .testTag("quick_profile_${profile.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(borderColor, borderColor))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(profile.accentColorHex))
                            } catch (e: Exception) {
                                CyberCyan
                            }
                        )
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = profile.dotHost,
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BatchPresetsCard(
    onApplyPreset: (BatchPreset) -> Unit,
    onNavigateToRules: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_presets_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Auto Presets",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart Per-App Presets",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Custom",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    modifier = Modifier.clickable { onNavigateToRules() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Apply recommended Private DNS settings to installed apps with one tap:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetButton(
                    title = "Ad-Block Browsers",
                    provider = "dns.adguard-dns.com",
                    color = NeonGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onApplyPreset(BatchPreset.BROWSERS_ADGUARD) }
                )
                PresetButton(
                    title = "Secure Banking",
                    provider = "dns.quad9.net",
                    color = CyberCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onApplyPreset(BatchPreset.BANKING_QUAD9) }
                )
            }
        }
    }
}

@Composable
private fun PresetButton(
    title: String,
    provider: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.3f)))
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "-> $provider",
                style = MaterialTheme.typography.bodySmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActivityLogSection(
    logs: List<AppLog>,
    onClearLogs: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Privacy Log",
                    tint = NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Local Switch Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (logs.isNotEmpty()) {
                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Logs",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Event-level trigger history. No URLs or browsing telemetry are ever logged.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (logs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavyCard)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent switches yet. Launch an app with a rule to trigger auto-switching.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavyCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                    logs.take(6).forEachIndexed { index, log ->
                        val formattedTime = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.event,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = log.profileName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (index < logs.take(6).size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(DarkBorder.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }
}
