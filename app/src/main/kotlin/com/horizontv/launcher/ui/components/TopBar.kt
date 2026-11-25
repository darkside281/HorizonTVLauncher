package com.horizontv.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.ClockFontStyle
import com.horizontv.launcher.model.ClockFontWeight
import com.horizontv.launcher.model.ClockStyle
import com.horizontv.launcher.model.WeatherData
import com.horizontv.launcher.model.WeatherForecastSize
import com.horizontv.launcher.model.WeatherIconStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun LauncherTopBar(
  accent: AccentPreset,
  showClock: Boolean,
  showDate: Boolean,
  use24HourClock: Boolean,
  clockStyle: ClockStyle = ClockStyle.CLASSIC,
  clockFontStyle: ClockFontStyle = ClockFontStyle.SANS,
  clockFontWeight: ClockFontWeight = ClockFontWeight.LIGHT,
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

  val fontFamily = remember(clockFontStyle) {
    when (clockFontStyle) {
      ClockFontStyle.SANS -> FontFamily.SansSerif
      ClockFontStyle.SERIF -> FontFamily.Serif
      ClockFontStyle.MONO -> FontFamily.Monospace
    }
  }
  val fontWeight = remember(clockFontWeight) {
    when (clockFontWeight) {
      ClockFontWeight.LIGHT -> FontWeight.Light
      ClockFontWeight.REGULAR -> FontWeight.Normal
      ClockFontWeight.BOLD -> FontWeight.Bold
    }
  }

  val heroShadow = remember {
    Shadow(
      color = Color.Black.copy(alpha = 0.55f),
      offset = Offset(0f, 3f),
      blurRadius = 14f,
    )
  }

  val controlsRow: @Composable () -> Unit = {
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

  if (clockStyle == ClockStyle.CENTERED) {
    Box(modifier = modifier.fillMaxWidth()) {
      // Top-right quick controls stay pinned cleanly
      Box(modifier = Modifier.align(Alignment.TopEnd)) {
        controlsRow()
      }
      // Large centered editorial cinema clock, date & weather
      Column(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (showClock) {
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = timeText,
              style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 74.sp,
                lineHeight = 78.sp,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
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
                  fontFamily = fontFamily,
                  fontWeight = FontWeight.Normal,
                  shadow = heroShadow,
                ),
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.padding(bottom = 13.dp),
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
              fontFamily = fontFamily,
              fontWeight = FontWeight.Normal,
              letterSpacing = 0.4.sp,
              shadow = heroShadow,
            ),
            color = Color.White.copy(alpha = 0.88f),
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
    }
    return
  }

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier.weight(1f, fill = false),
    ) {
      when (clockStyle) {
        ClockStyle.MINIMAL -> {
          if (showClock || showDate) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (showClock) {
                Row(verticalAlignment = Alignment.Bottom) {
                  Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                      fontSize = 30.sp,
                      lineHeight = 34.sp,
                      fontFamily = fontFamily,
                      fontWeight = fontWeight,
                      fontFeatureSettings = "tnum",
                      shadow = heroShadow,
                    ),
                    color = Color.White,
                    maxLines = 1,
                  )
                  if (amPmText.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = amPmText,
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = fontFamily,
                        shadow = heroShadow,
                      ),
                      color = Color.White.copy(alpha = 0.78f),
                      modifier = Modifier.padding(bottom = 4.dp),
                      maxLines = 1,
                    )
                  }
                }
              }
              if (showClock && showDate) {
                Text(
                  text = "•",
                  style = MaterialTheme.typography.titleMedium.copy(shadow = heroShadow),
                  color = accent.primary.copy(alpha = 0.90f),
                )
              }
              if (showDate) {
                Text(
                  text = dateText,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Normal,
                    shadow = heroShadow,
                  ),
                  color = Color.White.copy(alpha = 0.86f),
                  maxLines = 1,
                )
              }
            }
          }
        }

        ClockStyle.ANALOG -> {
          if (showClock || showDate) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              if (showClock) {
                AnalogClockDial(
                  timeMillis = currentTimeMillis,
                  accent = accent,
                  modifier = Modifier.size(86.dp),
                )
              }
              Column {
                if (showClock) {
                  Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                      text = timeText,
                      style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontFamily = fontFamily,
                        fontWeight = fontWeight,
                        fontFeatureSettings = "tnum",
                        shadow = heroShadow,
                      ),
                      color = Color.White,
                      maxLines = 1,
                    )
                    if (amPmText.isNotEmpty()) {
                      Spacer(modifier = Modifier.width(5.dp))
                      Text(
                        text = amPmText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                          fontFamily = fontFamily,
                          shadow = heroShadow,
                        ),
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 3.dp),
                      )
                    }
                  }
                }
                if (showDate) {
                  Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontFamily = fontFamily,
                      fontWeight = FontWeight.Normal,
                      shadow = heroShadow,
                    ),
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }

        else -> {
          // CLASSIC
          if (showClock) {
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge.copy(
                  fontSize = 58.sp,
                  lineHeight = 62.sp,
                  fontFamily = fontFamily,
                  fontWeight = fontWeight,
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
                    fontFamily = fontFamily,
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
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.3.sp,
                shadow = heroShadow,
              ),
              color = Color.White.copy(alpha = 0.86f),
              maxLines = 1,
            )
          }
        }
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
    controlsRow()
  }
}

@Composable
private fun AnalogClockDial(
  timeMillis: Long,
  accent: AccentPreset,
  modifier: Modifier = Modifier,
) {
  val cal = remember(timeMillis) {
    Calendar.getInstance().apply { this.timeInMillis = timeMillis }
  }
  val hours = cal.get(Calendar.HOUR)
  val minutes = cal.get(Calendar.MINUTE)
  val hourAngle = ((hours + minutes / 60f) / 12f) * 2f * PI.toFloat() - (PI.toFloat() / 2f)
  val minuteAngle = (minutes / 60f) * 2f * PI.toFloat() - (PI.toFloat() / 2f)

  Canvas(modifier = modifier) {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    // Translucent dial face
    drawCircle(
      color = Color.Black.copy(alpha = 0.38f),
      radius = radius,
      center = center,
    )
    drawCircle(
      color = Color.White.copy(alpha = 0.28f),
      radius = radius - 1.5.dp.toPx(),
      center = center,
      style = Stroke(width = 1.8.dp.toPx()),
    )

    // 12 hour markers
    for (i in 0 until 12) {
      val a = (i / 12f) * 2f * PI.toFloat() - (PI.toFloat() / 2f)
      val isMajor = i % 3 == 0
      val inner = radius * (if (isMajor) 0.75f else 0.83f)
      val outer = radius * 0.90f
      drawLine(
        color = Color.White.copy(alpha = if (isMajor) 0.88f else 0.42f),
        start = Offset(center.x + cos(a) * inner, center.y + sin(a) * inner),
        end = Offset(center.x + cos(a) * outer, center.y + sin(a) * outer),
        strokeWidth = (if (isMajor) 2.2.dp else 1.2.dp).toPx(),
        cap = StrokeCap.Round,
      )
    }

    // Hour hand
    drawLine(
      color = Color.White,
      start = center,
      end = Offset(
        center.x + cos(hourAngle) * (radius * 0.48f),
        center.y + sin(hourAngle) * (radius * 0.48f),
      ),
      strokeWidth = 3.2.dp.toPx(),
      cap = StrokeCap.Round,
    )

    // Minute hand
    drawLine(
      color = Color.White.copy(alpha = 0.92f),
      start = center,
      end = Offset(
        center.x + cos(minuteAngle) * (radius * 0.68f),
        center.y + sin(minuteAngle) * (radius * 0.68f),
      ),
      strokeWidth = 2.2.dp.toPx(),
      cap = StrokeCap.Round,
    )

    // Accent center hub
    drawCircle(
      color = accent.primary,
      radius = 3.5.dp.toPx(),
      center = center,
    )
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
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = contentColor,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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

  Surface(
    modifier = modifier
      .size(42.dp)
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
        tint = if (isFocused) accent.onPrimary else Color.White,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
