package com.horizontv.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizontv.launcher.R
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.AppInfo
import com.horizontv.launcher.model.AppRow
import com.horizontv.launcher.model.ExternalSearchProvider
import com.horizontv.launcher.model.HomeTileSize
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JustWatchServiceCategory
import com.horizontv.launcher.model.JustWatchServicePreset
import com.horizontv.launcher.model.LauncherSettings
import com.horizontv.launcher.model.MediaWallpaperProvider
import com.horizontv.launcher.model.RedditSort
import com.horizontv.launcher.model.ScreensaverMode
import com.horizontv.launcher.model.WeatherCondition
import com.horizontv.launcher.model.WeatherForecastSize
import com.horizontv.launcher.model.WeatherIconStyle
import com.horizontv.launcher.ui.theme.LauncherPalettes

private enum class SettingsCategory(
  val title: String,
  val description: String,
  val icon: ImageVector,
) {
  ROWS("Home", "Rows, tile size, clock, date & layout", Icons.Rounded.Home),
  SEARCH("Search", "Search button, JustWatch, Jellyfin & streaming apps", Icons.Rounded.Search),
  JELLYFIN("Wallpapers", "Live wallpapers from Jellyfin, Plex, Emby, TMDB & Reddit", Icons.Rounded.Movie),
  SCREENSAVER("Screensaver", "Photos, wallpaper artwork & ambient mode", Icons.Rounded.Slideshow),
  WEATHER("Weather", "Location, 1–7 day forecast, size & icon style", Icons.Rounded.Cloud),
  APPEARANCE("Appearance", "Ambient themes, custom wallpaper & accent color", Icons.Rounded.Palette),
  SYSTEM("Launcher & Apps", "Default launcher, Play Store, apps & backup", Icons.Rounded.Settings),
  ABOUT("About", "Version, features & launcher details", Icons.Rounded.Info),
}

@Composable
fun SettingsOverlay(
  settings: LauncherSettings,
  accent: AccentPreset,
  hiddenApps: List<AppInfo>,
  jellyfinBackdropCount: Int,
  jellyfinLibraries: List<JellyfinLibrary>,
  screensaverPhotoCount: Int,
  externalSearchProviders: List<ExternalSearchProvider>,
  weatherFetchError: String?,
  isDefaultHome: Boolean,
  appVersionName: String,
  appVersionCode: Int,
  onClose: () -> Unit,
  onCreateRow: () -> Unit,
  onManageRowApps: (String) -> Unit,
  onOrderRowApps: (String) -> Unit,
  onRenameRow: (String) -> Unit,
  onMoveRowUp: (String) -> Unit,
  onMoveRowDown: (String) -> Unit,
  onDeleteRow: (String) -> Unit,
  onSelectTheme: (String) -> Unit,
  onSelectAccent: (String) -> Unit,
  onPickWallpaper: () -> Unit,
  onClearWallpaper: () -> Unit,
  onSetWallpaperDim: (Float) -> Unit,
  onSelectWallpaperProvider: (MediaWallpaperProvider) -> Unit,
  onOpenJellyfinSetup: () -> Unit,
  onToggleJellyfinSlideshow: () -> Unit,
  onSetJellyfinInterval: (Int) -> Unit,
  onToggleJellyfinShowTitle: () -> Unit,
  onSetJellyfinMediaType: (String) -> Unit,
  onSelectJellyfinLibrary: (JellyfinLibrary?) -> Unit,
  onSetJellyfinBackdropLimit: (Int) -> Unit,
  onNextJellyfinBackdrop: () -> Unit,
  onSetRedditSort: (String) -> Unit,
  onOpenAddSubreddit: () -> Unit,
  onOpenWeatherSetup: () -> Unit,
  onToggleWeatherEnabled: () -> Unit,
  onSetWeatherDays: (Int) -> Unit,
  onToggleWeatherShowForecast: () -> Unit,
  onSetWeatherForecastSize: (String) -> Unit,
  onToggleWeatherUnit: () -> Unit,
  onSetWeatherIconStyle: (String) -> Unit,
  onToggleWeatherBackdrop: () -> Unit,
  onRefreshWeather: () -> Unit,
  onOpenSelfAppInfo: () -> Unit,
  onToggleShowSearchBar: () -> Unit,
  onToggleSearchProvider: (String) -> Unit,
  onToggleJustWatchFilterServices: () -> Unit,
  onToggleJustWatchService: (String) -> Unit,
  onSelectAllJustWatchServices: () -> Unit,
  onClearAllJustWatchServices: () -> Unit,
  onToggleJustWatchIncludeRentBuy: () -> Unit,
  onToggleJustWatchHideUnmatchedTitles: () -> Unit,
  onToggleShowAllAppsSection: () -> Unit,
  onToggleShowAddAppTile: () -> Unit,
  onToggleShowTileTitles: () -> Unit,
  onToggleLockHomeLayout: () -> Unit,
  onSetHomeTileSize: (String) -> Unit,
  onToggleShowClock: () -> Unit,
  onToggleShowDate: () -> Unit,
  onToggle24HourClock: () -> Unit,
  onSetGridColumns: (Int) -> Unit,
  onToggleShowNonLeanback: () -> Unit,
  onUnhideApp: (String) -> Unit,
  onOpenHomeSettings: () -> Unit,
  onOpenPlayStore: () -> Unit,
  onBackupConfig: () -> Unit,
  onRestoreConfig: () -> Unit,
  onResetToDefaults: () -> Unit = {},
  onRefreshApps: () -> Unit,
  onPreviewScreensaver: () -> Unit,
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
) {
  var activeCategory by remember { mutableStateOf(SettingsCategory.ROWS) }
  var activeSearchSection by remember { mutableStateOf(0) } // 0 = General, 1 = My Services, 2 = Streaming Apps
  var activeServiceCategory by remember { mutableStateOf(JustWatchServiceCategory.MAIN_SERVICES) }
  var confirmResetLauncher by remember { mutableStateOf(false) }
  val tabRequesters = remember {
    SettingsCategory.entries.associateWith { FocusRequester() }
  }
  val rightPaneEntryRequester = remember { FocusRequester() }

  Dialog(
    onDismissRequest = onClose,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    LaunchedEffect(Unit) {
      runCatching { tabRequesters[SettingsCategory.ROWS]?.requestFocus() }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.62f))
        .pointerInput(Unit) { detectTapGestures { onClose() } },
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier
          .width(860.dp)
          .height(540.dp)
          .pointerInput(Unit) { detectTapGestures { /* consume */ } },
        shape = RoundedCornerShape(28.dp),
        color = Color(0xEC0D1422),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.16f)),
        tonalElevation = 16.dp,
      ) {
        Row(modifier = Modifier.fillMaxSize()) {
          // LEFT CATEGORY SIDEBAR
          val sidebarScroll = rememberScrollState()
          Column(
            modifier = Modifier
              .width(248.dp)
              .fillMaxHeight()
              .background(Color(0xFF080D18).copy(alpha = 0.92f))
              .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(
                  text = "Customize",
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color.White,
                )
                Text(
                  text = "Horizon TV Launcher",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.54f),
                )
              }

              Column(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth()
                  .verticalScroll(sidebarScroll),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                SettingsCategory.entries.forEach { category ->
                  val subtitle = when (category) {
                    SettingsCategory.ROWS -> if (settings.lockHomeLayout) {
                      "${settings.rows.size} row${if (settings.rows.size == 1) "" else "s"} • Locked"
                    } else {
                      "${settings.rows.size} row${if (settings.rows.size == 1) "" else "s"}"
                    }
                    SettingsCategory.SEARCH -> if (!settings.showSearchBar) {
                      "Hidden"
                    } else {
                      val enabledCount = buildList {
                        if (settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_APPS)) add("Apps")
                        if (settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_JUSTWATCH)) add("JustWatch")
                        if (settings.isJellyfinConfigured && settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_JELLYFIN)) add("Jellyfin")
                        externalSearchProviders.forEach { p ->
                          if (settings.isSearchProviderEnabled(p.id)) add(p.label)
                        }
                        if (settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_YOUTUBE)) add("YouTube")
                        if (settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_WEB)) add("Web")
                      }.size
                      "ON • $enabledCount provider${if (enabledCount == 1) "" else "s"}"
                    }
                    SettingsCategory.JELLYFIN -> {
                      val provider = settings.activeWallpaperProvider
                      when {
                        settings.jellyfinEnabled && jellyfinBackdropCount > 0 ->
                          "${provider.label} • $jellyfinBackdropCount"
                        settings.jellyfinEnabled -> "${provider.label} • Loading…"
                        settings.isProviderConfigured(provider) -> "${provider.label} • Paused"
                        else -> "${provider.label} • Off"
                      }
                    }
                    SettingsCategory.SCREENSAVER -> if (settings.screensaverEnabled) {
                      "${settings.activeScreensaverMode.label} • ${settings.screensaverTimeoutMinutes}m"
                    } else {
                      "${settings.activeScreensaverMode.label} • Off"
                    }
                    SettingsCategory.WEATHER -> when {
                      settings.weatherEnabled && settings.isWeatherConfigured ->
                        "${settings.weatherLocationName.substringBefore(",")} • ${settings.weatherDays}d"
                      settings.isWeatherConfigured -> "Hidden"
                      else -> "Off"
                    }
                    SettingsCategory.APPEARANCE -> LauncherPalettes.accentById(settings.accentId).name
                    SettingsCategory.SYSTEM -> if (isDefaultHome) "Default Active ✓" else "Set Default"
                    SettingsCategory.ABOUT -> "v$appVersionName"
                  }

                  SidebarCategoryItem(
                    category = category,
                    subtitle = subtitle,
                    selected = activeCategory == category,
                    accent = accent,
                    focusRequester = tabRequesters.getValue(category),
                    rightFocusRequester = rightPaneEntryRequester,
                    onSelect = { activeCategory = category },
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "◄ Categories  •  Settings ►",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.44f),
              modifier = Modifier.padding(horizontal = 8.dp),
            )
          }

          // Subtle vertical separator
          Box(
            modifier = Modifier
              .width(1.dp)
              .fillMaxHeight()
              .background(Color.White.copy(alpha = 0.10f)),
          )

          // RIGHT DETAIL PANE
          val activeTabRequester = tabRequesters.getValue(activeCategory)
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .padding(horizontal = 28.dp, vertical = 22.dp),
          ) {
            // Detail Header
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .background(accent.primary.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = activeCategory.icon,
                    contentDescription = null,
                    tint = accent.primary,
                    modifier = Modifier.size(20.dp),
                  )
                }
                Column {
                  Text(
                    text = activeCategory.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                  )
                  Text(
                    text = activeCategory.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.60f),
                  )
                }
              }

              QuickActionIcon(
                icon = Icons.Rounded.Close,
                contentDescription = "Close Customize",
                accent = accent,
                onClick = onClose,
              )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scrollable Category Content
            val detailScroll = rememberScrollState()
            LaunchedEffect(activeCategory) {
              detailScroll.scrollTo(0)
            }

            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(detailScroll),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              when (activeCategory) {
                SettingsCategory.ROWS -> {
                  SettingsToggleRow(
                    title = "Lock Home Layout",
                    subtitle = if (settings.lockHomeLayout) {
                      "Locked — prevents rearranging, renaming, adding, or removing apps & rows"
                    } else {
                      "Unlocked — freely reorder apps, edit rows & customize icons"
                    },
                    checked = settings.lockHomeLayout,
                    accent = accent,
                    onClick = onToggleLockHomeLayout,
                    modifier = Modifier
                      .focusRequester(rightPaneEntryRequester)
                      .focusProperties { left = activeTabRequester },
                  )

                  if (!settings.lockHomeLayout) {
                    SettingsFieldLabel("Home Rows")
                    settings.rows.forEachIndexed { index, row ->
                      RowManageItem(
                        row = row,
                        isFirst = index == 0,
                        isLast = index == settings.rows.lastIndex,
                        canDelete = settings.rows.size > 1,
                        accent = accent,
                        firstFocusModifier = Modifier.focusProperties { left = activeTabRequester },
                        onManageApps = { onManageRowApps(row.id) },
                        onOrderApps = { onOrderRowApps(row.id) },
                        onRename = { onRenameRow(row.id) },
                        onMoveUp = { onMoveRowUp(row.id) },
                        onMoveDown = { onMoveRowDown(row.id) },
                        onDelete = { onDeleteRow(row.id) },
                      )
                    }
                    SettingsActionChip(
                      icon = Icons.Rounded.Add,
                      label = "Add Extra Row",
                      accent = accent,
                      highlighted = true,
                      onClick = onCreateRow,
                      modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { left = activeTabRequester },
                    )
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  SettingsFieldLabel("Tiles")

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    HomeTileSize.entries.forEachIndexed { idx, tileSize ->
                      SelectablePill(
                        label = "${tileSize.label} Tiles",
                        selected = HomeTileSize.fromId(settings.homeTileSize) == tileSize,
                        accent = accent,
                        onClick = { onSetHomeTileSize(tileSize.id) },
                        modifier = Modifier
                          .weight(1f)
                          .then(
                            if (idx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier,
                          ),
                      )
                    }
                  }

                  SettingsToggleRow(
                    title = "Show App Titles Under Tiles",
                    subtitle = if (settings.showTileTitles) {
                      "Shown — display app names underneath each tile"
                    } else {
                      "Hidden — clean artwork-only tiles with no text underneath"
                    },
                    checked = settings.showTileTitles,
                    accent = accent,
                    onClick = onToggleShowTileTitles,
                    modifier = Modifier.focusProperties { left = activeTabRequester },
                  )

                  if (!settings.lockHomeLayout) {
                    SettingsToggleRow(
                      title = "Show \"+ Add App\" Tile on Rows",
                      subtitle = if (settings.showAddAppTile) {
                        "Enabled — show the + Add App tile at the end of each row"
                      } else {
                        "Hidden — use + above or Hold OK to edit row apps"
                      },
                      checked = settings.showAddAppTile,
                      accent = accent,
                      onClick = onToggleShowAddAppTile,
                      modifier = Modifier.focusProperties { left = activeTabRequester },
                    )
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  SettingsFieldLabel("Clock & Date")

                  SettingsToggleRow(
                    title = "Show Clock",
                    subtitle = if (settings.showClock) "Large hero clock shown top-left" else "Clock hidden",
                    checked = settings.showClock,
                    accent = accent,
                    onClick = onToggleShowClock,
                    modifier = Modifier.focusProperties { left = activeTabRequester },
                  )

                  if (settings.showClock) {
                    SettingsToggleRow(
                      title = "24-Hour Clock",
                      subtitle = if (settings.use24HourClock) "22:45 format (24-hour)" else "10:45 PM format (12-hour)",
                      checked = settings.use24HourClock,
                      accent = accent,
                      onClick = onToggle24HourClock,
                      modifier = Modifier.focusProperties { left = activeTabRequester },
                    )
                  }

                  SettingsToggleRow(
                    title = "Show Date",
                    subtitle = if (settings.showDate) "Day, date & month shown under clock" else "Date hidden",
                    checked = settings.showDate,
                    accent = accent,
                    onClick = onToggleShowDate,
                    modifier = Modifier.focusProperties { left = activeTabRequester },
                  )

                  Spacer(modifier = Modifier.height(4.dp))
                  SettingsFieldLabel("All Apps Grid")

                  SettingsToggleRow(
                    title = "Show All Apps Section",
                    subtitle = "Display the All Apps grid below your custom rows",
                    checked = settings.showAllAppsSection,
                    accent = accent,
                    onClick = onToggleShowAllAppsSection,
                    modifier = Modifier.focusProperties { left = activeTabRequester },
                  )

                  if (settings.showAllAppsSection) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      listOf(4, 5, 6).forEachIndexed { i, cols ->
                        SelectablePill(
                          label = "$cols Columns",
                          selected = settings.gridColumns == cols,
                          accent = accent,
                          onClick = { onSetGridColumns(cols) },
                          modifier = Modifier
                            .weight(1f)
                            .then(if (i == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                        )
                      }
                    }
                  }
                }

                SettingsCategory.SEARCH -> {
                  val justWatchEnabled = settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_JUSTWATCH)
                  val searchTabs = listOf(
                    0 to "General",
                    1 to "My Services (${settings.justWatchServices.size})",
                    2 to "Streaming Apps (${externalSearchProviders.size})",
                  )

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    searchTabs.forEachIndexed { idx, (tabId, tabLabel) ->
                      SelectablePill(
                        label = tabLabel,
                        selected = activeSearchSection == tabId,
                        accent = accent,
                        onClick = { activeSearchSection = tabId },
                        modifier = Modifier
                          .weight(1f)
                          .then(
                            if (idx == 0) {
                              Modifier
                                .focusRequester(rightPaneEntryRequester)
                                .focusProperties { left = activeTabRequester }
                            } else {
                              Modifier
                            },
                          ),
                      )
                    }
                  }

                  when (activeSearchSection) {
                    0 -> {
                      SettingsFieldLabel("Search Bar")
                      SettingsToggleRow(
                        title = "Show Search Button",
                        subtitle = if (settings.showSearchBar) {
                          "Shown in the top-right bar for universal search"
                        } else {
                          "Hidden from the top bar"
                        },
                        checked = settings.showSearchBar,
                        accent = accent,
                        onClick = onToggleShowSearchBar,
                        modifier = Modifier.focusProperties { left = activeTabRequester },
                      )

                      SettingsFieldLabel("Core Search Providers")

                      SettingsToggleRow(
                        title = "Where to Watch (JustWatch UK)",
                        subtitle = "Show which streaming services carry a movie or show",
                        checked = justWatchEnabled,
                        accent = accent,
                        onClick = { onToggleSearchProvider(LauncherSettings.SEARCH_PROVIDER_JUSTWATCH) },
                        modifier = Modifier.focusProperties { left = activeTabRequester },
                      )

                      SettingsToggleRow(
                        title = "Jellyfin Movies & TV Shows",
                        subtitle = if (settings.isJellyfinConfigured) {
                          "Live search your Jellyfin library with vertical posters"
                        } else {
                          "Connect a Jellyfin server in the Wallpapers tab to search your library"
                        },
                        checked = settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_JELLYFIN),
                        accent = accent,
                        onClick = { onToggleSearchProvider(LauncherSettings.SEARCH_PROVIDER_JELLYFIN) },
                        modifier = Modifier.focusProperties { left = activeTabRequester },
                      )

                      SettingsToggleRow(
                        title = "Installed Apps",
                        subtitle = "Show matching installed apps when typing a search query",
                        checked = settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_APPS),
                        accent = accent,
                        onClick = { onToggleSearchProvider(LauncherSettings.SEARCH_PROVIDER_APPS) },
                        modifier = Modifier.focusProperties { left = activeTabRequester },
                      )

                      SettingsToggleRow(
                        title = "YouTube",
                        subtitle = "Quick shortcut to search in YouTube TV / SmartTube",
                        checked = settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_YOUTUBE),
                        accent = accent,
                        onClick = { onToggleSearchProvider(LauncherSettings.SEARCH_PROVIDER_YOUTUBE) },
                        modifier = Modifier.focusProperties { left = activeTabRequester },
                      )

                      SettingsToggleRow(
                        title = "Web Search",
                        subtitle = "Quick shortcut to search the web in your browser",
                        checked = settings.isSearchProviderEnabled(LauncherSettings.SEARCH_PROVIDER_WEB),
                        accent = accent,
                        onClick = { onToggleSearchProvider(LauncherSettings.SEARCH_PROVIDER_WEB) },
                        modifier = Modifier.focusProperties { left = activeTabRequester },
                      )
                    }

                    1 -> {
                      if (!justWatchEnabled) {
                        Surface(
                          shape = RoundedCornerShape(14.dp),
                          color = Color.White.copy(alpha = 0.05f),
                          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                          modifier = Modifier.fillMaxWidth(),
                        ) {
                          Text(
                            text = "Where to Watch (JustWatch UK) is currently turned off. Enable it under General to filter by your streaming services.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.68f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                          )
                        }
                      } else {
                        SettingsFieldLabel("JustWatch Filter Options")

                        SettingsToggleRow(
                          title = "Only Show My Streaming Services",
                          subtitle = if (settings.justWatchFilterServices) {
                            "Filtering results to your ${settings.justWatchServices.size} selected service(s)"
                          } else {
                            "Showing all UK streaming services — turn on to filter to yours"
                          },
                          checked = settings.justWatchFilterServices,
                          accent = accent,
                          onClick = onToggleJustWatchFilterServices,
                          modifier = Modifier.focusProperties { left = activeTabRequester },
                        )

                        SettingsToggleRow(
                          title = "Include Rent & Buy Offers",
                          subtitle = if (settings.justWatchIncludeRentBuy) {
                            "Showing Subscription, Free, Rent, and Buy offers"
                          } else {
                            "Only showing Included with Subscription & Free streams"
                          },
                          checked = settings.justWatchIncludeRentBuy,
                          accent = accent,
                          onClick = onToggleJustWatchIncludeRentBuy,
                          modifier = Modifier.focusProperties { left = activeTabRequester },
                        )

                        if (settings.justWatchFilterServices || !settings.justWatchIncludeRentBuy) {
                          SettingsToggleRow(
                            title = "Hide Titles Not on My Services",
                            subtitle = if (settings.justWatchHideUnmatchedTitles) {
                              "Only showing movies & shows available on your services"
                            } else {
                              "Still showing title cards even when not on your services"
                            },
                            checked = settings.justWatchHideUnmatchedTitles,
                            accent = accent,
                            onClick = onToggleJustWatchHideUnmatchedTitles,
                            modifier = Modifier.focusProperties { left = activeTabRequester },
                          )
                        }

                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                          SettingsFieldLabel("Choose Services (${settings.justWatchServices.size}/${JustWatchServicePreset.entries.size})")
                          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectablePill(
                              label = "Select All",
                              selected = settings.justWatchServices.size == JustWatchServicePreset.entries.size,
                              accent = accent,
                              onClick = onSelectAllJustWatchServices,
                            )
                            SelectablePill(
                              label = "Clear All",
                              selected = false,
                              accent = accent,
                              onClick = onClearAllJustWatchServices,
                            )
                          }
                        }

                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          JustWatchServiceCategory.entries.forEachIndexed { idx, category ->
                            val selectedCount = JustWatchServicePreset.entries.count {
                              it.category == category && it.id in settings.justWatchServices
                            }
                            val shortLabel = when (category) {
                              JustWatchServiceCategory.MAIN_SERVICES -> "Main ($selectedCount)"
                              JustWatchServiceCategory.PRIME_CHANNELS -> "Prime Channels ($selectedCount)"
                              JustWatchServiceCategory.RENT_BUY_STORES -> "Rent & Buy ($selectedCount)"
                            }
                            SelectablePill(
                              label = shortLabel,
                              selected = activeServiceCategory == category,
                              accent = accent,
                              onClick = { activeServiceCategory = category },
                              modifier = Modifier
                                .weight(1f)
                                .then(
                                  if (idx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier,
                                ),
                            )
                          }
                        }

                        val presetsInCategory = JustWatchServicePreset.entries.filter {
                          it.category == activeServiceCategory
                        }
                        Column(
                          modifier = Modifier.fillMaxWidth(),
                          verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          presetsInCategory.chunked(2).forEach { pair ->
                            Row(
                              modifier = Modifier.fillMaxWidth(),
                              horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                              pair.forEachIndexed { colIdx, service ->
                                val isSelected = service.id in settings.justWatchServices
                                SelectablePill(
                                  label = if (isSelected) "✓  ${service.label}" else service.label,
                                  selected = isSelected,
                                  accent = accent,
                                  onClick = { onToggleJustWatchService(service.id) },
                                  modifier = Modifier
                                    .weight(1f)
                                    .then(
                                      if (colIdx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier,
                                    ),
                                )
                              }
                              if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                              }
                            }
                          }
                        }
                      }
                    }

                    else -> {
                      SettingsFieldLabel("Installed Streaming App Shortcuts")

                      if (externalSearchProviders.isEmpty()) {
                        Surface(
                          shape = RoundedCornerShape(14.dp),
                          color = Color.White.copy(alpha = 0.05f),
                          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                          modifier = Modifier.fillMaxWidth(),
                        ) {
                          Text(
                            text = "No extra streaming search apps detected on this device yet. Installed apps like Netflix, Prime Video, Disney+, Crunchyroll, Plex, Apple TV, or Spotify will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.62f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                          )
                        }
                      } else {
                        externalSearchProviders.forEach { provider ->
                          SettingsToggleRow(
                            title = provider.label,
                            subtitle = "Show \"Search ${provider.label}\" shortcut button in Search",
                            checked = settings.isSearchProviderEnabled(provider.id),
                            accent = accent,
                            onClick = { onToggleSearchProvider(provider.id) },
                            modifier = Modifier.focusProperties { left = activeTabRequester },
                          )
                        }
                      }

                      SettingsActionChip(
                        icon = Icons.Rounded.Refresh,
                        label = "Rescan Installed Streaming Apps",
                        accent = accent,
                        highlighted = false,
                        onClick = onRefreshApps,
                        modifier = Modifier
                          .fillMaxWidth()
                          .focusProperties { left = activeTabRequester },
                      )
                    }
                  }
                }

                SettingsCategory.JELLYFIN -> {
                  val activeProvider = settings.activeWallpaperProvider
                  val providerConfig = settings.configFor(activeProvider)
                  val isReddit = activeProvider == MediaWallpaperProvider.REDDIT
                  val isProviderConfigured = settings.isProviderConfigured(activeProvider)
                  val providerServerUrl = settings.serverUrlFor(activeProvider)
                  val providerLibraryIds = settings.libraryIdsFor(activeProvider)

                  SettingsFieldLabel("Wallpaper Provider")
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    MediaWallpaperProvider.entries.forEachIndexed { idx, provider ->
                      SelectablePill(
                        label = provider.label,
                        selected = activeProvider == provider,
                        accent = accent,
                        showCheckmark = true,
                        onClick = { onSelectWallpaperProvider(provider) },
                        modifier = Modifier
                          .weight(1f)
                          .then(
                            if (idx == 0) {
                              Modifier
                                .focusRequester(rightPaneEntryRequester)
                                .focusProperties { left = activeTabRequester }
                            } else {
                              Modifier
                            },
                          ),
                      )
                    }
                  }

                  if (activeProvider.requiresServerConfig) {
                    SettingsActionChip(
                      icon = Icons.Rounded.Movie,
                      label = when {
                        activeProvider == MediaWallpaperProvider.TMDB && isProviderConfigured ->
                          "TMDB: API Key Connected (Change…)"
                        activeProvider == MediaWallpaperProvider.TMDB ->
                          "Connect TMDB API Key…"
                        isProviderConfigured ->
                          "${activeProvider.label} Server: ${providerServerUrl.removePrefix("http://").removePrefix("https://")}"
                        else ->
                          "Connect ${activeProvider.label} Server…"
                      },
                      accent = accent,
                      highlighted = settings.jellyfinEnabled && isProviderConfigured,
                      onClick = onOpenJellyfinSetup,
                      modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { left = activeTabRequester },
                    )
                  }

                  if (isProviderConfigured) {
                    SettingsToggleRow(
                      title = "${activeProvider.label} Slideshow",
                      subtitle = if (settings.jellyfinEnabled) {
                        if (jellyfinBackdropCount > 0) {
                          if (isReddit) {
                            "Playing $jellyfinBackdropCount widescreen Reddit wallpapers"
                          } else {
                            "Playing $jellyfinBackdropCount movie & TV backdrops"
                          }
                        } else {
                          "Loading wallpapers from ${activeProvider.label}…"
                        }
                      } else {
                        "Paused — select to resume slideshow"
                      },
                      checked = settings.jellyfinEnabled,
                      accent = accent,
                      onClick = onToggleJellyfinSlideshow,
                      modifier = Modifier.focusProperties { left = activeTabRequester },
                    )

                    if (settings.jellyfinEnabled) {
                      if (isReddit) {
                        SettingsFieldLabel("Reddit Feed Sort")
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          RedditSort.entries.forEachIndexed { i, sort ->
                            SelectablePill(
                              label = sort.label,
                              selected = settings.redditSort == sort.id,
                              accent = accent,
                              onClick = { onSetRedditSort(sort.id) },
                              modifier = Modifier
                                .weight(1f)
                                .then(if (i == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                            )
                          }
                        }

                        val allSubNames = (LauncherSettings.PRESET_REDDIT_SUBREDDITS + settings.redditCustomSubreddits.sorted()).distinct()
                        SettingsFieldLabel("Subreddits (${settings.redditSubreddits.size} Selected — Multi-Select)")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                          allSubNames.chunked(3).forEach { rowSubs ->
                            Row(
                              modifier = Modifier.fillMaxWidth(),
                              horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                              rowSubs.forEachIndexed { colIdx, sub ->
                                val isSelected = sub in settings.redditSubreddits
                                SelectablePill(
                                  label = "r/$sub",
                                  selected = isSelected,
                                  accent = accent,
                                  showCheckmark = true,
                                  onClick = {
                                    onSelectJellyfinLibrary(
                                      JellyfinLibrary(id = sub, name = "r/$sub", collectionType = "reddit"),
                                    )
                                  },
                                  modifier = Modifier
                                    .weight(1f)
                                    .then(if (colIdx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                                )
                              }
                              repeat(3 - rowSubs.size) {
                                Spacer(modifier = Modifier.weight(1f))
                              }
                            }
                          }
                        }

                        SettingsActionChip(
                          icon = Icons.Rounded.Add,
                          label = "Add Custom Subreddit…",
                          accent = accent,
                          highlighted = false,
                          onClick = onOpenAddSubreddit,
                          modifier = Modifier
                            .fillMaxWidth()
                            .focusProperties { left = activeTabRequester },
                        )
                      } else {
                        SettingsFieldLabel("Media Type (Movies / TV)")
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          JellyfinMediaType.entries.forEachIndexed { i, mediaType ->
                            SelectablePill(
                              label = mediaType.label,
                              selected = providerConfig.mediaType == mediaType.id,
                              accent = accent,
                              onClick = { onSetJellyfinMediaType(mediaType.id) },
                              modifier = Modifier
                                .weight(1f)
                                .then(if (i == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                            )
                          }
                        }

                        if (jellyfinLibraries.isNotEmpty()) {
                          val isTmdb = activeProvider == MediaWallpaperProvider.TMDB
                          val selectedCount = providerLibraryIds.size
                          val noun = if (isTmdb) "Categories" else "Libraries"
                          SettingsFieldLabel(
                            if (selectedCount > 0) {
                              "${activeProvider.label} $noun ($selectedCount Selected — Multi-Select)"
                            } else {
                              "${activeProvider.label} $noun (Select One or More)"
                            },
                          )
                          val allOptions: List<JellyfinLibrary?> = if (isTmdb) jellyfinLibraries else listOf(null) + jellyfinLibraries
                          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            allOptions.chunked(3).forEach { rowLibs ->
                              Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                              ) {
                                rowLibs.forEachIndexed { colIdx, lib ->
                                  val isSelected = if (lib == null) {
                                    providerLibraryIds.isEmpty()
                                  } else {
                                    lib.id in providerLibraryIds
                                  }
                                  SelectablePill(
                                    label = lib?.name ?: "All Libraries",
                                    selected = isSelected,
                                    accent = accent,
                                    showCheckmark = true,
                                    onClick = { onSelectJellyfinLibrary(lib) },
                                    modifier = Modifier
                                      .weight(1f)
                                      .then(if (colIdx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                                  )
                                }
                                repeat(3 - rowLibs.size) {
                                  Spacer(modifier = Modifier.weight(1f))
                                }
                              }
                            }
                          }
                        }
                      }

                      SettingsFieldLabel("Wallpaper Count")
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        listOf(20, 40, 80, 150, 300).forEachIndexed { i, count ->
                          SelectablePill(
                            label = "$count",
                            selected = providerConfig.backdropLimit == count,
                            accent = accent,
                            onClick = { onSetJellyfinBackdropLimit(count) },
                            modifier = Modifier
                              .weight(1f)
                              .then(if (i == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                          )
                        }
                      }

                      SettingsFieldLabel("Slideshow Speed")
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        listOf("15s" to 15, "30s" to 30, "1m" to 60, "5m" to 300).forEachIndexed { i, (label, secs) ->
                          SelectablePill(
                            label = label,
                            selected = providerConfig.intervalSeconds == secs,
                            accent = accent,
                            onClick = { onSetJellyfinInterval(secs) },
                            modifier = Modifier
                              .weight(1f)
                              .then(if (i == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                          )
                        }
                      }

                      SettingsToggleRow(
                        title = if (isReddit) "Show Subreddit Credit" else "Show Movie / Show Logo",
                        subtitle = if (isReddit) {
                          "Show subtle r/subreddit credit in the bottom-right corner"
                        } else {
                          "Overlay the ${activeProvider.label} ClearLogo on the wallpaper"
                        },
                        checked = providerConfig.showTitle,
                        accent = accent,
                        onClick = onToggleJellyfinShowTitle,
                        modifier = Modifier.focusProperties { left = activeTabRequester },
                      )

                      WallpaperDimmingRow(
                        currentDim = settings.wallpaperDimAlpha,
                        accent = accent,
                        leftFocusRequester = activeTabRequester,
                        onSetDim = onSetWallpaperDim,
                      )

                      SettingsActionChip(
                        icon = Icons.Rounded.SkipNext,
                        label = "Next Backdrop Now",
                        accent = accent,
                        highlighted = false,
                        onClick = onNextJellyfinBackdrop,
                        modifier = Modifier
                          .fillMaxWidth()
                          .focusProperties { left = activeTabRequester },
                      )
                    }
                  }
                }

                SettingsCategory.SCREENSAVER -> {
                  val mode = settings.activeScreensaverMode
                  val activeProvider = settings.activeWallpaperProvider

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    SettingsActionChip(
                      icon = Icons.Rounded.PlayArrow,
                      label = "Preview Screensaver Now",
                      accent = accent,
                      highlighted = true,
                      onClick = onPreviewScreensaver,
                      modifier = Modifier
                        .weight(1f)
                        .focusRequester(rightPaneEntryRequester)
                        .focusProperties { left = activeTabRequester },
                    )
                    SettingsActionChip(
                      icon = Icons.Rounded.Settings,
                      label = "Android System Screensaver",
                      accent = accent,
                      highlighted = false,
                      onClick = onOpenSystemScreensaverSettings,
                      modifier = Modifier.weight(1f),
                    )
                  }

                  SettingsToggleRow(
                    title = "Enable Idle Screensaver",
                    subtitle = if (settings.screensaverEnabled) {
                      "Starts automatically after ${settings.screensaverTimeoutMinutes} min of inactivity on Home"
                    } else {
                      "Off — you can still launch Preview or select Horizon in Android System Screensaver"
                    },
                    checked = settings.screensaverEnabled,
                    accent = accent,
                    onClick = onToggleScreensaverEnabled,
                    modifier = Modifier.focusProperties { left = activeTabRequester },
                  )

                  if (settings.screensaverEnabled) {
                    SettingsFieldLabel("Idle Timeout")
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      listOf(1, 3, 5, 10, 15, 30).forEachIndexed { idx, mins ->
                        SelectablePill(
                          label = "${mins}m",
                          selected = settings.screensaverTimeoutMinutes == mins,
                          accent = accent,
                          onClick = { onSetScreensaverTimeoutMinutes(mins) },
                          modifier = Modifier
                            .weight(1f)
                            .then(
                              if (idx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier,
                            ),
                        )
                      }
                    }
                  }

                  SettingsFieldLabel("Screensaver Mode")
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    ScreensaverMode.entries.forEachIndexed { idx, candidate ->
                      SelectablePill(
                        label = candidate.label,
                        selected = mode == candidate,
                        accent = accent,
                        onClick = { onSetScreensaverMode(candidate) },
                        modifier = Modifier
                          .weight(1f)
                          .then(
                            if (idx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier,
                          ),
                      )
                    }
                  }

                  when (mode) {
                    ScreensaverMode.WALLPAPER_ARTWORK -> {
                      Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Column(
                          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                          verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                          Text(
                            text = "Artwork Source: ${activeProvider.label}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                          )
                          Text(
                            text = if (settings.isProviderConfigured(activeProvider)) {
                              "Pulls high-res backdrops & ClearLogos from ${activeProvider.label}. Configure provider & categories in Customize → Wallpapers."
                            } else {
                              "${activeProvider.label} isn't configured yet — configure a provider in Customize → Wallpapers (or switch to Reddit / Ambient Mode)."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.65f),
                          )
                        }
                      }
                    }

                    ScreensaverMode.PHOTOS -> {
                      Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Column(
                          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                          verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                          Text(
                            text = if (screensaverPhotoCount > 0) {
                              "$screensaverPhotoCount photo${if (screensaverPhotoCount == 1) "" else "s"} ready for slideshow"
                            } else {
                              "No photos selected yet"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                          )
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                          ) {
                            SettingsActionChip(
                              icon = Icons.Rounded.FolderOpen,
                              label = "Pick Photo Folder",
                              accent = accent,
                              highlighted = screensaverPhotoCount == 0,
                              onClick = onPickScreensaverPhotoFolder,
                              modifier = Modifier
                                .weight(1f)
                                .focusProperties { left = activeTabRequester },
                            )
                            SettingsActionChip(
                              icon = Icons.Rounded.PhotoLibrary,
                              label = "Pick Photos",
                              accent = accent,
                              highlighted = false,
                              onClick = onPickScreensaverPhotos,
                              modifier = Modifier.weight(1f),
                            )
                          }
                          if (screensaverPhotoCount > 0 || !settings.screensaverPhotoFolderUri.isNullOrBlank() || settings.screensaverPhotoUris.isNotEmpty()) {
                            SettingsActionChip(
                              icon = Icons.Rounded.DeleteOutline,
                              label = "Clear Selected Photos",
                              accent = accent,
                              highlighted = false,
                              onClick = onClearScreensaverPhotos,
                              modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { left = activeTabRequester },
                            )
                          }
                        }
                      }
                    }

                    ScreensaverMode.AMBIENT -> {
                      SettingsToggleRow(
                        title = "Cycle Ambient Themes",
                        subtitle = if (settings.screensaverAmbientCycle) {
                          "Smoothly transitions through all 6 Horizon ambient palettes"
                        } else {
                          "Stays on your current theme (${LauncherPalettes.themeById(settings.themeId).name})"
                        },
                        checked = settings.screensaverAmbientCycle,
                        accent = accent,
                        onClick = onToggleScreensaverAmbientCycle,
                        modifier = Modifier.focusProperties { left = activeTabRequester },
                      )
                    }
                  }

                  if (mode != ScreensaverMode.AMBIENT) {
                    SettingsFieldLabel("Slide Interval")
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      listOf(10 to "10s", 20 to "20s", 30 to "30s", 60 to "1m", 120 to "2m").forEachIndexed { idx, (secs, label) ->
                        SelectablePill(
                          label = label,
                          selected = settings.screensaverIntervalSeconds == secs,
                          accent = accent,
                          onClick = { onSetScreensaverIntervalSeconds(secs) },
                          modifier = Modifier
                            .weight(1f)
                            .then(
                              if (idx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier,
                            ),
                        )
                      }
                    }

                    SettingsToggleRow(
                      title = "Ken Burns Motion",
                      subtitle = "Slow cinematic zoom & gentle pan across each slide",
                      checked = settings.screensaverKenBurns,
                      accent = accent,
                      onClick = onToggleScreensaverKenBurns,
                      modifier = Modifier.focusProperties { left = activeTabRequester },
                    )
                  }

                  SettingsFieldLabel("Overlays & Display")

                  SettingsToggleRow(
                    title = "Show Clock & Date",
                    subtitle = "Floating time & date overlay on the screensaver",
                    checked = settings.screensaverShowClock,
                    accent = accent,
                    onClick = onToggleScreensaverShowClock,
                    modifier = Modifier.focusProperties { left = activeTabRequester },
                  )

                  SettingsToggleRow(
                    title = "Show Weather",
                    subtitle = if (settings.isWeatherConfigured) {
                      "Current temperature & condition for ${settings.weatherLocationName.substringBefore(",")}"
                    } else {
                      "Configure a city in Customize → Weather to display weather"
                    },
                    checked = settings.screensaverShowWeather,
                    accent = accent,
                    onClick = onToggleScreensaverShowWeather,
                    modifier = Modifier.focusProperties { left = activeTabRequester },
                  )

                  if (mode != ScreensaverMode.AMBIENT) {
                    SettingsToggleRow(
                      title = "Show Title / ClearLogo",
                      subtitle = "Display artwork ClearLogo, movie/show title, or photo filename",
                      checked = settings.screensaverShowTitle,
                      accent = accent,
                      onClick = onToggleScreensaverShowTitle,
                      modifier = Modifier.focusProperties { left = activeTabRequester },
                    )
                  }

                  SettingsToggleRow(
                    title = "OLED Burn-In Protection",
                    subtitle = "Gently shifts clock & overlay corners every 90 seconds",
                    checked = settings.screensaverBurnInShift,
                    accent = accent,
                    onClick = onToggleScreensaverBurnInShift,
                    modifier = Modifier.focusProperties { left = activeTabRequester },
                  )

                  SettingsFieldLabel("Night Dimming")
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    listOf(0.0f to "0%", 0.15f to "15%", 0.25f to "25%", 0.40f to "40%", 0.60f to "60%", 0.75f to "75%").forEachIndexed { idx, (alpha, label) ->
                      SelectablePill(
                        label = label,
                        selected = kotlin.math.abs(settings.screensaverDimAlpha - alpha) < 0.04f,
                        accent = accent,
                        onClick = { onSetScreensaverDimAlpha(alpha) },
                        modifier = Modifier
                          .weight(1f)
                          .then(
                            if (idx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier,
                          ),
                      )
                    }
                  }
                }

                SettingsCategory.WEATHER -> {
                  SettingsActionChip(
                    icon = Icons.Rounded.LocationOn,
                    label = if (settings.isWeatherConfigured) {
                      "Location: ${settings.weatherLocationName}"
                    } else {
                      "Set Weather Location…"
                    },
                    accent = accent,
                    highlighted = settings.weatherEnabled,
                    onClick = onOpenWeatherSetup,
                    modifier = Modifier
                      .fillMaxWidth()
                      .focusRequester(rightPaneEntryRequester)
                      .focusProperties { left = activeTabRequester },
                  )

                  if (settings.isWeatherConfigured) {
                    SettingsToggleRow(
                      title = "Show Weather Under Clock",
                      subtitle = if (settings.weatherEnabled) {
                        "${settings.weatherDays}-day forecast (${if (settings.weatherUseFahrenheit) "°F" else "°C"})"
                      } else {
                        "Hidden — select to enable"
                      },
                      checked = settings.weatherEnabled,
                      accent = accent,
                      onClick = onToggleWeatherEnabled,
                      modifier = Modifier.focusProperties { left = activeTabRequester },
                    )

                    if (settings.weatherEnabled) {
                      SettingsToggleRow(
                        title = "Show Multi-Day Forecast",
                        subtitle = if (settings.weatherShowForecast) {
                          "Showing ${settings.weatherDays}-day forecast strip below current temperature"
                        } else {
                          "Current conditions only — multi-day forecast hidden"
                        },
                        checked = settings.weatherShowForecast,
                        accent = accent,
                        onClick = onToggleWeatherShowForecast,
                        modifier = Modifier.focusProperties { left = activeTabRequester },
                      )

                      if (settings.weatherShowForecast) {
                        SettingsFieldLabel("Forecast Days (1 to 7)")
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                          (1..7).forEach { dayCount ->
                            SelectablePill(
                              label = "$dayCount",
                              selected = settings.weatherDays == dayCount,
                              accent = accent,
                              onClick = { onSetWeatherDays(dayCount) },
                              modifier = Modifier
                                .weight(1f)
                                .then(if (dayCount == 1) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                            )
                          }
                        }

                        SettingsFieldLabel("Forecast Days Size")
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          WeatherForecastSize.entries.forEachIndexed { i, sizeOption ->
                            SelectablePill(
                              label = sizeOption.label,
                              selected = settings.weatherForecastSize == sizeOption.id,
                              accent = accent,
                              onClick = { onSetWeatherForecastSize(sizeOption.id) },
                              modifier = Modifier
                                .weight(1f)
                                .then(if (i == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                            )
                          }
                        }
                      }

                      SettingsFieldLabel("Weather Icon Style")
                      WeatherIconStyle.entries.chunked(2).forEach { pair ->
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          pair.forEachIndexed { i, style ->
                            WeatherIconStyleChip(
                              style = style,
                              selected = settings.weatherIconStyle == style.id,
                              accent = accent,
                              onClick = { onSetWeatherIconStyle(style.id) },
                              modifier = Modifier
                                .weight(1f)
                                .then(if (i == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                            )
                          }
                        }
                      }

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        SelectablePill(
                          label = "Celsius (°C)",
                          selected = !settings.weatherUseFahrenheit,
                          accent = accent,
                          onClick = { if (settings.weatherUseFahrenheit) onToggleWeatherUnit() },
                          modifier = Modifier
                            .weight(1f)
                            .focusProperties { left = activeTabRequester },
                        )
                        SelectablePill(
                          label = "Fahrenheit (°F)",
                          selected = settings.weatherUseFahrenheit,
                          accent = accent,
                          onClick = { if (!settings.weatherUseFahrenheit) onToggleWeatherUnit() },
                          modifier = Modifier.weight(1f),
                        )
                      }

                      SettingsToggleRow(
                        title = "Weather Backdrop Box",
                        subtitle = if (settings.weatherShowBackdrop) {
                          "Frosted dark box behind multi-day forecast"
                        } else {
                          "Transparent — floats cleanly over wallpaper"
                        },
                        checked = settings.weatherShowBackdrop,
                        accent = accent,
                        onClick = onToggleWeatherBackdrop,
                        modifier = Modifier.focusProperties { left = activeTabRequester },
                      )

                      SettingsActionChip(
                        icon = Icons.Rounded.Refresh,
                        label = "Refresh Weather Now",
                        accent = accent,
                        highlighted = false,
                        onClick = onRefreshWeather,
                        modifier = Modifier
                          .fillMaxWidth()
                          .focusProperties { left = activeTabRequester },
                      )

                      if (weatherFetchError != null) {
                        Surface(
                          shape = RoundedCornerShape(12.dp),
                          color = Color(0xFF451A1A).copy(alpha = 0.85f),
                          border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.55f)),
                          modifier = Modifier.fillMaxWidth(),
                        ) {
                          Text(
                            text = weatherFetchError,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFECACA),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                          )
                        }
                        SettingsActionChip(
                          icon = Icons.Rounded.Settings,
                          label = "Open Horizon App Info (Allow Network Access)",
                          accent = accent,
                          highlighted = false,
                          onClick = onOpenSelfAppInfo,
                          modifier = Modifier
                            .fillMaxWidth()
                            .focusProperties { left = activeTabRequester },
                        )
                      }
                    }
                  }
                }

                SettingsCategory.APPEARANCE -> {
                  SettingsFieldLabel("Ambient Background Themes")
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LauncherPalettes.themes.chunked(3).forEachIndexed { rowIdx, rowThemes ->
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        rowThemes.forEachIndexed { colIdx, theme ->
                          val isSelected = !settings.jellyfinEnabled &&
                            settings.customWallpaperUri == null &&
                            settings.themeId == theme.id
                          ThemeSwatchCard(
                            name = theme.name,
                            colors = listOf(theme.baseColor, theme.topGlow, theme.bottomGlow),
                            isSelected = isSelected,
                            accent = accent,
                            onClick = { onSelectTheme(theme.id) },
                            modifier = Modifier
                              .weight(1f)
                              .then(if (rowIdx == 0 && colIdx == 0) Modifier.focusRequester(rightPaneEntryRequester) else Modifier)
                              .then(if (colIdx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                          )
                        }
                      }
                    }
                  }

                  SettingsFieldLabel("Custom Photo Wallpaper")
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    SettingsActionChip(
                      icon = Icons.Rounded.Image,
                      label = "Choose Image…",
                      accent = accent,
                      highlighted = !settings.jellyfinEnabled && settings.customWallpaperUri != null,
                      onClick = onPickWallpaper,
                      modifier = Modifier
                        .weight(1f)
                        .focusProperties { left = activeTabRequester },
                    )
                    if (settings.customWallpaperUri != null) {
                      SettingsActionChip(
                        icon = Icons.Rounded.Wallpaper,
                        label = "Reset Theme",
                        accent = accent,
                        highlighted = false,
                        onClick = onClearWallpaper,
                        modifier = Modifier.weight(1f),
                      )
                    }
                  }

                  if (settings.customWallpaperUri != null) {
                    WallpaperDimmingRow(
                      currentDim = settings.wallpaperDimAlpha,
                      accent = accent,
                      leftFocusRequester = activeTabRequester,
                      onSetDim = onSetWallpaperDim,
                    )
                  }

                  SettingsFieldLabel("Accent Color")
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LauncherPalettes.accents.chunked(3).forEach { rowAccents ->
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        rowAccents.forEachIndexed { colIdx, preset ->
                          AccentSwatchChip(
                            preset = preset,
                            isSelected = settings.accentId == preset.id,
                            onClick = { onSelectAccent(preset.id) },
                            modifier = Modifier
                              .weight(1f)
                              .then(if (colIdx == 0) Modifier.focusProperties { left = activeTabRequester } else Modifier),
                          )
                        }
                      }
                    }
                  }
                }

                SettingsCategory.SYSTEM -> {
                  SettingsFieldLabel("Quick Actions")
                  SettingsActionChip(
                    icon = if (isDefaultHome) Icons.Rounded.Check else Icons.Rounded.Home,
                    label = if (isDefaultHome) {
                      "Default Android Launcher Active (Change…)"
                    } else {
                      "Set as Default Android Launcher"
                    },
                    accent = accent,
                    highlighted = isDefaultHome,
                    onClick = onOpenHomeSettings,
                    modifier = Modifier
                      .fillMaxWidth()
                      .focusRequester(rightPaneEntryRequester)
                      .focusProperties { left = activeTabRequester },
                  )

                  SettingsActionChip(
                    icon = Icons.Rounded.Shop,
                    label = "Open Google Play Store",
                    accent = accent,
                    highlighted = false,
                    onClick = onOpenPlayStore,
                    modifier = Modifier
                      .fillMaxWidth()
                      .focusProperties { left = activeTabRequester },
                  )

                  Spacer(modifier = Modifier.height(4.dp))
                  SettingsFieldLabel("App Discovery")
                  SettingsToggleRow(
                    title = "Show Phone / Sideloaded Apps",
                    subtitle = "Include standard Android apps alongside TV apps",
                    checked = settings.showNonLeanbackApps,
                    accent = accent,
                    onClick = onToggleShowNonLeanback,
                    modifier = Modifier.focusProperties { left = activeTabRequester },
                  )

                  SettingsActionChip(
                    icon = Icons.Rounded.Refresh,
                    label = "Refresh Installed Apps",
                    accent = accent,
                    highlighted = false,
                    onClick = onRefreshApps,
                    modifier = Modifier
                      .fillMaxWidth()
                      .focusProperties { left = activeTabRequester },
                  )

                  Spacer(modifier = Modifier.height(4.dp))
                  SettingsFieldLabel("Backup & Restore")
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    SettingsActionChip(
                      icon = Icons.Rounded.Upload,
                      label = "Backup Config…",
                      accent = accent,
                      highlighted = false,
                      onClick = onBackupConfig,
                      modifier = Modifier
                        .weight(1f)
                        .focusProperties { left = activeTabRequester },
                    )
                    SettingsActionChip(
                      icon = Icons.Rounded.Download,
                      label = "Restore Config…",
                      accent = accent,
                      highlighted = false,
                      onClick = onRestoreConfig,
                      modifier = Modifier.weight(1f),
                    )
                  }

                  if (!confirmResetLauncher) {
                    SettingsActionChip(
                      icon = Icons.Rounded.Restore,
                      label = "Reset Launcher to Defaults…",
                      accent = accent,
                      highlighted = false,
                      onClick = { confirmResetLauncher = true },
                      modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { left = activeTabRequester },
                    )
                  } else {
                    Surface(
                      shape = RoundedCornerShape(14.dp),
                      color = Color(0xFF7F1D1D).copy(alpha = 0.28f),
                      border = BorderStroke(1.5.dp, Color(0xFFF87171).copy(alpha = 0.55f)),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        Text(
                          text = "Reset Horizon TV Launcher to defaults?",
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                          color = Color.White,
                        )
                        Text(
                          text = "This clears custom rows, themes, wallpaper connections, weather, search filters, hidden apps, and screensaver settings. Consider backing up your config first.",
                          style = MaterialTheme.typography.bodySmall,
                          color = Color.White.copy(alpha = 0.78f),
                        )
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          SettingsActionChip(
                            icon = Icons.Rounded.Close,
                            label = "Cancel",
                            accent = accent,
                            highlighted = false,
                            onClick = { confirmResetLauncher = false },
                            modifier = Modifier
                              .weight(1f)
                              .focusProperties { left = activeTabRequester },
                          )
                          SettingsActionChip(
                            icon = Icons.Rounded.DeleteOutline,
                            label = "Confirm Reset",
                            accent = accent.copy(
                              primary = Color(0xFFEF4444),
                              onPrimary = Color.White,
                            ),
                            highlighted = true,
                            onClick = {
                              confirmResetLauncher = false
                              onResetToDefaults()
                            },
                            modifier = Modifier.weight(1f),
                          )
                        }
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  SettingsFieldLabel("Hidden Apps (${hiddenApps.size})")
                  if (hiddenApps.isEmpty()) {
                    Text(
                      text = "No apps hidden. Hold OK or press Menu on any app to hide it.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color.White.copy(alpha = 0.55f),
                    )
                  } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      hiddenApps.forEach { hidden ->
                        HiddenAppRestoreRow(
                          app = hidden,
                          accent = accent,
                          onRestore = { onUnhideApp(hidden.packageName) },
                          modifier = Modifier.focusProperties { left = activeTabRequester },
                        )
                      }
                    }
                  }
                }

                SettingsCategory.ABOUT -> {
                  Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = accent.primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.5.dp, accent.primary.copy(alpha = 0.40f)),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Row(
                      modifier = Modifier.padding(18.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                      Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier.size(60.dp),
                      ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                          Image(
                            painter = painterResource(id = R.drawable.ic_launcher_background),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                          )
                          Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Horizon TV Launcher Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                          )
                        }
                      }
                      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                          text = "Horizon TV Launcher",
                          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                          color = Color.White,
                        )
                        Text(
                          text = "Version $appVersionName (Build $appVersionCode)",
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                          color = accent.primary,
                        )
                        Text(
                          text = "A clean, ad-free Android TV home launcher designed for speed, crisp D-pad navigation, and living-room full-bleed wallpapers.",
                          style = MaterialTheme.typography.bodySmall,
                          color = Color.White.copy(alpha = 0.78f),
                        )
                      }
                    }
                  }

                  SettingsFieldLabel("What's New in v2.5")
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      val whatsNew = listOf(
                        "JustWatch My Streaming Services — filter results to your subscriptions & Prime Video Channels (Paramount+, MGM+, Crunchyroll, Discovery+, Shudder, MUBI & more)",
                        "Overview Screen Deep-Links — opening movies & TV shows jumps straight to the title details page on Prime Video, Netflix, Disney+, Apple TV, ITVX, Paramount+ & Crunchyroll",
                        "Jellyfin Search Posters — vertical 2:3 movie & TV posters in search results",
                        "Show/Hide App Titles Under Tiles — toggle under Customize → Home for a cleaner artwork-only dock",
                        "Improved Search Navigation — cleaner empty search screen, organized Search settings & full D-pad return to Search Box and Close",
                      )
                      whatsNew.forEach { bullet ->
                        Text(
                          text = "•  $bullet",
                          style = MaterialTheme.typography.bodySmall,
                          color = Color.White.copy(alpha = 0.82f),
                        )
                      }
                    }
                  }

                  SettingsFieldLabel("Highlights")
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      val highlights = listOf(
                        "Custom Home Rows & Tiles — add, rename, reorder & lock rows, custom app icons, tile sizing & optional tile labels",
                        "Live Wallpapers & Screensaver — Jellyfin, Plex, Emby, TMDB & Reddit widescreen slideshows with ClearLogos, plus idle screensaver & Android DreamService",
                        "Live Weather — Open-Meteo 1–7 day forecast with 4 icon styles, sizing options & transparent mode",
                        "Universal Search — live JustWatch UK availability, Jellyfin library search & direct streaming app shortcuts",
                        "Backup & Restore — portable JSON backup of your full launcher setup across devices",
                      )
                      highlights.forEach { bullet ->
                        Text(
                          text = "•  $bullet",
                          style = MaterialTheme.typography.bodySmall,
                          color = Color.White.copy(alpha = 0.80f),
                        )
                      }
                    }
                  }

                  SettingsFieldLabel("App Details")
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Text(
                        text = "Package: com.horizontv.launcher",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.74f),
                      )
                      Text(
                        text = "Default Home Launcher: ${if (isDefaultHome) "Active ✓" else "Not set"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.74f),
                      )
                    }
                  }

                  SettingsActionChip(
                    icon = Icons.Rounded.Settings,
                    label = "Open Android App Info",
                    accent = accent,
                    highlighted = false,
                    onClick = onOpenSelfAppInfo,
                    modifier = Modifier
                      .fillMaxWidth()
                      .focusRequester(rightPaneEntryRequester)
                      .focusProperties { left = activeTabRequester },
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SidebarCategoryItem(
  category: SettingsCategory,
  subtitle: String,
  selected: Boolean,
  accent: AccentPreset,
  focusRequester: FocusRequester,
  rightFocusRequester: FocusRequester,
  onSelect: () -> Unit,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      selected -> accent.primary.copy(alpha = 0.22f)
      else -> Color.Transparent
    },
    label = "sidebarCatBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White
  val subFg = if (isFocused) accent.onPrimary.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.58f)

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .focusRequester(focusRequester)
      .focusProperties { right = rightFocusRequester }
      .onFocusChanged { state ->
        isFocused = state.isFocused
        if (state.isFocused) onSelect()
      }
      .clickable(onClick = onSelect),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      selected -> BorderStroke(1.dp, accent.primary.copy(alpha = 0.65f))
      else -> null
    },
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = category.icon,
        contentDescription = category.title,
        tint = if (isFocused) accent.onPrimary else if (selected) accent.primary else Color.White.copy(alpha = 0.78f),
        modifier = Modifier.size(20.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = category.title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = if (selected || isFocused) FontWeight.SemiBold else FontWeight.Medium,
          ),
          color = fg,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelMedium,
          color = subFg,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun WallpaperDimmingRow(
  currentDim: Float,
  accent: AccentPreset,
  leftFocusRequester: FocusRequester,
  onSetDim: (Float) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    SettingsFieldLabel("Wallpaper Dimming")
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val presets = listOf("Light" to 0.25f, "Balanced" to 0.45f, "Dark" to 0.65f, "Cinema" to 0.80f)
      presets.forEachIndexed { i, (label, alpha) ->
        val active = kotlin.math.abs(currentDim - alpha) < 0.08f
        SelectablePill(
          label = label,
          selected = active,
          accent = accent,
          onClick = { onSetDim(alpha) },
          modifier = Modifier
            .weight(1f)
            .then(if (i == 0) Modifier.focusProperties { left = leftFocusRequester } else Modifier),
        )
      }
    }
  }
}

@Composable
private fun RowManageItem(
  row: AppRow,
  isFirst: Boolean,
  isLast: Boolean,
  canDelete: Boolean,
  accent: AccentPreset,
  firstFocusModifier: Modifier,
  onManageApps: () -> Unit,
  onOrderApps: () -> Unit,
  onRename: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color.White.copy(alpha = 0.06f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = row.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = buildString {
            append("${row.packageNames.size} app${if (row.packageNames.size == 1) "" else "s"}")
            if (isFirst) append(" • Hero dock")
          },
          style = MaterialTheme.typography.labelMedium,
          color = Color.White.copy(alpha = 0.60f),
        )
      }

      RowSmallActionButton(
        icon = Icons.Rounded.Add,
        contentDescription = "Add or Remove Apps in ${row.title}",
        accent = accent,
        onClick = onManageApps,
        modifier = firstFocusModifier,
      )
      if (row.packageNames.size >= 2) {
        RowSmallActionButton(
          icon = Icons.Rounded.SwapVert,
          contentDescription = "Tile Order in ${row.title}",
          accent = accent,
          onClick = onOrderApps,
        )
      }
      RowSmallActionButton(
        icon = Icons.Rounded.Edit,
        contentDescription = "Rename ${row.title}",
        accent = accent,
        onClick = onRename,
      )
      if (!isFirst) {
        RowSmallActionButton(
          icon = Icons.Rounded.ArrowUpward,
          contentDescription = "Move ${row.title} Up",
          accent = accent,
          onClick = onMoveUp,
        )
      }
      if (!isLast) {
        RowSmallActionButton(
          icon = Icons.Rounded.ArrowDownward,
          contentDescription = "Move ${row.title} Down",
          accent = accent,
          onClick = onMoveDown,
        )
      }
      if (canDelete) {
        RowSmallActionButton(
          icon = Icons.Rounded.DeleteOutline,
          contentDescription = "Delete ${row.title}",
          accent = accent,
          isDestructive = true,
          onClick = onDelete,
        )
      }
    }
  }
}

@Composable
private fun RowSmallActionButton(
  icon: ImageVector,
  contentDescription: String,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isDestructive: Boolean = false,
) {
  var isFocused by remember { mutableStateOf(false) }
  val focusBg = if (isDestructive) Color(0xFFEF4444) else accent.primary
  val focusFg = if (isDestructive) Color.White else accent.onPrimary

  val bg by animateColorAsState(
    targetValue = if (isFocused) focusBg else Color.White.copy(alpha = 0.10f),
    label = "rowSmallBtnBg",
  )
  val tint = when {
    isFocused -> focusFg
    isDestructive -> Color(0xFFFCA5A5)
    else -> Color.White
  }

  Surface(
    modifier = modifier
      .size(36.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = CircleShape,
    color = bg,
    border = if (isFocused) BorderStroke(2.dp, Color.White) else null,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun WeatherIconStyleChip(
  style: WeatherIconStyle,
  selected: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary.copy(alpha = 0.34f)
      selected -> accent.primary.copy(alpha = 0.22f)
      else -> Color.White.copy(alpha = 0.06f)
    },
    label = "weatherStyleBg",
  )

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      selected -> BorderStroke(1.5.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      WeatherConditionIcon(
        condition = WeatherCondition.PARTLY_CLOUDY,
        style = style,
        accent = accent,
        size = 20.dp,
      )
      Text(
        text = style.label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun SettingsFieldLabel(title: String) {
  Text(
    text = title.uppercase(),
    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
    color = Color.White.copy(alpha = 0.56f),
  )
}

@Composable
private fun ThemeSwatchCard(
  name: String,
  colors: List<Color>,
  isSelected: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .height(52.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = Color.Transparent,
    border = when {
      isFocused -> BorderStroke(2.5.dp, Color.White)
      isSelected -> BorderStroke(2.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    },
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.linearGradient(colors = colors))
        .background(Color.Black.copy(alpha = 0.30f))
        .padding(horizontal = 10.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = name.substringBefore(" "),
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        if (isSelected) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "Selected",
            tint = accent.primary,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun AccentSwatchChip(
  preset: AccentPreset,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = if (isFocused) preset.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      isSelected -> BorderStroke(2.dp, preset.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier = Modifier
          .size(14.dp)
          .clip(CircleShape)
          .background(preset.primary),
      )
      Text(
        text = preset.name.substringBefore(" "),
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun SettingsToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = if (isFocused) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
    label = "toggleRowBg",
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = if (isFocused) BorderStroke(2.dp, accent.primary) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.62f),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Surface(
        shape = RoundedCornerShape(50),
        color = if (checked) accent.primary else Color.White.copy(alpha = 0.15f),
      ) {
        Text(
          text = if (checked) "ON" else "OFF",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = if (checked) accent.onPrimary else Color.White.copy(alpha = 0.8f),
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
      }
    }
  }
}

@Composable
private fun SelectablePill(
  label: String,
  selected: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  showCheckmark: Boolean = false,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      selected -> accent.primary.copy(alpha = 0.28f)
      else -> Color.White.copy(alpha = 0.07f)
    },
    label = "selectPillBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      selected -> BorderStroke(1.5.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Row(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
    ) {
      if (showCheckmark && selected) {
        Icon(
          imageVector = Icons.Rounded.Check,
          contentDescription = null,
          tint = if (isFocused) accent.onPrimary else accent.primary,
          modifier = Modifier.size(14.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun SettingsActionChip(
  icon: ImageVector,
  label: String,
  accent: AccentPreset,
  highlighted: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      highlighted -> accent.primary.copy(alpha = 0.24f)
      else -> Color.White.copy(alpha = 0.08f)
    },
    label = "actionChipBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      highlighted -> BorderStroke(1.5.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = fg,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun HiddenAppRestoreRow(
  app: AppInfo,
  accent: AccentPreset,
  onRestore: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onRestore),
    shape = RoundedCornerShape(14.dp),
    color = if (isFocused) accent.primary else Color.White.copy(alpha = 0.06f),
    border = if (isFocused) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AppIconBadge(app = app, sizeDp = 34)
      Text(
        text = app.label,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color = if (isFocused) accent.onPrimary else Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Restore,
          contentDescription = "Restore",
          tint = if (isFocused) accent.onPrimary else accent.primary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "Restore",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = if (isFocused) accent.onPrimary else accent.primary,
        )
      }
    }
  }
}
