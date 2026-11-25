package com.horizontv.launcher.ui

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.AppFilter
import com.horizontv.launcher.model.AppInfo
import com.horizontv.launcher.model.ClockFontStyle
import com.horizontv.launcher.model.ClockFontWeight
import com.horizontv.launcher.model.ClockStyle
import com.horizontv.launcher.model.ExternalSearchProvider
import com.horizontv.launcher.model.HomeTileSize
import com.horizontv.launcher.model.HomeTileSpacing
import com.horizontv.launcher.model.TileAspectShape
import com.horizontv.launcher.model.TileCornerStyle
import com.horizontv.launcher.model.TileFocusStyle
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinSearchItem
import com.horizontv.launcher.model.JustWatchRegion
import com.horizontv.launcher.model.LauncherSettings
import com.horizontv.launcher.model.LauncherUiState
import com.horizontv.launcher.model.MediaWallpaperProvider
import com.horizontv.launcher.model.ResolvedAppRow
import com.horizontv.launcher.model.ScreensaverMode
import com.horizontv.launcher.model.SearchProviderFilter
import com.horizontv.launcher.model.StreamingOffer
import com.horizontv.launcher.model.TvInputSource
import com.horizontv.launcher.model.UpdateCheckSchedule
import com.horizontv.launcher.model.WeatherForecastSize
import com.horizontv.launcher.model.WeatherIconStyle
import com.horizontv.launcher.model.WeatherLocation
import com.horizontv.launcher.ui.components.AddFavoritePickerDialog
import com.horizontv.launcher.ui.components.AddFavoriteTile
import com.horizontv.launcher.ui.components.AmbientBackground
import com.horizontv.launcher.ui.components.AppActionDialog
import com.horizontv.launcher.ui.components.AppGridTile
import com.horizontv.launcher.ui.components.AppUpdateDialog
import com.horizontv.launcher.ui.components.PinDialog
import com.horizontv.launcher.ui.components.SetupWizardDialog
import com.horizontv.launcher.ui.components.WebCompanionDialog
import com.horizontv.launcher.ui.components.FavoriteBannerCard
import com.horizontv.launcher.ui.components.JellyfinSetupDialog
import com.horizontv.launcher.ui.components.LauncherTopBar
import com.horizontv.launcher.ui.components.RenameTextDialog
import com.horizontv.launcher.ui.components.ScreensaverOverlay
import com.horizontv.launcher.ui.components.SettingsOverlay
import com.horizontv.launcher.ui.components.TileOrderDialog
import com.horizontv.launcher.ui.components.TvInputsDialog
import com.horizontv.launcher.ui.components.UniversalSearchDialog
import com.horizontv.launcher.ui.components.WeatherSetupDialog
import com.horizontv.launcher.ui.theme.LauncherPalettes
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
private val NoAutoBringIntoViewSpec = object : BringIntoViewSpec {
  override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(
  state: LauncherUiState,
  onAppFocused: (AppInfo?) -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  onOpenAppMenu: (AppInfo, String?) -> Unit,
  onCloseAppMenu: () -> Unit,
  onToggleAppInRow: (String, String) -> Unit,
  onMoveAppInRow: (String, String, Int) -> Unit,
  onStartReordering: (String, String) -> Unit,
  onStopReordering: () -> Unit,
  onOpenRenameApp: (AppInfo) -> Unit,
  onCloseRenameApp: () -> Unit,
  onRenameApp: (String, String) -> Unit,
  onOpenCreateRow: () -> Unit,
  onCloseCreateRow: () -> Unit,
  onCreateRow: (String) -> Unit,
  onOpenRenameRow: (String) -> Unit,
  onCloseRenameRow: () -> Unit,
  onRenameRow: (String, String) -> Unit,
  onMoveRow: (String, Int) -> Unit,
  onDeleteRow: (String) -> Unit,
  onOpenAddAppsToRow: (String) -> Unit,
  onCloseAddAppsToRow: () -> Unit,
  onOpenTileOrder: (String) -> Unit,
  onCloseTileOrder: () -> Unit,
  onSortRowApps: (String) -> Unit,
  onPickCustomIcon: (String) -> Unit,
  onResetCustomIcon: (String) -> Unit,
  onHideApp: (String) -> Unit,
  onUnhideApp: (String) -> Unit,
  onToggleAppPinLock: (AppInfo) -> Unit = {},
  onUnlockAppPin: (String) -> Unit = {},
  onRequestSetPin: () -> Unit = {},
  onRequestChangePin: () -> Unit = {},
  onRequestRemovePin: () -> Unit = {},
  onTogglePinProtectSettings: () -> Unit = {},
  onPinVerified: () -> Unit = {},
  onNewPinConfirmed: (String) -> Unit = {},
  onDismissPinPrompt: () -> Unit = {},
  onSelectFilter: (AppFilter) -> Unit,
  onToggleSearch: () -> Unit,
  onUpdateSearchQuery: (String) -> Unit,
  onCommitRecentSearch: (String) -> Unit = {},
  onClearRecentSearches: () -> Unit = {},
  onVoiceSearch: () -> Unit = {},
  onSelectSearchProvider: (SearchProviderFilter) -> Unit,
  onOpenJellyfinSearchItem: (JellyfinSearchItem) -> Unit,
  onOpenStreamingOffer: (StreamingOffer) -> Unit,
  onLoadMoreStreaming: () -> Unit = {},
  onSearchExternalProvider: (ExternalSearchProvider, String) -> Unit,
  onSearchYouTube: (String) -> Unit,
  onSearchWeb: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onCloseSettings: () -> Unit,
  onSelectTheme: (String) -> Unit,
  onSelectAccent: (String) -> Unit,
  onPickWallpaper: () -> Unit,
  onClearWallpaper: () -> Unit,
  onSetWallpaperDim: (Float) -> Unit,
  onToggleWallpaperKenBurns: () -> Unit = {},
  onSelectWallpaperProvider: (MediaWallpaperProvider) -> Unit,
  onOpenJellyfinSetup: () -> Unit,
  onCloseJellyfinSetup: () -> Unit,
  onConnectJellyfin: (serverUrl: String, apiKey: String?, username: String?, password: String?) -> Unit,
  onDisconnectJellyfin: () -> Unit,
  onToggleJellyfinSlideshow: () -> Unit,
  onSetJellyfinInterval: (Int) -> Unit,
  onToggleJellyfinShowTitle: () -> Unit,
  onSetJellyfinMediaType: (String) -> Unit,
  onSelectJellyfinLibrary: (JellyfinLibrary?) -> Unit,
  onSetJellyfinBackdropLimit: (Int) -> Unit,
  onNextJellyfinBackdrop: () -> Unit,
  onSetRedditSort: (String) -> Unit,
  onOpenAddSubreddit: () -> Unit,
  onCloseAddSubreddit: () -> Unit,
  onAddCustomSubreddit: (String) -> Unit,
  onOpenWeatherSetup: () -> Unit,
  onCloseWeatherSetup: () -> Unit,
  onSearchWeatherLocations: (String) -> Unit,
  onSelectWeatherLocation: (WeatherLocation) -> Unit,
  onToggleWeatherEnabled: () -> Unit,
  onSetWeatherDays: (Int) -> Unit,
  onToggleWeatherShowForecast: () -> Unit,
  onSetWeatherForecastSize: (String) -> Unit,
  onToggleWeatherUnit: () -> Unit,
  onSetWeatherIconStyle: (String) -> Unit,
  onToggleWeatherBackdrop: () -> Unit,
  onRefreshWeather: () -> Unit,
  onToggle24HourClock: () -> Unit,
  onSetGridColumns: (Int) -> Unit,
  onToggleShowNonLeanback: () -> Unit,
  onToggleShowSearchBar: () -> Unit,
  onToggleSearchOpenDetailsPopup: () -> Unit = {},
  onToggleSearchProvider: (String) -> Unit,
  onToggleJustWatchFilterServices: () -> Unit = {},
  onToggleJustWatchService: (String) -> Unit = {},
  onSelectAllJustWatchServices: () -> Unit = {},
  onClearAllJustWatchServices: () -> Unit = {},
  onToggleJustWatchIncludeRentBuy: () -> Unit = {},
  onToggleJustWatchHideUnmatchedTitles: () -> Unit = {},
  onSetJustWatchRegion: (JustWatchRegion) -> Unit = {},
  onToggleJustWatchShowRottenTomatoes: () -> Unit = {},
  onToggleJustWatchShowAgeRating: () -> Unit = {},
  onToggleShowAllAppsSection: () -> Unit,
  onToggleShowAddAppTile: () -> Unit,
  onToggleShowTileTitles: () -> Unit,
  onToggleShowTvInputsButton: () -> Unit = {},
  onOpenTvInputs: () -> Unit = {},
  onCloseTvInputs: () -> Unit = {},
  onSelectTvInput: (TvInputSource) -> Unit = {},
  onToggleLockHomeLayout: () -> Unit,
  onSetHomeTileSize: (String) -> Unit,
  onToggleMinimalistDockMode: () -> Unit = {},
  onSetMinimalistDockMode: (Boolean) -> Unit = {},
  onSet24HourClock: (Boolean) -> Unit = {},
  onOpenSetupWizard: () -> Unit = {},
  onCompleteSetupWizard: () -> Unit = {},
  onEnableRedditWallpapersFromWizard: () -> Unit = {},
  onStartWebCompanion: () -> Unit = {},
  onStopWebCompanion: () -> Unit = {},
  onSetTileOpacity: (Float) -> Unit = {},
  onSetTileSpacing: (String) -> Unit = {},
  onSetTileCornerStyle: (String) -> Unit = {},
  onSetTileFocusStyle: (String) -> Unit = {},
  onCycleRowTileShape: (String) -> Unit = {},
  onSetClockStyle: (String) -> Unit = {},
  onSetClockFontStyle: (String) -> Unit = {},
  onSetClockFontWeight: (String) -> Unit = {},
  onToggleShowClock: () -> Unit,
  onToggleShowDate: () -> Unit,
  onOpenAppInfo: (String) -> Unit,
  onUninstallApp: (String) -> Unit,
  onOpenWifi: () -> Unit,
  onOpenBluetooth: () -> Unit,
  onOpenSystemSettings: () -> Unit,
  onOpenHomeSettings: () -> Unit,
  onOpenPlayStore: () -> Unit,
  onBackupConfig: () -> Unit,
  onRestoreConfig: () -> Unit,
  onResetToDefaults: () -> Unit = {},
  onRefreshApps: () -> Unit,
  onStartScreensaver: () -> Unit,
  onStopScreensaver: () -> Unit,
  onToggleScreensaverEnabled: () -> Unit,
  onSetScreensaverMode: (ScreensaverMode) -> Unit,
  onSetScreensaverTimeoutMinutes: (Int) -> Unit,
  onSetScreensaverIntervalSeconds: (Int) -> Unit,
  onToggleScreensaverKenBurns: () -> Unit,
  onToggleScreensaverShowClock: () -> Unit,
  onToggleScreensaverShowWeather: () -> Unit,
  onToggleScreensaverShowTitle: () -> Unit,
  onToggleScreensaverBurnInShift: () -> Unit,
  onSetScreensaverDimAlpha: (Float) -> Unit,
  onToggleScreensaverAmbientCycle: () -> Unit,
  onPickScreensaverPhotoFolder: () -> Unit,
  onPickScreensaverPhotos: () -> Unit,
  onClearScreensaverPhotos: () -> Unit,
  onOpenSystemScreensaverSettings: () -> Unit,
  onCheckForUpdates: () -> Unit = {},
  onLoadLatestRelease: () -> Unit = {},
  onSetUpdateCheckSchedule: (UpdateCheckSchedule) -> Unit = {},
  onOpenUpdateDialog: () -> Unit = {},
  onDismissUpdateDialog: () -> Unit = {},
  onInstallUpdate: () -> Unit = {},
  homeFocusTrigger: Int = 0,
  appVersionName: String = "2.1.0",
  appVersionCode: Int = 24,
) {
  val theme = remember(state.settings.themeId) { LauncherPalettes.themeById(state.settings.themeId) }
  val accent = remember(state.settings.accentId) { LauncherPalettes.accentById(state.settings.accentId) }
  val tileSize = remember(state.settings.homeTileSize) { HomeTileSize.fromId(state.settings.homeTileSize) }
  val resolvedRows = state.resolvedRows
  val heroRow = resolvedRows.first()
  val extraRows = resolvedRows.drop(1)
  val filteredApps = state.filteredApps
  val pinnedSet = remember(state.settings.rows) { state.settings.allPinnedPackages }
  val columns = state.settings.gridColumns.coerceIn(4, 6)
  val appRows = remember(filteredApps, columns) { filteredApps.chunked(columns) }

  val verticalScrollState = rememberScrollState()
  val rowListStates = remember { mutableStateMapOf<String, LazyListState>() }
  val rowYOffsets = remember { mutableStateMapOf<String, Int>() }
  var allAppsHeaderY by remember { mutableIntStateOf(0) }
  val gridRowYOffsets = remember { mutableStateMapOf<Int, Int>() }
  var dockScrollTargetY by remember { mutableIntStateOf(0) }
  var dockScrollNonce by remember { mutableIntStateOf(0) }

  fun requestDockScroll(targetY: Int) {
    dockScrollTargetY = targetY.coerceAtLeast(0)
    dockScrollNonce += 1
  }

  LaunchedEffect(dockScrollTargetY, dockScrollNonce) {
    if (dockScrollNonce > 0) {
      verticalScrollState.animateScrollTo(
        value = dockScrollTargetY,
        animationSpec = tween(durationMillis = 240),
      )
    }
  }

  // Deterministic FocusRequesters for every horizontal band & grid cell so D-pad Up/Down ALWAYS works
  val topBarFocusRequester = remember { FocusRequester() }
  val rowCardFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
  val rowAddFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
  val filterFocusRequester = remember { FocusRequester() }
  val gridTileFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
  val lastFocusedPackageByRow = remember { mutableStateMapOf<String, String?>() }
  val inputModeManager = LocalInputModeManager.current
  var hasFocusOnScreen by remember { mutableStateOf(false) }
  var isHeroEntryFocused by remember { mutableStateOf(false) }
  var lastInteractionMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

  // Idle timer for built-in Horizon Screensaver
  LaunchedEffect(
    state.settings.screensaverEnabled,
    state.settings.screensaverTimeoutMinutes,
    state.isScreensaverActive,
    state.isSettingsOpen,
    state.isSearchVisible,
    state.menuTarget,
  ) {
    if (!state.settings.screensaverEnabled ||
      state.isScreensaverActive ||
      state.isSettingsOpen ||
      state.isSearchVisible ||
      state.menuTarget != null
    ) {
      return@LaunchedEffect
    }
    val timeoutMs = state.settings.screensaverTimeoutMinutes.coerceIn(1, 60) * 60_000L
    while (true) {
      delay(5_000L)
      if (System.currentTimeMillis() - lastInteractionMillis >= timeoutMs) {
        onStartScreensaver()
        break
      }
    }
  }

  fun rowFirstTileRequester(row: ResolvedAppRow): FocusRequester {
    val firstApp = row.apps.firstOrNull()
    return if (firstApp != null) {
      rowCardFocusRequesters.getOrPut("${row.id}:${firstApp.packageName}") { FocusRequester() }
    } else {
      rowAddFocusRequesters.getOrPut(row.id) { FocusRequester() }
    }
  }

  // Always returns a FocusRequester for a tile that is currently visible & attached in the row's LazyRow,
  // so pressing Down from the TopBar or Up/Down between rows NEVER targets an off-screen recycled item.
  fun rowEntryRequester(row: ResolvedAppRow): FocusRequester {
    val listState = rowListStates[row.id]
    val visibleIndices = listState?.layoutInfo?.visibleItemsInfo?.map { it.index }.orEmpty()
    val lastPkg = lastFocusedPackageByRow[row.id]
    if (lastPkg != null) {
      val lastIdx = row.apps.indexOfFirst { it.packageName == lastPkg }
      if (lastIdx >= 0 && (visibleIndices.isEmpty() || lastIdx in visibleIndices)) {
        return rowCardFocusRequesters.getOrPut("${row.id}:$lastPkg") { FocusRequester() }
      }
    }
    val firstVisibleIdx = listState?.firstVisibleItemIndex ?: 0
    val visibleApp = row.apps.getOrNull(firstVisibleIdx)
    if (visibleApp != null) {
      return rowCardFocusRequesters.getOrPut("${row.id}:${visibleApp.packageName}") { FocusRequester() }
    }
    val hasAddTile = (state.settings.showAddAppTile && !state.settings.lockHomeLayout) || row.apps.isEmpty()
    if (firstVisibleIdx >= row.apps.size && hasAddTile) {
      return rowAddFocusRequesters.getOrPut(row.id) { FocusRequester() }
    }
    return rowFirstTileRequester(row)
  }

  fun gridTileRequester(rowIdx: Int, colIdx: Int): FocusRequester {
    return gridTileFocusRequesters.getOrPut("$rowIdx:$colIdx") { FocusRequester() }
  }

  val firstAllAppsTarget: FocusRequester? = when {
    !state.settings.showAllAppsSection -> null
    state.settings.showNonLeanbackApps -> filterFocusRequester
    appRows.isNotEmpty() -> gridTileRequester(0, 0)
    else -> null
  }

  // Always default focus to Row 1, Tile 1 when opening or returning to the launcher
  LaunchedEffect(state.isLoading, heroRow.id, heroRow.apps.firstOrNull()?.packageName, homeFocusTrigger) {
    if (state.isLoading) return@LaunchedEffect
    val anyOverlayOpen = state.isSettingsOpen ||
      state.isSearchVisible ||
      state.menuTarget != null ||
      state.setupProviderDialog != null ||
      state.isWeatherDialogOpen ||
      state.addingAppsToRowId != null ||
      state.orderingRowId != null ||
      state.isCreatingRow ||
      state.renamingRowId != null ||
      state.renamingApp != null ||
      state.isAddingSubreddit
    if (anyOverlayOpen) return@LaunchedEffect

    verticalScrollState.scrollTo(0)
    dockScrollTargetY = 0
    rowListStates.getOrPut(heroRow.id) { LazyListState() }.scrollToItem(0)

    val req = rowFirstTileRequester(heroRow)
    repeat(12) { attempt ->
      withFrameNanos { }
      if (attempt > 0) delay(50L)
      inputModeManager.requestInputMode(InputMode.Keyboard)
      val requested = runCatching { req.requestFocus() }.isSuccess
      if (requested) {
        withFrameNanos { }
        if (isHeroEntryFocused) return@LaunchedEffect
      }
    }
  }

  // Keep the reordering card scrolled into view and focused as it slides left/right
  LaunchedEffect(state.reorderingTarget, state.settings.rows) {
    val target = state.reorderingTarget ?: return@LaunchedEffect
    val row = resolvedRows.firstOrNull { it.id == target.rowId } ?: return@LaunchedEffect
    val idx = row.apps.indexOfFirst { it.packageName == target.packageName }
    if (idx >= 0) {
      rowListStates.getOrPut(row.id) { LazyListState() }.animateScrollToItem(idx)
      val req = rowCardFocusRequesters.getOrPut("${row.id}:${target.packageName}") { FocusRequester() }
      runCatching { req.requestFocus() }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .onFocusChanged { hasFocusOnScreen = it.hasFocus }
      // Screen-level safety net: default D-pad entry to Row 1 Tile 1 + Reorder Mode keys
      .onPreviewKeyEvent { keyEvent ->
        lastInteractionMillis = System.currentTimeMillis()
        val native = keyEvent.nativeKeyEvent
        val reorder = state.reorderingTarget
        if (reorder != null) {
          if (native.action == KeyEvent.ACTION_DOWN) {
            when (native.keyCode) {
              KeyEvent.KEYCODE_DPAD_LEFT -> {
                onMoveAppInRow(reorder.rowId, reorder.packageName, -1)
                return@onPreviewKeyEvent true
              }
              KeyEvent.KEYCODE_DPAD_RIGHT -> {
                onMoveAppInRow(reorder.rowId, reorder.packageName, +1)
                return@onPreviewKeyEvent true
              }
              KeyEvent.KEYCODE_DPAD_UP,
              KeyEvent.KEYCODE_DPAD_DOWN,
              -> return@onPreviewKeyEvent true
              KeyEvent.KEYCODE_DPAD_CENTER,
              KeyEvent.KEYCODE_ENTER,
              KeyEvent.KEYCODE_NUMPAD_ENTER,
              KeyEvent.KEYCODE_BACK,
              -> {
                onStopReordering()
                return@onPreviewKeyEvent true
              }
            }
          }
          return@onPreviewKeyEvent false
        }
        if (!hasFocusOnScreen && !state.isLoading && native.action == KeyEvent.ACTION_DOWN) {
          when (native.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            -> {
              inputModeManager.requestInputMode(InputMode.Keyboard)
              runCatching { rowEntryRequester(heroRow).requestFocus() }
              return@onPreviewKeyEvent true
            }
          }
        }
        false
      },
  ) {
    AmbientBackground(
      theme = theme,
      customWallpaperUri = state.settings.customWallpaperUri,
      jellyfinSlide = if (state.settings.jellyfinEnabled) state.jellyfinSlide else null,
      dimAlpha = state.settings.wallpaperDimAlpha,
      focusedTint = state.focusedApp?.tintColor,
      kenBurnsEnabled = state.settings.wallpaperKenBurns,
    )

    if (state.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          CircularProgressIndicator(color = accent.primary)
          Text(
            text = "Loading installed apps…",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
      }
    } else {
      val minimalistMode = state.settings.minimalistDockMode
      val tileOpacity = state.settings.tileOpacity
      val tileSpacingDp = HomeTileSpacing.fromId(state.settings.tileSpacing).spacingDp.dp
      val tileCornerStyle = remember(state.settings.tileCornerStyle) {
        TileCornerStyle.fromId(state.settings.tileCornerStyle)
      }
      val tileFocusStyle = remember(state.settings.tileFocusStyle) {
        TileFocusStyle.fromId(state.settings.tileFocusStyle)
      }
      val clockStyle = remember(state.settings.clockStyle) {
        ClockStyle.fromId(state.settings.clockStyle)
      }
      val clockFontStyle = remember(state.settings.clockFontStyle) {
        ClockFontStyle.fromId(state.settings.clockFontStyle)
      }
      val clockFontWeight = remember(state.settings.clockFontWeight) {
        ClockFontWeight.fromId(state.settings.clockFontWeight)
      }
      val maxRowTileHeightDp = remember(resolvedRows, tileSize) {
        resolvedRows.maxOfOrNull { it.tileShape.dimensionsFor(tileSize).second } ?: tileSize.heightDp
      }
      val singleRowDockHeight = (
        maxRowTileHeightDp +
          (if (state.settings.showTileTitles) 108 else 82) -
          (if (minimalistMode) 36 else 0)
        ).dp
      var allAppsGridY by remember { mutableIntStateOf(0) }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .navigationBarsPadding()
          .padding(
            top = if (minimalistMode) 24.dp else 30.dp,
            bottom = if (minimalistMode) 4.dp else 10.dp,
          ),
      ) {
        // Fixed Top Hero: Clock, Date, Weather, Controls, Reorder Hint, and Locked ClearLogo
        // Takes all remaining space above the single active row and stays completely stationary
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            val reorderingRowTitle = state.reorderingTarget?.let { t ->
              resolvedRows.firstOrNull { it.id == t.rowId }?.title
            }
            LauncherTopBar(
              accent = accent,
              showClock = state.settings.showClock,
              showDate = state.settings.showDate,
              use24HourClock = state.settings.use24HourClock,
              clockStyle = clockStyle,
              clockFontStyle = clockFontStyle,
              clockFontWeight = clockFontWeight,
              showSearchButton = state.settings.showSearchBar,
              isSearchActive = state.isSearchVisible,
              showTvInputsButton = state.settings.showTvInputsButton && state.tvInputs.isNotEmpty(),
              weatherData = state.weatherData,
              weatherEnabled = state.settings.weatherEnabled,
              weatherDays = state.settings.weatherDays,
              weatherShowForecast = state.settings.weatherShowForecast,
              weatherForecastSize = WeatherForecastSize.fromId(state.settings.weatherForecastSize),
              weatherIconStyle = WeatherIconStyle.fromId(state.settings.weatherIconStyle),
              weatherShowBackdrop = state.settings.weatherShowBackdrop,
              onToggleSearch = onToggleSearch,
              onOpenTvInputs = onOpenTvInputs,
              onOpenCustomize = onOpenSettings,
              onOpenSystemSettings = onOpenSystemSettings,
              topBarFocusRequester = topBarFocusRequester,
              downFocusRequester = rowEntryRequester(heroRow),
              onTopBarFocused = {
                isHeroEntryFocused = false
                requestDockScroll(0)
              },
            )

            if (reorderingRowTitle != null) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.56f),
                border = BorderStroke(1.5.dp, accent.primary),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = null,
                    tint = accent.primary,
                    modifier = Modifier.size(20.dp),
                  )
                  Text(
                    text = "Reordering $reorderingRowTitle — press Left / Right to move, OK or Back when done",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                  )
                }
              }
            }
          }

          // Bottom-Right Locked ClearLogo / Reddit Credit Slot
          val activeSlide = if (state.settings.jellyfinEnabled && state.settings.activeWallpaperShowTitle) {
            state.jellyfinSlide
          } else {
            null
          }
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(bottom = 8.dp),
            contentAlignment = Alignment.BottomEnd,
          ) {
            Crossfade(
              targetState = activeSlide,
              animationSpec = tween(durationMillis = 1200),
              modifier = Modifier
                .width(340.dp)
                .height(104.dp),
              label = "jellyfinLogoCrossfade",
            ) { slide ->
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd,
              ) {
                if (slide != null) {
                  val logo = slide.logoImage
                  if (logo != null) {
                    Image(
                      bitmap = logo,
                      contentDescription = slide.backdrop.title,
                      contentScale = ContentScale.Fit,
                      alignment = Alignment.BottomEnd,
                      modifier = Modifier.fillMaxSize(),
                    )
                  } else if (state.settings.activeWallpaperProvider == MediaWallpaperProvider.REDDIT) {
                    Surface(
                      shape = RoundedCornerShape(50),
                      color = Color.Black.copy(alpha = 0.42f),
                      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                      Text(
                        text = slide.backdrop.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                      )
                    }
                  } else {
                    Text(
                      text = slide.backdrop.title,
                      style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(
                          color = Color.Black.copy(alpha = 0.75f),
                          offset = Offset(0f, 3f),
                          blurRadius = 14f,
                        ),
                      ),
                      color = Color.White.copy(alpha = 0.92f),
                      textAlign = TextAlign.End,
                    )
                  }
                }
              }
            }
          }
        }

        // Single-Row Independently Scrollable Bottom Dock (Custom Rows + Optional All Apps)
        // Sized to exactly one row so only the active row is visible at a time (no peeking next row).
        // Uses NoAutoBringIntoViewSpec on the vertical dock so Compose never fights our exact row alignment,
        // while restoring default BringIntoViewSpec inside so horizontal LazyRow scrolling stays native.
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(singleRowDockHeight)
            .padding(horizontal = 16.dp),
        ) {
          val bottomScrollSlack = if (extraRows.isNotEmpty() || state.settings.showAllAppsSection) {
            singleRowDockHeight
          } else {
            0.dp
          }
          val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current

          CompositionLocalProvider(LocalBringIntoViewSpec provides NoAutoBringIntoViewSpec) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
                .padding(bottom = bottomScrollSlack),
              verticalArrangement = Arrangement.spacedBy(48.dp),
            ) {
              CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                // Primary Row (Row 0)
                val heroDownTarget = if (extraRows.isNotEmpty()) {
                  rowEntryRequester(extraRows.first())
                } else {
                  firstAllAppsTarget
                }
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                      rowYOffsets[heroRow.id] = coords.positionInParent().y.toInt()
                    },
                ) {
                  CustomAppRowSection(
                    row = heroRow,
                    accent = accent,
                    cardSurface = theme.cardSurface,
                    tileSize = tileSize,
                    listState = rowListStates.getOrPut(heroRow.id) { LazyListState() },
                    reorderingPackage = if (state.reorderingTarget?.rowId == heroRow.id) {
                      state.reorderingTarget.packageName
                    } else {
                      null
                    },
                    upFocusRequester = topBarFocusRequester,
                    downFocusRequester = heroDownTarget,
                    focusRequesterFor = { pkg ->
                      rowCardFocusRequesters.getOrPut("${heroRow.id}:$pkg") { FocusRequester() }
                    },
                    addTileFocusRequester = rowAddFocusRequesters.getOrPut(heroRow.id) { FocusRequester() },
                    onAppFocused = { app ->
                      lastFocusedPackageByRow[heroRow.id] = app?.packageName
                      val firstPkg = heroRow.apps.firstOrNull()?.packageName
                      isHeroEntryFocused = (firstPkg == null && app == null) ||
                        (firstPkg != null && app?.packageName == firstPkg)
                      onAppFocused(app)
                      requestDockScroll(0)
                    },
                    onLaunchApp = onLaunchApp,
                    onOpenAppMenu = { app -> onOpenAppMenu(app, heroRow.id) },
                    onMoveApp = { pkg, delta -> onMoveAppInRow(heroRow.id, pkg, delta) },
                    onFinishReorder = onStopReordering,
                    showTileTitles = state.settings.showTileTitles,
                    isAppPinLocked = { pkg -> state.settings.isAppPinLocked(pkg) },
                    showHeader = !minimalistMode,
                    compactVerticalPadding = minimalistMode,
                    tileOpacity = tileOpacity,
                    tileSpacingDp = tileSpacingDp,
                    cornerStyle = tileCornerStyle,
                    focusStyle = tileFocusStyle,
                    showAddTile = state.settings.showAddAppTile && !state.settings.lockHomeLayout,
                    onAddApps = { onOpenAddAppsToRow(heroRow.id) },
                  )
                }

                // Extra Custom Rows (Rows 1..N) with explicit Up/Down focus chain
                extraRows.forEachIndexed { extraIdx, extraRow ->
                  val prevRow = if (extraIdx == 0) heroRow else extraRows[extraIdx - 1]
                  val nextTarget = if (extraIdx < extraRows.lastIndex) {
                    rowEntryRequester(extraRows[extraIdx + 1])
                  } else {
                    firstAllAppsTarget
                  }
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .onGloballyPositioned { coords ->
                        rowYOffsets[extraRow.id] = coords.positionInParent().y.toInt()
                      },
                  ) {
                    CustomAppRowSection(
                      row = extraRow,
                      accent = accent,
                      cardSurface = theme.cardSurface,
                      tileSize = tileSize,
                      listState = rowListStates.getOrPut(extraRow.id) { LazyListState() },
                      reorderingPackage = if (state.reorderingTarget?.rowId == extraRow.id) {
                        state.reorderingTarget.packageName
                      } else {
                        null
                      },
                      upFocusRequester = rowEntryRequester(prevRow),
                      downFocusRequester = nextTarget,
                      focusRequesterFor = { pkg ->
                        rowCardFocusRequesters.getOrPut("${extraRow.id}:$pkg") { FocusRequester() }
                      },
                      addTileFocusRequester = rowAddFocusRequesters.getOrPut(extraRow.id) { FocusRequester() },
                      onAppFocused = { app ->
                        lastFocusedPackageByRow[extraRow.id] = app?.packageName
                        isHeroEntryFocused = false
                        onAppFocused(app)
                        requestDockScroll(rowYOffsets[extraRow.id] ?: 0)
                      },
                      onLaunchApp = onLaunchApp,
                      onOpenAppMenu = { app -> onOpenAppMenu(app, extraRow.id) },
                      onMoveApp = { pkg, delta -> onMoveAppInRow(extraRow.id, pkg, delta) },
                      onFinishReorder = onStopReordering,
                      showTileTitles = state.settings.showTileTitles,
                      isAppPinLocked = { pkg -> state.settings.isAppPinLocked(pkg) },
                      showHeader = true,
                      compactVerticalPadding = minimalistMode,
                      tileOpacity = tileOpacity,
                      tileSpacingDp = tileSpacingDp,
                      showAddTile = state.settings.showAddAppTile && !state.settings.lockHomeLayout,
                      onAddApps = { onOpenAddAppsToRow(extraRow.id) },
                    )
                  }
                }

                if (state.settings.showAllAppsSection) {
                  val lastCustomRowEntry = rowEntryRequester(resolvedRows.last())

                  // All Apps Header + Category Filter Pills
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .onGloballyPositioned { coords ->
                        allAppsHeaderY = coords.positionInParent().y.toInt()
                      }
                      .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = null,
                        tint = accent.primary,
                        modifier = Modifier.size(20.dp),
                      )
                      Text(
                        text = "All Apps",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                      )
                      Text(
                        text = "(${filteredApps.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.58f),
                      )
                    }

                    if (state.settings.showNonLeanbackApps) {
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppFilter.entries.forEachIndexed { idx, filter ->
                          FilterChipPill(
                            label = filter.label,
                            selected = state.selectedFilter == filter,
                            accent = accent,
                            onClick = { onSelectFilter(filter) },
                            modifier = Modifier
                              .then(if (idx == 0) Modifier.focusRequester(filterFocusRequester) else Modifier)
                              .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                  requestDockScroll(allAppsHeaderY)
                                }
                              }
                              .focusProperties {
                                up = lastCustomRowEntry
                                if (appRows.isNotEmpty()) down = gridTileRequester(0, 0)
                              },
                          )
                        }
                      }
                    }
                  }

                  // All Apps Grid Rows with deterministic Up/Down focus wiring
                  if (appRows.isEmpty()) {
                    Surface(
                      shape = RoundedCornerShape(16.dp),
                      color = Color.Black.copy(alpha = 0.34f),
                      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    ) {
                      Text(
                        text = "No apps found in this category.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.76f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                      )
                    }
                  } else {
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                          allAppsGridY = coords.positionInParent().y.toInt()
                        },
                      verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                      appRows.forEachIndexed { rowIndex, rowApps ->
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                              gridRowYOffsets[rowIndex] = coords.positionInParent().y.toInt()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                          horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                          rowApps.forEachIndexed { colIndex, app ->
                            val upTarget = if (rowIndex == 0) {
                              if (state.settings.showNonLeanbackApps) filterFocusRequester else lastCustomRowEntry
                            } else {
                              gridTileRequester(rowIndex - 1, colIndex)
                            }
                            val downTarget = if (rowIndex < appRows.lastIndex) {
                              val nextRow = appRows[rowIndex + 1]
                              gridTileRequester(rowIndex + 1, colIndex.coerceAtMost(nextRow.lastIndex))
                            } else {
                              null
                            }
                            AppGridTile(
                              app = app,
                              accent = accent,
                              cardSurface = theme.cardSurface,
                              isFavorite = app.packageName in pinnedSet,
                              isPinLocked = state.settings.isAppPinLocked(app.packageName),
                              tileOpacity = tileOpacity,
                              onFocused = {
                                onAppFocused(app)
                                val targetY = if (rowIndex == 0) {
                                  allAppsHeaderY
                                } else {
                                  (allAppsGridY + (gridRowYOffsets[rowIndex] ?: 0)).coerceAtLeast(0)
                                }
                                requestDockScroll(targetY)
                              },
                              onLaunch = { onLaunchApp(app) },
                              onOpenMenu = { onOpenAppMenu(app, null) },
                              showTitle = state.settings.showTileTitles,
                              tileFocusRequester = gridTileRequester(rowIndex, colIndex),
                              upFocusRequester = upTarget,
                              downFocusRequester = downTarget,
                              modifier = Modifier.weight(1f),
                            )
                          }
                          val emptySlots = columns - rowApps.size
                          repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Subtle Bottom Status Toast
    AnimatedVisibility(
      visible = state.statusMessage != null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 28.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xEE0F172A),
        border = BorderStroke(1.5.dp, accent.primary),
        tonalElevation = 8.dp,
      ) {
        Text(
          text = state.statusMessage.orEmpty(),
          style = MaterialTheme.typography.labelLarge,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
        )
      }
    }

    // Isolated-Window App Context Action Menu
    val target = state.menuTarget
    if (target != null) {
      AppActionDialog(
        app = target.app,
        allRows = state.settings.rows,
        sourceRowId = target.rowId,
        isLayoutLocked = state.settings.lockHomeLayout,
        isPinEnabled = state.settings.isPinEnabled,
        isAppPinLocked = state.settings.isAppPinLocked(target.app.packageName),
        accent = accent,
        onDismiss = onCloseAppMenu,
        onLaunch = { onLaunchApp(target.app) },
        onToggleAppInRow = { rowId -> onToggleAppInRow(rowId, target.app.packageName) },
        onStartReorder = { rowId -> onStartReordering(rowId, target.app.packageName) },
        onMoveLeft = { rowId -> onMoveAppInRow(rowId, target.app.packageName, -1) },
        onMoveRight = { rowId -> onMoveAppInRow(rowId, target.app.packageName, +1) },
        onRenameApp = { onOpenRenameApp(target.app) },
        onPickCustomIcon = { onPickCustomIcon(target.app.packageName) },
        onResetCustomIcon = { onResetCustomIcon(target.app.packageName) },
        onHideApp = { onHideApp(target.app.packageName) },
        onTogglePinLock = { onToggleAppPinLock(target.app) },
        onOpenAppInfo = { onOpenAppInfo(target.app.packageName) },
        onUninstall = { onUninstallApp(target.app.packageName) },
      )
    }

    // Rename App Dialog
    val renaming = state.renamingApp
    if (renaming != null) {
      RenameTextDialog(
        title = "Rename App",
        subtitle = "Original name: ${renaming.originalLabel}",
        initialValue = renaming.label,
        placeholder = renaming.originalLabel,
        resetLabel = "Reset",
        accent = accent,
        onDismiss = onCloseRenameApp,
        onSave = { newName -> onRenameApp(renaming.packageName, newName) },
        onReset = { onRenameApp(renaming.packageName, "") },
      )
    }

    // Create Extra Row Dialog
    if (state.isCreatingRow) {
      RenameTextDialog(
        title = "Add Extra Row",
        subtitle = "Name your new home screen row (you can pick apps next)",
        initialValue = "",
        placeholder = "e.g. Streaming, Games, Movies, Kids…",
        resetLabel = "Clear",
        accent = accent,
        onDismiss = onCloseCreateRow,
        onSave = onCreateRow,
        onReset = { onCreateRow("Row ${state.settings.rows.size + 1}") },
      )
    }

    // Rename Any Row Dialog (opened from Customize)
    val renamingRow = state.renamingRowId?.let { id ->
      state.settings.rows.firstOrNull { it.id == id }
    }
    if (renamingRow != null) {
      RenameTextDialog(
        title = "Rename Row",
        subtitle = "Customize the title for this row",
        initialValue = renamingRow.title,
        placeholder = "Favorites",
        resetLabel = "Default",
        accent = accent,
        onDismiss = onCloseRenameRow,
        onSave = { newTitle -> onRenameRow(renamingRow.id, newTitle) },
        onReset = { onRenameRow(renamingRow.id, "") },
      )
    }

    // Quick "+ Add App" Picker Dialog for any Row
    val addingRow = state.addingAppsToRowId?.let { id ->
      state.settings.rows.firstOrNull { it.id == id }
    }
    if (addingRow != null) {
      AddFavoritePickerDialog(
        rowTitle = addingRow.title,
        allApps = state.allVisibleApps,
        favoritePackages = addingRow.packageNames.toSet(),
        accent = accent,
        onToggleFavorite = { pkg -> onToggleAppInRow(addingRow.id, pkg) },
        onDismiss = onCloseAddAppsToRow,
      )
    }

    val orderingRow = state.orderingRowId?.let { id ->
      resolvedRows.firstOrNull { it.id == id }
    }
    if (orderingRow != null) {
      TileOrderDialog(
        rowTitle = orderingRow.title,
        orderedApps = orderingRow.apps,
        accent = accent,
        onMoveApp = { pkg, delta -> onMoveAppInRow(orderingRow.id, pkg, delta) },
        onSortAlphabetically = { onSortRowApps(orderingRow.id) },
        onDismiss = onCloseTileOrder,
      )
    }

    // Media Wallpaper Server Setup Dialog (Jellyfin, Plex, Emby)
    val setupProvider = state.setupProviderDialog
    if (setupProvider != null) {
      JellyfinSetupDialog(
        provider = setupProvider,
        initialServerUrl = state.settings.serverUrlFor(setupProvider),
        initialApiKey = state.settings.tokenFor(setupProvider),
        isTesting = state.isTestingJellyfin,
        errorMessage = state.jellyfinError,
        isConfigured = state.settings.isProviderConfigured(setupProvider),
        accent = accent,
        onDismiss = onCloseJellyfinSetup,
        onConnect = onConnectJellyfin,
        onDisconnect = onDisconnectJellyfin,
      )
    }

    // Add Custom Reddit Subreddit Dialog
    if (state.isAddingSubreddit) {
      RenameTextDialog(
        title = "Add Custom Subreddit",
        subtitle = "Enter any subreddit name (e.g. MostBeautiful, MinimalWallpaper, Cyberpunk)",
        initialValue = "",
        placeholder = "MostBeautiful",
        resetLabel = "Clear",
        accent = accent,
        onDismiss = onCloseAddSubreddit,
        onSave = onAddCustomSubreddit,
        onReset = onCloseAddSubreddit,
      )
    }

    // Weather Location Search Dialog
    if (state.isWeatherDialogOpen) {
      WeatherSetupDialog(
        currentLocationName = state.settings.weatherLocationName,
        isSearching = state.isSearchingWeather,
        searchResults = state.weatherSearchResults,
        errorMessage = state.weatherError,
        accent = accent,
        onDismiss = onCloseWeatherSetup,
        onSearch = onSearchWeatherLocations,
        onSelectLocation = onSelectWeatherLocation,
      )
    }

    // Isolated-Window Universal Multi-Provider Search Overlay
    if (state.isSearchVisible) {
      val enabledExternalProviders = state.externalSearchProviders.filter {
        state.settings.isSearchProviderEnabled(it.id)
      }
      UniversalSearchDialog(
        query = state.searchQuery,
        selectedProvider = state.searchProvider,
        matchingApps = state.searchMatchingApps,
        isJellyfinConfigured = state.settings.isJellyfinConfigured,
        isSearchingJellyfin = state.isSearchingJellyfin,
        jellyfinResults = state.jellyfinSearchResults,
        jellyfinError = state.jellyfinSearchError,
        isSearchingStreaming = state.isSearchingStreaming,
        isLoadingMoreStreaming = state.isLoadingMoreStreaming,
        streamingResults = state.filteredStreamingSearchResults,
        streamingHasMore = state.streamingHasMore,
        streamingError = state.streamingSearchError,
        isServiceFilterActive = state.settings.justWatchFilterServices || !state.settings.justWatchIncludeRentBuy,
        justWatchRegionCode = state.settings.activeJustWatchRegion.code,
        showRottenTomatoes = state.settings.justWatchShowRottenTomatoes,
        showAgeRating = state.settings.justWatchShowAgeRating,
        openDetailsPopup = state.settings.searchOpenDetailsPopup,
        recentSearches = state.settings.recentSearches,
        externalSearchProviders = enabledExternalProviders,
        appsProviderEnabled = state.settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_APPS),
        jellyfinProviderEnabled = state.settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_JELLYFIN),
        streamingProviderEnabled = state.settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_JUSTWATCH),
        youtubeProviderEnabled = state.settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_YOUTUBE),
        webProviderEnabled = state.settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_WEB),
        accent = accent,
        onQueryChange = onUpdateSearchQuery,
        onCommitRecentSearch = onCommitRecentSearch,
        onClearRecentSearches = onClearRecentSearches,
        onVoiceSearch = onVoiceSearch,
        onSelectProvider = onSelectSearchProvider,
        onLaunchApp = onLaunchApp,
        onOpenJellyfinItem = onOpenJellyfinSearchItem,
        onOpenStreamingOffer = onOpenStreamingOffer,
        onLoadMoreStreaming = onLoadMoreStreaming,
        onSearchExternalProvider = onSearchExternalProvider,
        onSearchYouTube = onSearchYouTube,
        onSearchWeb = onSearchWeb,
        onDismiss = onToggleSearch,
      )
    }

    // Isolated-Window Customize & Settings Drawer
    if (state.isSettingsOpen) {
      SettingsOverlay(
        settings = state.settings,
        accent = accent,
        hiddenApps = state.hiddenApps,
        jellyfinBackdropCount = state.jellyfinBackdropCount,
        jellyfinLibraries = state.jellyfinLibraries,
        screensaverPhotoCount = state.screensaverPhotoCount,
        externalSearchProviders = state.externalSearchProviders,
        weatherFetchError = state.weatherFetchError,
        isDefaultHome = state.isDefaultHome,
        appVersionName = appVersionName,
        appVersionCode = appVersionCode,
        onClose = onCloseSettings,
        onCreateRow = onOpenCreateRow,
        onManageRowApps = onOpenAddAppsToRow,
        onOrderRowApps = onOpenTileOrder,
        onRenameRow = onOpenRenameRow,
        onMoveRowUp = { rowId -> onMoveRow(rowId, -1) },
        onMoveRowDown = { rowId -> onMoveRow(rowId, +1) },
        onDeleteRow = onDeleteRow,
        onSelectTheme = onSelectTheme,
        onSelectAccent = onSelectAccent,
        onPickWallpaper = onPickWallpaper,
        onClearWallpaper = onClearWallpaper,
        onSetWallpaperDim = onSetWallpaperDim,
        onToggleWallpaperKenBurns = onToggleWallpaperKenBurns,
        onSelectWallpaperProvider = onSelectWallpaperProvider,
        onOpenJellyfinSetup = onOpenJellyfinSetup,
        onToggleJellyfinSlideshow = onToggleJellyfinSlideshow,
        onSetJellyfinInterval = onSetJellyfinInterval,
        onToggleJellyfinShowTitle = onToggleJellyfinShowTitle,
        onSetJellyfinMediaType = onSetJellyfinMediaType,
        onSelectJellyfinLibrary = onSelectJellyfinLibrary,
        onSetJellyfinBackdropLimit = onSetJellyfinBackdropLimit,
        onNextJellyfinBackdrop = onNextJellyfinBackdrop,
        onSetRedditSort = onSetRedditSort,
        onOpenAddSubreddit = onOpenAddSubreddit,
        onOpenWeatherSetup = onOpenWeatherSetup,
        onToggleWeatherEnabled = onToggleWeatherEnabled,
        onSetWeatherDays = onSetWeatherDays,
        onToggleWeatherShowForecast = onToggleWeatherShowForecast,
        onSetWeatherForecastSize = onSetWeatherForecastSize,
        onToggleWeatherUnit = onToggleWeatherUnit,
        onSetWeatherIconStyle = onSetWeatherIconStyle,
        onToggleWeatherBackdrop = onToggleWeatherBackdrop,
        onRefreshWeather = onRefreshWeather,
        onOpenSelfAppInfo = { onOpenAppInfo("com.horizontv.launcher") },
        onToggleShowSearchBar = onToggleShowSearchBar,
        onToggleSearchOpenDetailsPopup = onToggleSearchOpenDetailsPopup,
        onToggleSearchProvider = onToggleSearchProvider,
        onToggleJustWatchFilterServices = onToggleJustWatchFilterServices,
        onToggleJustWatchService = onToggleJustWatchService,
        onSelectAllJustWatchServices = onSelectAllJustWatchServices,
        onClearAllJustWatchServices = onClearAllJustWatchServices,
        onToggleJustWatchIncludeRentBuy = onToggleJustWatchIncludeRentBuy,
        onToggleJustWatchHideUnmatchedTitles = onToggleJustWatchHideUnmatchedTitles,
        onSetJustWatchRegion = onSetJustWatchRegion,
        onToggleJustWatchShowRottenTomatoes = onToggleJustWatchShowRottenTomatoes,
        onToggleJustWatchShowAgeRating = onToggleJustWatchShowAgeRating,
        onToggleShowAllAppsSection = onToggleShowAllAppsSection,
        onToggleShowAddAppTile = onToggleShowAddAppTile,
        onToggleShowTileTitles = onToggleShowTileTitles,
        onToggleShowTvInputsButton = onToggleShowTvInputsButton,
        tvInputsCount = state.tvInputs.size,
        onToggleLockHomeLayout = onToggleLockHomeLayout,
        onSetHomeTileSize = onSetHomeTileSize,
        onToggleMinimalistDockMode = onToggleMinimalistDockMode,
        onSetTileOpacity = onSetTileOpacity,
        onSetTileSpacing = onSetTileSpacing,
        onSetTileCornerStyle = onSetTileCornerStyle,
        onSetTileFocusStyle = onSetTileFocusStyle,
        onCycleRowTileShape = onCycleRowTileShape,
        onSetClockStyle = onSetClockStyle,
        onSetClockFontStyle = onSetClockFontStyle,
        onSetClockFontWeight = onSetClockFontWeight,
        onToggleShowClock = onToggleShowClock,
        onToggleShowDate = onToggleShowDate,
        onToggle24HourClock = onToggle24HourClock,
        onSetGridColumns = onSetGridColumns,
        onToggleShowNonLeanback = onToggleShowNonLeanback,
        onUnhideApp = onUnhideApp,
        lockedApps = state.lockedApps,
        onRequestSetPin = onRequestSetPin,
        onRequestChangePin = onRequestChangePin,
        onRequestRemovePin = onRequestRemovePin,
        onTogglePinProtectSettings = onTogglePinProtectSettings,
        onUnlockAppPin = onUnlockAppPin,
        onOpenSetupWizard = onOpenSetupWizard,
        onStartWebCompanion = onStartWebCompanion,
        onOpenHomeSettings = onOpenHomeSettings,
        onOpenPlayStore = onOpenPlayStore,
        onBackupConfig = onBackupConfig,
        onRestoreConfig = onRestoreConfig,
        onResetToDefaults = onResetToDefaults,
        onRefreshApps = onRefreshApps,
        onPreviewScreensaver = onStartScreensaver,
        onToggleScreensaverEnabled = onToggleScreensaverEnabled,
        onSetScreensaverMode = onSetScreensaverMode,
        onSetScreensaverTimeoutMinutes = onSetScreensaverTimeoutMinutes,
        onSetScreensaverIntervalSeconds = onSetScreensaverIntervalSeconds,
        onToggleScreensaverKenBurns = onToggleScreensaverKenBurns,
        onToggleScreensaverShowClock = onToggleScreensaverShowClock,
        onToggleScreensaverShowWeather = onToggleScreensaverShowWeather,
        onToggleScreensaverShowTitle = onToggleScreensaverShowTitle,
        onToggleScreensaverBurnInShift = onToggleScreensaverBurnInShift,
        onSetScreensaverDimAlpha = onSetScreensaverDimAlpha,
        onToggleScreensaverAmbientCycle = onToggleScreensaverAmbientCycle,
        onPickScreensaverPhotoFolder = onPickScreensaverPhotoFolder,
        onPickScreensaverPhotos = onPickScreensaverPhotos,
        onClearScreensaverPhotos = onClearScreensaverPhotos,
        onOpenSystemScreensaverSettings = onOpenSystemScreensaverSettings,
        availableUpdate = state.availableUpdate,
        latestRelease = state.latestRelease,
        isCheckingForUpdate = state.isCheckingForUpdate,
        isDownloadingUpdate = state.isDownloadingUpdate,
        updateStatusText = state.updateStatusText,
        onCheckForUpdates = onCheckForUpdates,
        onLoadLatestRelease = onLoadLatestRelease,
        onSetUpdateCheckSchedule = onSetUpdateCheckSchedule,
        onOpenUpdateDialog = onOpenUpdateDialog,
      )
    }

    if (state.isTvInputsDialogOpen) {
      TvInputsDialog(
        inputs = state.tvInputs,
        accent = accent,
        onSelectInput = onSelectTvInput,
        onDismiss = onCloseTvInputs,
      )
    }

    if (state.showUpdateDialog && state.availableUpdate != null) {
      AppUpdateDialog(
        currentVersionName = appVersionName,
        update = state.availableUpdate,
        isDownloading = state.isDownloadingUpdate,
        downloadProgress = state.updateDownloadProgress,
        statusText = state.updateStatusText,
        accent = accent,
        onUpdateNow = onInstallUpdate,
        onDismiss = onDismissUpdateDialog,
      )
    }

    val pinPrompt = state.pinPrompt
    if (pinPrompt != null) {
      PinDialog(
        prompt = pinPrompt,
        currentPin = state.settings.parentalPin,
        accent = accent,
        onPinVerified = onPinVerified,
        onNewPinConfirmed = onNewPinConfirmed,
        onDismiss = onDismissPinPrompt,
      )
    }

    if (state.isSetupWizardOpen) {
      SetupWizardDialog(
        settings = state.settings,
        allApps = state.allVisibleApps,
        isDefaultHome = state.isDefaultHome,
        accent = accent,
        onSetMinimalistDock = onSetMinimalistDockMode,
        onSet24HourClock = onSet24HourClock,
        onOpenHomeSettings = onOpenHomeSettings,
        onSelectTheme = onSelectTheme,
        onEnableRedditWallpapers = onEnableRedditWallpapersFromWizard,
        onSetJustWatchRegion = onSetJustWatchRegion,
        onToggleFavoriteApp = { pkg ->
          onToggleAppInRow(state.settings.primaryRow.id, pkg)
        },
        onFinish = onCompleteSetupWizard,
      )
    }

    val companionInfo = state.webCompanionInfo
    if (companionInfo != null) {
      WebCompanionDialog(
        info = companionInfo,
        accent = accent,
        onStopAndClose = onStopWebCompanion,
      )
    }

    // Full-Screen Idle / Preview Screensaver Overlay
    AnimatedVisibility(
      visible = state.isScreensaverActive,
      enter = fadeIn(animationSpec = tween(durationMillis = 650)),
      exit = fadeOut(animationSpec = tween(durationMillis = 280)),
      modifier = Modifier.fillMaxSize(),
    ) {
      ScreensaverOverlay(
        settings = state.settings,
        theme = theme,
        accent = accent,
        artworkSlide = state.jellyfinSlide,
        photoSlide = state.screensaverPhotoSlide,
        weatherData = state.weatherData,
        onDismiss = {
          lastInteractionMillis = System.currentTimeMillis()
          onStopScreensaver()
        },
      )
    }
  }
}

@Composable
private fun CustomAppRowSection(
  row: ResolvedAppRow,
  accent: AccentPreset,
  cardSurface: Color,
  tileSize: HomeTileSize,
  listState: LazyListState,
  reorderingPackage: String?,
  upFocusRequester: FocusRequester?,
  downFocusRequester: FocusRequester?,
  focusRequesterFor: (String) -> FocusRequester,
  addTileFocusRequester: FocusRequester,
  onAppFocused: (AppInfo?) -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  onOpenAppMenu: (AppInfo) -> Unit,
  onMoveApp: (String, Int) -> Unit,
  onFinishReorder: () -> Unit,
  showTileTitles: Boolean,
  isAppPinLocked: (String) -> Boolean = { false },
  showHeader: Boolean = true,
  compactVerticalPadding: Boolean = false,
  tileOpacity: Float = 1.0f,
  tileSpacingDp: androidx.compose.ui.unit.Dp = 20.dp,
  cornerStyle: TileCornerStyle = TileCornerStyle.ROUNDED,
  focusStyle: TileFocusStyle = TileFocusStyle.GLOW,
  showAddTile: Boolean,
  onAddApps: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    if (showHeader) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = row.title,
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
        )
        Text(
          text = "(${row.apps.size})",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.58f),
        )
      }
    }

    val hasAddTile = showAddTile || row.apps.isEmpty()
    val verticalPad = if (compactVerticalPadding) 14.dp else 22.dp
    LazyRow(
      state = listState,
      horizontalArrangement = Arrangement.spacedBy(tileSpacingDp),
      contentPadding = PaddingValues(top = verticalPad, bottom = verticalPad, start = 36.dp, end = 36.dp),
    ) {
      itemsIndexed(
        items = row.apps,
        key = { _, app -> "${row.id}_${app.packageName}" },
      ) { index, app ->
        FavoriteBannerCard(
          app = app,
          accent = accent,
          cardSurface = cardSurface,
          isReordering = reorderingPackage == app.packageName,
          canMoveLeft = index > 0,
          canMoveRight = index < row.apps.lastIndex,
          onFocused = { onAppFocused(app) },
          onLaunch = { onLaunchApp(app) },
          onOpenMenu = { onOpenAppMenu(app) },
          onMoveLeft = { onMoveApp(app.packageName, -1) },
          onMoveRight = { onMoveApp(app.packageName, +1) },
          onFinishReorder = onFinishReorder,
          tileSize = tileSize,
          tileShape = row.tileShape,
          cornerStyle = cornerStyle,
          focusStyle = focusStyle,
          showTitle = showTileTitles,
          isPinLocked = isAppPinLocked(app.packageName),
          tileOpacity = tileOpacity,
          blockLeft = index == 0,
          blockRight = index == row.apps.lastIndex && !hasAddTile,
          cardFocusRequester = focusRequesterFor(app.packageName),
          upFocusRequester = upFocusRequester,
          downFocusRequester = downFocusRequester,
        )
      }
      if (hasAddTile) {
        item(key = "${row.id}_add_tile") {
          AddFavoriteTile(
            accent = accent,
            cardSurface = cardSurface,
            onFocused = { onAppFocused(null) },
            onClick = onAddApps,
            tileSize = tileSize,
            tileShape = row.tileShape,
            cornerStyle = cornerStyle,
            focusStyle = focusStyle,
            showTitle = showTileTitles,
            tileOpacity = tileOpacity,
            blockLeft = row.apps.isEmpty(),
            blockRight = true,
            cardFocusRequester = addTileFocusRequester,
            upFocusRequester = upFocusRequester,
            downFocusRequester = downFocusRequester,
          )
        }
      }
    }
  }
}

@Composable
private fun FilterChipPill(
  label: String,
  selected: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      selected -> accent.primary.copy(alpha = 0.30f)
      else -> Color.Black.copy(alpha = 0.34f)
    },
    label = "filterChipBg",
  )
  val contentColor = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(50),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      selected -> BorderStroke(1.5.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    },
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
      color = contentColor,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
    )
  }
}
