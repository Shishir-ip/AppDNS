package com.example.service

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.example.data.db.AppDnsDatabase
import com.example.data.model.AppRule
import com.example.data.model.DnsProfile
import com.example.data.repository.DnsRepository
import com.example.system.PrivateDnsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDnsController private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val database = AppDnsDatabase.getDatabase(context, scope)
    val repository = DnsRepository(
        context,
        database.dnsProfileDao(),
        database.appRuleDao(),
        database.appLogDao()
    )

    private val _isDnsActive = MutableStateFlow(false)
    val isDnsActive: StateFlow<Boolean> = _isDnsActive.asStateFlow()

    private val _activeProfile = MutableStateFlow<DnsProfile?>(null)
    val activeProfile: StateFlow<DnsProfile?> = _activeProfile.asStateFlow()

    private val _currentForegroundApp = MutableStateFlow<String?>(null)
    val currentForegroundApp: StateFlow<String?> = _currentForegroundApp.asStateFlow()

    private val _currentForegroundAppName = MutableStateFlow<String?>(null)
    val currentForegroundAppName: StateFlow<String?> = _currentForegroundAppName.asStateFlow()

    private val _triggerSource = MutableStateFlow("Manual")
    val triggerSource: StateFlow<String> = _triggerSource.asStateFlow()

    private val _isAccessibilityConnected = MutableStateFlow(false)
    val isAccessibilityConnected: StateFlow<Boolean> = _isAccessibilityConnected.asStateFlow()

    private val _hasSecureSettingsPermission = MutableStateFlow(false)
    val hasSecureSettingsPermission: StateFlow<Boolean> = _hasSecureSettingsPermission.asStateFlow()

    private val _currentSystemDnsMode = MutableStateFlow(PrivateDnsManager.MODE_OFF)
    val currentSystemDnsMode: StateFlow<String> = _currentSystemDnsMode.asStateFlow()

    private val _currentSystemSpecifier = MutableStateFlow<String?>("")
    val currentSystemSpecifier: StateFlow<String?> = _currentSystemSpecifier.asStateFlow()

    private var lastRulePackage: String? = null
    private var lastRuleExitAction: String? = null

    init {
        refreshSystemDnsState()

        scope.launch(Dispatchers.IO) {
            val defaultId = repository.getDefaultProfileId()
            val profile = repository.getProfileById(defaultId)
                ?: repository.getProfileById("cloudflare_standard")
            _activeProfile.value = profile
        }
    }

    fun refreshSystemDnsState() {
        _hasSecureSettingsPermission.value = PrivateDnsManager.hasSecureSettingsPermission(context)
        val mode = PrivateDnsManager.getPrivateDnsMode(context)
        val specifier = PrivateDnsManager.getPrivateDnsSpecifier(context)
        _currentSystemDnsMode.value = mode
        _currentSystemSpecifier.value = specifier

        _isDnsActive.value = mode == PrivateDnsManager.MODE_HOSTNAME || mode == PrivateDnsManager.MODE_OPPORTUNISTIC

        // Match with known profile if hostname mode
        if (mode == PrivateDnsManager.MODE_HOSTNAME && !specifier.isNullOrBlank()) {
            scope.launch(Dispatchers.IO) {
                val profiles = repository.allProfiles.firstOrNull() ?: emptyList()
                val matched = profiles.firstOrNull { it.dotHost.equals(specifier, ignoreCase = true) }
                if (matched != null) {
                    _activeProfile.value = matched
                }
            }
        }
    }

    fun setAccessibilityConnected(connected: Boolean) {
        _isAccessibilityConnected.value = connected
    }

    suspend fun attemptRootPermissionGrant(): Boolean {
        val success = PrivateDnsManager.grantWithRoot(context.packageName)
        refreshSystemDnsState()
        return success
    }

    fun startDns(profile: DnsProfile, source: String = "Manual"): Boolean {
        val success = PrivateDnsManager.setHostname(context, profile.dotHost)
        _activeProfile.value = profile
        _triggerSource.value = source

        if (success) {
            _isDnsActive.value = true
            _currentSystemDnsMode.value = PrivateDnsManager.MODE_HOSTNAME
            _currentSystemSpecifier.value = profile.dotHost

            scope.launch(Dispatchers.IO) {
                repository.addLog(
                    packageName = context.packageName,
                    appName = "AppDNS",
                    event = "System Private DNS set to ${profile.dotHost} ($source)",
                    profileName = profile.name
                )
            }
        }
        notifyTilesUpdated()
        return success
    }

    fun stopDns(source: String = "Manual"): Boolean {
        val success = PrivateDnsManager.turnOff(context)
        _triggerSource.value = source

        if (success) {
            _isDnsActive.value = false
            _currentSystemDnsMode.value = PrivateDnsManager.MODE_OFF
            _currentSystemSpecifier.value = ""

            scope.launch(Dispatchers.IO) {
                repository.addLog(
                    packageName = context.packageName,
                    appName = "AppDNS",
                    event = "System Private DNS turned OFF ($source)",
                    profileName = "Off"
                )
            }
        }
        notifyTilesUpdated()
        return success
    }

    fun setOpportunisticDns(source: String = "Manual"): Boolean {
        val success = PrivateDnsManager.setOpportunistic(context)
        _triggerSource.value = source

        if (success) {
            _isDnsActive.value = true
            _currentSystemDnsMode.value = PrivateDnsManager.MODE_OPPORTUNISTIC

            scope.launch(Dispatchers.IO) {
                repository.addLog(
                    packageName = context.packageName,
                    appName = "AppDNS",
                    event = "System Private DNS set to Automatic ($source)",
                    profileName = "Automatic"
                )
            }
        }
        notifyTilesUpdated()
        return success
    }

    fun switchProfile(profile: DnsProfile, source: String = "Manual"): Boolean {
        return startDns(profile, source)
    }

    /**
     * Called whenever an application opens or changes focus in foreground.
     */
    fun onForegroundAppChanged(packageName: String) {
        if (packageName == context.packageName) return
        if (packageName == "com.android.systemui" || packageName.contains("inputmethod")) return

        val previousPkg = _currentForegroundApp.value
        if (previousPkg == packageName) return

        _currentForegroundApp.value = packageName

        scope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val appLabel = try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }
            withContext(Dispatchers.Main) {
                _currentForegroundAppName.value = appLabel
            }

            if (!repository.isAutoSwitchEnabled()) return@launch

            // Check if there is an active rule configured for this app
            val rule = repository.getRuleForPackage(packageName)

            if (rule != null && rule.isEnabled) {
                // Rule triggered!
                val targetProfile = repository.getProfileById(rule.dnsProfileId)
                if (targetProfile != null) {
                    lastRulePackage = packageName
                    lastRuleExitAction = rule.onExitAction

                    withContext(Dispatchers.Main) {
                        startDns(targetProfile, source = "Auto: $appLabel")
                    }

                    repository.addLog(
                        packageName = packageName,
                        appName = appLabel,
                        event = "Auto-switched Private DNS for $appLabel (${targetProfile.dotHost})",
                        profileName = targetProfile.name
                    )
                }
            } else {
                // No rule for current app.
                // If previous app had a rule and we just exited it, apply exit action!
                if (lastRulePackage != null) {
                    val exitAction = lastRuleExitAction ?: repository.getFallbackAction()
                    val exitedAppName = _currentForegroundAppName.value ?: "App"

                    when (exitAction) {
                        AppRule.ACTION_TURN_OFF -> {
                            withContext(Dispatchers.Main) {
                                stopDns(source = "Exit: $exitedAppName")
                            }
                            repository.addLog(
                                packageName = packageName,
                                appName = exitedAppName,
                                event = "Turned off Private DNS on exit",
                                profileName = "Off"
                            )
                        }
                        AppRule.ACTION_REVERT_DEFAULT -> {
                            val defaultId = repository.getDefaultProfileId()
                            val defaultProfile = repository.getProfileById(defaultId)
                            if (defaultProfile != null) {
                                withContext(Dispatchers.Main) {
                                    startDns(defaultProfile, source = "Reverted: Default")
                                }
                                repository.addLog(
                                    packageName = packageName,
                                    appName = exitedAppName,
                                    event = "Reverted to default Private DNS on exit (${defaultProfile.dotHost})",
                                    profileName = defaultProfile.name
                                )
                            } else {
                                withContext(Dispatchers.Main) {
                                    setOpportunisticDns(source = "Reverted: Auto")
                                }
                            }
                        }
                        AppRule.ACTION_KEEP_ACTIVE -> {
                            // Leave active as is
                        }
                    }
                    lastRulePackage = null
                    lastRuleExitAction = null
                }
            }
        }
    }

    fun notifyTilesUpdated() {
        try {
            TileService.requestListeningState(
                context,
                ComponentName(context, AppDnsToggleTileService::class.java)
            )
            TileService.requestListeningState(
                context,
                ComponentName(context, AppDnsSelectorTileService::class.java)
            )
        } catch (e: Exception) {
            // Ignore in tests
        }
    }

    companion object {
        @Volatile
        private var instance: AppDnsController? = null

        fun getInstance(context: Context): AppDnsController {
            return instance ?: synchronized(this) {
                instance ?: AppDnsController(context.applicationContext).also { instance = it }
            }
        }
    }
}
