package com.horizontv.launcher

import android.service.dreams.DreamService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.horizontv.launcher.data.AndroidScreensaverPhotoSource
import com.horizontv.launcher.data.JellyfinClient
import com.horizontv.launcher.data.JellyfinDataSource
import com.horizontv.launcher.data.LauncherPreferences
import com.horizontv.launcher.data.PlexClient
import com.horizontv.launcher.data.RedditWallpaperClient
import com.horizontv.launcher.data.TmdbWallpaperClient
import com.horizontv.launcher.data.WeatherClient
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JellyfinSlide
import com.horizontv.launcher.model.LauncherSettings
import com.horizontv.launcher.model.MediaWallpaperProvider
import com.horizontv.launcher.model.ScreensaverMode
import com.horizontv.launcher.model.ScreensaverPhotoSlide
import com.horizontv.launcher.model.WeatherData
import com.horizontv.launcher.ui.components.ScreensaverOverlay
import com.horizontv.launcher.ui.theme.HorizonTVLauncherTheme
import com.horizontv.launcher.ui.theme.LauncherPalettes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Android TV System Screensaver (Daydream) service.
 * Select "Horizon Screensaver" in Android TV Settings → Device Preferences → Screen saver.
 */
class HorizonDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

  private val lifecycleRegistry = LifecycleRegistry(this)
  private val savedStateRegistryController = SavedStateRegistryController.create(this)
  private var dreamScope: CoroutineScope? = null

  private var settingsState by mutableStateOf(LauncherSettings())
  private var artworkSlide by mutableStateOf<JellyfinSlide?>(null)
  private var photoSlide by mutableStateOf<ScreensaverPhotoSlide?>(null)
  private var weatherData by mutableStateOf<WeatherData?>(null)

  override val lifecycle: Lifecycle
    get() = lifecycleRegistry

  override val savedStateRegistry: SavedStateRegistry
    get() = savedStateRegistryController.savedStateRegistry

  override fun onCreate() {
    super.onCreate()
    savedStateRegistryController.performRestore(null)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    isInteractive = false
    isFullscreen = true
    isScreenBright = true

    settingsState = LauncherPreferences(applicationContext).load()

    val composeView = ComposeView(this).apply {
      setViewTreeLifecycleOwner(this@HorizonDreamService)
      setViewTreeSavedStateRegistryOwner(this@HorizonDreamService)
      setContent {
        val currentSettings = settingsState
        val theme = LauncherPalettes.themeById(currentSettings.themeId)
        val accent = LauncherPalettes.accentById(currentSettings.accentId)
        HorizonTVLauncherTheme(wallpaperTheme = theme, accentPreset = accent) {
          ScreensaverOverlay(
            settings = currentSettings,
            theme = theme,
            accent = accent,
            artworkSlide = artworkSlide,
            photoSlide = photoSlide,
            weatherData = weatherData,
            onDismiss = { finish() },
          )
        }
      }
    }

    window?.decorView?.let { decor ->
      decor.setViewTreeLifecycleOwner(this)
      decor.setViewTreeSavedStateRegistryOwner(this)
    }
    setContentView(composeView)
  }

  override fun onDreamingStarted() {
    super.onDreamingStarted()
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    dreamScope = scope
    val s = settingsState

    // Load weather if enabled
    if (s.screensaverShowWeather && s.isWeatherConfigured) {
      val lat = s.weatherLatitude
      val lon = s.weatherLongitude
      if (lat != null && lon != null) {
        scope.launch {
          WeatherClient().fetchForecast(
            locationName = s.weatherLocationName,
            latitude = lat,
            longitude = lon,
            useFahrenheit = s.weatherUseFahrenheit,
          ).onSuccess { weatherData = it }
        }
      }
    }

    // Start content rotation according to selected screensaver mode
    val intervalMs = (s.screensaverIntervalSeconds.coerceIn(5, 300) * 1_000L)
    when (s.activeScreensaverMode) {
      ScreensaverMode.WALLPAPER_ARTWORK -> {
        scope.launch { runArtworkLoop(s, intervalMs) }
      }
      ScreensaverMode.PHOTOS -> {
        scope.launch { runPhotoLoop(s, intervalMs) }
      }
      ScreensaverMode.AMBIENT -> Unit
    }
  }

  private suspend fun runArtworkLoop(s: LauncherSettings, intervalMs: Long) {
    val provider = s.activeWallpaperProvider
    if (!s.isProviderConfigured(provider)) return
    val client: JellyfinDataSource = when (provider) {
      MediaWallpaperProvider.JELLYFIN -> JellyfinClient()
      MediaWallpaperProvider.EMBY -> JellyfinClient()
      MediaWallpaperProvider.PLEX -> PlexClient()
      MediaWallpaperProvider.TMDB -> TmdbWallpaperClient()
      MediaWallpaperProvider.REDDIT -> RedditWallpaperClient()
    }
    val providerCfg = s.configFor(provider)
    val backdrops = client.fetchBackdrops(
      serverUrl = s.serverUrlFor(provider),
      token = s.tokenFor(provider),
      userId = s.userIdFor(provider),
      mediaType = JellyfinMediaType.fromId(providerCfg.mediaType),
      libraryIds = s.libraryIdsFor(provider),
      limit = providerCfg.backdropLimit,
    ).getOrDefault(emptyList()).shuffled()
    if (backdrops.isEmpty()) return

    val token = s.tokenFor(provider)
    var index = 0
    while (dreamScope?.isActive == true) {
      val item = backdrops[index % backdrops.size]
      index++
      val img = client.downloadBackdropImage(imageUrl = item.imageUrl, token = token)
      if (img != null) {
        val logo = if (s.screensaverShowTitle && !item.logoUrl.isNullOrBlank()) {
          client.downloadLogoImage(logoUrl = item.logoUrl, token = token)
        } else {
          null
        }
        artworkSlide = JellyfinSlide(backdrop = item, image = img, logoImage = logo)
        delay(intervalMs)
      } else {
        delay(500L)
      }
    }
  }

  private suspend fun runPhotoLoop(s: LauncherSettings, intervalMs: Long) {
    val loader = AndroidScreensaverPhotoSource(applicationContext)
    val entries = loader.listPhotos(s.screensaverPhotoFolderUri, s.screensaverPhotoUris).shuffled()
    if (entries.isEmpty()) return
    var index = 0
    while (dreamScope?.isActive == true) {
      val entry = entries[index % entries.size]
      index++
      val slide = loader.loadPhoto(entry)
      if (slide != null) {
        photoSlide = slide
        delay(intervalMs)
      } else {
        delay(500L)
      }
    }
  }

  override fun onDreamingStopped() {
    super.onDreamingStopped()
    dreamScope?.cancel()
    dreamScope = null
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
  }

  override fun onDestroy() {
    dreamScope?.cancel()
    dreamScope = null
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    super.onDestroy()
  }
}
