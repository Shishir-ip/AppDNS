package com.shishir.appdns.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shishir.appdns.data.AppEntry
import com.shishir.appdns.ui.AppViewModel
import com.shishir.appdns.ui.components.PopIn
import com.shishir.appdns.ui.components.Pressable
import com.shishir.appdns.ui.components.RubberSwitch
import com.shishir.appdns.ui.components.StaggerIn
import com.shishir.appdns.ui.components.rememberRubberOverscroll
import com.shishir.appdns.ui.components.rubberOverscroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppFilter { ALL, CONFIGURED, USER, SYSTEM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(vm: AppViewModel, snackbar: SnackbarHostState) {
    val snap by vm.store.snapshot.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsState()
    val loading by vm.loadingApps.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppFilter.ALL) }
    var selected by remember { mutableStateOf<AppEntry?>(null) }
    val scope = rememberCoroutineScope()
    val overscroll = rememberRubberOverscroll()

    val filtered = remember(apps, query, filter, snap.rules) {
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                app.label.contains(query, true) || app.pkg.contains(query, true)
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.CONFIGURED -> snap.rulesByPkg.containsKey(app.pkg)
                AppFilter.USER -> !app.isSystem
                AppFilter.SYSTEM -> app.isSystem
            }
            matchesQuery && matchesFilter
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("App rules", style = MaterialTheme.typography.headlineMedium)
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(20.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Pressable(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, null, Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("All", filter == AppFilter.ALL) { filter = AppFilter.ALL }
                FilterChip("Configured", filter == AppFilter.CONFIGURED) { filter = AppFilter.CONFIGURED }
                FilterChip("User", filter == AppFilter.USER) { filter = AppFilter.USER }
                FilterChip("System", filter == AppFilter.SYSTEM) { filter = AppFilter.SYSTEM }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (loading && apps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading apps…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().rubberOverscroll(overscroll),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(filtered, key = { _, a -> a.pkg }) { index, app ->
                    val rule = snap.rulesByPkg[app.pkg]
                    val profile = rule?.let { snap.profilesById[it.profileId] }
                    StaggerIn(index) {
                        AppRow(app, profile?.name, rule?.enabled == true) { selected = app }
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No apps match",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    selected?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            RuleSheet(
                vm = vm,
                app = app,
                onSaved = { msg ->
                    scope.launch {
                        snackbar.showSnackbar(msg)
                    }
                    scope.launch {
                        delay(220)
                        selected = null
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        tween(200), label = "chipBg"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "chipFg"
    )
    Pressable(onClick = onClick, modifier = Modifier.clip(CircleShape).background(bg)) {
        Text(
            label,
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AppRow(app: AppEntry, profileName: String?, enabled: Boolean, onClick: () -> Unit) {
    Pressable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (app.icon != null) {
                Image(app.icon, null, Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)))
            } else {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                    contentAlignment = Alignment.Center
                ) { Text(app.label.take(1), fontWeight = FontWeight.Bold) }
            }
            Column(Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                Text(
                    app.pkg,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (profileName != null) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        profileName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "Set",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RuleSheet(vm: AppViewModel, app: AppEntry, onSaved: (String) -> Unit) {
    val snap by vm.store.snapshot.collectAsStateWithLifecycle()
    val rule = snap.rulesByPkg[app.pkg]
    var savedProfile by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(app.label, style = MaterialTheme.typography.titleLarge)
        Text(
            "Choose the DNS profile to apply while this app is open",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        // No rule option
        SheetOption(
            title = "No rule",
            subtitle = "Follow the global exit behavior",
            selected = rule == null,
            saved = savedProfile == "__none__"
        ) {
            vm.setRule(app.pkg, null)
            savedProfile = "__none__"
            onSaved("Rule removed for ${app.label}")
        }

        snap.profiles.forEach { profile ->
            SheetOption(
                title = profile.name,
                subtitle = profile.host,
                selected = rule?.profileId == profile.id,
                saved = savedProfile == profile.id
            ) {
                vm.setRule(app.pkg, profile.id)
                savedProfile = profile.id
                onSaved("${app.label} → ${profile.name}")
            }
        }

        if (rule != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rule enabled", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                RubberSwitch(
                    checked = rule.enabled,
                    onCheckedChange = { vm.toggleRule(app.pkg, it) }
                )
            }
        }
    }
}

@Composable
private fun SheetOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    saved: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        tween(200), label = "optBg"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        tween(200), label = "optFg"
    )
    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg)) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = fg.copy(alpha = 0.65f), fontSize = 12.sp)
            }
            PopIn(visible = saved || selected) {
                Icon(Icons.Rounded.Check, null, Modifier.size(20.dp), tint = fg)
            }
        }
    }
}