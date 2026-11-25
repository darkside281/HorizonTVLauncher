package com.horizontv.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.horizontv.launcher.model.JustWatchRegion
import com.horizontv.launcher.model.LauncherSettings
import com.horizontv.launcher.model.MediaWallpaperProvider
import com.horizontv.launcher.ui.theme.LauncherPalettes

@Composable
fun SetupWizardDialog(
  settings: LauncherSettings,
  allApps: List<AppInfo>,
  isDefaultHome: Boolean,
  accent: AccentPreset,
  onSetMinimalistDock: (Boolean) -> Unit,
  onSet24HourClock: (Boolean) -> Unit,
  onOpenHomeSettings: () -> Unit,
  onSelectTheme: (String) -> Unit,
  onEnableRedditWallpapers: () -> Unit,
  onSetJustWatchRegion: (JustWatchRegion) -> Unit,
  onToggleFavoriteApp: (String) -> Unit,
  onFinish: () -> Unit,
) {
  var step by remember { mutableIntStateOf(0) } // 0 = Basics, 1 = Wallpaper, 2 = Region, 3 = Apps
  val primaryFocusRequester = remember { FocusRequester() }
  val bodyScroll = rememberScrollState()

  val stepTitles = listOf(
    "Welcome & Basics",
    "Wallpaper",
    "Region & Search",
    "Favorite Apps",
  )

  LaunchedEffect(step) {
    bodyScroll.scrollTo(0)
    runCatching { primaryFocusRequester.requestFocus() }
  }

  Dialog(
    onDismissRequest = {
      if (step > 0) step -= 1 else onFinish()
    },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = false,
    ),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.78f))
        .padding(horizontal = 42.dp, vertical = 26.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier
          .width(960.dp)
          .height(564.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xF60C1322),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.16f)),
        shadowElevation = 28.dp,
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // 1. Top 4-Step Progress Stepper
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            stepTitles.forEachIndexed { idx, label ->
              val isDone = idx < step
              val isCurrent = idx == step
              Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                color = when {
                  isCurrent -> accent.primary.copy(alpha = 0.22f)
                  isDone -> Color(0xFF10B981).copy(alpha = 0.16f)
                  else -> Color.White.copy(alpha = 0.05f)
                },
                border = BorderStroke(
                  1.dp,
                  when {
                    isCurrent -> accent.primary
                    isDone -> Color(0xFF10B981).copy(alpha = 0.55f)
                    else -> Color.White.copy(alpha = 0.10f)
                  },
                ),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Box(
                    modifier = Modifier
                      .size(20.dp)
                      .background(
                        color = when {
                          isCurrent -> accent.primary
                          isDone -> Color(0xFF10B981)
                          else -> Color.White.copy(alpha = 0.14f)
                        },
                        shape = CircleShape,
                      ),
                    contentAlignment = Alignment.Center,
                  ) {
                    if (isDone) {
                      Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color(0xFF04121F),
                        modifier = Modifier.size(13.dp),
                      )
                    } else {
                      Text(
                        text = "${idx + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isCurrent) accent.onPrimary else Color.White.copy(alpha = 0.72f),
                      )
                    }
                  }
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = when {
                      isCurrent -> Color.White
                      isDone -> Color.White.copy(alpha = 0.88f)
                      else -> Color.White.copy(alpha = 0.54f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 2. Active Step Content
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(bodyScroll),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            when (step) {
              // STEP 1: Welcome & Basics
              0 -> {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accent.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.size(52.dp),
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
                        contentDescription = "Horizon TV Launcher",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                      )
                    }
                  }
                  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                      text = "Welcome to Horizon TV Launcher",
                      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                      color = Color.White,
                    )
                    Text(
                      text = "Let's set up a few essentials in under a minute. Everything can be changed later in Customize.",
                      style = MaterialTheme.typography.bodySmall,
                      color = Color.White.copy(alpha = 0.68f),
                    )
                  }
                }

                WizardSectionLabel("HOME DOCK STYLE", accent)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  WizardOptionCard(
                    title = "Standard Home Dock",
                    subtitle = "Classic layout with row headers, app titles, and crisp full-opacity tiles.",
                    selected = !settings.minimalistDockMode,
                    accent = accent,
                    onClick = { onSetMinimalistDock(false) },
                    modifier = Modifier
                      .weight(1f)
                      .focusRequester(primaryFocusRequester),
                  )
                  WizardOptionCard(
                    title = "Minimalist Bottom-Dock",
                    subtitle = "Low-profile dock pushed lower on screen so wallpapers & ClearLogos shine.",
                    selected = settings.minimalistDockMode,
                    accent = accent,
                    onClick = { onSetMinimalistDock(true) },
                    modifier = Modifier.weight(1f),
                  )
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    WizardSectionLabel("CLOCK FORMAT", accent)
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      WizardPillButton(
                        label = "12-Hour (9:45 PM)",
                        selected = !settings.use24HourClock,
                        accent = accent,
                        onClick = { onSet24HourClock(false) },
                        modifier = Modifier.weight(1f),
                      )
                      WizardPillButton(
                        label = "24-Hour (21:45)",
                        selected = settings.use24HourClock,
                        accent = accent,
                        onClick = { onSet24HourClock(true) },
                        modifier = Modifier.weight(1f),
                      )
                    }
                  }

                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    WizardSectionLabel("DEFAULT HOME LAUNCHER", accent)
                    WizardPillButton(
                      label = if (isDefaultHome) "Default Launcher Active ✓" else "Set Horizon as Default Home…",
                      selected = isDefaultHome,
                      accent = accent,
                      icon = if (isDefaultHome) Icons.Rounded.Check else Icons.Rounded.Home,
                      onClick = onOpenHomeSettings,
                      modifier = Modifier.fillMaxWidth(),
                    )
                  }
                }
              }

              // STEP 2: Wallpaper (Standard Ambient Themes OR Live Reddit Wallpapers)
              1 -> {
                val isRedditActive = settings.jellyfinEnabled &&
                  settings.activeWallpaperProvider == MediaWallpaperProvider.REDDIT

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "Choose Your Wallpaper",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                  )
                  Text(
                    text = "Start with a built-in ambient theme or turn on live 4K widescreen photography from Reddit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f),
                  )
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                  verticalAlignment = Alignment.Top,
                ) {
                  // Left: Standard Ambient Themes (3x2)
                  Column(
                    modifier = Modifier.weight(1.35f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    WizardSectionLabel("STANDARD AMBIENT THEMES", accent)
                    LauncherPalettes.themes.chunked(3).forEachIndexed { rowIdx, rowThemes ->
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        rowThemes.forEachIndexed { colIdx, theme ->
                          val isSelected = !settings.jellyfinEnabled &&
                            settings.customWallpaperUri == null &&
                            settings.themeId == theme.id
                          WizardThemeSwatch(
                            name = theme.name,
                            colors = listOf(theme.baseColor, theme.topGlow, theme.bottomGlow),
                            selected = isSelected,
                            accent = accent,
                            onClick = { onSelectTheme(theme.id) },
                            modifier = Modifier
                              .weight(1f)
                              .then(
                                if (rowIdx == 0 && colIdx == 0) Modifier.focusRequester(primaryFocusRequester) else Modifier,
                              ),
                          )
                        }
                      }
                    }
                  }

                  // Right: Live Reddit Wallpapers
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    WizardSectionLabel("OR LIVE PHOTOGRAPHY", accent)
                    WizardRedditCard(
                      selected = isRedditActive,
                      accent = accent,
                      onClick = onEnableRedditWallpapers,
                      modifier = Modifier.fillMaxWidth(),
                    )
                  }
                }

                // Info banner about advanced providers in Customize
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = accent.primary.copy(alpha = 0.10f),
                  border = BorderStroke(1.dp, accent.primary.copy(alpha = 0.30f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Info,
                      contentDescription = null,
                      tint = accent.primary,
                      modifier = Modifier.size(20.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(
                        text = "Want movie & TV backdrops from Jellyfin, Plex, Emby, TMDB, or your own photos?",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                      )
                      Text(
                        text = "Advanced wallpaper providers, slideshow speed, ClearLogos & dimming can be configured anytime in Customize → Wallpapers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                      )
                    }
                  }
                }
              }

              // STEP 3: Region & Search
              2 -> {
                val activeRegion = settings.activeJustWatchRegion
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "Region & Search",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                  )
                  Text(
                    text = "Choose your country so Universal Search shows local streaming availability and age ratings (${activeRegion.ratingExample}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f),
                  )
                }

                WizardSectionLabel("COUNTRY / REGION (${activeRegion.code})", accent)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  JustWatchRegion.entries.chunked(2).forEachIndexed { rowIdx, pair ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      pair.forEachIndexed { colIdx, region ->
                        WizardPillButton(
                          label = "${region.label} (${region.code}) — ${region.ratingExample}",
                          selected = activeRegion == region,
                          accent = accent,
                          onClick = { onSetJustWatchRegion(region) },
                          modifier = Modifier
                            .weight(1f)
                            .then(
                              if (rowIdx == 0 && colIdx == 0) Modifier.focusRequester(primaryFocusRequester) else Modifier,
                            ),
                        )
                      }
                      if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }

                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = Color.White.copy(alpha = 0.05f),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = "Tip: You can filter Search to your exact streaming subscriptions & Prime Video Channels, toggle Rotten Tomatoes scores, and configure Weather or Parental Controls anytime under Customize.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  )
                }
              }

              // STEP 4: Favorite Apps
              else -> {
                val favSet = settings.primaryRow.packageNames.toSet()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "Pick Your Favorite Apps (${favSet.size} selected)",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                  )
                  Text(
                    text = "Choose the apps you want on your main Home row. You can add extra rows or reorder tiles anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f),
                  )
                }

                val tvAppsFirst = remember(allApps) {
                  allApps.sortedWith(compareByDescending<AppInfo> { it.isTvApp }.thenBy { it.label.lowercase() })
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  tvAppsFirst.chunked(3).forEachIndexed { rowIdx, rowApps ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      rowApps.forEachIndexed { colIdx, app ->
                        val isSelected = app.packageName in favSet
                        WizardAppToggleChip(
                          app = app,
                          selected = isSelected,
                          accent = accent,
                          onClick = { onToggleFavoriteApp(app.packageName) },
                          modifier = Modifier
                            .weight(1f)
                            .then(
                              if (rowIdx == 0 && colIdx == 0) Modifier.focusRequester(primaryFocusRequester) else Modifier,
                            ),
                        )
                      }
                      repeat(3 - rowApps.size) {
                        Spacer(modifier = Modifier.weight(1f))
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // 3. Footer Navigation Bar
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(Color.White.copy(alpha = 0.10f)),
          )
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            if (step == 0) {
              WizardFooterButton(
                label = "Skip Setup",
                accent = accent,
                isPrimary = false,
                onClick = onFinish,
              )
            } else {
              WizardFooterButton(
                label = "Back",
                accent = accent,
                isPrimary = false,
                leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = { step -= 1 },
              )
            }

            Text(
              text = "Step ${step + 1} of 4  •  Use D-Pad & OK",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.46f),
            )

            val nextLabel = when (step) {
              0 -> "Next: Wallpaper"
              1 -> "Next: Region & Search"
              2 -> "Next: Favorite Apps"
              else -> "Finish & Open Home"
            }
            WizardFooterButton(
              label = nextLabel,
              accent = accent,
              isPrimary = true,
              trailingIcon = if (step < 3) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Check,
              onClick = {
                if (step < 3) step += 1 else onFinish()
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WizardSectionLabel(text: String, accent: AccentPreset) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
    color = accent.primary,
  )
}

@Composable
private fun WizardOptionCard(
  title: String,
  subtitle: String,
  selected: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary.copy(alpha = 0.24f)
      selected -> accent.primary.copy(alpha = 0.14f)
      else -> Color.White.copy(alpha = 0.05f)
    },
    label = "wizOptBg",
  )

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      selected -> BorderStroke(1.5.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
        )
        if (selected) {
          Box(
            modifier = Modifier
              .size(22.dp)
              .background(accent.primary, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = null,
              tint = accent.onPrimary,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.72f),
      )
    }
  }
}

@Composable
private fun WizardPillButton(
  label: String,
  selected: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      selected -> accent.primary.copy(alpha = 0.24f)
      else -> Color.White.copy(alpha = 0.06f)
    },
    label = "wizPillBg",
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
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    },
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = fg,
          modifier = Modifier.size(16.dp),
        )
      } else if (selected) {
        Icon(
          imageVector = Icons.Rounded.Check,
          contentDescription = null,
          tint = if (isFocused) accent.onPrimary else accent.primary,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun WizardThemeSwatch(
  name: String,
  colors: List<Color>,
  selected: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .height(78.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    border = when {
      isFocused -> BorderStroke(2.5.dp, Color.White)
      selected -> BorderStroke(2.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    },
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.linearGradient(colors)),
    ) {
      if (selected) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .size(20.dp)
            .background(accent.primary, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = accent.onPrimary,
            modifier = Modifier.size(13.dp),
          )
        }
      }
      Text(
        text = name,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(horizontal = 10.dp, vertical = 8.dp),
      )
    }
  }
}

@Composable
private fun WizardRedditCard(
  selected: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary.copy(alpha = 0.24f)
      selected -> accent.primary.copy(alpha = 0.16f)
      else -> Color.White.copy(alpha = 0.06f)
    },
    label = "wizRedditBg",
  )

  Surface(
    modifier = modifier
      .height(164.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.5.dp, Color.White)
      selected -> BorderStroke(2.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    },
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(
            Brush.linearGradient(
              listOf(Color(0xFFF97316), Color(0xFF7C3AED), Color(0xFF0F172A)),
            ),
          )
          .padding(horizontal = 12.dp, vertical = 8.dp),
      ) {
        Row(
          modifier = Modifier.align(Alignment.CenterStart),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Wallpaper,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = "Nature, Space & 4K Art",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
        }
        if (selected) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .size(22.dp)
              .background(accent.primary, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = null,
              tint = accent.onPrimary,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = if (selected) "Live Reddit Wallpapers (Active ✓)" else "Live Reddit Wallpapers",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
        )
        Text(
          text = "Auto-rotating 4K widescreen photography & art — works instantly with no account or API key.",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.72f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun WizardAppToggleChip(
  app: AppInfo,
  selected: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      selected -> accent.primary.copy(alpha = 0.22f)
      else -> Color.White.copy(alpha = 0.06f)
    },
    label = "wizAppBg",
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
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      AppIconBadge(app = app, sizeDp = 30)
      Text(
        text = app.label,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      if (selected) {
        Icon(
          imageVector = Icons.Rounded.Check,
          contentDescription = "Selected",
          tint = if (isFocused) accent.onPrimary else accent.primary,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

@Composable
private fun WizardFooterButton(
  label: String,
  accent: AccentPreset,
  isPrimary: Boolean,
  onClick: () -> Unit,
  leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> if (isPrimary) Color.White else accent.primary
      isPrimary -> accent.primary
      else -> Color.White.copy(alpha = 0.08f)
    },
    label = "wizFooterBg",
  )
  val fg = when {
    isFocused && isPrimary -> Color(0xFF04121F)
    isFocused -> accent.onPrimary
    isPrimary -> accent.onPrimary
    else -> Color.White.copy(alpha = 0.88f)
  }

  Surface(
    modifier = Modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(50),
    color = bg,
    border = if (isFocused) {
      BorderStroke(2.dp, if (isPrimary) accent.primary else Color.White)
    } else {
      BorderStroke(1.dp, if (isPrimary) accent.primary else Color.White.copy(alpha = 0.14f))
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (leadingIcon != null) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint = fg,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = fg,
      )
      if (trailingIcon != null) {
        Icon(
          imageVector = trailingIcon,
          contentDescription = null,
          tint = fg,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}
