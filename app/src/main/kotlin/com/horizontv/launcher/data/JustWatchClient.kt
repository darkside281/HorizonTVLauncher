package com.horizontv.launcher.data

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.horizontv.launcher.model.StreamingOffer
import com.horizontv.launcher.model.StreamingSearchItem
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class StreamingSearchPage(
  val items: List<StreamingSearchItem>,
  val hasNextPage: Boolean = false,
  val endCursor: String? = null,
)

interface StreamingSearchDataSource {
  suspend fun searchTitles(
    query: String,
    countryCode: String = "GB",
    limit: Int = 8,
  ): Result<List<StreamingSearchItem>>

  suspend fun searchTitlesPage(
    query: String,
    countryCode: String = "GB",
    limit: Int = 8,
    afterCursor: String? = null,
  ): Result<StreamingSearchPage> = searchTitles(query, countryCode, limit).map { StreamingSearchPage(it) }

  suspend fun downloadPoster(url: String): ImageBitmap?
}

class JustWatchClient : StreamingSearchDataSource {

  override suspend fun searchTitles(
    query: String,
    countryCode: String,
    limit: Int,
  ): Result<List<StreamingSearchItem>> =
    searchTitlesPage(query = query, countryCode = countryCode, limit = limit, afterCursor = null)
      .map { it.items }

  override suspend fun searchTitlesPage(
    query: String,
    countryCode: String,
    limit: Int,
    afterCursor: String?,
  ): Result<StreamingSearchPage> = withContext(Dispatchers.IO) {
    runCatching {
      val trimmed = query.trim()
      if (trimmed.length < 2) return@runCatching StreamingSearchPage(emptyList())

      val payload = JSONObject().apply {
        put("operationName", "GetSuggestedTitles")
        put(
          "variables",
          JSONObject().apply {
            put("country", countryCode.uppercase())
            put("language", "en")
            put("first", limit.coerceIn(1, 20))
            if (!afterCursor.isNullOrBlank()) {
              put("after", afterCursor)
            } else {
              put("after", JSONObject.NULL)
            }
            put("filter", JSONObject().put("searchQuery", trimmed))
          },
        )
        put("query", SEARCH_GRAPHQL_QUERY)
      }

      val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 9000
        readTimeout = 10000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV) HorizonTVLauncher/2.3")
      }
      try {
        conn.outputStream.use { out ->
          out.write(payload.toString().toByteArray(Charsets.UTF_8))
        }
        val code = conn.responseCode
        val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
          ?.bufferedReader()
          ?.use { it.readText() }
          .orEmpty()
        if (code !in 200..299) {
          throw IllegalStateException("JustWatch returned HTTP $code")
        }
        parseSearchPage(raw)
      } finally {
        conn.disconnect()
      }
    }
  }

  override suspend fun downloadPoster(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
      val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 6000
        readTimeout = 8000
        setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV) HorizonTVLauncher/2.3")
      }
      try {
        if (conn.responseCode !in 200..299) return@runCatching null
        val bytes = conn.inputStream.use { it.readBytes() }
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        var sample = 1
        while (opts.outWidth / sample > 260 || opts.outHeight / sample > 380) {
          sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)?.asImageBitmap()
      } finally {
        conn.disconnect()
      }
    }.getOrNull()
  }

  private fun parseSearchPage(rawJson: String): StreamingSearchPage {
    val root = JSONObject(rawJson)
    val popular = root.optJSONObject("data")
      ?.optJSONObject("popularTitles")
      ?: return StreamingSearchPage(emptyList())
    val pageInfo = popular.optJSONObject("pageInfo")
    val hasNextPage = pageInfo?.optBoolean("hasNextPage", false) ?: false
    val endCursor = pageInfo?.optString("endCursor", "")?.trim()?.ifEmpty { null }
    val edges = popular.optJSONArray("edges")
      ?: return StreamingSearchPage(emptyList(), hasNextPage = false, endCursor = null)

    val results = mutableListOf<StreamingSearchItem>()
    for (i in 0 until edges.length()) {
      val node = edges.optJSONObject(i)?.optJSONObject("node") ?: continue
      val id = node.optString("id", "").trim()
      val objectType = node.optString("objectType", "MOVIE").trim()
      val content = node.optJSONObject("content") ?: continue
      val title = content.optString("title", "").trim()
      if (id.isEmpty() || title.isEmpty()) continue

      val year = content.optInt("originalReleaseYear", 0).takeIf { it > 1800 }
      val shortDescription = if (!content.isNull("shortDescription")) {
        content.optString("shortDescription", "").trim().replace(Regex("\\s+"), " ").ifEmpty { null }
      } else {
        null
      }
      val ageCertification = if (!content.isNull("ageCertification")) {
        content.optString("ageCertification", "").trim().ifEmpty { null }
      } else {
        null
      }
      val scoring = content.optJSONObject("scoring")
      val imdbScore = scoring
        ?.optDouble("imdbScore", Double.NaN)
        ?.takeIf { !it.isNaN() && it > 0.0 }
        ?.toFloat()
      val tomatoMeter = if (scoring != null && !scoring.isNull("tomatoMeter")) {
        scoring.optInt("tomatoMeter", -1).takeIf { it in 0..100 }
      } else {
        null
      }
      val certifiedFresh = scoring?.optBoolean("certifiedFresh", false) ?: false
      val rawPoster = content.optString("posterUrl", "").trim()
      val posterUrl = if (rawPoster.isNotEmpty()) {
        val resolved = rawPoster
          .replace("{profile}", "s332")
          .replace("{format}", "jpg")
        if (resolved.startsWith("http")) resolved else "https://images.justwatch.com$resolved"
      } else {
        null
      }

      val offersArr = node.optJSONArray("offers")
      val offersWebArr = node.optJSONArray("offersWeb")
      val byProvider = LinkedHashMap<String, StreamingOffer>()

      fun processOffers(arr: org.json.JSONArray?) {
        if (arr == null) return
        for (j in 0 until arr.length()) {
          val o = arr.optJSONObject(j) ?: continue
          val pkg = o.optJSONObject("package") ?: continue
          val rawName = pkg.optString("clearName", "").trim()
          val shortName = pkg.optString("shortName", "").trim()
          val monetization = o.optString("monetizationType", "").uppercase()
          if (rawName.isEmpty()) continue

          val normalizedName = normalizeProviderName(rawName, shortName)
          val rawDeeplink = o.optString("deeplinkURL", "").trim().ifEmpty { null }
          val rawWebUrl = o.optString("standardWebURL", "").trim().ifEmpty { null }
          if (rawDeeplink == null && rawWebUrl == null) continue

          val (deeplink, webUrl) = preferOverviewUrl(
            rawDeeplink = rawDeeplink,
            rawWebUrl = rawWebUrl,
          )

          val candidate = StreamingOffer(
            providerName = normalizedName,
            shortName = shortName,
            monetizationType = monetization,
            deeplinkUrl = deeplink,
            webUrl = webUrl,
          )
          val key = normalizedName.lowercase()
          val existing = byProvider[key]
          if (existing == null ||
            offerPriority(candidate) < offerPriority(existing) ||
            (offerPriority(candidate) == offerPriority(existing) && existing.deeplinkUrl == null && candidate.deeplinkUrl != null)
          ) {
            byProvider[key] = candidate
          }
        }
      }

      processOffers(offersArr)
      processOffers(offersWebArr)

      val sortedOffers = byProvider.values
        .sortedBy { offerPriority(it) }

      results.add(
        StreamingSearchItem(
          id = id,
          title = title,
          objectType = objectType,
          year = year,
          shortDescription = shortDescription,
          ageCertification = ageCertification,
          imdbScore = imdbScore,
          tomatoMeter = tomatoMeter,
          certifiedFresh = certifiedFresh,
          posterUrl = posterUrl,
          offers = sortedOffers,
        ),
      )
    }
    return StreamingSearchPage(
      items = results,
      hasNextPage = hasNextPage && endCursor != null && results.isNotEmpty(),
      endCursor = endCursor,
    )
  }

  private fun normalizeProviderName(rawName: String, shortName: String): String {
    val trimmed = rawName.trim()
    val lower = trimmed.lowercase()
    val short = shortName.trim().lowercase()
    return when {
      lower == "netflix standard with ads" || lower == "netflix kids" || short == "nfa" || short == "nfk" -> "Netflix"
      lower == "amazon prime video with ads" || short == "pva" -> "Amazon Prime Video"
      lower == "paramount+ amazon channel" || lower == "paramount plus amazon channel" || short == "app" -> "Paramount+ (Prime)"
      lower == "mgm plus amazon channel" || lower == "mgm+ amazon channel" || lower == "mgm amazon channel" || short == "mgp" -> "MGM+ (Prime)"
      lower == "crunchyroll amazon channel" || short == "cra" -> "Crunchyroll (Prime)"
      lower == "discovery+ amazon channel" || short == "adp" -> "Discovery+ (Prime)"
      lower == "shudder amazon channel" || short == "asd" -> "Shudder (Prime)"
      lower == "mubi amazon channel" || short == "amb" -> "MUBI (Prime)"
      lower == "hayu amazon channel" || short == "ahy" -> "Hayu (Prime)"
      lower == "acorntv amazon channel" || lower == "acorn tv amazon channel" || short == "aac" -> "Acorn TV (Prime)"
      lower == "bfi player amazon channel" || short == "abf" -> "BFI Player (Prime)"
      lower == "curzon amazon channel" || short == "cac" -> "Curzon (Prime)"
      lower == "studiocanal presents amazon channel" || short == "soa" -> "Studiocanal (Prime)"
      lower == "hbo max amazon channel" || lower == "max amazon channel" || short == "aho" -> "HBO Max (Prime)"
      lower.endsWith(" amazon channel") -> {
        val prefix = trimmed.substring(0, trimmed.length - " amazon channel".length).trim()
        "$prefix (Prime)"
      }
      lower == "paramount plus" || lower == "paramount plus premium" || lower == "paramount plus basic with ads" ||
        short == "pmp" || short == "ppp" || short == "pba" -> "Paramount+"
      lower == "itvx premium" || short == "itp" -> "ITVX"
      lower == "channel 4 plus" || short == "a4p" -> "Channel 4"
      trimmed == "5" || short == "myf" -> "My5"
      else -> trimmed
    }
  }

  private fun preferOverviewUrl(
    rawDeeplink: String?,
    rawWebUrl: String?,
  ): Pair<String?, String?> {
    var deep = rawDeeplink
    var web = rawWebUrl

    // 1. Amazon Prime Video & Prime Video Channels: use /detail?gti=<gti> (without stream/episode asin) so it opens the Title Overview page
    if (deep != null && deep.contains("watch.amazon.", ignoreCase = true)) {
      val gtiMatch = web?.let { Regex("""[?&]gti=([^&#]+)""").find(it)?.groupValues?.getOrNull(1) }
      deep = if (!gtiMatch.isNullOrBlank()) {
        "intent://watch.amazon.co.uk/detail?gti=$gtiMatch#Intent;package=com.amazon.amazonvideo.livingroom;scheme=https;end"
      } else {
        deep
          .replace("/watch?", "/detail?")
          .replace(Regex("""([?&])time=0&"""), "$1")
          .replace(Regex("""&time=0\b"""), "")
      }
    }
    if (web != null && web.contains("watch.amazon.", ignoreCase = true)) {
      web = web
        .replace("/watch?", "/detail?")
        .replace(Regex("""([?&])time=0&"""), "$1")
        .replace(Regex("""&time=0\b"""), "")
    }

    // 2. Netflix: /title/<id> opens the Movie or Show overview instead of /watch/<id> auto-playback
    if (deep != null && deep.contains("netflix.com/watch/", ignoreCase = true)) {
      deep = deep.replace("/watch/", "/title/", ignoreCase = true)
    }
    if (web != null && web.contains("netflix.com/watch/", ignoreCase = true)) {
      web = web.replace("/watch/", "/title/", ignoreCase = true)
    }

    // 3. Paramount+: strip /video/<episodeId>/<slug> so it opens /shows/<showSlug> (movies already use /movies/<slug>/<id>)
    if (deep != null && deep.contains("paramountplus.com/shows/", ignoreCase = true)) {
      deep = deep
        .replace(Regex("""(/shows/[^/#?]+)/video/[^#?]+"""), "$1")
        .replace(Regex("""(%2Fshows%2F[^;%]+)/video[^;]*""", RegexOption.IGNORE_CASE), "$1")
        .replace(Regex("""(%2Fshows%2F[^;%]+)%2Fvideo[^;]*""", RegexOption.IGNORE_CASE), "$1")
    }
    if (web != null && web.contains("paramountplus.com/shows/", ignoreCase = true)) {
      web = web.replace(Regex("""(/shows/[^/#?]+)/video/[^#?]+"""), "$1")
    }

    // 4. Disney+: use /browse/entity-<id> from standardWebURL instead of /play/<id>
    if (deep != null && deep.contains("disneyplus.com/play/", ignoreCase = true) &&
      web != null && web.contains("disneyplus.com/browse/", ignoreCase = true)
    ) {
      val browsePath = web.substringAfter("://", "").ifEmpty { null }
      if (browsePath != null) {
        deep = deep.replace(Regex("""www\.disneyplus\.com/play/[^#?]+"""), browsePath)
      }
    }

    // 5. Apple TV: convert /episode/<slug>/<epId>?showId=<showId> to /show/<showId> and strip playableId on both movies & shows
    val appleShowId = Regex("""[?&]showId=([a-zA-Z0-9._-]+)""")
      .find(deep.orEmpty())?.groupValues?.getOrNull(1)
      ?: Regex("""[?&]showId=([a-zA-Z0-9._-]+)""").find(web.orEmpty())?.groupValues?.getOrNull(1)
    if (!appleShowId.isNullOrBlank()) {
      if (deep != null && deep.contains("tv.apple.com", ignoreCase = true)) {
        deep = deep.replace(
          Regex("""tv\.apple\.com/([a-z]{2})/episode/[^#]+"""),
          "tv.apple.com/$1/show/$appleShowId",
        )
      }
      if (web != null && web.contains("tv.apple.com", ignoreCase = true)) {
        web = web.replace(
          Regex("""tv\.apple\.com/([a-z]{2})/episode/[^?#]+"""),
          "tv.apple.com/$1/show/$appleShowId",
        )
      }
    }
    if (deep != null && deep.contains("tv.apple.com", ignoreCase = true)) {
      deep = deep.replace(Regex("""([?&])playableId=[^&#]+&?"""), "$1").replace("?#", "#").replace("&#", "#")
    }
    if (web != null && web.contains("tv.apple.com", ignoreCase = true)) {
      web = web.replace(Regex("""([?&])playableId=[^&#]+&?"""), "$1").trimEnd('?', '&')
    }

    // 6. ITVX: open the movie/show hub /watch/<slug>/<id> rather than /playvideo?productionId=<id>
    if (web != null && web.contains("itv.com/watch/", ignoreCase = true)) {
      val overviewHub = Regex("""https?://www\.itv\.com/watch/[^/?#]+/[^/?#]+""")
        .find(web)?.value
      if (!overviewHub.isNullOrBlank()) {
        web = overviewHub
        val hostAndPath = overviewHub.substringAfter("://")
        deep = "intent://$hostAndPath#Intent;launchFlags=0x00800000;scheme=https;package=air.ITVMobilePlayer;end"
      }
    }

    // 7. Crunchyroll: JustWatch specifies outdated component com.crunchyroll.crunchyroid/.MainActivity
    //    (actual activity is .main.ui.MainActivity). Strip the component attribute so Android resolves the package's scheme handler.
    if (deep != null && deep.contains("com.crunchyroll.crunchyroid", ignoreCase = true)) {
      deep = deep.replace(Regex("""component=com\.crunchyroll\.crunchyroid/[^;]+;"""), "")
    }

    return deep to web
  }

  private fun offerPriority(offer: StreamingOffer): Int = when (offer.monetizationType) {
    "FLATRATE" -> 0
    "FREE" -> 1
    "ADS" -> 2
    "RENT" -> 3
    "BUY" -> 4
    else -> 5
  }

  companion object {
    private const val ENDPOINT = "https://apis.justwatch.com/graphql"

    private const val SEARCH_GRAPHQL_QUERY = """
      query GetSuggestedTitles(${'$'}country: Country!, ${'$'}language: Language!, ${'$'}first: Int!, ${'$'}after: String, ${'$'}filter: TitleFilter) {
        popularTitles(country: ${'$'}country, first: ${'$'}first, after: ${'$'}after, filter: ${'$'}filter) {
          pageInfo {
            hasNextPage
            endCursor
          }
          edges {
            node {
              id
              objectType
              content(country: ${'$'}country, language: ${'$'}language) {
                title
                originalReleaseYear
                shortDescription
                ageCertification
                posterUrl
                scoring {
                  imdbScore
                  tomatoMeter
                  certifiedFresh
                }
              }
              offers(country: ${'$'}country, platform: ANDROID_TV) {
                monetizationType
                presentationType
                standardWebURL
                deeplinkURL(platform: ANDROID_TV)
                package { clearName shortName packageId }
              }
              offersWeb: offers(country: ${'$'}country, platform: WEB) {
                monetizationType
                presentationType
                standardWebURL
                deeplinkURL(platform: ANDROID_TV)
                package { clearName shortName packageId }
              }
            }
          }
        }
      }
    """
  }
}
