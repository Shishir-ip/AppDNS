package com.shishir.appdns.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shishir.appdns.core.DnsEngine
import com.shishir.appdns.core.UpdateManager
import com.shishir.appdns.core.Watchdog
import com.shishir.appdns.data.AppEntry
import com.shishir.appdns.data.AppRule
import com.shishir.appdns.data.AppsRepo
import com.shishir.appdns.data.DnsProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data class Available(val info: UpdateManager.ReleaseInfo) : UpdateState
    data class Downloading(
        val percent: Int,
        val info: UpdateManager.ReleaseInfo,
        val done: Long = 0,
        val total: Long = 0
    ) : UpdateState
    data class Ready(val file: File, val info: UpdateManager.ReleaseInfo) : UpdateState
    data class Error(val message: String) : UpdateState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val engine = DnsEngine.get(app)
    val store = engine.store

    private val _status = MutableStateFlow(Watchdog.check(app))
    val status: StateFlow<Watchdog.Status> = _status.asStateFlow()

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _loadingApps = MutableStateFlow(false)
    val loadingApps: StateFlow<Boolean> = _loadingApps.asStateFlow()

    init {
        viewModelScope.launch {
            _apps.value = AppsRepo.load(app)
        }
    }

    fun refreshStatus() {
        _status.value = Watchdog.check(getApplication())
        engine.refresh()
    }

    fun reloadApps() {
        viewModelScope.launch {
            _loadingApps.value = true
            _apps.value = AppsRepo.load(getApplication(), force = true)
            _loadingApps.value = false
        }
    }

    fun setRule(pkg: String, profileId: String?) {
        if (profileId == null) store.removeRule(pkg)
        else store.saveRule(AppRule(pkg, profileId))
        engine.reapply()
    }

    fun toggleRule(pkg: String, enabled: Boolean) {
        val rule = store.current.rulesByPkg[pkg] ?: return
        store.saveRule(rule.copy(enabled = enabled))
        engine.reapply()
    }

    fun addProfile(name: String, host: String): DnsProfile {
        val profile = DnsProfile(UUID.randomUUID().toString(), name.trim(), host.trim())
        store.upsertProfile(profile)
        return profile
    }

    fun updateProfile(profile: DnsProfile) {
        store.upsertProfile(profile)
        engine.reapply()
    }

    fun deleteProfile(id: String) {
        store.deleteProfile(id)
        engine.reapply()
    }

    fun setDefaultProfile(id: String) {
        store.setPrefs { it.copy(defaultProfileId = id) }
    }

    fun resetApp() {
        store.reset()
        engine.reapply()
    }

    // ---- in-app updater ----

    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    fun checkForUpdates() {
        val cur = _update.value
        if (cur is UpdateState.Checking || cur is UpdateState.Downloading) return
        _update.value = UpdateState.Checking
        viewModelScope.launch {
            _update.value = when (val r = UpdateManager.check()) {
                is UpdateManager.CheckResult.Available -> UpdateState.Available(r.info)
                is UpdateManager.CheckResult.UpToDate -> UpdateState.UpToDate(r.current)
                is UpdateManager.CheckResult.Error -> UpdateState.Error(r.message)
            }
        }
    }

    fun downloadUpdate(info: UpdateManager.ReleaseInfo) {
        if (_update.value is UpdateState.Downloading) return
        _update.value = UpdateState.Downloading(0, info)
        viewModelScope.launch {
            try {
                val file = UpdateManager.download(getApplication(), info) { pct, done, total ->
                    _update.value = UpdateState.Downloading(pct, info, done, total)
                }
                _update.value = UpdateState.Ready(file, info)
            } catch (e: Exception) {
                _update.value = UpdateState.Error("Download failed — check your connection and retry")
            }
        }
    }

    fun dismissUpdate() {
        if (_update.value !is UpdateState.Downloading) _update.value = UpdateState.Idle
    }
}