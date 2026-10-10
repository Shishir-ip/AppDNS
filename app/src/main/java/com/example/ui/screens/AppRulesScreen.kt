package com.example.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppRule
import com.example.data.model.DnsProfile
import com.example.data.model.InstalledAppInfo
import com.example.ui.components.AppIconImage
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.AppDnsViewModel
import com.example.ui.viewmodel.AppFilter
import kotlinx.coroutines.launch

// Pre-allocated brush definitions to eliminate GC allocations during scroll
private val ActiveRuleBorderBrush = Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.5f), NeonGreen.copy(alpha = 0.4f)))
private val InactiveRuleBorderBrush = Brush.horizontalGradient(listOf(DarkBorder, DarkBorder))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRulesScreen(
    viewModel: AppDnsViewModel,
    modifier: Modifier = Modifier
) {
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val allRules by viewModel.allRules.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()

    var selectedAppToConfig by remember { mutableStateOf<InstalledAppInfo?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val profileMap = remember(allProfiles) {
        allProfiles.associateBy { it.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_rules_screen")
    ) {
        // Search & Filter header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_field"),
                placeholder = { Text("Search installed apps...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkNavyCard,
                    unfocusedContainerColor = DarkNavyCard
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item(key = "filter_all") {
                    FilterChip(
                        selected = selectedFilter == AppFilter.ALL,
                        onClick = { viewModel.setFilter(AppFilter.ALL) },
                        label = { Text("All Apps") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan
                        )
                    )
                }
                item(key = "filter_configured") {
                    FilterChip(
                        selected = selectedFilter == AppFilter.CONFIGURED,
                        onClick = { viewModel.setFilter(AppFilter.CONFIGURED) },
                        label = { Text("Configured (${allRules.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan
                        )
                    )
                }
                item(key = "filter_user") {
                    FilterChip(
                        selected = selectedFilter == AppFilter.USER,
                        onClick = { viewModel.setFilter(AppFilter.USER) },
                        label = { Text("User Apps") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan
                        )
                    )
                }
                item(key = "filter_system") {
                    FilterChip(
                        selected = selectedFilter == AppFilter.SYSTEM,
                        onClick = { viewModel.setFilter(AppFilter.SYSTEM) },
                        label = { Text("System Apps") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan
                        )
                    )
                }
            }
        }

        if (isLoadingApps) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyberCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Scanning installed applications...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "No apps",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching apps found" else "No applications available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { appInfo ->
                    val rule = appInfo.rule
                    val profile = rule?.let { profileMap[it.dnsProfileId] }

                    AppRuleRowItem(
                        appInfo = appInfo,
                        configuredProfile = profile,
                        onItemClicked = {
                            selectedAppToConfig = appInfo
                        },
                        onToggleRule = { enabled ->
                            viewModel.toggleRuleEnabled(appInfo.packageName, enabled)
                        }
                    )
                }
            }
        }
    }

    // Modal BottomSheet for Rule Configuration
    selectedAppToConfig?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { selectedAppToConfig = null },
            sheetState = sheetState,
            containerColor = DarkNavyCard,
            modifier = Modifier.imePadding()
        ) {
            RuleConfigSheetContent(
                appInfo = app,
                allProfiles = allProfiles,
                onSave = { selectedProfileId, exitAction ->
                    viewModel.saveRule(
                        packageName = app.packageName,
                        appName = app.appName,
                        dnsProfileId = selectedProfileId,
                        onExitAction = exitAction
                    )
                    coroutineScope.launch {
                        sheetState.hide()
                        selectedAppToConfig = null
                    }
                },
                onDelete = {
                    viewModel.deleteRule(app.packageName)
                    coroutineScope.launch {
                        sheetState.hide()
                        selectedAppToConfig = null
                    }
                },
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        selectedAppToConfig = null
                    }
                }
            )
        }
    }
}

@Composable
private fun AppRuleRowItem(
    appInfo: InstalledAppInfo,
    configuredProfile: DnsProfile?,
    onItemClicked: () -> Unit,
    onToggleRule: (Boolean) -> Unit
) {
    val rule = appInfo.rule
    val isConfigured = rule != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClicked() }
            .testTag("app_item_${appInfo.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isConfigured && rule.isEnabled) {
            CardDefaults.outlinedCardBorder().copy(brush = ActiveRuleBorderBrush)
        } else {
            CardDefaults.outlinedCardBorder().copy(brush = InactiveRuleBorderBrush)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                drawable = appInfo.icon,
                appName = appInfo.appName,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isConfigured) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (rule.isEnabled) NeonGreen else MaterialTheme.colorScheme.outline)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = configuredProfile?.name ?: "Selected DNS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (rule.isEnabled) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• on exit: ${rule.onExitAction.replace("ACTION_", "").lowercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "Tap to set custom DNS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isConfigured) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleRule,
                    modifier = Modifier.testTag("rule_toggle_${appInfo.packageName}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyberCyan
                    )
                )
            } else {
                IconButton(onClick = onItemClicked) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Add DNS Rule",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleConfigSheetContent(
    appInfo: InstalledAppInfo,
    allProfiles: List<DnsProfile>,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val existingRule = appInfo.rule
    var selectedProfileId by remember {
        mutableStateOf(existingRule?.dnsProfileId ?: allProfiles.firstOrNull()?.id ?: "cloudflare_standard")
    }
    var exitAction by remember {
        mutableStateOf(existingRule?.onExitAction ?: AppRule.ACTION_REVERT_DEFAULT)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("configure_rule_bottom_sheet")
    ) {
        // App header
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(
                drawable = appInfo.icon,
                appName = appInfo.appName,
                size = 50.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Select DNS Provider for this App:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Profiles selector in smooth unified container
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allProfiles.forEach { profile ->
                val isSelected = profile.id == selectedProfileId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isSelected) CyberCyan else DarkBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedProfileId = profile.id }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${profile.category} • ${profile.dotHost}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { selectedProfileId = profile.id }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "When leaving this app:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Exit action options
        ExitActionRadioItem(
            title = "Revert to Default DNS profile",
            subtitle = "Restores your primary baseline DNS immediately",
            selected = exitAction == AppRule.ACTION_REVERT_DEFAULT,
            onClick = { exitAction = AppRule.ACTION_REVERT_DEFAULT }
        )
        ExitActionRadioItem(
            title = "Turn off DNS completely",
            subtitle = "Returns to standard system cellular/Wi-Fi DNS",
            selected = exitAction == AppRule.ACTION_TURN_OFF,
            onClick = { exitAction = AppRule.ACTION_TURN_OFF }
        )
        ExitActionRadioItem(
            title = "Keep this profile active",
            subtitle = "Does not switch when returning home",
            selected = exitAction == AppRule.ACTION_KEEP_ACTIVE,
            onClick = { exitAction = AppRule.ACTION_KEEP_ACTIVE }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (existingRule != null) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
            Button(
                onClick = { onSave(selectedProfileId, exitAction) },
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("save_rule_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Rule", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ExitActionRadioItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
