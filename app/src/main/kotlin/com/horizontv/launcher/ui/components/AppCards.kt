package com.horizontv.launcher.ui.components

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.AppInfo
import com.horizontv.launcher.model.HomeTileSize
import com.horizontv.launcher.model.TileAspectShape
import com.horizontv.launcher.model.TileCornerStyle
import com.horizontv.launcher.model.TileFocusStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteBannerCard(
  app: AppInfo,
  accent: AccentPreset,
  cardSurface: Color,
  isReordering: Boolean,
  canMoveLeft: Boolean,
  canMoveRight: Boolean,
  onFocused: () -> Unit,
  onLaunch: () -> Unit,
  onOpenMenu: () -> Unit,
  onMoveLeft: () -> Unit,
  onMoveRight: () -> Unit,
  onFinishReorder: () -> Unit,
  modifier: Modifier = Modifier,
  tileSize: HomeTileSize = HomeTileSize.MEDIUM,
  tileShape: TileAspectShape = TileAspectShape.BANNER_16_9,
  cornerStyle: TileCornerStyle = TileCornerStyle.ROUNDED,
  focusStyle: TileFocusStyle = TileFocusStyle.GLOW,
  showTitle: Boolean = true,
  isPinLocked: Boolean = false,
  tileOpacity: Float = 1.0f,
  blockLeft: Boolean = false,
  blockRight: Boolean = false,
  cardFocusRequester: FocusRequester = remember { FocusRequester() },
  upFocusRequester: FocusRequester? = null,
  downFocusRequester: FocusRequester? = null,
) {
  var isFocused by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val selectKeyController = remember(scope) { TvSelectKeyController(scope) }

  DisposableEffect(selectKeyController) {
    onDispose { selectKeyController.reset() }
  }

  LaunchedEffect(isReordering) {
    if (isReordering) {
      runCatching { cardFocusRequester.requestFocus() }
    }
  }

  val (cardWidthDp, cardHeightDp, cardIconDp) = remember(tileSize, tileShape) {
    tileShape.dimensionsFor(tileSize)
  }
  val cornerRadius = cornerStyle.radiusDp.dp
  val cardShape = RoundedCornerShape(cornerRadius)
  val activeFocus = isFocused || isReordering

  val targetScale = when {
    isReordering -> 1.09f
    isFocused -> when (focusStyle) {
      TileFocusStyle.ZOOM -> 1.11f
      TileFocusStyle.SHADOW -> 1.06f
      else -> 1.08f
    }
    else -> 1f
  }
  val scaleState = animateFloatAsState(
    targetValue = targetScale,
    animationSpec = spring(dampingRatio = 0.82f, stiffness = 650f),
    label = "favScale",
  )
  val liftDpState = animateFloatAsState(
    targetValue = if (activeFocus) {
      if (focusStyle == TileFocusStyle.SHADOW) -9f else -7f
    } else {
      0f
    },
    animationSpec = spring(dampingRatio = 0.82f, stiffness = 650f),
    label = "favLift",
  )
  val borderWidth by animateDpAsState(
    targetValue = when {
      isReordering -> 3.dp
      isFocused -> when (focusStyle) {
        TileFocusStyle.GLOW -> 3.dp
        TileFocusStyle.OUTLINE -> 3.5.dp
        TileFocusStyle.ZOOM -> 1.dp
        TileFocusStyle.SHADOW -> 1.5.dp
      }
      else -> 1.dp
    },
    label = "favBorderWidth",
  )
  val borderColor by animateColorAsState(
    targetValue = when {
      isReordering -> accent.primary
      isFocused -> when (focusStyle) {
        TileFocusStyle.GLOW -> Color.White
        TileFocusStyle.OUTLINE -> accent.primary
        TileFocusStyle.ZOOM -> Color.White.copy(alpha = 0.28f)
        TileFocusStyle.SHADOW -> Color.White.copy(alpha = 0.50f)
      }
      else -> Color.White.copy(alpha = 0.18f)
    },
    label = "favBorderColor",
  )
  val cardAlphaState = animateFloatAsState(
    targetValue = if (activeFocus) 1f else tileOpacity.coerceIn(0.35f, 1f),
    label = "favAlpha",
  )

  Column(
    modifier = modifier
      .width(cardWidthDp.dp)
      .graphicsLayer {
        translationY = liftDpState.value * density
        alpha = cardAlphaState.value
      }
      .zIndex(if (activeFocus) 2f else 0f),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(cardHeightDp.dp)
        .graphicsLayer {
          val s = scaleState.value
          scaleX = s
          scaleY = s
        }
        .drawBehind {
          if (activeFocus && focusStyle == TileFocusStyle.GLOW) {
            val baseRadius = cornerRadius.toPx()
            for (i in 1..4) {
              val spread = (i * 3.dp.toPx())
              drawRoundRect(
                color = accent.glow.copy(alpha = 0.22f / i),
                topLeft = Offset(-spread, -spread),
                size = Size(size.width + spread * 2f, size.height + spread * 2f),
                cornerRadius = CornerRadius(baseRadius + spread * 0.55f),
              )
            }
          }
        }
        .shadow(
          elevation = if (activeFocus) {
            when (focusStyle) {
              TileFocusStyle.SHADOW -> 24.dp
              TileFocusStyle.ZOOM -> 18.dp
              else -> 14.dp
            }
          } else {
            6.dp
          },
          shape = cardShape,
          ambientColor = Color.Black,
          spotColor = Color.Black,
        )
        .focusRequester(cardFocusRequester)
        .focusProperties {
          if (upFocusRequester != null) up = upFocusRequester
          if (downFocusRequester != null) down = downFocusRequester
          if (blockLeft) left = FocusRequester.Cancel
          if (blockRight) right = FocusRequester.Cancel
        }
        .onFocusChanged { state ->
          isFocused = state.isFocused
          if (state.isFocused) {
            onFocused()
          } else {
            selectKeyController.reset()
          }
        }
        .onPreviewKeyEvent { keyEvent ->
          val native = keyEvent.nativeKeyEvent
          if (isReordering && native.action == KeyEvent.ACTION_DOWN) {
            when (native.keyCode) {
              KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (canMoveLeft) onMoveLeft()
                return@onPreviewKeyEvent true
              }
              KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (canMoveRight) onMoveRight()
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
                selectKeyController.reset()
                onFinishReorder()
                return@onPreviewKeyEvent true
              }
            }
          }
          selectKeyController.onKeyEvent(
            nativeEvent = native,
            onShortClick = { if (isReordering) onFinishReorder() else onLaunch() },
            onLongPress = { if (!isReordering) onOpenMenu() },
          )
        }
        .combinedClickable(
          onClick = {
            if (isReordering) onFinishReorder() else onLaunch()
          },
          onLongClick = {
            if (!isReordering) onOpenMenu()
          },
        ),
      shape = cardShape,
      color = cardSurface,
      border = BorderStroke(borderWidth, borderColor),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        val useBannerFill = app.banner != null && (tileShape != TileAspectShape.SQUARE_1_1 || app.hasCustomIcon)
        if (useBannerFill && app.banner != null) {
          Image(
            bitmap = app.banner,
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.20f),
                  ),
                ),
              ),
          )
        } else {
          // Harmonized rich glass card with centered official icon (label sits cleanly below)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.linearGradient(
                  colors = listOf(
                    app.tintColor.copy(alpha = 0.54f),
                    cardSurface.copy(alpha = 0.92f),
                    app.tintColor.copy(alpha = 0.28f),
                  ),
                ),
              ),
            contentAlignment = Alignment.Center,
          ) {
            AppIconBadge(app = app, sizeDp = cardIconDp)
          }
        }

        if (isPinLocked && !isReordering) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp)
              .size(24.dp)
              .background(Color.Black.copy(alpha = 0.68f), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = "PIN Locked",
              tint = Color.White,
              modifier = Modifier.size(13.dp),
            )
          }
        }

        if (isReordering) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.50f))
              .padding(horizontal = 12.dp),
          ) {
            if (canMoveLeft) {
              Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = "Move Left",
                tint = accent.primary,
                modifier = Modifier
                  .align(Alignment.CenterStart)
                  .size(34.dp)
                  .background(Color.Black.copy(alpha = 0.70f), CircleShape),
              )
            }
            Surface(
              modifier = Modifier.align(Alignment.Center),
              shape = RoundedCornerShape(50),
              color = accent.primary,
            ) {
              Text(
                text = "MOVE  ◄ ►",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accent.onPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              )
            }
            if (canMoveRight) {
              Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Move Right",
                tint = accent.primary,
                modifier = Modifier
                  .align(Alignment.CenterEnd)
                  .size(34.dp)
                  .background(Color.Black.copy(alpha = 0.70f), CircleShape),
              )
            }
          }
        }
      }
    }

    if (showTitle) {
      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = app.label,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = if (isFocused || isReordering) FontWeight.SemiBold else FontWeight.Medium,
        ),
        color = if (isFocused || isReordering) Color.White else Color.White.copy(alpha = 0.76f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp),
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridTile(
  app: AppInfo,
  accent: AccentPreset,
  cardSurface: Color,
  isFavorite: Boolean,
  isPinLocked: Boolean = false,
  tileOpacity: Float = 1.0f,
  onFocused: () -> Unit,
  onLaunch: () -> Unit,
  onOpenMenu: () -> Unit,
  modifier: Modifier = Modifier,
  showTitle: Boolean = true,
  tileFocusRequester: FocusRequester = remember { FocusRequester() },
  upFocusRequester: FocusRequester? = null,
  downFocusRequester: FocusRequester? = null,
) {
  var isFocused by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val selectKeyController = remember(scope) { TvSelectKeyController(scope) }

  DisposableEffect(selectKeyController) {
    onDispose { selectKeyController.reset() }
  }

  val scaleState = animateFloatAsState(
    targetValue = if (isFocused) 1.07f else 1f,
    animationSpec = spring(dampingRatio = 0.82f, stiffness = 650f),
    label = "tileScale",
  )
  val liftDpState = animateFloatAsState(
    targetValue = if (isFocused) -5f else 0f,
    animationSpec = spring(dampingRatio = 0.82f, stiffness = 650f),
    label = "tileLift",
  )
  val borderWidth by animateDpAsState(
    targetValue = if (isFocused) 2.5.dp else 1.dp,
    label = "tileBorderWidth",
  )
  val borderColor by animateColorAsState(
    targetValue = if (isFocused) Color.White else Color.White.copy(alpha = 0.14f),
    label = "tileBorderColor",
  )
  val cardAlphaState = animateFloatAsState(
    targetValue = if (isFocused) 1f else tileOpacity.coerceIn(0.35f, 1f),
    label = "tileAlpha",
  )

  Column(
    modifier = modifier
      .graphicsLayer {
        translationY = liftDpState.value * density
        alpha = cardAlphaState.value
      }
      .zIndex(if (isFocused) 2f else 0f),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(112.dp)
        .graphicsLayer {
          val s = scaleState.value
          scaleX = s
          scaleY = s
        }
        .shadow(
          elevation = if (isFocused) 20.dp else 4.dp,
          shape = RoundedCornerShape(16.dp),
          ambientColor = Color.Black,
          spotColor = if (isFocused) accent.glow else Color.Black,
        )
        .focusRequester(tileFocusRequester)
        .focusProperties {
          if (upFocusRequester != null) up = upFocusRequester
          if (downFocusRequester != null) down = downFocusRequester
        }
        .onFocusChanged { state ->
          isFocused = state.isFocused
          if (state.isFocused) {
            onFocused()
          } else {
            selectKeyController.reset()
          }
        }
        .onPreviewKeyEvent { keyEvent ->
          selectKeyController.onKeyEvent(
            nativeEvent = keyEvent.nativeKeyEvent,
            onShortClick = onLaunch,
            onLongPress = onOpenMenu,
          )
        }
        .combinedClickable(
          onClick = onLaunch,
          onLongClick = onOpenMenu,
        ),
      shape = RoundedCornerShape(16.dp),
      color = cardSurface.copy(alpha = if (isFocused) 0.96f else 0.80f),
      border = BorderStroke(borderWidth, borderColor),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (app.banner != null) {
          // Full native Android TV banner artwork when available
          Image(
            bitmap = app.banner,
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          // Clean glass card with official app icon + ambient tint wash
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.linearGradient(
                  colors = listOf(
                    app.tintColor.copy(alpha = if (isFocused) 0.45f else 0.28f),
                    cardSurface.copy(alpha = 0.88f),
                  ),
                ),
              ),
            contentAlignment = Alignment.Center,
          ) {
            AppIconBadge(app = app, sizeDp = 58)
          }
        }

        if (isPinLocked) {
          Box(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp)
              .size(22.dp)
              .background(Color.Black.copy(alpha = 0.68f), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = "PIN Locked",
              tint = Color.White,
              modifier = Modifier.size(12.dp),
            )
          }
        }

        if (isFavorite) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp)
              .size(22.dp)
              .background(Color.Black.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Star,
              contentDescription = "Favorite",
              tint = accent.primary,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
    }

    if (showTitle) {
      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = app.label,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium,
        ),
        color = if (isFocused) Color.White else Color.White.copy(alpha = 0.78f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp),
      )
    }
  }
}

@Composable
fun AddFavoriteTile(
  accent: AccentPreset,
  cardSurface: Color,
  onFocused: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tileSize: HomeTileSize = HomeTileSize.MEDIUM,
  showTitle: Boolean = true,
  isPinLocked: Boolean = false,
  tileOpacity: Float = 1.0f,
  blockLeft: Boolean = false,
  blockRight: Boolean = false,
  cardFocusRequester: FocusRequester = remember { FocusRequester() },
  upFocusRequester: FocusRequester? = null,
  downFocusRequester: FocusRequester? = null,
  tileShape: TileAspectShape = TileAspectShape.BANNER_16_9,
  cornerStyle: TileCornerStyle = TileCornerStyle.ROUNDED,
  focusStyle: TileFocusStyle = TileFocusStyle.GLOW,
) {
  var isFocused by remember { mutableStateOf(false) }
  val (shapeW, shapeH, _) = remember(tileSize, tileShape) { tileShape.dimensionsFor(tileSize) }
  val addWidthDp = if (tileShape == TileAspectShape.BANNER_16_9) tileSize.addTileWidthDp else shapeW
  val cornerRadius = cornerStyle.radiusDp.dp
  val cardShape = RoundedCornerShape(cornerRadius)

  val scaleState = animateFloatAsState(
    targetValue = if (isFocused) 1.08f else 1f,
    animationSpec = spring(dampingRatio = 0.82f, stiffness = 650f),
    label = "addTileScale",
  )
  val liftDpState = animateFloatAsState(
    targetValue = if (isFocused) -7f else 0f,
    animationSpec = spring(dampingRatio = 0.82f, stiffness = 650f),
    label = "addTileLift",
  )
  val borderColor by animateColorAsState(
    targetValue = if (isFocused) {
      if (focusStyle == TileFocusStyle.OUTLINE) accent.primary else Color.White
    } else {
      Color.White.copy(alpha = 0.22f)
    },
    label = "addTileBorder",
  )
  val cardAlphaState = animateFloatAsState(
    targetValue = if (isFocused) 1f else tileOpacity.coerceIn(0.35f, 1f),
    label = "addTileAlpha",
  )

  Column(
    modifier = modifier
      .width(addWidthDp.dp)
      .graphicsLayer {
        translationY = liftDpState.value * density
        alpha = cardAlphaState.value
      }
      .zIndex(if (isFocused) 2f else 0f),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(shapeH.dp)
        .graphicsLayer {
          val s = scaleState.value
          scaleX = s
          scaleY = s
        }
        .drawBehind {
          if (isFocused && focusStyle == TileFocusStyle.GLOW) {
            val baseRadius = cornerRadius.toPx()
            for (i in 1..4) {
              val spread = (i * 3.dp.toPx())
              drawRoundRect(
                color = accent.glow.copy(alpha = 0.22f / i),
                topLeft = Offset(-spread, -spread),
                size = Size(size.width + spread * 2f, size.height + spread * 2f),
                cornerRadius = CornerRadius(baseRadius + spread * 0.55f),
              )
            }
          }
        }
        .shadow(
          elevation = if (isFocused) 12.dp else 4.dp,
          shape = cardShape,
          ambientColor = Color.Black,
          spotColor = Color.Black,
        )
        .focusRequester(cardFocusRequester)
        .focusProperties {
          if (upFocusRequester != null) up = upFocusRequester
          if (downFocusRequester != null) down = downFocusRequester
          if (blockLeft) left = FocusRequester.Cancel
          if (blockRight) right = FocusRequester.Cancel
        }
        .onFocusChanged { state ->
          isFocused = state.isFocused
          if (state.isFocused) onFocused()
        }
        .clickable(onClick = onClick),
      shape = cardShape,
      color = if (isFocused) accent.primary.copy(alpha = 0.28f) else cardSurface.copy(alpha = 0.56f),
      border = BorderStroke(if (isFocused) 3.dp else 1.5.dp, borderColor),
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier
            .size(50.dp)
            .background(
              if (isFocused) accent.primary else Color.White.copy(alpha = 0.12f),
              CircleShape,
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add App",
            tint = if (isFocused) accent.onPrimary else Color.White,
            modifier = Modifier.size(28.dp),
          )
        }
      }
    }

    if (showTitle) {
      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Add App",
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium,
        ),
        color = if (isFocused) Color.White else Color.White.copy(alpha = 0.72f),
        maxLines = 1,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
fun AppIconBadge(
  app: AppInfo,
  sizeDp: Int,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .size(sizeDp.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(Color.White.copy(alpha = 0.08f)),
    contentAlignment = Alignment.Center,
  ) {
    if (app.icon != null) {
      Image(
        bitmap = app.icon,
        contentDescription = app.label,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Icon(
        imageVector = Icons.Rounded.Apps,
        contentDescription = app.label,
        tint = Color.White.copy(alpha = 0.8f),
        modifier = Modifier.size((sizeDp * 0.55f).dp),
      )
    }
  }
}

internal class TvSelectKeyController(
  private val scope: CoroutineScope,
) {
  private var holdJob: Job? = null
  private var isHoldingSelect: Boolean = false
  private var longPressFired: Boolean = false
  private var pressDownUptimeMs: Long = 0L

  fun reset() {
    holdJob?.cancel()
    holdJob = null
    isHoldingSelect = false
    longPressFired = false
    pressDownUptimeMs = 0L
  }

  fun onKeyEvent(
    nativeEvent: KeyEvent,
    onShortClick: () -> Unit,
    onLongPress: () -> Unit,
  ): Boolean {
    val keyCode = nativeEvent.keyCode
    if (keyCode == KeyEvent.KEYCODE_MENU && nativeEvent.action == KeyEvent.ACTION_UP) {
      reset()
      onLongPress()
      return true
    }
    val isSelectKey = keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
      keyCode == KeyEvent.KEYCODE_ENTER ||
      keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
    if (!isSelectKey) return false

    when (nativeEvent.action) {
      KeyEvent.ACTION_DOWN -> {
        if (nativeEvent.repeatCount == 0) {
          reset()
          isHoldingSelect = true
          pressDownUptimeMs = SystemClock.uptimeMillis()
          holdJob = scope.launch {
            delay(400L)
            if (isHoldingSelect && !longPressFired) {
              longPressFired = true
              onLongPress()
            }
          }
        } else if (
          isHoldingSelect &&
          !longPressFired &&
          (nativeEvent.isLongPress ||
            nativeEvent.repeatCount >= 2 ||
            (nativeEvent.eventTime - nativeEvent.downTime) >= 380L)
        ) {
          holdJob?.cancel()
          longPressFired = true
          onLongPress()
        }
        // Always consume select key down so combinedClickable never double-triggers on release
        return true
      }
      KeyEvent.ACTION_UP -> {
        holdJob?.cancel()
        holdJob = null
        val heldMs = maxOf(
          nativeEvent.eventTime - nativeEvent.downTime,
          if (pressDownUptimeMs > 0L) SystemClock.uptimeMillis() - pressDownUptimeMs else 0L,
        )
        val wasHolding = isHoldingSelect
        val alreadyFiredLong = longPressFired
        isHoldingSelect = false
        longPressFired = false
        pressDownUptimeMs = 0L

        if (!alreadyFiredLong && (nativeEvent.isLongPress || (wasHolding && heldMs >= 380L))) {
          onLongPress()
        } else if (wasHolding && !alreadyFiredLong && !nativeEvent.isCanceled) {
          onShortClick()
        }
        return true
      }
    }
    return false
  }
}
