package com.horizontv.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.horizontv.launcher.data.AndroidScreensaverPhotoSource
import com.horizontv.launcher.data.AppRepository
import com.horizontv.launcher.data.GitHubUpdater
import com.horizontv.launcher.data.LauncherPreferences
import com.horizontv.launcher.ui.LauncherScreen
import com.horizontv.launcher.ui.LauncherViewModel
import com.horizontv.launcher.ui.theme.HorizonTVLauncherTheme
import com.horizontv.launcher.ui.theme.LauncherPalettes

class MainActivity : ComponentActivity() {

  private var pendingIconPackage: String? = null
  private var homeFocusTrigger by mutableIntStateOf(0)

  private val viewModel: LauncherViewModel by viewModels {
    val installedVersion = runCatching {
      packageManager.getPackageInfo(packageName, 0).versionName ?: "2.5.17"
    }.getOrDefault("2.5.17")
    LauncherViewModel.Factory(
      appDataSource = AppRepository(applicationContext),
      settingsStore = LauncherPreferences(applicationContext),
      screensaverPhotoSource = AndroidScreensaverPhotoSource(applicationContext),
      appUpdateDataSource = GitHubUpdater(applicationContext),
      currentVersionName = installedVersion,
    )
  }

  private val cb by lazy { MainCallbacks(this, viewModel) }

  private val wallpaperPicker = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      runCatching {
        contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
      }
      viewModel.setCustomWallpaper(uri.toString())
    }
  }

  private val customIconPicker = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    val targetPkg = pendingIconPackage
    pendingIconPackage = null
    if (uri != null && targetPkg != null) {
      viewModel.setCustomAppIcon(targetPkg, uri)
    }
  }

  private val homeRoleLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
  ) {
    viewModel.onDefaultHomeResult()
  }

  private val backupConfigLauncher = registerForActivityResult(
    ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null) {
      val saved = runCatching {
        val json = viewModel.exportBackupJson()
        contentResolver.openOutputStream(uri)?.use { out ->
          out.write(json.toByteArray(Charsets.UTF_8))
        }
        true
      }.getOrDefault(false)
      viewModel.onBackupExported(saved)
    }
  }

  private val restoreConfigLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      val raw = runCatching {
        contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
      }.getOrNull()
      if (raw != null) {
        viewModel.restoreBackupJson(raw)
      } else {
        viewModel.onBackupExported(false)
      }
    }
  }

  private val screensaverFolderPicker = registerForActivityResult(
    ActivityResultContracts.OpenDocumentTree(),
  ) { uri ->
    if (uri != null) {
      runCatching {
        contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
      }
      viewModel.setScreensaverPhotoFolder(uri.toString())
    }
  }

  private val screensaverPhotosPicker = registerForActivityResult(
    ActivityResultContracts.OpenMultipleDocuments(),
  ) { uris ->
    if (uris.isNotEmpty()) {
      uris.forEach { uri ->
        runCatching {
          contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
      }
      viewModel.setScreensaverPhotoUris(uris.map { it.toString() })
    }
  }

  private val voiceSearchLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    if (result.resultCode == RESULT_OK) {
      val spoken = result.data
        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        ?.firstOrNull { it.isNotBlank() }
        ?.trim()
      if (!spoken.isNullOrEmpty()) {
        viewModel.updateSearchQuery(spoken)
        viewModel.commitRecentSearch(spoken)
      }
    }
  }

  private fun launchVoiceSearch() {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_PROMPT, "Search movies, TV shows & apps")
      putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    val launched = runCatching {
      voiceSearchLauncher.launch(intent)
      true
    }.getOrDefault(false)
    if (!launched) {
      Toast.makeText(this, "Voice search is not available on this device", Toast.LENGTH_SHORT).show()
    }
  }

  private fun openSystemScreensaverSettings() {
    val opened = runCatching {
      startActivity(Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      true
    }.getOrDefault(false)
    if (!opened) {
      viewModel.openSystemSettings()
    }
  }

  private fun requestDefaultHomeLauncher() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(RoleManager::class.java)
      if (roleManager != null &&
        roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
      ) {
        val launched = runCatching {
          homeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
          true
        }.getOrDefault(false)
        if (launched) return
      }
    }
    viewModel.openHomeSettings()
  }

  // Resolution-independent UI: always lay out against a fixed design width so the launcher
  // looks identical on 720p, 1080p and 4K TVs regardless of the device's reported density.
  override fun attachBaseContext(newBase: Context) {
    val metrics = newBase.resources.displayMetrics
    val longSidePx = max(metrics.widthPixels, metrics.heightPixels)
    val shortSidePx = min(metrics.widthPixels, metrics.heightPixels)
    if (longSidePx <= 0) {
      super.attachBaseContext(newBase)
      return
    }
    val targetDpi = (longSidePx * 160f / DESIGN_WIDTH_DP).roundToInt().coerceAtLeast(80)
    val config = Configuration(newBase.resources.configuration).apply {
      densityDpi = targetDpi
      screenWidthDp = (longSidePx * 160f / targetDpi).roundToInt()
      screenHeightDp = (shortSidePx * 160f / targetDpi).roundToInt()
      smallestScreenWidthDp = screenHeightDp
    }
    super.attachBaseContext(newBase.createConfigurationContext(config))
  }

  // ADB-driven settings transfer between devices:
  //   am start -n com.horizontv.launcher/.MainActivity --ez horizon_export_backup true
  //   am start -n com.horizontv.launcher/.MainActivity --ez horizon_import_backup true
  // File lives at /sdcard/Android/data/com.horizontv.launcher/files/horizon-tv-backup.json
  private fun handleAdbBackupIntent(intent: Intent?) {
    if (intent == null) return
    val dir = getExternalFilesDir(null) ?: return
    val file = File(dir, ADB_BACKUP_FILE)
    if (intent.getBooleanExtra(EXTRA_EXPORT_BACKUP, false)) {
      intent.removeExtra(EXTRA_EXPORT_BACKUP)
      val ok = runCatching { file.writeText(viewModel.exportBackupJson(), Charsets.UTF_8) }.isSuccess
      viewModel.onBackupExported(ok)
    }
    if (intent.getBooleanExtra(EXTRA_IMPORT_BACKUP, false)) {
      intent.removeExtra(EXTRA_IMPORT_BACKUP)
      val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
      if (raw != null) viewModel.restoreBackupJson(raw) else viewModel.onBackupExported(false)
    }
    if (intent.getBooleanExtra(EXTRA_PREVIEW_SCREENSAVER, false)) {
      intent.removeExtra(EXTRA_PREVIEW_SCREENSAVER)
      viewModel.startScreensaver()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleAdbBackupIntent(intent)

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )

    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      val theme = LauncherPalettes.themeById(state.settings.themeId)
      val accent = LauncherPalettes.accentById(state.settings.accentId)

      // Handle Back gracefully for overlays/dialogs and keep Home screen anchored
      BackHandler(enabled = true) {
        when {
          state.pinPrompt != null -> viewModel.dismissPinPrompt()
          state.webCompanionInfo != null -> viewModel.stopWebCompanion()
          state.isSetupWizardOpen -> viewModel.completeSetupWizard()
          state.isScreensaverActive -> viewModel.stopScreensaver()
          state.isTvInputsDialogOpen -> viewModel.closeTvInputsDialog()
          state.showUpdateDialog -> viewModel.dismissUpdateDialog()
          state.isWeatherDialogOpen -> viewModel.closeWeatherSetup()
          state.isAddingSubreddit -> viewModel.closeAddSubredditDialog()
          state.setupProviderDialog != null -> viewModel.closeJellyfinSetup()
          state.renamingApp != null -> viewModel.closeRenameApp()
          state.isCreatingRow -> viewModel.closeCreateRowDialog()
          state.renamingRowId != null -> viewModel.closeRenameRowDialog()
          state.addingAppsToRowId != null -> viewModel.closeAddAppsToRow()
          state.orderingRowId != null -> viewModel.closeTileOrderDialog()
          state.menuTarget != null -> viewModel.closeAppMenu()
          state.isSettingsOpen -> viewModel.closeSettings()
          state.reorderingTarget != null -> viewModel.stopReordering()
          state.isSearchVisible -> viewModel.toggleSearch()
          else -> {
            // Stay on the Home screen when acting as the TV launcher
          }
        }
      }

      HorizonTVLauncherTheme(
        wallpaperTheme = theme,
        accentPreset = accent,
      ) {
        LauncherScreen(
          state = state,
          onAppFocused = cb.onAppFocused,
          onLaunchApp = cb.onLaunchApp,
          onOpenAppMenu = cb.onOpenAppMenu,
          onCloseAppMenu = cb.onCloseAppMenu,
          onToggleAppInRow = cb.onToggleAppInRow,
          onMoveAppInRow = cb.onMoveAppInRow,
          onStartReordering = cb.onStartReordering,
          onStopReordering = cb.onStopReordering,
          onOpenRenameApp = cb.onOpenRenameApp,
          onCloseRenameApp = cb.onCloseRenameApp,
          onRenameApp = cb.onRenameApp,
          onOpenCreateRow = cb.onOpenCreateRow,
          onCloseCreateRow = cb.onCloseCreateRow,
          onCreateRow = cb.onCreateRow,
          onOpenRenameRow = cb.onOpenRenameRow,
          onCloseRenameRow = cb.onCloseRenameRow,
          onRenameRow = cb.onRenameRow,
          onMoveRow = cb.onMoveRow,
          onDeleteRow = cb.onDeleteRow,
          onOpenAddAppsToRow = cb.onOpenAddAppsToRow,
          onCloseAddAppsToRow = cb.onCloseAddAppsToRow,
          onOpenTileOrder = cb.onOpenTileOrder,
          onCloseTileOrder = cb.onCloseTileOrder,
          onSortRowApps = cb.onSortRowApps,
          onPickCustomIcon = cb.onPickCustomIcon,
          onResetCustomIcon = cb.onResetCustomIcon,
          onHideApp = cb.onHideApp,
          onUnhideApp = cb.onUnhideApp,
          onToggleAppPinLock = cb.onToggleAppPinLock,
          onUnlockAppPin = cb.onUnlockAppPin,
          onRequestSetPin = cb.onRequestSetPin,
          onRequestChangePin = cb.onRequestChangePin,
          onRequestRemovePin = cb.onRequestRemovePin,
          onTogglePinProtectSettings = cb.onTogglePinProtectSettings,
          onPinVerified = cb.onPinVerified,
          onNewPinConfirmed = cb.onNewPinConfirmed,
          onDismissPinPrompt = cb.onDismissPinPrompt,
          onSelectFilter = cb.onSelectFilter,
          onToggleSearch = cb.onToggleSearch,
          onUpdateSearchQuery = cb.onUpdateSearchQuery,
          onCommitRecentSearch = cb.onCommitRecentSearch,
          onClearRecentSearches = cb.onClearRecentSearches,
          onVoiceSearch = cb.onVoiceSearch,
          onSelectSearchProvider = cb.onSelectSearchProvider,
          onOpenJellyfinSearchItem = cb.onOpenJellyfinSearchItem,
          onOpenStreamingOffer = cb.onOpenStreamingOffer,
          onLoadMoreStreaming = cb.onLoadMoreStreaming,
          onSearchExternalProvider = cb.onSearchExternalProvider,
          onSearchYouTube = cb.onSearchYouTube,
          onSearchWeb = cb.onSearchWeb,
          onOpenSettings = cb.onOpenSettings,
          onCloseSettings = cb.onCloseSettings,
          onSelectTheme = cb.onSelectTheme,
          onSelectAccent = cb.onSelectAccent,
          onPickWallpaper = cb.onPickWallpaper,
          onClearWallpaper = cb.onClearWallpaper,
          onSetWallpaperDim = cb.onSetWallpaperDim,
          onToggleWallpaperKenBurns = cb.onToggleWallpaperKenBurns,
          onSelectWallpaperProvider = cb.onSelectWallpaperProvider,
          onOpenJellyfinSetup = cb.onOpenJellyfinSetup,
          onCloseJellyfinSetup = cb.onCloseJellyfinSetup,
          onConnectJellyfin = cb.onConnectJellyfin,
          onDisconnectJellyfin = cb.onDisconnectJellyfin,
          onToggleJellyfinSlideshow = cb.onToggleJellyfinSlideshow,
          onSetJellyfinInterval = cb.onSetJellyfinInterval,
          onToggleJellyfinShowTitle = cb.onToggleJellyfinShowTitle,
          onSetJellyfinMediaType = cb.onSetJellyfinMediaType,
          onSelectJellyfinLibrary = cb.onSelectJellyfinLibrary,
          onSetJellyfinBackdropLimit = cb.onSetJellyfinBackdropLimit,
          onNextJellyfinBackdrop = cb.onNextJellyfinBackdrop,
          onSetRedditSort = cb.onSetRedditSort,
          onOpenAddSubreddit = cb.onOpenAddSubreddit,
          onCloseAddSubreddit = cb.onCloseAddSubreddit,
          onAddCustomSubreddit = cb.onAddCustomSubreddit,
          onOpenWeatherSetup = cb.onOpenWeatherSetup,
          onCloseWeatherSetup = cb.onCloseWeatherSetup,
          onSearchWeatherLocations = cb.onSearchWeatherLocations,
          onSelectWeatherLocation = cb.onSelectWeatherLocation,
          onToggleWeatherEnabled = cb.onToggleWeatherEnabled,
          onSetWeatherDays = cb.onSetWeatherDays,
          onToggleWeatherShowForecast = cb.onToggleWeatherShowForecast,
          onSetWeatherForecastSize = cb.onSetWeatherForecastSize,
          onToggleWeatherUnit = cb.onToggleWeatherUnit,
          onSetWeatherIconStyle = cb.onSetWeatherIconStyle,
          onToggleWeatherBackdrop = cb.onToggleWeatherBackdrop,
          onRefreshWeather = cb.onRefreshWeather,
          onToggle24HourClock = cb.onToggle24HourClock,
          onSetGridColumns = cb.onSetGridColumns,
          onToggleShowNonLeanback = cb.onToggleShowNonLeanback,
          onToggleShowSearchBar = cb.onToggleShowSearchBar,
          onToggleSearchOpenDetailsPopup = cb.onToggleSearchOpenDetailsPopup,
          onToggleSearchProvider = cb.onToggleSearchProvider,
          onToggleJustWatchFilterServices = cb.onToggleJustWatchFilterServices,
          onToggleJustWatchService = cb.onToggleJustWatchService,
          onSelectAllJustWatchServices = cb.onSelectAllJustWatchServices,
          onClearAllJustWatchServices = cb.onClearAllJustWatchServices,
          onToggleJustWatchIncludeRentBuy = cb.onToggleJustWatchIncludeRentBuy,
          onToggleJustWatchHideUnmatchedTitles = cb.onToggleJustWatchHideUnmatchedTitles,
          onSetJustWatchRegion = cb.onSetJustWatchRegion,
          onToggleJustWatchShowRottenTomatoes = cb.onToggleJustWatchShowRottenTomatoes,
          onToggleJustWatchShowAgeRating = cb.onToggleJustWatchShowAgeRating,
          onToggleShowAllAppsSection = cb.onToggleShowAllAppsSection,
          onToggleShowAddAppTile = cb.onToggleShowAddAppTile,
          onToggleShowTileTitles = cb.onToggleShowTileTitles,
          onToggleShowTvInputsButton = cb.onToggleShowTvInputsButton,
          onOpenTvInputs = cb.onOpenTvInputs,
          onCloseTvInputs = cb.onCloseTvInputs,
          onSelectTvInput = cb.onSelectTvInput,
          onToggleLockHomeLayout = cb.onToggleLockHomeLayout,
          onSetHomeTileSize = cb.onSetHomeTileSize,
          onToggleMinimalistDockMode = cb.onToggleMinimalistDockMode,
          onSetMinimalistDockMode = cb.onSetMinimalistDockMode,
          onSet24HourClock = cb.onSet24HourClock,
          onOpenSetupWizard = cb.onOpenSetupWizard,
          onCompleteSetupWizard = cb.onCompleteSetupWizard,
          onEnableRedditWallpapersFromWizard = cb.onEnableRedditWallpapersFromWizard,
          onStartWebCompanion = cb.onStartWebCompanion,
          onStopWebCompanion = cb.onStopWebCompanion,
          onSetTileOpacity = cb.onSetTileOpacity,
          onSetTileSpacing = cb.onSetTileSpacing,
          onSetTileCornerStyle = cb.onSetTileCornerStyle,
          onSetTileFocusStyle = cb.onSetTileFocusStyle,
          onCycleRowTileShape = cb.onCycleRowTileShape,
          onSetClockStyle = cb.onSetClockStyle,
          onSetClockFontStyle = cb.onSetClockFontStyle,
          onSetClockFontWeight = cb.onSetClockFontWeight,
          onToggleShowClock = cb.onToggleShowClock,
          onToggleShowDate = cb.onToggleShowDate,
          onOpenAppInfo = cb.onOpenAppInfo,
          onUninstallApp = cb.onUninstallApp,
          onOpenWifi = cb.onOpenWifi,
          onOpenBluetooth = cb.onOpenBluetooth,
          onOpenSystemSettings = cb.onOpenSystemSettings,
          onOpenHomeSettings = cb.onOpenHomeSettings,
          onOpenPlayStore = cb.onOpenPlayStore,
          onBackupConfig = cb.onBackupConfig,
          onRestoreConfig = cb.onRestoreConfig,
          onResetToDefaults = cb.onResetToDefaults,
          onRefreshApps = cb.onRefreshApps,
          onStartScreensaver = cb.onStartScreensaver,
          onStopScreensaver = cb.onStopScreensaver,
          onToggleScreensaverEnabled = cb.onToggleScreensaverEnabled,
          onSetScreensaverMode = cb.onSetScreensaverMode,
          onSetScreensaverTimeoutMinutes = cb.onSetScreensaverTimeoutMinutes,
          onSetScreensaverIntervalSeconds = cb.onSetScreensaverIntervalSeconds,
          onToggleScreensaverKenBurns = cb.onToggleScreensaverKenBurns,
          onToggleScreensaverShowClock = cb.onToggleScreensaverShowClock,
          onToggleScreensaverShowWeather = cb.onToggleScreensaverShowWeather,
          onToggleScreensaverShowTitle = cb.onToggleScreensaverShowTitle,
          onToggleScreensaverBurnInShift = cb.onToggleScreensaverBurnInShift,
          onSetScreensaverDimAlpha = cb.onSetScreensaverDimAlpha,
          onToggleScreensaverAmbientCycle = cb.onToggleScreensaverAmbientCycle,
          onPickScreensaverPhotoFolder = cb.onPickScreensaverPhotoFolder,
          onPickScreensaverPhotos = cb.onPickScreensaverPhotos,
          onClearScreensaverPhotos = cb.onClearScreensaverPhotos,
          onOpenSystemScreensaverSettings = cb.onOpenSystemScreensaverSettings,
          onCheckForUpdates = cb.onCheckForUpdates,
          onLoadLatestRelease = cb.onLoadLatestRelease,
          onSetUpdateCheckSchedule = cb.onSetUpdateCheckSchedule,
          onOpenUpdateDialog = cb.onOpenUpdateDialog,
          onDismissUpdateDialog = cb.onDismissUpdateDialog,
          onInstallUpdate = cb.onInstallUpdate,
          homeFocusTrigger = homeFocusTrigger,
          appVersionName = cb.appVersionName,
          appVersionCode = cb.appVersionCode,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleAdbBackupIntent(intent)
    homeFocusTrigger += 1
  }

  private class MainCallbacks(
    activity: MainActivity,
    vm: LauncherViewModel,
  ) {
    val onAppFocused = vm::onAppFocused
    val onLaunchApp = vm::launchApp
    val onOpenAppMenu = vm::openAppMenu
    val onCloseAppMenu = vm::closeAppMenu
    val onToggleAppInRow = vm::toggleAppInRow
    val onMoveAppInRow = vm::moveAppInRow
    val onStartReordering = vm::startReordering
    val onStopReordering = vm::stopReordering
    val onOpenRenameApp = vm::openRenameApp
    val onCloseRenameApp = vm::closeRenameApp
    val onRenameApp = vm::renameApp
    val onOpenCreateRow = vm::openCreateRowDialog
    val onCloseCreateRow = vm::closeCreateRowDialog
    val onCreateRow: (String) -> Unit = { title -> vm.createRow(title, openAppPickerAfter = true) }
    val onOpenRenameRow = vm::openRenameRowDialog
    val onCloseRenameRow = vm::closeRenameRowDialog
    val onRenameRow = vm::renameRow
    val onMoveRow = vm::moveRow
    val onDeleteRow = vm::deleteRow
    val onOpenAddAppsToRow = vm::openAddAppsToRow
    val onCloseAddAppsToRow = vm::closeAddAppsToRow
    val onOpenTileOrder = vm::openTileOrderDialog
    val onCloseTileOrder = vm::closeTileOrderDialog
    val onSortRowApps = vm::sortRowAppsAlphabetically
    val onPickCustomIcon: (String) -> Unit = { packageName ->
      activity.pendingIconPackage = packageName
      runCatching { activity.customIconPicker.launch(arrayOf("image/*")) }
    }
    val onResetCustomIcon = vm::resetCustomAppIcon
    val onHideApp = vm::hideApp
    val onUnhideApp = vm::unhideApp
    val onToggleAppPinLock = vm::toggleAppPinLock
    val onUnlockAppPin = vm::unlockAppPin
    val onRequestSetPin = vm::requestSetPin
    val onRequestChangePin = vm::requestChangePin
    val onRequestRemovePin = vm::requestRemovePin
    val onTogglePinProtectSettings = vm::togglePinProtectSettings
    val onPinVerified = vm::onPinVerified
    val onNewPinConfirmed = vm::onNewPinConfirmed
    val onDismissPinPrompt = vm::dismissPinPrompt
    val onSelectFilter = vm::setFilter
    val onToggleSearch = vm::toggleSearch
    val onUpdateSearchQuery = vm::updateSearchQuery
    val onCommitRecentSearch = vm::commitRecentSearch
    val onClearRecentSearches = vm::clearRecentSearches
    val onVoiceSearch = activity::launchVoiceSearch
    val onSelectSearchProvider = vm::setSearchProvider
    val onOpenJellyfinSearchItem = vm::openJellyfinSearchItem
    val onOpenStreamingOffer = vm::openStreamingOffer
    val onLoadMoreStreaming = vm::loadMoreStreamingResults
    val onSearchExternalProvider = vm::openExternalProviderSearch
    val onSearchYouTube = vm::openYouTubeSearch
    val onSearchWeb = vm::openWebSearch
    val onOpenSettings = vm::openSettings
    val onCloseSettings = vm::closeSettings
    val onSelectTheme = vm::selectTheme
    val onSelectAccent = vm::selectAccent
    val onPickWallpaper: () -> Unit = {
      runCatching { activity.wallpaperPicker.launch(arrayOf("image/*")) }
    }
    val onClearWallpaper: () -> Unit = { vm.setCustomWallpaper(null) }
    val onSetWallpaperDim = vm::setWallpaperDimAlpha
    val onToggleWallpaperKenBurns = vm::toggleWallpaperKenBurns
    val onSelectWallpaperProvider = vm::selectWallpaperProvider
    val onOpenJellyfinSetup: () -> Unit = { vm.openJellyfinSetup() }
    val onCloseJellyfinSetup = vm::closeJellyfinSetup
    val onConnectJellyfin: (String, String?, String?, String?) -> Unit = { url, key, user, pass ->
      vm.connectJellyfin(url, key, user, pass)
    }
    val onDisconnectJellyfin: () -> Unit = { vm.disconnectJellyfin() }
    val onToggleJellyfinSlideshow = vm::toggleJellyfinSlideshow
    val onSetJellyfinInterval = vm::setJellyfinInterval
    val onToggleJellyfinShowTitle = vm::toggleJellyfinShowTitle
    val onSetJellyfinMediaType = vm::setJellyfinMediaType
    val onSelectJellyfinLibrary = vm::selectJellyfinLibrary
    val onSetJellyfinBackdropLimit = vm::setJellyfinBackdropLimit
    val onNextJellyfinBackdrop = vm::nextJellyfinBackdrop
    val onSetRedditSort = vm::setRedditSort
    val onOpenAddSubreddit = vm::openAddSubredditDialog
    val onCloseAddSubreddit = vm::closeAddSubredditDialog
    val onAddCustomSubreddit = vm::addCustomSubreddit
    val onOpenWeatherSetup = vm::openWeatherSetup
    val onCloseWeatherSetup = vm::closeWeatherSetup
    val onSearchWeatherLocations = vm::searchWeatherLocations
    val onSelectWeatherLocation = vm::selectWeatherLocation
    val onToggleWeatherEnabled = vm::toggleWeatherEnabled
    val onSetWeatherDays = vm::setWeatherDays
    val onToggleWeatherShowForecast = vm::toggleWeatherShowForecast
    val onSetWeatherForecastSize = vm::setWeatherForecastSize
    val onToggleWeatherUnit = vm::toggleWeatherUnit
    val onSetWeatherIconStyle = vm::setWeatherIconStyle
    val onToggleWeatherBackdrop = vm::toggleWeatherBackdrop
    val onRefreshWeather = vm::refreshWeather
    val onToggle24HourClock = vm::toggle24HourClock
    val onSetGridColumns = vm::setGridColumns
    val onToggleShowNonLeanback = vm::toggleShowNonLeanbackApps
    val onToggleShowSearchBar = vm::toggleShowSearchBar
    val onToggleSearchOpenDetailsPopup = vm::toggleSearchOpenDetailsPopup
    val onToggleSearchProvider = vm::toggleSearchProvider
    val onToggleJustWatchFilterServices = vm::toggleJustWatchFilterServices
    val onToggleJustWatchService = vm::toggleJustWatchService
    val onSelectAllJustWatchServices = vm::selectAllJustWatchServices
    val onClearAllJustWatchServices = vm::clearAllJustWatchServices
    val onToggleJustWatchIncludeRentBuy = vm::toggleJustWatchIncludeRentBuy
    val onToggleJustWatchHideUnmatchedTitles = vm::toggleJustWatchHideUnmatchedTitles
    val onSetJustWatchRegion = vm::setJustWatchRegion
    val onToggleJustWatchShowRottenTomatoes = vm::toggleJustWatchShowRottenTomatoes
    val onToggleJustWatchShowAgeRating = vm::toggleJustWatchShowAgeRating
    val onToggleShowAllAppsSection = vm::toggleShowAllAppsSection
    val onToggleShowAddAppTile = vm::toggleShowAddAppTile
    val onToggleShowTileTitles = vm::toggleShowTileTitles
    val onToggleShowTvInputsButton = vm::toggleShowTvInputsButton
    val onOpenTvInputs = vm::openTvInputsDialog
    val onCloseTvInputs = vm::closeTvInputsDialog
    val onSelectTvInput = vm::selectTvInput
    val onToggleLockHomeLayout = vm::toggleLockHomeLayout
    val onSetHomeTileSize = vm::setHomeTileSize
    val onToggleMinimalistDockMode = vm::toggleMinimalistDockMode
    val onSetMinimalistDockMode = vm::setMinimalistDockMode
    val onSet24HourClock = vm::set24HourClock
    val onOpenSetupWizard = vm::openSetupWizard
    val onCompleteSetupWizard = vm::completeSetupWizard
    val onEnableRedditWallpapersFromWizard = vm::enableRedditWallpapersFromWizard
    val onStartWebCompanion = vm::startWebCompanion
    val onStopWebCompanion = vm::stopWebCompanion
    val onSetTileOpacity = vm::setTileOpacity
    val onSetTileSpacing = vm::setTileSpacing
    val onSetTileCornerStyle = vm::setTileCornerStyle
    val onSetTileFocusStyle = vm::setTileFocusStyle
    val onCycleRowTileShape = vm::cycleRowTileShape
    val onSetClockStyle = vm::setClockStyle
    val onSetClockFontStyle = vm::setClockFontStyle
    val onSetClockFontWeight = vm::setClockFontWeight
    val onToggleShowClock = vm::toggleShowClock
    val onToggleShowDate = vm::toggleShowDate
    val onOpenAppInfo = vm::openAppInfo
    val onUninstallApp = vm::uninstallApp
    val onOpenWifi = vm::openWifiSettings
    val onOpenBluetooth = vm::openBluetoothSettings
    val onOpenSystemSettings = vm::openSystemSettings
    val onOpenHomeSettings = activity::requestDefaultHomeLauncher
    val onOpenPlayStore = vm::openPlayStore
    val onBackupConfig: () -> Unit = {
      runCatching { activity.backupConfigLauncher.launch("horizon-tv-backup.json") }
    }
    val onRestoreConfig: () -> Unit = {
      runCatching { activity.restoreConfigLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
    }
    val onResetToDefaults = vm::resetToDefaults
    val onRefreshApps = vm::refreshApps
    val onStartScreensaver = vm::startScreensaver
    val onStopScreensaver = vm::stopScreensaver
    val onToggleScreensaverEnabled = vm::toggleScreensaverEnabled
    val onSetScreensaverMode = vm::setScreensaverMode
    val onSetScreensaverTimeoutMinutes = vm::setScreensaverTimeoutMinutes
    val onSetScreensaverIntervalSeconds = vm::setScreensaverIntervalSeconds
    val onToggleScreensaverKenBurns = vm::toggleScreensaverKenBurns
    val onToggleScreensaverShowClock = vm::toggleScreensaverShowClock
    val onToggleScreensaverShowWeather = vm::toggleScreensaverShowWeather
    val onToggleScreensaverShowTitle = vm::toggleScreensaverShowTitle
    val onToggleScreensaverBurnInShift = vm::toggleScreensaverBurnInShift
    val onSetScreensaverDimAlpha = vm::setScreensaverDimAlpha
    val onToggleScreensaverAmbientCycle = vm::toggleScreensaverAmbientCycle
    val onPickScreensaverPhotoFolder: () -> Unit = {
      runCatching { activity.screensaverFolderPicker.launch(null) }
    }
    val onPickScreensaverPhotos: () -> Unit = {
      runCatching { activity.screensaverPhotosPicker.launch(arrayOf("image/*")) }
    }
    val onClearScreensaverPhotos = vm::clearScreensaverPhotos
    val onOpenSystemScreensaverSettings = activity::openSystemScreensaverSettings
    val onCheckForUpdates: () -> Unit = { vm.checkForUpdates(manual = true) }
    val onLoadLatestRelease = vm::loadLatestReleaseInfoIfNeeded
    val onSetUpdateCheckSchedule = vm::setUpdateCheckSchedule
    val onOpenUpdateDialog = vm::openUpdateDialog
    val onDismissUpdateDialog = vm::dismissUpdateDialog
    val onInstallUpdate = vm::installAvailableUpdate
    val appVersionName: String = runCatching {
      activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "2.5.36"
    }.getOrDefault("2.5.36")
    val appVersionCode: Int = runCatching {
      val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt() else {
        @Suppress("DEPRECATION")
        info.versionCode
      }
    }.getOrDefault(80)
  }

  companion object {
    private const val DESIGN_WIDTH_DP = 1152f
    private const val ADB_BACKUP_FILE = "horizon-tv-backup.json"
    private const val EXTRA_EXPORT_BACKUP = "horizon_export_backup"
    private const val EXTRA_IMPORT_BACKUP = "horizon_import_backup"
    private const val EXTRA_PREVIEW_SCREENSAVER = "horizon_preview_screensaver"
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshNetworkStatus()
  }
}
