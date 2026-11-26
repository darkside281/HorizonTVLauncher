package com.horizontv.launcher.data

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.horizontv.launcher.model.ContinueWatchingItem
import com.horizontv.launcher.model.JellyfinBackdrop
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JellyfinSearchItem
import com.horizontv.launcher.model.MediaWallpaperProvider
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class JellyfinAuthResult(
  val serverUrl: String,
  val token: String,
  val userId: String,
  val libraries: List<JellyfinLibrary>,
  val backdrops: List<JellyfinBackdrop>,
)

interface JellyfinDataSource {
  suspend fun authenticateAndFetch(
    rawServerUrl: String,
    apiKey: String?,
    username: String?,
    password: String?,
    mediaType: JellyfinMediaType = JellyfinMediaType.BOTH,
    libraryIds: Set<String> = emptySet(),
    limit: Int = 40,
  ): Result<JellyfinAuthResult>

  suspend fun fetchLibraries(
    serverUrl: String,
    token: String,
    userId: String,
  ): Result<List<JellyfinLibrary>>

  suspend fun fetchBackdrops(
    serverUrl: String,
    token: String,
    userId: String,
    mediaType: JellyfinMediaType = JellyfinMediaType.BOTH,
    libraryIds: Set<String> = emptySet(),
    limit: Int = 40,
  ): Result<List<JellyfinBackdrop>>

  suspend fun downloadBackdropImage(
    imageUrl: String,
    token: String,
  ): ImageBitmap?

  suspend fun downloadLogoImage(
    logoUrl: String,
    token: String,
  ): ImageBitmap?

  suspend fun searchItems(
    serverUrl: String,
    token: String,
    userId: String,
    query: String,
    limit: Int = 16,
  ): Result<List<JellyfinSearchItem>>

  suspend fun downloadThumbnail(
    imageUrl: String,
    token: String,
  ): ImageBitmap?

  suspend fun fetchContinueWatching(
    serverUrl: String,
    token: String,
    userId: String,
    provider: MediaWallpaperProvider = MediaWallpaperProvider.JELLYFIN,
    limit: Int = 12,
  ): Result<List<ContinueWatchingItem>> = Result.success(emptyList())
}

class JellyfinClient : JellyfinDataSource {

  override suspend fun fetchContinueWatching(
    serverUrl: String,
    token: String,
    userId: String,
    provider: MediaWallpaperProvider,
    limit: Int,
  ): Result<List<ContinueWatchingItem>> = withContext(Dispatchers.IO) {
    runCatching {
      val baseUrl = normalizeUrl(serverUrl)
      if (baseUrl.isEmpty() || token.isBlank()) return@runCatching emptyList()
      val resolvedUserId = userId.ifBlank { fetchFirstUserId(baseUrl, token).orEmpty() }
      if (resolvedUserId.isBlank()) return@runCatching emptyList()

      val seen = LinkedHashMap<String, ContinueWatchingItem>()
      val resumeUrl = "$baseUrl/Users/$resolvedUserId/Items/Resume?Limit=$limit&Recursive=true&MediaTypes=Video&EnableImageTypes=Backdrop,Thumb,Primary"
      val nextUpUrl = "$baseUrl/Shows/NextUp?UserId=$resolvedUserId&Limit=$limit&EnableImageTypes=Backdrop,Thumb,Primary"

      fun parseItems(raw: String?, isNextUp: Boolean) {
        if (raw.isNullOrBlank()) return
        val items = runCatching { JSONObject(raw).optJSONArray("Items") }.getOrNull() ?: return
        for (i in 0 until items.length()) {
          if (seen.size >= limit) break
          val obj = items.optJSONObject(i) ?: continue
          val id = obj.optString("Id", "").trim()
          if (id.isEmpty() || id in seen) continue
          val type = obj.optString("Type", "")
          val isEpisode = type.equals("Episode", ignoreCase = true)
          val name = obj.optString("Name", "").trim()
          val seriesName = obj.optString("SeriesName", "").trim()
          val seasonNum = obj.optInt("ParentIndexNumber", 0)
          val epNum = obj.optInt("IndexNumber", 0)
          val epCode = if (isEpisode && seasonNum > 0 && epNum > 0) "S$seasonNum · E$epNum" else if (isEpisode) "Episode" else "Movie"

          val userData = obj.optJSONObject("UserData")
          val runtimeTicks = obj.optLong("RunTimeTicks", 0L)
          val posTicks = userData?.optLong("PlaybackPositionTicks", 0L) ?: 0L
          val pct = when {
            userData != null && userData.has("PlayedPercentage") ->
              (userData.optDouble("PlayedPercentage", 0.0) / 100.0).toFloat()
            runtimeTicks > 0L && posTicks > 0L -> (posTicks.toFloat() / runtimeTicks.toFloat())
            else -> 0f
          }.coerceIn(0f, 1f)

          val remMinutes = if (runtimeTicks > posTicks && runtimeTicks > 0L) {
            ((runtimeTicks - posTicks) / 600_000_000L).toInt().coerceAtLeast(1)
          } else {
            0
          }
          val remLabel = when {
            isNextUp && pct <= 0.01f -> "Next Up"
            remMinutes >= 60 -> "${remMinutes / 60}h ${remMinutes % 60}m"
            remMinutes > 0 -> "${remMinutes}m left"
            else -> ""
          }

          val imageUrl = buildString {
            val seriesId = obj.optString("SeriesId", "").trim()
            val hasBackdrop = (obj.optJSONArray("BackdropImageTags")?.length() ?: 0) > 0
            val parentBackdropId = obj.optString("ParentBackdropItemId", "").trim()
            when {
              hasBackdrop -> append("$baseUrl/Items/$id/Images/Backdrop/0?fillWidth=560&quality=82")
              obj.optJSONObject("ImageTags")?.has("Thumb") == true ->
                append("$baseUrl/Items/$id/Images/Thumb?fillWidth=560&quality=82")
              parentBackdropId.isNotEmpty() ->
                append("$baseUrl/Items/$parentBackdropId/Images/Backdrop/0?fillWidth=560&quality=82")
              seriesId.isNotEmpty() ->
                append("$baseUrl/Items/$seriesId/Images/Backdrop/0?fillWidth=560&quality=82")
              else ->
                append("$baseUrl/Items/$id/Images/Primary?fillWidth=560&quality=82")
            }
          }

          val title = if (isEpisode && seriesName.isNotEmpty()) seriesName else name
          val subtitle = if (isEpisode) {
            listOf(epCode, name).filter { it.isNotBlank() }.joinToString(" — ")
          } else {
            val yr = obj.optInt("ProductionYear", 0)
            if (yr > 0) "$yr • Resume" else "Resume"
          }

          seen[id] = ContinueWatchingItem(
            itemId = id,
            provider = provider,
            title = title.ifBlank { "Untitled" },
            subtitle = subtitle,
            badge = if (isNextUp && pct <= 0.01f) "Next Up" else epCode,
            remainingLabel = remLabel,
            progress = pct,
            imageUrl = imageUrl,
          )
        }
      }

      fun fetchJson(url: String): String? = runCatching {
        val conn = openConnection(url, token)
        try {
          if (conn.responseCode in 200..299) readResponseText(conn) else null
        } finally {
          conn.disconnect()
        }
      }.getOrNull()

      parseItems(fetchJson(resumeUrl), isNextUp = false)
      parseItems(fetchJson(nextUpUrl), isNextUp = true)
      seen.values.toList()
    }
  }

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
      val baseUrl = normalizeUrl(rawServerUrl)
      require(baseUrl.isNotEmpty()) { "Enter your Jellyfin server URL (e.g. http://192.168.1.50:8096)" }

      var resolvedToken = apiKey?.trim().orEmpty()
      var resolvedUserId = ""

      if (resolvedToken.isEmpty()) {
        val user = username?.trim().orEmpty()
        require(user.isNotEmpty()) { "Enter an API Key or a Jellyfin Username" }
        val authJson = authenticateByName(baseUrl, user, password.orEmpty())
        resolvedToken = authJson.optString("AccessToken", "").trim()
        resolvedUserId = authJson.optJSONObject("User")?.optString("Id", "").orEmpty().trim()
        require(resolvedToken.isNotEmpty()) { "Authentication failed — check username & password" }
      } else {
        resolvedUserId = fetchFirstUserId(baseUrl, resolvedToken).orEmpty()
      }

      val libraries = fetchLibrariesInternal(baseUrl, resolvedToken, resolvedUserId)
      val backdrops = fetchBackdropsInternal(
        baseUrl = baseUrl,
        token = resolvedToken,
        userId = resolvedUserId,
        mediaType = mediaType,
        libraryIds = libraryIds,
        limit = limit,
      )
      require(backdrops.isNotEmpty()) {
        "Connected to Jellyfin, but no movie or TV show backdrops were found in your library."
      }

      JellyfinAuthResult(
        serverUrl = baseUrl,
        token = resolvedToken,
        userId = resolvedUserId,
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
      val baseUrl = normalizeUrl(serverUrl)
      val effectiveUserId = userId.ifBlank { fetchFirstUserId(baseUrl, token).orEmpty() }
      fetchLibrariesInternal(baseUrl, token, effectiveUserId)
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
      val baseUrl = normalizeUrl(serverUrl)
      val effectiveUserId = userId.ifBlank { fetchFirstUserId(baseUrl, token).orEmpty() }
      fetchBackdropsInternal(
        baseUrl = baseUrl,
        token = token,
        userId = effectiveUserId,
        mediaType = mediaType,
        libraryIds = libraryIds,
        limit = limit,
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

        val decoded = BitmapFactory.decodeByteArray(
          bytes,
          0,
          bytes.size,
          BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
        decoded?.asImageBitmap()
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
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 800 || bounds.outHeight / sampleSize > 400) {
          sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
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
      val baseUrl = normalizeUrl(serverUrl)
      if (baseUrl.isEmpty() || token.isBlank()) return@runCatching emptyList()
      val effectiveUserId = userId.ifBlank { fetchFirstUserId(baseUrl, token).orEmpty() }
      val path = if (effectiveUserId.isNotBlank()) {
        "$baseUrl/Users/${URLEncoder.encode(effectiveUserId, "UTF-8")}/Items"
      } else {
        "$baseUrl/Items"
      }
      val encodedQuery = URLEncoder.encode(cleaned, "UTF-8")
      val safeLimit = limit.coerceIn(4, 30)
      val url = "$path?SearchTerm=$encodedQuery&Recursive=true&IncludeItemTypes=Movie,Series" +
        "&Fields=ProductionYear,CommunityRating&ImageTypeLimit=1&EnableImageTypes=Backdrop,Primary&Limit=$safeLimit"
      val conn = openConnection(url, token)
      try {
        val code = conn.responseCode
        if (code !in 200..299) {
          throw IllegalStateException("Jellyfin search returned HTTP $code")
        }
        val json = JSONObject(readResponseText(conn))
        val items = json.optJSONArray("Items") ?: return@runCatching emptyList()
        buildList {
          for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val id = item.optString("Id", "").trim()
            val name = item.optString("Name", "").trim()
            val type = item.optString("Type", "Movie").trim()
            if (id.isEmpty() || name.isEmpty()) continue
            val year = item.optInt("ProductionYear", 0).takeIf { it > 1900 }
            val rating = if (item.has("CommunityRating") && !item.isNull("CommunityRating")) {
              item.optDouble("CommunityRating", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }?.toFloat()
            } else {
              null
            }
            val backdropTag = item.optJSONArray("BackdropImageTags")?.optString(0, "")?.trim().orEmpty()
            val primaryTag = item.optJSONObject("ImageTags")?.optString("Primary", "")?.trim().orEmpty()
            val imageUrl = when {
              primaryTag.isNotEmpty() ->
                "$baseUrl/Items/$id/Images/Primary?maxHeight=380&maxWidth=260&quality=85&tag=${URLEncoder.encode(primaryTag, "UTF-8")}"
              backdropTag.isNotEmpty() ->
                "$baseUrl/Items/$id/Images/Backdrop/0?maxWidth=320&quality=82&tag=${URLEncoder.encode(backdropTag, "UTF-8")}"
              else -> null
            }
            add(
              JellyfinSearchItem(
                itemId = id,
                title = name,
                itemType = type,
                year = year,
                communityRating = rating,
                imageUrl = imageUrl,
              ),
            )
          }
        }
      } finally {
        conn.disconnect()
      }
    }
  }

  override suspend fun downloadThumbnail(
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
        while ((bounds.outWidth / sampleSize) > 480 || (bounds.outHeight / sampleSize) > 360) {
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

  private fun authenticateByName(
    baseUrl: String,
    username: String,
    password: String,
  ): JSONObject {
    val conn = openConnection("$baseUrl/Users/AuthenticateByName", token = null).apply {
      requestMethod = "POST"
      doOutput = true
      setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    }
    try {
      val body = JSONObject().apply {
        put("Username", username)
        put("Pw", password)
      }.toString()
      OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

      val code = conn.responseCode
      if (code !in 200..299) {
        throw IllegalStateException("Jellyfin sign-in failed (HTTP $code). Check username/password.")
      }
      val text = readResponseText(conn)
      return JSONObject(text)
    } finally {
      conn.disconnect()
    }
  }

  private fun fetchFirstUserId(baseUrl: String, token: String): String? {
    return runCatching {
      val conn = openConnection("$baseUrl/Users", token)
      try {
        if (conn.responseCode !in 200..299) return@runCatching null
        val arr = JSONArray(readResponseText(conn))
        if (arr.length() > 0) arr.optJSONObject(0)?.optString("Id", "")?.trim() else null
      } finally {
        conn.disconnect()
      }
    }.getOrNull()
  }

  private fun fetchLibrariesInternal(
    baseUrl: String,
    token: String,
    userId: String,
  ): List<JellyfinLibrary> {
    if (userId.isBlank()) return emptyList()
    return runCatching {
      val url = "$baseUrl/Users/${URLEncoder.encode(userId, "UTF-8")}/Views"
      val conn = openConnection(url, token)
      try {
        if (conn.responseCode !in 200..299) return@runCatching emptyList()
        val json = JSONObject(readResponseText(conn))
        val items = json.optJSONArray("Items") ?: return@runCatching emptyList()
        buildList {
          for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val id = item.optString("Id", "").trim()
            val name = item.optString("Name", "").trim()
            val type = item.optString("CollectionType", "").trim().lowercase()
            if (id.isEmpty() || name.isEmpty()) continue
            // Include video/movie/tv/mixed libraries (exclude pure music/books/playlists)
            if (type in setOf("", "movies", "tvshows", "boxsets", "homevideos")) {
              add(JellyfinLibrary(id = id, name = name, collectionType = type))
            }
          }
        }
      } finally {
        conn.disconnect()
      }
    }.getOrDefault(emptyList())
  }

  private fun fetchBackdropsInternal(
    baseUrl: String,
    token: String,
    userId: String,
    mediaType: JellyfinMediaType,
    libraryIds: Set<String>,
    limit: Int,
  ): List<JellyfinBackdrop> {
    val safeLimit = limit.coerceIn(10, 500)
    val cleanedIds = libraryIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    if (cleanedIds.isEmpty()) {
      return fetchBackdropsForSingleParent(baseUrl, token, userId, mediaType, parentId = null, limit = safeLimit)
    }
    if (cleanedIds.size == 1) {
      return fetchBackdropsForSingleParent(baseUrl, token, userId, mediaType, parentId = cleanedIds.first(), limit = safeLimit)
    }

    // Multi-library: fetch from each selected library and interleave fairly up to safeLimit
    val perLibraryLimit = ((safeLimit * 2) / cleanedIds.size).coerceIn(20, safeLimit.coerceAtMost(200))
    val perLibraryResults = mutableListOf<List<JellyfinBackdrop>>()
    var lastError: Throwable? = null

    for (parentId in cleanedIds) {
      val res = runCatching {
        fetchBackdropsForSingleParent(baseUrl, token, userId, mediaType, parentId = parentId, limit = perLibraryLimit)
      }
      res.onSuccess { list ->
        if (list.isNotEmpty()) perLibraryResults.add(list)
      }.onFailure { err ->
        lastError = err
      }
    }

    if (perLibraryResults.isEmpty()) {
      lastError?.let { throw it }
    }

    val merged = LinkedHashMap<String, JellyfinBackdrop>()
    var round = 0
    while (merged.size < safeLimit) {
      var addedInRound = false
      for (list in perLibraryResults) {
        if (round < list.size) {
          val item = list[round]
          if (!merged.containsKey(item.itemId)) {
            merged[item.itemId] = item
            if (merged.size >= safeLimit) break
          }
          addedInRound = true
        }
      }
      if (!addedInRound) break
      round++
    }
    return merged.values.toList().shuffled()
  }

  private fun fetchBackdropsForSingleParent(
    baseUrl: String,
    token: String,
    userId: String,
    mediaType: JellyfinMediaType,
    parentId: String?,
    limit: Int,
  ): List<JellyfinBackdrop> {
    val path = if (userId.isNotBlank()) {
      "$baseUrl/Users/${URLEncoder.encode(userId, "UTF-8")}/Items"
    } else {
      "$baseUrl/Items"
    }
    val safeLimit = limit.coerceIn(10, 500)
    val parentParam = if (!parentId.isNullOrBlank()) {
      "&ParentId=${URLEncoder.encode(parentId, "UTF-8")}"
    } else {
      ""
    }
    val query = "Recursive=true&IncludeItemTypes=${mediaType.apiTypes}&ImageTypes=Backdrop&EnableImageTypes=Backdrop,Logo&SortBy=Random&Limit=$safeLimit&Fields=ProductionYear$parentParam"
    val conn = openConnection("$path?$query", token)
    try {
      val code = conn.responseCode
      if (code == 401 || code == 403) {
        throw IllegalStateException("Unauthorized (HTTP $code) — check your Jellyfin API key or login.")
      }
      if (code !in 200..299) {
        throw IllegalStateException("Jellyfin returned HTTP $code — check your server URL.")
      }

      val json = JSONObject(readResponseText(conn))
      val items = json.optJSONArray("Items") ?: return emptyList()
      val results = mutableListOf<JellyfinBackdrop>()
      for (i in 0 until items.length()) {
        val item = items.optJSONObject(i) ?: continue
        val id = item.optString("Id", "").trim()
        val name = item.optString("Name", "").trim()
        val tags = item.optJSONArray("BackdropImageTags")
        if (id.isEmpty() || tags == null || tags.length() == 0) continue
        val tag = tags.optString(0, "").trim()
        val year = item.optInt("ProductionYear", 0).takeIf { it > 1900 }
        val tagParam = if (tag.isNotEmpty()) "&tag=${URLEncoder.encode(tag, "UTF-8")}" else ""
        val imageUrl = "$baseUrl/Items/$id/Images/Backdrop/0?maxWidth=1920&quality=88$tagParam"

        val logoTag = item.optJSONObject("ImageTags")?.optString("Logo", "").orEmpty().trim()
        val logoUrl = if (logoTag.isNotEmpty()) {
          "$baseUrl/Items/$id/Images/Logo?maxWidth=640&maxHeight=220&tag=${URLEncoder.encode(logoTag, "UTF-8")}"
        } else {
          null
        }

        results.add(
          JellyfinBackdrop(
            itemId = id,
            title = name,
            year = year,
            imageUrl = imageUrl,
            logoUrl = logoUrl,
          ),
        )
      }
      return results.shuffled()
    } finally {
      conn.disconnect()
    }
  }

  private fun openConnection(urlString: String, token: String?): HttpURLConnection {
    return (URL(urlString).openConnection() as HttpURLConnection).apply {
      connectTimeout = 8000
      readTimeout = 10000
      setRequestProperty("Accept", "application/json")
      val authHeader = buildString {
        append("MediaBrowser Client=\"Horizon TV Launcher\", Device=\"Android TV\", DeviceId=\"horizon-tv-launcher\", Version=\"1.7.0\"")
        if (!token.isNullOrBlank()) {
          append(", Token=\"$token\"")
        }
      }
      setRequestProperty("Authorization", authHeader)
      if (!token.isNullOrBlank()) {
        setRequestProperty("X-Emby-Token", token)
      }
    }
  }

  private fun readResponseText(conn: HttpURLConnection): String {
    return BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
  }

  companion object {
    fun normalizeUrl(raw: String): String {
      val trimmed = raw.trim().trimEnd('/')
      if (trimmed.isEmpty()) return ""
      return if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
      ) {
        trimmed
      } else {
        "http://$trimmed"
      }
    }
  }
}
