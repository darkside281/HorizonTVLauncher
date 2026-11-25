package com.horizontv.launcher.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.horizontv.launcher.data.AppUpdateDataSource
import com.horizontv.launcher.data.GitHubUpdater
import com.horizontv.launcher.data.InstalledAppsDataSource
import com.horizontv.launcher.data.JellyfinClient
import com.horizontv.launcher.data.JellyfinDataSource
import com.horizontv.launcher.data.JustWatchClient
import com.horizontv.launcher.data.LocalWebCompanionServer
import com.horizontv.launcher.data.PhotoEntry
import com.horizontv.launcher.data.PlexClient
import com.horizontv.launcher.data.RedditWallpaperClient
import com.horizontv.launcher.data.ScreensaverPhotoSource
import com.horizontv.launcher.data.SettingsBackupCodec
import com.horizontv.launcher.data.SettingsBackupSerializer
import com.horizontv.launcher.data.SettingsStore
import com.horizontv.launcher.data.StreamingSearchDataSource
import com.horizontv.launcher.data.TmdbWallpaperClient
import com.horizontv.launcher.data.WeatherClient
import com.horizontv.launcher.data.WeatherDataSource
import com.horizontv.launcher.data.WebCompanionBridge
import com.horizontv.launcher.model.AppFilter
import com.horizontv.launcher.model.AppInfo
import com.horizontv.launcher.model.AppMenuTarget
import com.horizontv.launcher.model.AppRow
import com.horizontv.launcher.model.ClockFontStyle
import com.horizontv.launcher.model.ClockFontWeight
import com.horizontv.launcher.model.ClockStyle
import com.horizontv.launcher.model.ExternalSearchProvider
import com.horizontv.launcher.model.HomeTileSize
import com.horizontv.launcher.model.HomeTileSpacing
import com.horizontv.launcher.model.TileAspectShape
import com.horizontv.launcher.model.TileCornerStyle
import com.horizontv.launcher.model.TileFocusStyle
import com.horizontv.launcher.model.JellyfinBackdrop
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JellyfinSearchItem
import com.horizontv.launcher.model.JellyfinSlide
import com.horizontv.launcher.model.JustWatchRegion
import com.horizontv.launcher.model.JustWatchServicePreset
import com.horizontv.launcher.model.LauncherSettings
import com.horizontv.launcher.model.LauncherUiState
import com.horizontv.launcher.model.MediaWallpaperProvider
import com.horizontv.launcher.model.PinPendingAction
import com.horizontv.launcher.model.PinPromptMode
import com.horizontv.launcher.model.PinPromptState
import com.horizontv.launcher.model.RedditSort
import com.horizontv.launcher.model.ReorderTarget
import com.horizontv.launcher.model.ScreensaverMode
import com.horizontv.launcher.model.SearchProviderFilter
import com.horizontv.launcher.model.StreamingOffer
import com.horizontv.launcher.model.TvInputSource
import com.horizontv.launcher.model.UpdateCheckSchedule
import com.horizontv.launcher.model.WeatherForecastSize
import com.horizontv.launcher.model.WeatherLocation
import com.horizontv.launcher.model.WebCompanionInfo
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LauncherViewModel(
  private val appDataSource: InstalledAppsDataSource,
  private val settingsStore: SettingsStore,
  private val jellyfinDataSource: JellyfinDataSource = JellyfinClient(),
  private val weatherDataSource: WeatherDataSource = WeatherClient(),
  private val backupSerializer: SettingsBackupSerializer = SettingsBackupCodec,
  private val plexDataSource: JellyfinDataSource = PlexClient(),
  private val redditDataSource: JellyfinDataSource = RedditWallpaperClient(),
  private val tmdbDataSource: JellyfinDataSource = TmdbWallpaperClient(),
  private val streamingSearchDataSource: StreamingSearchDataSource = JustWatchClient(),
  private val screensaverPhotoSource: ScreensaverPhotoSource? = null,
  private val appUpdateDataSource: AppUpdateDataSource? = null,
  private val currentVersionName: String = "2.5.17",
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    LauncherUiState(
      isLoading = true,
      isDefaultHome = appDataSource.isDefaultHomeLauncher(),
      settings = settingsStore.load(),
      networkType = appDataSource.getNetworkType(),
    ),
  )
  val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

  private var statusJob: Job? = null
  private var slideshowJob: Job? = null
  private var weatherJob: Job? = null
  private var jellyfinSearchJob: Job? = null
  private var streamingSearchJob: Job? = null
  private var screensaverPhotoJob: Job? = null
  private var cachedBackdrops: List<JellyfinBackdrop> = emptyList()
  private var currentBackdropIndex: Int = 0
  private var cachedPhotoEntries: List<PhotoEntry> = emptyList()
  private var currentPhotoIndex: Int = 0

  private fun dataSourceFor(provider: MediaWallpaperProvider): JellyfinDataSource = when (provider) {
    MediaWallpaperProvider.PLEX -> plexDataSource
    MediaWallpaperProvider.TMDB -> tmdbDataSource
    MediaWallpaperProvider.REDDIT -> redditDataSource
    MediaWallpaperProvider.JELLYFIN, MediaWallpaperProvider.EMBY -> jellyfinDataSource
  }

  init {
    refreshApps()
    observePackageChanges()
    observeNetworkRecovery()
    if (_uiState.value.settings.jellyfinEnabled && _uiState.value.settings.isActiveWallpaperProviderConfigured) {
      startJellyfinSlideshow(fetchFreshList = true)
    }
    if (_uiState.value.settings.weatherEnabled && _uiState.value.settings.isWeatherConfigured) {
      startWeatherRefreshLoop()
    }
    refreshScreensaverPhotoCount()
    if (appUpdateDataSource != null) {
      viewModelScope.launch {
        delay(4_000L)
        val s = _uiState.value.settings
        val shouldAutoCheck = when (s.activeUpdateCheckSchedule) {
          UpdateCheckSchedule.MANUAL -> false
          UpdateCheckSchedule.WEEKLY -> (System.currentTimeMillis() - s.lastUpdateCheckMillis) >= 7L * 24L * 60L * 60L * 1000L
          UpdateCheckSchedule.AUTOMATIC -> true
        }
        if (shouldAutoCheck) {
          checkForUpdates(manual = false)
        }
      }
    }
  }

  private fun observeNetworkRecovery() {
    viewModelScope.launch {
      appDataSource.networkAvailable().collect {
        val state = _uiState.value
        val s = state.settings
        if (s.weatherEnabled && s.isWeatherConfigured && state.weatherData == null && weatherJob?.isActive != true) {
          startWeatherRefreshLoop()
        }
        if (s.jellyfinEnabled && s.isActiveWallpaperProviderConfigured && state.jellyfinSlide == null && slideshowJob?.isActive != true) {
          startJellyfinSlideshow(fetchFreshList = cachedBackdrops.isEmpty())
        }
      }
    }
  }

  private fun observePackageChanges() {
    viewModelScope.launch {
      appDataSource.packageChanges().collect {
        refreshApps()
      }
    }
  }

  fun refreshApps() {
    viewModelScope.launch {
      var currentSettings = _uiState.value.settings
      val loadedApps = appDataSource.loadInstalledApps(currentSettings.customIconPaths)
      val networkType = appDataSource.getNetworkType()
      val searchProviders = appDataSource.getInstalledSearchProviders()
      val tvInputs = appDataSource.loadTvInputs()

      if (!currentSettings.hasInitializedFavorites && loadedApps.isNotEmpty()) {
        val preferredTv = loadedApps.filter { it.isTvApp }.take(6)
        val initialFavorites = (preferredTv.ifEmpty { loadedApps.take(6) })
          .map { it.packageName }
        val existingRows = currentSettings.rows.ifEmpty {
          listOf(AppRow(id = LauncherSettings.DEFAULT_FAVORITES_ROW_ID, title = "Favorites"))
        }
        val firstRow = existingRows.first()
        val updatedRows = if (firstRow.packageNames.isEmpty()) {
          listOf(firstRow.copy(packageNames = initialFavorites)) + existingRows.drop(1)
        } else {
          existingRows
        }
        currentSettings = currentSettings.copy(
          rows = updatedRows,
          hasInitializedFavorites = true,
        )
        settingsStore.save(currentSettings)
      }

      _uiState.update { state ->
        state.copy(
          isLoading = false,
          allApps = loadedApps,
          settings = currentSettings,
          isSetupWizardOpen = !currentSettings.hasCompletedSetupWizard,
          networkType = networkType,
          externalSearchProviders = searchProviders,
          tvInputs = tvInputs,
        )
      }
    }
  }

  fun refreshNetworkStatus() {
    val state = _uiState.value
    _uiState.update {
      it.copy(
        networkType = appDataSource.getNetworkType(),
        isDefaultHome = appDataSource.isDefaultHomeLauncher(),
      )
    }
    val s = state.settings
    if (s.weatherEnabled && s.isWeatherConfigured && state.weatherData == null) {
      startWeatherRefreshLoop()
    }
    if (s.jellyfinEnabled && s.isActiveWallpaperProviderConfigured && state.jellyfinSlide == null) {
      startJellyfinSlideshow(fetchFreshList = cachedBackdrops.isEmpty())
    }
  }

  fun onDefaultHomeResult() {
    val nowDefault = appDataSource.isDefaultHomeLauncher()
    _uiState.update { it.copy(isDefaultHome = nowDefault) }
    if (nowDefault) {
      showStatus("Horizon TV Launcher is now your Default Home app")
    }
  }

  fun onAppFocused(app: AppInfo?) {
    _uiState.update { it.copy(focusedApp = app) }
  }

  fun launchApp(app: AppInfo) {
    if (_uiState.value.settings.isAppPinLocked(app.packageName)) {
      _uiState.update {
        it.copy(
          menuTarget = null,
          pinPrompt = PinPromptState(
            mode = PinPromptMode.VERIFY,
            title = "App Locked",
            subtitle = "Enter 4-digit PIN to open ${app.label}",
            pendingAction = PinPendingAction.LaunchApp(app),
          ),
        )
      }
      return
    }
    if (!appDataSource.launchApp(app)) {
      showStatus("Unable to launch ${app.label}")
    }
  }

  fun openAppMenu(app: AppInfo, rowId: String? = null) {
    _uiState.update {
      it.copy(
        menuTarget = AppMenuTarget(app = app, rowId = rowId),
        reorderingTarget = null,
      )
    }
  }

  fun closeAppMenu() {
    _uiState.update { it.copy(menuTarget = null) }
  }

  fun openRenameApp(app: AppInfo) {
    _uiState.update {
      it.copy(
        menuTarget = null,
        renamingApp = app,
      )
    }
  }

  fun closeRenameApp() {
    _uiState.update { it.copy(renamingApp = null) }
  }

  fun renameApp(packageName: String, newName: String) {
    val trimmed = newName.trim()
    val original = _uiState.value.allApps.firstOrNull { it.packageName == packageName }?.originalLabel ?: "App"
    updateSettings { settings ->
      val updatedLabels = settings.customLabels.toMutableMap()
      if (trimmed.isEmpty() || trimmed == original) {
        updatedLabels.remove(packageName)
        showStatus("Reset name to $original")
      } else {
        updatedLabels[packageName] = trimmed
        showStatus("Renamed to $trimmed")
      }
      settings.copy(customLabels = updatedLabels)
    }
    _uiState.update { it.copy(renamingApp = null) }
  }

  // --- Row Management ---

  private fun isLayoutLockedWithToast(): Boolean {
    if (_uiState.value.settings.lockHomeLayout) {
      showStatus("Home layout is locked — unlock in Customize → Home")
      return true
    }
    return false
  }

  fun openCreateRowDialog() {
    if (isLayoutLockedWithToast()) return
    _uiState.update {
      it.copy(
        isSettingsOpen = false,
        isCreatingRow = true,
      )
    }
  }

  fun closeCreateRowDialog() {
    _uiState.update { it.copy(isCreatingRow = false) }
  }

  fun createRow(title: String, openAppPickerAfter: Boolean = true) {
    val cleaned = title.trim().ifEmpty { "Row ${_uiState.value.settings.rows.size + 1}" }
    val newId = "row_${UUID.randomUUID().toString().take(8)}"
    updateSettings { settings ->
      settings.copy(
        rows = settings.rows + AppRow(id = newId, title = cleaned, packageNames = emptyList()),
      )
    }
    _uiState.update {
      it.copy(
        isCreatingRow = false,
        addingAppsToRowId = if (openAppPickerAfter) newId else null,
      )
    }
    showStatus("Created row \"$cleaned\"")
  }

  fun openRenameRowDialog(rowId: String) {
    _uiState.update {
      it.copy(
        isSettingsOpen = false,
        renamingRowId = rowId,
      )
    }
  }

  fun closeRenameRowDialog() {
    _uiState.update { it.copy(renamingRowId = null) }
  }

  fun renameRow(rowId: String, newTitle: String) {
    val fallback = if (rowId == LauncherSettings.DEFAULT_FAVORITES_ROW_ID) "Favorites" else "Custom Row"
    val cleaned = newTitle.trim().ifEmpty { fallback }
    updateSettings { settings ->
      settings.copy(
        rows = settings.rows.map { row ->
          if (row.id == rowId) row.copy(title = cleaned) else row
        },
      )
    }
    _uiState.update { it.copy(renamingRowId = null) }
    showStatus("Row renamed to \"$cleaned\"")
  }

  fun moveRow(rowId: String, delta: Int) = moveRowInternal(rowId, delta, bypassLayoutLock = false)

  private fun moveRowInternal(rowId: String, delta: Int, bypassLayoutLock: Boolean) {
    if (!bypassLayoutLock && isLayoutLockedWithToast()) return
    updateSettings { settings ->
      val list = settings.rows.toMutableList()
      val idx = list.indexOfFirst { it.id == rowId }
      if (idx == -1) return@updateSettings settings
      val target = (idx + delta).coerceIn(0, list.lastIndex)
      if (target == idx) return@updateSettings settings
      val item = list.removeAt(idx)
      list.add(target, item)
      settings.copy(rows = list)
    }
  }

  fun deleteRow(rowId: String) = deleteRowInternal(rowId, bypassLayoutLock = false)

  private fun deleteRowInternal(rowId: String, bypassLayoutLock: Boolean) {
    if (!bypassLayoutLock && isLayoutLockedWithToast()) return
    val currentRows = _uiState.value.settings.rows
    if (currentRows.size <= 1) {
      showStatus("Keep at least one row on your home screen")
      return
    }
    val removedTitle = currentRows.firstOrNull { it.id == rowId }?.title ?: "Row"
    updateSettings { settings ->
      settings.copy(rows = settings.rows.filterNot { it.id == rowId })
    }
    _uiState.update {
      it.copy(
        reorderingTarget = if (it.reorderingTarget?.rowId == rowId) null else it.reorderingTarget,
        addingAppsToRowId = if (it.addingAppsToRowId == rowId) null else it.addingAppsToRowId,
      )
    }
    showStatus("Deleted row \"$removedTitle\"")
  }

  fun openAddAppsToRow(rowId: String) {
    if (isLayoutLockedWithToast()) return
    _uiState.update {
      it.copy(
        isSettingsOpen = false,
        addingAppsToRowId = rowId,
        reorderingTarget = null,
      )
    }
  }

  fun closeAddAppsToRow() {
    _uiState.update { it.copy(addingAppsToRowId = null) }
  }

  fun openTileOrderDialog(rowId: String) {
    if (isLayoutLockedWithToast()) return
    _uiState.update {
      it.copy(
        isSettingsOpen = false,
        orderingRowId = rowId,
        reorderingTarget = null,
      )
    }
  }

  fun closeTileOrderDialog() {
    _uiState.update { it.copy(orderingRowId = null) }
  }

  fun sortRowAppsAlphabetically(rowId: String) = sortRowAppsAlphabeticallyInternal(rowId, bypassLayoutLock = false)

  private fun sortRowAppsAlphabeticallyInternal(rowId: String, bypassLayoutLock: Boolean) {
    if (!bypassLayoutLock && isLayoutLockedWithToast()) return
    val labelsByPkg = _uiState.value.allVisibleApps.associate { it.packageName to it.label }
    val rowTitle = _uiState.value.settings.rows.firstOrNull { it.id == rowId }?.title ?: "Row"
    updateSettings { settings ->
      val updatedRows = settings.rows.map { row ->
        if (row.id != rowId) {
          row
        } else {
          val sorted = row.packageNames.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { pkg -> labelsByPkg[pkg] ?: pkg },
          )
          row.copy(packageNames = sorted)
        }
      }
      settings.copy(rows = updatedRows)
    }
    showStatus("Sorted $rowTitle A–Z")
  }

  fun toggleAppInRow(rowId: String, packageName: String) =
    toggleAppInRowInternal(rowId, packageName, bypassLayoutLock = false)

  private fun toggleAppInRowInternal(rowId: String, packageName: String, bypassLayoutLock: Boolean) {
    if (!bypassLayoutLock && isLayoutLockedWithToast()) return
    val appLabel = _uiState.value.allApps.firstOrNull { it.packageName == packageName }?.label ?: "App"
    val rowTitle = _uiState.value.settings.rows.firstOrNull { it.id == rowId }?.title ?: "Row"
    updateSettings { settings ->
      val updatedRows = settings.rows.map { row ->
        if (row.id != rowId) {
          row
        } else {
          val updatedPkgs = if (packageName in row.packageNames) {
            showStatus("Removed $appLabel from $rowTitle")
            row.packageNames - packageName
          } else {
            showStatus("Added $appLabel to $rowTitle")
            row.packageNames + packageName
          }
          row.copy(packageNames = updatedPkgs)
        }
      }
      settings.copy(
        rows = updatedRows,
        hasInitializedFavorites = true,
      )
    }
  }

  fun moveAppInRow(rowId: String, packageName: String, delta: Int) =
    moveAppInRowInternal(rowId, packageName, delta, bypassLayoutLock = false)

  private fun moveAppInRowInternal(rowId: String, packageName: String, delta: Int, bypassLayoutLock: Boolean) {
    if (!bypassLayoutLock && isLayoutLockedWithToast()) return
    updateSettings { settings ->
      val updatedRows = settings.rows.map { row ->
        if (row.id != rowId) {
          row
        } else {
          val current = row.packageNames.toMutableList()
          val index = current.indexOf(packageName)
          if (index == -1) return@map row
          val target = (index + delta).coerceIn(0, current.lastIndex)
          if (target == index) return@map row
          val item = current.removeAt(index)
          current.add(target, item)
          row.copy(packageNames = current)
        }
      }
      settings.copy(rows = updatedRows)
    }
  }

  fun startReordering(rowId: String, packageName: String) {
    if (isLayoutLockedWithToast()) return
    val row = _uiState.value.settings.rows.firstOrNull { it.id == rowId } ?: return
    if (packageName !in row.packageNames) return
    _uiState.update {
      it.copy(
        menuTarget = null,
        reorderingTarget = ReorderTarget(rowId = rowId, packageName = packageName),
      )
    }
  }

  fun stopReordering() {
    _uiState.update { it.copy(reorderingTarget = null) }
  }

  // --- Media Server Backdrop Slideshow (Jellyfin, Plex, Emby) ---

  fun selectWallpaperProvider(provider: MediaWallpaperProvider) {
    if (_uiState.value.settings.activeWallpaperProvider == provider) return
    stopJellyfinSlideshow()
    cachedBackdrops = emptyList()
    currentBackdropIndex = 0
    val isConfigured = _uiState.value.settings.isProviderConfigured(provider)
    updateSettings { settings ->
      settings.copy(
        wallpaperProvider = provider.id,
        jellyfinEnabled = isConfigured,
        customWallpaperUri = if (isConfigured) null else settings.customWallpaperUri,
      )
    }
    _uiState.update {
      it.copy(
        jellyfinSlide = null,
        jellyfinBackdropCount = 0,
        jellyfinLibraries = emptyList(),
      )
    }
    if (isConfigured) {
      startJellyfinSlideshow(fetchFreshList = true)
      showStatus("Switched wallpaper provider to ${provider.label}")
    } else {
      showStatus("Selected ${provider.label} — connect your server to enable")
    }
  }

  fun openJellyfinSetup(provider: MediaWallpaperProvider = _uiState.value.settings.activeWallpaperProvider) {
    _uiState.update {
      it.copy(
        isSettingsOpen = false,
        setupProviderDialog = provider,
        jellyfinError = null,
      )
    }
  }

  fun closeJellyfinSetup() {
    _uiState.update {
      it.copy(
        setupProviderDialog = null,
        isTestingJellyfin = false,
        jellyfinError = null,
      )
    }
  }

  fun connectJellyfin(
    serverUrl: String,
    apiKey: String?,
    username: String?,
    password: String?,
    provider: MediaWallpaperProvider = _uiState.value.setupProviderDialog
      ?: _uiState.value.settings.activeWallpaperProvider,
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isTestingJellyfin = true, jellyfinError = null) }
      val cur = _uiState.value.settings
      val providerCfg = cur.configFor(provider)
      val client = dataSourceFor(provider)
      val result = client.authenticateAndFetch(
        rawServerUrl = serverUrl,
        apiKey = apiKey,
        username = username,
        password = password,
        mediaType = JellyfinMediaType.fromId(providerCfg.mediaType),
        libraryIds = cur.libraryIdsFor(provider),
        limit = providerCfg.backdropLimit,
      )
      result.fold(
        onSuccess = { auth ->
          cachedBackdrops = auth.backdrops
          currentBackdropIndex = 0
          updateSettings { settings ->
            val base = settings.copy(
              wallpaperProvider = provider.id,
              jellyfinEnabled = true,
              customWallpaperUri = null,
            )
            when (provider) {
              MediaWallpaperProvider.JELLYFIN -> base.copy(
                jellyfinServerUrl = auth.serverUrl,
                jellyfinApiKey = auth.token,
                jellyfinUserId = auth.userId,
              )
              MediaWallpaperProvider.PLEX -> base.copy(
                plexServerUrl = auth.serverUrl,
                plexToken = auth.token,
              )
              MediaWallpaperProvider.EMBY -> base.copy(
                embyServerUrl = auth.serverUrl,
                embyApiKey = auth.token,
                embyUserId = auth.userId,
              )
              MediaWallpaperProvider.TMDB -> base.copy(
                tmdbApiKey = auth.token,
              )
              MediaWallpaperProvider.REDDIT -> base
            }
          }
          _uiState.update {
            it.copy(
              isTestingJellyfin = false,
              setupProviderDialog = null,
              jellyfinError = null,
              jellyfinBackdropCount = cachedBackdrops.size,
              jellyfinLibraries = auth.libraries,
            )
          }
          showStatus("${provider.label} connected — ${cachedBackdrops.size} backdrops ready")
          startJellyfinSlideshow(fetchFreshList = false)
        },
        onFailure = { err ->
          val msg = err.message ?: "Could not connect to ${provider.label} server"
          _uiState.update {
            it.copy(
              isTestingJellyfin = false,
              jellyfinError = msg,
            )
          }
        },
      )
    }
  }

  fun setJellyfinMediaType(typeId: String) {
    val parsed = JellyfinMediaType.fromId(typeId)
    val provider = _uiState.value.settings.activeWallpaperProvider
    updateSettings {
      it.withProviderConfig(provider) { cfg -> cfg.copy(mediaType = parsed.id) }
    }
    if (_uiState.value.settings.jellyfinEnabled) {
      cachedBackdrops = emptyList()
      startJellyfinSlideshow(fetchFreshList = true)
      showStatus("${provider.label} filter: ${parsed.label}")
    }
  }

  fun selectJellyfinLibrary(library: JellyfinLibrary?) {
    val provider = _uiState.value.settings.activeWallpaperProvider
    val currentIds = _uiState.value.settings.libraryIdsFor(provider)
    val updatedIds = if (library == null) {
      when (provider) {
        MediaWallpaperProvider.REDDIT -> LauncherSettings.DEFAULT_REDDIT_SUBREDDITS
        MediaWallpaperProvider.TMDB -> LauncherSettings.DEFAULT_TMDB_CATEGORIES
        else -> emptySet()
      }
    } else {
      val toggled = if (library.id in currentIds) currentIds - library.id else currentIds + library.id
      when {
        provider == MediaWallpaperProvider.REDDIT && toggled.isEmpty() -> LauncherSettings.DEFAULT_REDDIT_SUBREDDITS
        provider == MediaWallpaperProvider.TMDB && toggled.isEmpty() -> LauncherSettings.DEFAULT_TMDB_CATEGORIES
        else -> toggled
      }
    }
    updateSettings { settings ->
      when (provider) {
        MediaWallpaperProvider.JELLYFIN -> settings.copy(jellyfinLibraryIds = updatedIds)
        MediaWallpaperProvider.PLEX -> settings.copy(plexLibraryIds = updatedIds)
        MediaWallpaperProvider.EMBY -> settings.copy(embyLibraryIds = updatedIds)
        MediaWallpaperProvider.TMDB -> settings.copy(tmdbCategories = updatedIds)
        MediaWallpaperProvider.REDDIT -> settings.copy(redditSubreddits = updatedIds)
      }
    }
    if (_uiState.value.settings.jellyfinEnabled) {
      cachedBackdrops = emptyList()
      startJellyfinSlideshow(fetchFreshList = true)
      val statusText = if (provider == MediaWallpaperProvider.REDDIT) {
        "Reddit: ${updatedIds.joinToString(", ") { "r/$it" }}"
      } else if (updatedIds.isEmpty()) {
        "${provider.label}: All Libraries"
      } else {
        val names = _uiState.value.jellyfinLibraries
          .filter { it.id in updatedIds }
          .map { it.name }
        if (names.isNotEmpty()) {
          "${provider.label} libraries: ${names.joinToString(", ")}"
        } else {
          "${provider.label}: ${updatedIds.size} selected"
        }
      }
      showStatus(statusText)
    }
  }

  fun setRedditSort(sortId: String) {
    val parsed = RedditSort.fromId(sortId)
    updateSettings { it.copy(redditSort = parsed.id) }
    val s = _uiState.value.settings
    if (s.jellyfinEnabled && s.activeWallpaperProvider == MediaWallpaperProvider.REDDIT) {
      cachedBackdrops = emptyList()
      startJellyfinSlideshow(fetchFreshList = true)
      showStatus("Reddit feed: ${parsed.label}")
    }
  }

  fun openAddSubredditDialog() {
    _uiState.update {
      it.copy(
        isSettingsOpen = false,
        isAddingSubreddit = true,
      )
    }
  }

  fun closeAddSubredditDialog() {
    _uiState.update { it.copy(isAddingSubreddit = false) }
  }

  fun addCustomSubreddit(rawSubreddit: String) {
    val cleaned = RedditWallpaperClient.sanitizeSubreddit(rawSubreddit)
    if (cleaned.length < 2) {
      showStatus("Enter a valid subreddit name (e.g. MostBeautiful)")
      return
    }
    updateSettings { settings ->
      settings.copy(
        wallpaperProvider = MediaWallpaperProvider.REDDIT.id,
        jellyfinEnabled = true,
        customWallpaperUri = null,
        redditCustomSubreddits = settings.redditCustomSubreddits + cleaned,
        redditSubreddits = settings.redditSubreddits + cleaned,
      )
    }
    _uiState.update { it.copy(isAddingSubreddit = false) }
    cachedBackdrops = emptyList()
    startJellyfinSlideshow(fetchFreshList = true)
    showStatus("Added r/$cleaned to Reddit wallpapers")
  }

  fun setJellyfinBackdropLimit(limit: Int) {
    val clamped = limit.coerceIn(10, 500)
    val provider = _uiState.value.settings.activeWallpaperProvider
    updateSettings {
      it.withProviderConfig(provider) { cfg -> cfg.copy(backdropLimit = clamped) }
    }
    if (_uiState.value.settings.jellyfinEnabled) {
      cachedBackdrops = emptyList()
      startJellyfinSlideshow(fetchFreshList = true)
      showStatus("Fetching up to $clamped ${provider.label} backdrops…")
    }
  }

  fun toggleJellyfinSlideshow() {
    val settings = _uiState.value.settings
    val provider = settings.activeWallpaperProvider
    if (!settings.isProviderConfigured(provider)) {
      openJellyfinSetup(provider)
      return
    }
    val nextEnabled = !settings.jellyfinEnabled
    updateSettings {
      it.copy(
        jellyfinEnabled = nextEnabled,
        customWallpaperUri = if (nextEnabled) null else it.customWallpaperUri,
      )
    }
    if (nextEnabled) {
      startJellyfinSlideshow(fetchFreshList = cachedBackdrops.isEmpty())
      showStatus("${provider.label} slideshow enabled")
    } else {
      stopJellyfinSlideshow()
      showStatus("${provider.label} slideshow paused")
    }
  }

  fun setJellyfinInterval(seconds: Int) {
    val clamped = seconds.coerceIn(10, 600)
    val provider = _uiState.value.settings.activeWallpaperProvider
    updateSettings {
      it.withProviderConfig(provider) { cfg -> cfg.copy(intervalSeconds = clamped) }
    }
    if (_uiState.value.settings.jellyfinEnabled) {
      startJellyfinSlideshow(fetchFreshList = false)
    }
  }

  fun toggleJellyfinShowTitle() {
    val provider = _uiState.value.settings.activeWallpaperProvider
    updateSettings {
      it.withProviderConfig(provider) { cfg -> cfg.copy(showTitle = !cfg.showTitle) }
    }
  }

  fun nextJellyfinBackdrop() {
    if (!_uiState.value.settings.jellyfinEnabled) return
    if (cachedBackdrops.isEmpty()) {
      startJellyfinSlideshow(fetchFreshList = true)
      return
    }
    currentBackdropIndex = (currentBackdropIndex + 1) % cachedBackdrops.size
    startJellyfinSlideshow(fetchFreshList = false)
  }

  fun disconnectJellyfin(
    provider: MediaWallpaperProvider = _uiState.value.setupProviderDialog
      ?: _uiState.value.settings.activeWallpaperProvider,
  ) {
    val isDisconnectingActive = _uiState.value.settings.activeWallpaperProvider == provider
    if (isDisconnectingActive) {
      stopJellyfinSlideshow()
      cachedBackdrops = emptyList()
      currentBackdropIndex = 0
    }
    updateSettings { settings ->
      val base = if (isDisconnectingActive) settings.copy(jellyfinEnabled = false) else settings
      when (provider) {
        MediaWallpaperProvider.JELLYFIN -> base.copy(
          jellyfinServerUrl = "",
          jellyfinApiKey = "",
          jellyfinUserId = "",
          jellyfinLibraryIds = emptySet(),
        )
        MediaWallpaperProvider.PLEX -> base.copy(
          plexServerUrl = "",
          plexToken = "",
          plexLibraryIds = emptySet(),
        )
        MediaWallpaperProvider.EMBY -> base.copy(
          embyServerUrl = "",
          embyApiKey = "",
          embyUserId = "",
          embyLibraryIds = emptySet(),
        )
        MediaWallpaperProvider.TMDB -> base.copy(
          tmdbApiKey = "",
          tmdbCategories = LauncherSettings.DEFAULT_TMDB_CATEGORIES,
        )
        MediaWallpaperProvider.REDDIT -> base
      }
    }
    _uiState.update {
      it.copy(
        setupProviderDialog = null,
        jellyfinSlide = if (isDisconnectingActive) null else it.jellyfinSlide,
        jellyfinBackdropCount = if (isDisconnectingActive) 0 else it.jellyfinBackdropCount,
        jellyfinLibraries = if (isDisconnectingActive) emptyList() else it.jellyfinLibraries,
      )
    }
    showStatus("${provider.label} server disconnected")
  }

  // --- Backup & Restore Configuration ---

  fun exportBackupJson(): String = backupSerializer.encodeToJson(_uiState.value.settings)

  fun onBackupExported(success: Boolean) {
    showStatus(if (success) "Configuration backed up" else "Could not save backup file")
  }

  fun restoreBackupJson(rawJson: String) {
    val decoded = backupSerializer.decodeFromJson(rawJson, existing = _uiState.value.settings)
    decoded.fold(
      onSuccess = { restored ->
        settingsStore.save(restored)
        cachedBackdrops = emptyList()
        currentBackdropIndex = 0
        stopJellyfinSlideshow()
        weatherJob?.cancel()
        weatherJob = null
        _uiState.update {
          it.copy(
            settings = restored,
            isSettingsOpen = false,
            jellyfinSlide = null,
            jellyfinBackdropCount = 0,
            jellyfinLibraries = emptyList(),
            weatherData = if (restored.weatherEnabled) it.weatherData else null,
          )
        }
        refreshApps()
        if (restored.jellyfinEnabled && restored.isActiveWallpaperProviderConfigured) {
          startJellyfinSlideshow(fetchFreshList = true)
        }
        if (restored.weatherEnabled && restored.isWeatherConfigured) {
          startWeatherRefreshLoop()
        }
        showStatus("Configuration restored (${restored.rows.size} rows)")
      },
      onFailure = { err ->
        showStatus(err.message ?: "Invalid backup file")
      },
    )
  }

  fun resetToDefaults() {
    val oldCustomIcons = _uiState.value.settings.customIconPaths.values.toList()
    val defaults = LauncherSettings()
    settingsStore.save(defaults)
    cachedBackdrops = emptyList()
    currentBackdropIndex = 0
    cachedPhotoEntries = emptyList()
    currentPhotoIndex = 0
    stopJellyfinSlideshow()
    weatherJob?.cancel()
    weatherJob = null
    screensaverPhotoJob?.cancel()
    screensaverPhotoJob = null
    viewModelScope.launch {
      oldCustomIcons.forEach { path -> appDataSource.deleteCustomIcon(path) }
    }
    _uiState.update {
      it.copy(
        settings = defaults,
        isSettingsOpen = false,
        isScreensaverActive = false,
        jellyfinSlide = null,
        jellyfinBackdropCount = 0,
        jellyfinLibraries = emptyList(),
        weatherData = null,
        weatherFetchError = null,
        screensaverPhotoSlide = null,
        screensaverPhotoCount = 0,
      )
    }
    refreshApps()
    showStatus("Launcher reset to default settings")
  }

  private fun startJellyfinSlideshow(fetchFreshList: Boolean) {
    slideshowJob?.cancel()
    val settings = _uiState.value.settings
    val provider = settings.activeWallpaperProvider
    if ((!settings.jellyfinEnabled && !_uiState.value.isScreensaverActive) || !settings.isProviderConfigured(provider)) return
    val client = dataSourceFor(provider)

    slideshowJob = viewModelScope.launch {
      var retrySeconds = 5L
      while (isActive && (_uiState.value.settings.jellyfinEnabled || _uiState.value.isScreensaverActive) && (fetchFreshList || cachedBackdrops.isEmpty())) {
        val current = _uiState.value.settings
        val activeProvider = current.activeWallpaperProvider
        val activeClient = dataSourceFor(activeProvider)
        val serverUrl = current.serverUrlFor(activeProvider)
        val token = current.tokenFor(activeProvider)
        val userId = current.userIdFor(activeProvider)
        val libraryIds = current.libraryIdsFor(activeProvider)

        if (_uiState.value.jellyfinLibraries.isEmpty()) {
          activeClient.fetchLibraries(
            serverUrl = serverUrl,
            token = token,
            userId = userId,
          ).onSuccess { libs ->
            _uiState.update { it.copy(jellyfinLibraries = libs) }
          }
        }

        val activeCfg = current.configFor(activeProvider)
        val fetched = activeClient.fetchBackdrops(
          serverUrl = serverUrl,
          token = token,
          userId = userId,
          mediaType = JellyfinMediaType.fromId(activeCfg.mediaType),
          libraryIds = libraryIds,
          limit = activeCfg.backdropLimit,
        )
        if (fetched.isSuccess) {
          val list = fetched.getOrDefault(emptyList())
          cachedBackdrops = list
          currentBackdropIndex = 0
          _uiState.update { it.copy(jellyfinBackdropCount = list.size) }
          if (list.isEmpty()) {
            showStatus("No backdrops found for that ${activeProvider.label} filter")
            return@launch
          }
          break
        } else {
          // Network/LAN may still be waking up after reboot — retry quickly
          delay(retrySeconds * 1000L)
          retrySeconds = (retrySeconds * 2L).coerceAtMost(60L)
        }
      }

      if (cachedBackdrops.isEmpty()) return@launch

      var consecutiveMisses = 0
      while (isActive && (_uiState.value.settings.jellyfinEnabled || _uiState.value.isScreensaverActive)) {
        val backdrop = cachedBackdrops[currentBackdropIndex % cachedBackdrops.size]
        val activeProvider = _uiState.value.settings.activeWallpaperProvider
        val token = _uiState.value.settings.tokenFor(activeProvider)
        val imgDeferred = async {
          client.downloadBackdropImage(imageUrl = backdrop.imageUrl, token = token)
        }
        val logoDeferred = async {
          backdrop.logoUrl?.let { url ->
            client.downloadLogoImage(logoUrl = url, token = token)
          }
        }
        val img = imgDeferred.await()
        val logoImg = logoDeferred.await()
        if (img != null) {
          consecutiveMisses = 0
          _uiState.update {
            it.copy(
              jellyfinSlide = JellyfinSlide(
                backdrop = backdrop,
                image = img,
                logoImage = logoImg,
              ),
              jellyfinBackdropCount = cachedBackdrops.size,
            )
          }
          val waitSec = if (_uiState.value.isScreensaverActive) {
            _uiState.value.settings.screensaverIntervalSeconds.coerceIn(5, 300)
          } else {
            _uiState.value.settings.activeWallpaperIntervalSeconds.coerceIn(10, 600)
          }
          delay(waitSec * 1000L)
        } else {
          consecutiveMisses++
          // If an image URL was vertical/deleted/broken, skip quickly instead of waiting the full interval
          delay(if (consecutiveMisses >= cachedBackdrops.size) 10_000L else 250L)
        }
        currentBackdropIndex = (currentBackdropIndex + 1) % cachedBackdrops.size
      }
    }
  }

  private fun stopJellyfinSlideshow() {
    slideshowJob?.cancel()
    slideshowJob = null
    _uiState.update { it.copy(jellyfinSlide = null) }
  }

  // --- Weather Widget ---

  fun openWeatherSetup() {
    _uiState.update {
      it.copy(
        isSettingsOpen = false,
        isWeatherDialogOpen = true,
        weatherError = null,
        weatherSearchResults = emptyList(),
      )
    }
  }

  fun closeWeatherSetup() {
    _uiState.update {
      it.copy(
        isWeatherDialogOpen = false,
        isSearchingWeather = false,
        weatherError = null,
      )
    }
  }

  fun searchWeatherLocations(query: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isSearchingWeather = true, weatherError = null) }
      weatherDataSource.searchLocations(query).fold(
        onSuccess = { results ->
          if (results.isEmpty()) {
            _uiState.update {
              it.copy(
                isSearchingWeather = false,
                weatherSearchResults = emptyList(),
                weatherError = "No matching cities found for \"$query\".",
              )
            }
          } else {
            _uiState.update {
              it.copy(
                isSearchingWeather = false,
                weatherSearchResults = results,
                weatherError = null,
              )
            }
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isSearchingWeather = false,
              weatherError = err.message ?: "Unable to search locations right now",
            )
          }
        },
      )
    }
  }

  fun selectWeatherLocation(location: WeatherLocation) {
    updateSettings { settings ->
      settings.copy(
        weatherEnabled = true,
        weatherLocationName = location.shortName,
        weatherLatitude = location.latitude,
        weatherLongitude = location.longitude,
      )
    }
    _uiState.update {
      it.copy(
        isWeatherDialogOpen = false,
        weatherSearchResults = emptyList(),
        weatherError = null,
      )
    }
    showStatus("Weather set to ${location.shortName}")
    startWeatherRefreshLoop()
  }

  fun toggleWeatherEnabled() {
    val settings = _uiState.value.settings
    if (!settings.isWeatherConfigured) {
      openWeatherSetup()
      return
    }
    val nextEnabled = !settings.weatherEnabled
    updateSettings { it.copy(weatherEnabled = nextEnabled) }
    if (nextEnabled) {
      startWeatherRefreshLoop()
      showStatus("Weather enabled")
    } else {
      weatherJob?.cancel()
      weatherJob = null
      showStatus("Weather hidden")
    }
  }

  fun setWeatherDays(days: Int) {
    updateSettings { it.copy(weatherDays = days.coerceIn(1, 7)) }
  }

  fun toggleWeatherShowForecast() {
    updateSettings { it.copy(weatherShowForecast = !it.weatherShowForecast) }
  }

  fun setWeatherForecastSize(sizeId: String) {
    val parsed = WeatherForecastSize.fromId(sizeId)
    updateSettings { it.copy(weatherForecastSize = parsed.id) }
  }

  fun toggleWeatherUnit() {
    updateSettings { it.copy(weatherUseFahrenheit = !it.weatherUseFahrenheit) }
    if (_uiState.value.settings.weatherEnabled && _uiState.value.settings.isWeatherConfigured) {
      startWeatherRefreshLoop()
    }
  }

  fun setWeatherIconStyle(styleId: String) {
    updateSettings { it.copy(weatherIconStyle = styleId) }
  }

  fun toggleWeatherBackdrop() {
    updateSettings { it.copy(weatherShowBackdrop = !it.weatherShowBackdrop) }
  }

  fun refreshWeather() {
    if (_uiState.value.settings.isWeatherConfigured) {
      startWeatherRefreshLoop()
      showStatus("Refreshing weather…")
    }
  }

  private fun startWeatherRefreshLoop() {
    weatherJob?.cancel()
    val s = _uiState.value.settings
    val lat = s.weatherLatitude ?: return
    val lon = s.weatherLongitude ?: return
    if (!s.weatherEnabled) return

    weatherJob = viewModelScope.launch {
      var retrySeconds = 5L
      while (isActive && _uiState.value.settings.weatherEnabled) {
        val currentSettings = _uiState.value.settings
        val result = weatherDataSource.fetchForecast(
          locationName = currentSettings.weatherLocationName,
          latitude = currentSettings.weatherLatitude ?: lat,
          longitude = currentSettings.weatherLongitude ?: lon,
          useFahrenheit = currentSettings.weatherUseFahrenheit,
        )
        if (result.isSuccess) {
          retrySeconds = 5L
          result.getOrNull()?.let { data ->
            _uiState.update { it.copy(weatherData = data, weatherFetchError = null) }
          }
          // Auto-refresh every 30 minutes once connected
          delay(30L * 60L * 1000L)
        } else {
          val errMsg = result.exceptionOrNull()?.message
            ?: "Unable to reach weather service right now."
          _uiState.update { state ->
            if (state.weatherData == null) state.copy(weatherFetchError = errMsg) else state
          }
          // Retry quickly if Wi-Fi/DNS wasn't up yet after reboot
          delay(retrySeconds * 1000L)
          retrySeconds = (retrySeconds * 2L).coerceAtMost(60L)
        }
      }
    }
  }

  // --- Custom App Icons ---

  fun setCustomAppIcon(packageName: String, sourceUri: Uri) {
    viewModelScope.launch {
      val savedPath = appDataSource.saveCustomIcon(packageName, sourceUri)
      if (savedPath != null) {
        updateSettings { settings ->
          val updated = settings.customIconPaths.toMutableMap()
          updated[packageName] = savedPath
          settings.copy(customIconPaths = updated)
        }
        refreshApps()
        showStatus("Custom icon updated")
      } else {
        showStatus("Could not load selected image")
      }
    }
  }

  fun resetCustomAppIcon(packageName: String) {
    viewModelScope.launch {
      val existingPath = _uiState.value.settings.customIconPaths[packageName]
      appDataSource.deleteCustomIcon(existingPath)
      updateSettings { settings ->
        settings.copy(customIconPaths = settings.customIconPaths - packageName)
      }
      closeAppMenu()
      refreshApps()
      showStatus("Restored original app icon")
    }
  }

  // --- Hiding, Filtering, Appearance & System ---

  fun hideApp(packageName: String) {
    val appLabel = _uiState.value.allVisibleApps.firstOrNull { it.packageName == packageName }?.label ?: "App"
    updateSettings { settings ->
      settings.copy(
        hiddenPackages = settings.hiddenPackages + packageName,
        rows = settings.rows.map { it.copy(packageNames = it.packageNames - packageName) },
      )
    }
    _uiState.update {
      it.copy(
        menuTarget = null,
        reorderingTarget = if (it.reorderingTarget?.packageName == packageName) null else it.reorderingTarget,
      )
    }
    showStatus("Hidden $appLabel (restore anytime in Customize)")
  }

  fun unhideApp(packageName: String) {
    val appLabel = _uiState.value.allApps.firstOrNull { it.packageName == packageName }?.label ?: "App"
    updateSettings { settings ->
      settings.copy(hiddenPackages = settings.hiddenPackages - packageName)
    }
    showStatus("Restored $appLabel")
  }

  fun setFilter(filter: AppFilter) {
    _uiState.update { it.copy(selectedFilter = filter) }
  }

  fun toggleSearch() {
    val nextVisible = !_uiState.value.isSearchVisible
    if (!nextVisible) {
      jellyfinSearchJob?.cancel()
      streamingSearchJob?.cancel()
    }
    _uiState.update {
      it.copy(
        isSearchVisible = nextVisible,
        searchQuery = if (nextVisible) it.searchQuery else "",
        searchProvider = if (nextVisible) it.searchProvider else SearchProviderFilter.ALL,
        isSearchingJellyfin = if (nextVisible) it.isSearchingJellyfin else false,
        jellyfinSearchResults = if (nextVisible) it.jellyfinSearchResults else emptyList(),
        jellyfinSearchError = if (nextVisible) it.jellyfinSearchError else null,
        isSearchingStreaming = if (nextVisible) it.isSearchingStreaming else false,
        isLoadingMoreStreaming = if (nextVisible) it.isLoadingMoreStreaming else false,
        streamingSearchResults = if (nextVisible) it.streamingSearchResults else emptyList(),
        streamingHasMore = if (nextVisible) it.streamingHasMore else false,
        streamingNextCursor = if (nextVisible) it.streamingNextCursor else null,
        streamingSearchError = if (nextVisible) it.streamingSearchError else null,
      )
    }
  }

  fun setSearchProvider(provider: SearchProviderFilter) {
    _uiState.update { it.copy(searchProvider = provider) }
  }

  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query, jellyfinSearchError = null, streamingSearchError = null) }
    jellyfinSearchJob?.cancel()
    streamingSearchJob?.cancel()
    val s = _uiState.value.settings
    val trimmed = query.trim()

    val jellyfinSearchOn = s.isJellyfinConfigured &&
      s.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_JELLYFIN)
    if (!jellyfinSearchOn || trimmed.length < 2) {
      _uiState.update {
        it.copy(
          isSearchingJellyfin = false,
          jellyfinSearchResults = emptyList(),
          jellyfinSearchError = null,
        )
      }
    } else {
      jellyfinSearchJob = viewModelScope.launch {
        _uiState.update { it.copy(isSearchingJellyfin = true, jellyfinSearchError = null) }
        delay(300L)
        val res = jellyfinDataSource.searchItems(
          serverUrl = s.jellyfinServerUrl,
          token = s.jellyfinApiKey,
          userId = s.jellyfinUserId,
          query = trimmed,
        )
        res.fold(
          onSuccess = { items ->
            _uiState.update {
              it.copy(
                isSearchingJellyfin = false,
                jellyfinSearchResults = items,
                jellyfinSearchError = null,
              )
            }
            val hydrated = items.map { item ->
              async {
                val url = item.imageUrl
                val thumb = if (url != null) {
                  jellyfinDataSource.downloadThumbnail(url, s.jellyfinApiKey)
                } else {
                  null
                }
                item.copy(thumbnail = thumb)
              }
            }.map { it.await() }
            if (_uiState.value.searchQuery.trim() == trimmed) {
              _uiState.update { it.copy(jellyfinSearchResults = hydrated) }
            }
          },
          onFailure = { err ->
            _uiState.update {
              it.copy(
                isSearchingJellyfin = false,
                jellyfinSearchResults = emptyList(),
                jellyfinSearchError = err.message ?: "Could not search Jellyfin right now",
              )
            }
          },
        )
      }
    }

    val justWatchOn = s.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_JUSTWATCH)
    if (!justWatchOn || trimmed.length < 2) {
      _uiState.update {
        it.copy(
          isSearchingStreaming = false,
          isLoadingMoreStreaming = false,
          streamingSearchResults = emptyList(),
          streamingHasMore = false,
          streamingNextCursor = null,
          streamingSearchError = null,
        )
      }
    } else {
      streamingSearchJob = viewModelScope.launch {
        _uiState.update {
          it.copy(
            isSearchingStreaming = true,
            isLoadingMoreStreaming = false,
            streamingHasMore = false,
            streamingNextCursor = null,
            streamingSearchError = null,
          )
        }
        delay(350L)
        val res = streamingSearchDataSource.searchTitlesPage(
          query = trimmed,
          countryCode = s.activeJustWatchRegion.code,
          limit = 8,
          afterCursor = null,
        )
        res.fold(
          onSuccess = { page ->
            val items = page.items
            _uiState.update {
              it.copy(
                isSearchingStreaming = false,
                streamingSearchResults = items,
                streamingHasMore = page.hasNextPage,
                streamingNextCursor = page.endCursor,
                streamingSearchError = null,
              )
            }
            val hydratedById = items.map { item ->
              async {
                val url = item.posterUrl
                val poster = if (url != null) {
                  streamingSearchDataSource.downloadPoster(url)
                } else {
                  null
                }
                item.id to poster
              }
            }.map { it.await() }.toMap()
            if (_uiState.value.searchQuery.trim() == trimmed) {
              _uiState.update { state ->
                state.copy(
                  streamingSearchResults = state.streamingSearchResults.map { existing ->
                    val poster = hydratedById[existing.id]
                    if (poster != null && existing.poster == null) existing.copy(poster = poster) else existing
                  },
                )
              }
            }
          },
          onFailure = { err ->
            _uiState.update {
              it.copy(
                isSearchingStreaming = false,
                streamingSearchResults = emptyList(),
                streamingHasMore = false,
                streamingNextCursor = null,
                streamingSearchError = err.message ?: "Could not search JustWatch right now",
              )
            }
          },
        )
      }
    }
  }

  fun loadMoreStreamingResults() {
    val current = _uiState.value
    val trimmed = current.searchQuery.trim()
    val cursor = current.streamingNextCursor
    if (trimmed.length < 2 || cursor.isNullOrBlank() || current.isSearchingStreaming || current.isLoadingMoreStreaming) {
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingMoreStreaming = true) }
      val res = streamingSearchDataSource.searchTitlesPage(
        query = trimmed,
        countryCode = _uiState.value.settings.activeJustWatchRegion.code,
        limit = 8,
        afterCursor = cursor,
      )
      res.fold(
        onSuccess = { page ->
          if (_uiState.value.searchQuery.trim() != trimmed) return@fold
          val existingIds = _uiState.value.streamingSearchResults.map { it.id }.toSet()
          val newItems = page.items.filterNot { it.id in existingIds }
          _uiState.update { state ->
            state.copy(
              isLoadingMoreStreaming = false,
              streamingSearchResults = state.streamingSearchResults + newItems,
              streamingHasMore = page.hasNextPage && page.endCursor != cursor,
              streamingNextCursor = page.endCursor,
            )
          }
          if (newItems.isNotEmpty()) {
            val hydratedById = newItems.map { item ->
              async {
                val url = item.posterUrl
                val poster = if (url != null) {
                  streamingSearchDataSource.downloadPoster(url)
                } else {
                  null
                }
                item.id to poster
              }
            }.map { it.await() }.toMap()
            if (_uiState.value.searchQuery.trim() == trimmed) {
              _uiState.update { state ->
                state.copy(
                  streamingSearchResults = state.streamingSearchResults.map { existing ->
                    val poster = hydratedById[existing.id]
                    if (poster != null && existing.poster == null) existing.copy(poster = poster) else existing
                  },
                )
              }
            }
          }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoadingMoreStreaming = false) }
          showStatus(err.message ?: "Could not load more JustWatch results")
        },
      )
    }
  }

  fun openStreamingOffer(offer: StreamingOffer) {
    if (appDataSource.openStreamingOffer(offer)) {
      toggleSearch()
    } else {
      showStatus("Could not open ${offer.providerName}")
    }
  }

  fun openJellyfinSearchItem(item: JellyfinSearchItem) {
    val serverUrl = _uiState.value.settings.jellyfinServerUrl
    if (appDataSource.openJellyfinItem(serverUrl, item.itemId)) {
      toggleSearch()
    } else {
      showStatus("Could not open ${item.title} in Jellyfin")
    }
  }

  fun openYouTubeSearch(query: String) {
    val q = query.trim()
    if (q.isEmpty()) return
    if (appDataSource.openYouTubeSearch(q)) {
      toggleSearch()
    } else {
      showStatus("No YouTube or browser app available")
    }
  }

  fun openWebSearch(query: String) {
    val q = query.trim()
    if (q.isEmpty()) return
    if (appDataSource.openWebSearch(q)) {
      toggleSearch()
    } else {
      showStatus("No web browser app available")
    }
  }

  fun openExternalProviderSearch(provider: ExternalSearchProvider, query: String) {
    val q = query.trim()
    if (q.isEmpty()) return
    if (appDataSource.openExternalProviderSearch(provider, q)) {
      toggleSearch()
    } else {
      showStatus("Could not open search in ${provider.label}")
    }
  }

  fun openSettings() {
    val s = _uiState.value.settings
    if (s.isPinEnabled && s.pinProtectSettings) {
      _uiState.update {
        it.copy(
          menuTarget = null,
          reorderingTarget = null,
          pinPrompt = PinPromptState(
            mode = PinPromptMode.VERIFY,
            title = "Customize Locked",
            subtitle = "Enter 4-digit PIN to open Customize",
            pendingAction = PinPendingAction.OpenSettings,
          ),
        )
      }
      return
    }
    _uiState.update {
      it.copy(
        isSettingsOpen = true,
        menuTarget = null,
        reorderingTarget = null,
      )
    }
  }

  fun closeSettings() {
    _uiState.update { it.copy(isSettingsOpen = false) }
  }

  // --- PIN Lock (Parental Controls) ---

  fun requestSetPin() {
    _uiState.update {
      it.copy(
        pinPrompt = PinPromptState(
          mode = PinPromptMode.SET_NEW,
          title = "Set 4-Digit PIN",
          subtitle = "Choose a 4-digit PIN for parental controls",
          pendingAction = PinPendingAction.SetNewPin,
        ),
      )
    }
  }

  fun requestChangePin() {
    if (!_uiState.value.settings.isPinEnabled) {
      requestSetPin()
      return
    }
    _uiState.update {
      it.copy(
        pinPrompt = PinPromptState(
          mode = PinPromptMode.VERIFY,
          title = "Change PIN",
          subtitle = "Enter your current 4-digit PIN first",
          pendingAction = PinPendingAction.ChangePin,
        ),
      )
    }
  }

  fun requestRemovePin() {
    if (!_uiState.value.settings.isPinEnabled) return
    _uiState.update {
      it.copy(
        pinPrompt = PinPromptState(
          mode = PinPromptMode.VERIFY,
          title = "Remove PIN Lock",
          subtitle = "Enter your 4-digit PIN to turn off PIN Lock",
          pendingAction = PinPendingAction.RemovePin,
        ),
      )
    }
  }

  fun togglePinProtectSettings() {
    val next = !_uiState.value.settings.pinProtectSettings
    updateSettings { it.copy(pinProtectSettings = next) }
    showStatus(if (next) "PIN required to open Customize" else "Customize unlocked without PIN")
  }

  fun toggleAppPinLock(app: AppInfo) {
    val state = _uiState.value
    val s = state.settings
    if (!s.isPinEnabled) {
      showStatus("Set a 4-digit PIN in Customize → Parental Controls first")
      return
    }
    val currentlyLocked = app.packageName in s.pinLockedPackages
    if (currentlyLocked && !state.isSettingsOpen) {
      _uiState.update {
        it.copy(
          menuTarget = null,
          pinPrompt = PinPromptState(
            mode = PinPromptMode.VERIFY,
            title = "Unlock App",
            subtitle = "Enter PIN to remove lock from ${app.label}",
            pendingAction = PinPendingAction.ToggleAppLock(app),
          ),
        )
      }
      return
    }
    updateSettings {
      val updated = if (currentlyLocked) {
        it.pinLockedPackages - app.packageName
      } else {
        it.pinLockedPackages + app.packageName
      }
      it.copy(pinLockedPackages = updated)
    }
    _uiState.update { it.copy(menuTarget = null) }
    showStatus(if (currentlyLocked) "${app.label} unlocked" else "${app.label} locked with PIN")
  }

  fun unlockAppPin(packageName: String) {
    val label = _uiState.value.allApps.firstOrNull { it.packageName == packageName }?.label ?: "App"
    updateSettings { it.copy(pinLockedPackages = it.pinLockedPackages - packageName) }
    showStatus("$label unlocked")
  }

  fun onPinVerified() {
    val prompt = _uiState.value.pinPrompt ?: return
    _uiState.update { it.copy(pinPrompt = null) }
    when (val action = prompt.pendingAction) {
      PinPendingAction.OpenSettings -> {
        _uiState.update {
          it.copy(
            isSettingsOpen = true,
            menuTarget = null,
            reorderingTarget = null,
          )
        }
      }
      is PinPendingAction.LaunchApp -> {
        if (!appDataSource.launchApp(action.app)) {
          showStatus("Unable to launch ${action.app.label}")
        }
      }
      is PinPendingAction.ToggleAppLock -> {
        updateSettings { it.copy(pinLockedPackages = it.pinLockedPackages - action.app.packageName) }
        showStatus("${action.app.label} unlocked")
      }
      PinPendingAction.ChangePin -> {
        _uiState.update {
          it.copy(
            pinPrompt = PinPromptState(
              mode = PinPromptMode.SET_NEW,
              title = "New 4-Digit PIN",
              subtitle = "Enter your new 4-digit PIN",
              pendingAction = PinPendingAction.SetNewPin,
            ),
          )
        }
      }
      PinPendingAction.RemovePin -> {
        updateSettings {
          it.copy(
            parentalPin = "",
            pinLockedPackages = emptySet(),
          )
        }
        showStatus("PIN Lock disabled")
      }
      PinPendingAction.SetNewPin -> Unit
    }
  }

  fun onNewPinConfirmed(newPin: String) {
    val clean = newPin.filter { it.isDigit() }.take(4)
    if (clean.length != 4) return
    updateSettings { it.copy(parentalPin = clean) }
    _uiState.update { it.copy(pinPrompt = null) }
    showStatus("4-digit PIN saved")
  }

  fun dismissPinPrompt() {
    _uiState.update { it.copy(pinPrompt = null) }
  }

  fun selectTheme(themeId: String) {
    stopJellyfinSlideshow()
    updateSettings {
      it.copy(
        themeId = themeId,
        customWallpaperUri = null,
        jellyfinEnabled = false,
      )
    }
  }

  fun selectAccent(accentId: String) {
    updateSettings { it.copy(accentId = accentId) }
  }

  fun setCustomWallpaper(uriString: String?) {
    if (uriString != null) {
      stopJellyfinSlideshow()
    }
    updateSettings {
      it.copy(
        customWallpaperUri = uriString,
        jellyfinEnabled = if (uriString != null) false else it.jellyfinEnabled,
      )
    }
    if (uriString != null) {
      showStatus("Custom wallpaper applied")
    } else {
      showStatus("Switched back to ambient theme")
    }
  }

  fun setWallpaperDimAlpha(alpha: Float) {
    updateSettings { it.copy(wallpaperDimAlpha = alpha.coerceIn(0.20f, 0.85f)) }
  }

  fun toggleWallpaperKenBurns() {
    val next = !_uiState.value.settings.wallpaperKenBurns
    updateSettings { it.copy(wallpaperKenBurns = next) }
    showStatus(if (next) "Wallpaper Ken Burns motion enabled" else "Wallpaper Ken Burns motion off")
  }

  fun toggle24HourClock() {
    updateSettings { it.copy(use24HourClock = !it.use24HourClock) }
  }

  fun toggleShowClock() {
    updateSettings { it.copy(showClock = !it.showClock) }
  }

  fun toggleShowDate() {
    updateSettings { it.copy(showDate = !it.showDate) }
  }

  fun toggleLockHomeLayout() {
    val nextLocked = !_uiState.value.settings.lockHomeLayout
    updateSettings { it.copy(lockHomeLayout = nextLocked) }
    if (nextLocked) {
      _uiState.update { it.copy(reorderingTarget = null) }
    }
    showStatus(if (nextLocked) "Home layout locked" else "Home layout unlocked")
  }

  fun setHomeTileSize(sizeId: String) {
    val resolved = HomeTileSize.fromId(sizeId)
    updateSettings { it.copy(homeTileSize = resolved.id) }
  }

  fun toggleMinimalistDockMode() {
    val next = !_uiState.value.settings.minimalistDockMode
    updateSettings { it.copy(minimalistDockMode = next) }
    showStatus(if (next) "Minimalist Bottom-Dock enabled" else "Standard Home dock enabled")
  }

  fun setMinimalistDockMode(enabled: Boolean) {
    updateSettings { it.copy(minimalistDockMode = enabled) }
  }

  fun set24HourClock(enabled: Boolean) {
    updateSettings { it.copy(use24HourClock = enabled) }
  }

  fun openSetupWizard() {
    _uiState.update {
      it.copy(
        isSettingsOpen = false,
        isSetupWizardOpen = true,
        menuTarget = null,
        reorderingTarget = null,
      )
    }
  }

  fun completeSetupWizard() {
    updateSettings { it.copy(hasCompletedSetupWizard = true) }
    _uiState.update { it.copy(isSetupWizardOpen = false) }
  }

  private var webCompanionServer: LocalWebCompanionServer? = null

  fun startWebCompanion() {
    stopWebCompanion()
    val bridge = object : WebCompanionBridge {
      override fun getSettings(): LauncherSettings = _uiState.value.settings
      override fun getApps(): List<AppInfo> = _uiState.value.allVisibleApps
      override fun getAllInstalledApps(): List<AppInfo> = _uiState.value.allApps
      override fun removeUninstalledFromRow(rowId: String) {
        val installedPkgs = _uiState.value.allApps.map { it.packageName }.toSet()
        updateSettings { s ->
          s.copy(
            rows = s.rows.map { r ->
              if (r.id == rowId) r.copy(packageNames = r.packageNames.filter { it in installedPkgs }) else r
            },
          )
        }
      }
      override fun exportBackupJson(): String = this@LauncherViewModel.exportBackupJson()
      override fun restoreBackupJson(raw: String): Boolean {
        val check = backupSerializer.decodeFromJson(raw, existing = _uiState.value.settings)
        if (check.isFailure) return false
        this@LauncherViewModel.restoreBackupJson(raw)
        return true
      }
      override fun saveServerConfig(provider: String, url: String, token: String) {
        when (provider) {
          "jellyfin" -> updateSettings {
            it.copy(
              jellyfinServerUrl = url,
              jellyfinApiKey = token,
              wallpaperProvider = MediaWallpaperProvider.JELLYFIN.id,
              jellyfinEnabled = url.isNotBlank() && token.isNotBlank(),
            )
          }
          "plex" -> updateSettings {
            it.copy(
              plexServerUrl = url,
              plexToken = token,
              wallpaperProvider = MediaWallpaperProvider.PLEX.id,
              jellyfinEnabled = url.isNotBlank() && token.isNotBlank(),
            )
          }
          "emby" -> updateSettings {
            it.copy(
              embyServerUrl = url,
              embyApiKey = token,
              wallpaperProvider = MediaWallpaperProvider.EMBY.id,
              jellyfinEnabled = url.isNotBlank() && token.isNotBlank(),
            )
          }
          "tmdb" -> updateSettings {
            it.copy(
              tmdbApiKey = token,
              wallpaperProvider = MediaWallpaperProvider.TMDB.id,
              jellyfinEnabled = token.isNotBlank(),
            )
          }
        }
        val s = _uiState.value.settings
        if (s.jellyfinEnabled && s.isActiveWallpaperProviderConfigured) {
          startJellyfinSlideshow(fetchFreshList = true)
        }
      }
      override fun updateHomeOptions(
        minimalistDock: Boolean?,
        use24HourClock: Boolean?,
        showTileTitles: Boolean?,
        tileOpacity: Float?,
        tileSpacing: String?,
        regionCode: String?,
      ) {
        updateSettings { s ->
          s.copy(
            minimalistDockMode = minimalistDock ?: s.minimalistDockMode,
            use24HourClock = use24HourClock ?: s.use24HourClock,
            showTileTitles = showTileTitles ?: s.showTileTitles,
            tileOpacity = tileOpacity?.coerceIn(0.35f, 1.0f) ?: s.tileOpacity,
            tileSpacing = tileSpacing ?: s.tileSpacing,
            justWatchRegion = regionCode ?: s.justWatchRegion,
          )
        }
      }
      override fun renameRow(rowId: String, newTitle: String) = this@LauncherViewModel.renameRow(rowId, newTitle)
      override fun createRow(title: String) = this@LauncherViewModel.createRow(title, openAppPickerAfter = false)
      override fun moveRow(rowId: String, delta: Int) = moveRowInternal(rowId, delta, bypassLayoutLock = true)
      override fun deleteRow(rowId: String) = deleteRowInternal(rowId, bypassLayoutLock = true)
      override fun toggleAppInRow(rowId: String, packageName: String) =
        toggleAppInRowInternal(rowId, packageName, bypassLayoutLock = true)
      override fun moveAppInRow(rowId: String, packageName: String, delta: Int) =
        moveAppInRowInternal(rowId, packageName, delta, bypassLayoutLock = true)
      override fun setRowAppsOrder(rowId: String, packageNames: List<String>) {
        val cleaned = packageNames.distinct()
        updateSettings { s ->
          s.copy(
            rows = s.rows.map { r ->
              if (r.id == rowId) r.copy(packageNames = cleaned) else r
            },
          )
        }
      }
      override fun sortRowAppsAlphabetically(rowId: String) =
        sortRowAppsAlphabeticallyInternal(rowId, bypassLayoutLock = true)
      override fun updateWallpaperOptions(
        themeId: String?,
        accentId: String?,
        providerId: String?,
        slideshowEnabled: Boolean?,
        dimAlpha: Float?,
        intervalSeconds: Int?,
        showTitle: Boolean?,
        clearCustomWallpaper: Boolean,
      ) {
        if (clearCustomWallpaper) {
          setCustomWallpaper(null)
          return
        }
        if (themeId != null) {
          selectTheme(themeId)
          return
        }
        if (accentId != null) {
          selectAccent(accentId)
        }
        val providerChanged = providerId != null && providerId != _uiState.value.settings.wallpaperProvider
        if (providerChanged) {
          selectWallpaperProvider(MediaWallpaperProvider.fromId(providerId))
        }
        updateSettings { s ->
          val activeProv = s.activeWallpaperProvider
          val withCfg = s.withProviderConfig(activeProv) { cfg ->
            cfg.copy(
              intervalSeconds = intervalSeconds?.coerceIn(10, 600) ?: cfg.intervalSeconds,
              showTitle = showTitle ?: cfg.showTitle,
            )
          }
          withCfg.copy(
            wallpaperDimAlpha = dimAlpha?.coerceIn(0.20f, 0.85f) ?: withCfg.wallpaperDimAlpha,
            jellyfinEnabled = if (!providerChanged && slideshowEnabled != null) {
              slideshowEnabled && withCfg.isActiveWallpaperProviderConfigured
            } else {
              withCfg.jellyfinEnabled
            },
          )
        }
        val latest = _uiState.value.settings
        if (latest.jellyfinEnabled && latest.isActiveWallpaperProviderConfigured) {
          startJellyfinSlideshow(fetchFreshList = providerChanged)
        } else if (slideshowEnabled == false) {
          stopJellyfinSlideshow()
        }
      }
      override fun updateScreensaverOptions(
        enabled: Boolean?,
        modeId: String?,
        timeoutMinutes: Int?,
        intervalSeconds: Int?,
        dimAlpha: Float?,
        kenBurns: Boolean?,
        showClock: Boolean?,
        showWeather: Boolean?,
        showTitle: Boolean?,
        burnInShift: Boolean?,
        ambientCycle: Boolean?,
      ) {
        updateSettings { s ->
          s.copy(
            screensaverEnabled = enabled ?: s.screensaverEnabled,
            screensaverMode = modeId ?: s.screensaverMode,
            screensaverTimeoutMinutes = timeoutMinutes?.coerceIn(1, 60) ?: s.screensaverTimeoutMinutes,
            screensaverIntervalSeconds = intervalSeconds?.coerceIn(5, 120) ?: s.screensaverIntervalSeconds,
            screensaverDimAlpha = dimAlpha?.coerceIn(0.0f, 0.75f) ?: s.screensaverDimAlpha,
            screensaverKenBurns = kenBurns ?: s.screensaverKenBurns,
            screensaverShowClock = showClock ?: s.screensaverShowClock,
            screensaverShowWeather = showWeather ?: s.screensaverShowWeather,
            screensaverShowTitle = showTitle ?: s.screensaverShowTitle,
            screensaverBurnInShift = burnInShift ?: s.screensaverBurnInShift,
            screensaverAmbientCycle = ambientCycle ?: s.screensaverAmbientCycle,
          )
        }
      }
      override fun searchWeatherLocationsSync(query: String): List<WeatherLocation> =
        kotlinx.coroutines.runBlocking {
          weatherDataSource.searchLocations(query).getOrDefault(emptyList())
        }
      override fun selectWeatherLocation(location: WeatherLocation) =
        this@LauncherViewModel.selectWeatherLocation(location)
      override fun updateWeatherOptions(
        enabled: Boolean?,
        useFahrenheit: Boolean?,
        showForecast: Boolean?,
        days: Int?,
        forecastSize: String?,
        iconStyle: String?,
        showBackdrop: Boolean?,
      ) {
        val unitChanged = useFahrenheit != null && useFahrenheit != _uiState.value.settings.weatherUseFahrenheit
        updateSettings { s ->
          s.copy(
            weatherEnabled = if (enabled != null) enabled && s.isWeatherConfigured else s.weatherEnabled,
            weatherUseFahrenheit = useFahrenheit ?: s.weatherUseFahrenheit,
            weatherShowForecast = showForecast ?: s.weatherShowForecast,
            weatherDays = days?.coerceIn(1, 7) ?: s.weatherDays,
            weatherForecastSize = forecastSize ?: s.weatherForecastSize,
            weatherIconStyle = iconStyle ?: s.weatherIconStyle,
            weatherShowBackdrop = showBackdrop ?: s.weatherShowBackdrop,
          )
        }
        val latest = _uiState.value.settings
        if (latest.weatherEnabled && latest.isWeatherConfigured && (unitChanged || enabled == true)) {
          startWeatherRefreshLoop()
        } else if (enabled == false) {
          weatherJob?.cancel()
          weatherJob = null
        }
      }
      override fun saveUploadedWallpaper(bytes: ByteArray): Boolean {
        val uri = appDataSource.saveCustomWallpaperBytes(bytes) ?: return false
        setCustomWallpaper(uri)
        return true
      }
      override fun saveUploadedAppIcon(packageName: String, bytes: ByteArray): Boolean {
        val savedPath = appDataSource.saveCustomIconBytes(packageName, bytes) ?: return false
        updateSettings { s ->
          s.copy(customIconPaths = s.customIconPaths + (packageName to savedPath))
        }
        refreshApps()
        return true
      }
      override fun onCompanionActivity(message: String) {
        _uiState.update { st ->
          st.copy(webCompanionInfo = st.webCompanionInfo?.copy(lastActivity = message))
        }
      }
    }

    val server = LocalWebCompanionServer(viewModelScope, bridge)
    server.start().fold(
      onSuccess = { (url, qrUrl, code) ->
        webCompanionServer = server
        _uiState.update {
          it.copy(
            isSettingsOpen = false,
            webCompanionInfo = WebCompanionInfo(
              url = url,
              qrUrl = qrUrl,
              sessionCode = code,
            ),
          )
        }
      },
      onFailure = { err ->
        showStatus(err.message ?: "Could not start Local Web Companion")
      },
    )
  }

  fun stopWebCompanion() {
    webCompanionServer?.stop()
    webCompanionServer = null
    _uiState.update { it.copy(webCompanionInfo = null) }
  }

  override fun onCleared() {
    stopWebCompanion()
    super.onCleared()
  }

  fun enableRedditWallpapersFromWizard() {
    selectWallpaperProvider(MediaWallpaperProvider.REDDIT)
    if (!_uiState.value.settings.jellyfinEnabled) {
      toggleJellyfinSlideshow()
    }
  }

  fun setTileOpacity(opacity: Float) {
    val clamped = opacity.coerceIn(0.35f, 1.0f)
    updateSettings { it.copy(tileOpacity = clamped) }
  }

  fun setTileSpacing(spacingId: String) {
    val resolved = HomeTileSpacing.fromId(spacingId)
    updateSettings { it.copy(tileSpacing = resolved.id) }
  }

  fun setTileCornerStyle(styleId: String) {
    val resolved = TileCornerStyle.fromId(styleId)
    updateSettings { it.copy(tileCornerStyle = resolved.id) }
  }

  fun setTileFocusStyle(styleId: String) {
    val resolved = TileFocusStyle.fromId(styleId)
    updateSettings { it.copy(tileFocusStyle = resolved.id) }
  }

  fun setClockStyle(styleId: String) {
    val resolved = ClockStyle.fromId(styleId)
    updateSettings { it.copy(clockStyle = resolved.id) }
  }

  fun setClockFontStyle(styleId: String) {
    val resolved = ClockFontStyle.fromId(styleId)
    updateSettings { it.copy(clockFontStyle = resolved.id) }
  }

  fun setClockFontWeight(weightId: String) {
    val resolved = ClockFontWeight.fromId(weightId)
    updateSettings { it.copy(clockFontWeight = resolved.id) }
  }

  fun cycleRowTileShape(rowId: String) {
    val row = _uiState.value.settings.rows.firstOrNull { it.id == rowId } ?: return
    val next = TileAspectShape.fromId(row.tileShape).next()
    setRowTileShape(rowId, next.id)
    showStatus("${row.title} shape: ${next.label}")
  }

  fun setRowTileShape(rowId: String, shapeId: String) {
    val resolved = TileAspectShape.fromId(shapeId)
    updateSettings { s ->
      s.copy(
        rows = s.rows.map { r ->
          if (r.id == rowId) r.copy(tileShape = resolved.id) else r
        },
      )
    }
  }

  fun toggleShowNonLeanbackApps() {
    updateSettings { it.copy(showNonLeanbackApps = !it.showNonLeanbackApps) }
  }

  fun toggleShowSearchBar() {
    val nextShow = !_uiState.value.settings.showSearchBar
    updateSettings { it.copy(showSearchBar = nextShow) }
    if (!nextShow && _uiState.value.isSearchVisible) {
      toggleSearch()
    }
  }

  fun toggleSearchOpenDetailsPopup() {
    val next = !_uiState.value.settings.searchOpenDetailsPopup
    updateSettings { it.copy(searchOpenDetailsPopup = next) }
    showStatus(if (next) "Search results open full details popup" else "Search results launch streaming apps directly")
  }

  fun commitRecentSearch(rawQuery: String) {
    val trimmed = rawQuery.trim()
    if (trimmed.length < 2) return
    updateSettings { current ->
      val deduped = current.recentSearches.filterNot { it.equals(trimmed, ignoreCase = true) }
      current.copy(recentSearches = (listOf(trimmed) + deduped).take(8))
    }
  }

  fun clearRecentSearches() {
    updateSettings { it.copy(recentSearches = emptyList()) }
    showStatus("Recent searches cleared")
  }

  fun toggleShowAllAppsSection() {
    updateSettings { it.copy(showAllAppsSection = !it.showAllAppsSection) }
  }

  fun toggleShowAddAppTile() {
    val next = !_uiState.value.settings.showAddAppTile
    updateSettings { it.copy(showAddAppTile = next) }
    showStatus(if (next) "\"+ Add App\" tiles shown on rows" else "\"+ Add App\" tiles hidden (rows locked)")
  }

  fun toggleShowTileTitles() {
    val next = !_uiState.value.settings.showTileTitles
    updateSettings { it.copy(showTileTitles = next) }
    showStatus(if (next) "App titles shown under tiles" else "App titles hidden under tiles")
  }

  fun toggleShowTvInputsButton() {
    val next = !_uiState.value.settings.showTvInputsButton
    updateSettings { it.copy(showTvInputsButton = next) }
    showStatus(if (next) "TV Inputs (HDMI) button enabled" else "TV Inputs (HDMI) button hidden")
  }

  fun openTvInputsDialog() {
    val inputs = appDataSource.loadTvInputs()
    _uiState.update {
      it.copy(
        tvInputs = inputs,
        isTvInputsDialogOpen = true,
        isSettingsOpen = false,
      )
    }
  }

  fun closeTvInputsDialog() {
    _uiState.update { it.copy(isTvInputsDialogOpen = false) }
  }

  fun selectTvInput(input: TvInputSource) {
    _uiState.update { it.copy(isTvInputsDialogOpen = false) }
    if (!appDataSource.openTvInput(input)) {
      showStatus("Unable to switch to ${input.label}")
    }
  }

  fun toggleSearchProvider(providerId: String) {
    updateSettings { settings ->
      val updated = if (providerId in settings.disabledSearchProviders) {
        settings.disabledSearchProviders - providerId
      } else {
        settings.disabledSearchProviders + providerId
      }
      settings.copy(disabledSearchProviders = updated)
    }
  }

  fun toggleJustWatchFilterServices() {
    val next = !_uiState.value.settings.justWatchFilterServices
    updateSettings { it.copy(justWatchFilterServices = next) }
    showStatus(if (next) "JustWatch filtered to your selected services" else "Showing all JustWatch streaming services")
  }

  fun toggleJustWatchService(serviceId: String) {
    updateSettings { settings ->
      val updated = if (serviceId in settings.justWatchServices) {
        settings.justWatchServices - serviceId
      } else {
        settings.justWatchServices + serviceId
      }
      settings.copy(
        justWatchServices = updated,
        justWatchFilterServices = true,
      )
    }
  }

  fun selectAllJustWatchServices() {
    val allIds = JustWatchServicePreset.entries.map { it.id }.toSet()
    updateSettings {
      it.copy(
        justWatchServices = allIds,
        justWatchFilterServices = true,
      )
    }
    showStatus("All streaming services selected")
  }

  fun clearAllJustWatchServices() {
    updateSettings {
      it.copy(justWatchServices = emptySet())
    }
    showStatus("Cleared selected streaming services")
  }

  fun toggleJustWatchIncludeRentBuy() {
    val next = !_uiState.value.settings.justWatchIncludeRentBuy
    updateSettings { it.copy(justWatchIncludeRentBuy = next) }
    showStatus(if (next) "Rent & Buy offers included" else "Only showing subscription & free offers")
  }

  fun toggleJustWatchHideUnmatchedTitles() {
    val next = !_uiState.value.settings.justWatchHideUnmatchedTitles
    updateSettings { it.copy(justWatchHideUnmatchedTitles = next) }
    showStatus(if (next) "Hiding titles without offers on your services" else "Showing all matching titles")
  }

  fun setJustWatchRegion(region: JustWatchRegion) {
    updateSettings { it.copy(justWatchRegion = region.code) }
    showStatus("Region set to ${region.label} (${region.code})")
  }

  fun toggleJustWatchShowRottenTomatoes() {
    val next = !_uiState.value.settings.justWatchShowRottenTomatoes
    updateSettings { it.copy(justWatchShowRottenTomatoes = next) }
    showStatus(if (next) "Rotten Tomatoes ratings shown" else "Rotten Tomatoes ratings hidden")
  }

  fun toggleJustWatchShowAgeRating() {
    val next = !_uiState.value.settings.justWatchShowAgeRating
    updateSettings { it.copy(justWatchShowAgeRating = next) }
    showStatus(if (next) "Age ratings shown" else "Age ratings hidden")
  }

  fun setGridColumns(columns: Int) {
    updateSettings { it.copy(gridColumns = columns.coerceIn(4, 6)) }
  }

  fun openAppInfo(packageName: String) {
    closeAppMenu()
    if (!appDataSource.openAppInfo(packageName)) {
      showStatus("App Info not available on this device")
    }
  }

  fun uninstallApp(packageName: String) {
    closeAppMenu()
    if (!appDataSource.uninstallApp(packageName)) {
      showStatus("Unable to open uninstall prompt")
    }
  }

  fun openWifiSettings() {
    if (!appDataSource.openWifiSettings()) {
      showStatus("Unable to open Wi-Fi settings")
    }
  }

  fun openBluetoothSettings() {
    if (!appDataSource.openBluetoothSettings()) {
      showStatus("Unable to open Bluetooth settings")
    }
  }

  fun openSystemSettings() {
    if (!appDataSource.openSystemSettings()) {
      showStatus("Unable to open System Settings")
    }
  }

  fun openHomeSettings() {
    if (!appDataSource.openHomeSettings()) {
      showStatus("Open Android Settings → Apps → Default apps to set Horizon as Home")
    }
  }

  // --- Screensaver ---

  fun startScreensaver() {
    val s = _uiState.value.settings
    _uiState.update {
      it.copy(
        isScreensaverActive = true,
        isSettingsOpen = false,
        isSearchVisible = false,
        menuTarget = null,
      )
    }
    when (s.activeScreensaverMode) {
      ScreensaverMode.PHOTOS -> startScreensaverPhotoLoop()
      ScreensaverMode.WALLPAPER_ARTWORK -> {
        if (s.isActiveWallpaperProviderConfigured && (_uiState.value.jellyfinSlide == null || slideshowJob?.isActive != true)) {
          startJellyfinSlideshow(fetchFreshList = cachedBackdrops.isEmpty())
        }
      }
      ScreensaverMode.AMBIENT -> Unit
    }
  }

  fun stopScreensaver() {
    screensaverPhotoJob?.cancel()
    screensaverPhotoJob = null
    _uiState.update { it.copy(isScreensaverActive = false) }
    val s = _uiState.value.settings
    if (!s.jellyfinEnabled) {
      slideshowJob?.cancel()
      slideshowJob = null
    }
  }

  fun toggleScreensaverEnabled() {
    val willEnable = !_uiState.value.settings.screensaverEnabled
    updateSettings { it.copy(screensaverEnabled = willEnable) }
    showStatus(if (willEnable) "Idle screensaver enabled" else "Idle screensaver turned off")
  }

  fun setScreensaverMode(mode: ScreensaverMode) {
    updateSettings { it.copy(screensaverMode = mode.id) }
  }

  fun setScreensaverTimeoutMinutes(minutes: Int) {
    updateSettings { it.copy(screensaverTimeoutMinutes = minutes.coerceIn(1, 60)) }
  }

  fun setScreensaverIntervalSeconds(seconds: Int) {
    updateSettings { it.copy(screensaverIntervalSeconds = seconds.coerceIn(5, 300)) }
  }

  fun toggleScreensaverKenBurns() {
    updateSettings { it.copy(screensaverKenBurns = !it.screensaverKenBurns) }
  }

  fun toggleScreensaverShowClock() {
    updateSettings { it.copy(screensaverShowClock = !it.screensaverShowClock) }
  }

  fun toggleScreensaverShowWeather() {
    updateSettings { it.copy(screensaverShowWeather = !it.screensaverShowWeather) }
  }

  fun toggleScreensaverShowTitle() {
    updateSettings { it.copy(screensaverShowTitle = !it.screensaverShowTitle) }
  }

  fun toggleScreensaverBurnInShift() {
    updateSettings { it.copy(screensaverBurnInShift = !it.screensaverBurnInShift) }
  }

  fun setScreensaverDimAlpha(alpha: Float) {
    updateSettings { it.copy(screensaverDimAlpha = alpha.coerceIn(0f, 0.75f)) }
  }

  fun toggleScreensaverAmbientCycle() {
    updateSettings { it.copy(screensaverAmbientCycle = !it.screensaverAmbientCycle) }
  }

  fun setScreensaverPhotoFolder(uri: String?) {
    updateSettings { it.copy(screensaverPhotoFolderUri = uri) }
    refreshScreensaverPhotoCount(showToast = true)
  }

  fun setScreensaverPhotoUris(uris: List<String>) {
    val cleaned = uris.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    updateSettings { it.copy(screensaverPhotoUris = cleaned) }
    refreshScreensaverPhotoCount(showToast = true)
  }

  fun clearScreensaverPhotos() {
    screensaverPhotoJob?.cancel()
    cachedPhotoEntries = emptyList()
    currentPhotoIndex = 0
    updateSettings {
      it.copy(
        screensaverPhotoFolderUri = null,
        screensaverPhotoUris = emptyList(),
      )
    }
    _uiState.update { it.copy(screensaverPhotoSlide = null, screensaverPhotoCount = 0) }
    showStatus("Screensaver photos cleared")
  }

  private fun refreshScreensaverPhotoCount(showToast: Boolean = false) {
    val source = screensaverPhotoSource ?: return
    val s = _uiState.value.settings
    if (s.screensaverPhotoFolderUri.isNullOrBlank() && s.screensaverPhotoUris.isEmpty()) {
      cachedPhotoEntries = emptyList()
      _uiState.update { it.copy(screensaverPhotoCount = 0, screensaverPhotoSlide = null) }
      return
    }
    viewModelScope.launch {
      val entries = source.listPhotos(s.screensaverPhotoFolderUri, s.screensaverPhotoUris)
      cachedPhotoEntries = entries.shuffled()
      currentPhotoIndex = 0
      _uiState.update { it.copy(screensaverPhotoCount = entries.size) }
      if (showToast) {
        if (entries.isEmpty()) {
          showStatus("No images found in selection")
        } else {
          showStatus("Loaded ${entries.size} photo${if (entries.size == 1) "" else "s"} for screensaver")
        }
      }
    }
  }

  private fun startScreensaverPhotoLoop() {
    val source = screensaverPhotoSource ?: return
    screensaverPhotoJob?.cancel()
    screensaverPhotoJob = viewModelScope.launch {
      val s = _uiState.value.settings
      if (cachedPhotoEntries.isEmpty()) {
        cachedPhotoEntries = source.listPhotos(s.screensaverPhotoFolderUri, s.screensaverPhotoUris).shuffled()
        _uiState.update { it.copy(screensaverPhotoCount = cachedPhotoEntries.size) }
      }
      if (cachedPhotoEntries.isEmpty()) return@launch

      while (isActive && _uiState.value.isScreensaverActive) {
        val entry = cachedPhotoEntries[currentPhotoIndex % cachedPhotoEntries.size]
        currentPhotoIndex = (currentPhotoIndex + 1) % cachedPhotoEntries.size
        val slide = source.loadPhoto(entry)
        if (slide != null) {
          _uiState.update { it.copy(screensaverPhotoSlide = slide) }
          val waitMs = (_uiState.value.settings.screensaverIntervalSeconds.coerceIn(5, 300) * 1_000L)
          delay(waitMs)
        } else {
          delay(350L)
        }
      }
    }
  }

  // --- GitHub Self-Updater ---

  fun setUpdateCheckSchedule(schedule: UpdateCheckSchedule) {
    updateSettings { it.copy(updateCheckSchedule = schedule.id) }
    showStatus("Update checks: ${schedule.label}")
  }

  fun checkForUpdates(manual: Boolean = true, showDialogIfAvailable: Boolean = true) {
    val updater = appUpdateDataSource ?: return
    if (_uiState.value.isCheckingForUpdate || _uiState.value.isDownloadingUpdate) return
    viewModelScope.launch {
      _uiState.update { it.copy(isCheckingForUpdate = true, updateStatusText = null) }
      updater.fetchLatestRelease(currentVersionName).fold(
        onSuccess = { release ->
          updateSettings { it.copy(lastUpdateCheckMillis = System.currentTimeMillis()) }
          val isNewer = release != null &&
            release.apkUrl.isNotEmpty() &&
            GitHubUpdater.isVersionNewer(release.versionName, currentVersionName)
          if (isNewer && release != null) {
            _uiState.update {
              it.copy(
                isCheckingForUpdate = false,
                latestRelease = release,
                availableUpdate = release,
                showUpdateDialog = showDialogIfAvailable,
                updateStatusText = null,
              )
            }
          } else {
            _uiState.update {
              it.copy(
                isCheckingForUpdate = false,
                latestRelease = release ?: it.latestRelease,
                availableUpdate = null,
                showUpdateDialog = false,
                updateStatusText = if (manual) "You're up to date (v$currentVersionName)" else null,
              )
            }
            if (manual) {
              showStatus("Horizon TV Launcher is up to date (v$currentVersionName)")
            }
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isCheckingForUpdate = false,
              updateStatusText = if (manual) (err.message ?: "Could not check GitHub for updates") else null,
            )
          }
          if (manual) {
            showStatus(err.message ?: "Could not check GitHub for updates")
          }
        },
      )
    }
  }

  fun loadLatestReleaseInfoIfNeeded() {
    if (_uiState.value.latestRelease == null && !_uiState.value.isCheckingForUpdate) {
      checkForUpdates(manual = false, showDialogIfAvailable = false)
    }
  }

  fun openUpdateDialog() {
    if (_uiState.value.availableUpdate != null) {
      _uiState.update { it.copy(showUpdateDialog = true, updateStatusText = null) }
    }
  }

  fun dismissUpdateDialog() {
    if (_uiState.value.isDownloadingUpdate) return
    _uiState.update { it.copy(showUpdateDialog = false, updateStatusText = null) }
  }

  fun installAvailableUpdate() {
    val updater = appUpdateDataSource ?: return
    val update = _uiState.value.availableUpdate ?: return
    if (_uiState.value.isDownloadingUpdate) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isDownloadingUpdate = true,
          updateDownloadProgress = 0f,
          updateStatusText = null,
        )
      }
      updater.downloadAndInstall(
        update = update,
        onProgress = { progress ->
          _uiState.update { it.copy(updateDownloadProgress = progress) }
        },
      ).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isDownloadingUpdate = false,
              showUpdateDialog = false,
            )
          }
        },
        onFailure = { err ->
          val msg = err.message ?: "Could not download update"
          _uiState.update {
            it.copy(
              isDownloadingUpdate = false,
              updateStatusText = msg,
            )
          }
          showStatus(msg)
        },
      )
    }
  }

  fun openPlayStore() {
    closeSettings()
    if (!appDataSource.openPlayStore()) {
      showStatus("Google Play Store is not installed on this device")
    }
  }

  private fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
    val updated = transform(_uiState.value.settings)
    settingsStore.save(updated)
    _uiState.update { it.copy(settings = updated) }
  }

  private fun showStatus(message: String) {
    statusJob?.cancel()
    _uiState.update { it.copy(statusMessage = message) }
    statusJob = viewModelScope.launch {
      delay(3000)
      _uiState.update { it.copy(statusMessage = null) }
    }
  }

  class Factory(
    private val appDataSource: InstalledAppsDataSource,
    private val settingsStore: SettingsStore,
    private val jellyfinDataSource: JellyfinDataSource = JellyfinClient(),
    private val weatherDataSource: WeatherDataSource = WeatherClient(),
    private val screensaverPhotoSource: ScreensaverPhotoSource? = null,
    private val appUpdateDataSource: AppUpdateDataSource? = null,
    private val currentVersionName: String = "2.5.17",
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return LauncherViewModel(
        appDataSource = appDataSource,
        settingsStore = settingsStore,
        jellyfinDataSource = jellyfinDataSource,
        weatherDataSource = weatherDataSource,
        screensaverPhotoSource = screensaverPhotoSource,
        appUpdateDataSource = appUpdateDataSource,
        currentVersionName = currentVersionName,
      ) as T
    }
  }
}
