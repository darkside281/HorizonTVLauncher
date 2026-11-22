package com.horizontv.launcher.data

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.horizontv.launcher.model.JellyfinBackdrop
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JellyfinSearchItem
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Native Plex Media Server REST client implementing [JellyfinDataSource] so Plex
 * shares the exact same slideshow, multi-library picker, ClearLogo overlay, and
 * search pipeline as Jellyfin & Emby.
 */
class PlexClient : JellyfinDataSource {

  override suspend fun authenticateAndFetch(
    rawServerUrl: String,
    apiKey: String?,
    username: String?,
    password: String?,
    mediaType: JellyfinMediaType,
    libraryIds: Set<String>,
    limit: Int,
  ): Result<JellyfinAuthResult> = withContext(Dispatchers.IO) {
    runCatching {
      val baseUrl = JellyfinClient.normalizeUrl(rawServerUrl)
      require(baseUrl.isNotEmpty()) { "Enter your Plex server URL (e.g. http://192.168.1.50:32400)" }

      var resolvedToken = apiKey?.trim().orEmpty()
      if (resolvedToken.isEmpty()) {
        val login = username?.trim().orEmpty()
        require(login.isNotEmpty()) { "Enter a Plex Token (X-Plex-Token) or your Plex.tv email/username" }
        resolvedToken = signInPlexTv(login, password.orEmpty())
      }

      val libraries = fetchLibrariesInternal(baseUrl, resolvedToken)
      require(libraries.isNotEmpty()) {
        "Connected to Plex, but no Movie or TV Show libraries were found. Check your server URL & Plex Token."
      }

      val backdrops = fetchBackdropsInternal(
        baseUrl = baseUrl,
        token = resolvedToken,
        mediaType = mediaType,
        libraryIds = libraryIds,
        limit = limit,
        knownLibraries = libraries,
      )
      require(backdrops.isNotEmpty()) {
        "Connected to Plex (${libraries.size} libraries), but no backdrop artwork was found."
      }

      JellyfinAuthResult(
        serverUrl = baseUrl,
        token = resolvedToken,
        userId = "",
        libraries = libraries,
        backdrops = backdrops,
      )
    }
  }

  override suspend fun fetchLibraries(
    serverUrl: String,
    token: String,
    userId: String,
  ): Result<List<JellyfinLibrary>> = withContext(Dispatchers.IO) {
    runCatching {
      val baseUrl = JellyfinClient.normalizeUrl(serverUrl)
      fetchLibrariesInternal(baseUrl, token)
    }
  }

  override suspend fun fetchBackdrops(
    serverUrl: String,
    token: String,
    userId: String,
    mediaType: JellyfinMediaType,
    libraryIds: Set<String>,
    limit: Int,
  ): Result<List<JellyfinBackdrop>> = withContext(Dispatchers.IO) {
    runCatching {
      val baseUrl = JellyfinClient.normalizeUrl(serverUrl)
      val libraries = fetchLibrariesInternal(baseUrl, token)
      fetchBackdropsInternal(
        baseUrl = baseUrl,
        token = token,
        mediaType = mediaType,
        libraryIds = libraryIds,
        limit = limit,
        knownLibraries = libraries,
      )
    }
  }

  override suspend fun downloadBackdropImage(
    imageUrl: String,
    token: String,
  ): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
      val conn = openConnection(imageUrl, token)
      try {
        if (conn.responseCode !in 200..299) return@runCatching null
        val bytes = conn.inputStream.use { it.readBytes() }
        if (bytes.isEmpty()) return@runCatching null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        var sampleSize = 1
        while ((bounds.outWidth / sampleSize) > 2560 || (bounds.outHeight / sampleSize) > 1440) {
          sampleSize *= 2
        }

        BitmapFactory.decodeByteArray(
          bytes,
          0,
          bytes.size,
          BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )?.asImageBitmap()
      } finally {
        conn.disconnect()
      }
    }.getOrNull()
  }

  override suspend fun downloadLogoImage(
    logoUrl: String,
    token: String,
  ): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
      val conn = openConnection(logoUrl, token)
      try {
        if (conn.responseCode !in 200..299) return@runCatching null
        val bytes = conn.inputStream.use { it.readBytes() }
        if (bytes.isEmpty()) return@runCatching null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
      } finally {
        conn.disconnect()
      }
    }.getOrNull()
  }

  override suspend fun searchItems(
    serverUrl: String,
    token: String,
    userId: String,
    query: String,
    limit: Int,
  ): Result<List<JellyfinSearchItem>> = withContext(Dispatchers.IO) {
    runCatching {
      val cleaned = query.trim()
      if (cleaned.length < 2) return@runCatching emptyList()
      val baseUrl = JellyfinClient.normalizeUrl(serverUrl)
      if (baseUrl.isEmpty() || token.isBlank()) return@runCatching emptyList()
      val encoded = URLEncoder.encode(cleaned, "UTF-8")
      val url = "$baseUrl/hubs/search?query=$encoded&limit=${limit.coerceIn(4, 24)}"
      val conn = openConnection(url, token)
      try {
        if (conn.responseCode !in 200..299) {
          throw IllegalStateException("Plex search returned HTTP ${conn.responseCode}")
        }
        val root = JSONObject(readResponseText(conn))
        val hubs = root.optJSONObject("MediaContainer")?.optJSONArray("Hub")
          ?: return@runCatching emptyList()
        buildList {
          for (h in 0 until hubs.length()) {
            val hub = hubs.optJSONObject(h) ?: continue
            val hubType = hub.optString("type", "").lowercase()
            if (hubType != "movie" && hubType != "show") continue
            val metadata = hub.optJSONArray("Metadata") ?: continue
            for (i in 0 until metadata.length()) {
              val item = metadata.optJSONObject(i) ?: continue
              val key = item.optString("ratingKey", "").trim()
              val title = item.optString("title", "").trim()
              if (key.isEmpty() || title.isEmpty()) continue
              val year = item.optInt("year", 0).takeIf { it > 1900 }
              val rating = if (item.has("rating") && !item.isNull("rating")) {
                item.optDouble("rating", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }?.toFloat()
              } else {
                null
              }
              val art = item.optString("art", "").trim()
              val thumb = item.optString("thumb", "").trim()
              val imageUrl = when {
                art.isNotEmpty() -> buildPlexImageUrl(baseUrl, art, token, width = 420, height = 236)
                thumb.isNotEmpty() -> buildPlexImageUrl(baseUrl, thumb, token, width = 320, height = 480)
                else -> null
              }
              add(
                JellyfinSearchItem(
                  itemId = key,
                  title = title,
                  itemType = if (hubType == "show") "Series" else "Movie",
                  year = year,
                  communityRating = rating,
                  imageUrl = imageUrl,
                ),
              )
            }
          }
        }.take(limit)
      } finally {
        conn.disconnect()
      }
    }
  }

  override suspend fun downloadThumbnail(
    imageUrl: String,
    token: String,
  ): ImageBitmap? = downloadBackdropImage(imageUrl, token)

  private fun signInPlexTv(login: String, password: String): String {
    val conn = openConnection("https://plex.tv/users/sign_in.json", token = null).apply {
      requestMethod = "POST"
      doOutput = true
      setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    }
    try {
      val body = "user[login]=${URLEncoder.encode(login, "UTF-8")}" +
        "&user[password]=${URLEncoder.encode(password, "UTF-8")}"
      OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
      val code = conn.responseCode
      if (code !in 200..299) {
        throw IllegalStateException("Plex.tv sign-in failed (HTTP $code). Check your email/password or use X-Plex-Token.")
      }
      val json = JSONObject(readResponseText(conn))
      val authToken = json.optJSONObject("user")?.optString("authToken", "")?.trim().orEmpty()
      require(authToken.isNotEmpty()) { "Plex.tv sign-in did not return an auth token." }
      return authToken
    } finally {
      conn.disconnect()
    }
  }

  private fun fetchLibrariesInternal(
    baseUrl: String,
    token: String,
  ): List<JellyfinLibrary> {
    val conn = openConnection("$baseUrl/library/sections", token)
    try {
      val code = conn.responseCode
      if (code == 401 || code == 403) {
        throw IllegalStateException("Unauthorized (HTTP $code) — check your Plex Token.")
      }
      if (code !in 200..299) {
        throw IllegalStateException("Plex returned HTTP $code — check your server URL (usually port 32400).")
      }
      val root = JSONObject(readResponseText(conn))
      val dirs = root.optJSONObject("MediaContainer")?.optJSONArray("Directory")
        ?: return emptyList()
      return buildList {
        for (i in 0 until dirs.length()) {
          val dir = dirs.optJSONObject(i) ?: continue
          val key = dir.optString("key", "").trim()
          val title = dir.optString("title", "").trim()
          val type = dir.optString("type", "").trim().lowercase()
          if (key.isEmpty() || title.isEmpty()) continue
          if (type == "movie" || type == "show") {
            add(
              JellyfinLibrary(
                id = key,
                name = title,
                collectionType = if (type == "show") "tvshows" else "movies",
              ),
            )
          }
        }
      }
    } finally {
      conn.disconnect()
    }
  }

  private fun fetchBackdropsInternal(
    baseUrl: String,
    token: String,
    mediaType: JellyfinMediaType,
    libraryIds: Set<String>,
    limit: Int,
    knownLibraries: List<JellyfinLibrary>,
  ): List<JellyfinBackdrop> {
    val safeLimit = limit.coerceIn(10, 500)
    val cleanedIds = libraryIds.map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    val targetSections = knownLibraries.filter { lib ->
      val matchesSelection = cleanedIds.isEmpty() || lib.id in cleanedIds
      val matchesMediaType = when (mediaType) {
        JellyfinMediaType.BOTH -> true
        JellyfinMediaType.MOVIES -> lib.collectionType == "movies"
        JellyfinMediaType.TV_SHOWS -> lib.collectionType == "tvshows"
      }
      matchesSelection && matchesMediaType
    }.ifEmpty {
      // If a user selected specific libraries that don't match the Media Type pill, respect their explicit libraries
      if (cleanedIds.isNotEmpty()) knownLibraries.filter { it.id in cleanedIds } else knownLibraries
    }

    if (targetSections.isEmpty()) return emptyList()

    val perSectionLimit = if (targetSections.size == 1) {
      safeLimit
    } else {
      ((safeLimit * 2) / targetSections.size).coerceIn(20, safeLimit.coerceAtMost(200))
    }

    val perLibraryResults = mutableListOf<List<JellyfinBackdrop>>()
    var lastError: Throwable? = null

    for (section in targetSections) {
      val res = runCatching {
        fetchSectionBackdrops(baseUrl, token, section.id, perSectionLimit)
      }
      res.onSuccess { list ->
        if (list.isNotEmpty()) perLibraryResults.add(list)
      }.onFailure { err ->
        lastError = err
      }
    }

    if (perLibraryResults.isEmpty()) {
      lastError?.let { throw it }
      return emptyList()
    }

    val merged = LinkedHashMap<String, JellyfinBackdrop>()
    var round = 0
    while (merged.size < safeLimit) {
      var added = false
      for (list in perLibraryResults) {
        if (round < list.size) {
          val item = list[round]
          if (!merged.containsKey(item.itemId)) {
            merged[item.itemId] = item
            if (merged.size >= safeLimit) break
          }
          added = true
        }
      }
      if (!added) break
      round++
    }
    return merged.values.toList().shuffled()
  }

  private fun fetchSectionBackdrops(
    baseUrl: String,
    token: String,
    sectionKey: String,
    limit: Int,
  ): List<JellyfinBackdrop> {
    val encodedKey = URLEncoder.encode(sectionKey, "UTF-8")
    val url = "$baseUrl/library/sections/$encodedKey/all" +
      "?sort=random&X-Plex-Container-Start=0&X-Plex-Container-Size=$limit"
    val conn = openConnection(url, token)
    try {
      val code = conn.responseCode
      if (code == 401 || code == 403) {
        throw IllegalStateException("Unauthorized (HTTP $code) — check your Plex Token.")
      }
      if (code !in 200..299) {
        throw IllegalStateException("Plex returned HTTP $code while loading backdrops.")
      }
      val root = JSONObject(readResponseText(conn))
      val items = root.optJSONObject("MediaContainer")?.optJSONArray("Metadata")
        ?: return emptyList()
      val results = mutableListOf<JellyfinBackdrop>()
      for (i in 0 until items.length()) {
        val item = items.optJSONObject(i) ?: continue
        val key = item.optString("ratingKey", "").trim()
        val title = item.optString("title", "").trim()
        val art = item.optString("art", "").trim()
        if (key.isEmpty() || title.isEmpty() || art.isEmpty()) continue
        val year = item.optInt("year", 0).takeIf { it > 1900 }
        val backdropUrl = buildPlexImageUrl(baseUrl, art, token, width = 1920, height = 1080)

        var logoUrl: String? = null
        val images = item.optJSONArray("Image")
        if (images != null) {
          for (j in 0 until images.length()) {
            val imgObj = images.optJSONObject(j) ?: continue
            if (imgObj.optString("type", "").equals("clearLogo", ignoreCase = true)) {
              val rawLogo = imgObj.optString("url", "").trim()
              if (rawLogo.isNotEmpty()) {
                logoUrl = buildDirectPlexUrl(baseUrl, rawLogo, token)
                break
              }
            }
          }
        }

        results.add(
          JellyfinBackdrop(
            itemId = key,
            title = title,
            year = year,
            imageUrl = backdropUrl,
            logoUrl = logoUrl,
          ),
        )
      }
      return results.shuffled()
    } finally {
      conn.disconnect()
    }
  }

  private fun buildPlexImageUrl(
    baseUrl: String,
    relativePath: String,
    token: String,
    width: Int,
    height: Int,
  ): String {
    if (relativePath.startsWith("http://", ignoreCase = true) ||
      relativePath.startsWith("https://", ignoreCase = true)
    ) {
      return relativePath
    }
    val encodedPath = URLEncoder.encode(relativePath, "UTF-8")
    val encodedToken = URLEncoder.encode(token, "UTF-8")
    return "$baseUrl/photo/:/transcode?width=$width&height=$height&minSize=1&upscale=1&url=$encodedPath&X-Plex-Token=$encodedToken"
  }

  private fun buildDirectPlexUrl(baseUrl: String, path: String, token: String): String {
    if (path.startsWith("http://", ignoreCase = true) ||
      path.startsWith("https://", ignoreCase = true)
    ) {
      return path
    }
    val sep = if (path.contains("?")) "&" else "?"
    return "$baseUrl$path${sep}X-Plex-Token=${URLEncoder.encode(token, "UTF-8")}"
  }

  private fun openConnection(urlString: String, token: String?): HttpURLConnection {
    return (URL(urlString).openConnection() as HttpURLConnection).apply {
      connectTimeout = 8000
      readTimeout = 10000
      setRequestProperty("Accept", "application/json")
      setRequestProperty("X-Plex-Client-Identifier", "horizon-tv-launcher")
      setRequestProperty("X-Plex-Product", "Horizon TV Launcher")
      setRequestProperty("X-Plex-Version", "2.0.0")
      setRequestProperty("X-Plex-Device", "Android TV")
      if (!token.isNullOrBlank()) {
        setRequestProperty("X-Plex-Token", token)
      }
    }
  }

  private fun readResponseText(conn: HttpURLConnection): String {
    return BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
  }
}
