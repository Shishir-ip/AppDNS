package com.shishir.appdns.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shishir.appdns.core.UpdateManager
import com.shishir.appdns.ui.UpdateState
import com.shishir.appdns.ui.theme.LocalHapticsOn
import com.shishir.appdns.ui.theme.MonoStyle

/**
 * The update experience: check → changelog → live download percentage →
 * install. Every state change cross-fades; the progress bar and completion
 * check use spring physics.
 */
@Composable
fun UpdateDialog(state: UpdateState, onDismiss: () -> Unit, onRetry: () -> Unit, onDownload: (UpdateManager.ReleaseInfo) -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsOn.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "updateState"
            ) { s ->
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (s) {
                        is UpdateState.Checking -> {
                            ShieldMark(true, Modifier.size(64.dp))
                            Text("Checking for updates…", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "Contacting GitHub releases",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is UpdateState.UpToDate -> {
                            Box(
                                Modifier.size(64.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Check, null, Modifier.size(34.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text("You're up to date", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                "AppDNS v${s.version} is the latest version.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            DialogButton("Done", onDismiss)
                        }

                        is UpdateState.Available -> {
                            ShieldMark(true, Modifier.size(56.dp))
                            Text("Update available", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Box(
                                Modifier.clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "v${s.info.version}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .verticalScroll(rememberScrollState())
                                    .padding(14.dp)
                            ) {
                                Text(
                                    s.info.notes,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DialogButton("Download update", { onDownload(s.info) }, Icons.Rounded.Download)
                            TextButton(onClick = onDismiss) { Text("Later") }
                        }

                        is UpdateState.Downloading -> {
                            Text(
                                "${s.percent}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                style = MonoStyle
                            )
                            val fraction by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = s.percent / 100f,
                                animationSpec = tween(280),
                                label = "progress"
                            )
                            Box(
                                Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(fraction).height(8.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Text(
                                if (s.total > 0) "%.1f MB of %.1f MB".format(
                                    s.done / 1048576f, s.total / 1048576f
                                ) else "Downloading…",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MonoStyle
                            )
                            Text(
                                "Keep AppDNS open until it finishes",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is UpdateState.Ready -> {
                            PopIn(visible = true) {
                                Box(
                                    Modifier.size(64.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Check, null, Modifier.size(34.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Text("Download complete", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                "v${s.info.version} is ready to install",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DialogButton("Install now", {
                                if (hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (UpdateManager.canInstall(context)) {
                                    context.startActivity(UpdateManager.installIntent(context, s.file))
                                } else {
                                    context.startActivity(UpdateManager.unknownSourcesIntent(context))
                                }
                            }, Icons.Rounded.InstallMobile)
                            TextButton(onClick = onDismiss) { Text("Later") }
                        }

                        is UpdateState.Error -> {
                            Icon(Icons.Rounded.Warning, null, Modifier.size(44.dp))
                            Text("Couldn't update", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                s.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DialogButton("Retry", onRetry)
                            TextButton(onClick = onDismiss) { Text("Close") }
                        }

                        UpdateState.Idle -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Pressable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Row(
            Modifier.padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(8.dp))
            }
            Text(
                label,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}