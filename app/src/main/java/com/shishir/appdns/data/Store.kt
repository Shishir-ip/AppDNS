package com.shishir.appdns.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Single source of truth. Everything is held in memory (Snapshot) so the
 * accessibility hot path never touches disk. Writes persist asynchronously.
 */
class Store(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("appdns_v3", Context.MODE_PRIVATE)
    private val disk = Executors.newSingleThreadExecutor()

    private val _snapshot = MutableStateFlow(load())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()
    val current: Snapshot get() = _snapshot.value

    @Synchronized
    fun update(transform: (Snapshot) -> Snapshot) {
        val next = transform(_snapshot.value)
        _snapshot.value = next
        disk.execute { persist(next) }
    }

    fun saveRule(rule: AppRule) = update { s ->
        s.copy(rules = s.rules.filter { it.pkg != rule.pkg } + rule)
    }

    fun removeRule(pkg: String) = update { s ->
        s.copy(rules = s.rules.filter { it.pkg != pkg })
    }

    fun upsertProfile(profile: DnsProfile) = update { s ->
        s.copy(profiles = s.profiles.filter { it.id != profile.id } + profile)
    }

    fun deleteProfile(id: String) = update { s ->
        s.copy(
            profiles = s.profiles.filter { it.id != id },
            rules = s.rules.filter { it.profileId != id },
            prefs = if (s.prefs.defaultProfileId == id) {
                s.prefs.copy(defaultProfileId = s.profiles.firstOrNull { it.id != id }?.id ?: "")
            } else s.prefs
        )
    }

    fun setPrefs(transform: (Prefs) -> Prefs) = update { s ->
        s.copy(prefs = transform(s.prefs))
    }

    fun reset() = update {
        Snapshot(Defaults.PROFILES, emptyList(), Prefs())
    }

    // ---- persistence ----

    private fun load(): Snapshot {
        return try {
            val profiles = prefs.getString(KEY_PROFILES, null)?.let { json ->
                val arr = JSONArray(json)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    DnsProfile(
                        o.getString("id"), o.getString("name"),
                        o.getString("host"), o.optBoolean("builtIn", false)
                    )
                }
            } ?: Defaults.PROFILES

            val rules = prefs.getString(KEY_RULES, null)?.let { json ->
                val arr = JSONArray(json)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    AppRule(o.getString("pkg"), o.getString("profileId"), o.optBoolean("enabled", true))
                }
            } ?: emptyList()

            val p = prefs.getString(KEY_PREFS, null)?.let { json ->
                val o = JSONObject(json)
                Prefs(
                    autoSwitch = o.optBoolean("autoSwitch", true),
                    theme = o.optInt("theme", ThemeMode.SYSTEM),
                    haptics = o.optBoolean("haptics", true),
                    exitBehavior = o.optInt("exitBehavior", ExitBehavior.OFF),
                    defaultProfileId = o.optString("defaultProfileId", "cloudflare"),
                    switchNotifications = o.optBoolean("switchNotifications", false)
                )
            } ?: Prefs()

            Snapshot(profiles, rules, p)
        } catch (e: Exception) {
            Snapshot(Defaults.PROFILES, emptyList(), Prefs())
        }
    }

    private fun persist(s: Snapshot) {
        try {
            val profiles = JSONArray()
            s.profiles.forEach { p ->
                profiles.put(JSONObject().apply {
                    put("id", p.id); put("name", p.name)
                    put("host", p.host); put("builtIn", p.builtIn)
                })
            }
            val rules = JSONArray()
            s.rules.forEach { r ->
                rules.put(JSONObject().apply {
                    put("pkg", r.pkg); put("profileId", r.profileId); put("enabled", r.enabled)
                })
            }
            val prefsJson = JSONObject().apply {
                put("autoSwitch", s.prefs.autoSwitch)
                put("theme", s.prefs.theme)
                put("haptics", s.prefs.haptics)
                put("exitBehavior", s.prefs.exitBehavior)
                put("defaultProfileId", s.prefs.defaultProfileId)
                put("switchNotifications", s.prefs.switchNotifications)
            }
            prefs.edit()
                .putString(KEY_PROFILES, profiles.toString())
                .putString(KEY_RULES, rules.toString())
                .putString(KEY_PREFS, prefsJson.toString())
                .apply()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val KEY_PROFILES = "profiles"
        private const val KEY_RULES = "rules"
        private const val KEY_PREFS = "prefs"
    }
}