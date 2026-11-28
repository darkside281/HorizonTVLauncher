package com.horizontv.launcher.data

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.horizontv.launcher.model.JellyfinBackdrop
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JellyfinSearchItem
import com.horizontv.launcher.model.LauncherSettings
import com.horizontv.launcher.model.RedditSort
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Projectivy-style Reddit wallpaper fetcher implementing [JellyfinDataSource].
 *
 * Uses the public Arctic Shift Reddit Posts API as primary (since Reddit blocks direct
 * unauthenticated `.json` requests on many networks with HTTP 403), falls back to direct
 * Reddit JSON, and falls back to Wallhaven 16:9 HD search if both are unavailable.
 */
class RedditWallpaperClient : JellyfinDataSource {

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
      val sort = RedditSort.fromId(rawServerUrl)
      val backdrops = fetchBackdropsInternal(sort, libraryIds, limit)
      JellyfinAuthResult(
        serverUrl = sort.id,
        token = "",
        userId = "",
        libraries = presetLibraries(),
        backdrops = backdrops,
      )
    }
  }

  override suspend fun fetchLibraries(
    serverUrl: String,
    token: String,
    userId: String,
  ): Result<List<JellyfinLibrary>> = Result.success(presetLibraries())

  override suspend fun fetchBackdrops(
    serverUrl: String,
    token: String,
    userId: String,
    mediaType: JellyfinMediaType,
    libraryIds: Set<String>,
    limit: Int,
  ): Result<List<JellyfinBackdrop>> = withContext(Dispatchers.IO) {
    runCatching {
      val sort = RedditSort.fromId(serverUrl)
      fetchBackdropsInternal(sort, libraryIds, limit)
    }
  }

  override suspend fun downloadBackdropImage(
    imageUrl: String,
    token: String,
  ): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
      val conn = openConnection(imageUrl)
      try {
        if (conn.responseCode !in 200..299) return@runCatching null
        val bytes = conn.inputStream.use { it.readBytes() }
        if (bytes.isEmpty()) return@runCatching null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        // Skip portrait/phone images that slipped through without preview dimensions
        if (bounds.outWidth < bounds.outHeight) return@runCatching null

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
  ): ImageBitmap? = null

  override suspend fun searchItems(
    serverUrl: String,
    token: String,
    userId: String,
    query: String,
    limit: Int,
  ): Result<List<JellyfinSearchItem>> = Result.success(emptyList())

  override suspend fun downloadThumbnail(
    imageUrl: String,
    token: String,
  ): ImageBitmap? = null

  private fun presetLibraries(): List<JellyfinLibrary> =
    LauncherSettings.PRESET_REDDIT_SUBREDDITS.map { sub ->
      JellyfinLibrary(
        id = sub,
        name = LauncherSettings.friendlyRedditLabel(sub),
        collectionType = "reddit",
      )
    }

  private fun fetchBackdropsInternal(
    sort: RedditSort,
    libraryIds: Set<String>,
    limit: Int,
  ): List<JellyfinBackdrop> {
    val safeLimit = limit.coerceIn(10, 500)
    val subs = libraryIds
      .map { sanitizeSubreddit(it) }
      .filter { it.isNotEmpty() }
      .distinct()
      .ifEmpty { LauncherSettings.DEFAULT_REDDIT_SUBREDDITS.toList() }

    val perSubResults = mutableListOf<List<JellyfinBackdrop>>()
    var lastError: Throwable? = null

    for (sub in subs) {
      val res = runCatching { fetchSubredditWallpapers(sub, sort) }
      res.onSuccess { list ->
        if (list.isNotEmpty()) perSubResults.add(list)
      }.onFailure { err ->
        lastError = err
      }
    }

    if (perSubResults.isEmpty()) {
      lastError?.let { throw it }
      return emptyList()
    }

    // Interleave evenly across selected subreddits and shuffle
    val merged = LinkedHashMap<String, JellyfinBackdrop>()
    var round = 0
    while (merged.size < safeLimit) {
      var added = false
      for (list in perSubResults) {
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

  private fun fetchSubredditWallpapers(
    subreddit: String,
    sort: RedditSort,
  ): List<JellyfinBackdrop> {
    val combined = LinkedHashMap<String, JellyfinBackdrop>()

    // 1. Primary: Arctic Shift public Reddit posts API (reliable HTTP 200 even when reddit.com returns 403)
    val arcticResult = runCatching { fetchFromArcticShift(subreddit, sort) }
    arcticResult.getOrNull()?.forEach { combined.putIfAbsent(it.itemId, it) }

    // 2. Fallback 1: Direct Reddit JSON (if Arctic Shift returned fewer than 12 usable landscape wallpapers)
    val directResult = if (combined.size < 12) {
      runCatching { fetchFromDirectReddit(subreddit, sort) }.also { res ->
        res.getOrNull()?.forEach { combined.putIfAbsent(it.itemId, it) }
      }
    } else {
      Result.success(emptyList())
    }

    // 3. Fallback 2: Top up with Wallhaven 16:9 1080p+ widescreen wallpapers if Reddit returned too few
    if (combined.size < 12) {
      runCatching { fetchFromWallhavenFallback(subreddit) }
        .getOrNull()
        ?.forEach { combined.putIfAbsent(it.itemId, it) }
    }

    if (combined.isNotEmpty()) return combined.values.toList().shuffled()

    throw (arcticResult.exceptionOrNull()
      ?: directResult.exceptionOrNull()
      ?: IllegalStateException("Could not load wallpapers for r/$subreddit"))
  }

  private fun fetchFromArcticShift(
    subreddit: String,
    sort: RedditSort,
  ): List<JellyfinBackdrop> {
    val encodedSub = URLEncoder.encode(subreddit, "UTF-8")
    val afterParam = when (sort) {
      RedditSort.HOT -> ""
      RedditSort.TOP_WEEK -> "&after=7d"
      RedditSort.TOP_MONTH -> "&after=30d"
      RedditSort.TOP_ALL -> "&after=365d"
    }
    val url = "https://arctic-shift.photon-reddit.com/api/posts/search?subreddit=$encodedSub&limit=100&sort=desc$afterParam"
    val conn = openConnection(url)
    try {
      val code = conn.responseCode
      if (code !in 200..299) {
        throw IllegalStateException("Reddit archive API returned HTTP $code for r/$subreddit")
      }
      val root = JSONObject(readResponseText(conn))
      val posts = root.optJSONArray("data") ?: return emptyList()
      return extractWallpapersFromRedditPosts(posts, subreddit, isWrappedChildren = false, sortByScore = sort != RedditSort.HOT)
    } finally {
      conn.disconnect()
    }
  }

  private fun fetchFromDirectReddit(
    subreddit: String,
    sort: RedditSort,
  ): List<JellyfinBackdrop> {
    val encodedSub = URLEncoder.encode(subreddit, "UTF-8")
    val url = "https://www.reddit.com/r/$encodedSub/${sort.pathSuffix}"
    val conn = openConnection(url)
    try {
      val code = conn.responseCode
      if (code !in 200..299) {
        throw IllegalStateException("Reddit returned HTTP $code for r/$subreddit")
      }
      val root = JSONObject(readResponseText(conn))
      val children = root.optJSONObject("data")?.optJSONArray("children") ?: return emptyList()
      return extractWallpapersFromRedditPosts(children, subreddit, isWrappedChildren = true, sortByScore = false)
    } finally {
      conn.disconnect()
    }
  }

  private fun extractWallpapersFromRedditPosts(
    array: JSONArray,
    subreddit: String,
    isWrappedChildren: Boolean,
    sortByScore: Boolean,
  ): List<JellyfinBackdrop> {
    val scored = mutableListOf<Pair<Int, JellyfinBackdrop>>()
    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i) ?: continue
      val data = if (isWrappedChildren) obj.optJSONObject("data") ?: continue else obj
      if (data.optBoolean("over_18", false)) continue
      if (data.optBoolean("is_video", false)) continue
      if (data.optBoolean("stickied", false)) continue

      val id = data.optString("id", "").trim()
      if (id.isEmpty()) continue

      var imageUrl = ""
      if (data.optBoolean("is_gallery", false)) {
        // Extract first landscape high-res image from Reddit gallery media_metadata
        val meta = data.optJSONObject("media_metadata")
        if (meta != null) {
          val keys = meta.keys()
          while (keys.hasNext()) {
            val entry = meta.optJSONObject(keys.next()) ?: continue
            val s = entry.optJSONObject("s") ?: continue
            val w = s.optInt("x", 0)
            val h = s.optInt("y", 0)
            val u = s.optString("u", "").replace("&amp;", "&").trim()
            if (w >= 1000 && w > h && u.startsWith("https://")) {
              imageUrl = u
              break
            }
          }
        }
      } else {
        val previewSource = data.optJSONObject("preview")
          ?.optJSONArray("images")
          ?.optJSONObject(0)
          ?.optJSONObject("source")
        val width = previewSource?.optInt("width", 0) ?: 0
        val height = previewSource?.optInt("height", 0) ?: 0

        // Keep landscape/widescreen TV-friendly images only
        if (width > 0 && height > 0) {
          if (width < 1000 || width < height) continue
        }

        val directUrl = data.optString("url_overridden_by_dest", data.optString("url", "")).trim()
        val isDirectImage = directUrl.startsWith("https://") && (
          directUrl.endsWith(".jpg", ignoreCase = true) ||
            directUrl.endsWith(".jpeg", ignoreCase = true) ||
            directUrl.endsWith(".png", ignoreCase = true) ||
            directUrl.endsWith(".webp", ignoreCase = true)
          )
        val previewUrl = previewSource?.optString("url", "")?.replace("&amp;", "&")?.trim().orEmpty()
        imageUrl = when {
          isDirectImage -> directUrl
          previewUrl.startsWith("https://") -> previewUrl
          else -> ""
        }
      }
      if (imageUrl.isEmpty()) continue

      val rawSub = data.optString("subreddit", subreddit).trim().ifEmpty { subreddit }
      val subDisplay = "${LauncherSettings.friendlyRedditLabel(rawSub)} • Reddit"
      val score = data.optInt("score", 0)
      scored.add(
        score to JellyfinBackdrop(
          itemId = "reddit_$id",
          title = subDisplay,
          year = null,
          imageUrl = imageUrl,
          logoUrl = null,
        ),
      )
    }
    val ordered = if (sortByScore) {
      scored.sortedByDescending { it.first }.map { it.second }
    } else {
      scored.map { it.second }
    }
    return ordered.shuffled()
  }

  private fun fetchFromWallhavenFallback(subreddit: String): List<JellyfinBackdrop> {
    val topic = when (subreddit.lowercase()) {
      "earthporn", "natureporn" -> "landscape nature"
      "spaceporn" -> "space galaxy"
      "cityporn", "architectureporn" -> "cityscape skyline"
      "skyporn" -> "sunset clouds sky"
      "minimalwallpaper" -> "minimalist"
      "imaginarylandscapes" -> "fantasy landscape"
      "wallpapers", "wallpaper", "widescreenwallpaper" -> "scenery"
      "christmas" -> "christmas tree snow lights"
      "winterporn" -> "winter snow mountains"
      else -> subreddit
    }
    val encoded = URLEncoder.encode(topic, "UTF-8")
    val seed = (100000..999999).random()
    val url = "https://wallhaven.cc/api/v1/search?q=$encoded&atleast=1920x1080&ratios=16x9&purity=100&sorting=random&seed=$seed"
    val conn = openConnection(url)
    try {
      if (conn.responseCode !in 200..299) return emptyList()
      val root = JSONObject(readResponseText(conn))
      val data = root.optJSONArray("data") ?: return emptyList()
      val results = mutableListOf<JellyfinBackdrop>()
      for (i in 0 until data.length()) {
        val item = data.optJSONObject(i) ?: continue
        val id = item.optString("id", "").trim()
        val path = item.optString("path", "").trim()
        if (id.isEmpty() || !path.startsWith("https://")) continue
        results.add(
          JellyfinBackdrop(
            itemId = "wh_$id",
            title = "${LauncherSettings.friendlyRedditLabel(subreddit)} • Reddit",
            year = null,
            imageUrl = path,
            logoUrl = null,
          ),
        )
      }
      return results.shuffled()
    } finally {
      conn.disconnect()
    }
  }

  private fun openConnection(urlString: String): HttpURLConnection {
    return (URL(urlString).openConnection() as HttpURLConnection).apply {
      connectTimeout = 8000
      readTimeout = 10000
      instanceFollowRedirects = true
      setRequestProperty("Accept", "application/json")
      setRequestProperty("User-Agent", "HorizonTVLauncher/2.1.1 (Android TV)")
    }
  }

  private fun readResponseText(conn: HttpURLConnection): String {
    return BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
  }

  companion object {
    fun sanitizeSubreddit(raw: String): String {
      return raw.trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.reddit.com/")
        .removePrefix("old.reddit.com/")
        .removePrefix("reddit.com/")
        .removePrefix("/r/")
        .removePrefix("r/")
        .substringBefore("/")
        .replace(Regex("[^a-zA-Z0-9_]"), "")
    }
  }
}
