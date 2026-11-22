package com.horizontv.launcher.data

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.horizontv.launcher.model.JellyfinBackdrop
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JellyfinSearchItem
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class TmdbWallpaperClient : JellyfinDataSource {

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
      val cleanKey = apiKey?.trim().orEmpty()
      require(cleanKey.isNotEmpty()) { "Please enter your TMDB API key or Read Access Token" }
      val json = requestJson("$API_BASE/configuration", cleanKey)
      if (!json.has("images")) {
        throw IllegalStateException("Unexpected response from TMDB")
      }
      val libs = CATEGORIES.map { (id, label, kind) ->
        JellyfinLibrary(id = id, name = label, collectionType = kind)
      }
      val backdrops = fetchBackdrops(
        serverUrl = API_BASE,
        token = cleanKey,
        userId = "",
        mediaType = mediaType,
        libraryIds = libraryIds.ifEmpty { setOf("trending_all", "popular_movie") },
        limit = limit,
      ).getOrThrow()
      JellyfinAuthResult(
        serverUrl = API_BASE,
        token = cleanKey,
        userId = "",
        libraries = libs,
        backdrops = backdrops,
      )
    }
  }

  override suspend fun fetchLibraries(
    serverUrl: String,
    token: String,
    userId: String,
  ): Result<List<JellyfinLibrary>> = Result.success(
    CATEGORIES.map { (id, label, kind) ->
      JellyfinLibrary(id = id, name = label, collectionType = kind)
    },
  )

  override suspend fun fetchBackdrops(
    serverUrl: String,
    token: String,
    userId: String,
    mediaType: JellyfinMediaType,
    libraryIds: Set<String>,
    limit: Int,
  ): Result<List<JellyfinBackdrop>> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanKey = token.trim()
      require(cleanKey.isNotEmpty()) { "TMDB API key is missing" }
      val targetLimit = limit.coerceIn(10, 200)

      val selectedIds = libraryIds.ifEmpty { setOf("trending_all", "popular_movie") }
      val activeCategories = CATEGORIES.filter { (id, _, kind) ->
        id in selectedIds && when (mediaType) {
          JellyfinMediaType.BOTH -> true
          JellyfinMediaType.MOVIES -> kind == "movie" || kind == "all"
          JellyfinMediaType.TV_SHOWS -> kind == "tv" || kind == "all"
        }
      }.ifEmpty {
        CATEGORIES.filter { (id, _, _) -> id in selectedIds }.ifEmpty { listOf(CATEGORIES.first()) }
      }

      val perCategoryBuckets = mutableListOf<List<JellyfinBackdrop>>()
      for ((id, _, _) in activeCategories) {
        val path = CATEGORY_PATHS[id] ?: continue
        val bucket = mutableListOf<JellyfinBackdrop>()
        // Fetch up to 2 pages per category if needed
        val pages = if (activeCategories.size == 1 && targetLimit > 20) 2 else 1
        for (page in 1..pages) {
          val sep = if ('?' in path) '&' else '?'
          val url = "$API_BASE$path${sep}language=en-US&page=$page"
          val json = runCatching { requestJson(url, cleanKey) }.getOrNull() ?: continue
          val results = json.optJSONArray("results") ?: continue
          for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val backdropPath = item.optString("backdrop_path", "").trim()
            if (backdropPath.isEmpty() || backdropPath == "null") continue

            val itemId = item.optInt("id", 0)
            if (itemId <= 0) continue

            val itemKind = item.optString("media_type", "").trim().ifEmpty {
              if (path.startsWith("/tv/")) "tv" else "movie"
            }
            if (mediaType == JellyfinMediaType.MOVIES && itemKind != "movie") continue
            if (mediaType == JellyfinMediaType.TV_SHOWS && itemKind != "tv") continue

            val title = item.optString("title", "").trim()
              .ifEmpty { item.optString("name", "").trim() }
            if (title.isEmpty()) continue

            val dateStr = item.optString("release_date", "").trim()
              .ifEmpty { item.optString("first_air_date", "").trim() }
            val year = dateStr.take(4).toIntOrNull()

            bucket.add(
              JellyfinBackdrop(
                itemId = "tmdb_${itemKind}_$itemId",
                title = title,
                year = year,
                imageUrl = "$IMAGE_BASE_BACKDROP$backdropPath",
                logoUrl = "$LOGO_SCHEME$itemKind:$itemId",
              ),
            )
          }
        }
        if (bucket.isNotEmpty()) {
          perCategoryBuckets.add(bucket.shuffled())
        }
      }

      // Round-robin interleave across selected categories so every category is represented
      val seen = HashSet<String>()
      val merged = mutableListOf<JellyfinBackdrop>()
      var idx = 0
      while (merged.size < targetLimit) {
        var addedAny = false
        for (bucket in perCategoryBuckets) {
          if (idx < bucket.size) {
            val candidate = bucket[idx]
            if (seen.add(candidate.itemId)) {
              merged.add(candidate)
              if (merged.size >= targetLimit) break
            }
            addedAny = true
          }
        }
        if (!addedAny) break
        idx += 1
      }
      if (merged.isEmpty()) {
        throw IllegalStateException("No TMDB backdrops found for the selected categories")
      }
      merged.shuffled()
    }
  }

  override suspend fun downloadBackdropImage(
    imageUrl: String,
    token: String,
  ): ImageBitmap? = downloadBitmap(imageUrl, token, maxWidth = 1920, maxHeight = 1080)

  override suspend fun downloadLogoImage(
    logoUrl: String,
    token: String,
  ): ImageBitmap? = downloadBitmap(logoUrl, token, maxWidth = 600, maxHeight = 260)

  private suspend fun downloadBitmap(
    url: String,
    token: String,
    maxWidth: Int,
    maxHeight: Int,
  ): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
      val resolvedUrl = if (url.startsWith(LOGO_SCHEME)) {
        resolveLogoImageUrl(url.removePrefix(LOGO_SCHEME), token.trim()) ?: return@runCatching null
      } else {
        url
      }
      val conn = (URL(resolvedUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 9000
        readTimeout = 12000
        setRequestProperty("User-Agent", "HorizonTVLauncher/2.4")
      }
      try {
        if (conn.responseCode !in 200..299) return@runCatching null
        val bytes = conn.inputStream.use { it.readBytes() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxWidth || bounds.outHeight / sample > maxHeight) {
          sample *= 2
        }
        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decode)?.asImageBitmap()
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
  ): Result<List<JellyfinSearchItem>> = Result.success(emptyList())

  override suspend fun downloadThumbnail(url: String, token: String): ImageBitmap? = null

  private fun resolveLogoImageUrl(spec: String, apiKey: String): String? {
    val parts = spec.split(":")
    if (parts.size != 2 || apiKey.isEmpty()) return null
    val kind = parts[0]
    val id = parts[1]
    val url = "$API_BASE/$kind/$id/images?include_image_language=en,null"
    val json = runCatching { requestJson(url, apiKey) }.getOrNull() ?: return null
    val logos = json.optJSONArray("logos") ?: return null
    var bestPath: String? = null
    var bestScore = -1.0
    for (i in 0 until logos.length()) {
      val logo = logos.optJSONObject(i) ?: continue
      val path = logo.optString("file_path", "").trim()
      if (path.isEmpty() || path.endsWith(".svg", ignoreCase = true)) continue
      val lang = logo.optString("iso_639_1", "")
      val votes = logo.optDouble("vote_average", 0.0)
      val aspect = logo.optDouble("aspect_ratio", 1.0)
      val score = (if (lang == "en") 100.0 else 0.0) + votes + (if (aspect >= 1.2) 5.0 else 0.0)
      if (score > bestScore) {
        bestScore = score
        bestPath = path
      }
    }
    return bestPath?.let { "$IMAGE_BASE_LOGO$it" }
  }

  private fun requestJson(baseUrl: String, keyOrToken: String): JSONObject {
    val isBearerToken = keyOrToken.length > 45 || '.' in keyOrToken
    val finalUrl = if (isBearerToken) {
      baseUrl
    } else {
      val sep = if ('?' in baseUrl) '&' else '?'
      "$baseUrl${sep}api_key=${URLEncoder.encode(keyOrToken, "UTF-8")}"
    }
    val conn = (URL(finalUrl).openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 9000
      readTimeout = 12000
      setRequestProperty("Accept", "application/json")
      setRequestProperty("User-Agent", "HorizonTVLauncher/2.4")
      if (isBearerToken) {
        setRequestProperty("Authorization", "Bearer $keyOrToken")
      }
    }
    return try {
      val code = conn.responseCode
      val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
        ?.bufferedReader()
        ?.use { it.readText() }
        .orEmpty()
      if (code == 401) {
        throw IllegalStateException("Invalid TMDB API key or Read Access Token (HTTP 401)")
      }
      if (code !in 200..299) {
        throw IllegalStateException("TMDB returned HTTP $code")
      }
      JSONObject(raw)
    } finally {
      conn.disconnect()
    }
  }

  companion object {
    const val API_BASE = "https://api.themoviedb.org/3"
    private const val IMAGE_BASE_BACKDROP = "https://image.tmdb.org/t/p/original"
    private const val IMAGE_BASE_LOGO = "https://image.tmdb.org/t/p/w500"
    private const val LOGO_SCHEME = "tmdb_logo:"

    val CATEGORIES: List<Triple<String, String, String>> = listOf(
      Triple("trending_all", "Trending Now", "all"),
      Triple("popular_movie", "Popular Movies", "movie"),
      Triple("top_rated_movie", "Top Rated Movies", "movie"),
      Triple("now_playing_movie", "Now Playing", "movie"),
      Triple("popular_tv", "Popular TV Shows", "tv"),
      Triple("top_rated_tv", "Top Rated TV Shows", "tv"),
    )

    private val CATEGORY_PATHS: Map<String, String> = mapOf(
      "trending_all" to "/trending/all/week",
      "popular_movie" to "/movie/popular",
      "top_rated_movie" to "/movie/top_rated",
      "now_playing_movie" to "/movie/now_playing",
      "popular_tv" to "/tv/popular",
      "top_rated_tv" to "/tv/top_rated",
    )
  }
}
