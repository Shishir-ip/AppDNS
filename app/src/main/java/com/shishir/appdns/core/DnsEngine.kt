package com.shishir.appdns.core

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.shishir.appdns.data.AppsRepo
import com.shishir.appdns.data.ExitBehavior
import com.shishir.appdns.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * The switching engine.
 *
 * Speed contract: [onForeground] runs directly on the accessibility event
 * thread with ZERO disk access, ZERO coroutines and ZERO debounce. Rules live
 * in an in-memory map; a DNS change is one binder write. Typical latency from
 * app launch to DNS applied: < 10 ms.
 */
class DnsEngine private constructor(private val context: Context) {

    val store = Store(context)
    private val main = Handler(Looper.getMainLooper())
    private val cr = context.contentResolver

    // ---- observable state (UI) ----
    private val _accessibilityConnected = MutableStateFlow(false)
    val accessibilityConnected: StateFlow<Boolean> = _accessibilityConnected.asStateFlow()

    private val _securePermission = MutableStateFlow(PrivateDns.hasPermission(context))
    val securePermission: StateFlow<Boolean> = _securePermission.asStateFlow()

    private val _currentPkg = MutableStateFlow<String?>(null)
    val currentPkg: StateFlow<String?> = _currentPkg.asStateFlow()

    private val _currentLabel = MutableStateFlow<String?>(null)
    val currentLabel: StateFlow<String?> = _currentLabel.asStateFlow()

    private val _sysMode = MutableStateFlow(PrivateDns.mode(cr))
    val sysMode: StateFlow<String> = _sysMode.asStateFlow()

    private val _sysHost = MutableStateFlow(PrivateDns.specifier(cr))
    val sysHost: StateFlow<String?> = _sysHost.asStateFlow()

    private val _switchCount = MutableStateFlow(0)
    val switchCount: StateFlow<Int> = _switchCount.asStateFlow()

    private val _lastSwitchAt = MutableStateFlow(0L)
    val lastSwitchAt: StateFlow<Long> = _lastSwitchAt.asStateFlow()

    // ---- hot-path state (volatile, no locks) ----
    @Volatile private var lastPkg: String? = null
    @Volatile private var activeRulePkg: String? = null
    @Volatile private var appliedMode: String = _sysMode.value
    @Volatile private var appliedHost: String? = _sysHost.value
    @Volatile private var service: AppDnsAccessibilityService? = null

    private val labelCache = ConcurrentHashMap<String, String>()

    init {
        // Keep UI + cache in sync when DNS is changed outside the app.
        val observer = object : ContentObserver(main) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        cr.registerContentObserver(Settings.Global.getUriFor("private_dns_mode"), false, observer)
        cr.registerContentObserver(Settings.Global.getUriFor("private_dns_specifier"), false, observer)

        // Re-evaluate when the screen wakes; some OEMs kill event delivery while dozing.
        val screenReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context, intent: android.content.Intent) {
                if (intent.action == android.content.Intent.ACTION_SCREEN_ON) {
                    refresh()
                    lastPkg = null
                    main.postDelayed({
                        val pkg = try {
                            service?.rootInActiveWindow?.packageName?.toString()
                        } catch (e: Exception) { null }
                        if (pkg != null) onForeground(pkg)
                    }, 250)
                    Watchdog.sync(context)
                }
            }
        }
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_ON)
        try {
            context.registerReceiver(screenReceiver, filter)
        } catch (_: Exception) {
        }
    }

    /** Hot path — called for every foreground app change. Must stay allocation-light. */
    fun onForeground(pkg: String) {
        if (pkg == context.packageName || isNeutral(pkg)) return
        if (pkg == lastPkg) return
        lastPkg = pkg

        val snap = store.current

        main.post {
            _currentPkg.value = pkg
            _currentLabel.value = labelOf(pkg)
        }

        if (!snap.prefs.autoSwitch) return

        val rule = snap.rulesByPkg[pkg]
        if (rule != null && rule.enabled) {
            val profile = snap.profilesById[rule.profileId] ?: return
            activeRulePkg = pkg
            applyLocked(PrivateDns.MODE_HOST, profile.host)
        } else if (activeRulePkg != null) {
            activeRulePkg = null
            when (snap.prefs.exitBehavior) {
                ExitBehavior.OFF -> applyLocked(PrivateDns.MODE_OFF, null)
                ExitBehavior.DEFAULT -> {
                    val def = snap.profilesById[snap.prefs.defaultProfileId]
                    if (def != null) applyLocked(PrivateDns.MODE_HOST, def.host)
                    else applyLocked(PrivateDns.MODE_AUTO, null)
                }
                // KEEP: leave DNS as-is
            }
        }
    }

    /** Writes only when the target differs from what we last applied. */
    private fun applyLocked(mode: String, host: String?) {
        if (mode == appliedMode && (mode != PrivateDns.MODE_HOST || host == appliedHost)) return
        val ok = PrivateDns.apply(cr, mode, host)
        if (ok) {
            appliedMode = mode
            appliedHost = host
            main.post {
                _sysMode.value = mode
                _sysHost.value = host
                _switchCount.value = _switchCount.value + 1
                _lastSwitchAt.value = System.currentTimeMillis()
            }
            notifyTile()
            if (store.current.prefs.switchNotifications) {
                Watchdog.notifySwitch(context, mode, host)
            }
        } else {
            // Permission likely revoked — surface it immediately.
            main.post {
                _securePermission.value = PrivateDns.hasPermission(context)
                Watchdog.sync(context)
            }
        }
    }

    /** Reads real system state; syncs cache + UI. Never writes. */
    fun refresh() {
        val mode = PrivateDns.mode(cr)
        val host = PrivateDns.specifier(cr)
        appliedMode = mode
        appliedHost = host
        val perm = PrivateDns.hasPermission(context)
        main.post {
            _sysMode.value = mode
            _sysHost.value = host
            _securePermission.value = perm
        }
    }

    /** Forces re-evaluation of the current foreground app (after settings changes). */
    fun reapply() {
        val cur = lastPkg
        lastPkg = null
        activeRulePkg = null
        if (cur != null) onForeground(cur)
    }

    fun setAutoSwitch(on: Boolean) {
        store.setPrefs { it.copy(autoSwitch = on) }
        if (!on) {
            activeRulePkg = null
            applyLocked(PrivateDns.MODE_OFF, null)
        } else {
            reapply()
        }
        notifyTile()
    }

    /** Manual apply from the UI (profile preview / test). */
    fun applyNow(mode: String, host: String?) {
        applyLocked(mode, host)
    }

    fun onServiceConnected(svc: AppDnsAccessibilityService?, connected: Boolean) {
        service = svc
        _accessibilityConnected.value = connected
        if (connected) {
            refresh()
            reapply()
        }
        Watchdog.sync(context)
    }

    fun labelOf(pkg: String): String {
        labelCache[pkg]?.let { return it }
        AppsRepo.labelOf(pkg)?.let { labelCache[pkg] = it; return it }
        val label = try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
        labelCache[pkg] = label
        return label
    }

    private fun isNeutral(pkg: String): Boolean {
        if (pkg == "android") return true
        if (pkg.contains("systemui")) return true
        if (pkg.contains("permissioncontroller")) return true
        if (pkg.contains("inputmethod")) return true
        if (pkg.contains("packageinstaller")) return true
        if (pkg.contains("credentialmanager")) return true
        return false
    }

    private fun notifyTile() {
        try {
            android.service.quicksettings.TileService.requestListeningState(
                context,
                android.content.ComponentName(context, DnsTileService::class.java)
            )
        } catch (_: Exception) {
        }
    }

    companion object {
        @Volatile private var instance: DnsEngine? = null

        fun init(context: Context): DnsEngine {
            return instance ?: synchronized(this) {
                instance ?: DnsEngine(context.applicationContext).also { instance = it }
            }
        }

        fun get(context: Context): DnsEngine = init(context)
    }
}