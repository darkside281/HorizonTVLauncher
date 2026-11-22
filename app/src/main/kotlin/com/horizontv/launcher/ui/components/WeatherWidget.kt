package com.horizontv.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FilterDrama
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.WeatherCondition
import com.horizontv.launcher.model.WeatherData
import com.horizontv.launcher.model.WeatherForecastSize
import com.horizontv.launcher.model.WeatherIconStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeatherWidget(
  weather: WeatherData,
  daysToShow: Int,
  showForecastDays: Boolean,
  forecastSize: WeatherForecastSize,
  iconStyle: WeatherIconStyle,
  showBackdrop: Boolean,
  accent: AccentPreset,
  modifier: Modifier = Modifier,
) {
  val textShadow = remember {
    Shadow(
      color = Color.Black.copy(alpha = 0.65f),
      offset = Offset(0f, 2f),
      blurRadius = 10f,
    )
  }
  val clampedDays = daysToShow.coerceIn(1, 7)
  val forecastDays = weather.daily.take(clampedDays)
  val today = forecastDays.firstOrNull()
  val scale = forecastSize.scale

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Current Weather Summary Line right under the date
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      WeatherConditionIcon(
        condition = weather.condition,
        style = iconStyle,
        accent = accent,
        size = 24.dp,
      )
      Text(
        text = "${weather.currentTemp}${weather.unitSymbol}",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.SemiBold,
          fontFeatureSettings = "tnum",
          shadow = textShadow,
        ),
        color = Color.White,
      )
      Text(
        text = buildString {
          append(weather.condition.label)
          if ((!showForecastDays || clampedDays == 1) && today != null) {
            append("  •  H:${today.tempMax}° L:${today.tempMin}°")
          }
          if (weather.locationName.isNotBlank()) {
            append("  •  ${weather.locationName}")
          }
        },
        style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
        color = Color.White.copy(alpha = 0.84f),
        maxLines = 1,
      )
    }

    // Multi-Day Forecast Strip (shown when enabled and 2 to 7 days selected)
    if (showForecastDays && clampedDays >= 2 && forecastDays.isNotEmpty()) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (showBackdrop) Color.Black.copy(alpha = 0.36f) else Color.Transparent,
        border = if (showBackdrop) BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)) else null,
      ) {
        Row(
          modifier = Modifier.padding(
            horizontal = if (showBackdrop) (12f * scale).dp else 0.dp,
            vertical = if (showBackdrop) (8f * scale).dp else 4.dp,
          ),
          horizontalArrangement = Arrangement.spacedBy((14f * scale).dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          forecastDays.forEach { day ->
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy((3f * scale).dp),
              modifier = Modifier.width((46f * scale).dp),
            ) {
              Text(
                text = day.dayLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontSize = (11f * scale).sp,
                  fontWeight = FontWeight.Medium,
                  shadow = if (showBackdrop) null else textShadow,
                ),
                color = Color.White.copy(alpha = if (showBackdrop) 0.74f else 0.88f),
                maxLines = 1,
              )
              WeatherConditionIcon(
                condition = day.condition,
                style = iconStyle,
                accent = accent,
                size = (20f * scale).dp,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((3f * scale).dp),
              ) {
                Text(
                  text = "${day.tempMax}°",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = (12f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                    shadow = if (showBackdrop) null else textShadow,
                  ),
                  color = Color.White,
                )
                Text(
                  text = "${day.tempMin}°",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = (11f * scale).sp,
                    fontFeatureSettings = "tnum",
                    shadow = if (showBackdrop) null else textShadow,
                  ),
                  color = Color.White.copy(alpha = if (showBackdrop) 0.56f else 0.74f),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun WeatherConditionIcon(
  condition: WeatherCondition,
  style: WeatherIconStyle,
  accent: AccentPreset,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  when (style) {
    WeatherIconStyle.EMOJI -> {
      val glyph = when (condition) {
        WeatherCondition.CLEAR -> "☀️"
        WeatherCondition.PARTLY_CLOUDY -> "⛅"
        WeatherCondition.CLOUDY -> "☁️"
        WeatherCondition.FOG -> "🌫️"
        WeatherCondition.DRIZZLE -> "🌦️"
        WeatherCondition.RAIN -> "🌧️"
        WeatherCondition.SNOW -> "❄️"
        WeatherCondition.STORM -> "⛈️"
      }
      Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = glyph,
          fontSize = (size.value * 0.78f).sp,
        )
      }
    }

    WeatherIconStyle.MINIMAL_LINE,
    WeatherIconStyle.ACCENT_TINT,
    -> {
      val tint = if (style == WeatherIconStyle.ACCENT_TINT) accent.primary else Color.White
      val vector = when (condition) {
        WeatherCondition.CLEAR -> Icons.Rounded.WbSunny
        WeatherCondition.PARTLY_CLOUDY -> Icons.Rounded.FilterDrama
        WeatherCondition.CLOUDY -> Icons.Rounded.Cloud
        WeatherCondition.FOG -> Icons.Rounded.Cloud
        WeatherCondition.DRIZZLE -> Icons.Rounded.Grain
        WeatherCondition.RAIN -> Icons.Rounded.WaterDrop
        WeatherCondition.SNOW -> Icons.Rounded.AcUnit
        WeatherCondition.STORM -> Icons.Rounded.Thunderstorm
      }
      Icon(
        imageVector = vector,
        contentDescription = condition.label,
        tint = tint,
        modifier = modifier.size(size),
      )
    }

    WeatherIconStyle.COLOR_VIVID -> {
      VividCanvasWeatherIcon(
        condition = condition,
        modifier = modifier.size(size),
      )
    }
  }
}

@Composable
private fun VividCanvasWeatherIcon(
  condition: WeatherCondition,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    fun drawSun(cx: Float, cy: Float, r: Float) {
      val sunGold = Color(0xFFFBBF24)
      val sunGlow = Color(0xFFFDE68A)
      drawCircle(color = sunGold.copy(alpha = 0.28f), radius = r * 1.45f, center = Offset(cx, cy))
      drawCircle(color = sunGold, radius = r, center = Offset(cx, cy))
      drawCircle(color = sunGlow, radius = r * 0.55f, center = Offset(cx - r * 0.15f, cy - r * 0.15f))
      for (i in 0 until 8) {
        val angle = (i * 45.0) * PI / 180.0
        val start = Offset(
          cx + (r * 1.25f * cos(angle)).toFloat(),
          cy + (r * 1.25f * sin(angle)).toFloat(),
        )
        val end = Offset(
          cx + (r * 1.62f * cos(angle)).toFloat(),
          cy + (r * 1.62f * sin(angle)).toFloat(),
        )
        drawLine(color = sunGold, start = start, end = end, strokeWidth = w * 0.07f, cap = StrokeCap.Round)
      }
    }

    fun drawCloud(topY: Float, color: Color = Color(0xFFE2E8F0)) {
      val bottomY = topY + h * 0.36f
      drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.14f, topY + h * 0.14f),
        size = Size(w * 0.72f, h * 0.24f),
        cornerRadius = CornerRadius(h * 0.12f, h * 0.12f),
      )
      drawCircle(color = color, radius = w * 0.20f, center = Offset(w * 0.38f, topY + h * 0.16f))
      drawCircle(color = color, radius = w * 0.16f, center = Offset(w * 0.62f, topY + h * 0.18f))
      drawCircle(color = color, radius = w * 0.13f, center = Offset(w * 0.24f, bottomY - h * 0.12f))
    }

    when (condition) {
      WeatherCondition.CLEAR -> {
        drawSun(w * 0.50f, h * 0.50f, w * 0.24f)
      }
      WeatherCondition.PARTLY_CLOUDY -> {
        drawSun(w * 0.66f, h * 0.34f, w * 0.18f)
        drawCloud(topY = h * 0.38f, color = Color(0xFFF1F5F9))
      }
      WeatherCondition.CLOUDY -> {
        drawCloud(topY = h * 0.26f, color = Color(0xFF94A3B8))
        drawCloud(topY = h * 0.38f, color = Color(0xFFE2E8F0))
      }
      WeatherCondition.FOG -> {
        drawCloud(topY = h * 0.20f, color = Color(0xFFCBD5E1))
        val fogColor = Color(0xFF94A3B8)
        drawLine(fogColor, Offset(w * 0.18f, h * 0.68f), Offset(w * 0.82f, h * 0.68f), w * 0.08f, StrokeCap.Round)
        drawLine(fogColor.copy(alpha = 0.75f), Offset(w * 0.26f, h * 0.82f), Offset(w * 0.74f, h * 0.82f), w * 0.08f, StrokeCap.Round)
      }
      WeatherCondition.DRIZZLE -> {
        drawCloud(topY = h * 0.20f, color = Color(0xFFCBD5E1))
        val drop = Color(0xFF38BDF8)
        listOf(0.32f, 0.52f, 0.70f).forEach { x ->
          drawLine(drop, Offset(w * x, h * 0.66f), Offset(w * (x - 0.05f), h * 0.79f), w * 0.07f, StrokeCap.Round)
        }
      }
      WeatherCondition.RAIN -> {
        drawCloud(topY = h * 0.18f, color = Color(0xFF94A3B8))
        val drop = Color(0xFF38BDF8)
        listOf(0.28f, 0.48f, 0.68f).forEach { x ->
          drawLine(drop, Offset(w * x, h * 0.62f), Offset(w * (x - 0.08f), h * 0.84f), w * 0.08f, StrokeCap.Round)
        }
      }
      WeatherCondition.SNOW -> {
        drawCloud(topY = h * 0.18f, color = Color(0xFFE2E8F0))
        val flake = Color(0xFFBAE6FD)
        listOf(Offset(w * 0.30f, h * 0.72f), Offset(w * 0.52f, h * 0.80f), Offset(w * 0.72f, h * 0.72f)).forEach { pt ->
          drawCircle(color = flake, radius = w * 0.07f, center = pt)
        }
      }
      WeatherCondition.STORM -> {
        drawCloud(topY = h * 0.16f, color = Color(0xFF94A3B8))
        val bolt = Path().apply {
          moveTo(w * 0.54f, h * 0.54f)
          lineTo(w * 0.38f, h * 0.73f)
          lineTo(w * 0.50f, h * 0.73f)
          lineTo(w * 0.42f, h * 0.92f)
          lineTo(w * 0.64f, h * 0.68f)
          lineTo(w * 0.52f, h * 0.68f)
          close()
        }
        drawPath(path = bolt, color = Color(0xFFFACC15))
      }
    }
  }
}
