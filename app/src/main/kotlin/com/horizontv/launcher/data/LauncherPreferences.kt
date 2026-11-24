package com.horizontv.launcher.data

import android.content.Context
import android.content.SharedPreferences
import com.horizontv.launcher.model.AppRow
import com.horizontv.launcher.model.HomeTileSize
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JustWatchRegion
import com.horizontv.launcher.model.LauncherSettings
import com.horizontv.launcher.model.MediaWallpaperProvider
import com.horizontv.launcher.model.RedditSort
import com.horizontv.launcher.model.ScreensaverMode
import com.horizontv.launcher.model.UpdateCheckSchedule
import com.horizontv.launcher.model.WallpaperProviderConfig
import com.horizontv.launcher.model.WeatherForecastSize
import org.json.JSONArray
import org.json.JSONObject

interface SettingsStore {
  fun load(): LauncherSettings
  fun save(settings: LauncherSettings)
}

interface SettingsBackupSerializer {
  fun encodeToJson(settings: LauncherSettings): String
  fun decodeFromJson(
    rawJson: String,
    existing: LauncherSettings = LauncherSettings(),
  ): Result<LauncherSettings>
}

object SettingsBackupCodec : SettingsBackupSerializer {
  private const val FORMAT_VERSION = 1

  override fun encodeToJson(settings: LauncherSettings): String {
    val root = JSONObject()
    root.put("app", "HorizonTVLauncher")
    root.put("backupVersion", FORMAT_VERSION)

    val rowsArr = JSONArray()
    settings.rows.forEach { row ->
      val obj = JSONObject()
      obj.put("id", row.id)
      obj.put("title", row.title)
      val pkgs = JSONArray()
      row.packageNames.forEach { pkgs.put(it) }
      obj.put("packages", pkgs)
      rowsArr.put(obj)
    }
    root.put("rows", rowsArr)

    val hiddenArr = JSONArray()
    settings.hiddenPackages.sorted().forEach { hiddenArr.put(it) }
    root.put("hiddenPackages", hiddenArr)

    val labelsObj = JSONObject()
    settings.customLabels.forEach { (k, v) -> labelsObj.put(k, v) }
    root.put("customLabels", labelsObj)

    root.put("themeId", settings.themeId)
    root.put("accentId", settings.accentId)
    root.put("wallpaperDimAlpha", settings.wallpaperDimAlpha.toDouble())
    root.put("showClock", settings.showClock)
    root.put("showDate", settings.showDate)
    root.put("use24HourClock", settings.use24HourClock)
    root.put("showNonLeanbackApps", settings.showNonLeanbackApps)
    root.put("showSearchBar", settings.showSearchBar)
    root.put("showAllAppsSection", settings.showAllAppsSection)
    root.put("showAddAppTile", settings.showAddAppTile)
    root.put("showTileTitles", settings.showTileTitles)
    root.put("showTvInputsButton", settings.showTvInputsButton)
    root.put("lockHomeLayout", settings.lockHomeLayout)
    root.put("updateCheckSchedule", settings.updateCheckSchedule)
    root.put("parentalPin", settings.parentalPin)
    root.put("pinProtectSettings", settings.pinProtectSettings)
    val pinLockedArr = JSONArray()
    settings.pinLockedPackages.sorted().forEach { pinLockedArr.put(it) }
    root.put("pinLockedPackages", pinLockedArr)
    root.put("homeTileSize", settings.homeTileSize)
    val disabledSearchArr = JSONArray()
    settings.disabledSearchProviders.sorted().forEach { disabledSearchArr.put(it) }
    root.put("disabledSearchProviders", disabledSearchArr)
    root.put("justWatchFilterServices", settings.justWatchFilterServices)
    val jwServicesArr = JSONArray()
    settings.justWatchServices.sorted().forEach { jwServicesArr.put(it) }
    root.put("justWatchServices", jwServicesArr)
    root.put("justWatchIncludeRentBuy", settings.justWatchIncludeRentBuy)
    root.put("justWatchHideUnmatchedTitles", settings.justWatchHideUnmatchedTitles)
    root.put("justWatchRegion", settings.justWatchRegion)
    root.put("justWatchShowRottenTomatoes", settings.justWatchShowRottenTomatoes)
    root.put("justWatchShowAgeRating", settings.justWatchShowAgeRating)
    root.put("gridColumns", settings.gridColumns)

    val jellyfinObj = JSONObject().apply {
      put("provider", settings.wallpaperProvider)
      put("enabled", settings.jellyfinEnabled)
      put("serverUrl", settings.jellyfinServerUrl)
      put("apiKey", settings.jellyfinApiKey)
      put("userId", settings.jellyfinUserId)
      put("intervalSeconds", settings.jellyfinIntervalSeconds)
      put("showTitle", settings.jellyfinShowTitle)
      put("mediaType", settings.jellyfinMediaType)
      val libIdsArr = JSONArray()
      settings.jellyfinLibraryIds.sorted().forEach { libIdsArr.put(it) }
      put("libraryIds", libIdsArr)
      put("backdropLimit", settings.jellyfinBackdropLimit)
      val providerConfigsObj = JSONObject()
      settings.wallpaperProviderConfigs.forEach { (providerId, cfg) ->
        providerConfigsObj.put(
          providerId,
          JSONObject().apply {
            put("intervalSeconds", cfg.intervalSeconds)
            put("showTitle", cfg.showTitle)
            put("mediaType", cfg.mediaType)
            put("backdropLimit", cfg.backdropLimit)
          },
        )
      }
      put("providerConfigs", providerConfigsObj)
    }
    root.put("jellyfin", jellyfinObj)

    val plexObj = JSONObject().apply {
      put("serverUrl", settings.plexServerUrl)
      put("token", settings.plexToken)
      val plexLibs = JSONArray()
      settings.plexLibraryIds.sorted().forEach { plexLibs.put(it) }
      put("libraryIds", plexLibs)
    }
    root.put("plex", plexObj)

    val embyObj = JSONObject().apply {
      put("serverUrl", settings.embyServerUrl)
      put("apiKey", settings.embyApiKey)
      put("userId", settings.embyUserId)
      val embyLibs = JSONArray()
      settings.embyLibraryIds.sorted().forEach { embyLibs.put(it) }
      put("libraryIds", embyLibs)
    }
    root.put("emby", embyObj)

    val tmdbObj = JSONObject().apply {
      put("apiKey", settings.tmdbApiKey)
      val cats = JSONArray()
      settings.tmdbCategories.sorted().forEach { cats.put(it) }
      put("categories", cats)
    }
    root.put("tmdb", tmdbObj)

    val redditObj = JSONObject().apply {
      put("sort", settings.redditSort)
      val subs = JSONArray()
      settings.redditSubreddits.sorted().forEach { subs.put(it) }
      put("subreddits", subs)
      val customSubs = JSONArray()
      settings.redditCustomSubreddits.sorted().forEach { customSubs.put(it) }
      put("customSubreddits", customSubs)
    }
    root.put("reddit", redditObj)

    val weatherObj = JSONObject().apply {
      put("enabled", settings.weatherEnabled)
      put("locationName", settings.weatherLocationName)
      if (settings.weatherLatitude != null) put("latitude", settings.weatherLatitude)
      if (settings.weatherLongitude != null) put("longitude", settings.weatherLongitude)
      put("days", settings.weatherDays)
      put("showForecast", settings.weatherShowForecast)
      put("forecastSize", settings.weatherForecastSize)
      put("useFahrenheit", settings.weatherUseFahrenheit)
      put("iconStyle", settings.weatherIconStyle)
      put("showBackdrop", settings.weatherShowBackdrop)
    }
    root.put("weather", weatherObj)

    val ssObj = JSONObject().apply {
      put("enabled", settings.screensaverEnabled)
      put("mode", settings.screensaverMode)
      put("timeoutMinutes", settings.screensaverTimeoutMinutes)
      put("intervalSeconds", settings.screensaverIntervalSeconds)
      put("kenBurns", settings.screensaverKenBurns)
      put("showClock", settings.screensaverShowClock)
      put("showWeather", settings.screensaverShowWeather)
      put("showTitle", settings.screensaverShowTitle)
      put("burnInShift", settings.screensaverBurnInShift)
      put("dimAlpha", settings.screensaverDimAlpha.toDouble())
      put("ambientCycle", settings.screensaverAmbientCycle)
    }
    root.put("screensaver", ssObj)

    return root.toString(2)
  }

  override fun decodeFromJson(
    rawJson: String,
    existing: LauncherSettings,
  ): Result<LauncherSettings> = runCatching {
    val root = JSONObject(rawJson)
    require(root.has("rows") || root.has("themeId") || root.has("jellyfin") || root.has("weather")) {
      "Selected file is not a valid Horizon TV Launcher backup"
    }

    val rows = if (root.has("rows")) {
      decodeRowsArray(root.optJSONArray("rows")).ifEmpty { existing.rows }
    } else {
      existing.rows
    }

    val hidden = if (root.has("hiddenPackages")) {
      val arr = root.optJSONArray("hiddenPackages")
      buildSet {
        if (arr != null) {
          for (i in 0 until arr.length()) {
            val pkg = arr.optString(i, "").trim()
            if (pkg.isNotEmpty()) add(pkg)
          }
        }
      }
    } else {
      existing.hiddenPackages
    }

    val customLabels = if (root.has("customLabels")) {
      val obj = root.optJSONObject("customLabels")
      buildMap {
        if (obj != null) {
          val keys = obj.keys()
          while (keys.hasNext()) {
            val k = keys.next()
            val v = obj.optString(k, "").trim()
            if (v.isNotEmpty()) put(k, v)
          }
        }
      }
    } else {
      existing.customLabels
    }

    val jf = root.optJSONObject("jellyfin")
    val plex = root.optJSONObject("plex")
    val emby = root.optJSONObject("emby")
    val tmdb = root.optJSONObject("tmdb")
    val reddit = root.optJSONObject("reddit")
    val w = root.optJSONObject("weather")
    val ss = root.optJSONObject("screensaver")

    existing.copy(
      rows = rows,
      hiddenPackages = hidden,
      customLabels = customLabels,
      themeId = root.optString("themeId", existing.themeId).ifBlank { existing.themeId },
      accentId = root.optString("accentId", existing.accentId).ifBlank { existing.accentId },
      wallpaperDimAlpha = root.optDouble("wallpaperDimAlpha", existing.wallpaperDimAlpha.toDouble())
        .toFloat()
        .coerceIn(0.20f, 0.85f),
      showClock = root.optBoolean("showClock", existing.showClock),
      showDate = root.optBoolean("showDate", existing.showDate),
      use24HourClock = root.optBoolean("use24HourClock", existing.use24HourClock),
      showNonLeanbackApps = root.optBoolean("showNonLeanbackApps", existing.showNonLeanbackApps),
      showSearchBar = root.optBoolean("showSearchBar", existing.showSearchBar),
      showAllAppsSection = root.optBoolean("showAllAppsSection", existing.showAllAppsSection),
      showAddAppTile = root.optBoolean("showAddAppTile", existing.showAddAppTile),
      showTileTitles = root.optBoolean("showTileTitles", existing.showTileTitles),
      showTvInputsButton = root.optBoolean("showTvInputsButton", existing.showTvInputsButton),
      lockHomeLayout = root.optBoolean("lockHomeLayout", existing.lockHomeLayout),
      updateCheckSchedule = UpdateCheckSchedule.fromId(
        root.optString("updateCheckSchedule", existing.updateCheckSchedule),
      ).id,
      parentalPin = root.optString("parentalPin", existing.parentalPin)
        .filter { it.isDigit() }
        .take(4)
        .let { if (it.length == 4) it else "" },
      pinProtectSettings = root.optBoolean("pinProtectSettings", existing.pinProtectSettings),
      pinLockedPackages = if (root.has("pinLockedPackages")) {
        val arr = root.optJSONArray("pinLockedPackages")
        buildSet {
          if (arr != null) {
            for (i in 0 until arr.length()) {
              val pkg = arr.optString(i, "").trim()
              if (pkg.isNotEmpty()) add(pkg)
            }
          }
        }
      } else {
        existing.pinLockedPackages
      },
      homeTileSize = HomeTileSize.fromId(
        root.optString("homeTileSize", existing.homeTileSize),
      ).id,
      disabledSearchProviders = if (root.has("disabledSearchProviders")) {
        val arr = root.optJSONArray("disabledSearchProviders")
        buildSet {
          if (arr != null) {
            for (i in 0 until arr.length()) {
              val id = arr.optString(i, "").trim()
              if (id.isNotEmpty()) add(id)
            }
          }
        }
      } else {
        existing.disabledSearchProviders
      },
      justWatchFilterServices = root.optBoolean("justWatchFilterServices", existing.justWatchFilterServices),
      justWatchServices = if (root.has("justWatchServices")) {
        val arr = root.optJSONArray("justWatchServices")
        buildSet {
          if (arr != null) {
            for (i in 0 until arr.length()) {
              val id = arr.optString(i, "").trim()
              if (id.isNotEmpty()) add(id)
            }
          }
        }
      } else {
        existing.justWatchServices
      },
      justWatchIncludeRentBuy = root.optBoolean("justWatchIncludeRentBuy", existing.justWatchIncludeRentBuy),
      justWatchHideUnmatchedTitles = root.optBoolean("justWatchHideUnmatchedTitles", existing.justWatchHideUnmatchedTitles),
      justWatchRegion = JustWatchRegion.fromCode(root.optString("justWatchRegion", existing.justWatchRegion)).code,
      justWatchShowRottenTomatoes = root.optBoolean("justWatchShowRottenTomatoes", existing.justWatchShowRottenTomatoes),
      justWatchShowAgeRating = root.optBoolean("justWatchShowAgeRating", existing.justWatchShowAgeRating),
      gridColumns = root.optInt("gridColumns", existing.gridColumns).coerceIn(4, 6),
      wallpaperProvider = MediaWallpaperProvider.fromId(
        jf?.optString("provider", existing.wallpaperProvider) ?: existing.wallpaperProvider,
      ).id,
      jellyfinEnabled = jf?.optBoolean("enabled", existing.jellyfinEnabled) ?: existing.jellyfinEnabled,
      jellyfinServerUrl = jf?.optString("serverUrl", existing.jellyfinServerUrl) ?: existing.jellyfinServerUrl,
      jellyfinApiKey = jf?.optString("apiKey", existing.jellyfinApiKey) ?: existing.jellyfinApiKey,
      jellyfinUserId = jf?.optString("userId", existing.jellyfinUserId) ?: existing.jellyfinUserId,
      jellyfinIntervalSeconds = (jf?.optInt("intervalSeconds", existing.jellyfinIntervalSeconds)
        ?: existing.jellyfinIntervalSeconds).coerceIn(10, 600),
      jellyfinShowTitle = jf?.optBoolean("showTitle", existing.jellyfinShowTitle) ?: existing.jellyfinShowTitle,
      jellyfinMediaType = jf?.optString("mediaType", existing.jellyfinMediaType)
        ?.ifBlank { JellyfinMediaType.BOTH.id }
        ?: existing.jellyfinMediaType,
      jellyfinLibraryIds = when {
        jf != null && jf.has("libraryIds") -> {
          val arr = jf.optJSONArray("libraryIds")
          buildSet {
            if (arr != null) {
              for (i in 0 until arr.length()) {
                val id = arr.optString(i, "").trim()
                if (id.isNotEmpty()) add(id)
              }
            }
          }
        }
        jf != null && jf.has("libraryId") -> {
          val legacy = jf.optString("libraryId", "").trim()
          if (legacy.isNotEmpty()) setOf(legacy) else emptySet()
        }
        else -> existing.jellyfinLibraryIds
      },
      jellyfinBackdropLimit = (jf?.optInt("backdropLimit", existing.jellyfinBackdropLimit)
        ?: existing.jellyfinBackdropLimit).coerceIn(10, 500),
      wallpaperProviderConfigs = decodeProviderConfigs(jf?.optJSONObject("providerConfigs"))
        ?: existing.wallpaperProviderConfigs,
      plexServerUrl = plex?.optString("serverUrl", existing.plexServerUrl) ?: existing.plexServerUrl,
      plexToken = plex?.optString("token", existing.plexToken) ?: existing.plexToken,
      plexLibraryIds = if (plex != null && plex.has("libraryIds")) {
        decodeStringSetArray(plex.optJSONArray("libraryIds"))
      } else {
        existing.plexLibraryIds
      },
      embyServerUrl = emby?.optString("serverUrl", existing.embyServerUrl) ?: existing.embyServerUrl,
      embyApiKey = emby?.optString("apiKey", existing.embyApiKey) ?: existing.embyApiKey,
      embyUserId = emby?.optString("userId", existing.embyUserId) ?: existing.embyUserId,
      embyLibraryIds = if (emby != null && emby.has("libraryIds")) {
        decodeStringSetArray(emby.optJSONArray("libraryIds"))
      } else {
        existing.embyLibraryIds
      },
      tmdbApiKey = tmdb?.optString("apiKey", existing.tmdbApiKey) ?: existing.tmdbApiKey,
      tmdbCategories = if (tmdb != null && tmdb.has("categories")) {
        decodeStringSetArray(tmdb.optJSONArray("categories")).ifEmpty { LauncherSettings.DEFAULT_TMDB_CATEGORIES }
      } else {
        existing.tmdbCategories
      },
      redditSubreddits = if (reddit != null && reddit.has("subreddits")) {
        decodeStringSetArray(reddit.optJSONArray("subreddits")).ifEmpty { LauncherSettings.DEFAULT_REDDIT_SUBREDDITS }
      } else {
        existing.redditSubreddits
      },
      redditCustomSubreddits = if (reddit != null && reddit.has("customSubreddits")) {
        decodeStringSetArray(reddit.optJSONArray("customSubreddits"))
      } else {
        existing.redditCustomSubreddits
      },
      redditSort = RedditSort.fromId(reddit?.optString("sort", existing.redditSort) ?: existing.redditSort).id,
      weatherEnabled = w?.optBoolean("enabled", existing.weatherEnabled) ?: existing.weatherEnabled,
      weatherLocationName = w?.optString("locationName", existing.weatherLocationName)
        ?: existing.weatherLocationName,
      weatherLatitude = if (w != null && w.has("latitude") && !w.isNull("latitude")) {
        w.optDouble("latitude")
      } else {
        existing.weatherLatitude
      },
      weatherLongitude = if (w != null && w.has("longitude") && !w.isNull("longitude")) {
        w.optDouble("longitude")
      } else {
        existing.weatherLongitude
      },
      weatherDays = (w?.optInt("days", existing.weatherDays) ?: existing.weatherDays).coerceIn(1, 7),
      weatherShowForecast = w?.optBoolean("showForecast", existing.weatherShowForecast)
        ?: existing.weatherShowForecast,
      weatherForecastSize = WeatherForecastSize.fromId(
        w?.optString("forecastSize", existing.weatherForecastSize) ?: existing.weatherForecastSize,
      ).id,
      weatherUseFahrenheit = w?.optBoolean("useFahrenheit", existing.weatherUseFahrenheit)
        ?: existing.weatherUseFahrenheit,
      weatherIconStyle = w?.optString("iconStyle", existing.weatherIconStyle)
        ?.ifBlank { existing.weatherIconStyle }
        ?: existing.weatherIconStyle,
      weatherShowBackdrop = w?.optBoolean("showBackdrop", existing.weatherShowBackdrop)
        ?: existing.weatherShowBackdrop,
      screensaverEnabled = ss?.optBoolean("enabled", existing.screensaverEnabled) ?: existing.screensaverEnabled,
      screensaverMode = ScreensaverMode.fromId(
        ss?.optString("mode", existing.screensaverMode) ?: existing.screensaverMode,
      ).id,
      screensaverTimeoutMinutes = (ss?.optInt("timeoutMinutes", existing.screensaverTimeoutMinutes)
        ?: existing.screensaverTimeoutMinutes).coerceIn(1, 60),
      screensaverIntervalSeconds = (ss?.optInt("intervalSeconds", existing.screensaverIntervalSeconds)
        ?: existing.screensaverIntervalSeconds).coerceIn(5, 300),
      screensaverKenBurns = ss?.optBoolean("kenBurns", existing.screensaverKenBurns)
        ?: existing.screensaverKenBurns,
      screensaverShowClock = ss?.optBoolean("showClock", existing.screensaverShowClock)
        ?: existing.screensaverShowClock,
      screensaverShowWeather = ss?.optBoolean("showWeather", existing.screensaverShowWeather)
        ?: existing.screensaverShowWeather,
      screensaverShowTitle = ss?.optBoolean("showTitle", existing.screensaverShowTitle)
        ?: existing.screensaverShowTitle,
      screensaverBurnInShift = ss?.optBoolean("burnInShift", existing.screensaverBurnInShift)
        ?: existing.screensaverBurnInShift,
      screensaverDimAlpha = (ss?.optDouble("dimAlpha", existing.screensaverDimAlpha.toDouble())
        ?: existing.screensaverDimAlpha.toDouble()).toFloat().coerceIn(0f, 0.75f),
      screensaverAmbientCycle = ss?.optBoolean("ambientCycle", existing.screensaverAmbientCycle)
        ?: existing.screensaverAmbientCycle,
      hasInitializedFavorites = true,
    )
  }

  internal fun decodeStringSetArray(array: JSONArray?): Set<String> {
    if (array == null) return emptySet()
    return buildSet {
      for (i in 0 until array.length()) {
        val v = array.optString(i, "").trim()
        if (v.isNotEmpty()) add(v)
      }
    }
  }

  internal fun decodeRowsArray(array: JSONArray?): List<AppRow> {
    if (array == null) return emptyList()
    return buildList {
      for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val id = obj.optString("id", "").trim().ifEmpty { "row_$i" }
        val title = obj.optString("title", "").trim().ifEmpty { "Row ${i + 1}" }
        val pkgsJson = obj.optJSONArray("packages")
        val pkgs = buildList {
          if (pkgsJson != null) {
            for (j in 0 until pkgsJson.length()) {
              val p = pkgsJson.optString(j, "").trim()
              if (p.isNotEmpty()) add(p)
            }
          }
        }.distinct()
        add(AppRow(id = id, title = title, packageNames = pkgs))
      }
    }
  }
}

class LauncherPreferences(context: Context) : SettingsStore {
  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun load(): LauncherSettings {
    val hidden = prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()
    val customLabels = decodeStringMap(prefs.getString(KEY_CUSTOM_LABELS, null))
    val customIconPaths = decodeStringMap(prefs.getString(KEY_CUSTOM_ICONS, null))
    val rows = loadRows()

    return LauncherSettings(
      rows = rows,
      hiddenPackages = hidden,
      customLabels = customLabels,
      customIconPaths = customIconPaths,
      themeId = prefs.getString(KEY_THEME_ID, "midnight_horizon") ?: "midnight_horizon",
      accentId = prefs.getString(KEY_ACCENT_ID, "sky_cyan") ?: "sky_cyan",
      customWallpaperUri = prefs.getString(KEY_CUSTOM_WALLPAPER, null),
      wallpaperDimAlpha = prefs.getFloat(KEY_WALLPAPER_DIM, 0.45f).coerceIn(0.20f, 0.85f),
      wallpaperProvider = MediaWallpaperProvider.fromId(
        prefs.getString(KEY_WALLPAPER_PROVIDER, MediaWallpaperProvider.JELLYFIN.id),
      ).id,
      jellyfinEnabled = prefs.getBoolean(KEY_JELLYFIN_ENABLED, false),
      jellyfinServerUrl = prefs.getString(KEY_JELLYFIN_URL, "") ?: "",
      jellyfinApiKey = prefs.getString(KEY_JELLYFIN_TOKEN, "") ?: "",
      jellyfinUserId = prefs.getString(KEY_JELLYFIN_USER_ID, "") ?: "",
      jellyfinIntervalSeconds = prefs.getInt(KEY_JELLYFIN_INTERVAL, 30).coerceIn(10, 600),
      jellyfinShowTitle = prefs.getBoolean(KEY_JELLYFIN_SHOW_TITLE, true),
      jellyfinMediaType = prefs.getString(KEY_JELLYFIN_MEDIA_TYPE, JellyfinMediaType.BOTH.id)
        ?: JellyfinMediaType.BOTH.id,
      jellyfinLibraryIds = loadJellyfinLibraryIds(),
      jellyfinBackdropLimit = prefs.getInt(KEY_JELLYFIN_LIMIT, 40).coerceIn(10, 500),
      plexServerUrl = prefs.getString(KEY_PLEX_URL, "") ?: "",
      plexToken = prefs.getString(KEY_PLEX_TOKEN, "") ?: "",
      plexLibraryIds = prefs.getStringSet(KEY_PLEX_LIBRARY_IDS, emptySet())?.toSet() ?: emptySet(),
      embyServerUrl = prefs.getString(KEY_EMBY_URL, "") ?: "",
      embyApiKey = prefs.getString(KEY_EMBY_TOKEN, "") ?: "",
      embyUserId = prefs.getString(KEY_EMBY_USER_ID, "") ?: "",
      embyLibraryIds = prefs.getStringSet(KEY_EMBY_LIBRARY_IDS, emptySet())?.toSet() ?: emptySet(),
      tmdbApiKey = prefs.getString(KEY_TMDB_API_KEY, "") ?: "",
      tmdbCategories = prefs.getStringSet(KEY_TMDB_CATEGORIES, null)
        ?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
        ?.ifEmpty { LauncherSettings.DEFAULT_TMDB_CATEGORIES }
        ?: LauncherSettings.DEFAULT_TMDB_CATEGORIES,
      redditSubreddits = prefs.getStringSet(KEY_REDDIT_SUBREDDITS, null)
        ?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
        ?.ifEmpty { LauncherSettings.DEFAULT_REDDIT_SUBREDDITS }
        ?: LauncherSettings.DEFAULT_REDDIT_SUBREDDITS,
      redditCustomSubreddits = prefs.getStringSet(KEY_REDDIT_CUSTOM_SUBREDDITS, emptySet())?.toSet() ?: emptySet(),
      redditSort = RedditSort.fromId(prefs.getString(KEY_REDDIT_SORT, RedditSort.TOP_MONTH.id)).id,
      wallpaperProviderConfigs = decodeProviderConfigsJson(prefs.getString(KEY_WALLPAPER_PROVIDER_CONFIGS, null)),
      weatherEnabled = prefs.getBoolean(KEY_WEATHER_ENABLED, false),
      weatherLocationName = prefs.getString(KEY_WEATHER_LOCATION, "") ?: "",
      weatherLatitude = prefs.getString(KEY_WEATHER_LAT, null)?.toDoubleOrNull(),
      weatherLongitude = prefs.getString(KEY_WEATHER_LON, null)?.toDoubleOrNull(),
      weatherDays = prefs.getInt(KEY_WEATHER_DAYS, 5).coerceIn(1, 7),
      weatherShowForecast = prefs.getBoolean(KEY_WEATHER_SHOW_FORECAST, true),
      weatherForecastSize = WeatherForecastSize.fromId(
        prefs.getString(KEY_WEATHER_FORECAST_SIZE, WeatherForecastSize.MEDIUM.id),
      ).id,
      weatherUseFahrenheit = prefs.getBoolean(KEY_WEATHER_FAHRENHEIT, false),
      weatherIconStyle = prefs.getString(KEY_WEATHER_ICON_STYLE, "color_vivid") ?: "color_vivid",
      weatherShowBackdrop = prefs.getBoolean(KEY_WEATHER_BACKDROP, true),
      showClock = prefs.getBoolean(KEY_SHOW_CLOCK, true),
      showDate = prefs.getBoolean(KEY_SHOW_DATE, true),
      use24HourClock = prefs.getBoolean(KEY_USE_24H, false),
      showNonLeanbackApps = prefs.getBoolean(KEY_SHOW_NON_LEANBACK, true),
      showSearchBar = prefs.getBoolean(KEY_SHOW_SEARCH_BAR, true),
      showAllAppsSection = prefs.getBoolean(KEY_SHOW_ALL_APPS, true),
      showAddAppTile = prefs.getBoolean(KEY_SHOW_ADD_APP_TILE, true),
      showTileTitles = prefs.getBoolean(KEY_SHOW_TILE_TITLES, true),
      showTvInputsButton = prefs.getBoolean(KEY_SHOW_TV_INPUTS, true),
      lockHomeLayout = prefs.getBoolean(KEY_LOCK_HOME_LAYOUT, false),
      homeTileSize = HomeTileSize.fromId(
        prefs.getString(KEY_HOME_TILE_SIZE, HomeTileSize.MEDIUM.id),
      ).id,
      disabledSearchProviders = prefs.getStringSet(KEY_DISABLED_SEARCH_PROVIDERS, emptySet())?.toSet() ?: emptySet(),
      justWatchFilterServices = prefs.getBoolean(KEY_JW_FILTER_SERVICES, false),
      justWatchServices = prefs.getStringSet(KEY_JW_SERVICES, LauncherSettings.DEFAULT_JUSTWATCH_SERVICES)?.toSet()
        ?: LauncherSettings.DEFAULT_JUSTWATCH_SERVICES,
      justWatchIncludeRentBuy = prefs.getBoolean(KEY_JW_INCLUDE_RENT_BUY, true),
      justWatchHideUnmatchedTitles = prefs.getBoolean(KEY_JW_HIDE_UNMATCHED, false),
      justWatchRegion = JustWatchRegion.fromCode(prefs.getString(KEY_JW_REGION, JustWatchRegion.GB.code)).code,
      justWatchShowRottenTomatoes = prefs.getBoolean(KEY_JW_SHOW_RT, true),
      justWatchShowAgeRating = prefs.getBoolean(KEY_JW_SHOW_AGE_RATING, true),
      gridColumns = prefs.getInt(KEY_GRID_COLUMNS, 5).coerceIn(4, 6),
      hasInitializedFavorites = prefs.getBoolean(KEY_INIT_FAVORITES, false),
      screensaverEnabled = prefs.getBoolean(KEY_SS_ENABLED, false),
      screensaverMode = ScreensaverMode.fromId(
        prefs.getString(KEY_SS_MODE, ScreensaverMode.WALLPAPER_ARTWORK.id),
      ).id,
      screensaverTimeoutMinutes = prefs.getInt(KEY_SS_TIMEOUT_MIN, 5).coerceIn(1, 60),
      screensaverIntervalSeconds = prefs.getInt(KEY_SS_INTERVAL_SEC, 20).coerceIn(5, 300),
      screensaverKenBurns = prefs.getBoolean(KEY_SS_KEN_BURNS, true),
      screensaverShowClock = prefs.getBoolean(KEY_SS_SHOW_CLOCK, true),
      screensaverShowWeather = prefs.getBoolean(KEY_SS_SHOW_WEATHER, true),
      screensaverShowTitle = prefs.getBoolean(KEY_SS_SHOW_TITLE, true),
      screensaverBurnInShift = prefs.getBoolean(KEY_SS_BURN_IN_SHIFT, true),
      screensaverDimAlpha = prefs.getFloat(KEY_SS_DIM_ALPHA, 0.25f).coerceIn(0f, 0.75f),
      screensaverAmbientCycle = prefs.getBoolean(KEY_SS_AMBIENT_CYCLE, true),
      screensaverPhotoFolderUri = prefs.getString(KEY_SS_PHOTO_FOLDER, null),
      screensaverPhotoUris = decodeStringList(prefs.getString(KEY_SS_PHOTO_URIS, null)),
      updateCheckSchedule = UpdateCheckSchedule.fromId(
        prefs.getString(KEY_UPDATE_CHECK_SCHEDULE, UpdateCheckSchedule.WEEKLY.id),
      ).id,
      lastUpdateCheckMillis = prefs.getLong(KEY_LAST_UPDATE_CHECK_MS, 0L),
      parentalPin = (prefs.getString(KEY_PARENTAL_PIN, "") ?: "")
        .filter { it.isDigit() }
        .let { if (it.length == 4) it else "" },
      pinProtectSettings = prefs.getBoolean(KEY_PIN_PROTECT_SETTINGS, true),
      pinLockedPackages = prefs.getStringSet(KEY_PIN_LOCKED_PACKAGES, emptySet())?.toSet() ?: emptySet(),
    )
  }

  override fun save(settings: LauncherSettings) {
    val safeRows = settings.rows.ifEmpty {
      listOf(AppRow(id = LauncherSettings.DEFAULT_FAVORITES_ROW_ID, title = "Favorites"))
    }
    prefs.edit()
      .putString(KEY_CUSTOM_ROWS, encodeRows(safeRows))
      .putStringSet(KEY_HIDDEN, settings.hiddenPackages)
      .putString(KEY_CUSTOM_LABELS, encodeStringMap(settings.customLabels))
      .putString(KEY_CUSTOM_ICONS, encodeStringMap(settings.customIconPaths))
      .putString(KEY_THEME_ID, settings.themeId)
      .putString(KEY_ACCENT_ID, settings.accentId)
      .putString(KEY_CUSTOM_WALLPAPER, settings.customWallpaperUri)
      .putFloat(KEY_WALLPAPER_DIM, settings.wallpaperDimAlpha)
      .putString(KEY_WALLPAPER_PROVIDER, settings.wallpaperProvider)
      .putBoolean(KEY_JELLYFIN_ENABLED, settings.jellyfinEnabled)
      .putString(KEY_JELLYFIN_URL, settings.jellyfinServerUrl)
      .putString(KEY_JELLYFIN_TOKEN, settings.jellyfinApiKey)
      .putString(KEY_JELLYFIN_USER_ID, settings.jellyfinUserId)
      .putInt(KEY_JELLYFIN_INTERVAL, settings.jellyfinIntervalSeconds)
      .putBoolean(KEY_JELLYFIN_SHOW_TITLE, settings.jellyfinShowTitle)
      .putString(KEY_JELLYFIN_MEDIA_TYPE, settings.jellyfinMediaType)
      .putStringSet(KEY_JELLYFIN_LIBRARY_IDS, settings.jellyfinLibraryIds)
      .remove(KEY_JELLYFIN_LIBRARY_ID)
      .remove(KEY_JELLYFIN_LIBRARY_NAME)
      .putInt(KEY_JELLYFIN_LIMIT, settings.jellyfinBackdropLimit.coerceIn(10, 500))
      .putString(KEY_PLEX_URL, settings.plexServerUrl)
      .putString(KEY_PLEX_TOKEN, settings.plexToken)
      .putStringSet(KEY_PLEX_LIBRARY_IDS, settings.plexLibraryIds)
      .putString(KEY_EMBY_URL, settings.embyServerUrl)
      .putString(KEY_EMBY_TOKEN, settings.embyApiKey)
      .putString(KEY_EMBY_USER_ID, settings.embyUserId)
      .putStringSet(KEY_EMBY_LIBRARY_IDS, settings.embyLibraryIds)
      .putString(KEY_TMDB_API_KEY, settings.tmdbApiKey)
      .putStringSet(KEY_TMDB_CATEGORIES, settings.tmdbCategories)
      .putStringSet(KEY_REDDIT_SUBREDDITS, settings.redditSubreddits)
      .putStringSet(KEY_REDDIT_CUSTOM_SUBREDDITS, settings.redditCustomSubreddits)
      .putString(KEY_REDDIT_SORT, settings.redditSort)
      .putString(KEY_WALLPAPER_PROVIDER_CONFIGS, encodeProviderConfigs(settings.wallpaperProviderConfigs))
      .putBoolean(KEY_WEATHER_ENABLED, settings.weatherEnabled)
      .putString(KEY_WEATHER_LOCATION, settings.weatherLocationName)
      .putString(KEY_WEATHER_LAT, settings.weatherLatitude?.toString())
      .putString(KEY_WEATHER_LON, settings.weatherLongitude?.toString())
      .putInt(KEY_WEATHER_DAYS, settings.weatherDays.coerceIn(1, 7))
      .putBoolean(KEY_WEATHER_SHOW_FORECAST, settings.weatherShowForecast)
      .putString(KEY_WEATHER_FORECAST_SIZE, settings.weatherForecastSize)
      .putBoolean(KEY_WEATHER_FAHRENHEIT, settings.weatherUseFahrenheit)
      .putString(KEY_WEATHER_ICON_STYLE, settings.weatherIconStyle)
      .putBoolean(KEY_WEATHER_BACKDROP, settings.weatherShowBackdrop)
      .putBoolean(KEY_SHOW_CLOCK, settings.showClock)
      .putBoolean(KEY_SHOW_DATE, settings.showDate)
      .putBoolean(KEY_USE_24H, settings.use24HourClock)
      .putBoolean(KEY_SHOW_NON_LEANBACK, settings.showNonLeanbackApps)
      .putBoolean(KEY_SHOW_SEARCH_BAR, settings.showSearchBar)
      .putBoolean(KEY_SHOW_ALL_APPS, settings.showAllAppsSection)
      .putBoolean(KEY_SHOW_ADD_APP_TILE, settings.showAddAppTile)
      .putBoolean(KEY_SHOW_TILE_TITLES, settings.showTileTitles)
      .putBoolean(KEY_SHOW_TV_INPUTS, settings.showTvInputsButton)
      .putBoolean(KEY_LOCK_HOME_LAYOUT, settings.lockHomeLayout)
      .putString(KEY_HOME_TILE_SIZE, settings.homeTileSize)
      .putStringSet(KEY_DISABLED_SEARCH_PROVIDERS, settings.disabledSearchProviders)
      .putBoolean(KEY_JW_FILTER_SERVICES, settings.justWatchFilterServices)
      .putStringSet(KEY_JW_SERVICES, settings.justWatchServices)
      .putBoolean(KEY_JW_INCLUDE_RENT_BUY, settings.justWatchIncludeRentBuy)
      .putBoolean(KEY_JW_HIDE_UNMATCHED, settings.justWatchHideUnmatchedTitles)
      .putString(KEY_JW_REGION, settings.justWatchRegion)
      .putBoolean(KEY_JW_SHOW_RT, settings.justWatchShowRottenTomatoes)
      .putBoolean(KEY_JW_SHOW_AGE_RATING, settings.justWatchShowAgeRating)
      .putInt(KEY_GRID_COLUMNS, settings.gridColumns)
      .putBoolean(KEY_INIT_FAVORITES, settings.hasInitializedFavorites)
      .putBoolean(KEY_SS_ENABLED, settings.screensaverEnabled)
      .putString(KEY_SS_MODE, settings.screensaverMode)
      .putInt(KEY_SS_TIMEOUT_MIN, settings.screensaverTimeoutMinutes.coerceIn(1, 60))
      .putInt(KEY_SS_INTERVAL_SEC, settings.screensaverIntervalSeconds.coerceIn(5, 300))
      .putBoolean(KEY_SS_KEN_BURNS, settings.screensaverKenBurns)
      .putBoolean(KEY_SS_SHOW_CLOCK, settings.screensaverShowClock)
      .putBoolean(KEY_SS_SHOW_WEATHER, settings.screensaverShowWeather)
      .putBoolean(KEY_SS_SHOW_TITLE, settings.screensaverShowTitle)
      .putBoolean(KEY_SS_BURN_IN_SHIFT, settings.screensaverBurnInShift)
      .putFloat(KEY_SS_DIM_ALPHA, settings.screensaverDimAlpha.coerceIn(0f, 0.75f))
      .putBoolean(KEY_SS_AMBIENT_CYCLE, settings.screensaverAmbientCycle)
      .putString(KEY_SS_PHOTO_FOLDER, settings.screensaverPhotoFolderUri)
      .putString(KEY_SS_PHOTO_URIS, encodeStringList(settings.screensaverPhotoUris))
      .putString(KEY_UPDATE_CHECK_SCHEDULE, settings.updateCheckSchedule)
      .putLong(KEY_LAST_UPDATE_CHECK_MS, settings.lastUpdateCheckMillis)
      .putString(KEY_PARENTAL_PIN, settings.parentalPin)
      .putBoolean(KEY_PIN_PROTECT_SETTINGS, settings.pinProtectSettings)
      .putStringSet(KEY_PIN_LOCKED_PACKAGES, settings.pinLockedPackages)
      .apply()
  }

  private fun loadJellyfinLibraryIds(): Set<String> {
    val saved = prefs.getStringSet(KEY_JELLYFIN_LIBRARY_IDS, null)
    if (saved != null) {
      return saved.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    val legacy = prefs.getString(KEY_JELLYFIN_LIBRARY_ID, "")?.trim().orEmpty()
    return if (legacy.isNotEmpty()) setOf(legacy) else emptySet()
  }

  private fun loadRows(): List<AppRow> {
    val rawRows = prefs.getString(KEY_CUSTOM_ROWS, null)
    if (!rawRows.isNullOrBlank()) {
      val parsed = runCatching { SettingsBackupCodec.decodeRowsArray(JSONArray(rawRows)) }.getOrDefault(emptyList())
      if (parsed.isNotEmpty()) return parsed
    }

    val legacyFavs = (prefs.getString(KEY_LEGACY_FAVORITES, "") ?: "")
      .split(DELIMITER)
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .distinct()
    val legacyTitle = prefs.getString(KEY_LEGACY_FAVORITES_TITLE, "Favorites")
      ?.trim()
      .orEmpty()
      .ifEmpty { "Favorites" }

    return listOf(
      AppRow(
        id = LauncherSettings.DEFAULT_FAVORITES_ROW_ID,
        title = legacyTitle,
        packageNames = legacyFavs,
      ),
    )
  }

  private fun encodeRows(rows: List<AppRow>): String {
    val array = JSONArray()
    rows.forEach { row ->
      val obj = JSONObject()
      obj.put("id", row.id)
      obj.put("title", row.title)
      val pkgs = JSONArray()
      row.packageNames.forEach { pkgs.put(it) }
      obj.put("packages", pkgs)
      array.put(obj)
    }
    return array.toString()
  }

  private fun encodeStringList(list: List<String>): String {
    val arr = JSONArray()
    list.forEach { arr.put(it) }
    return arr.toString()
  }

  private fun decodeStringList(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val v = arr.optString(i, "").trim()
          if (v.isNotEmpty()) add(v)
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun encodeStringMap(map: Map<String, String>): String {
    val json = JSONObject()
    map.forEach { (k, v) -> json.put(k, v) }
    return json.toString()
  }

  private fun decodeStringMap(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
      val json = JSONObject(raw)
      val out = LinkedHashMap<String, String>()
      val keys = json.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val value = json.optString(key, "").trim()
        if (value.isNotEmpty()) out[key] = value
      }
      out
    }.getOrDefault(emptyMap())
  }

  private fun encodeProviderConfigs(map: Map<String, WallpaperProviderConfig>): String {
    val root = JSONObject()
    map.forEach { (providerId, cfg) ->
      root.put(
        providerId,
        JSONObject().apply {
          put("intervalSeconds", cfg.intervalSeconds)
          put("showTitle", cfg.showTitle)
          put("mediaType", cfg.mediaType)
          put("backdropLimit", cfg.backdropLimit)
        },
      )
    }
    return root.toString()
  }

  private fun decodeProviderConfigsJson(raw: String?): Map<String, WallpaperProviderConfig> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
      decodeProviderConfigs(JSONObject(raw)) ?: emptyMap()
    }.getOrDefault(emptyMap())
  }

  companion object {
    private const val PREFS_NAME = "horizon_tv_launcher_prefs"
    private const val DELIMITER = "|"
    private const val KEY_CUSTOM_ROWS = "custom_rows_json"
    private const val KEY_LEGACY_FAVORITES = "favorite_packages"
    private const val KEY_LEGACY_FAVORITES_TITLE = "favorites_title"
    private const val KEY_HIDDEN = "hidden_packages"
    private const val KEY_CUSTOM_LABELS = "custom_labels_json"
    private const val KEY_CUSTOM_ICONS = "custom_icons_json"
    private const val KEY_THEME_ID = "theme_id"
    private const val KEY_ACCENT_ID = "accent_id"
    private const val KEY_CUSTOM_WALLPAPER = "custom_wallpaper_uri"
    private const val KEY_WALLPAPER_DIM = "wallpaper_dim_alpha"
    private const val KEY_WALLPAPER_PROVIDER = "wallpaper_provider"
    private const val KEY_JELLYFIN_ENABLED = "jellyfin_enabled"
    private const val KEY_JELLYFIN_URL = "jellyfin_server_url"
    private const val KEY_JELLYFIN_TOKEN = "jellyfin_api_key"
    private const val KEY_JELLYFIN_USER_ID = "jellyfin_user_id"
    private const val KEY_JELLYFIN_INTERVAL = "jellyfin_interval_seconds"
    private const val KEY_JELLYFIN_SHOW_TITLE = "jellyfin_show_title"
    private const val KEY_JELLYFIN_MEDIA_TYPE = "jellyfin_media_type"
    private const val KEY_JELLYFIN_LIBRARY_ID = "jellyfin_library_id"
    private const val KEY_JELLYFIN_LIBRARY_NAME = "jellyfin_library_name"
    private const val KEY_JELLYFIN_LIBRARY_IDS = "jellyfin_library_ids"
    private const val KEY_JELLYFIN_LIMIT = "jellyfin_backdrop_limit"
    private const val KEY_PLEX_URL = "plex_server_url"
    private const val KEY_PLEX_TOKEN = "plex_token"
    private const val KEY_PLEX_LIBRARY_IDS = "plex_library_ids"
    private const val KEY_EMBY_URL = "emby_server_url"
    private const val KEY_EMBY_TOKEN = "emby_api_key"
    private const val KEY_EMBY_USER_ID = "emby_user_id"
    private const val KEY_EMBY_LIBRARY_IDS = "emby_library_ids"
    private const val KEY_TMDB_API_KEY = "tmdb_api_key"
    private const val KEY_TMDB_CATEGORIES = "tmdb_categories"
    private const val KEY_REDDIT_SUBREDDITS = "reddit_subreddits"
    private const val KEY_REDDIT_CUSTOM_SUBREDDITS = "reddit_custom_subreddits"
    private const val KEY_REDDIT_SORT = "reddit_sort"
    private const val KEY_WALLPAPER_PROVIDER_CONFIGS = "wallpaper_provider_configs_json"
    private const val KEY_WEATHER_ENABLED = "weather_enabled"
    private const val KEY_WEATHER_LOCATION = "weather_location_name"
    private const val KEY_WEATHER_LAT = "weather_latitude"
    private const val KEY_WEATHER_LON = "weather_longitude"
    private const val KEY_WEATHER_DAYS = "weather_days"
    private const val KEY_WEATHER_SHOW_FORECAST = "weather_show_forecast"
    private const val KEY_WEATHER_FORECAST_SIZE = "weather_forecast_size"
    private const val KEY_WEATHER_FAHRENHEIT = "weather_use_fahrenheit"
    private const val KEY_WEATHER_ICON_STYLE = "weather_icon_style"
    private const val KEY_WEATHER_BACKDROP = "weather_show_backdrop"
    private const val KEY_SHOW_CLOCK = "show_clock"
    private const val KEY_SHOW_DATE = "show_date"
    private const val KEY_USE_24H = "use_24h_clock"
    private const val KEY_SHOW_NON_LEANBACK = "show_non_leanback"
    private const val KEY_SHOW_SEARCH_BAR = "show_search_bar"
    private const val KEY_SHOW_ALL_APPS = "show_all_apps_section"
    private const val KEY_SHOW_ADD_APP_TILE = "show_add_app_tile"
    private const val KEY_SHOW_TILE_TITLES = "show_tile_titles"
    private const val KEY_SHOW_TV_INPUTS = "show_tv_inputs_button"
    private const val KEY_LOCK_HOME_LAYOUT = "lock_home_layout"
    private const val KEY_HOME_TILE_SIZE = "home_tile_size"
    private const val KEY_DISABLED_SEARCH_PROVIDERS = "disabled_search_providers"
    private const val KEY_JW_FILTER_SERVICES = "justwatch_filter_services"
    private const val KEY_JW_SERVICES = "justwatch_services_v2"
    private const val KEY_JW_INCLUDE_RENT_BUY = "justwatch_include_rent_buy"
    private const val KEY_JW_HIDE_UNMATCHED = "justwatch_hide_unmatched_titles"
    private const val KEY_JW_REGION = "justwatch_region"
    private const val KEY_JW_SHOW_RT = "justwatch_show_rotten_tomatoes"
    private const val KEY_JW_SHOW_AGE_RATING = "justwatch_show_age_rating"
    private const val KEY_GRID_COLUMNS = "grid_columns"
    private const val KEY_INIT_FAVORITES = "has_initialized_favorites"
    private const val KEY_SS_ENABLED = "screensaver_enabled"
    private const val KEY_SS_MODE = "screensaver_mode"
    private const val KEY_SS_TIMEOUT_MIN = "screensaver_timeout_minutes"
    private const val KEY_SS_INTERVAL_SEC = "screensaver_interval_seconds"
    private const val KEY_SS_KEN_BURNS = "screensaver_ken_burns"
    private const val KEY_SS_SHOW_CLOCK = "screensaver_show_clock"
    private const val KEY_SS_SHOW_WEATHER = "screensaver_show_weather"
    private const val KEY_SS_SHOW_TITLE = "screensaver_show_title"
    private const val KEY_SS_BURN_IN_SHIFT = "screensaver_burn_in_shift"
    private const val KEY_SS_DIM_ALPHA = "screensaver_dim_alpha"
    private const val KEY_SS_AMBIENT_CYCLE = "screensaver_ambient_cycle"
    private const val KEY_SS_PHOTO_FOLDER = "screensaver_photo_folder_uri"
    private const val KEY_SS_PHOTO_URIS = "screensaver_photo_uris_json"
    private const val KEY_UPDATE_CHECK_SCHEDULE = "update_check_schedule"
    private const val KEY_LAST_UPDATE_CHECK_MS = "last_update_check_millis"
    private const val KEY_PARENTAL_PIN = "parental_pin"
    private const val KEY_PIN_PROTECT_SETTINGS = "pin_protect_settings"
    private const val KEY_PIN_LOCKED_PACKAGES = "pin_locked_packages"
  }
}

private fun decodeProviderConfigs(obj: JSONObject?): Map<String, WallpaperProviderConfig>? {
  if (obj == null) return null
  val out = LinkedHashMap<String, WallpaperProviderConfig>()
  val keys = obj.keys()
  while (keys.hasNext()) {
    val providerId = keys.next().trim()
    val entry = obj.optJSONObject(providerId) ?: continue
    out[providerId] = WallpaperProviderConfig(
      intervalSeconds = entry.optInt("intervalSeconds", 30).coerceIn(10, 600),
      showTitle = entry.optBoolean("showTitle", true),
      mediaType = entry.optString("mediaType", JellyfinMediaType.BOTH.id)
        .ifBlank { JellyfinMediaType.BOTH.id },
      backdropLimit = entry.optInt("backdropLimit", 40).coerceIn(10, 500),
    )
  }
  return out
}
