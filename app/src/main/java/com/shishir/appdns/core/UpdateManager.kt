package com.shishir.appdns.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.shishir.appdns.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight GitHub-releases updater. No third-party HTTP library —
 * plain HttpURLConnection + org.json, both already on the platform.
 */
object UpdateManager {

    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/Shishir-ip/AppDNS/releases/latest"
    const val REPO_URL = "https://github.com/Shishir-ip/AppDNS"

    data class ReleaseInfo(
        val version: String,
        val notes: String,
        val apkUrl: String
    )

    sealed interface CheckResult {
        data class Available(val info: ReleaseInfo) : CheckResult
        data class UpToDate(val current: String) : CheckResult
        data class Error(val message: String) : CheckResult
    }

    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                setRequestProperty("User-Agent", "AppDNS-Android")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (conn.responseCode == 404) {
                return@withContext CheckResult.UpToDate(BuildConfig.VERSION_NAME)
            }
            if (conn.responseCode != 200) {
                return@withContext CheckResult.Error("GitHub returned ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = json.optString("tag_name", "")
            val notes = json.optString("body", "").ifBlank { "Bug fixes and improvements." }
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name", "").endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url")
                        break
                    }
                }
            }
            if (apkUrl.isNullOrBlank()) {
                return@withContext CheckResult.Error("Release has no APK asset")
            }
            val remote = tag.trimStart('v', 'V')
            if (isNewer(remote, BuildConfig.VERSION_NAME)) {
                CheckResult.Available(ReleaseInfo(remote, notes, apkUrl))
            } else {
                CheckResult.UpToDate(BuildConfig.VERSION_NAME)
            }
        } catch (e: Exception) {
            CheckResult.Error("No internet or GitHub unreachable")
        } finally {
            conn?.disconnect()
        }
    }

    /** Numeric dot comparison: 3.10 > 3.9, v-prefix tolerated by caller. */
    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split('.').map { it.toIntOrNull() ?: 0 }
        val l = local.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /**
     * Streams the APK into app-private external storage, reporting integer
     * percent progress. Returns the downloaded file.
     */
    suspend fun download(
        context: Context,
        info: ReleaseInfo,
        onProgress: (percent: Int, doneBytes: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update"
        )
        dir.mkdirs()
        val target = File(dir, "AppDNS-${info.version}.apk")
        if (target.exists()) target.delete()

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "AppDNS-Android")
            }
            if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            var done = 0L
            var lastPercent = -1
            conn.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        val pct = if (total > 0) ((done * 100) / total).toInt() else 0
                        if (pct != lastPercent) {
                            lastPercent = pct
                            onProgress(pct, done, total)
                        }
                    }
                }
            }
            if (total > 0 && done < total) throw Exception("Incomplete download")
            onProgress(100, done, total)
            target
        } finally {
            conn?.disconnect()
        }
    }

    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun installIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun unknownSourcesIntent(context: Context): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}