package com.horizontv.launcher.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
  val packageName: String,
  val activityName: String,
  val label: String,
  val originalLabel: String = label,
  val isTvApp: Boolean,
  val isSystemApp: Boolean,
  val icon: ImageBitmap?,
  val banner: ImageBitmap?,
  val tintColor: Color,
  val hasCustomIcon: Boolean = false,
)

data class AppRow(
  val id: String,
  val title: String,
  val packageNames: List<String> = emptyList(),
)

data class ResolvedAppRow(
  val id: String,
  val title: String,
  val apps: List<AppInfo>,
)

data class ReorderTarget(
  val rowId: String,
  val packageName: String,
)

data class AppMenuTarget(
  val app: AppInfo,
  val rowId: String?,
)

enum class AppFilter(val label: String) {
  ALL("All Apps"),
  TV_APPS("TV Apps"),
  OTHER_APPS("Other Apps"),
}

enum class HomeTileSpacing(val id: String, val label: String, val spacingDp: Int) {
  COMPACT("compact", "Compact", 12),
  NORMAL("normal", "Normal", 20),
  WIDE("wide", "Wide", 30);

  companion object {
    fun fromId(id: String?): HomeTileSpacing =
      entries.firstOrNull { it.id == id } ?: NORMAL
  }
}

enum class SearchProviderFilter(val label: String) {
  ALL("All"),
  APPS("Apps"),
  JELLYFIN("Jellyfin"),
  STREAMING("Where to Watch"),
}

data class StreamingOffer(
  val providerName: String,
  val shortName: String,
  val monetizationType: String,
  val deeplinkUrl: String?,
  val webUrl: String?,
) {
  val isIncluded: Boolean
    get() = monetizationType == "FLATRATE" || monetizationType == "FREE" || monetizationType == "ADS"

  val badgeLabel: String
    get() = when (monetizationType) {
      "FREE", "ADS" -> "$providerName (Free)"
      "RENT" -> "Rent: $providerName"
      "BUY" -> "Buy: $providerName"
      else -> providerName
    }
}

data class StreamingSearchItem(
  val id: String,
  val title: String,
  val objectType: String,
  val year: Int?,
  val runtimeMinutes: Int? = null,
  val shortDescription: String? = null,
  val genres: List<String> = emptyList(),
  val cast: List<String> = emptyList(),
  val directors: List<String> = emptyList(),
  val ageCertification: String? = null,
  val imdbScore: Float? = null,
  val tomatoMeter: Int? = null,
  val certifiedFresh: Boolean = false,
  val posterUrl: String? = null,
  val poster: ImageBitmap? = null,
  val offers: List<StreamingOffer> = emptyList(),
) {
  val typeLabel: String
    get() = if (objectType.equals("SHOW", ignoreCase = true)) "TV Show" else "Movie"

  val formattedRuntime: String?
    get() {
      val mins = runtimeMinutes ?: return null
      if (mins <= 0) return null
      val h = mins / 60
      val m = mins % 60
      return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
      }
    }
}

enum class JustWatchRegion(val code: String, val label: String, val ratingExample: String) {
  GB("GB", "United Kingdom", "U, PG, 12, 15, 18"),
  US("US", "United States", "G, PG, PG-13, R, TV-MA"),
  CA("CA", "Canada", "G, PG, 14A, 18A"),
  AU("AU", "Australia", "G, PG, M, MA15+"),
  IE("IE", "Ireland", "G, PG, 12A, 15A, 18"),
  DE("DE", "Germany", "FSK 0, 6, 12, 16, 18"),
  FR("FR", "France", "U, 10, 12, 16, 18"),
  NL("NL", "Netherlands", "AL, 6, 9, 12, 16");

  companion object {
    fun fromCode(code: String?): JustWatchRegion =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: GB
  }
}

enum class JustWatchServiceCategory(val title: String) {
  MAIN_SERVICES("Main Streaming & UK Catch-Up"),
  PRIME_CHANNELS("Amazon Prime Video Channels (via Prime App)"),
  RENT_BUY_STORES("Digital Rent & Buy Stores"),
}

enum class JustWatchServicePreset(
  val id: String,
  val label: String,
  val category: JustWatchServiceCategory,
  val shortNames: Set<String>,
  val nameKeywords: List<String>,
  val isPrimeChannel: Boolean = false,
) {
  // 1. Main Streaming & UK Catch-Up
  NETFLIX("netflix", "Netflix", JustWatchServiceCategory.MAIN_SERVICES, setOf("nfx", "nfa", "nfk"), listOf("netflix")),
  PRIME_VIDEO("prime_video", "Amazon Prime Video", JustWatchServiceCategory.MAIN_SERVICES, setOf("prv", "amp", "pva"), listOf("amazon prime video", "prime video")),
  DISNEY_PLUS("disney_plus", "Disney+", JustWatchServiceCategory.MAIN_SERVICES, setOf("dnp"), listOf("disney plus", "disney+")),
  APPLE_TV("apple_tv", "Apple TV+", JustWatchServiceCategory.MAIN_SERVICES, setOf("atp"), listOf("apple tv+", "apple tv")),
  BBC_IPLAYER("bbc_iplayer", "BBC iPlayer", JustWatchServiceCategory.MAIN_SERVICES, setOf("bbc"), listOf("bbc iplayer")),
  ITVX("itvx", "ITVX", JustWatchServiceCategory.MAIN_SERVICES, setOf("itv", "itp"), listOf("itvx", "itv")),
  CHANNEL_4("channel_4", "Channel 4", JustWatchServiceCategory.MAIN_SERVICES, setOf("al4", "a4p", "ch4"), listOf("channel 4", "all 4")),
  MY5("my5", "My5 (Channel 5)", JustWatchServiceCategory.MAIN_SERVICES, setOf("myf", "my5"), listOf("my5", "channel 5")),
  NOW_SKY("now_sky", "NOW / Sky Go", JustWatchServiceCategory.MAIN_SERVICES, setOf("ntv", "now", "skg", "sky", "sgb"), listOf("now tv", "sky go", "sky sports")),
  UKTV_PLAY("uktv_play", "U (UKTV Play)", JustWatchServiceCategory.MAIN_SERVICES, setOf("ukt"), listOf("uktv")),
  PARAMOUNT_PLUS("paramount_plus", "Paramount+ (Direct App)", JustWatchServiceCategory.MAIN_SERVICES, setOf("pmp", "ppp", "pba"), listOf("paramount+", "paramount plus")),
  CRUNCHYROLL("crunchyroll", "Crunchyroll (Direct App)", JustWatchServiceCategory.MAIN_SERVICES, setOf("cru"), listOf("crunchyroll")),
  DISCOVERY_PLUS("discovery_plus", "Discovery+ (Direct App)", JustWatchServiceCategory.MAIN_SERVICES, setOf("dpe", "dcp", "dis"), listOf("discovery+", "discovery plus")),
  MUBI("mubi", "MUBI (Direct App)", JustWatchServiceCategory.MAIN_SERVICES, setOf("mbi"), listOf("mubi")),
  SHUDDER("shudder", "Shudder (Direct App)", JustWatchServiceCategory.MAIN_SERVICES, setOf("shd"), listOf("shudder")),
  ACORN_BFI_HAYU("acorn_bfi_hayu", "Acorn / BFI / Hayu / BritBox (Apps)", JustWatchServiceCategory.MAIN_SERVICES, setOf("act", "bfi", "hay", "bbx"), listOf("acorn tv", "bfi player", "hayu", "britbox")),
  PLEX("plex", "Plex / Pluto / Tubi (Free)", JustWatchServiceCategory.MAIN_SERVICES, setOf("plx", "ptv", "tbv"), listOf("plex", "pluto tv", "tubi")),
  YOUTUBE("youtube", "YouTube", JustWatchServiceCategory.MAIN_SERVICES, setOf("ytr", "yfr", "ytb"), listOf("youtube")),

  // 2. Amazon Prime Video Channels (opened directly via Prime Video)
  PARAMOUNT_PRIME("paramount_prime", "Paramount+ (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("app", "pma"), listOf("paramount+ (prime)", "paramount+ amazon", "paramount plus amazon"), isPrimeChannel = true),
  ITVX_PRIME("itvx_prime", "ITVX (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("itva", "itpa"), listOf("itvx (prime)", "itvx amazon", "itv amazon"), isPrimeChannel = true),
  MGM_PRIME("mgm_prime", "MGM+ (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("mgp", "mga", "mgm"), listOf("mgm+ (prime)", "mgm plus amazon", "mgm+ amazon", "mgm amazon"), isPrimeChannel = true),
  CRUNCHYROLL_PRIME("crunchyroll_prime", "Crunchyroll (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("cra"), listOf("crunchyroll (prime)", "crunchyroll amazon"), isPrimeChannel = true),
  DISCOVERY_PRIME("discovery_prime", "Discovery+ (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("adp", "dca"), listOf("discovery+ (prime)", "discovery+ amazon"), isPrimeChannel = true),
  SHUDDER_PRIME("shudder_prime", "Shudder (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("asd", "sha"), listOf("shudder (prime)", "shudder amazon"), isPrimeChannel = true),
  MUBI_PRIME("mubi_prime", "MUBI (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("amb", "mba"), listOf("mubi (prime)", "mubi amazon"), isPrimeChannel = true),
  BFI_PRIME("bfi_prime", "BFI Player (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("abf"), listOf("bfi player (prime)", "bfi player amazon"), isPrimeChannel = true),
  ACORN_PRIME("acorn_prime", "Acorn TV / BritBox (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("aac", "bba"), listOf("acorn tv (prime)", "acorntv amazon", "acorn tv amazon", "britbox amazon"), isPrimeChannel = true),
  HAYU_PRIME("hayu_prime", "Hayu (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("ahy", "hya"), listOf("hayu (prime)", "hayu amazon"), isPrimeChannel = true),
  INDIE_PRIME("indie_prime", "Curzon / Studiocanal / Arrow (Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("cac", "soa", "arr"), listOf("curzon (prime)", "studiocanal (prime)", "arrow tv (prime)", "curzon amazon", "studiocanal presents amazon", "arrow tv amazon"), isPrimeChannel = true),
  MAX_SONY_PRIME("max_sony_prime", "HBO Max / Lionsgate+ / Sony (via Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("aho", "sco", "lga"), listOf("hbo max (prime)", "max (prime)", "hbo max amazon", "max amazon", "sony pictures core", "lionsgate+ amazon"), isPrimeChannel = true),
  SPECIALTY_PRIME("specialty_prime", "Hidive / History Hit / Docs (Prime)", JustWatchServiceCategory.PRIME_CHANNELS, setOf("hda", "hil", "dov", "pbm", "mqa", "mha"), listOf("hidive", "history hit", "docsville", "pbs america", "marquee tv", "mhz choice"), isPrimeChannel = true),
  ALL_PRIME_CHANNELS("all_prime_channels", "All Other Prime Video Channels", JustWatchServiceCategory.PRIME_CHANNELS, emptySet(), listOf("(prime)", "amazon channel"), isPrimeChannel = true),

  // 3. Digital Rent & Buy Stores
  AMAZON_STORE("amazon_store", "Amazon Video Store (Rent & Buy)", JustWatchServiceCategory.RENT_BUY_STORES, setOf("amz"), listOf("amazon video")),
  DIGITAL_STORES("digital_stores", "Apple / Sky / Rakuten / Google Store", JustWatchServiceCategory.RENT_BUY_STORES, setOf("itu", "sks", "wki", "ply", "chc"), listOf("apple tv store", "sky store", "rakuten", "google play", "curzon home cinema"));

  fun matches(offer: StreamingOffer): Boolean {
    val short = offer.shortName.trim().lowercase()
    val name = offer.providerName.trim().lowercase()
    val offerIsPrimeChannel = name.endsWith("(prime)") || name.contains("amazon channel") ||
      short in setOf("app", "pma", "itva", "itpa", "mgp", "mga", "cra", "adp", "dca", "asd", "amb", "abf", "aac", "ahy", "cac", "soa", "arr", "aho", "sco", "hda", "hil", "dov", "pbm", "mqa", "mha")

    if (isPrimeChannel != offerIsPrimeChannel) return false
    if (short.isNotEmpty() && short in shortNames) return true
    if (id == "now_sky" && (name == "now" || name.startsWith("now ") || name.contains("sky go"))) return true
    if (id == "apple_tv" && name.contains("apple tv store")) return false
    if (id == "prime_video" && name == "amazon video") return false
    return nameKeywords.any { kw -> name.contains(kw) }
  }
}

data class ExternalSearchProvider(
  val id: String,
  val label: String,
  val packageName: String,
)

data class JellyfinSearchItem(
  val itemId: String,
  val title: String,
  val itemType: String,
  val year: Int?,
  val communityRating: Float? = null,
  val imageUrl: String? = null,
  val thumbnail: ImageBitmap? = null,
) {
  val typeLabel: String
    get() = if (itemType.equals("Series", ignoreCase = true)) "TV Show" else "Movie"
}

enum class NetworkType {
  WIFI,
  ETHERNET,
  CELLULAR,
  NONE,
}

data class WallpaperTheme(
  val id: String,
  val name: String,
  val baseColor: Color,
  val midColor: Color,
  val topGlow: Color,
  val bottomGlow: Color,
  val cardSurface: Color,
)

data class AccentPreset(
  val id: String,
  val name: String,
  val primary: Color,
  val onPrimary: Color,
  val glow: Color,
)

data class JellyfinBackdrop(
  val itemId: String,
  val title: String,
  val year: Int?,
  val imageUrl: String,
  val logoUrl: String? = null,
)

enum class MediaWallpaperProvider(
  val id: String,
  val label: String,
  val defaultPortHint: String,
  val tokenLabel: String,
  val usernameLabel: String,
) {
  JELLYFIN(
    id = "jellyfin",
    label = "Jellyfin",
    defaultPortHint = "http://192.168.1.50:8096",
    tokenLabel = "API Key (Jellyfin Dashboard → API Keys)",
    usernameLabel = "Jellyfin Username",
  ),
  PLEX(
    id = "plex",
    label = "Plex",
    defaultPortHint = "http://192.168.1.50:32400",
    tokenLabel = "Plex Token (X-Plex-Token)",
    usernameLabel = "Plex.tv Email or Username",
  ),
  EMBY(
    id = "emby",
    label = "Emby",
    defaultPortHint = "http://192.168.1.50:8096",
    tokenLabel = "API Key (Emby Dashboard → API Keys)",
    usernameLabel = "Emby Username",
  ),
  TMDB(
    id = "tmdb",
    label = "TMDB",
    defaultPortHint = "https://api.themoviedb.org/3",
    tokenLabel = "TMDB API Key (v3) or Read Access Token (v4)",
    usernameLabel = "",
  ),
  REDDIT(
    id = "reddit",
    label = "Reddit",
    defaultPortHint = "",
    tokenLabel = "",
    usernameLabel = "",
  );

  val requiresServerConfig: Boolean
    get() = this != REDDIT

  val requiresServerUrl: Boolean
    get() = this != REDDIT && this != TMDB

  companion object {
    fun fromId(id: String?): MediaWallpaperProvider =
      entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: JELLYFIN
  }
}

enum class RedditSort(val id: String, val label: String, val pathSuffix: String) {
  HOT("hot", "Hot", "hot.json?limit=50&raw_json=1"),
  TOP_WEEK("top_week", "Top Week", "top.json?t=week&limit=50&raw_json=1"),
  TOP_MONTH("top_month", "Top Month", "top.json?t=month&limit=50&raw_json=1"),
  TOP_ALL("top_all", "Top All Time", "top.json?t=all&limit=50&raw_json=1");

  companion object {
    fun fromId(id: String?): RedditSort =
      entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TOP_MONTH
  }
}

enum class JellyfinMediaType(
  val id: String,
  val label: String,
  val apiTypes: String,
) {
  BOTH("both", "Both", "Movie,Series"),
  MOVIES("movies", "Movies", "Movie"),
  TV_SHOWS("tv", "TV Shows", "Series"),
  ;

  companion object {
    fun fromId(id: String): JellyfinMediaType =
      entries.firstOrNull { it.id == id } ?: BOTH
  }
}

data class JellyfinLibrary(
  val id: String,
  val name: String,
  val collectionType: String = "",
)

data class JellyfinSlide(
  val backdrop: JellyfinBackdrop,
  val image: ImageBitmap,
  val logoImage: ImageBitmap? = null,
)

enum class ScreensaverMode(val id: String, val label: String) {
  WALLPAPER_ARTWORK("artwork", "Wallpaper Artwork"),
  PHOTOS("photos", "My Photos"),
  AMBIENT("ambient", "Ambient Mode"),
  ;

  companion object {
    fun fromId(id: String?): ScreensaverMode =
      entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: WALLPAPER_ARTWORK
  }
}

data class ScreensaverPhotoSlide(
  val uri: String,
  val title: String,
  val image: ImageBitmap,
)

enum class WeatherIconStyle(val id: String, val label: String) {
  COLOR_VIVID("color_vivid", "Vivid Color"),
  MINIMAL_LINE("minimal_line", "Minimal"),
  EMOJI("emoji", "Emoji"),
  ACCENT_TINT("accent_tint", "Accent"),
  ;

  companion object {
    fun fromId(id: String): WeatherIconStyle =
      entries.firstOrNull { it.id == id } ?: COLOR_VIVID
  }
}

enum class WeatherForecastSize(val id: String, val label: String, val scale: Float) {
  SMALL("small", "Small", 0.85f),
  MEDIUM("medium", "Medium", 1.0f),
  LARGE("large", "Large", 1.25f);

  companion object {
    fun fromId(id: String?): WeatherForecastSize =
      entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MEDIUM
  }
}

enum class HomeTileSize(
  val id: String,
  val label: String,
  val widthDp: Int,
  val heightDp: Int,
  val addTileWidthDp: Int,
  val iconSizeDp: Int,
) {
  SMALL(
    id = "small",
    label = "Small",
    widthDp = 172,
    heightDp = 98,
    addTileWidthDp = 136,
    iconSizeDp = 50,
  ),
  MEDIUM(
    id = "medium",
    label = "Medium",
    widthDp = 216,
    heightDp = 122,
    addTileWidthDp = 164,
    iconSizeDp = 64,
  ),
  LARGE(
    id = "large",
    label = "Large",
    widthDp = 264,
    heightDp = 148,
    addTileWidthDp = 196,
    iconSizeDp = 76,
  );

  companion object {
    fun fromId(id: String?): HomeTileSize =
      entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MEDIUM
  }
}

enum class WeatherCondition(val label: String) {
  CLEAR("Clear"),
  PARTLY_CLOUDY("Partly Cloudy"),
  CLOUDY("Cloudy"),
  FOG("Fog"),
  DRIZZLE("Drizzle"),
  RAIN("Rain"),
  SNOW("Snow"),
  STORM("Storm"),
}

data class WeatherLocation(
  val name: String,
  val region: String,
  val country: String,
  val latitude: Double,
  val longitude: Double,
) {
  val displayName: String
    get() = listOf(name, region, country)
      .filter { it.isNotBlank() }
      .distinct()
      .joinToString(", ")

  val shortName: String
    get() = if (country.isNotBlank()) "$name, $country" else name
}

data class DailyForecast(
  val dayLabel: String,
  val condition: WeatherCondition,
  val tempMax: Int,
  val tempMin: Int,
)

data class WeatherData(
  val locationName: String,
  val currentTemp: Int,
  val condition: WeatherCondition,
  val unitSymbol: String,
  val daily: List<DailyForecast>,
)

enum class UpdateCheckSchedule(val id: String, val label: String, val description: String) {
  MANUAL("manual", "Manual Only", "Off — only check when you press Check for Updates"),
  WEEKLY("weekly", "Every 7 Days", "Check automatically once a week"),
  AUTOMATIC("automatic", "Automatic", "Check automatically whenever Horizon starts");

  companion object {
    fun fromId(id: String?): UpdateCheckSchedule =
      entries.firstOrNull { it.id == id } ?: WEEKLY
  }
}

data class WallpaperProviderConfig(
  val intervalSeconds: Int = 30,
  val showTitle: Boolean = true,
  val mediaType: String = JellyfinMediaType.BOTH.id,
  val backdropLimit: Int = 40,
)

data class LauncherSettings(
  val rows: List<AppRow> = listOf(AppRow(id = DEFAULT_FAVORITES_ROW_ID, title = "Favorites")),
  val hiddenPackages: Set<String> = emptySet(),
  val customLabels: Map<String, String> = emptyMap(),
  val customIconPaths: Map<String, String> = emptyMap(),
  val themeId: String = "midnight_horizon",
  val accentId: String = "sky_cyan",
  val customWallpaperUri: String? = null,
  val wallpaperDimAlpha: Float = 0.45f,
  val wallpaperProvider: String = MediaWallpaperProvider.JELLYFIN.id,
  val jellyfinEnabled: Boolean = false,
  val jellyfinServerUrl: String = "",
  val jellyfinApiKey: String = "",
  val jellyfinUserId: String = "",
  val jellyfinIntervalSeconds: Int = 30,
  val jellyfinShowTitle: Boolean = true,
  val jellyfinMediaType: String = JellyfinMediaType.BOTH.id,
  val jellyfinLibraryIds: Set<String> = emptySet(),
  val jellyfinBackdropLimit: Int = 40,
  val plexServerUrl: String = "",
  val plexToken: String = "",
  val plexLibraryIds: Set<String> = emptySet(),
  val embyServerUrl: String = "",
  val embyApiKey: String = "",
  val embyUserId: String = "",
  val embyLibraryIds: Set<String> = emptySet(),
  val tmdbApiKey: String = "",
  val tmdbCategories: Set<String> = DEFAULT_TMDB_CATEGORIES,
  val redditSubreddits: Set<String> = DEFAULT_REDDIT_SUBREDDITS,
  val redditCustomSubreddits: Set<String> = emptySet(),
  val redditSort: String = RedditSort.TOP_MONTH.id,
  val wallpaperProviderConfigs: Map<String, WallpaperProviderConfig> = emptyMap(),
  val weatherEnabled: Boolean = false,
  val weatherLocationName: String = "",
  val weatherLatitude: Double? = null,
  val weatherLongitude: Double? = null,
  val weatherDays: Int = 5,
  val weatherShowForecast: Boolean = true,
  val weatherForecastSize: String = WeatherForecastSize.MEDIUM.id,
  val weatherUseFahrenheit: Boolean = false,
  val weatherIconStyle: String = WeatherIconStyle.COLOR_VIVID.id,
  val weatherShowBackdrop: Boolean = true,
  val showClock: Boolean = true,
  val showDate: Boolean = true,
  val use24HourClock: Boolean = false,
  val showNonLeanbackApps: Boolean = true,
  val showSearchBar: Boolean = true,
  val showAllAppsSection: Boolean = true,
  val showAddAppTile: Boolean = true,
  val showTileTitles: Boolean = true,
  val showTvInputsButton: Boolean = true,
  val lockHomeLayout: Boolean = false,
  val minimalistDockMode: Boolean = false,
  val tileOpacity: Float = 1.0f,
  val tileSpacing: String = HomeTileSpacing.NORMAL.id,
  val homeTileSize: String = HomeTileSize.MEDIUM.id,
  val disabledSearchProviders: Set<String> = emptySet(),
  val justWatchFilterServices: Boolean = false,
  val justWatchServices: Set<String> = DEFAULT_JUSTWATCH_SERVICES,
  val justWatchIncludeRentBuy: Boolean = true,
  val justWatchHideUnmatchedTitles: Boolean = false,
  val justWatchRegion: String = JustWatchRegion.GB.code,
  val justWatchShowRottenTomatoes: Boolean = true,
  val justWatchShowAgeRating: Boolean = true,
  val searchOpenDetailsPopup: Boolean = true,
  val gridColumns: Int = 5,
  val hasInitializedFavorites: Boolean = false,
  val screensaverEnabled: Boolean = false,
  val screensaverMode: String = ScreensaverMode.WALLPAPER_ARTWORK.id,
  val screensaverTimeoutMinutes: Int = 5,
  val screensaverIntervalSeconds: Int = 20,
  val screensaverKenBurns: Boolean = true,
  val screensaverShowClock: Boolean = true,
  val screensaverShowWeather: Boolean = true,
  val screensaverShowTitle: Boolean = true,
  val screensaverBurnInShift: Boolean = true,
  val screensaverDimAlpha: Float = 0.25f,
  val screensaverAmbientCycle: Boolean = true,
  val screensaverPhotoFolderUri: String? = null,
  val screensaverPhotoUris: List<String> = emptyList(),
  val updateCheckSchedule: String = UpdateCheckSchedule.WEEKLY.id,
  val lastUpdateCheckMillis: Long = 0L,
  val parentalPin: String = "",
  val pinProtectSettings: Boolean = true,
  val pinLockedPackages: Set<String> = emptySet(),
) {
  val isPinEnabled: Boolean
    get() = parentalPin.length == 4 && parentalPin.all { it.isDigit() }

  fun isAppPinLocked(packageName: String): Boolean =
    isPinEnabled && packageName in pinLockedPackages

  val activeUpdateCheckSchedule: UpdateCheckSchedule
    get() = UpdateCheckSchedule.fromId(updateCheckSchedule)

  val activeJustWatchRegion: JustWatchRegion
    get() = JustWatchRegion.fromCode(justWatchRegion)

  val activeScreensaverMode: ScreensaverMode
    get() = ScreensaverMode.fromId(screensaverMode)

  val primaryRow: AppRow
    get() = rows.firstOrNull() ?: AppRow(id = DEFAULT_FAVORITES_ROW_ID, title = "Favorites")

  val allPinnedPackages: Set<String>
    get() = rows.flatMap { it.packageNames }.toSet()

  val activeWallpaperProvider: MediaWallpaperProvider
    get() = MediaWallpaperProvider.fromId(wallpaperProvider)

  fun configFor(provider: MediaWallpaperProvider = activeWallpaperProvider): WallpaperProviderConfig {
    return wallpaperProviderConfigs[provider.id] ?: WallpaperProviderConfig(
      intervalSeconds = jellyfinIntervalSeconds,
      showTitle = jellyfinShowTitle,
      mediaType = jellyfinMediaType,
      backdropLimit = jellyfinBackdropLimit,
    )
  }

  fun withProviderConfig(
    provider: MediaWallpaperProvider = activeWallpaperProvider,
    transform: (WallpaperProviderConfig) -> WallpaperProviderConfig,
  ): LauncherSettings {
    val updated = transform(configFor(provider))
    val nextMap = wallpaperProviderConfigs + (provider.id to updated)
    return if (provider == MediaWallpaperProvider.JELLYFIN) {
      copy(
        wallpaperProviderConfigs = nextMap,
        jellyfinIntervalSeconds = updated.intervalSeconds,
        jellyfinShowTitle = updated.showTitle,
        jellyfinMediaType = updated.mediaType,
        jellyfinBackdropLimit = updated.backdropLimit,
      )
    } else {
      copy(wallpaperProviderConfigs = nextMap)
    }
  }

  val activeWallpaperShowTitle: Boolean
    get() = configFor().showTitle

  val activeWallpaperIntervalSeconds: Int
    get() = configFor().intervalSeconds

  val activeWallpaperMediaType: String
    get() = configFor().mediaType

  val activeWallpaperBackdropLimit: Int
    get() = configFor().backdropLimit

  val isJellyfinConfigured: Boolean
    get() = jellyfinServerUrl.isNotBlank() && jellyfinApiKey.isNotBlank()

  val isPlexConfigured: Boolean
    get() = plexServerUrl.isNotBlank() && plexToken.isNotBlank()

  val isEmbyConfigured: Boolean
    get() = embyServerUrl.isNotBlank() && embyApiKey.isNotBlank()

  val isTmdbConfigured: Boolean
    get() = tmdbApiKey.isNotBlank()

  fun isProviderConfigured(provider: MediaWallpaperProvider): Boolean = when (provider) {
    MediaWallpaperProvider.JELLYFIN -> isJellyfinConfigured
    MediaWallpaperProvider.PLEX -> isPlexConfigured
    MediaWallpaperProvider.EMBY -> isEmbyConfigured
    MediaWallpaperProvider.TMDB -> isTmdbConfigured
    MediaWallpaperProvider.REDDIT -> true
  }

  val isActiveWallpaperProviderConfigured: Boolean
    get() = isProviderConfigured(activeWallpaperProvider)

  fun serverUrlFor(provider: MediaWallpaperProvider): String = when (provider) {
    MediaWallpaperProvider.JELLYFIN -> jellyfinServerUrl
    MediaWallpaperProvider.PLEX -> plexServerUrl
    MediaWallpaperProvider.EMBY -> embyServerUrl
    MediaWallpaperProvider.TMDB -> "https://api.themoviedb.org/3"
    MediaWallpaperProvider.REDDIT -> redditSort
  }

  fun tokenFor(provider: MediaWallpaperProvider): String = when (provider) {
    MediaWallpaperProvider.JELLYFIN -> jellyfinApiKey
    MediaWallpaperProvider.PLEX -> plexToken
    MediaWallpaperProvider.EMBY -> embyApiKey
    MediaWallpaperProvider.TMDB -> tmdbApiKey
    MediaWallpaperProvider.REDDIT -> ""
  }

  fun userIdFor(provider: MediaWallpaperProvider): String = when (provider) {
    MediaWallpaperProvider.JELLYFIN -> jellyfinUserId
    MediaWallpaperProvider.PLEX -> ""
    MediaWallpaperProvider.EMBY -> embyUserId
    MediaWallpaperProvider.TMDB -> ""
    MediaWallpaperProvider.REDDIT -> ""
  }

  fun libraryIdsFor(provider: MediaWallpaperProvider): Set<String> = when (provider) {
    MediaWallpaperProvider.JELLYFIN -> jellyfinLibraryIds
    MediaWallpaperProvider.PLEX -> plexLibraryIds
    MediaWallpaperProvider.EMBY -> embyLibraryIds
    MediaWallpaperProvider.TMDB -> tmdbCategories
    MediaWallpaperProvider.REDDIT -> redditSubreddits
  }

  val isWeatherConfigured: Boolean
    get() = weatherLocationName.isNotBlank() && weatherLatitude != null && weatherLongitude != null

  fun isSearchProviderEnabled(providerId: String): Boolean = providerId !in disabledSearchProviders

  companion object {
    const val DEFAULT_FAVORITES_ROW_ID = "favorites"
    const val SEARCH_PROVIDER_APPS = "apps"
    const val SEARCH_PROVIDER_JELLYFIN = "jellyfin"
    const val SEARCH_PROVIDER_JUSTWATCH = "justwatch"
    const val SEARCH_PROVIDER_YOUTUBE = "youtube"
    const val SEARCH_PROVIDER_WEB = "web"
    val DEFAULT_JUSTWATCH_SERVICES: Set<String> = setOf(
      JustWatchServicePreset.NETFLIX.id,
      JustWatchServicePreset.PRIME_VIDEO.id,
      JustWatchServicePreset.PARAMOUNT_PRIME.id,
      JustWatchServicePreset.MGM_PRIME.id,
      JustWatchServicePreset.CRUNCHYROLL_PRIME.id,
      JustWatchServicePreset.DISCOVERY_PRIME.id,
      JustWatchServicePreset.DISNEY_PLUS.id,
      JustWatchServicePreset.BBC_IPLAYER.id,
      JustWatchServicePreset.ITVX.id,
      JustWatchServicePreset.CHANNEL_4.id,
      JustWatchServicePreset.MY5.id,
    )
    val DEFAULT_TMDB_CATEGORIES: Set<String> = setOf("trending_all", "popular_movie")
    val DEFAULT_REDDIT_SUBREDDITS: Set<String> = setOf("EarthPorn", "wallpapers", "SpacePorn")
    val PRESET_REDDIT_SUBREDDITS: List<String> = listOf(
      "EarthPorn",
      "wallpapers",
      "wallpaper",
      "SpacePorn",
      "CityPorn",
      "SkyPorn",
      "MinimalWallpaper",
      "ImaginaryLandscapes",
      "WidescreenWallpaper",
    )
  }
}

data class TvInputSource(
  val id: String,
  val label: String,
  val typeLabel: String,
  val isPassthrough: Boolean,
  val isConnected: Boolean,
  val sortOrder: Int = 0,
)

data class AppUpdateInfo(
  val versionName: String,
  val tagName: String,
  val releaseName: String,
  val releaseNotes: String,
  val publishedAt: String = "",
  val apkUrl: String,
  val apkFileName: String,
  val apkSizeBytes: Long,
  val htmlUrl: String,
)

enum class PinPromptMode {
  VERIFY,
  SET_NEW,
  CONFIRM_NEW,
}

sealed class PinPendingAction {
  data object OpenSettings : PinPendingAction()
  data class LaunchApp(val app: AppInfo) : PinPendingAction()
  data class ToggleAppLock(val app: AppInfo) : PinPendingAction()
  data object SetNewPin : PinPendingAction()
  data object ChangePin : PinPendingAction()
  data object RemovePin : PinPendingAction()
}

data class PinPromptState(
  val mode: PinPromptMode,
  val title: String,
  val subtitle: String,
  val pendingAction: PinPendingAction,
)

data class LauncherUiState(
  val isLoading: Boolean = true,
  val isDefaultHome: Boolean = false,
  val allApps: List<AppInfo> = emptyList(),
  val settings: LauncherSettings = LauncherSettings(),
  val selectedFilter: AppFilter = AppFilter.ALL,
  val searchQuery: String = "",
  val isSearchVisible: Boolean = false,
  val searchProvider: SearchProviderFilter = SearchProviderFilter.ALL,
  val isSearchingJellyfin: Boolean = false,
  val jellyfinSearchResults: List<JellyfinSearchItem> = emptyList(),
  val jellyfinSearchError: String? = null,
  val isSearchingStreaming: Boolean = false,
  val isLoadingMoreStreaming: Boolean = false,
  val streamingSearchResults: List<StreamingSearchItem> = emptyList(),
  val streamingHasMore: Boolean = false,
  val streamingNextCursor: String? = null,
  val streamingSearchError: String? = null,
  val externalSearchProviders: List<ExternalSearchProvider> = emptyList(),
  val networkType: NetworkType = NetworkType.WIFI,
  val focusedApp: AppInfo? = null,
  val menuTarget: AppMenuTarget? = null,
  val renamingApp: AppInfo? = null,
  val renamingRowId: String? = null,
  val isCreatingRow: Boolean = false,
  val addingAppsToRowId: String? = null,
  val orderingRowId: String? = null,
  val reorderingTarget: ReorderTarget? = null,
  val isSettingsOpen: Boolean = false,
  val setupProviderDialog: MediaWallpaperProvider? = null,
  val isAddingSubreddit: Boolean = false,
  val isTestingJellyfin: Boolean = false,
  val jellyfinError: String? = null,
  val jellyfinSlide: JellyfinSlide? = null,
  val jellyfinBackdropCount: Int = 0,
  val jellyfinLibraries: List<JellyfinLibrary> = emptyList(),
  val weatherData: WeatherData? = null,
  val weatherFetchError: String? = null,
  val isWeatherDialogOpen: Boolean = false,
  val isSearchingWeather: Boolean = false,
  val weatherSearchResults: List<WeatherLocation> = emptyList(),
  val weatherError: String? = null,
  val isScreensaverActive: Boolean = false,
  val screensaverPhotoSlide: ScreensaverPhotoSlide? = null,
  val screensaverPhotoCount: Int = 0,
  val tvInputs: List<TvInputSource> = emptyList(),
  val isTvInputsDialogOpen: Boolean = false,
  val availableUpdate: AppUpdateInfo? = null,
  val latestRelease: AppUpdateInfo? = null,
  val showUpdateDialog: Boolean = false,
  val isCheckingForUpdate: Boolean = false,
  val isDownloadingUpdate: Boolean = false,
  val updateDownloadProgress: Float = 0f,
  val updateStatusText: String? = null,
  val pinPrompt: PinPromptState? = null,
  val statusMessage: String? = null,
) {
  private val decoratedApps: List<AppInfo>
    get() = allApps.map { app ->
      val customName = settings.customLabels[app.packageName]?.trim()
      if (!customName.isNullOrEmpty() && customName != app.label) {
        app.copy(label = customName)
      } else {
        app
      }
    }

  private val visiblePool: List<AppInfo>
    get() = decoratedApps.filter { app ->
      app.packageName !in settings.hiddenPackages &&
        (settings.showNonLeanbackApps || app.isTvApp)
    }

  val resolvedRows: List<ResolvedAppRow>
    get() {
      val byPackage = visiblePool.associateBy { it.packageName }
      val sourceRows = settings.rows.ifEmpty {
        listOf(AppRow(id = LauncherSettings.DEFAULT_FAVORITES_ROW_ID, title = "Favorites"))
      }
      return sourceRows.map { row ->
        ResolvedAppRow(
          id = row.id,
          title = row.title.ifBlank { "Row" },
          apps = row.packageNames.mapNotNull { byPackage[it] },
        )
      }
    }

  val filteredApps: List<AppInfo>
    get() {
      val query = searchQuery.trim().lowercase()
      return visiblePool.filter { app ->
        val matchesCategory = when (selectedFilter) {
          AppFilter.ALL -> true
          AppFilter.TV_APPS -> app.isTvApp
          AppFilter.OTHER_APPS -> !app.isTvApp
        }
        val matchesSearch = query.isEmpty() ||
          app.label.lowercase().contains(query) ||
          app.originalLabel.lowercase().contains(query) ||
          app.packageName.lowercase().contains(query)
        matchesCategory && matchesSearch
      }.sortedBy { it.label.lowercase() }
    }

  val allVisibleApps: List<AppInfo>
    get() = visiblePool.sortedBy { it.label.lowercase() }

  val searchMatchingApps: List<AppInfo>
    get() {
      val query = searchQuery.trim().lowercase()
      val pool = visiblePool.sortedBy { it.label.lowercase() }
      if (query.isEmpty()) return pool.take(12)
      return pool.filter { app ->
        app.label.lowercase().contains(query) ||
          app.originalLabel.lowercase().contains(query) ||
          app.packageName.lowercase().contains(query)
      }
    }

  val hiddenApps: List<AppInfo>
    get() = decoratedApps.filter { it.packageName in settings.hiddenPackages }

  val lockedApps: List<AppInfo>
    get() = decoratedApps.filter { it.packageName in settings.pinLockedPackages }.sortedBy { it.label.lowercase() }

  val filteredStreamingSearchResults: List<StreamingSearchItem>
    get() {
      val activePresets = if (settings.justWatchFilterServices) {
        JustWatchServicePreset.entries.filter { it.id in settings.justWatchServices }
      } else {
        emptyList()
      }
      val includeItvxPrime = JustWatchServicePreset.ITVX_PRIME.id in settings.justWatchServices
      return streamingSearchResults.mapNotNull { item ->
        val baseOffers = if (includeItvxPrime) {
          val hasItvx = item.offers.any { offer ->
            val short = offer.shortName.trim().lowercase()
            short == "itv" || short == "itp" || offer.providerName.equals("ITVX", ignoreCase = true)
          }
          val hasNativeItvxPrime = item.offers.any { offer ->
            JustWatchServicePreset.ITVX_PRIME.matches(offer)
          }
          if (hasItvx && !hasNativeItvxPrime) {
            val amazonOffer = item.offers.firstOrNull { offer ->
              (offer.deeplinkUrl?.contains("watch.amazon.", ignoreCase = true) == true) ||
                (offer.webUrl?.contains("watch.amazon.", ignoreCase = true) == true)
            }
            val encodedTitle = java.net.URLEncoder.encode(item.title, "UTF-8")
            val primeDeepLink = amazonOffer?.deeplinkUrl
              ?: "intent://watch.amazon.co.uk/search?phrase=$encodedTitle#Intent;package=com.amazon.amazonvideo.livingroom;scheme=https;end"
            val primeWebUrl = amazonOffer?.webUrl
              ?: "https://www.amazon.co.uk/s?k=$encodedTitle&i=instant-video"
            val syntheticItvxPrime = StreamingOffer(
              providerName = "ITVX (Prime)",
              shortName = "itva",
              monetizationType = "FLATRATE",
              deeplinkUrl = primeDeepLink,
              webUrl = primeWebUrl,
            )
            listOf(syntheticItvxPrime) + item.offers
          } else {
            item.offers
          }
        } else {
          item.offers
        }

        val filteredOffers = baseOffers.filter { offer ->
          val matchesMonetization = settings.justWatchIncludeRentBuy || offer.isIncluded
          val matchesService = !settings.justWatchFilterServices ||
            activePresets.any { preset -> preset.matches(offer) }
          matchesMonetization && matchesService
        }
        val isFilteringActive = settings.justWatchFilterServices || !settings.justWatchIncludeRentBuy
        if (settings.justWatchHideUnmatchedTitles && isFilteringActive && filteredOffers.isEmpty()) {
          null
        } else {
          item.copy(offers = filteredOffers)
        }
      }
    }
}
