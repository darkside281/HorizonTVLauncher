package com.horizontv.launcher.data

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.horizontv.launcher.model.AppInfo
import com.horizontv.launcher.model.ClockFontStyle
import com.horizontv.launcher.model.ClockFontWeight
import com.horizontv.launcher.model.ClockStyle
import com.horizontv.launcher.model.HomeTileSize
import com.horizontv.launcher.model.HomeTileSpacing
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JustWatchRegion
import com.horizontv.launcher.model.JustWatchServicePreset
import com.horizontv.launcher.model.LauncherSettings
import com.horizontv.launcher.model.MediaWallpaperProvider
import com.horizontv.launcher.model.RedditSort
import com.horizontv.launcher.model.ScreensaverMode
import com.horizontv.launcher.model.TileAspectShape
import com.horizontv.launcher.model.TileCornerStyle
import com.horizontv.launcher.model.TileFocusStyle
import com.horizontv.launcher.model.UpdateCheckSchedule
import com.horizontv.launcher.model.WeatherForecastSize
import com.horizontv.launcher.model.WeatherIconStyle
import com.horizontv.launcher.model.WeatherLocation
import com.horizontv.launcher.ui.theme.LauncherPalettes
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

interface WebCompanionBridge {
  fun getSettings(): LauncherSettings
  fun getApps(): List<AppInfo>
  fun getAllInstalledApps(): List<AppInfo>
  fun removeUninstalledFromRow(rowId: String)
  fun exportBackupJson(): String
  fun restoreBackupJson(raw: String): Boolean
  fun saveServerConfig(provider: String, url: String, token: String)
  fun updateHomeJson(json: JSONObject)
  fun updateSearchJson(json: JSONObject)
  fun updateWallpaperJson(json: JSONObject)
  fun nextWallpaperNow()
  fun updateScreensaverJson(json: JSONObject)
  fun updateWeatherJson(json: JSONObject)
  fun updateSecurityJson(json: JSONObject)
  fun updateAppAction(action: String, packageName: String, value: String)
  fun renameRow(rowId: String, newTitle: String)
  fun createRow(title: String)
  fun moveRow(rowId: String, delta: Int)
  fun deleteRow(rowId: String)
  fun setRowTileShape(rowId: String, shapeId: String)
  fun toggleAppInRow(rowId: String, packageName: String)
  fun moveAppInRow(rowId: String, packageName: String, delta: Int)
  fun setRowAppsOrder(rowId: String, packageNames: List<String>)
  fun sortRowAppsAlphabetically(rowId: String)
  fun searchWeatherLocationsSync(query: String): List<WeatherLocation>
  fun selectWeatherLocation(location: WeatherLocation)
  fun saveUploadedWallpaper(bytes: ByteArray): Boolean
  fun saveUploadedAppIcon(packageName: String, bytes: ByteArray): Boolean
  fun onCompanionActivity(message: String)
}

class LocalWebCompanionServer(
  private val scope: CoroutineScope,
  private val bridge: WebCompanionBridge,
) {
  private var serverSocket: ServerSocket? = null
  private var acceptJob: Job? = null
  private val running = AtomicBoolean(false)

  var sessionCode: String = ""
    private set
  var baseUrl: String = ""
    private set
  var qrUrl: String = ""
    private set

  fun start(preferredPort: Int = 8765): Result<Triple<String, String, String>> = runCatching {
    stop()
    val socket = runCatching { ServerSocket(preferredPort) }
      .getOrElse { ServerSocket(0) }
    val port = socket.localPort
    val ip = findLocalIpv4Address() ?: "127.0.0.1"
    val code = (1000..9999).random().toString()

    serverSocket = socket
    sessionCode = code
    baseUrl = "http://$ip:$port"
    qrUrl = "http://$ip:$port/?code=$code"
    running.set(true)

    acceptJob = scope.launch(Dispatchers.IO) {
      while (running.get() && !socket.isClosed) {
        val client = runCatching { socket.accept() }.getOrNull() ?: break
        launch(Dispatchers.IO) {
          handleClient(client)
        }
      }
    }

    Triple(baseUrl, qrUrl, sessionCode)
  }

  fun stop() {
    running.set(false)
    acceptJob?.cancel()
    acceptJob = null
    runCatching { serverSocket?.close() }
    serverSocket = null
  }

  private fun handleClient(socket: Socket) {
    socket.use { client ->
      client.soTimeout = 15_000
      val input = BufferedInputStream(client.getInputStream())
      val output = client.getOutputStream()

      val requestLine = readLine(input) ?: return
      val parts = requestLine.split(" ")
      if (parts.size < 2) return
      val method = parts[0].uppercase()
      val fullPath = parts[1]
      val path = fullPath.substringBefore("?")
      val queryParams = parseQueryParams(fullPath.substringAfter("?", ""))

      var contentLength = 0
      while (true) {
        val header = readLine(input) ?: break
        if (header.isEmpty()) break
        val idx = header.indexOf(':')
        if (idx > 0) {
          val key = header.substring(0, idx).trim().lowercase()
          val value = header.substring(idx + 1).trim()
          if (key == "content-length") {
            contentLength = value.toIntOrNull()?.coerceIn(0, 12 * 1024 * 1024) ?: 0
          }
        }
      }

      val bodyBytes = if (contentLength > 0) readExactBytes(input, contentLength) else ByteArray(0)

      if (method == "OPTIONS") {
        writeResponse(output, 204, "text/plain", ByteArray(0))
        return
      }

      if (method == "GET" && (path == "/" || path == "/index.html")) {
        writeResponse(output, 200, "text/html; charset=utf-8", COMPANION_HTML.toByteArray(Charsets.UTF_8))
        return
      }

      if (path.startsWith("/api/")) {
        val suppliedCode = queryParams["code"].orEmpty().trim()
        if (suppliedCode != sessionCode) {
          writeJson(output, 401, JSONObject().put("error", "Invalid session code"))
          return
        }
      }

      when {
        method == "GET" && path == "/api/state" -> {
          writeJson(output, 200, buildStateJson())
        }

        method == "POST" && path == "/api/servers" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val provider = json.optString("provider", "").trim().lowercase()
          val url = json.optString("url", "").trim()
          val token = json.optString("token", "").trim()
          bridge.saveServerConfig(provider, url, token)
          bridge.onCompanionActivity("Updated ${provider.uppercase()} settings from browser")
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/home" -> {
          bridge.updateHomeJson(JSONObject(bodyBytes.toString(Charsets.UTF_8)))
          bridge.onCompanionActivity("Updated Home, Tile & Clock settings from browser")
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/search" -> {
          bridge.updateSearchJson(JSONObject(bodyBytes.toString(Charsets.UTF_8)))
          bridge.onCompanionActivity("Updated Search & Streaming Services from browser")
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/wallpaper" -> {
          bridge.updateWallpaperJson(JSONObject(bodyBytes.toString(Charsets.UTF_8)))
          bridge.onCompanionActivity("Updated Wallpaper & Theme settings from browser")
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/wallpaper/next" -> {
          bridge.nextWallpaperNow()
          bridge.onCompanionActivity("Skipped to next wallpaper backdrop")
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/screensaver" -> {
          bridge.updateScreensaverJson(JSONObject(bodyBytes.toString(Charsets.UTF_8)))
          bridge.onCompanionActivity("Updated Screensaver settings from browser")
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "GET" && path == "/api/weather/search" -> {
          val q = queryParams["q"].orEmpty().trim()
          val results = if (q.length >= 2) bridge.searchWeatherLocationsSync(q) else emptyList()
          val arr = JSONArray()
          results.forEach { loc ->
            arr.put(
              JSONObject().apply {
                put("name", loc.name)
                put("region", loc.region)
                put("country", loc.country)
                put("displayName", loc.displayName)
                put("latitude", loc.latitude)
                put("longitude", loc.longitude)
              },
            )
          }
          writeJson(output, 200, JSONObject().put("results", arr))
        }

        method == "POST" && path == "/api/weather/location" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val loc = WeatherLocation(
            name = json.optString("name", "").trim(),
            region = json.optString("region", "").trim(),
            country = json.optString("country", "").trim(),
            latitude = json.optDouble("latitude", 0.0),
            longitude = json.optDouble("longitude", 0.0),
          )
          if (loc.name.isNotEmpty()) {
            bridge.selectWeatherLocation(loc)
            bridge.onCompanionActivity("Set Weather location to ${loc.shortName}")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/weather" -> {
          bridge.updateWeatherJson(JSONObject(bodyBytes.toString(Charsets.UTF_8)))
          bridge.onCompanionActivity("Updated Weather settings from browser")
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/security" -> {
          bridge.updateSecurityJson(JSONObject(bodyBytes.toString(Charsets.UTF_8)))
          bridge.onCompanionActivity("Updated Parental Controls / System settings")
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/apps/action" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val action = json.optString("action", "").trim().lowercase()
          val pkg = json.optString("packageName", "").trim()
          val value = json.optString("value", "")
          if (action.isNotEmpty() && pkg.isNotEmpty()) {
            bridge.updateAppAction(action, pkg, value)
            bridge.onCompanionActivity("Updated app ($action) from browser")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/rows/rename" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val id = json.optString("id", "")
          val title = json.optString("title", "").trim()
          if (id.isNotEmpty() && title.isNotEmpty()) {
            bridge.renameRow(id, title)
            bridge.onCompanionActivity("Renamed row to \"$title\"")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/rows/shape" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val id = json.optString("id", "")
          val shape = json.optString("tileShape", "")
          if (id.isNotEmpty() && shape.isNotEmpty()) {
            bridge.setRowTileShape(id, shape)
            bridge.onCompanionActivity("Changed row tile shape")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/rows/add" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val title = json.optString("title", "").trim()
          if (title.isNotEmpty()) {
            bridge.createRow(title)
            bridge.onCompanionActivity("Added row \"$title\"")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/rows/move" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val id = json.optString("id", "")
          val delta = json.optInt("delta", 0)
          if (id.isNotEmpty() && delta != 0) {
            bridge.moveRow(id, delta)
            bridge.onCompanionActivity("Reordered Home rows")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/rows/delete" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val id = json.optString("id", "")
          if (id.isNotEmpty()) {
            bridge.deleteRow(id)
            bridge.onCompanionActivity("Deleted a Home row")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/rows/apps/toggle" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val rowId = json.optString("rowId", "")
          val pkg = json.optString("packageName", "")
          if (rowId.isNotEmpty() && pkg.isNotEmpty()) {
            bridge.toggleAppInRow(rowId, pkg)
            bridge.onCompanionActivity("Updated Dock / Row apps from browser")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/rows/apps/move" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val rowId = json.optString("rowId", "")
          val pkg = json.optString("packageName", "")
          val delta = json.optInt("delta", 0)
          if (rowId.isNotEmpty() && pkg.isNotEmpty() && delta != 0) {
            bridge.moveAppInRow(rowId, pkg, delta)
            bridge.onCompanionActivity("Rearranged Dock / Row tiles from browser")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/rows/apps/reorder" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val rowId = json.optString("rowId", "")
          val arr = json.optJSONArray("packageNames")
          if (rowId.isNotEmpty() && arr != null) {
            val pkgs = buildList {
              for (i in 0 until arr.length()) {
                val p = arr.optString(i, "").trim()
                if (p.isNotEmpty()) add(p)
              }
            }
            bridge.setRowAppsOrder(rowId, pkgs)
            bridge.onCompanionActivity("Dragged & reordered Dock / Row tiles")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/rows/apps/sort" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val rowId = json.optString("rowId", "")
          if (rowId.isNotEmpty()) {
            bridge.sortRowAppsAlphabetically(rowId)
            bridge.onCompanionActivity("Sorted row tiles A–Z from browser")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "POST" && path == "/api/rows/apps/cleanup" -> {
          val json = JSONObject(bodyBytes.toString(Charsets.UTF_8))
          val rowId = json.optString("rowId", "")
          if (rowId.isNotEmpty()) {
            bridge.removeUninstalledFromRow(rowId)
            bridge.onCompanionActivity("Cleaned up uninstalled apps from row")
          }
          writeJson(output, 200, JSONObject().put("ok", true))
        }

        method == "GET" && path == "/api/icon" -> {
          val pkg = queryParams["pkg"].orEmpty().trim()
          val app = bridge.getAllInstalledApps().firstOrNull { it.packageName == pkg }
          val img = app?.icon ?: app?.banner
          if (img != null) {
            val out = ByteArrayOutputStream()
            runCatching {
              val bmp = img.asAndroidBitmap()
              bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            val bytes = out.toByteArray()
            if (bytes.isNotEmpty()) {
              writeResponse(output, 200, "image/png", bytes, mapOf("Cache-Control" to "public, max-age=300"))
            } else {
              writeResponse(output, 404, "text/plain", ByteArray(0))
            }
          } else {
            writeResponse(output, 404, "text/plain", ByteArray(0))
          }
        }

        method == "GET" && path == "/api/backup" -> {
          val raw = bridge.exportBackupJson().toByteArray(Charsets.UTF_8)
          bridge.onCompanionActivity("Backup JSON downloaded to browser")
          writeResponse(
            output = output,
            status = 200,
            contentType = "application/json; charset=utf-8",
            body = raw,
            extraHeaders = mapOf("Content-Disposition" to "attachment; filename=\"horizon-tv-backup.json\""),
          )
        }

        method == "POST" && path == "/api/restore" -> {
          val ok = bridge.restoreBackupJson(bodyBytes.toString(Charsets.UTF_8))
          if (ok) {
            bridge.onCompanionActivity("Restored backup JSON from browser")
            writeJson(output, 200, JSONObject().put("ok", true))
          } else {
            writeJson(output, 400, JSONObject().put("error", "Invalid Horizon backup JSON"))
          }
        }

        method == "POST" && path == "/api/upload-wallpaper" -> {
          val ok = bodyBytes.isNotEmpty() && bridge.saveUploadedWallpaper(bodyBytes)
          if (ok) {
            bridge.onCompanionActivity("Uploaded custom wallpaper from browser")
            writeJson(output, 200, JSONObject().put("ok", true))
          } else {
            writeJson(output, 400, JSONObject().put("error", "Could not save wallpaper image"))
          }
        }

        method == "POST" && path == "/api/upload-icon" -> {
          val pkg = queryParams["pkg"].orEmpty().trim()
          val ok = pkg.isNotEmpty() && bodyBytes.isNotEmpty() && bridge.saveUploadedAppIcon(pkg, bodyBytes)
          if (ok) {
            bridge.onCompanionActivity("Uploaded custom icon for $pkg")
            writeJson(output, 200, JSONObject().put("ok", true))
          } else {
            writeJson(output, 400, JSONObject().put("error", "Could not save app icon"))
          }
        }

        else -> writeJson(output, 404, JSONObject().put("error", "Not found"))
      }
    }
  }

  private fun buildStateJson(): JSONObject {
    val s = bridge.getSettings()
    val allInstalled = bridge.getAllInstalledApps()
    val installedByPkg = allInstalled.associateBy { it.packageName }

    val rowsArr = JSONArray()
    s.rows.forEach { r ->
      val pkgsArr = JSONArray()
      val itemsArr = JSONArray()
      r.packageNames.forEach { pkg ->
        pkgsArr.put(pkg)
        val installedApp = installedByPkg[pkg]
        val friendlyLabel = s.customLabels[pkg]?.takeIf { it.isNotBlank() }
          ?: installedApp?.label
          ?: friendlyPackageName(pkg)
        itemsArr.put(
          JSONObject().apply {
            put("packageName", pkg)
            put("label", friendlyLabel)
            put("installed", installedApp != null)
          },
        )
      }
      val installedCount = r.packageNames.count { it in installedByPkg }
      rowsArr.put(
        JSONObject().apply {
          put("id", r.id)
          put("title", r.title)
          put("tileShape", r.tileShape)
          put("count", installedCount)
          put("totalCount", r.packageNames.size)
          put("packageNames", pkgsArr)
          put("items", itemsArr)
        },
      )
    }

    val appsArr = JSONArray()
    bridge.getApps().sortedBy { it.label.lowercase() }.forEach { app ->
      appsArr.put(
        JSONObject().apply {
          put("packageName", app.packageName)
          put("label", app.label)
          put("originalLabel", app.originalLabel)
          put("hasCustomIcon", app.hasCustomIcon)
          put("pinLocked", app.packageName in s.pinLockedPackages)
        },
      )
    }

    val hiddenAppsArr = JSONArray()
    s.hiddenPackages.sorted().forEach { pkg ->
      val app = installedByPkg[pkg]
      hiddenAppsArr.put(
        JSONObject()
          .put("packageName", pkg)
          .put("label", app?.label ?: friendlyPackageName(pkg)),
      )
    }

    val regionsArr = JSONArray()
    JustWatchRegion.entries.forEach { reg ->
      regionsArr.put(JSONObject().put("code", reg.code).put("label", "${reg.label} (${reg.code})"))
    }
    val servicesArr = JSONArray()
    JustWatchServicePreset.entries.forEach { svc ->
      servicesArr.put(
        JSONObject()
          .put("id", svc.id)
          .put("label", svc.label)
          .put("category", svc.category.title)
          .put("enabled", svc.id in s.justWatchServices),
      )
    }
    val themesArr = JSONArray()
    LauncherPalettes.themes.forEach { t -> themesArr.put(JSONObject().put("id", t.id).put("name", t.name)) }
    val accentsArr = JSONArray()
    LauncherPalettes.accents.forEach { a -> accentsArr.put(JSONObject().put("id", a.id).put("name", a.name)) }
    val providersArr = JSONArray()
    MediaWallpaperProvider.entries.forEach { p ->
      providersArr.put(
        JSONObject().put("id", p.id).put("label", p.label).put("configured", s.isProviderConfigured(p)),
      )
    }
    val redditArr = JSONArray()
    (LauncherSettings.PRESET_REDDIT_SUBREDDITS + s.redditCustomSubreddits).distinct().forEach { sub ->
      redditArr.put(
        JSONObject()
          .put("id", sub)
          .put("label", LauncherSettings.friendlyRedditLabel(sub))
          .put("enabled", sub in s.redditSubreddits),
      )
    }
    val tmdbCatList = listOf(
      "trending_all" to "Trending Now",
      "popular_movie" to "Popular Movies",
      "top_rated_movie" to "Top Rated Movies",
      "now_playing_movie" to "Now Playing",
      "popular_tv" to "Popular TV Shows",
      "top_rated_tv" to "Top Rated TV Shows",
    )
    val tmdbCatsArr = JSONArray()
    tmdbCatList.forEach { (id, lbl) ->
      tmdbCatsArr.put(JSONObject().put("id", id).put("label", lbl).put("enabled", id in s.tmdbCategories))
    }

    return JSONObject().apply {
      // Home, Tiles, Clock & All Apps
      put("minimalistDockMode", s.minimalistDockMode)
      put("lockHomeLayout", s.lockHomeLayout)
      put("homeTileSize", s.homeTileSize)
      put("tileCornerStyle", s.tileCornerStyle)
      put("tileFocusStyle", s.tileFocusStyle)
      put("tileOpacity", s.tileOpacity.toDouble())
      put("tileSpacing", s.tileSpacing)
      put("showTileTitles", s.showTileTitles)
      put("showAddAppTile", s.showAddAppTile)
      put("showClock", s.showClock)
      put("showDate", s.showDate)
      put("use24HourClock", s.use24HourClock)
      put("clockStyle", s.clockStyle)
      put("clockFontStyle", s.clockFontStyle)
      put("clockFontWeight", s.clockFontWeight)
      put("showTvInputsButton", s.showTvInputsButton)
      put("showAllAppsSection", s.showAllAppsSection)
      put("gridColumns", s.gridColumns)
      put("showNonLeanbackApps", s.showNonLeanbackApps)

      // Search & Services
      put("showSearchBar", s.showSearchBar)
      put("searchOpenDetailsPopup", s.searchOpenDetailsPopup)
      put("justWatchRegion", s.justWatchRegion)
      put("justWatchShowRottenTomatoes", s.justWatchShowRottenTomatoes)
      put("justWatchShowAgeRating", s.justWatchShowAgeRating)
      put("justWatchFilterServices", s.justWatchFilterServices)
      put("justWatchIncludeRentBuy", s.justWatchIncludeRentBuy)
      put("justWatchHideUnmatchedTitles", s.justWatchHideUnmatchedTitles)
      put("searchProviderApps", s.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_APPS))
      put("searchProviderJellyfin", s.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_JELLYFIN))
      put("searchProviderJustWatch", s.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_JUSTWATCH))
      put("searchProviderYouTube", s.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_YOUTUBE))
      put("searchProviderWeb", s.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_WEB))

      // Servers
      put("jellyfinServerUrl", s.jellyfinServerUrl)
      put("jellyfinApiKey", s.jellyfinApiKey)
      put("plexServerUrl", s.plexServerUrl)
      put("plexToken", s.plexToken)
      put("embyServerUrl", s.embyServerUrl)
      put("embyApiKey", s.embyApiKey)
      put("tmdbApiKey", s.tmdbApiKey)

      // Wallpapers & Theme
      put("themeId", s.themeId)
      put("accentId", s.accentId)
      put("hasCustomWallpaper", !s.customWallpaperUri.isNullOrBlank())
      put("wallpaperProvider", s.wallpaperProvider)
      put("wallpaperSlideshowEnabled", s.jellyfinEnabled)
      put("wallpaperDimAlpha", s.wallpaperDimAlpha.toDouble())
      put("wallpaperKenBurns", s.wallpaperKenBurns)
      put("wallpaperIntervalSeconds", s.activeWallpaperIntervalSeconds)
      put("wallpaperBackdropLimit", s.activeWallpaperBackdropLimit)
      put("wallpaperMediaType", s.activeWallpaperMediaType)
      put("wallpaperShowTitle", s.activeWallpaperShowTitle)
      put("redditSort", s.redditSort)

      // Screensaver
      put("screensaverEnabled", s.screensaverEnabled)
      put("screensaverMode", s.screensaverMode)
      put("screensaverTimeoutMinutes", s.screensaverTimeoutMinutes)
      put("screensaverIntervalSeconds", s.screensaverIntervalSeconds)
      put("screensaverDimAlpha", s.screensaverDimAlpha.toDouble())
      put("screensaverKenBurns", s.screensaverKenBurns)
      put("screensaverShowClock", s.screensaverShowClock)
      put("screensaverShowWeather", s.screensaverShowWeather)
      put("screensaverShowTitle", s.screensaverShowTitle)
      put("screensaverBurnInShift", s.screensaverBurnInShift)
      put("screensaverAmbientCycle", s.screensaverAmbientCycle)

      // Weather
      put("weatherEnabled", s.weatherEnabled)
      put("weatherLocationName", s.weatherLocationName)
      put("weatherUseFahrenheit", s.weatherUseFahrenheit)
      put("weatherShowForecast", s.weatherShowForecast)
      put("weatherDays", s.weatherDays)
      put("weatherForecastSize", s.weatherForecastSize)
      put("weatherIconStyle", s.weatherIconStyle)
      put("weatherShowBackdrop", s.weatherShowBackdrop)

      // Security & System
      put("isPinEnabled", s.isPinEnabled)
      put("pinProtectSettings", s.pinProtectSettings)
      put("updateCheckSchedule", s.updateCheckSchedule)

      // Collections
      put("rows", rowsArr)
      put("apps", appsArr)
      put("hiddenApps", hiddenAppsArr)
      put("regions", regionsArr)
      put("streamingServices", servicesArr)
      put("themes", themesArr)
      put("accents", accentsArr)
      put("wallpaperProviders", providersArr)
      put("redditCategories", redditArr)
      put("tmdbCategories", tmdbCatsArr)
    }
  }

  private fun readLine(input: BufferedInputStream): String? {
    val out = ByteArrayOutputStream()
    var prev = -1
    while (true) {
      val b = input.read()
      if (b == -1) return if (out.size() == 0) null else out.toString("UTF-8")
      if (b == '\n'.code) {
        val str = out.toString("UTF-8")
        return if (prev == '\r'.code && str.isNotEmpty()) str.dropLast(1) else str
      }
      out.write(b)
      prev = b
    }
  }

  private fun readExactBytes(input: BufferedInputStream, length: Int): ByteArray {
    val buf = ByteArray(length)
    var read = 0
    while (read < length) {
      val n = input.read(buf, read, length - read)
      if (n <= 0) break
      read += n
    }
    return if (read == length) buf else buf.copyOf(read)
  }

  private fun parseQueryParams(query: String): Map<String, String> {
    if (query.isBlank()) return emptyMap()
    val map = LinkedHashMap<String, String>()
    query.split("&").forEach { pair ->
      val idx = pair.indexOf('=')
      if (idx > 0) {
        val k = runCatching { URLDecoder.decode(pair.substring(0, idx), "UTF-8") }.getOrDefault(pair.substring(0, idx))
        val v = runCatching { URLDecoder.decode(pair.substring(idx + 1), "UTF-8") }.getOrDefault(pair.substring(idx + 1))
        map[k] = v
      }
    }
    return map
  }

  private fun writeJson(output: OutputStream, status: Int, json: JSONObject) {
    writeResponse(output, status, "application/json; charset=utf-8", json.toString().toByteArray(Charsets.UTF_8))
  }

  private fun writeResponse(
    output: OutputStream,
    status: Int,
    contentType: String,
    body: ByteArray,
    extraHeaders: Map<String, String> = emptyMap(),
  ) {
    val statusText = when (status) {
      200 -> "OK"
      204 -> "No Content"
      400 -> "Bad Request"
      401 -> "Unauthorized"
      404 -> "Not Found"
      else -> "Error"
    }
    val sb = StringBuilder()
      .append("HTTP/1.1 ").append(status).append(' ').append(statusText).append("\r\n")
      .append("Content-Type: ").append(contentType).append("\r\n")
      .append("Content-Length: ").append(body.size).append("\r\n")
      .append("Cache-Control: no-store\r\n")
      .append("Connection: close\r\n")
    extraHeaders.forEach { (k, v) -> sb.append(k).append(": ").append(v).append("\r\n") }
    sb.append("\r\n")
    output.write(sb.toString().toByteArray(Charsets.UTF_8))
    if (body.isNotEmpty()) output.write(body)
    output.flush()
  }

  private fun findLocalIpv4Address(): String? {
    val candidates = mutableListOf<String>()
    runCatching {
      val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching
      while (interfaces.hasMoreElements()) {
        val ni = interfaces.nextElement()
        if (!ni.isUp || ni.isLoopback) continue
        val addrs = ni.inetAddresses
        while (addrs.hasMoreElements()) {
          val addr = addrs.nextElement()
          if (addr is Inet4Address && !addr.isLoopbackAddress) {
            val host = addr.hostAddress.orEmpty()
            if (host.isNotBlank()) candidates.add(host)
          }
        }
      }
    }
    return candidates.firstOrNull { it.startsWith("192.168.") || it.startsWith("10.") || it.startsWith("172.") }
      ?: candidates.firstOrNull()
  }

  private fun friendlyPackageName(pkg: String): String {
    val known = mapOf(
      "com.netflix.mediaclient" to "Netflix",
      "com.netflix.ninja" to "Netflix",
      "com.amazon.amazonvideo.livingroom" to "Prime Video",
      "com.amazon.avod.thirdpartyclient" to "Prime Video",
      "com.disney.disneyplus" to "Disney+",
      "com.apple.atve.androidtv.appletv" to "Apple TV",
      "com.wbd.stream" to "Max",
      "com.hbo.hbonow" to "Max",
      "com.crunchyroll.crunchyroid" to "Crunchyroll",
      "com.cbs.ott" to "Paramount+",
      "com.google.android.youtube.tv" to "YouTube",
      "com.google.android.youtube" to "YouTube",
      "com.teamsmart.videomanager.tv" to "SmartTube",
      "com.liskovsoft.smarttubetv.beta" to "SmartTube Beta",
      "org.jellyfin.androidtv" to "Jellyfin",
      "com.plexapp.android" to "Plex",
      "tv.emby.embyatv" to "Emby",
      "org.xbmc.kodi" to "Kodi",
      "com.stremio.one" to "Stremio",
      "com.spotify.tv.android" to "Spotify",
      "com.nvidia.bbciplayer" to "BBC iPlayer",
      "uk.co.bbc.iplayer" to "BBC iPlayer",
      "air.ITVMobilePlayer" to "ITVX",
      "com.itv.itvxtv" to "ITVX",
      "com.channel4.ondemand" to "Channel 4",
      "com.mobileiq.demand5" to "My5",
      "com.bskyb.nowtv" to "NOW",
      "com.android.calculator2" to "Calculator",
      "com.android.deskclock" to "Clock",
      "org.lineageos.etar" to "Calendar",
      "org.lineageos.aperture" to "Camera",
      "com.supercell.clashofclans" to "Clash of Clans",
      "com.android.vending" to "Google Play Store",
      "com.android.settings" to "Settings",
    )
    known[pkg]?.let { return it }
    val raw = pkg.substringAfterLast('.')
      .replace(Regex("([a-z])([A-Z])"), "$1 $2")
      .replace('_', ' ')
      .replace('-', ' ')
      .replace(Regex("\\d+$"), "")
      .trim()
      .ifEmpty { pkg }
    return raw.split(' ').filter { it.isNotBlank() }.joinToString(" ") { w ->
      w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
  }

  companion object {
    private val COMPANION_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>Horizon TV Companion</title>
<style>
  :root { --bg:#060b16; --card:#10192b; --border:rgba(255,255,255,.12); --accent:#38bdf8; --text:#f8fafc; --muted:#94a3b8; }
  * { box-sizing:border-box; }
  body { margin:0; font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif; background:radial-gradient(circle at 15% 10%, rgba(14,165,233,.22), transparent 50%), var(--bg); color:var(--text); min-height:100vh; padding:18px; }
  .wrap { max-width:960px; margin:0 auto; }
  .top { display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:12px; margin-bottom:16px; }
  h1 { margin:0; font-size:22px; }
  .sub { color:var(--muted); font-size:13px; margin-top:2px; }
  .badge { background:rgba(16,185,129,.16); border:1px solid rgba(16,185,129,.5); color:#6ee7b7; padding:6px 12px; border-radius:999px; font-size:12px; font-weight:700; }
  .tabs { display:flex; flex-wrap:wrap; gap:8px; margin-bottom:16px; }
  .tab { background:rgba(255,255,255,.06); border:1px solid var(--border); color:#cbd5e1; padding:9px 13px; border-radius:12px; cursor:pointer; font-weight:600; font-size:13px; }
  .tab.active { background:rgba(56,189,248,.22); border-color:var(--accent); color:#fff; }
  .card { background:var(--card); border:1px solid var(--border); border-radius:18px; padding:18px; margin-bottom:16px; }
  .card h2 { margin:0 0 6px; font-size:16px; }
  label { display:block; font-size:11px; font-weight:700; letter-spacing:.06em; color:var(--accent); margin:13px 0 6px; text-transform:uppercase; }
  input[type=text], input[type=password], select { width:100%; background:#070d19; border:1px solid var(--border); color:#fff; padding:10px 12px; border-radius:10px; font-size:14px; }
  .row { display:flex; flex-wrap:wrap; gap:8px; align-items:center; }
  .grid2 { display:grid; grid-template-columns:repeat(auto-fit, minmax(210px, 1fr)); gap:12px; }
  .grid3 { display:grid; grid-template-columns:repeat(auto-fit, minmax(170px, 1fr)); gap:8px; }
  .btn { background:var(--accent); color:#04121f; border:none; padding:9px 15px; border-radius:999px; font-weight:800; font-size:13px; cursor:pointer; }
  .btn.sm { padding:6px 11px; font-size:12px; }
  .btn.sec { background:rgba(255,255,255,.08); color:#fff; border:1px solid var(--border); font-weight:600; }
  .btn.danger { background:rgba(239,68,68,.2); color:#fca5a5; border:1px solid rgba(239,68,68,.45); }
  .item { display:flex; align-items:center; justify-content:space-between; gap:8px; background:rgba(255,255,255,.04); border:1px solid var(--border); padding:9px 12px; border-radius:12px; margin-top:8px; }
  .chip-check { display:flex; align-items:center; gap:8px; background:rgba(255,255,255,.04); border:1px solid var(--border); padding:8px 10px; border-radius:10px; font-size:13px; cursor:pointer; }
  .scrollbox { max-height:260px; overflow-y:auto; margin-top:8px; padding-right:4px; }
  .toast { position:fixed; bottom:20px; left:50%; transform:translateX(-50%); background:#0f172a; border:1px solid var(--accent); color:#fff; padding:10px 18px; border-radius:999px; font-size:13px; font-weight:600; display:none; z-index:99; }
  .hidden { display:none; }
  .toggle { display:flex; align-items:center; justify-content:space-between; padding:10px 0; border-bottom:1px solid rgba(255,255,255,.07); font-size:14px; gap:12px; }
  input[type=checkbox] { width:18px; height:18px; accent-color:var(--accent); cursor:pointer; flex-shrink:0; }
  .pill { display:inline-block; background:rgba(56,189,248,.16); color:#7dd3fc; border:1px solid rgba(56,189,248,.35); padding:3px 10px; border-radius:999px; font-size:12px; font-weight:700; }
  .pill.warn { background:rgba(245,158,11,.14); color:#fcd34d; border-color:rgba(245,158,11,.35); font-size:11px; padding:2px 8px; }
  .app-meta { display:flex; align-items:center; gap:10px; min-width:0; }
  .app-ico { width:30px; height:30px; border-radius:8px; object-fit:cover; background:rgba(255,255,255,.08); flex-shrink:0; }
  .app-fallback { width:30px; height:30px; border-radius:8px; background:rgba(56,189,248,.16); color:#7dd3fc; display:inline-flex; align-items:center; justify-content:center; font-weight:800; font-size:13px; flex-shrink:0; }
  .drag-handle { cursor:grab; color:var(--muted); font-size:16px; padding:4px 6px; user-select:none; touch-action:none; letter-spacing:-2px; }
  .drag-handle:active { cursor:grabbing; color:var(--accent); }
  .item.dragging { opacity:.45; border-color:var(--accent); }
  .item.drag-over { border-color:var(--accent); background:rgba(56,189,248,.14); box-shadow:0 0 0 1px var(--accent); }
</style>
</head>
<body>
<div class="wrap">
  <div class="top">
    <div>
      <h1>Horizon TV Companion</h1>
      <div class="sub">Complete live TV remote editor • Every setting saves immediately</div>
    </div>
    <div id="statusBadge" class="badge">Connecting…</div>
  </div>

  <div id="authCard" class="card hidden">
    <h2>Enter 4-Digit TV Session Code</h2>
    <p class="sub">Enter the code displayed on your TV screen to unlock editing.</p>
    <div class="row" style="margin-top:12px;">
      <input id="codeInput" type="text" maxlength="4" placeholder="e.g. 4829" style="max-width:180px;font-size:18px;letter-spacing:4px;text-align:center;" />
      <button class="btn" onclick="connectWithCode()">Connect</button>
    </div>
  </div>

  <div id="appBody" class="hidden">
    <div class="tabs">
      <button class="tab active" onclick="showTab('home', this)">Home &amp; Dock</button>
      <button class="tab" onclick="showTab('search', this)">Search &amp; Services</button>
      <button class="tab" onclick="showTab('media', this)">Wallpapers &amp; Apps</button>
      <button class="tab" onclick="showTab('ambient', this)">Screensaver &amp; Weather</button>
      <button class="tab" onclick="showTab('servers', this)">Servers &amp; API Keys</button>
      <button class="tab" onclick="showTab('security', this)">Security &amp; Backup</button>
    </div>

    <!-- 1. HOME & DOCK TAB -->
    <div id="tab-home">
      <div class="card">
        <div class="row" style="justify-content:space-between;">
          <div>
            <h2>Dock &amp; Row Apps</h2>
            <div class="sub">Drag the ⋮⋮ handle, use ↑/↓ buttons, or add any installed Android app.</div>
          </div>
          <div class="row">
            <button id="cleanupRowBtn" class="btn sec sm hidden" onclick="cleanupSelectedRow()">Clean Up Uninstalled</button>
            <button class="btn sec sm" onclick="sortSelectedRow()">Sort A–Z</button>
          </div>
        </div>
        <label>Row to Edit</label>
        <select id="dockRowSelect" onchange="renderDockApps()"></select>
        <label>Current Tiles (in display order)</label>
        <div id="dockCurrentApps"></div>
        <label>Add Installed Android App</label>
        <input id="appSearchInput" type="text" placeholder="Filter installed TV apps…" oninput="renderDockApps()" />
        <div id="dockAvailableApps" class="scrollbox"></div>
      </div>

      <div class="card">
        <h2>Home Rows &amp; Per-Row Shapes</h2>
        <div class="sub">Switch each row between 16:9 Banner, 1:1 Square, or 2:3 Poster tiles.</div>
        <div id="rowsList"></div>
        <div class="row" style="margin-top:14px;">
          <input id="newRowTitle" type="text" placeholder="New row name (e.g. Streaming, Games)" style="flex:1;" />
          <button class="btn" onclick="addRow()">+ Add Row</button>
        </div>
      </div>

      <div class="card">
        <h2>Tile Style &amp; Layout</h2>
        <div class="toggle"><span>Minimalist Bottom-Dock Mode</span><input id="optMinimalist" type="checkbox" onchange="saveHomeOpts()" /></div>
        <div class="toggle"><span>Lock Home Layout (TV Remote)</span><input id="optLockLayout" type="checkbox" onchange="saveHomeOpts()" /></div>
        <div class="toggle"><span>Show App Titles Under Tiles</span><input id="optTitles" type="checkbox" onchange="saveHomeOpts()" /></div>
        <div class="toggle"><span>Show "+ Add App" Tile on Rows</span><input id="optAddTile" type="checkbox" onchange="saveHomeOpts()" /></div>
        <div class="grid2">
          <div>
            <label>Tile Size</label>
            <select id="optTileSize" onchange="saveHomeOpts()">
              <option value="small">Small</option><option value="medium">Medium</option><option value="large">Large</option>
            </select>
          </div>
          <div>
            <label>Corner Roundness</label>
            <select id="optCorners" onchange="saveHomeOpts()">
              <option value="sharp">Sharp</option><option value="rounded">Rounded</option><option value="soft">Soft</option>
            </select>
          </div>
          <div>
            <label>Focus Border Style</label>
            <select id="optFocusStyle" onchange="saveHomeOpts()">
              <option value="glow">Accent Glow</option><option value="outline">Crisp Outline</option><option value="zoom">Zoom Only</option><option value="shadow">Soft Shadow</option>
            </select>
          </div>
          <div>
            <label>Tile Spacing</label>
            <select id="optSpacing" onchange="saveHomeOpts()">
              <option value="compact">Compact</option><option value="normal">Normal</option><option value="wide">Wide</option>
            </select>
          </div>
          <div>
            <label>Tile Opacity (Unfocused)</label>
            <select id="optOpacity" onchange="saveHomeOpts()">
              <option value="1.0">100%</option><option value="0.85">85%</option><option value="0.7">70%</option><option value="0.55">55%</option><option value="0.4">40%</option>
            </select>
          </div>
        </div>
      </div>

      <div class="card">
        <h2>Clock, Header &amp; All Apps</h2>
        <div class="toggle"><span>Show Clock</span><input id="optShowClock" type="checkbox" onchange="saveHomeOpts()" /></div>
        <div class="toggle"><span>24-Hour Clock</span><input id="opt24h" type="checkbox" onchange="saveHomeOpts()" /></div>
        <div class="toggle"><span>Show Date</span><input id="optShowDate" type="checkbox" onchange="saveHomeOpts()" /></div>
        <div class="toggle"><span>Show TV Inputs (HDMI) Button</span><input id="optTvInputs" type="checkbox" onchange="saveHomeOpts()" /></div>
        <div class="grid2">
          <div>
            <label>Clock Layout Style</label>
            <select id="optClockStyle" onchange="saveHomeOpts()">
              <option value="classic">Classic</option><option value="centered">Centered Cinema</option><option value="analog">Analog Dial</option><option value="minimal">Minimal</option>
            </select>
          </div>
          <div>
            <label>Clock Font</label>
            <select id="optClockFont" onchange="saveHomeOpts()">
              <option value="sans">Modern Sans</option><option value="serif">Editorial Serif</option><option value="mono">Tech Mono</option>
            </select>
          </div>
          <div>
            <label>Clock Weight</label>
            <select id="optClockWeight" onchange="saveHomeOpts()">
              <option value="light">Light</option><option value="regular">Regular</option><option value="bold">Bold</option>
            </select>
          </div>
          <div>
            <label>All Apps Grid Columns</label>
            <select id="optGridCols" onchange="saveHomeOpts()">
              <option value="4">4 Columns</option><option value="5">5 Columns</option><option value="6">6 Columns</option>
            </select>
          </div>
        </div>
        <div class="toggle" style="margin-top:8px;"><span>Show All Apps Section</span><input id="optAllApps" type="checkbox" onchange="saveHomeOpts()" /></div>
        <div class="toggle"><span>Show Non-TV / Sideloaded Apps</span><input id="optNonLeanback" type="checkbox" onchange="saveHomeOpts()" /></div>
      </div>
    </div>

    <!-- 2. SEARCH & SERVICES TAB -->
    <div id="tab-search" class="hidden">
      <div class="card">
        <h2>Search General &amp; Region</h2>
        <div class="toggle"><span>Show Top-Right Search Button</span><input id="sShowBar" type="checkbox" onchange="saveSearchOpts()" /></div>
        <div class="toggle"><span>Open Full Title Details Popup on Click</span><input id="sDetailsPopup" type="checkbox" onchange="saveSearchOpts()" /></div>
        <div class="toggle"><span>Show Rotten Tomatoes Scores</span><input id="sShowRT" type="checkbox" onchange="saveSearchOpts()" /></div>
        <div class="toggle"><span>Show Age Ratings</span><input id="sShowAge" type="checkbox" onchange="saveSearchOpts()" /></div>
        <label>Search Country / Region</label>
        <select id="optRegion" onchange="saveSearchOpts()"></select>
        <label>Core Search Providers</label>
        <div class="grid3">
          <label class="chip-check"><input id="spJustWatch" type="checkbox" onchange="saveSearchOpts()" /> Where to Watch</label>
          <label class="chip-check"><input id="spJellyfin" type="checkbox" onchange="saveSearchOpts()" /> Jellyfin</label>
          <label class="chip-check"><input id="spApps" type="checkbox" onchange="saveSearchOpts()" /> Installed Apps</label>
          <label class="chip-check"><input id="spYouTube" type="checkbox" onchange="saveSearchOpts()" /> YouTube</label>
          <label class="chip-check"><input id="spWeb" type="checkbox" onchange="saveSearchOpts()" /> Web Search</label>
        </div>
      </div>

      <div class="card">
        <h2>My Streaming Services (Where to Watch)</h2>
        <div class="toggle"><span>Only Show My Streaming Services</span><input id="sFilterSvc" type="checkbox" onchange="saveSearchOpts()" /></div>
        <div class="toggle"><span>Include Rent &amp; Buy Offers</span><input id="sRentBuy" type="checkbox" onchange="saveSearchOpts()" /></div>
        <div class="toggle"><span>Hide Titles Not on My Services</span><input id="sHideUnmatched" type="checkbox" onchange="saveSearchOpts()" /></div>
        <div id="streamingServicesList" style="margin-top:10px;"></div>
      </div>
    </div>

    <!-- 3. WALLPAPERS & APPS TAB -->
    <div id="tab-media" class="hidden">
      <div class="card">
        <div class="row" style="justify-content:space-between;">
          <h2>Wallpaper, Theme &amp; Motion</h2>
          <button class="btn sec sm" onclick="nextWallpaper()">Next Backdrop →</button>
        </div>
        <div class="grid2">
          <div>
            <label>Ambient Theme</label>
            <select id="wpTheme" onchange="saveWallpaperTheme()"></select>
          </div>
          <div>
            <label>Accent Color</label>
            <select id="wpAccent" onchange="saveWallpaperOpts()"></select>
          </div>
        </div>
        <div class="toggle" style="margin-top:8px;"><span>Live Slideshow Wallpaper</span><input id="wpSlideshow" type="checkbox" onchange="saveWallpaperOpts()" /></div>
        <div class="toggle"><span>Ken Burns Slow Motion (Home Wallpaper)</span><input id="wpKenBurns" type="checkbox" onchange="saveWallpaperOpts()" /></div>
        <div class="toggle"><span>Show Movie/Show Logo or Credit</span><input id="wpShowTitle" type="checkbox" onchange="saveWallpaperOpts()" /></div>
        <div class="grid2">
          <div>
            <label>Slideshow Provider</label>
            <select id="wpProvider" onchange="saveWallpaperOpts()"></select>
          </div>
          <div>
            <label>Slide Interval</label>
            <select id="wpInterval" onchange="saveWallpaperOpts()">
              <option value="15">15 seconds</option><option value="30">30 seconds</option><option value="60">1 minute</option><option value="120">2 minutes</option><option value="300">5 minutes</option>
            </select>
          </div>
          <div>
            <label>Wallpaper Dimming</label>
            <select id="wpDim" onchange="saveWallpaperOpts()">
              <option value="0.25">25% (Bright)</option><option value="0.45">45% (Balanced)</option><option value="0.65">65% (Dark)</option><option value="0.80">80% (Cinema)</option>
            </select>
          </div>
          <div>
            <label>Wallpaper Count</label>
            <select id="wpLimit" onchange="saveWallpaperOpts()">
              <option value="20">20</option><option value="40">40</option><option value="80">80</option><option value="150">150</option><option value="300">300</option>
            </select>
          </div>
          <div>
            <label>Media Type (Jellyfin/Emby/Plex)</label>
            <select id="wpMediaType" onchange="saveWallpaperOpts()">
              <option value="both">Movies &amp; TV</option><option value="movies">Movies Only</option><option value="tv">TV Shows Only</option>
            </select>
          </div>
          <div>
            <label>Reddit Sort</label>
            <select id="wpRedditSort" onchange="saveWallpaperOpts()">
              <option value="hot">Hot</option><option value="top_week">Top Week</option><option value="top_month">Top Month</option><option value="top_all">Top All Time</option>
            </select>
          </div>
        </div>
        <label>Reddit Wallpaper Categories</label>
        <div id="redditCatsGrid" class="grid3"></div>
        <label>TMDB Categories</label>
        <div id="tmdbCatsGrid" class="grid3"></div>
      </div>

      <div class="card">
        <h2>Custom Home Wallpaper</h2>
        <div class="sub">Upload a 16:9 JPG or PNG from your phone or PC to set immediately on your TV.</div>
        <div class="row" style="margin-top:12px;">
          <input id="wallpaperFile" type="file" accept="image/*" />
          <button class="btn" onclick="uploadWallpaper()">Upload Wallpaper</button>
          <button id="clearWpBtn" class="btn sec hidden" onclick="clearCustomWallpaper()">Clear Custom Wallpaper</button>
        </div>
      </div>

      <div class="card">
        <h2>Customize App (Icon, Rename, Hide)</h2>
        <label>Select Installed App</label>
        <select id="iconAppSelect" onchange="syncSelectedAppInputs()"></select>
        <label>Custom Display Name</label>
        <div class="row">
          <input id="appRenameInput" type="text" placeholder="Custom app name (leave empty to reset)" style="flex:1;" />
          <button class="btn sec" onclick="renameSelectedApp()">Save Name</button>
          <button class="btn danger" onclick="hideSelectedApp()">Hide App</button>
        </div>
        <label>Custom App Icon / Banner</label>
        <div class="row">
          <input id="iconFile" type="file" accept="image/*" />
          <button class="btn" onclick="uploadAppIcon()">Upload Icon</button>
          <button class="btn sec" onclick="resetSelectedAppIcon()">Reset Icon</button>
        </div>
      </div>
    </div>

    <!-- 4. SCREENSAVER & WEATHER TAB -->
    <div id="tab-ambient" class="hidden">
      <div class="card">
        <h2>Weather Location &amp; Display</h2>
        <div class="sub">Current location: <span id="weatherLocBadge" class="pill">Not set</span></div>
        <label>Search City / Town</label>
        <div class="row">
          <input id="weatherQuery" type="text" placeholder="e.g. London, New York, Tokyo…" style="flex:1;" onkeydown="if(event.key==='Enter')searchWeather()" />
          <button class="btn" onclick="searchWeather()">Search</button>
        </div>
        <div id="weatherResults"></div>
        <div class="toggle" style="margin-top:10px;"><span>Show Weather on Home</span><input id="wEnabled" type="checkbox" onchange="saveWeatherOpts()" /></div>
        <div class="toggle"><span>Show Multi-Day Forecast</span><input id="wShowForecast" type="checkbox" onchange="saveWeatherOpts()" /></div>
        <div class="toggle"><span>Show Weather Card Backdrop</span><input id="wBackdrop" type="checkbox" onchange="saveWeatherOpts()" /></div>
        <div class="grid2">
          <div><label>Temperature Unit</label><select id="wUnit" onchange="saveWeatherOpts()"><option value="C">Celsius (°C)</option><option value="F">Fahrenheit (°F)</option></select></div>
          <div><label>Forecast Days</label><select id="wDays" onchange="saveWeatherOpts()"><option value="1">1 Day</option><option value="3">3 Days</option><option value="5">5 Days</option><option value="7">7 Days</option></select></div>
          <div><label>Forecast Size</label><select id="wSize" onchange="saveWeatherOpts()"><option value="small">Small</option><option value="medium">Medium</option><option value="large">Large</option></select></div>
          <div><label>Icon Style</label><select id="wIconStyle" onchange="saveWeatherOpts()"><option value="color_vivid">Vivid Color</option><option value="minimal_line">Minimal</option><option value="emoji">Emoji</option><option value="accent_tint">Accent</option></select></div>
        </div>
      </div>

      <div class="card">
        <h2>Screensaver Settings</h2>
        <div class="toggle"><span>Enable Built-In Screensaver</span><input id="ssEnabled" type="checkbox" onchange="saveScreensaverOpts()" /></div>
        <div class="grid2">
          <div><label>Mode</label><select id="ssMode" onchange="saveScreensaverOpts()"><option value="artwork">Wallpaper Artwork</option><option value="photos">My Photos</option><option value="ambient">Ambient Mode</option></select></div>
          <div><label>Idle Timeout</label><select id="ssTimeout" onchange="saveScreensaverOpts()"><option value="1">1 minute</option><option value="2">2 minutes</option><option value="5">5 minutes</option><option value="10">10 minutes</option><option value="15">15 minutes</option><option value="30">30 minutes</option></select></div>
          <div><label>Slide Interval</label><select id="ssInterval" onchange="saveScreensaverOpts()"><option value="10">10 seconds</option><option value="20">20 seconds</option><option value="30">30 seconds</option><option value="60">60 seconds</option></select></div>
          <div><label>Screensaver Dimming</label><select id="ssDim" onchange="saveScreensaverOpts()"><option value="0.0">0% (Off)</option><option value="0.25">25%</option><option value="0.45">45%</option><option value="0.65">65%</option></select></div>
        </div>
        <div class="toggle"><span>Ken Burns Slow Motion</span><input id="ssKenBurns" type="checkbox" onchange="saveScreensaverOpts()" /></div>
        <div class="toggle"><span>Show Clock</span><input id="ssClock" type="checkbox" onchange="saveScreensaverOpts()" /></div>
        <div class="toggle"><span>Show Weather</span><input id="ssWeather" type="checkbox" onchange="saveScreensaverOpts()" /></div>
        <div class="toggle"><span>Show Artwork Title</span><input id="ssTitle" type="checkbox" onchange="saveScreensaverOpts()" /></div>
        <div class="toggle"><span>OLED Burn-In Shift</span><input id="ssBurnIn" type="checkbox" onchange="saveScreensaverOpts()" /></div>
        <div class="toggle"><span>Cycle Ambient Themes</span><input id="ssAmbientCycle" type="checkbox" onchange="saveScreensaverOpts()" /></div>
      </div>
    </div>

    <!-- 5. SERVERS TAB -->
    <div id="tab-servers" class="card hidden">
      <h2>Media Wallpaper Servers &amp; API Keys</h2>
      <div class="sub">Paste URLs and API keys directly from your clipboard.</div>
      <label>Provider</label>
      <select id="srvProvider" onchange="syncServerInputs()">
        <option value="jellyfin">Jellyfin Server</option>
        <option value="plex">Plex Media Server</option>
        <option value="emby">Emby Server</option>
        <option value="tmdb">TMDB API Key</option>
      </select>
      <div id="srvUrlWrap">
        <label>Server URL</label>
        <input id="srvUrl" type="text" placeholder="http://192.168.0.50:8096" />
      </div>
      <label id="srvTokenLabel">API Key / Access Token</label>
      <input id="srvToken" type="text" placeholder="Paste API key or token" />
      <div class="row" style="margin-top:16px;">
        <button class="btn" onclick="saveServer()">Save &amp; Apply to TV</button>
      </div>
    </div>

    <!-- 6. SECURITY & BACKUP TAB -->
    <div id="tab-security" class="hidden">
      <div class="card">
        <h2>Parental Controls (4-Digit PIN)</h2>
        <div class="sub">Status: <span id="pinStatusBadge" class="pill">Off</span></div>
        <div class="row" style="margin-top:12px;">
          <input id="pinInput" type="password" maxlength="4" placeholder="New 4-digit PIN" style="max-width:180px;" />
          <button class="btn" onclick="saveParentalPin()">Set / Change PIN</button>
          <button id="clearPinBtn" class="btn danger hidden" onclick="clearParentalPin()">Remove PIN</button>
        </div>
        <div class="toggle" style="margin-top:10px;"><span>Require PIN to Open Customize</span><input id="pinProtectSettings" type="checkbox" onchange="saveSecurityOpts()" /></div>
        <label>PIN-Locked Apps</label>
        <div id="pinAppsList" class="scrollbox"></div>
      </div>

      <div class="card">
        <h2>Hidden Apps</h2>
        <div id="hiddenAppsList"></div>
      </div>

      <div class="card">
        <h2>Updates, Backup &amp; Reset</h2>
        <label>GitHub Update Check Schedule</label>
        <select id="optUpdateSchedule" onchange="saveSecurityOpts()">
          <option value="automatic">Automatic (on startup)</option>
          <option value="weekly">Every 7 Days</option>
          <option value="manual">Manual Only</option>
        </select>
        <div class="row" style="margin-top:16px;">
          <button class="btn" onclick="downloadBackup()">⬇ Download Backup (.json)</button>
        </div>
        <label style="margin-top:18px;">Restore Backup File (.json)</label>
        <div class="row">
          <input id="restoreFile" type="file" accept=".json,application/json" />
          <button class="btn sec" onclick="uploadRestore()">⬆ Upload &amp; Restore</button>
        </div>
        <div class="row" style="margin-top:18px;">
          <button class="btn danger" onclick="resetLauncherDefaults()">Reset Launcher to Defaults</button>
        </div>
      </div>
    </div>
  </div>
</div>
<div id="toast" class="toast"></div>
<script>
let CODE = new URLSearchParams(location.search).get('code') || '';
let STATE = null;
let SELECTED_ROW_ID = '';

function esc(s) { return String(s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
function toast(msg) {
  const el = document.getElementById('toast');
  el.textContent = msg; el.style.display = 'block';
  clearTimeout(window._tt); window._tt = setTimeout(() => el.style.display = 'none', 2800);
}
function showTab(name, btn) {
  document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  ['home','search','media','ambient','servers','security'].forEach(t => {
    document.getElementById('tab-' + t).classList.toggle('hidden', t !== name);
  });
}
function pickNearest(val, opts) {
  return opts.reduce((a,b) => Math.abs(parseFloat(b)-val) < Math.abs(parseFloat(a)-val) ? b : a);
}
async function api(path, opts) {
  const sep = path.includes('?') ? '&' : '?';
  const res = await fetch(path + sep + 'code=' + encodeURIComponent(CODE), opts);
  if (res.status === 401) {
    document.getElementById('authCard').classList.remove('hidden');
    document.getElementById('appBody').classList.add('hidden');
    document.getElementById('statusBadge').textContent = 'Locked';
    throw new Error('Unauthorized');
  }
  return res;
}
async function loadState() {
  try {
    const res = await api('/api/state');
    STATE = await res.json();
    document.getElementById('authCard').classList.add('hidden');
    document.getElementById('appBody').classList.remove('hidden');
    document.getElementById('statusBadge').textContent = '● Live Connected';
    renderState();
  } catch (e) {}
}
function connectWithCode() { CODE = document.getElementById('codeInput').value.trim(); loadState(); }
function syncServerInputs() {
  if (!STATE) return;
  const p = document.getElementById('srvProvider').value;
  document.getElementById('srvUrlWrap').style.display = p === 'tmdb' ? 'none' : 'block';
  document.getElementById('srvUrl').value = p === 'jellyfin' ? STATE.jellyfinServerUrl : p === 'plex' ? STATE.plexServerUrl : p === 'emby' ? STATE.embyServerUrl : '';
  document.getElementById('srvToken').value = p === 'jellyfin' ? STATE.jellyfinApiKey : p === 'plex' ? STATE.plexToken : p === 'emby' ? STATE.embyApiKey : STATE.tmdbApiKey;
}
function syncSelectedAppInputs() {
  if (!STATE) return;
  const pkg = document.getElementById('iconAppSelect').value;
  const app = STATE.apps.find(a => a.packageName === pkg);
  document.getElementById('appRenameInput').value = app ? app.label : '';
}
function appIconHtml(pkg, label, installed) {
  const initial = esc((label || '?').charAt(0).toUpperCase());
  if (!installed) return '<span class="app-fallback">' + initial + '</span>';
  const src = '/api/icon?pkg=' + encodeURIComponent(pkg) + '&code=' + encodeURIComponent(CODE);
  return '<img class="app-ico" src="' + src + '" alt="" onerror="this.outerHTML=\'<span class=app-fallback>' + initial + '</span>\'" />';
}
function renderDockApps() {
  if (!STATE) return;
  const rowSel = document.getElementById('dockRowSelect');
  const rowId = rowSel.value || (STATE.rows[0] && STATE.rows[0].id) || '';
  SELECTED_ROW_ID = rowId;
  const row = STATE.rows.find(r => r.id === rowId) || STATE.rows[0];
  if (!row) return;
  const items = row.items || [];
  document.getElementById('cleanupRowBtn').classList.toggle('hidden', !items.some(it => !it.installed));

  const curEl = document.getElementById('dockCurrentApps');
  curEl.innerHTML = '';
  if (items.length === 0) {
    curEl.innerHTML = '<div class="sub" style="padding:8px 0;">No apps in this row yet. Add some from below.</div>';
  } else {
    let dragFromIdx = -1, touchOverIdx = -1;
    const commitReorder = async (fromIdx, toIdx) => {
      if (fromIdx < 0 || toIdx < 0 || fromIdx === toIdx) return;
      const order = items.map(x => x.packageName);
      const [moved] = order.splice(fromIdx, 1);
      order.splice(toIdx, 0, moved);
      await api('/api/rows/apps/reorder', { method:'POST', body:JSON.stringify({ rowId:row.id, packageNames:order }) });
      toast('Tile order updated'); loadState();
    };
    const clearHighlights = () => curEl.querySelectorAll('.item').forEach(el => el.classList.remove('dragging', 'drag-over'));
    items.forEach((it, idx) => {
      const div = document.createElement('div');
      div.className = 'item'; div.draggable = true; div.dataset.idx = String(idx);
      div.ondragstart = (e) => { dragFromIdx = idx; div.classList.add('dragging'); if (e.dataTransfer) { e.dataTransfer.effectAllowed = 'move'; e.dataTransfer.setData('text/plain', String(idx)); } };
      div.ondragover = (e) => { e.preventDefault(); if (dragFromIdx !== -1 && dragFromIdx !== idx) { curEl.querySelectorAll('.item').forEach(el => el.classList.remove('drag-over')); div.classList.add('drag-over'); } };
      div.ondragleave = () => div.classList.remove('drag-over');
      div.ondrop = async (e) => { e.preventDefault(); const from = dragFromIdx; dragFromIdx = -1; clearHighlights(); await commitReorder(from, idx); };
      div.ondragend = () => { dragFromIdx = -1; clearHighlights(); };

      const badge = it.installed ? '' : ' <span class="pill warn">Not installed</span>';
      const meta = document.createElement('div'); meta.className = 'app-meta';
      const handle = document.createElement('span'); handle.className = 'drag-handle'; handle.title = 'Drag to reorder'; handle.textContent = '⋮⋮';
      handle.ontouchstart = () => { dragFromIdx = idx; touchOverIdx = idx; div.classList.add('dragging'); };
      handle.ontouchmove = (e) => {
        if (dragFromIdx === -1) return;
        e.preventDefault();
        const t = e.touches[0];
        const target = document.elementFromPoint(t.clientX, t.clientY);
        const itemEl = target && target.closest ? target.closest('#dockCurrentApps .item') : null;
        curEl.querySelectorAll('.item').forEach(el => el.classList.remove('drag-over'));
        if (itemEl && itemEl.dataset.idx !== undefined) {
          touchOverIdx = parseInt(itemEl.dataset.idx, 10);
          if (touchOverIdx !== dragFromIdx) itemEl.classList.add('drag-over');
        }
      };
      handle.ontouchend = async () => { const from = dragFromIdx, to = touchOverIdx; dragFromIdx = -1; touchOverIdx = -1; clearHighlights(); await commitReorder(from, to); };
      meta.appendChild(handle);
      const infoWrap = document.createElement('div'); infoWrap.className = 'app-meta';
      infoWrap.innerHTML = appIconHtml(it.packageName, it.label, it.installed) + '<div><strong>' + (idx + 1) + '. ' + esc(it.label) + '</strong>' + badge + '</div>';
      meta.appendChild(infoWrap);
      div.appendChild(meta);

      const acts = document.createElement('div'); acts.className = 'row';
      if (idx > 0) { const bUp = document.createElement('button'); bUp.className='btn sec sm'; bUp.textContent='↑'; bUp.onclick = async () => { await api('/api/rows/apps/move', { method:'POST', body:JSON.stringify({ rowId:row.id, packageName:it.packageName, delta:-1 }) }); loadState(); }; acts.appendChild(bUp); }
      if (idx < items.length - 1) { const bDn = document.createElement('button'); bDn.className='btn sec sm'; bDn.textContent='↓'; bDn.onclick = async () => { await api('/api/rows/apps/move', { method:'POST', body:JSON.stringify({ rowId:row.id, packageName:it.packageName, delta:1 }) }); loadState(); }; acts.appendChild(bDn); }
      const bRem = document.createElement('button'); bRem.className='btn danger sm'; bRem.textContent='Remove';
      bRem.onclick = async () => { await api('/api/rows/apps/toggle', { method:'POST', body:JSON.stringify({ rowId:row.id, packageName:it.packageName }) }); toast('Removed ' + it.label); loadState(); };
      acts.appendChild(bRem);
      div.appendChild(acts);
      curEl.appendChild(div);
    });
  }

  const q = (document.getElementById('appSearchInput').value || '').trim().toLowerCase();
  const inRow = new Set(row.packageNames || []);
  const availEl = document.getElementById('dockAvailableApps');
  availEl.innerHTML = '';
  const available = STATE.apps.filter(a => !inRow.has(a.packageName) && (!q || a.label.toLowerCase().includes(q)));
  if (available.length === 0) {
    availEl.innerHTML = '<div class="sub" style="padding:8px 0;">No matching apps left to add.</div>';
  } else {
    available.forEach(a => {
      const div = document.createElement('div'); div.className = 'item';
      div.innerHTML = '<div class="app-meta">' + appIconHtml(a.packageName, a.label, true) + '<div><strong>' + esc(a.label) + '</strong></div></div>';
      const bAdd = document.createElement('button'); bAdd.className='btn sm'; bAdd.textContent='+ Add';
      bAdd.onclick = async () => { await api('/api/rows/apps/toggle', { method:'POST', body:JSON.stringify({ rowId:row.id, packageName:a.packageName }) }); toast('Added ' + a.label); loadState(); };
      div.appendChild(bAdd);
      availEl.appendChild(div);
    });
  }
}
function renderState() {
  syncServerInputs();
  // Home & Layout
  document.getElementById('optMinimalist').checked = !!STATE.minimalistDockMode;
  document.getElementById('optLockLayout').checked = !!STATE.lockHomeLayout;
  document.getElementById('optTitles').checked = !!STATE.showTileTitles;
  document.getElementById('optAddTile').checked = !!STATE.showAddAppTile;
  document.getElementById('optTileSize').value = STATE.homeTileSize || 'medium';
  document.getElementById('optCorners').value = STATE.tileCornerStyle || 'rounded';
  document.getElementById('optFocusStyle').value = STATE.tileFocusStyle || 'glow';
  document.getElementById('optSpacing').value = STATE.tileSpacing || 'normal';
  document.getElementById('optOpacity').value = pickNearest(STATE.tileOpacity, ['1.0','0.85','0.7','0.55','0.4']);
  document.getElementById('optShowClock').checked = !!STATE.showClock;
  document.getElementById('opt24h').checked = !!STATE.use24HourClock;
  document.getElementById('optShowDate').checked = !!STATE.showDate;
  document.getElementById('optTvInputs').checked = !!STATE.showTvInputsButton;
  document.getElementById('optClockStyle').value = STATE.clockStyle || 'classic';
  document.getElementById('optClockFont').value = STATE.clockFontStyle || 'sans';
  document.getElementById('optClockWeight').value = STATE.clockFontWeight || 'light';
  document.getElementById('optGridCols').value = String(STATE.gridColumns || 5);
  document.getElementById('optAllApps').checked = !!STATE.showAllAppsSection;
  document.getElementById('optNonLeanback').checked = !!STATE.showNonLeanbackApps;

  // Dock row selector
  const dockSel = document.getElementById('dockRowSelect');
  dockSel.innerHTML = STATE.rows.map((r, i) => '<option value="'+esc(r.id)+'">'+esc(r.title)+(i===0?' (Primary / Dock)':'')+' ('+r.count+' apps)</option>').join('');
  dockSel.value = (SELECTED_ROW_ID && STATE.rows.some(r => r.id === SELECTED_ROW_ID)) ? SELECTED_ROW_ID : (STATE.rows[0] ? STATE.rows[0].id : '');
  renderDockApps();

  // Rows list with per-row Tile Shape
  const rowsEl = document.getElementById('rowsList');
  rowsEl.innerHTML = '';
  STATE.rows.forEach((r, idx) => {
    const div = document.createElement('div'); div.className = 'item';
    div.innerHTML = '<div><strong>'+esc(r.title)+'</strong> <span class="sub">('+r.count+' apps)</span></div>';
    const acts = document.createElement('div'); acts.className = 'row';
    const shSel = document.createElement('select');
    shSel.style.width = 'auto'; shSel.style.padding = '5px 8px'; shSel.style.fontSize = '12px';
    shSel.innerHTML = '<option value="banner_16_9">16:9</option><option value="square_1_1">1:1</option><option value="poster_2_3">2:3</option>';
    shSel.value = r.tileShape || 'banner_16_9';
    shSel.onchange = async () => { await api('/api/rows/shape', { method:'POST', body:JSON.stringify({ id:r.id, tileShape:shSel.value }) }); toast('Row shape updated'); loadState(); };
    acts.appendChild(shSel);
    const bRen = document.createElement('button'); bRen.className='btn sec sm'; bRen.textContent='Rename';
    bRen.onclick = async () => { const nt = prompt('Rename row:', r.title); if (nt && nt.trim()) { await api('/api/rows/rename', { method:'POST', body:JSON.stringify({ id:r.id, title:nt.trim() }) }); toast('Row renamed'); loadState(); } };
    acts.appendChild(bRen);
    if (idx > 0) { const bUp = document.createElement('button'); bUp.className='btn sec sm'; bUp.textContent='↑'; bUp.onclick = async () => { await api('/api/rows/move', { method:'POST', body:JSON.stringify({ id:r.id, delta:-1 }) }); loadState(); }; acts.appendChild(bUp); }
    if (idx < STATE.rows.length - 1) { const bDn = document.createElement('button'); bDn.className='btn sec sm'; bDn.textContent='↓'; bDn.onclick = async () => { await api('/api/rows/move', { method:'POST', body:JSON.stringify({ id:r.id, delta:1 }) }); loadState(); }; acts.appendChild(bDn); }
    if (STATE.rows.length > 1) { const bDel = document.createElement('button'); bDel.className='btn danger sm'; bDel.textContent='Delete'; bDel.onclick = async () => { if (confirm('Delete row "' + r.title + '"?')) { await api('/api/rows/delete', { method:'POST', body:JSON.stringify({ id:r.id }) }); toast('Row deleted'); loadState(); } }; acts.appendChild(bDel); }
    div.appendChild(acts);
    rowsEl.appendChild(div);
  });

  // Search & Services
  document.getElementById('sShowBar').checked = !!STATE.showSearchBar;
  document.getElementById('sDetailsPopup').checked = !!STATE.searchOpenDetailsPopup;
  document.getElementById('sShowRT').checked = !!STATE.justWatchShowRottenTomatoes;
  document.getElementById('sShowAge').checked = !!STATE.justWatchShowAgeRating;
  const regSel = document.getElementById('optRegion');
  regSel.innerHTML = STATE.regions.map(r => '<option value="'+r.code+'">'+esc(r.label)+'</option>').join('');
  regSel.value = STATE.justWatchRegion || 'GB';
  document.getElementById('spJustWatch').checked = !!STATE.searchProviderJustWatch;
  document.getElementById('spJellyfin').checked = !!STATE.searchProviderJellyfin;
  document.getElementById('spApps').checked = !!STATE.searchProviderApps;
  document.getElementById('spYouTube').checked = !!STATE.searchProviderYouTube;
  document.getElementById('spWeb').checked = !!STATE.searchProviderWeb;
  document.getElementById('sFilterSvc').checked = !!STATE.justWatchFilterServices;
  document.getElementById('sRentBuy').checked = !!STATE.justWatchIncludeRentBuy;
  document.getElementById('sHideUnmatched').checked = !!STATE.justWatchHideUnmatchedTitles;

  const svcEl = document.getElementById('streamingServicesList');
  const grouped = {};
  STATE.streamingServices.forEach(sv => { (grouped[sv.category] = grouped[sv.category] || []).push(sv); });
  svcEl.innerHTML = Object.keys(grouped).map(cat =>
    '<label>' + esc(cat) + '</label><div class="grid3">' +
    grouped[cat].map(sv => '<label class="chip-check"><input type="checkbox" class="jw-svc-cb" data-id="'+esc(sv.id)+'" '+(sv.enabled?'checked':'')+' onchange="saveSearchOpts()" /> '+esc(sv.label)+'</label>').join('') +
    '</div>'
  ).join('');

  // Wallpapers & Apps
  const thSel = document.getElementById('wpTheme');
  thSel.innerHTML = STATE.themes.map(t => '<option value="'+t.id+'">'+esc(t.name)+'</option>').join('');
  thSel.value = STATE.themeId;
  const acSel = document.getElementById('wpAccent');
  acSel.innerHTML = STATE.accents.map(a => '<option value="'+a.id+'">'+esc(a.name)+'</option>').join('');
  acSel.value = STATE.accentId;
  const prSel = document.getElementById('wpProvider');
  prSel.innerHTML = STATE.wallpaperProviders.map(p => '<option value="'+p.id+'">'+esc(p.label)+(p.configured?' ✓':' (Not connected)')+'</option>').join('');
  prSel.value = STATE.wallpaperProvider;
  document.getElementById('wpSlideshow').checked = !!STATE.wallpaperSlideshowEnabled;
  document.getElementById('wpKenBurns').checked = !!STATE.wallpaperKenBurns;
  document.getElementById('wpShowTitle').checked = !!STATE.wallpaperShowTitle;
  document.getElementById('wpInterval').value = pickNearest(STATE.wallpaperIntervalSeconds, ['15','30','60','120','300']);
  document.getElementById('wpDim').value = pickNearest(STATE.wallpaperDimAlpha, ['0.25','0.45','0.65','0.80']);
  document.getElementById('wpLimit').value = pickNearest(STATE.wallpaperBackdropLimit, ['20','40','80','150','300']);
  document.getElementById('wpMediaType').value = STATE.wallpaperMediaType || 'both';
  document.getElementById('wpRedditSort').value = STATE.redditSort || 'top_month';
  document.getElementById('clearWpBtn').classList.toggle('hidden', !STATE.hasCustomWallpaper);

  document.getElementById('redditCatsGrid').innerHTML = STATE.redditCategories.map(c =>
    '<label class="chip-check"><input type="checkbox" class="rd-cat-cb" data-id="'+esc(c.id)+'" '+(c.enabled?'checked':'')+' onchange="saveWallpaperOpts()" /> '+esc(c.label)+'</label>'
  ).join('');
  document.getElementById('tmdbCatsGrid').innerHTML = STATE.tmdbCategories.map(c =>
    '<label class="chip-check"><input type="checkbox" class="tm-cat-cb" data-id="'+esc(c.id)+'" '+(c.enabled?'checked':'')+' onchange="saveWallpaperOpts()" /> '+esc(c.label)+'</label>'
  ).join('');

  const appSel = document.getElementById('iconAppSelect');
  const prevApp = appSel.value;
  appSel.innerHTML = STATE.apps.map(a => '<option value="'+esc(a.packageName)+'">'+esc(a.label)+(a.hasCustomIcon?' ★':'')+'</option>').join('');
  if (prevApp && STATE.apps.some(a => a.packageName === prevApp)) appSel.value = prevApp;
  syncSelectedAppInputs();

  // Weather & Screensaver
  document.getElementById('weatherLocBadge').textContent = STATE.weatherLocationName || 'Not set';
  document.getElementById('wEnabled').checked = !!STATE.weatherEnabled;
  document.getElementById('wShowForecast').checked = !!STATE.weatherShowForecast;
  document.getElementById('wBackdrop').checked = !!STATE.weatherShowBackdrop;
  document.getElementById('wUnit').value = STATE.weatherUseFahrenheit ? 'F' : 'C';
  document.getElementById('wDays').value = pickNearest(STATE.weatherDays, ['1','3','5','7']);
  document.getElementById('wSize').value = STATE.weatherForecastSize || 'medium';
  document.getElementById('wIconStyle').value = STATE.weatherIconStyle || 'color_vivid';
  document.getElementById('ssEnabled').checked = !!STATE.screensaverEnabled;
  document.getElementById('ssMode').value = STATE.screensaverMode || 'artwork';
  document.getElementById('ssTimeout').value = pickNearest(STATE.screensaverTimeoutMinutes, ['1','2','5','10','15','30']);
  document.getElementById('ssInterval').value = pickNearest(STATE.screensaverIntervalSeconds, ['10','20','30','60']);
  document.getElementById('ssDim').value = pickNearest(STATE.screensaverDimAlpha, ['0.0','0.25','0.45','0.65']);
  document.getElementById('ssKenBurns').checked = !!STATE.screensaverKenBurns;
  document.getElementById('ssClock').checked = !!STATE.screensaverShowClock;
  document.getElementById('ssWeather').checked = !!STATE.screensaverShowWeather;
  document.getElementById('ssTitle').checked = !!STATE.screensaverShowTitle;
  document.getElementById('ssBurnIn').checked = !!STATE.screensaverBurnInShift;
  document.getElementById('ssAmbientCycle').checked = !!STATE.screensaverAmbientCycle;

  // Security & System
  document.getElementById('pinStatusBadge').textContent = STATE.isPinEnabled ? 'PIN Active' : 'Off';
  document.getElementById('clearPinBtn').classList.toggle('hidden', !STATE.isPinEnabled);
  document.getElementById('pinProtectSettings').checked = !!STATE.pinProtectSettings;
  document.getElementById('optUpdateSchedule').value = STATE.updateCheckSchedule || 'weekly';

  const pinEl = document.getElementById('pinAppsList');
  pinEl.innerHTML = '';
  STATE.apps.forEach(a => {
    const div = document.createElement('div'); div.className = 'item';
    div.innerHTML = '<div class="app-meta">' + appIconHtml(a.packageName, a.label, true) + '<div><strong>' + esc(a.label) + '</strong></div></div>';
    const b = document.createElement('button');
    b.className = a.pinLocked ? 'btn danger sm' : 'btn sec sm';
    b.textContent = a.pinLocked ? '🔒 Locked' : 'Unlocked';
    b.onclick = async () => { await api('/api/apps/action', { method:'POST', body:JSON.stringify({ action:'toggle_pin', packageName:a.packageName }) }); toast(a.pinLocked ? 'Unlocked ' + a.label : 'PIN-locked ' + a.label); loadState(); };
    div.appendChild(b);
    pinEl.appendChild(div);
  });

  const hidEl = document.getElementById('hiddenAppsList');
  hidEl.innerHTML = '';
  if (!STATE.hiddenApps || STATE.hiddenApps.length === 0) {
    hidEl.innerHTML = '<div class="sub">No hidden apps.</div>';
  } else {
    STATE.hiddenApps.forEach(h => {
      const div = document.createElement('div'); div.className = 'item';
      div.innerHTML = '<div><strong>' + esc(h.label) + '</strong></div>';
      const b = document.createElement('button'); b.className = 'btn sec sm'; b.textContent = 'Unhide';
      b.onclick = async () => { await api('/api/apps/action', { method:'POST', body:JSON.stringify({ action:'unhide', packageName:h.packageName }) }); toast('Unhid ' + h.label); loadState(); };
      div.appendChild(b);
      hidEl.appendChild(div);
    });
  }
}
async function sortSelectedRow() { if (!SELECTED_ROW_ID) return; await api('/api/rows/apps/sort', { method:'POST', body:JSON.stringify({ rowId: SELECTED_ROW_ID }) }); toast('Sorted tiles A–Z'); loadState(); }
async function cleanupSelectedRow() { if (!SELECTED_ROW_ID) return; await api('/api/rows/apps/cleanup', { method:'POST', body:JSON.stringify({ rowId: SELECTED_ROW_ID }) }); toast('Removed uninstalled apps'); loadState(); }
async function saveServer() {
  await api('/api/servers', { method:'POST', body:JSON.stringify({ provider:document.getElementById('srvProvider').value, url:document.getElementById('srvUrl').value.trim(), token:document.getElementById('srvToken').value.trim() }) });
  toast('Saved & applied to TV'); loadState();
}
async function saveHomeOpts() {
  await api('/api/home', { method:'POST', body:JSON.stringify({
    minimalistDockMode: document.getElementById('optMinimalist').checked,
    lockHomeLayout: document.getElementById('optLockLayout').checked,
    showTileTitles: document.getElementById('optTitles').checked,
    showAddAppTile: document.getElementById('optAddTile').checked,
    homeTileSize: document.getElementById('optTileSize').value,
    tileCornerStyle: document.getElementById('optCorners').value,
    tileFocusStyle: document.getElementById('optFocusStyle').value,
    tileSpacing: document.getElementById('optSpacing').value,
    tileOpacity: parseFloat(document.getElementById('optOpacity').value),
    showClock: document.getElementById('optShowClock').checked,
    use24HourClock: document.getElementById('opt24h').checked,
    showDate: document.getElementById('optShowDate').checked,
    showTvInputsButton: document.getElementById('optTvInputs').checked,
    clockStyle: document.getElementById('optClockStyle').value,
    clockFontStyle: document.getElementById('optClockFont').value,
    clockFontWeight: document.getElementById('optClockWeight').value,
    gridColumns: parseInt(document.getElementById('optGridCols').value, 10),
    showAllAppsSection: document.getElementById('optAllApps').checked,
    showNonLeanbackApps: document.getElementById('optNonLeanback').checked
  }) });
  toast('Home settings updated');
}
async function saveSearchOpts() {
  const services = Array.from(document.querySelectorAll('.jw-svc-cb:checked')).map(cb => cb.dataset.id);
  await api('/api/search', { method:'POST', body:JSON.stringify({
    showSearchBar: document.getElementById('sShowBar').checked,
    searchOpenDetailsPopup: document.getElementById('sDetailsPopup').checked,
    justWatchShowRottenTomatoes: document.getElementById('sShowRT').checked,
    justWatchShowAgeRating: document.getElementById('sShowAge').checked,
    justWatchRegion: document.getElementById('optRegion').value,
    searchProviderJustWatch: document.getElementById('spJustWatch').checked,
    searchProviderJellyfin: document.getElementById('spJellyfin').checked,
    searchProviderApps: document.getElementById('spApps').checked,
    searchProviderYouTube: document.getElementById('spYouTube').checked,
    searchProviderWeb: document.getElementById('spWeb').checked,
    justWatchFilterServices: document.getElementById('sFilterSvc').checked,
    justWatchIncludeRentBuy: document.getElementById('sRentBuy').checked,
    justWatchHideUnmatchedTitles: document.getElementById('sHideUnmatched').checked,
    justWatchServices: services
  }) });
  toast('Search & services updated');
}
async function saveWallpaperTheme() {
  await api('/api/wallpaper', { method:'POST', body:JSON.stringify({ themeId: document.getElementById('wpTheme').value }) });
  toast('Ambient theme applied'); loadState();
}
async function saveWallpaperOpts() {
  const redditSubs = Array.from(document.querySelectorAll('.rd-cat-cb:checked')).map(cb => cb.dataset.id);
  const tmdbCats = Array.from(document.querySelectorAll('.tm-cat-cb:checked')).map(cb => cb.dataset.id);
  await api('/api/wallpaper', { method:'POST', body:JSON.stringify({
    accentId: document.getElementById('wpAccent').value,
    wallpaperProvider: document.getElementById('wpProvider').value,
    wallpaperSlideshowEnabled: document.getElementById('wpSlideshow').checked,
    wallpaperKenBurns: document.getElementById('wpKenBurns').checked,
    wallpaperIntervalSeconds: parseInt(document.getElementById('wpInterval').value, 10),
    wallpaperDimAlpha: parseFloat(document.getElementById('wpDim').value),
    wallpaperBackdropLimit: parseInt(document.getElementById('wpLimit').value, 10),
    wallpaperMediaType: document.getElementById('wpMediaType').value,
    wallpaperShowTitle: document.getElementById('wpShowTitle').checked,
    redditSort: document.getElementById('wpRedditSort').value,
    redditSubreddits: redditSubs,
    tmdbCategories: tmdbCats
  }) });
  toast('Wallpaper settings updated'); loadState();
}
async function nextWallpaper() { await api('/api/wallpaper/next', { method:'POST' }); toast('Loading next backdrop…'); }
async function clearCustomWallpaper() { await api('/api/wallpaper', { method:'POST', body:JSON.stringify({ clearCustomWallpaper: true }) }); toast('Cleared custom wallpaper'); loadState(); }
async function renameSelectedApp() {
  const pkg = document.getElementById('iconAppSelect').value;
  if (!pkg) return;
  await api('/api/apps/action', { method:'POST', body:JSON.stringify({ action:'rename', packageName:pkg, value:document.getElementById('appRenameInput').value }) });
  toast('App name updated'); loadState();
}
async function resetSelectedAppIcon() {
  const pkg = document.getElementById('iconAppSelect').value;
  if (!pkg) return;
  await api('/api/apps/action', { method:'POST', body:JSON.stringify({ action:'reset_icon', packageName:pkg }) });
  toast('Reset to default icon'); loadState();
}
async function hideSelectedApp() {
  const pkg = document.getElementById('iconAppSelect').value;
  if (!pkg) return;
  await api('/api/apps/action', { method:'POST', body:JSON.stringify({ action:'hide', packageName:pkg }) });
  toast('App hidden'); loadState();
}
async function saveScreensaverOpts() {
  await api('/api/screensaver', { method:'POST', body:JSON.stringify({
    screensaverEnabled: document.getElementById('ssEnabled').checked,
    screensaverMode: document.getElementById('ssMode').value,
    screensaverTimeoutMinutes: parseInt(document.getElementById('ssTimeout').value, 10),
    screensaverIntervalSeconds: parseInt(document.getElementById('ssInterval').value, 10),
    screensaverDimAlpha: parseFloat(document.getElementById('ssDim').value),
    screensaverKenBurns: document.getElementById('ssKenBurns').checked,
    screensaverShowClock: document.getElementById('ssClock').checked,
    screensaverShowWeather: document.getElementById('ssWeather').checked,
    screensaverShowTitle: document.getElementById('ssTitle').checked,
    screensaverBurnInShift: document.getElementById('ssBurnIn').checked,
    screensaverAmbientCycle: document.getElementById('ssAmbientCycle').checked
  }) });
  toast('Screensaver settings updated');
}
async function searchWeather() {
  const q = document.getElementById('weatherQuery').value.trim();
  const resEl = document.getElementById('weatherResults');
  if (q.length < 2) return toast('Enter at least 2 characters');
  resEl.innerHTML = '<div class="sub" style="padding:8px 0;">Searching cities…</div>';
  const res = await api('/api/weather/search?q=' + encodeURIComponent(q));
  const data = await res.json();
  resEl.innerHTML = '';
  if (!data.results || data.results.length === 0) { resEl.innerHTML = '<div class="sub" style="padding:8px 0;">No cities found.</div>'; return; }
  data.results.forEach(loc => {
    const div = document.createElement('div'); div.className = 'item';
    div.innerHTML = '<div><strong>' + esc(loc.displayName) + '</strong></div>';
    const bSet = document.createElement('button'); bSet.className='btn sm'; bSet.textContent='Set Location';
    bSet.onclick = async () => { await api('/api/weather/location', { method:'POST', body:JSON.stringify(loc) }); resEl.innerHTML = ''; document.getElementById('weatherQuery').value = ''; toast('Weather set to ' + loc.name); loadState(); };
    div.appendChild(bSet);
    resEl.appendChild(div);
  });
}
async function saveWeatherOpts() {
  await api('/api/weather', { method:'POST', body:JSON.stringify({
    weatherEnabled: document.getElementById('wEnabled').checked,
    weatherShowForecast: document.getElementById('wShowForecast').checked,
    weatherShowBackdrop: document.getElementById('wBackdrop').checked,
    weatherUseFahrenheit: document.getElementById('wUnit').value === 'F',
    weatherDays: parseInt(document.getElementById('wDays').value, 10),
    weatherForecastSize: document.getElementById('wSize').value,
    weatherIconStyle: document.getElementById('wIconStyle').value
  }) });
  toast('Weather settings updated');
}
async function saveParentalPin() {
  const pin = document.getElementById('pinInput').value.trim();
  if (!/^\d{4}$/.test(pin)) return toast('Enter a 4-digit numeric PIN');
  await api('/api/security', { method:'POST', body:JSON.stringify({ parentalPin: pin }) });
  document.getElementById('pinInput').value = '';
  toast('4-digit PIN saved'); loadState();
}
async function clearParentalPin() {
  if (!confirm('Remove Parental Control PIN?')) return;
  await api('/api/security', { method:'POST', body:JSON.stringify({ clearPin: true }) });
  toast('PIN removed'); loadState();
}
async function saveSecurityOpts() {
  await api('/api/security', { method:'POST', body:JSON.stringify({
    pinProtectSettings: document.getElementById('pinProtectSettings').checked,
    updateCheckSchedule: document.getElementById('optUpdateSchedule').value
  }) });
  toast('Security & update settings saved');
}
async function resetLauncherDefaults() {
  if (!confirm('Reset ALL Horizon settings, rows, and themes back to defaults?')) return;
  await api('/api/security', { method:'POST', body:JSON.stringify({ resetToDefaults: true }) });
  toast('Launcher reset to defaults'); loadState();
}
async function addRow() {
  const inp = document.getElementById('newRowTitle');
  if (!inp.value.trim()) return;
  await api('/api/rows/add', { method:'POST', body:JSON.stringify({ title: inp.value.trim() }) });
  inp.value = ''; toast('Row added'); loadState();
}
function downloadBackup() { window.location.href = '/api/backup?code=' + encodeURIComponent(CODE); toast('Downloading backup…'); }
async function uploadRestore() {
  const f = document.getElementById('restoreFile').files[0];
  if (!f) return toast('Choose a .json backup file first');
  const res = await api('/api/restore', { method:'POST', body:await f.text() });
  if (res.ok) { toast('Backup restored on TV!'); loadState(); } else { toast('Invalid backup file'); }
}
async function uploadWallpaper() {
  const f = document.getElementById('wallpaperFile').files[0];
  if (!f) return toast('Choose an image first');
  const res = await api('/api/upload-wallpaper', { method:'POST', body:await f.arrayBuffer() });
  toast(res.ok ? 'Wallpaper applied on TV!' : 'Upload failed');
  if (res.ok) loadState();
}
async function uploadAppIcon() {
  const f = document.getElementById('iconFile').files[0];
  const pkg = document.getElementById('iconAppSelect').value;
  if (!f || !pkg) return toast('Choose an app and image first');
  const res = await api('/api/upload-icon?pkg=' + encodeURIComponent(pkg), { method:'POST', body:await f.arrayBuffer() });
  toast(res.ok ? 'Custom icon applied!' : 'Upload failed');
  if (res.ok) loadState();
}
if (CODE) loadState(); else { document.getElementById('authCard').classList.remove('hidden'); document.getElementById('statusBadge').textContent = 'Enter Code'; }
</script>
</body>
</html>
""".trimIndent()
  }
}
