package com.horizontv.launcher.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.horizontv.launcher.model.JellyfinSlide
import com.horizontv.launcher.model.WallpaperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AmbientBackground(
  theme: WallpaperTheme,
  customWallpaperUri: String?,
  jellyfinSlide: JellyfinSlide?,
  dimAlpha: Float,
  focusedTint: Color?,
  modifier: Modifier = Modifier,
  kenBurnsEnabled: Boolean = false,
) {
  val context = LocalContext.current
  val customBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = customWallpaperUri) {
    value = if (customWallpaperUri.isNullOrBlank()) {
      null
    } else {
      loadDownsampledWallpaper(context, customWallpaperUri)
    }
  }

  val hasImageBackdrop = jellyfinSlide != null || customBitmap != null
  val kenBurnsPhaseState = if (kenBurnsEnabled && hasImageBackdrop) {
    val infiniteTransition = rememberInfiniteTransition(label = "homeWallpaperMotion")
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 28_000, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
      ),
      label = "homeKenBurnsPhase",
    )
  } else {
    null
  }

  val motionModifier = if (kenBurnsPhaseState != null) {
    Modifier.graphicsLayer {
      val phase = kenBurnsPhaseState.value
      val scale = 1.04f + (phase * 0.10f)
      scaleX = scale
      scaleY = scale
      translationX = (phase - 0.5f) * size.width * 0.045f
      translationY = (0.5f - phase) * size.height * 0.032f
    }
  } else {
    Modifier
  }

  val animatedTopGlow by animateColorAsState(
    targetValue = if (focusedTint != null) lerp(theme.topGlow, focusedTint, 0.32f) else theme.topGlow,
    animationSpec = tween(durationMillis = 700),
    label = "ambientTopGlow",
  )
  val animatedBase by animateColorAsState(
    targetValue = theme.baseColor,
    animationSpec = tween(durationMillis = 450),
    label = "ambientBase",
  )
  val animatedMid by animateColorAsState(
    targetValue = theme.midColor,
    animationSpec = tween(durationMillis = 450),
    label = "ambientMid",
  )
  val animatedBottomGlow by animateColorAsState(
    targetValue = theme.bottomGlow,
    animationSpec = tween(durationMillis = 550),
    label = "ambientBottomGlow",
  )

  Box(modifier = modifier.fillMaxSize()) {
    if (jellyfinSlide != null) {
      // Smooth cinema-grade crossfade slideshow for Jellyfin movie & TV backdrops
      Crossfade(
        targetState = jellyfinSlide,
        animationSpec = tween(durationMillis = 1400),
        label = "jellyfinSlideshowCrossfade",
      ) { slide ->
        Image(
          bitmap = slide.image,
          contentDescription = slide.backdrop.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxSize()
            .then(motionModifier),
        )
      }
      WallpaperReadabilityScrim(dimAlpha = dimAlpha)
    } else if (customBitmap != null) {
      Image(
        bitmap = customBitmap!!,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .then(motionModifier),
      )
      WallpaperReadabilityScrim(dimAlpha = dimAlpha)
    } else {
      // Scenic Projectivy-inspired layered sky, celestial horizon glow, and mountain silhouettes
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.0f to animatedBase,
              0.38f to lerp(animatedMid, animatedTopGlow, 0.28f),
              0.62f to lerp(animatedMid, animatedBottomGlow, 0.24f),
              1.0f to animatedBase,
            ),
          ),
      )

      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val sunCenter = Offset(w * 0.74f, h * 0.24f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color.White.copy(alpha = 0.24f),
              animatedBottomGlow.copy(alpha = 0.34f),
              animatedTopGlow.copy(alpha = 0.14f),
              Color.Transparent,
            ),
            center = sunCenter,
            radius = w * 0.42f,
          ),
          center = sunCenter,
          radius = w * 0.42f,
        )

        val leftGlow = Offset(w * 0.18f, h * 0.30f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              animatedTopGlow.copy(alpha = 0.24f),
              Color.Transparent,
            ),
            center = leftGlow,
            radius = w * 0.48f,
          ),
          center = leftGlow,
          radius = w * 0.48f,
        )

        val farRidge = Path().apply {
          moveTo(0f, h * 0.64f)
          lineTo(w * 0.14f, h * 0.44f)
          lineTo(w * 0.28f, h * 0.56f)
          lineTo(w * 0.47f, h * 0.36f)
          lineTo(w * 0.64f, h * 0.52f)
          lineTo(w * 0.81f, h * 0.40f)
          lineTo(w, h * 0.54f)
          lineTo(w, h)
          lineTo(0f, h)
          close()
        }
        drawPath(
          path = farRidge,
          brush = Brush.verticalGradient(
            colors = listOf(
              lerp(animatedMid, animatedBase, 0.35f).copy(alpha = 0.82f),
              animatedBase.copy(alpha = 0.96f),
            ),
            startY = h * 0.36f,
            endY = h,
          ),
        )

        val nearRidge = Path().apply {
          moveTo(0f, h * 0.72f)
          lineTo(w * 0.20f, h * 0.55f)
          lineTo(w * 0.38f, h * 0.68f)
          lineTo(w * 0.59f, h * 0.50f)
          lineTo(w * 0.77f, h * 0.65f)
          lineTo(w, h * 0.57f)
          lineTo(w, h)
          lineTo(0f, h)
          close()
        }
        drawPath(
          path = nearRidge,
          brush = Brush.verticalGradient(
            colors = listOf(
              animatedBase.copy(alpha = 0.90f),
              Color(0xFF04070D).copy(alpha = 0.97f),
            ),
            startY = h * 0.50f,
            endY = h,
          ),
        )
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              0.0f to Color.Transparent,
              0.55f to Color.Transparent,
              0.82f to Color.Black.copy(alpha = 0.38f),
              1.0f to Color.Black.copy(alpha = 0.65f),
            ),
          ),
      )
    }
  }
}

@Composable
private fun WallpaperReadabilityScrim(dimAlpha: Float) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          0.0f to Color.Black.copy(alpha = (dimAlpha * 0.70f).coerceIn(0.18f, 0.58f)),
          0.38f to Color.Black.copy(alpha = (dimAlpha * 0.25f).coerceIn(0.08f, 0.42f)),
          0.70f to Color.Black.copy(alpha = (dimAlpha + 0.16f).coerceAtMost(0.86f)),
          1.0f to Color.Black.copy(alpha = (dimAlpha + 0.34f).coerceAtMost(0.94f)),
        ),
      ),
  )
}

private suspend fun loadDownsampledWallpaper(
  context: Context,
  uriString: String,
): ImageBitmap? = withContext(Dispatchers.IO) {
  runCatching {
    val uri = Uri.parse(uriString)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

    var sampleSize = 1
    val maxW = 1920
    val maxH = 1080
    while ((bounds.outWidth / sampleSize) > maxW * 2 || (bounds.outHeight / sampleSize) > maxH * 2) {
      sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
    }
    context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, decodeOptions)?.asImageBitmap()
    }
  }.getOrNull()
}
