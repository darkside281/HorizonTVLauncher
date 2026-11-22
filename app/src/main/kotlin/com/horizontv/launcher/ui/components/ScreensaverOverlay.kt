package com.horizontv.launcher.ui.components

import android.view.KeyEvent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.JellyfinSlide
import com.horizontv.launcher.model.LauncherSettings
import com.horizontv.launcher.model.ScreensaverMode
import com.horizontv.launcher.model.ScreensaverPhotoSlide
import com.horizontv.launcher.model.WallpaperTheme
import com.horizontv.launcher.model.WeatherData
import com.horizontv.launcher.model.WeatherIconStyle
import com.horizontv.launcher.ui.theme.LauncherPalettes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun ScreensaverOverlay(
  settings: LauncherSettings,
  theme: WallpaperTheme,
  accent: AccentPreset,
  artworkSlide: JellyfinSlide?,
  photoSlide: ScreensaverPhotoSlide?,
  weatherData: WeatherData?,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }
  val mode = settings.activeScreensaverMode

  LaunchedEffect(Unit) {
    runCatching { focusRequester.requestFocus() }
  }

  // Live clock tick synced to minute boundary
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      nowMillis = System.currentTimeMillis()
      val remainder = nowMillis % 60_000L
      delay((60_000L - remainder).coerceAtLeast(1_000L))
    }
  }

  // OLED burn-in protection: shift overlay corner every 90 seconds
  var cornerSlot by remember { mutableIntStateOf(0) }
  LaunchedEffect(settings.screensaverBurnInShift) {
    if (!settings.screensaverBurnInShift) {
      cornerSlot = 0
      return@LaunchedEffect
    }
    while (true) {
      delay(90_000L)
      cornerSlot = (cornerSlot + 1) % 4
    }
  }

  // Ambient theme cycling
  var ambientThemeIdx by remember(theme.id) {
    mutableIntStateOf(LauncherPalettes.themes.indexOfFirst { it.id == theme.id }.coerceAtLeast(0))
  }
  LaunchedEffect(mode, settings.screensaverAmbientCycle) {
    if (mode != ScreensaverMode.AMBIENT || !settings.screensaverAmbientCycle) return@LaunchedEffect
    while (true) {
      delay(22_000L)
      ambientThemeIdx = (ambientThemeIdx + 1) % LauncherPalettes.themes.size
    }
  }

  val activeAmbientTheme = if (mode == ScreensaverMode.AMBIENT && settings.screensaverAmbientCycle) {
    LauncherPalettes.themes[ambientThemeIdx % LauncherPalettes.themes.size]
  } else {
    theme
  }

  val infiniteTransition = rememberInfiniteTransition(label = "screensaverMotion")
  val kenBurnsPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 28_000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "kenBurnsPhase",
  )

  val heroShadow = remember {
    Shadow(
      color = Color.Black.copy(alpha = 0.72f),
      offset = Offset(0f, 3f),
      blurRadius = 16f,
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .focusRequester(focusRequester)
      .focusable()
      .onPreviewKeyEvent { keyEvent ->
        val native = keyEvent.nativeKeyEvent
        if (native.action == KeyEvent.ACTION_DOWN) {
          onDismiss()
          true
        } else {
          // Consume key-up so waking from screensaver never triggers an action underneath
          true
        }
      }
      .pointerInput(Unit) {
        detectTapGestures { onDismiss() }
      },
  ) {
    // 1. Visual backdrop layer
    when (mode) {
      ScreensaverMode.WALLPAPER_ARTWORK -> {
        if (artworkSlide != null) {
          Crossfade(
            targetState = artworkSlide,
            animationSpec = tween(durationMillis = 1_400),
            label = "ssArtworkFade",
          ) { slide ->
            KenBurnsImage(
              bitmap = slide.image,
              contentDescription = slide.backdrop.title,
              enabled = settings.screensaverKenBurns,
              phase = kenBurnsPhase,
              contentScale = ContentScale.Crop,
            )
          }
        } else {
          AnimatedAmbientCanvas(theme = activeAmbientTheme, phase = kenBurnsPhase)
        }
      }

      ScreensaverMode.PHOTOS -> {
        if (photoSlide != null) {
          Crossfade(
            targetState = photoSlide,
            animationSpec = tween(durationMillis = 1_400),
            label = "ssPhotoFade",
          ) { slide ->
            val isPortrait = slide.image.height > slide.image.width
            Box(modifier = Modifier.fillMaxSize()) {
              if (isPortrait) {
                // Tasteful dimmed fill behind portrait photos instead of harsh black bars
                Image(
                  bitmap = slide.image,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.22f },
                )
              }
              KenBurnsImage(
                bitmap = slide.image,
                contentDescription = slide.title,
                enabled = settings.screensaverKenBurns,
                phase = kenBurnsPhase,
                contentScale = if (isPortrait) ContentScale.Fit else ContentScale.Crop,
              )
            }
          }
        } else {
          AnimatedAmbientCanvas(theme = activeAmbientTheme, phase = kenBurnsPhase)
        }
      }

      ScreensaverMode.AMBIENT -> {
        AnimatedAmbientCanvas(theme = activeAmbientTheme, phase = kenBurnsPhase)
      }
    }

    // 2. Subtle cinema vignette + user-configurable night dimming
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            0f to Color.Black.copy(alpha = (settings.screensaverDimAlpha + 0.14f).coerceAtMost(0.88f)),
            0.35f to Color.Black.copy(alpha = settings.screensaverDimAlpha),
            0.70f to Color.Black.copy(alpha = settings.screensaverDimAlpha),
            1f to Color.Black.copy(alpha = (settings.screensaverDimAlpha + 0.26f).coerceAtMost(0.92f)),
          ),
        ),
    )

    // 3. Floating Clock, Weather, and Artwork/Photo Title with OLED Burn-In Shift
    Crossfade(
      targetState = cornerSlot,
      animationSpec = tween(durationMillis = 900),
      label = "ssCornerShift",
      modifier = Modifier.fillMaxSize(),
    ) { slot ->
      val clockAlignment = when (slot) {
        0 -> Alignment.BottomStart
        1 -> Alignment.TopStart
        2 -> Alignment.BottomEnd
        else -> Alignment.TopEnd
      }
      val titleAlignment = when (slot) {
        0 -> Alignment.BottomEnd
        1 -> Alignment.BottomStart
        2 -> Alignment.BottomStart
        else -> Alignment.BottomEnd
      }
      val clockHorizontalAlignment = when (slot) {
        2, 3 -> Alignment.End
        else -> Alignment.Start
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 56.dp, vertical = 44.dp),
      ) {
        if (settings.screensaverShowClock || (settings.screensaverShowWeather && weatherData != null)) {
          val now = remember(nowMillis) { Date(nowMillis) }
          val timeText = remember(nowMillis, settings.use24HourClock) {
            SimpleDateFormat(if (settings.use24HourClock) "HH:mm" else "h:mm", Locale.getDefault()).format(now)
          }
          val amPmText = remember(nowMillis, settings.use24HourClock) {
            if (settings.use24HourClock) "" else SimpleDateFormat("a", Locale.getDefault()).format(now)
          }
          val dateText = remember(nowMillis) {
            SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
          }

          Column(
            modifier = Modifier.align(clockAlignment),
            horizontalAlignment = clockHorizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            if (settings.screensaverShowClock) {
              Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = timeText,
                  style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 68.sp,
                    lineHeight = 70.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp,
                    shadow = heroShadow,
                  ),
                  color = Color.White,
                )
                if (amPmText.isNotEmpty()) {
                  Text(
                    text = amPmText,
                    style = MaterialTheme.typography.titleLarge.copy(
                      fontSize = 20.sp,
                      fontWeight = FontWeight.Medium,
                      shadow = heroShadow,
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 12.dp),
                  )
                }
              }
              Text(
                text = dateText,
                style = MaterialTheme.typography.titleLarge.copy(
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Normal,
                  shadow = heroShadow,
                ),
                color = Color.White.copy(alpha = 0.82f),
              )
            }

            if (settings.screensaverShowWeather && weatherData != null) {
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                WeatherConditionIcon(
                  condition = weatherData.condition,
                  style = WeatherIconStyle.fromId(settings.weatherIconStyle),
                  accent = accent,
                  size = 26.dp,
                )
                Text(
                  text = "${weatherData.currentTemp}${weatherData.unitSymbol}  •  ${weatherData.condition.label}",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    shadow = heroShadow,
                  ),
                  color = Color.White.copy(alpha = 0.90f),
                )
              }
            }
          }
        }

        // Artwork ClearLogo / Title or Photo Title
        if (settings.screensaverShowTitle) {
          when (mode) {
            ScreensaverMode.WALLPAPER_ARTWORK -> {
              if (artworkSlide != null) {
                val logo = artworkSlide.logoImage
                if (logo != null) {
                  Image(
                    bitmap = logo,
                    contentDescription = artworkSlide.backdrop.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                      .align(titleAlignment)
                      .widthIn(max = 320.dp)
                      .heightIn(max = 96.dp),
                  )
                } else {
                  val caption = buildString {
                    append(artworkSlide.backdrop.title)
                    artworkSlide.backdrop.year?.let { append(" ($it)") }
                  }
                  Text(
                    text = caption,
                    style = MaterialTheme.typography.headlineMedium.copy(
                      fontWeight = FontWeight.Medium,
                      shadow = heroShadow,
                    ),
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(titleAlignment),
                  )
                }
              }
            }

            ScreensaverMode.PHOTOS -> {
              val caption = photoSlide?.title?.ifBlank { null }
                ?: "No photos selected — choose a folder or photos in Customize → Screensaver"
              Text(
                text = caption,
                style = MaterialTheme.typography.bodyLarge.copy(shadow = heroShadow),
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(titleAlignment),
              )
            }

            ScreensaverMode.AMBIENT -> Unit
          }
        }
      }
    }
  }
}

@Composable
private fun KenBurnsImage(
  bitmap: ImageBitmap,
  contentDescription: String?,
  enabled: Boolean,
  phase: Float,
  contentScale: ContentScale,
) {
  val scale = if (enabled) 1.0f + (0.085f * phase) else 1.0f
  val panX = if (enabled) (phase - 0.5f) * 36f else 0f
  val panY = if (enabled) (0.5f - phase) * 20f else 0f

  Image(
    bitmap = bitmap,
    contentDescription = contentDescription,
    contentScale = contentScale,
    modifier = Modifier
      .fillMaxSize()
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationX = panX
        translationY = panY
      },
  )
}

@Composable
private fun AnimatedAmbientCanvas(
  theme: WallpaperTheme,
  phase: Float,
) {
  val baseColor by animateColorAsState(theme.baseColor, tween(2_400), label = "ambBase")
  val midColor by animateColorAsState(theme.midColor, tween(2_400), label = "ambMid")
  val topGlow by animateColorAsState(theme.topGlow, tween(2_400), label = "ambTop")
  val bottomGlow by animateColorAsState(theme.bottomGlow, tween(2_400), label = "ambBot")

  Canvas(modifier = Modifier.fillMaxSize()) {
    drawRect(
      brush = Brush.verticalGradient(
        0f to baseColor,
        0.55f to midColor,
        1f to baseColor,
      ),
    )

    val angle = phase * 2f * PI.toFloat()
    val orb1Center = Offset(
      x = size.width * (0.28f + 0.14f * cos(angle)),
      y = size.height * (0.30f + 0.12f * sin(angle)),
    )
    val orb2Center = Offset(
      x = size.width * (0.74f + 0.14f * sin(angle)),
      y = size.height * (0.68f + 0.12f * cos(angle)),
    )
    val orb3Center = Offset(
      x = size.width * (0.52f + 0.10f * cos(angle * 0.5f)),
      y = size.height * (0.48f + 0.10f * sin(angle * 0.5f)),
    )

    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(topGlow.copy(alpha = 0.52f), Color.Transparent),
        center = orb1Center,
        radius = size.maxDimension * 0.58f,
      ),
      radius = size.maxDimension * 0.58f,
      center = orb1Center,
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(bottomGlow.copy(alpha = 0.44f), Color.Transparent),
        center = orb2Center,
        radius = size.maxDimension * 0.54f,
      ),
      radius = size.maxDimension * 0.54f,
      center = orb2Center,
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(topGlow.copy(alpha = 0.24f), Color.Transparent),
        center = orb3Center,
        radius = size.maxDimension * 0.40f,
      ),
      radius = size.maxDimension * 0.40f,
      center = orb3Center,
    )
  }
}
