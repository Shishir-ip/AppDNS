package com.shishir.appdns.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val pkg: String,
    val label: String,
    val isSystem: Boolean,
    val icon: ImageBitmap?
)

/** Loads the launchable app catalog once; icons are rasterized off the main thread. */
object AppsRepo {

    @Volatile
    private var cache: List<AppEntry>? = null

    suspend fun load(context: Context, force: Boolean = false): List<AppEntry> {
        cache?.let { if (!force) return it }
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val seen = HashSet<String>()
            val out = ArrayList<AppEntry>()
            val resolved = try {
                pm.queryIntentActivities(intent, 0)
            } catch (e: Exception) {
                emptyList()
            }
            for (info in resolved) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (pkg == context.packageName || !seen.add(pkg)) continue
                val label = try { info.loadLabel(pm).toString() } catch (e: Exception) { pkg }
                val isSystem = try {
                    (pm.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (e: Exception) { false }
                val icon = try {
                    info.loadIcon(pm)?.toBitmap(96, 96)?.asImageBitmap()
                } catch (e: Exception) { null }
                out.add(AppEntry(pkg, label, isSystem, icon))
            }
            out.sortBy { it.label.lowercase() }
            cache = out
            out
        }
    }

    fun cached(): List<AppEntry> = cache ?: emptyList()

    fun labelOf(pkg: String): String? = cache?.firstOrNull { it.pkg == pkg }?.label
}