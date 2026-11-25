package com.horizontv.launcher.ui.components

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.AppFolder
import com.horizontv.launcher.model.AppInfo
import com.horizontv.launcher.model.HomeTileSize
import com.horizontv.launcher.model.TileAspectShape
import com.horizontv.launcher.model.TileCornerStyle
import com.horizontv.launcher.model.TileFocusStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderBannerCard(
  folder: AppFolder,
  apps: List<AppInfo>,
  accent: AccentPreset,
  cardSurface: Color,
  isReordering: Boolean,
  canMoveLeft: Boolean,
  canMoveRight: Boolean,
  onFocused: () -> Unit,
  onOpenFolder: () -> Unit,
  onStartReorder: () -> Unit,
  onMoveLeft: () -> Unit,
  onMoveRight: () -> Unit,
  onFinishReorder: () -> Unit,
  modifier: Modifier = Modifier,
  tileSize: HomeTileSize = HomeTileSize.MEDIUM,
  tileShape: TileAspectShape = TileAspectShape.BANNER_16_9,
  cornerStyle: TileCornerStyle = TileCornerStyle.ROUNDED,
  focusStyle: TileFocusStyle = TileFocusStyle.GLOW,
  showTitle: Boolean = true,
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

  DisposableEffect(selectKeyController) { onDispose { selectKeyController.reset() } }
  LaunchedEffect(isReordering) {
    if (isReordering) runCatching { cardFocusRequester.requestFocus() }
  }

  val (cardWidthDp, cardHeightDp, _) = remember(tileSize, tileShape) { tileShape.dimensionsFor(tileSize) }
  val cornerRadius = cornerStyle.radiusDp.dp
  val cardShape = RoundedCornerShape(cornerRadius)
  val activeFocus = isFocused || isReordering

  val scale by animateFloatAsState(
    targetValue = if (isReordering) 1.09f else if (isFocused) 1.08f else 1f,
    animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f),
    label = "folderScale",
  )
  val liftOffset by animateDpAsState(
    targetValue = if (activeFocus) (-7).dp else 0.dp,
    animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
    label = "folderLift",
  )
  val borderWidth by animateDpAsState(
    targetValue = if (activeFocus) {
      if (focusStyle == TileFocusStyle.ZOOM) 1.dp else 3.dp
    } else {
      1.dp
    },
    label = "folderBorderW",
  )
  val borderColor by animateColorAsState(
    targetValue = when {
      isReordering -> accent.primary
      isFocused -> if (focusStyle == TileFocusStyle.OUTLINE) accent.primary else Color.White
      else -> Color.White.copy(alpha = 0.22f)
    },
    label = "folderBorderC",
  )
  val cardAlpha by animateFloatAsState(
    targetValue = if (activeFocus) 1f else tileOpacity.coerceIn(0.35f, 1f),
    label = "folderAlpha",
  )

  Column(
    modifier = modifier
      .width(cardWidthDp.dp)
      .offset(y = liftOffset)
      .alpha(cardAlpha)
      .zIndex(if (activeFocus) 2f else 0f),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(cardHeightDp.dp)
        .scale(scale)
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
          elevation = if (activeFocus) 16.dp else 6.dp,
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
        .onFocusChanged { st ->
          isFocused = st.isFocused
          if (st.isFocused) onFocused() else selectKeyController.reset()
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
              KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> return@onPreviewKeyEvent true
              KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BACK -> {
                selectKeyController.reset()
                onFinishReorder()
                return@onPreviewKeyEvent true
              }
            }
          }
          selectKeyController.onKeyEvent(
            nativeEvent = native,
            onShortClick = { if (isReordering) onFinishReorder() else onOpenFolder() },
            onLongPress = { if (!isReordering) onStartReorder() },
          )
        }
        .combinedClickable(
          onClick = { if (isReordering) onFinishReorder() else onOpenFolder() },
          onLongClick = { if (!isReordering) onStartReorder() },
        ),
      shape = cardShape,
      color = cardSurface.copy(alpha = 0.88f),
      border = BorderStroke(borderWidth, borderColor),
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color.White.copy(alpha = 0.10f),
                cardSurface.copy(alpha = 0.94f),
                accent.primary.copy(alpha = 0.14f),
              ),
            ),
          ),
      ) {
        val previewApps = apps.take(4)
        if (previewApps.isEmpty()) {
          Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = folder.title,
            tint = accent.primary.copy(alpha = 0.85f),
            modifier = Modifier
              .align(Alignment.Center)
              .size(44.dp),
          )
        } else {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            for (r in 0..1) {
              Row(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                for (c in 0..1) {
                  val app = previewApps.getOrNull(r * 2 + c)
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxSize()
                      .clip(RoundedCornerShape(8.dp))
                      .background(
                        if (app != null) app.tintColor.copy(alpha = 0.35f)
                        else Color.White.copy(alpha = 0.05f),
                      ),
                    contentAlignment = Alignment.Center,
                  ) {
                    if (app != null) {
                      val img = app.banner ?: app.icon
                      if (img != null) {
                        Image(
                          bitmap = img,
                          contentDescription = app.label,
                          contentScale = if (app.banner != null) ContentScale.Crop else ContentScale.Fit,
                          modifier = Modifier.fillMaxSize().padding(if (app.banner != null) 0.dp else 4.dp),
                        )
                      } else {
                        Text(
                          text = app.label.take(2).uppercase(),
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                          color = Color.White,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Subtle count badge top-right
        Surface(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp),
          shape = RoundedCornerShape(50),
          color = accent.primary,
        ) {
          Text(
            text = apps.size.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = accent.onPrimary,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }

        if (isReordering) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.52f))
              .padding(horizontal = 10.dp),
          ) {
            if (canMoveLeft) {
              Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = "Move Left",
                tint = accent.primary,
                modifier = Modifier
                  .align(Alignment.CenterStart)
                  .size(30.dp)
                  .background(Color.Black.copy(alpha = 0.72f), CircleShape),
              )
            }
            Surface(
              modifier = Modifier.align(Alignment.Center),
              shape = RoundedCornerShape(50),
              color = accent.primary,
            ) {
              Text(
                text = "MOVE ◄ ►",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent.onPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
            if (canMoveRight) {
              Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Move Right",
                tint = accent.primary,
                modifier = Modifier
                  .align(Alignment.CenterEnd)
                  .size(30.dp)
                  .background(Color.Black.copy(alpha = 0.72f), CircleShape),
              )
            }
          }
        }
      }
    }

    if (showTitle) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = folder.title,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = if (activeFocus) FontWeight.SemiBold else FontWeight.Medium,
        ),
        color = if (activeFocus) Color.White else Color.White.copy(alpha = 0.80f),
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

@Composable
fun FolderPopupDialog(
  folder: AppFolder,
  apps: List<AppInfo>,
  accent: AccentPreset,
  cardSurface: Color,
  isLockedLayout: Boolean,
  isAppPinLocked: (String) -> Boolean,
  onLaunchApp: (AppInfo) -> Unit,
  onOpenAppMenu: (AppInfo) -> Unit,
  onEditApps: () -> Unit,
  onRenameFolder: () -> Unit,
  onDeleteFolder: () -> Unit,
  onDismiss: () -> Unit,
) {
  val firstTileRequester = remember { FocusRequester() }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    LaunchedEffect(folder.id, apps.size) {
      runCatching { firstTileRequester.requestFocus() }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.72f))
        .pointerInput(Unit) { detectTapGestures { onDismiss() } },
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier
          .width(740.dp)
          .heightIn(max = 560.dp)
          .pointerInput(Unit) { detectTapGestures { /* consume */ } },
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF0E1729),
        border = BorderStroke(1.5.dp, accent.primary.copy(alpha = 0.36f)),
        tonalElevation = 14.dp,
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .background(accent.primary.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Folder,
                  contentDescription = null,
                  tint = accent.primary,
                  modifier = Modifier.size(22.dp),
                )
              }
              Column {
                Text(
                  text = folder.title,
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${apps.size} app${if (apps.size == 1) "" else "s"}  •  OK to launch  •  Hold OK for options",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White.copy(alpha = 0.60f),
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (!isLockedLayout) {
                QuickActionIcon(
                  icon = Icons.Rounded.Add,
                  contentDescription = "Edit Folder Apps",
                  accent = accent,
                  onClick = onEditApps,
                  modifier = if (apps.isEmpty()) Modifier.focusRequester(firstTileRequester) else Modifier,
                )
                QuickActionIcon(
                  icon = Icons.Rounded.Edit,
                  contentDescription = "Rename Folder",
                  accent = accent,
                  onClick = onRenameFolder,
                )
                QuickActionIcon(
                  icon = Icons.Rounded.DeleteOutline,
                  contentDescription = "Delete Folder",
                  accent = accent,
                  onClick = onDeleteFolder,
                )
              }
              QuickActionIcon(
                icon = Icons.Rounded.Close,
                contentDescription = "Close Folder",
                accent = accent,
                onClick = onDismiss,
              )
            }
          }

          if (apps.isEmpty()) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Color.White.copy(alpha = 0.05f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            ) {
              Text(
                text = "This folder is empty. Press + above to add apps.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.74f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
              )
            }
          } else {
            val rows = remember(apps) { apps.chunked(3) }
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              rows.forEachIndexed { rIdx, rowApps ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  rowApps.forEachIndexed { cIdx, app ->
                    AppGridTile(
                      app = app,
                      accent = accent,
                      cardSurface = cardSurface,
                      isFavorite = false,
                      isPinLocked = isAppPinLocked(app.packageName),
                      onFocused = {},
                      onLaunch = {
                        onDismiss()
                        onLaunchApp(app)
                      },
                      onOpenMenu = {
                        onDismiss()
                        onOpenAppMenu(app)
                      },
                      modifier = Modifier.weight(1f),
                      tileFocusRequester = if (rIdx == 0 && cIdx == 0) {
                        firstTileRequester
                      } else {
                        remember(app.packageName) { FocusRequester() }
                      },
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
    }
  }
}
