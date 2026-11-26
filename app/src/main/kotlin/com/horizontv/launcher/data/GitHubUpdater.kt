package com.horizontv.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.horizontv.launcher.model.AppUpdateInfo
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface AppUpdateDataSource {
  suspend fun fetchLatestRelease(currentVersionName: String): Result<AppUpdateInfo?> =
    checkForUpdate(currentVersionName)

  suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdateInfo?>
  suspend fun downloadAndInstall(
    update: AppUpdateInfo,
    onProgress: (Float) -> Unit,
  ): Result<Unit>
}

class GitHubUpdater(
  private val context: Context,
  private val repoOwner: String = "darkside281",
  private val repoName: String = "HorizonTVLauncher",
) : AppUpdateDataSource {

  override suspend fun fetchLatestRelease(currentVersionName: String): Result<AppUpdateInfo?> =
    withContext(Dispatchers.IO) {
      runCatching {
        val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          connectTimeout = 8_000
          readTimeout = 10_000
          setRequestProperty("Accept", "application/vnd.github+json")
          setRequestProperty("User-Agent", "HorizonTVLauncher/$currentVersionName")
        }
        try {
          val code = conn.responseCode
          if (code == 404) return@runCatching null
          val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
          if (code !in 200..299) {
            throw IllegalStateException("GitHub returned HTTP $code")
          }

          val root = JSONObject(raw)
          if (root.optBoolean("draft", false) || root.optBoolean("prerelease", false)) {
            return@runCatching null
          }
          val tagName = root.optString("tag_name", "").trim()
          val remoteVersion = tagName.removePrefix("v").removePrefix("V").trim()
          if (remoteVersion.isEmpty()) return@runCatching null

          val assets = root.optJSONArray("assets")
          var apkUrl = ""
          var apkName = "HorizonTVLauncher-$tagName.apk"
          var apkSize = 0L
          if (assets != null) {
            for (i in 0 until assets.length()) {
              val asset = assets.optJSONObject(i) ?: continue
              val name = asset.optString("name", "").trim()
              val dlUrl = asset.optString("browser_download_url", "").trim()
              if (name.endsWith(".apk", ignoreCase = true) &&
                !name.contains("full", ignoreCase = true) &&
                dlUrl.isNotEmpty()
              ) {
                apkUrl = dlUrl
                apkName = name
                apkSize = asset.optLong("size", 0L)
                break
              }
            }
          }

          val rawPublished = root.optString("published_at", "").trim()
          val formattedDate = formatReleaseDate(rawPublished)

          AppUpdateInfo(
            versionName = remoteVersion,
            tagName = tagName,
            releaseName = root.optString("name", "").trim().ifEmpty { "Horizon TV Launcher $tagName" },
            releaseNotes = root.optString("body", "").trim(),
            publishedAt = formattedDate,
            apkUrl = apkUrl,
            apkFileName = apkName,
            apkSizeBytes = apkSize,
            htmlUrl = root.optString("html_url", "").trim(),
          )
        } finally {
          conn.disconnect()
        }
      }
    }

  override suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdateInfo?> =
    fetchLatestRelease(currentVersionName).map { release ->
      if (release != null &&
        release.apkUrl.isNotEmpty() &&
        isVersionNewer(release.versionName, currentVersionName)
      ) {
        release
      } else {
        null
      }
    }

  private fun formatReleaseDate(isoTimestamp: String): String {
    if (isoTimestamp.isBlank()) return ""
    return runCatching {
      val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }
      val date = parser.parse(isoTimestamp) ?: return@runCatching isoTimestamp.substringBefore("T")
      SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
    }.getOrDefault(isoTimestamp.substringBefore("T"))
  }

  override suspend fun downloadAndInstall(
    update: AppUpdateInfo,
    onProgress: (Float) -> Unit,
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      // On Android 8+, ensure this app is allowed to request package installs
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
      ) {
        val permIntent = Intent(
          Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
          Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(permIntent) }
        throw IllegalStateException("Allow \"Install unknown apps\" for Horizon TV Launcher, then tap Update Now again")
      }

      val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
      updatesDir.listFiles()?.forEach { old ->
        if (old.name.endsWith(".apk", ignoreCase = true)) {
          runCatching { old.delete() }
        }
      }

      val safeFileName = update.apkFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
      val outFile = File(updatesDir, safeFileName)

      val conn = openFollowingRedirects(update.apkUrl)
      try {
        val code = conn.responseCode
        if (code !in 200..299) {
          throw IllegalStateException("Download failed (HTTP $code)")
        }
        val totalBytes = conn.contentLengthLong.takeIf { it > 0L } ?: update.apkSizeBytes
        conn.inputStream.use { input ->
          FileOutputStream(outFile).use { output ->
            val buf = ByteArray(16 * 1024)
            var downloaded = 0L
            var lastReported = -1
            while (true) {
              val read = input.read(buf)
              if (read <= 0) break
              output.write(buf, 0, read)
              downloaded += read
              if (totalBytes > 0L) {
                val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                if (pct != lastReported) {
                  lastReported = pct
                  onProgress(pct / 100f)
                }
              }
            }
          }
        }
      } finally {
        conn.disconnect()
      }

      onProgress(1f)
      val apkUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outFile,
      )
      val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(installIntent)
    }
  }

  private fun openFollowingRedirects(rawUrl: String): HttpURLConnection {
    var currentUrl = rawUrl
    repeat(6) {
      val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        instanceFollowRedirects = false
        connectTimeout = 12_000
        readTimeout = 30_000
        setRequestProperty("User-Agent", "HorizonTVLauncher-Updater")
      }
      val code = conn.responseCode
      if (code in 300..399) {
        val next = conn.getHeaderField("Location")
        conn.disconnect()
        if (next.isNullOrBlank()) throw IllegalStateException("Empty redirect location")
        currentUrl = next
      } else {
        return conn
      }
    }
    throw IllegalStateException("Too many redirects while downloading update")
  }

  companion object {
    fun isVersionNewer(remote: String, current: String): Boolean {
      val remoteParts = parseVersionParts(remote)
      val currentParts = parseVersionParts(current)
      val maxLen = maxOf(remoteParts.size, currentParts.size)
      for (i in 0 until maxLen) {
        val r = remoteParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (r != c) return r > c
      }
      return false
    }

    private fun parseVersionParts(version: String): List<Int> {
      return version
        .trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore("-")
        .split(".")
        .mapNotNull { it.toIntOrNull() }
    }
  }
}
