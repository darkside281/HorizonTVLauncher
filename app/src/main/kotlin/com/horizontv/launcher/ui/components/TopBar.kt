package com.horizontv.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsInputHdmi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.WeatherData
import com.horizontv.launcher.model.WeatherForecastSize
import com.horizontv.launcher.model.WeatherIconStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun LauncherTopBar(
  accent: AccentPreset,
  showClock: Boolean,
  showDate: Boolean,
  use24HourClock: Boolean,
  showSearchButton: Boolean,
  isSearchActive: Boolean,
  showTvInputsButton: Boolean = false,
  weatherData: WeatherData?,
  weatherEnabled: Boolean,
  weatherDays: Int,
  weatherShowForecast: Boolean,
  weatherForecastSize: WeatherForecastSize,
  weatherIconStyle: WeatherIconStyle,
  weatherShowBackdrop: Boolean,
  onToggleSearch: () -> Unit,
  onOpenTvInputs: () -> Unit = {},
  onOpenCustomize: () -> Unit,
  onOpenSystemSettings: () -> Unit,
  modifier: Modifier = Modifier,
  topBarFocusRequester: FocusRequester = remember { FocusRequester() },
  downFocusRequester: FocusRequester? = null,
  onTopBarFocused: () -> Unit = {},
) {
  var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

  LaunchedEffect(Unit) {
    while (true) {
      currentTimeMillis = System.currentTimeMillis()
      val millisIntoMinute = currentTimeMillis % 60_000L
      delay((60_000L - millisIntoMinute).coerceAtLeast(1_000L))
    }
  }

  val now = remember(currentTimeMillis) { Date(currentTimeMillis) }
  val timePattern = if (use24HourClock) "HH:mm" else "h:mm"
  val amPmPattern = "a"
  val timeText = remember(currentTimeMillis, use24HourClock) {
    SimpleDateFormat(timePattern, Locale.getDefault()).format(now)
  }
  val amPmText = remember(currentTimeMillis, use24HourClock) {
    if (use24HourClock) "" else SimpleDateFormat(amPmPattern, Locale.getDefault()).format(now)
  }
  val dateText = remember(currentTimeMillis) {
    SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now)
  }

  val heroShadow = remember {
    Shadow(
      color = Color.Black.copy(alpha = 0.55f),
      offset = Offset(0f, 3f),
      blurRadius = 14f,
    )
  }

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Left: Minimal Projectivy-style airy large hero clock, date & weather
    Column(
      modifier = Modifier.weight(1f, fill = false),
    ) {
      if (showClock) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge.copy(
              fontSize = 58.sp,
              lineHeight = 62.sp,
              fontWeight = FontWeight.Light,
              letterSpacing = (-1.5).sp,
              fontFeatureSettings = "tnum",
              shadow = heroShadow,
            ),
            color = Color.White,
            maxLines = 1,
          )
          if (amPmText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = amPmText,
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Normal,
                shadow = heroShadow,
              ),
              color = Color.White.copy(alpha = 0.78f),
              modifier = Modifier.padding(bottom = 10.dp),
              maxLines = 1,
            )
          }
        }
      }

      if (showDate) {
        if (showClock) Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = dateText,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
            shadow = heroShadow,
          ),
          color = Color.White.copy(alpha = 0.86f),
          maxLines = 1,
        )
      }

      if (weatherEnabled && weatherData != null) {
        if (showClock || showDate) Spacer(modifier = Modifier.height(10.dp))
        WeatherWidget(
          weather = weatherData,
          daysToShow = weatherDays,
          showForecastDays = weatherShowForecast,
          forecastSize = weatherForecastSize,
          iconStyle = weatherIconStyle,
          showBackdrop = weatherShowBackdrop,
          accent = accent,
        )
      }
    }

    Spacer(modifier = Modifier.width(20.dp))

    // Top-Right: Compact translucent dark-glass quick controls
    Row(
      modifier = Modifier.padding(top = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (showSearchButton) {
        QuickActionPill(
          icon = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
          label = if (isSearchActive) "Close" else "Search",
          accent = accent,
          highlighted = isSearchActive,
          onClick = onToggleSearch,
          onFocused = onTopBarFocused,
          modifier = Modifier
            .focusRequester(topBarFocusRequester)
            .focusProperties {
              up = FocusRequester.Cancel
              left = FocusRequester.Cancel
              if (downFocusRequester != null) down = downFocusRequester
            },
        )
      }
      if (showTvInputsButton) {
        QuickActionIcon(
          icon = Icons.Rounded.SettingsInputHdmi,
          contentDescription = "TV Inputs (HDMI)",
          accent = accent,
          onClick = onOpenTvInputs,
          onFocused = onTopBarFocused,
          modifier = Modifier
            .then(if (!showSearchButton) Modifier.focusRequester(topBarFocusRequester) else Modifier)
            .focusProperties {
              up = FocusRequester.Cancel
              if (!showSearchButton) left = FocusRequester.Cancel
              if (downFocusRequester != null) down = downFocusRequester
            },
        )
      }
      QuickActionIcon(
        icon = Icons.Rounded.Palette,
        contentDescription = "Customize Horizon",
        accent = accent,
        onClick = onOpenCustomize,
        onFocused = onTopBarFocused,
        modifier = Modifier
          .then(if (!showSearchButton && !showTvInputsButton) Modifier.focusRequester(topBarFocusRequester) else Modifier)
          .focusProperties {
            up = FocusRequester.Cancel
            if (!showSearchButton && !showTvInputsButton) left = FocusRequester.Cancel
            if (downFocusRequester != null) down = downFocusRequester
          },
      )
      QuickActionIcon(
        icon = Icons.Rounded.Settings,
        contentDescription = "Android System Settings",
        accent = accent,
        onClick = onOpenSystemSettings,
        onFocused = onTopBarFocused,
        modifier = Modifier.focusProperties {
          up = FocusRequester.Cancel
          right = FocusRequester.Cancel
          if (downFocusRequester != null) down = downFocusRequester
        },
      )
    }
  }
}

@Composable
fun QuickActionPill(
  icon: ImageVector,
  label: String,
  accent: AccentPreset,
  highlighted: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onFocused: () -> Unit = {},
) {
  var isFocused by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(
    targetValue = if (isFocused) 1.07f else 1f,
    animationSpec = spring(stiffness = 450f),
    label = "pillScale",
  )
  val bgColor by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      highlighted -> accent.primary.copy(alpha = 0.30f)
      else -> Color.Black.copy(alpha = 0.34f)
    },
    label = "pillBg",
  )
  val contentColor = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .scale(scale)
      .clip(RoundedCornerShape(50))
      .onFocusChanged {
        isFocused = it.isFocused
        if (it.isFocused) onFocused()
      }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(50),
    color = bgColor,
    border = if (isFocused) {
      BorderStroke(2.dp, Color.White.copy(alpha = 0.92f))
    } else if (highlighted) {
      BorderStroke(1.dp, accent.primary.copy(alpha = 0.75f))
    } else {
      BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = contentColor,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun QuickActionIcon(
  icon: ImageVector,
  contentDescription: String,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onFocused: () -> Unit = {},
) {
  var isFocused by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(
    targetValue = if (isFocused) 1.12f else 1f,
    animationSpec = spring(stiffness = 450f),
    label = "iconScale",
  )
  val bgColor by animateColorAsState(
    targetValue = if (isFocused) accent.primary else Color.Black.copy(alpha = 0.34f),
    label = "iconBg",
  )
  val tint = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .size(40.dp)
      .scale(scale)
      .clip(CircleShape)
      .onFocusChanged {
        isFocused = it.isFocused
        if (it.isFocused) onFocused()
      }
      .clickable(onClick = onClick),
    shape = CircleShape,
    color = bgColor,
    border = if (isFocused) {
      BorderStroke(2.dp, Color.White.copy(alpha = 0.92f))
    } else {
      BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    },
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
