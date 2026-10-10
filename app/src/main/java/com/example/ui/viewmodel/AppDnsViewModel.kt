package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLog
import com.example.data.model.AppRule
import com.example.data.model.DnsProfile
import com.example.data.model.InstalledAppInfo
import com.example.service.AppDnsController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppFilter {
    ALL,
    CONFIGURED,
    USER,
    SYSTEM
}

enum class BatchPreset {
    BROWSERS_ADGUARD,
    BANKING_QUAD9,
    STREAMING_CLOUDFLARE
}

class AppDnsViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = AppDnsController.getInstance(application)
    private val repository = controller.repository

    val isDnsActive: StateFlow<Boolean> = controller.isDnsActive
    val activeProfile: StateFlow<DnsProfile?> = controller.activeProfile
    val triggerSource: StateFlow<String> = controller.triggerSource
    val currentForegroundApp: StateFlow<String?> = controller.currentForegroundApp
    val currentForegroundAppName: StateFlow<String?> = controller.currentForegroundAppName
    val isAccessibilityConnected: StateFlow<Boolean> = controller.isAccessibilityConnected
    val hasSecureSettingsPermission: StateFlow<Boolean> = controller.hasSecureSettingsPermission
    val currentSystemDnsMode: StateFlow<String> = controller.currentSystemDnsMode
    val currentSystemSpecifier: StateFlow<String?> = controller.currentSystemSpecifier

    val allProfiles: StateFlow<List<DnsProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRules: StateFlow<List<AppRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<AppLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _rawInstalledApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AppFilter.ALL)
    val selectedFilter: StateFlow<AppFilter> = _selectedFilter.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    // Config bottom sheet / dialog states
    val selectedAppForRule = MutableStateFlow<InstalledAppInfo?>(null)
    val editingProfile = MutableStateFlow<DnsProfile?>(null)
    val isAddProfileDialogOpen = MutableStateFlow(false)

    // Settings states
    val autoSwitchEnabled = MutableStateFlow(repository.isAutoSwitchEnabled())
    val defaultProfileId = MutableStateFlow(repository.getDefaultProfileId())
    val autoStartOnBoot = MutableStateFlow(repository.isAutoStartOnBoot())
    val splitTunnelMode = MutableStateFlow(repository.isSplitTunnelMode())
    val fallbackAction = MutableStateFlow(repository.getFallbackAction())

    // Filtered installed apps
    val filteredApps: StateFlow<List<InstalledAppInfo>> = combine(
        _rawInstalledApps,
        allRules,
        _searchQuery,
        _selectedFilter
    ) { apps, rules, query, filter ->
        val ruleMap = rules.associateBy { it.packageName }
        val updated = apps.map { app ->
            app.copy(rule = ruleMap[app.packageName])
        }

        updated.filter { app ->
            val matchesQuery = query.isBlank() ||
                app.appName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.CONFIGURED -> app.rule != null
                AppFilter.USER -> !app.isSystemApp
                AppFilter.SYSTEM -> app.isSystemApp
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = repository.getInstalledApps()
            _rawInstalledApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppFilter) {
        _selectedFilter.value = filter
    }

    fun toggleDns() {
        if (isDnsActive.value) {
            controller.stopDns(source = "Manual UI")
        } else {
            val target = activeProfile.value
                ?: allProfiles.value.firstOrNull { it.id == defaultProfileId.value }
                ?: allProfiles.value.firstOrNull()

            target?.let { controller.startDns(it, source = "Manual UI") }
        }
    }

    fun selectProfile(profile: DnsProfile) {
        controller.switchProfile(profile, source = "Manual UI")
    }

    fun saveRule(
        packageName: String,
        appName: String,
        dnsProfileId: String,
        onExitAction: String = AppRule.ACTION_REVERT_DEFAULT
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val rule = AppRule(
                packageName = packageName,
                appName = appName,
                dnsProfileId = dnsProfileId,
                isEnabled = true,
                onExitAction = onExitAction
            )
            repository.saveRule(rule)
            repository.addLog(
                packageName = packageName,
                appName = appName,
                event = "Configured DNS rule for $appName",
                profileName = repository.getProfileById(dnsProfileId)?.name ?: dnsProfileId
            )
        }
    }

    fun deleteRule(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRule(packageName)
        }
    }

    fun toggleRuleEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setRuleEnabled(packageName, enabled)
        }
    }

    fun saveCustomProfile(profile: DnsProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetProfile = if (profile.id.isBlank()) {
                profile.copy(id = "custom_" + UUID.randomUUID().toString().take(8))
            } else {
                profile
            }
            repository.saveProfile(targetProfile)
        }
    }

    fun deleteProfile(profile: DnsProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!profile.isPreset) {
                repository.deleteProfile(profile)
            }
        }
    }

    fun toggleFavoriteProfile(profile: DnsProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(profile.id, !profile.isFavorite)
        }
    }

    fun pingAllProfiles() {
        viewModelScope.launch {
            _isPinging.value = true
            repository.testAllProfilesLatency(allProfiles.value)
            _isPinging.value = false
        }
    }

    fun pingSingleProfile(profile: DnsProfile) {
        viewModelScope.launch {
            repository.testProfileLatency(profile)
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }

    fun refreshSystemState() {
        controller.refreshSystemDnsState()
    }

    fun attemptRootGrant(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = controller.attemptRootPermissionGrant()
            onComplete(result)
        }
    }

    fun getAdbCommand(): String {
        return com.example.system.PrivateDnsManager.getAdbCommand(getApplication<Application>().packageName)
    }

    fun setAutoSwitchEnabled(enabled: Boolean) {
        autoSwitchEnabled.value = enabled
        repository.setAutoSwitchEnabled(enabled)
    }

    fun setDefaultProfileId(id: String) {
        defaultProfileId.value = id
        repository.setDefaultProfileId(id)
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        autoStartOnBoot.value = enabled
        repository.setAutoStartOnBoot(enabled)
    }

    fun setSplitTunnelMode(enabled: Boolean) {
        splitTunnelMode.value = enabled
        repository.setSplitTunnelMode(enabled)
    }

    fun setFallbackAction(action: String) {
        fallbackAction.value = action
        repository.setFallbackAction(action)
    }

    fun applyBatchPreset(preset: BatchPreset) {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = _rawInstalledApps.value
            when (preset) {
                BatchPreset.BROWSERS_ADGUARD -> {
                    val browserKeywords = listOf("browser", "chrome", "firefox", "opera", "edge", "brave", "vivaldi", "duckduckgo", "kiwi")
                    val browserApps = apps.filter { app ->
                        browserKeywords.any { kw ->
                            app.appName.contains(kw, ignoreCase = true) ||
                            app.packageName.contains(kw, ignoreCase = true)
                        }
                    }
                    browserApps.forEach { app ->
                        saveRule(app.packageName, app.appName, "adguard_dns", AppRule.ACTION_REVERT_DEFAULT)
                    }
                }
                BatchPreset.BANKING_QUAD9 -> {
                    val financeKeywords = listOf("bank", "pay", "finance", "crypto", "wallet", "credit", "cash", "money")
                    val financeApps = apps.filter { app ->
                        financeKeywords.any { kw ->
                            app.appName.contains(kw, ignoreCase = true) ||
                            app.packageName.contains(kw, ignoreCase = true)
                        }
                    }
                    financeApps.forEach { app ->
                        saveRule(app.packageName, app.appName, "quad9_dns", AppRule.ACTION_REVERT_DEFAULT)
                    }
                }
                BatchPreset.STREAMING_CLOUDFLARE -> {
                    val streamKeywords = listOf("tube", "netflix", "spotify", "music", "prime", "disney", "twitch", "game")
                    val streamApps = apps.filter { app ->
                        streamKeywords.any { kw ->
                            app.appName.contains(kw, ignoreCase = true) ||
                            app.packageName.contains(kw, ignoreCase = true)
                        }
                    }
                    streamApps.forEach { app ->
                        saveRule(app.packageName, app.appName, "cloudflare_standard", AppRule.ACTION_REVERT_DEFAULT)
                    }
                }
            }
        }
    }
}
