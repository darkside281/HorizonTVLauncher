package com.horizontv.launcher.ui

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.horizontv.launcher.data.InstalledAppsDataSource
import com.horizontv.launcher.data.JellyfinAuthResult
import com.horizontv.launcher.data.JellyfinDataSource
import com.horizontv.launcher.data.SettingsBackupSerializer
import com.horizontv.launcher.data.SettingsStore
import com.horizontv.launcher.data.StreamingSearchDataSource
import com.horizontv.launcher.data.WeatherDataSource
import com.horizontv.launcher.model.AppFilter
import com.horizontv.launcher.model.AppInfo
import com.horizontv.launcher.model.DailyForecast
import com.horizontv.launcher.model.ExternalSearchProvider
import com.horizontv.launcher.model.JellyfinBackdrop
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JellyfinSearchItem
import com.horizontv.launcher.model.LauncherSettings
import com.horizontv.launcher.model.MediaWallpaperProvider
import com.horizontv.launcher.model.NetworkType
import com.horizontv.launcher.model.SearchProviderFilter
import com.horizontv.launcher.model.StreamingOffer
import com.horizontv.launcher.model.StreamingSearchItem
import com.horizontv.launcher.model.WeatherCondition
import com.horizontv.launcher.model.WeatherData
import com.horizontv.launcher.model.WeatherIconStyle
import com.horizontv.launcher.model.WeatherLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun sampleApps(): List<AppInfo> = listOf(
    AppInfo("com.tv.youtube", ".Main", "YouTube", isTvApp = true, isSystemApp = true, icon = null, banner = null, tintColor = Color.Red),
    AppInfo("com.tv.plex", ".Main", "Plex", isTvApp = true, isSystemApp = false, icon = null, banner = null, tintColor = Color.Yellow),
    AppInfo("com.tv.kodi", ".Main", "Kodi", isTvApp = true, isSystemApp = false, icon = null, banner = null, tintColor = Color.Blue),
    AppInfo("com.mobile.browser", ".Main", "Side Browser", isTvApp = false, isSystemApp = false, icon = null, banner = null, tintColor = Color.Green),
  )

  private class FakeAppsDataSource(private val apps: List<AppInfo>) : InstalledAppsDataSource {
    var deletedPaths = mutableListOf<String>()
    var openedJellyfinItemId: String? = null
    var searchedYouTubeQuery: String? = null
    var searchedWebQuery: String? = null
    override suspend fun loadInstalledApps(customIconPaths: Map<String, String>): List<AppInfo> =
      apps.map { it.copy(hasCustomIcon = it.packageName in customIconPaths) }
    override suspend fun saveCustomIcon(packageName: String, sourceUri: Uri): String =
      "/fake/custom_icons/$packageName.png"
    override suspend fun deleteCustomIcon(path: String?) {
      if (path != null) deletedPaths.add(path)
    }
    override fun packageChanges(): Flow<Unit> = emptyFlow()
    override fun networkAvailable(): Flow<Unit> = emptyFlow()
    override fun getNetworkType(): NetworkType = NetworkType.ETHERNET
    override fun launchApp(app: AppInfo): Boolean = true
    override fun openAppInfo(packageName: String): Boolean = true
    override fun uninstallApp(packageName: String): Boolean = true
    override fun openWifiSettings(): Boolean = true
    override fun openBluetoothSettings(): Boolean = true
    override fun openSystemSettings(): Boolean = true
    override fun openHomeSettings(): Boolean = true
    override fun openPlayStore(): Boolean = true
    override fun isDefaultHomeLauncher(): Boolean = false
    override fun openJellyfinItem(serverUrl: String, itemId: String): Boolean {
      openedJellyfinItemId = itemId
      return true
    }
    override fun openYouTubeSearch(query: String): Boolean {
      searchedYouTubeQuery = query
      return true
    }
    override fun openWebSearch(query: String): Boolean {
      searchedWebQuery = query
      return true
    }
    override fun getInstalledSearchProviders(): List<ExternalSearchProvider> = listOf(
      ExternalSearchProvider("netflix", "Netflix", "com.netflix.ninja"),
    )
    override fun openExternalProviderSearch(provider: ExternalSearchProvider, query: String): Boolean = true
    override fun openStreamingOffer(offer: StreamingOffer): Boolean = true
  }

  private class FakeStreamingSearchDataSource : StreamingSearchDataSource {
    override suspend fun searchTitles(
      query: String,
      countryCode: String,
      limit: Int,
    ): Result<List<StreamingSearchItem>> = Result.success(
      listOf(
        StreamingSearchItem(
          id = "ts38796",
          title = "Stranger Things",
          objectType = "SHOW",
          year = 2016,
          imdbScore = 8.6f,
          offers = listOf(
            StreamingOffer(
              providerName = "Netflix",
              shortName = "nfx",
              monetizationType = "FLATRATE",
              deeplinkUrl = "intent://www.netflix.com/watch/80057281#Intent;scheme=https;package=com.netflix.ninja;end",
              webUrl = "https://www.netflix.com/title/80057281",
            ),
          ),
        ),
      ),
    )

    override suspend fun downloadPoster(url: String): ImageBitmap? = null
  }

  private class FakeSettingsStore(initial: LauncherSettings = LauncherSettings()) : SettingsStore {
    var saved: LauncherSettings = initial
    override fun load(): LauncherSettings = saved
    override fun save(settings: LauncherSettings) {
      saved = settings
    }
  }

  private class FakeJellyfinDataSource : JellyfinDataSource {
    var lastMediaType: JellyfinMediaType = JellyfinMediaType.BOTH
    var lastLibraryIds: Set<String> = emptySet()
    var lastLimit: Int = 40
    private val sampleLibraries = listOf(
      JellyfinLibrary("lib_movies", "Movies", "movies"),
      JellyfinLibrary("lib_tv", "TV Shows", "tvshows"),
    )
    private val sampleBackdrops = listOf(
      JellyfinBackdrop("id1", "Interstellar", 2014, "http://jf/1"),
      JellyfinBackdrop("id2", "Dune: Part Two", 2024, "http://jf/2"),
    )
    override suspend fun authenticateAndFetch(
      rawServerUrl: String,
      apiKey: String?,
      username: String?,
      password: String?,
      mediaType: JellyfinMediaType,
      libraryIds: Set<String>,
      limit: Int,
    ): Result<JellyfinAuthResult> {
      lastMediaType = mediaType
      lastLibraryIds = libraryIds
      lastLimit = limit
      return Result.success(
        JellyfinAuthResult(
          serverUrl = "http://192.168.1.50:8096",
          token = apiKey ?: "tok123",
          userId = "user1",
          libraries = sampleLibraries,
          backdrops = sampleBackdrops,
        ),
      )
    }

    override suspend fun fetchLibraries(
      serverUrl: String,
      token: String,
      userId: String,
    ): Result<List<JellyfinLibrary>> = Result.success(sampleLibraries)

    override suspend fun fetchBackdrops(
      serverUrl: String,
      token: String,
      userId: String,
      mediaType: JellyfinMediaType,
      libraryIds: Set<String>,
      limit: Int,
    ): Result<List<JellyfinBackdrop>> {
      lastMediaType = mediaType
      lastLibraryIds = libraryIds
      lastLimit = limit
      return Result.success(sampleBackdrops)
    }

    override suspend fun downloadBackdropImage(imageUrl: String, token: String): ImageBitmap? = null
    override suspend fun downloadLogoImage(logoUrl: String, token: String): ImageBitmap? = null
    override suspend fun searchItems(
      serverUrl: String,
      token: String,
      userId: String,
      query: String,
      limit: Int,
    ): Result<List<JellyfinSearchItem>> = Result.success(
      listOf(
        JellyfinSearchItem("item_dune", "Dune: Part Two", "Movie", 2024, 8.5f),
        JellyfinSearchItem("item_moon", "Moon Knight", "Series", 2022, 7.6f),
      ).filter { it.title.contains(query, ignoreCase = true) },
    )
    override suspend fun downloadThumbnail(imageUrl: String, token: String): ImageBitmap? = null
  }

  private class FakeWeatherDataSource : WeatherDataSource {
    override suspend fun searchLocations(query: String): Result<List<WeatherLocation>> = Result.success(
      listOf(WeatherLocation("London", "England", "United Kingdom", 51.5072, -0.1276)),
    )

    override suspend fun fetchForecast(
      locationName: String,
      latitude: Double,
      longitude: Double,
      useFahrenheit: Boolean,
    ): Result<WeatherData> = Result.success(
      WeatherData(
        locationName = locationName,
        currentTemp = if (useFahrenheit) 57 else 14,
        condition = WeatherCondition.PARTLY_CLOUDY,
        unitSymbol = if (useFahrenheit) "°F" else "°C",
        daily = (0 until 7).map { i ->
          DailyForecast("D$i", WeatherCondition.CLEAR, 18, 10)
        },
      ),
    )
  }

  @Test
  fun seedsPrimaryRowOnFirstLaunchAndReorders() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val vm = LauncherViewModel(FakeAppsDataSource(sampleApps()), store)
    advanceUntilIdle()

    val state = vm.uiState.value
    assertFalse(state.isLoading)
    val favRowId = state.resolvedRows.first().id
    assertEquals(listOf("com.tv.youtube", "com.tv.plex", "com.tv.kodi"), state.resolvedRows.first().apps.map { it.packageName })

    vm.moveAppInRow(favRowId, "com.tv.plex", -1)
    assertEquals(listOf("com.tv.plex", "com.tv.youtube", "com.tv.kodi"), vm.uiState.value.resolvedRows.first().apps.map { it.packageName })

    vm.moveAppInRow(favRowId, "com.tv.plex", +1)
    vm.moveAppInRow(favRowId, "com.tv.plex", +1)
    assertEquals(listOf("com.tv.youtube", "com.tv.kodi", "com.tv.plex"), vm.uiState.value.resolvedRows.first().apps.map { it.packageName })
    assertEquals(listOf("com.tv.youtube", "com.tv.kodi", "com.tv.plex"), store.saved.primaryRow.packageNames)
  }

  @Test
  fun createsRenamesMovesAndDeletesExtraRows() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val vm = LauncherViewModel(FakeAppsDataSource(sampleApps()), store)
    advanceUntilIdle()

    val primaryId = vm.uiState.value.settings.rows.first().id
    vm.createRow("Streaming", openAppPickerAfter = false)
    assertEquals(2, vm.uiState.value.settings.rows.size)
    val streamingId = vm.uiState.value.settings.rows[1].id
    assertEquals("Streaming", vm.uiState.value.settings.rows[1].title)

    // Add apps to the new row & rename it
    vm.toggleAppInRow(streamingId, "com.tv.plex")
    vm.toggleAppInRow(streamingId, "com.tv.kodi")
    vm.renameRow(streamingId, "Movies & TV")
    assertEquals("Movies & TV", vm.uiState.value.resolvedRows[1].title)
    assertEquals(listOf("com.tv.plex", "com.tv.kodi"), vm.uiState.value.resolvedRows[1].apps.map { it.packageName })

    // Move extra row up to become the hero row
    vm.moveRow(streamingId, -1)
    assertEquals(listOf(streamingId, primaryId), vm.uiState.value.settings.rows.map { it.id })

    // Delete extra row, and verify last remaining row cannot be deleted
    vm.deleteRow(streamingId)
    assertEquals(listOf(primaryId), vm.uiState.value.settings.rows.map { it.id })
    vm.deleteRow(primaryId)
    assertEquals(1, vm.uiState.value.settings.rows.size)
  }

  @Test
  fun hideAndUnhideAppUpdatesVisibleAndHiddenLists() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val vm = LauncherViewModel(FakeAppsDataSource(sampleApps()), store)
    advanceUntilIdle()

    vm.hideApp("com.tv.plex")
    assertTrue(vm.uiState.value.filteredApps.none { it.packageName == "com.tv.plex" })
    assertTrue(vm.uiState.value.resolvedRows.first().apps.none { it.packageName == "com.tv.plex" })
    assertEquals(listOf("com.tv.plex"), vm.uiState.value.hiddenApps.map { it.packageName })

    vm.unhideApp("com.tv.plex")
    assertTrue(vm.uiState.value.filteredApps.any { it.packageName == "com.tv.plex" })
    assertTrue(vm.uiState.value.hiddenApps.isEmpty())
  }

  @Test
  fun renamesIndividualAppsAndResets() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val vm = LauncherViewModel(FakeAppsDataSource(sampleApps()), store)
    advanceUntilIdle()

    vm.renameApp("com.tv.plex", "Movies")
    assertEquals("Movies", vm.uiState.value.resolvedRows.first().apps.first { it.packageName == "com.tv.plex" }.label)
    assertEquals("Movies", store.saved.customLabels["com.tv.plex"])

    vm.renameApp("com.tv.plex", "")
    assertEquals("Plex", vm.uiState.value.resolvedRows.first().apps.first { it.packageName == "com.tv.plex" }.label)
    assertNull(store.saved.customLabels["com.tv.plex"])
  }

  @Test
  fun filtersByCategoryAndSearchQuery() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val vm = LauncherViewModel(
      appDataSource = FakeAppsDataSource(sampleApps()),
      settingsStore = store,
      streamingSearchDataSource = FakeStreamingSearchDataSource(),
    )
    advanceUntilIdle()

    vm.setFilter(AppFilter.OTHER_APPS)
    assertEquals(listOf("com.mobile.browser"), vm.uiState.value.filteredApps.map { it.packageName })

    vm.setFilter(AppFilter.ALL)
    vm.updateSearchQuery("ko")
    advanceUntilIdle()
    assertEquals(listOf("com.tv.kodi"), vm.uiState.value.filteredApps.map { it.packageName })
  }

  @Test
  fun updatesThemeWallpaperAndLayoutPreferences() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val vm = LauncherViewModel(FakeAppsDataSource(sampleApps()), store)
    advanceUntilIdle()

    vm.setCustomWallpaper("content://media/wallpaper.jpg")
    assertEquals("content://media/wallpaper.jpg", store.saved.customWallpaperUri)

    vm.selectTheme("aurora_borealis")
    assertEquals("aurora_borealis", store.saved.themeId)
    assertNull(store.saved.customWallpaperUri)

    vm.selectAccent("emerald_mint")
    vm.toggle24HourClock()
    vm.setGridColumns(6)
    assertEquals("emerald_mint", store.saved.accentId)
    assertTrue(store.saved.use24HourClock)
    assertEquals(6, store.saved.gridColumns)
  }

  @Test
  fun connectsAndConfiguresJellyfinSlideshow() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val jf = FakeJellyfinDataSource()
    val vm = LauncherViewModel(FakeAppsDataSource(sampleApps()), store, jf)
    advanceUntilIdle()

    vm.connectJellyfin("192.168.1.50:8096", apiKey = "secret_key", username = null, password = null)
    testDispatcher.scheduler.runCurrent()

    assertTrue(store.saved.jellyfinEnabled)
    assertEquals("http://192.168.1.50:8096", store.saved.jellyfinServerUrl)
    assertEquals("secret_key", store.saved.jellyfinApiKey)
    assertEquals(2, vm.uiState.value.jellyfinBackdropCount)
    assertEquals(2, vm.uiState.value.jellyfinLibraries.size)

    vm.setJellyfinMediaType(JellyfinMediaType.MOVIES.id)
    vm.setJellyfinBackdropLimit(150)
    testDispatcher.scheduler.runCurrent()
    assertEquals(JellyfinMediaType.MOVIES, jf.lastMediaType)
    assertEquals(150, jf.lastLimit)

    val tvLib = JellyfinLibrary("lib_tv", "TV Shows", "tvshows")
    val movieLib = JellyfinLibrary("lib_movies", "Movies", "movies")

    // Multi-select: add TV Shows, then add Movies -> both selected
    vm.selectJellyfinLibrary(tvLib)
    testDispatcher.scheduler.runCurrent()
    assertEquals(setOf("lib_tv"), jf.lastLibraryIds)

    vm.selectJellyfinLibrary(movieLib)
    testDispatcher.scheduler.runCurrent()
    assertEquals(setOf("lib_tv", "lib_movies"), jf.lastLibraryIds)

    // Toggling TV Shows again removes only TV Shows
    vm.selectJellyfinLibrary(tvLib)
    testDispatcher.scheduler.runCurrent()
    assertEquals(setOf("lib_movies"), jf.lastLibraryIds)

    // Selecting All Libraries (null) clears selection back to empty set
    vm.selectJellyfinLibrary(null)
    testDispatcher.scheduler.runCurrent()
    assertEquals(emptySet<String>(), jf.lastLibraryIds)

    vm.setJellyfinInterval(60)
    assertEquals(60, store.saved.jellyfinIntervalSeconds)

    vm.disconnectJellyfin()
    assertFalse(store.saved.jellyfinEnabled)
    assertEquals("", store.saved.jellyfinServerUrl)
  }

  @Test
  fun exportsAndRestoresLauncherConfigurationJson() = runTest(testDispatcher) {
    val snapshots = mutableMapOf<String, LauncherSettings>()
    val fakeSerializer = object : SettingsBackupSerializer {
      override fun encodeToJson(settings: LauncherSettings): String {
        val key = "snapshot_${snapshots.size}"
        snapshots[key] = settings
        return key
      }

      override fun decodeFromJson(rawJson: String, existing: LauncherSettings): Result<LauncherSettings> {
        val saved = snapshots[rawJson] ?: return Result.failure(IllegalArgumentException("Bad backup"))
        return Result.success(saved)
      }
    }

    val store = FakeSettingsStore()
    val vm = LauncherViewModel(
      FakeAppsDataSource(sampleApps()),
      store,
      FakeJellyfinDataSource(),
      FakeWeatherDataSource(),
      fakeSerializer,
    )
    advanceUntilIdle()

    vm.createRow("Kids", openAppPickerAfter = false)
    vm.selectAccent("coral_rose")
    vm.setJellyfinBackdropLimit(80)
    vm.setWeatherDays(3)

    val backupJson = vm.exportBackupJson()

    val freshStore = FakeSettingsStore()
    val restoredVm = LauncherViewModel(
      FakeAppsDataSource(sampleApps()),
      freshStore,
      FakeJellyfinDataSource(),
      FakeWeatherDataSource(),
      fakeSerializer,
    )
    advanceUntilIdle()

    restoredVm.restoreBackupJson(backupJson)
    testDispatcher.scheduler.runCurrent()

    assertEquals(2, freshStore.saved.rows.size)
    assertEquals("Kids", freshStore.saved.rows[1].title)
    assertEquals("coral_rose", freshStore.saved.accentId)
    assertEquals(80, freshStore.saved.jellyfinBackdropLimit)
    assertEquals(3, freshStore.saved.weatherDays)
  }

  @Test
  fun configuresWeatherLocationDaysUnitsAndIconStyle() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val vm = LauncherViewModel(
      FakeAppsDataSource(sampleApps()),
      store,
      FakeJellyfinDataSource(),
      FakeWeatherDataSource(),
    )
    advanceUntilIdle()

    val london = WeatherLocation("London", "England", "United Kingdom", 51.5072, -0.1276)
    vm.selectWeatherLocation(london)
    testDispatcher.scheduler.runCurrent()

    assertTrue(store.saved.weatherEnabled)
    assertEquals("London, United Kingdom", store.saved.weatherLocationName)
    assertEquals(14, vm.uiState.value.weatherData?.currentTemp)

    vm.setWeatherDays(7)
    vm.setWeatherIconStyle(WeatherIconStyle.MINIMAL_LINE.id)
    vm.toggleWeatherUnit()
    testDispatcher.scheduler.runCurrent()

    assertEquals(7, store.saved.weatherDays)
    assertEquals(WeatherIconStyle.MINIMAL_LINE.id, store.saved.weatherIconStyle)
    assertTrue(store.saved.weatherUseFahrenheit)
    assertEquals(57, vm.uiState.value.weatherData?.currentTemp)

    // Cleanly stop weather loop so runTest completes immediately
    vm.toggleWeatherEnabled()
    assertFalse(store.saved.weatherEnabled)
  }

  @Test
  fun togglesHomeSectionsAndSearchesJellyfinMedia() = runTest(testDispatcher) {
    val apps = FakeAppsDataSource(sampleApps())
    val store = FakeSettingsStore(
      LauncherSettings(
        jellyfinEnabled = false,
        jellyfinServerUrl = "http://192.168.1.50:8096",
        jellyfinApiKey = "secret_key",
        jellyfinUserId = "user1",
      ),
    )
    val vm = LauncherViewModel(
      appDataSource = apps,
      settingsStore = store,
      jellyfinDataSource = FakeJellyfinDataSource(),
      weatherDataSource = FakeWeatherDataSource(),
      streamingSearchDataSource = FakeStreamingSearchDataSource(),
    )
    advanceUntilIdle()

    // Toggle Search Bar, All Apps Section & "+ Add App" tile visibility
    assertTrue(store.saved.showSearchBar)
    assertTrue(store.saved.showAllAppsSection)
    assertTrue(store.saved.showAddAppTile)
    vm.toggleShowSearchBar()
    vm.toggleShowAllAppsSection()
    vm.toggleShowAddAppTile()
    assertFalse(store.saved.showSearchBar)
    assertFalse(store.saved.showAllAppsSection)
    assertFalse(store.saved.showAddAppTile)
    vm.toggleShowSearchBar()
    assertTrue(store.saved.showSearchBar)

    // Toggle individual search providers (including installed streaming apps)
    assertTrue(store.saved.isSearchProviderEnabled("netflix"))
    vm.toggleSearchProvider("netflix")
    assertFalse(store.saved.isSearchProviderEnabled("netflix"))
    vm.toggleSearchProvider("netflix")
    assertTrue(store.saved.isSearchProviderEnabled("netflix"))

    // Universal search: Apps + Jellyfin live search + deep-link
    vm.toggleSearch()
    vm.setSearchProvider(SearchProviderFilter.JELLYFIN)
    vm.updateSearchQuery("Dune")
    advanceUntilIdle()

    assertEquals(1, vm.uiState.value.jellyfinSearchResults.size)
    val match = vm.uiState.value.jellyfinSearchResults.first()
    assertEquals("Dune: Part Two", match.title)

    vm.openJellyfinSearchItem(match)
    assertEquals("item_dune", apps.openedJellyfinItemId)
    assertFalse(vm.uiState.value.isSearchVisible)
  }

  @Test
  fun switchesBetweenJellyfinAndPlexWallpaperProviders() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val jf = FakeJellyfinDataSource()
    val plex = FakeJellyfinDataSource()
    val vm = LauncherViewModel(
      appDataSource = FakeAppsDataSource(sampleApps()),
      settingsStore = store,
      jellyfinDataSource = jf,
      plexDataSource = plex,
    )
    advanceUntilIdle()

    // Connect Plex as wallpaper provider and multi-select libraries
    vm.selectWallpaperProvider(MediaWallpaperProvider.PLEX)
    assertEquals(MediaWallpaperProvider.PLEX.id, store.saved.wallpaperProvider)

    vm.connectJellyfin(
      serverUrl = "192.168.1.99:32400",
      apiKey = "plex_token_123",
      username = null,
      password = null,
      provider = MediaWallpaperProvider.PLEX,
    )
    testDispatcher.scheduler.runCurrent()

    assertTrue(store.saved.jellyfinEnabled)
    assertEquals("plex_token_123", store.saved.plexToken)
    assertEquals(MediaWallpaperProvider.PLEX, store.saved.activeWallpaperProvider)

    vm.selectJellyfinLibrary(JellyfinLibrary("1", "Movies", "movies"))
    vm.selectJellyfinLibrary(JellyfinLibrary("2", "TV Shows", "tvshows"))
    testDispatcher.scheduler.runCurrent()
    assertEquals(setOf("1", "2"), store.saved.plexLibraryIds)
    assertEquals(setOf("1", "2"), plex.lastLibraryIds)

    // Disconnecting Plex cleanly clears Plex state
    vm.disconnectJellyfin(MediaWallpaperProvider.PLEX)
    assertEquals("", store.saved.plexServerUrl)
    assertEquals("", store.saved.plexToken)
    assertFalse(store.saved.jellyfinEnabled)
  }

  @Test
  fun configuresRedditWallpaperProviderSubredditsAndSort() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val reddit = FakeJellyfinDataSource()
    val vm = LauncherViewModel(
      appDataSource = FakeAppsDataSource(sampleApps()),
      settingsStore = store,
      redditDataSource = reddit,
    )
    advanceUntilIdle()

    vm.selectWallpaperProvider(MediaWallpaperProvider.REDDIT)
    testDispatcher.scheduler.runCurrent()

    assertEquals(MediaWallpaperProvider.REDDIT, store.saved.activeWallpaperProvider)
    assertTrue(store.saved.jellyfinEnabled)

    vm.addCustomSubreddit("r/ArchitecturePorn/")
    testDispatcher.scheduler.runCurrent()
    assertTrue("ArchitecturePorn" in store.saved.redditCustomSubreddits)
    assertTrue("ArchitecturePorn" in store.saved.redditSubreddits)
    assertTrue("ArchitecturePorn" in reddit.lastLibraryIds)

    vm.setRedditSort("hot")
    assertEquals("hot", store.saved.redditSort)

    // Cleanly stop slideshow loop so runTest completes immediately
    vm.toggleJellyfinSlideshow()
    assertFalse(store.saved.jellyfinEnabled)
  }

  @Test
  fun enforcesHomeLayoutLockAndTogglesClockDateAndForecastOptions() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val vm = LauncherViewModel(FakeAppsDataSource(sampleApps()), store)
    advanceUntilIdle()

    val primaryRowId = vm.uiState.value.settings.rows.first().id
    val originalOrder = vm.uiState.value.settings.rows.first().packageNames

    // Lock home layout -> reordering & adding/removing blocked
    vm.toggleLockHomeLayout()
    assertTrue(store.saved.lockHomeLayout)
    vm.moveAppInRow(primaryRowId, originalOrder[1], -1)
    assertEquals(originalOrder, store.saved.rows.first().packageNames)
    vm.startReordering(primaryRowId, originalOrder[0])
    assertNull(vm.uiState.value.reorderingTarget)

    // Unlock -> reordering works again
    vm.toggleLockHomeLayout()
    assertFalse(store.saved.lockHomeLayout)
    vm.moveAppInRow(primaryRowId, originalOrder[1], -1)
    assertEquals(originalOrder[1], store.saved.rows.first().packageNames.first())

    // Clock, Date, Tile size, and Weather forecast days / size toggles
    vm.toggleShowClock()
    vm.toggleShowDate()
    vm.toggleWeatherShowForecast()
    vm.setWeatherForecastSize("large")
    vm.setHomeTileSize("large")
    assertFalse(store.saved.showClock)
    assertFalse(store.saved.showDate)
    assertFalse(store.saved.weatherShowForecast)
    assertEquals("large", store.saved.weatherForecastSize)
    assertEquals("large", store.saved.homeTileSize)

    vm.setHomeTileSize("small")
    assertEquals("small", store.saved.homeTileSize)
    vm.setHomeTileSize("invalid_size")
    assertEquals("medium", store.saved.homeTileSize)

    // Tile Order dialog & alphabetical sorting
    vm.openTileOrderDialog(primaryRowId)
    assertEquals(primaryRowId, vm.uiState.value.orderingRowId)
    assertFalse(vm.uiState.value.isSettingsOpen)
    vm.sortRowAppsAlphabetically(primaryRowId)
    val sortedLabels = vm.uiState.value.resolvedRows.first().apps.map { it.label }
    assertEquals(sortedLabels.sortedWith(String.CASE_INSENSITIVE_ORDER), sortedLabels)
    vm.closeTileOrderDialog()
    assertNull(vm.uiState.value.orderingRowId)
  }

  @Test
  fun searchesJustWatchStreamingOffersAndOpensOffer() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val vm = LauncherViewModel(
      appDataSource = FakeAppsDataSource(sampleApps()),
      settingsStore = store,
      streamingSearchDataSource = FakeStreamingSearchDataSource(),
    )
    advanceUntilIdle()

    vm.toggleSearch()
    vm.updateSearchQuery("Stranger")
    advanceUntilIdle()

    val results = vm.uiState.value.streamingSearchResults
    assertEquals(1, results.size)
    assertEquals("Stranger Things", results.first().title)
    assertEquals("Netflix", results.first().offers.first().providerName)

    vm.openStreamingOffer(results.first().offers.first())
    assertFalse(vm.uiState.value.isSearchVisible)
  }

  @Test
  fun configuresTmdbWallpaperProviderAndCategories() = runTest(testDispatcher) {
    val store = FakeSettingsStore()
    val tmdb = FakeJellyfinDataSource()
    val vm = LauncherViewModel(
      appDataSource = FakeAppsDataSource(sampleApps()),
      settingsStore = store,
      tmdbDataSource = tmdb,
      streamingSearchDataSource = FakeStreamingSearchDataSource(),
    )
    advanceUntilIdle()

    vm.selectWallpaperProvider(MediaWallpaperProvider.TMDB)
    vm.connectJellyfin(
      serverUrl = "https://api.themoviedb.org/3",
      apiKey = "tmdb_test_key_123",
      username = null,
      password = null,
      provider = MediaWallpaperProvider.TMDB,
    )
    testDispatcher.scheduler.runCurrent()

    assertEquals(MediaWallpaperProvider.TMDB, store.saved.activeWallpaperProvider)
    assertEquals("tmdb_test_key_123", store.saved.tmdbApiKey)
    assertTrue(store.saved.jellyfinEnabled)

    vm.selectJellyfinLibrary(JellyfinLibrary("top_rated_tv", "Top Rated TV Shows", "tv"))
    testDispatcher.scheduler.runCurrent()
    assertTrue("top_rated_tv" in store.saved.tmdbCategories)

    vm.disconnectJellyfin(MediaWallpaperProvider.TMDB)
    assertEquals("", store.saved.tmdbApiKey)
    assertFalse(store.saved.jellyfinEnabled)
  }
}
