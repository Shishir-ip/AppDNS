package com.shishir.appdns.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shishir.appdns.core.PrivateDns
import com.shishir.appdns.data.DnsProfile
import com.shishir.appdns.ui.AppViewModel
import com.shishir.appdns.ui.components.Pressable
import com.shishir.appdns.ui.components.StaggerIn
import com.shishir.appdns.ui.components.rememberRubberOverscroll
import com.shishir.appdns.ui.components.rubberOverscroll
import com.shishir.appdns.ui.theme.MonoStyle
import kotlinx.coroutines.launch

private val HOST_REGEX =
    Regex("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)+$")

@Composable
fun ProfilesScreen(vm: AppViewModel, snackbar: SnackbarHostState) {
    val snap by vm.store.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<DnsProfile?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DnsProfile?>(null) }
    val overscroll = rememberRubberOverscroll()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("New profile", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).rubberOverscroll(overscroll),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DNS profiles", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "The default profile is used when a rule says “restore default”. Tap the circle to set it.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            itemsIndexed(snap.profiles, key = { _, p -> p.id }) { index, profile ->
                StaggerIn(index) {
                    ProfileCard(
                        profile = profile,
                        isDefault = snap.prefs.defaultProfileId == profile.id,
                        onDefault = {
                            vm.setDefaultProfile(profile.id)
                            scope.launch { snackbar.showSnackbar("${profile.name} is now the default") }
                        },
                        onApply = {
                            vm.engine.applyNow(PrivateDns.MODE_HOST, profile.host)
                            scope.launch { snackbar.showSnackbar("Applied ${profile.host} now") }
                        },
                        onEdit = { editing = profile },
                        onDelete = { deleting = profile }
                    )
                }
            }
        }
    }

    if (creating || editing != null) {
        ProfileDialog(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, host ->
                val existing = editing
                if (existing != null) {
                    vm.updateProfile(existing.copy(name = name, host = host))
                } else {
                    vm.addProfile(name, host)
                }
                creating = false; editing = null
                scope.launch { snackbar.showSnackbar("Profile saved") }
            }
        )
    }

    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text("Delete ${profile.name}?") },
            text = { Text("App rules using this profile will be removed too.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteProfile(profile.id)
                    deleting = null
                    scope.launch { snackbar.showSnackbar("Profile deleted") }
                }) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: DnsProfile,
    isDefault: Boolean,
    onDefault: () -> Unit,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Default selector with spring fill
            val fill by animateFloatAsState(
                if (isDefault) 1f else 0f,
                spring(dampingRatio = 0.5f, stiffness = 420f),
                label = "radioFill"
            )
            Pressable(onClick = onDefault, pressScale = 0.85f) {
                Box(
                    Modifier
                        .size(24.dp)
                        .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(13.dp)
                            .graphicsLayer { scaleX = fill; scaleY = fill; alpha = fill }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (isDefault) {
                        Box(
                            Modifier.clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "DEFAULT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                Text(profile.host, style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallAction("Apply now", Icons.Rounded.PlayArrow, onApply)
            if (!profile.builtIn) {
                SmallAction("Edit", Icons.Rounded.Edit, onEdit)
                SmallAction("Delete", Icons.Rounded.Delete, onDelete)
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Pressable(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, Modifier.size(15.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProfileDialog(
    initial: DnsProfile?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var touched by remember { mutableStateOf(false) }
    val hostValid = HOST_REGEX.matches(host.trim())
    val nameValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text(if (initial == null) "New DNS profile" else "Edit profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it; touched = true },
                    label = { Text("Private DNS hostname") },
                    placeholder = { Text("dns.example.com") },
                    singleLine = true,
                    isError = touched && !hostValid,
                    supportingText = {
                        if (touched && !hostValid) Text("Enter a valid hostname, e.g. dns.example.com")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Use the DoT/DoH hostname from your DNS provider — not an IP address.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), host.trim()) },
                enabled = nameValid && hostValid
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}