package com.horizontv.launcher.data

import android.app.SearchManager
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.horizontv.launcher.model.AppInfo
import com.horizontv.launcher.model.ExternalSearchProvider
import com.horizontv.launcher.model.NetworkType
import com.horizontv.launcher.model.StreamingOffer
import com.horizontv.launcher.model.TvInputSource
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

interface InstalledAppsDataSource {
  suspend fun loadInstalledApps(customIconPaths: Map<String, String> = emptyMap()): List<AppInfo>
  suspend fun saveCustomIcon(packageName: String, sourceUri: Uri): String?
  suspend fun deleteCustomIcon(path: String?)
  fun saveCustomIconBytes(packageName: String, bytes: ByteArray): String? = null
  fun saveCustomWallpaperBytes(bytes: ByteArray): String? = null
  fun packageChanges(): Flow<Unit>
  fun networkAvailable(): Flow<Unit>
  fun getNetworkType(): NetworkType
  fun launchApp(app: AppInfo): Boolean
  fun openAppInfo(packageName: String): Boolean
  fun uninstallApp(packageName: String): Boolean
  fun openWifiSettings(): Boolean
  fun openBluetoothSettings(): Boolean
  fun openSystemSettings(): Boolean
  fun openHomeSettings(): Boolean
  fun openPlayStore(): Boolean
  fun isDefaultHomeLauncher(): Boolean
  fun openJellyfinItem(serverUrl: String, itemId: String): Boolean
  fun openYouTubeSearch(query: String): Boolean
  fun openWebSearch(query: String): Boolean
  fun getInstalledSearchProviders(): List<ExternalSearchProvider>
  fun openExternalProviderSearch(provider: ExternalSearchProvider, query: String): Boolean
  fun openStreamingOffer(offer: StreamingOffer): Boolean
  fun loadTvInputs(): List<TvInputSource> = emptyList()
  fun openTvInput(input: TvInputSource): Boolean = false
}

class AppRepository(private val context: Context) : InstalledAppsDataSource {
  private val pm: PackageManager = context.packageManager

  override suspend fun loadInstalledApps(
    customIconPaths: Map<String, String>,
  ): List<AppInfo> = withContext(Dispatchers.IO) {
    val selfPackage = context.packageName
    val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    }
    val standardIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val leanbackInfos = queryActivities(leanbackIntent)
    val standardInfos = queryActivities(standardIntent)

    val merged = LinkedHashMap<String, Pair<ResolveInfo, Boolean>>()
    for (info in leanbackInfos) {
      val pkg = info.activityInfo?.packageName ?: continue
      if (pkg == selfPackage) continue
      merged[pkg] = info to true
    }
    for (info in standardInfos) {
      val pkg = info.activityInfo?.packageName ?: continue
      if (pkg == selfPackage) continue
      if (!merged.containsKey(pkg)) {
        merged[pkg] = info to false
      }
    }

    merged.values.mapNotNull { (resolveInfo, isTvApp) ->
      val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
      val packageName = activityInfo.packageName
      val activityName = activityInfo.name
      val originalLabel = resolveInfo.loadLabel(pm).toString().trim()
        .ifEmpty { packageName }

      val appFlags = activityInfo.applicationInfo?.flags ?: 0
      val isSystemApp = (appFlags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
        (appFlags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

      val customPath = customIconPaths[packageName]
      val customBitmap = if (!customPath.isNullOrBlank()) {
        runCatching {
          val file = File(customPath)
          if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }.getOrNull()
      } else {
        null
      }

      val iconBitmap: Bitmap?
      val bannerBitmap: Bitmap?
      val hasCustomIcon: Boolean

      if (customBitmap != null) {
        hasCustomIcon = true
        val aspect = customBitmap.width.toFloat() / customBitmap.height.coerceAtLeast(1)
        if (aspect >= 1.35f) {
          // Wide custom banner artwork fills the 16:9 card edge-to-edge
          bannerBitmap = customBitmap
          iconBitmap = customBitmap
        } else {
          // Square/portrait custom logo renders crisply centered on the tinted glass card
          bannerBitmap = null
          iconBitmap = customBitmap
        }
      } else {
        hasCustomIcon = false
        iconBitmap = runCatching {
          resolveInfo.loadIcon(pm)?.toBoundedBitmap(128, 128)
        }.getOrNull()

        val bannerDrawable = runCatching {
          activityInfo.loadBanner(pm)
            ?: activityInfo.applicationInfo?.loadBanner(pm)
            ?: pm.getApplicationBanner(packageName)
        }.getOrNull()

        bannerBitmap = runCatching {
          bannerDrawable?.toBoundedBitmap(320, 180)
        }.getOrNull()
      }

      val tintColor = iconBitmap?.extractAccentColor() ?: Color(0xFF334155)

      AppInfo(
        packageName = packageName,
        activityName = activityName,
        label = originalLabel,
        originalLabel = originalLabel,
        isTvApp = isTvApp,
        isSystemApp = isSystemApp,
        icon = iconBitmap?.asImageBitmap(),
        banner = bannerBitmap?.asImageBitmap(),
        tintColor = tintColor,
        hasCustomIcon = hasCustomIcon,
      )
    }.sortedBy { it.label.lowercase() }
  }

  override suspend fun saveCustomIcon(
    packageName: String,
    sourceUri: Uri,
  ): String? = withContext(Dispatchers.IO) {
    runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(sourceUri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
      }
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

      var sampleSize = 1
      while ((bounds.outWidth / sampleSize) > 960 || (bounds.outHeight / sampleSize) > 960) {
        sampleSize *= 2
      }

      val decoded = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
          inSampleSize = sampleSize
        })
      } ?: return@runCatching null

      val maxSide = 480f
      val scale = minOf(maxSide / decoded.width, maxSide / decoded.height, 1f)
      val finalBitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(
          decoded,
          (decoded.width * scale).toInt().coerceAtLeast(1),
          (decoded.height * scale).toInt().coerceAtLeast(1),
          true,
        ).also { if (it !== decoded) decoded.recycle() }
      } else {
        decoded
      }

      val dir = File(context.filesDir, "custom_icons").apply { mkdirs() }
      val safeName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
      val outFile = File(dir, "$safeName.png")
      FileOutputStream(outFile).use { fos ->
        finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
      }
      finalBitmap.recycle()
      outFile.absolutePath
    }.getOrNull()
  }

  override suspend fun deleteCustomIcon(path: String?) = withContext(Dispatchers.IO) {
    if (!path.isNullOrBlank()) {
      runCatching { File(path).delete() }
    }
  }

  override fun saveCustomIconBytes(packageName: String, bytes: ByteArray): String? = runCatching {
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
    val maxSide = 512
    val finalBitmap = if (decoded.width > maxSide || decoded.height > maxSide) {
      val scale = minOf(maxSide.toFloat() / decoded.width, maxSide.toFloat() / decoded.height)
      Bitmap.createScaledBitmap(
        decoded,
        (decoded.width * scale).toInt().coerceAtLeast(1),
        (decoded.height * scale).toInt().coerceAtLeast(1),
        true,
      ).also { if (it !== decoded) decoded.recycle() }
    } else {
      decoded
    }
    val dir = File(context.filesDir, "custom_icons").apply { mkdirs() }
    val safeName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val outFile = File(dir, "$safeName.png")
    FileOutputStream(outFile).use { fos ->
      finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
    }
    finalBitmap.recycle()
    outFile.absolutePath
  }.getOrNull()

  override fun saveCustomWallpaperBytes(bytes: ByteArray): String? = runCatching {
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
    val maxW = 1920
    val maxH = 1080
    val finalBitmap = if (decoded.width > maxW || decoded.height > maxH) {
      val scale = minOf(maxW.toFloat() / decoded.width, maxH.toFloat() / decoded.height)
      Bitmap.createScaledBitmap(
        decoded,
        (decoded.width * scale).toInt().coerceAtLeast(1),
        (decoded.height * scale).toInt().coerceAtLeast(1),
        true,
      ).also { if (it !== decoded) decoded.recycle() }
    } else {
      decoded
    }
    val dir = File(context.filesDir, "custom_wallpapers").apply { mkdirs() }
    val outFile = File(dir, "companion_${System.currentTimeMillis()}.jpg")
    FileOutputStream(outFile).use { fos ->
      finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
    }
    finalBitmap.recycle()
    Uri.fromFile(outFile).toString()
  }.getOrNull()

  override fun packageChanges(): Flow<Unit> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        trySend(Unit)
      }
    }
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_PACKAGE_ADDED)
      addAction(Intent.ACTION_PACKAGE_REMOVED)
      addAction(Intent.ACTION_PACKAGE_REPLACED)
      addAction(Intent.ACTION_PACKAGE_CHANGED)
      addDataScheme("package")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      context.registerReceiver(receiver, filter)
    }
    awaitClose {
      runCatching { context.unregisterReceiver(receiver) }
    }
  }

  override fun networkAvailable(): Flow<Unit> = callbackFlow {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (cm == null) {
      awaitClose {}
      return@callbackFlow
    }
    var hadNetwork = false
    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        hadNetwork = true
        trySend(Unit)
      }

      override fun onLost(network: Network) {
        hadNetwork = false
      }

      override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        if (!hadNetwork && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
          hadNetwork = true
          trySend(Unit)
        }
      }
    }
    val registered = runCatching {
      cm.registerDefaultNetworkCallback(callback)
      true
    }.getOrDefault(false)
    awaitClose {
      if (registered) {
        runCatching { cm.unregisterNetworkCallback(callback) }
      }
    }
  }

  override fun getNetworkType(): NetworkType {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return NetworkType.NONE
    val active = cm.activeNetwork ?: return NetworkType.NONE
    val caps = cm.getNetworkCapabilities(active) ?: return NetworkType.NONE
    return when {
      caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
      else -> NetworkType.NONE
    }
  }

  override fun launchApp(app: AppInfo): Boolean {
    val leanbackIntent = pm.getLeanbackLaunchIntentForPackage(app.packageName)
    val standardIntent = pm.getLaunchIntentForPackage(app.packageName)
    val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
      component = ComponentName(app.packageName, app.activityName)
      addCategory(if (app.isTvApp) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
    }

    val candidates = listOfNotNull(
      if (app.isTvApp) leanbackIntent else null,
      standardIntent,
      leanbackIntent,
      explicitIntent,
    )
    for (intent in candidates) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      if (startActivitySafe(intent)) return true
    }
    return false
  }

  override fun openAppInfo(packageName: String): Boolean {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return startActivitySafe(intent)
  }

  override fun uninstallApp(packageName: String): Boolean {
    val intent = Intent(Intent.ACTION_DELETE).apply {
      data = Uri.parse("package:$packageName")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return startActivitySafe(intent)
  }

  override fun openWifiSettings(): Boolean {
    return startActivitySafe(Intent(Settings.ACTION_WIFI_SETTINGS)) ||
      startActivitySafe(Intent(Settings.ACTION_WIRELESS_SETTINGS)) ||
      openSystemSettings()
  }

  override fun openBluetoothSettings(): Boolean {
    return startActivitySafe(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) ||
      openSystemSettings()
  }

  override fun openSystemSettings(): Boolean {
    return startActivitySafe(Intent(Settings.ACTION_SETTINGS))
  }

  override fun openHomeSettings(): Boolean {
    return startActivitySafe(Intent(Settings.ACTION_HOME_SETTINGS)) ||
      startActivitySafe(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) ||
      openSystemSettings()
  }

  override fun openPlayStore(): Boolean {
    val vendingPkg = "com.android.vending"
    val launch = pm.getLeanbackLaunchIntentForPackage(vendingPkg)
      ?: pm.getLaunchIntentForPackage(vendingPkg)
    if (launch != null && startActivitySafe(launch)) return true
    return startActivitySafe(Intent(Intent.ACTION_VIEW, Uri.parse("market://home"))) ||
      startActivitySafe(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store")))
  }

  override fun isDefaultHomeLauncher(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = context.getSystemService(RoleManager::class.java)
      if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
      }
    }
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.resolveActivity(homeIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    } else {
      @Suppress("DEPRECATION")
      pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    return resolved?.activityInfo?.packageName == context.packageName
  }

  override fun openJellyfinItem(serverUrl: String, itemId: String): Boolean {
    val cleanId = itemId.trim()
    val uuid = formatJellyfinUuid(cleanId)
    // 1. Deep-link directly into Jellyfin for Android TV's StartupActivity with ItemId extra
    val tvIntent = Intent(Intent.ACTION_VIEW).apply {
      component = ComponentName(
        "org.jellyfin.androidtv",
        "org.jellyfin.androidtv.ui.startup.StartupActivity",
      )
      putExtra("ItemId", uuid)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    if (startActivitySafe(tvIntent)) return true

    // 2. Fallback: open Jellyfin item details URL (works in Jellyfin Mobile or Browser)
    val base = JellyfinClient.normalizeUrl(serverUrl)
    if (base.isNotEmpty()) {
      val detailsUri = Uri.parse("$base/web/index.html#!/details?id=$cleanId")
      if (startActivitySafe(Intent(Intent.ACTION_VIEW, detailsUri))) return true
    }

    // 3. Final fallback: launch Jellyfin Android TV home if installed
    val fallbackLaunch = pm.getLeanbackLaunchIntentForPackage("org.jellyfin.androidtv")
      ?: pm.getLaunchIntentForPackage("org.jellyfin.androidtv")
      ?: pm.getLaunchIntentForPackage("org.jellyfin.mobile")
    return fallbackLaunch != null && startActivitySafe(fallbackLaunch)
  }

  override fun openYouTubeSearch(query: String): Boolean {
    val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
    val uri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")
    val ytPackages = listOf(
      "com.google.android.youtube.tv",
      "com.liskovsoft.smarttubetv.beta",
      "com.teamsmart.videomanager.tv",
      "com.google.android.youtube",
    )
    for (pkg in ytPackages) {
      val targeted = Intent(Intent.ACTION_VIEW, uri).apply { setPackage(pkg) }
      if (startActivitySafe(targeted)) return true
    }
    return startActivitySafe(Intent(Intent.ACTION_VIEW, uri))
  }

  override fun openWebSearch(query: String): Boolean {
    val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
    val uri = Uri.parse("https://www.google.com/search?q=$encoded")
    return startActivitySafe(Intent(Intent.ACTION_VIEW, uri))
  }

  override fun getInstalledSearchProviders(): List<ExternalSearchProvider> {
    val known = listOf(
      Triple("netflix", "Netflix", listOf("com.netflix.ninja", "com.netflix.mediaclient")),
      Triple("prime", "Prime Video", listOf("com.amazon.amazonvideo.livingroom", "com.amazon.amazonvideo.livingroom.nvidia", "com.amazon.avod.thirdpartyclient")),
      Triple("disney", "Disney+", listOf("com.disney.disneyplus")),
      Triple("plex", "Plex", listOf("com.plexapp.android")),
      Triple("crunchyroll", "Crunchyroll", listOf("com.crunchyroll.crunchyroid")),
      Triple("appletv", "Apple TV", listOf("com.apple.atve.androidtv.appletv")),
      Triple("max", "Max", listOf("com.wbd.stream", "com.hbo.hbonow")),
      Triple("hulu", "Hulu", listOf("com.hulu.livingroomplus", "com.hulu.plus")),
      Triple("spotify", "Spotify", listOf("com.spotify.tv.android", "com.spotify.music")),
    )
    return buildList {
      for ((id, label, candidates) in known) {
        val installedPkg = candidates.firstOrNull { isPackageInstalled(it) }
        if (installedPkg != null) {
          add(ExternalSearchProvider(id = id, label = label, packageName = installedPkg))
        }
      }
    }
  }

  override fun openExternalProviderSearch(provider: ExternalSearchProvider, query: String): Boolean {
    val cleaned = query.trim()
    val encoded = java.net.URLEncoder.encode(cleaned, "UTF-8")

    // 1. Try native ACTION_SEARCH targeted directly to the installed app
    val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
      setPackage(provider.packageName)
      putExtra(SearchManager.QUERY, cleaned)
      putExtra("query", cleaned)
    }
    if (startActivitySafe(searchIntent)) return true

    // 2. Try known web/deep-link search URL targeted to the installed app
    val deepUrl = when (provider.id) {
      "netflix" -> "https://www.netflix.com/search?q=$encoded"
      "prime" -> "https://www.primevideo.com/search/ref=atv_nb_sr?phrase=$encoded"
      "disney" -> "https://www.disneyplus.com/search?q=$encoded"
      "spotify" -> "spotify:search:$encoded"
      else -> null
    }
    if (deepUrl != null) {
      val targetedView = Intent(Intent.ACTION_VIEW, Uri.parse(deepUrl)).apply {
        setPackage(provider.packageName)
      }
      if (startActivitySafe(targetedView)) return true
      // Fallback: open the deep link via Android's resolver
      if (startActivitySafe(Intent(Intent.ACTION_VIEW, Uri.parse(deepUrl)))) return true
    }

    // 3. Final fallback: launch the app directly
    val launch = pm.getLeanbackLaunchIntentForPackage(provider.packageName)
      ?: pm.getLaunchIntentForPackage(provider.packageName)
    return launch != null && startActivitySafe(launch)
  }

  override fun openStreamingOffer(offer: StreamingOffer): Boolean {
    val deep = offer.deeplinkUrl?.trim().orEmpty()
    var targetPackage: String? = null
    if (deep.isNotEmpty()) {
      if (deep.startsWith("intent:", ignoreCase = true)) {
        val parsed = runCatching { Intent.parseUri(deep, Intent.URI_INTENT_SCHEME) }.getOrNull()
        if (parsed != null) {
          targetPackage = parsed.`package` ?: parsed.component?.packageName
          if (startActivitySafe(parsed)) return true
          // If JustWatch specified a stale/renamed Activity component class, retry with only the package & data URI
          if (parsed.component != null) {
            val withoutComponent = Intent(parsed).apply {
              component = null
              if (`package`.isNullOrBlank() && !targetPackage.isNullOrBlank()) {
                setPackage(targetPackage)
              }
            }
            if (startActivitySafe(withoutComponent)) return true
          }
          // Next fallback: launch the target streaming app itself if installed
          if (!targetPackage.isNullOrBlank()) {
            val appLaunch = pm.getLeanbackLaunchIntentForPackage(targetPackage)
              ?: pm.getLaunchIntentForPackage(targetPackage)
            if (appLaunch != null && startActivitySafe(appLaunch)) return true
          }
        }
      } else {
        if (startActivitySafe(Intent(Intent.ACTION_VIEW, Uri.parse(deep)))) return true
      }
    }
    val web = offer.webUrl?.trim().orEmpty()
    if (web.isNotEmpty()) {
      if (startActivitySafe(Intent(Intent.ACTION_VIEW, Uri.parse(web)))) return true
    }
    val fallbackPkg = targetPackage ?: knownPackageForProvider(offer.shortName, offer.providerName)
    if (!fallbackPkg.isNullOrBlank()) {
      val appLaunch = pm.getLeanbackLaunchIntentForPackage(fallbackPkg)
        ?: pm.getLaunchIntentForPackage(fallbackPkg)
      if (appLaunch != null && startActivitySafe(appLaunch)) return true
    }
    return false
  }

  private fun knownPackageForProvider(shortName: String, providerName: String): String? {
    val s = shortName.trim().lowercase()
    val name = providerName.trim().lowercase()
    val candidates = when {
      s == "cru" || name == "crunchyroll" -> listOf("com.crunchyroll.crunchyroid")
      s == "nfx" || s == "nfa" || name == "netflix" -> listOf("com.netflix.ninja", "com.netflix.mediaclient")
      s == "prv" || s == "pva" || s == "amz" || name.contains("prime") || name.contains("amazon") ->
        listOf("com.amazon.amazonvideo.livingroom", "com.amazon.amazonvideo.livingroom.nvidia", "com.amazon.avod.thirdpartyclient")
      s == "dnp" || name.contains("disney") -> listOf("com.disney.disneyplus")
      s == "pmp" || s == "ppp" || s == "pba" || name == "paramount+" -> listOf("com.cbs.ca", "com.cbs.app")
      s == "itv" || s == "itp" || name == "itvx" -> listOf("air.ITVMobilePlayer", "com.itv.tenft.itvhub")
      s == "atp" || s == "itu" || name.contains("apple tv") -> listOf("com.apple.atve.androidtv.appletv")
      s == "bbc" || name.contains("iplayer") -> listOf("uk.co.bbc.iplayer")
      s == "al4" || s == "a4p" || name.contains("channel 4") -> listOf("com.channel4.ondemand")
      s == "myf" || name == "my5" -> listOf("com.mobileiq.demand5")
      else -> emptyList()
    }
    return candidates.firstOrNull { isPackageInstalled(it) }
  }

  override fun loadTvInputs(): List<TvInputSource> {
    val manager = runCatching {
      context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
    }.getOrNull() ?: return emptyList()

    val rawList = runCatching { manager.tvInputList }.getOrNull().orEmpty()
    return rawList.mapNotNull { info ->
      val hidden = runCatching { info.isHidden(context) }.getOrDefault(false)
      if (hidden) return@mapNotNull null

      val isPassthrough = info.isPassthroughInput
      val type = info.type
      // Show hardware HDMI/AV/Component/Composite/VGA/DisplayPort/DVI/SCART passthrough inputs & hardware tuners
      val isHardwareType = type == TvInputInfo.TYPE_HDMI ||
        type == TvInputInfo.TYPE_COMPONENT ||
        type == TvInputInfo.TYPE_COMPOSITE ||
        type == TvInputInfo.TYPE_DISPLAY_PORT ||
        type == TvInputInfo.TYPE_DVI ||
        type == TvInputInfo.TYPE_VGA ||
        type == TvInputInfo.TYPE_SCART ||
        type == TvInputInfo.TYPE_SVIDEO ||
        isPassthrough
      if (!isHardwareType) return@mapNotNull null

      val customLabel = runCatching { info.loadCustomLabel(context)?.toString()?.trim() }.getOrNull()
      val defaultLabel = runCatching { info.loadLabel(context)?.toString()?.trim() }.getOrNull()
      val label = customLabel?.ifEmpty { null }
        ?: defaultLabel?.ifEmpty { null }
        ?: return@mapNotNull null

      val (typeLabel, order) = when (type) {
        TvInputInfo.TYPE_HDMI -> "HDMI" to 0
        TvInputInfo.TYPE_TUNER -> "Tuner" to 1
        TvInputInfo.TYPE_DISPLAY_PORT -> "DisplayPort" to 2
        TvInputInfo.TYPE_COMPONENT -> "Component" to 3
        TvInputInfo.TYPE_COMPOSITE -> "AV / Composite" to 4
        TvInputInfo.TYPE_VGA -> "VGA" to 5
        TvInputInfo.TYPE_DVI -> "DVI" to 6
        TvInputInfo.TYPE_SCART -> "SCART" to 7
        TvInputInfo.TYPE_SVIDEO -> "S-Video" to 8
        else -> "Input" to 9
      }

      val state = runCatching { manager.getInputState(info.id) }.getOrDefault(TvInputManager.INPUT_STATE_DISCONNECTED)
      val connected = state == TvInputManager.INPUT_STATE_CONNECTED

      TvInputSource(
        id = info.id,
        label = label,
        typeLabel = typeLabel,
        isPassthrough = isPassthrough,
        isConnected = connected,
        sortOrder = order,
      )
    }.sortedWith(compareBy<TvInputSource> { it.sortOrder }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label })
  }

  override fun openTvInput(input: TvInputSource): Boolean {
    val primaryUri = if (input.isPassthrough) {
      TvContract.buildChannelUriForPassthroughInput(input.id)
    } else {
      TvContract.buildChannelsUriForInput(input.id)
    }
    if (startActivitySafe(Intent(Intent.ACTION_VIEW, primaryUri))) return true
    val fallbackUri = TvContract.buildChannelsUriForInput(input.id)
    if (fallbackUri != primaryUri && startActivitySafe(Intent(Intent.ACTION_VIEW, fallbackUri))) return true
    return false
  }

  private fun isPackageInstalled(packageName: String): Boolean {
    return runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
      } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(packageName, 0)
      }
      true
    }.getOrDefault(false)
  }

  private fun formatJellyfinUuid(rawId: String): String {
    val hex = rawId.replace("-", "")
    if (hex.length != 32) return rawId
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
  }

  private fun queryActivities(intent: Intent): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
      @Suppress("DEPRECATION")
      pm.queryIntentActivities(intent, 0)
    }
  }

  private fun startActivitySafe(intent: Intent): Boolean {
    return runCatching {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      true
    }.getOrDefault(false)
  }
}

private fun Drawable.toBoundedBitmap(maxWidth: Int, maxHeight: Int): Bitmap {
  if (this is BitmapDrawable && bitmap != null && !bitmap.isRecycled) {
    val bw = bitmap.width.coerceAtLeast(1)
    val bh = bitmap.height.coerceAtLeast(1)
    if (bw <= maxWidth && bh <= maxHeight) {
      return bitmap
    }
    val scale = minOf(maxWidth.toFloat() / bw, maxHeight.toFloat() / bh)
    return Bitmap.createScaledBitmap(
      bitmap,
      (bw * scale).toInt().coerceAtLeast(1),
      (bh * scale).toInt().coerceAtLeast(1),
      true,
    )
  }

  val intrinsicW = intrinsicWidth.takeIf { it > 0 } ?: maxWidth
  val intrinsicH = intrinsicHeight.takeIf { it > 0 } ?: maxHeight
  val scale = minOf(maxWidth.toFloat() / intrinsicW, maxHeight.toFloat() / intrinsicH, 1f)
  val targetW = (intrinsicW * scale).toInt().coerceAtLeast(1)
  val targetH = (intrinsicH * scale).toInt().coerceAtLeast(1)

  val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  setBounds(0, 0, canvas.width, canvas.height)
  draw(canvas)
  return bitmap
}

private fun Bitmap.extractAccentColor(): Color {
  val sample = Bitmap.createScaledBitmap(this, 16, 16, true)
  var rSum = 0L
  var gSum = 0L
  var bSum = 0L
  var weightSum = 0L
  val hsv = FloatArray(3)

  for (y in 0 until sample.height) {
    for (x in 0 until sample.width) {
      val pixel = sample.getPixel(x, y)
      val alpha = (pixel ushr 24) and 0xFF
      if (alpha < 140) continue
      val r = (pixel ushr 16) and 0xFF
      val g = (pixel ushr 8) and 0xFF
      val b = pixel and 0xFF
      android.graphics.Color.RGBToHSV(r, g, b, hsv)
      val satWeight = (hsv[1] * 4f).toInt().coerceAtLeast(1)
      val valWeight = if (hsv[2] in 0.15f..0.95f) 2 else 1
      val weight = (satWeight * valWeight).toLong()
      rSum += r * weight
      gSum += g * weight
      bSum += b * weight
      weightSum += weight
    }
  }
  if (sample !== this) sample.recycle()

  if (weightSum == 0L) return Color(0xFF334155)
  return Color(
    red = (rSum / weightSum).toInt().coerceIn(0, 255),
    green = (gSum / weightSum).toInt().coerceIn(0, 255),
    blue = (bSum / weightSum).toInt().coerceIn(0, 255),
  )
}
