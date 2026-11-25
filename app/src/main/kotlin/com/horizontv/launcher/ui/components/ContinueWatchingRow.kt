package com.horizontv.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.ContinueWatchingItem
import com.horizontv.launcher.model.HomeTileSize
import com.horizontv.launcher.model.TileCornerStyle
import com.horizontv.launcher.model.TileFocusStyle

@Composable
fun ContinueWatchingRowSection(
  items: List<ContinueWatchingItem>,
  accent: AccentPreset,
  cardSurface: Color,
  tileSize: HomeTileSize,
  listState: LazyListState,
  upFocusRequester: FocusRequester?,
  downFocusRequester: FocusRequester?,
  focusRequesterFor: (String) -> FocusRequester,
  onFocused: (ContinueWatchingItem) -> Unit,
  onClickItem: (ContinueWatchingItem) -> Unit,
  compactVerticalPadding: Boolean = false,
  cornerStyle: TileCornerStyle = TileCornerStyle.ROUNDED,
  focusStyle: TileFocusStyle = TileFocusStyle.GLOW,
  tileOpacity: Float = 1.0f,
  tileSpacingDp: Dp = 20.dp,
) {
  if (items.isEmpty()) return
  val providerLabel = items.first().provider.label
  val verticalPad = if (compactVerticalPadding) 12.dp else 18.dp

  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 36.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Continue Watching",
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
      )
      Surface(
        shape = RoundedCornerShape(50),
        color = accent.primary.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, accent.primary.copy(alpha = 0.42f)),
      ) {
        Text(
          text = providerLabel,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = accent.primary,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
        )
      }
    }

    LazyRow(
      state = listState,
      horizontalArrangement = Arrangement.spacedBy(tileSpacingDp),
      contentPadding = PaddingValues(top = verticalPad, bottom = verticalPad, start = 36.dp, end = 36.dp),
    ) {
      itemsIndexed(items = items, key = { _, it -> "cw_${it.provider.id}_${it.itemId}" }) { idx, item ->
        ContinueWatchingCard(
          item = item,
          accent = accent,
          cardSurface = cardSurface,
          tileSize = tileSize,
          cornerStyle = cornerStyle,
          focusStyle = focusStyle,
          tileOpacity = tileOpacity,
          blockLeft = idx == 0,
          blockRight = idx == items.lastIndex,
          focusRequester = focusRequesterFor(item.itemId),
          upFocusRequester = upFocusRequester,
          downFocusRequester = downFocusRequester,
          onFocused = { onFocused(item) },
          onClick = { onClickItem(item) },
        )
      }
    }
  }
}

@Composable
private fun ContinueWatchingCard(
  item: ContinueWatchingItem,
  accent: AccentPreset,
  cardSurface: Color,
  tileSize: HomeTileSize,
  cornerStyle: TileCornerStyle,
  focusStyle: TileFocusStyle,
  tileOpacity: Float,
  blockLeft: Boolean,
  blockRight: Boolean,
  focusRequester: FocusRequester,
  upFocusRequester: FocusRequester?,
  downFocusRequester: FocusRequester?,
  onFocused: () -> Unit,
  onClick: () -> Unit,
) {
  var isFocused by remember { mutableStateOf(false) }
  val cornerRadius = cornerStyle.radiusDp.dp
  val shape = RoundedCornerShape(cornerRadius)
  val cardHeightDp = tileSize.heightDp
  val cardWidthDp = (cardHeightDp * 16 / 9).coerceAtLeast(tileSize.widthDp)

  val scale by animateFloatAsState(
    targetValue = if (isFocused) 1.07f else 1f,
    animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f),
    label = "cwScale",
  )
  val lift by animateDpAsState(
    targetValue = if (isFocused) (-7).dp else 0.dp,
    animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
    label = "cwLift",
  )
  val borderColor by animateColorAsState(
    targetValue = if (isFocused) {
      if (focusStyle == TileFocusStyle.OUTLINE) accent.primary else Color.White
    } else {
      Color.White.copy(alpha = 0.18f)
    },
    label = "cwBorder",
  )
  val alpha by animateFloatAsState(
    targetValue = if (isFocused) 1f else tileOpacity.coerceIn(0.35f, 1f),
    label = "cwAlpha",
  )

  Column(
    modifier = Modifier
      .width(cardWidthDp.dp)
      .offset(y = lift)
      .alpha(alpha)
      .zIndex(if (isFocused) 2f else 0f),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(cardHeightDp.dp)
        .scale(scale)
        .drawBehind {
          if (isFocused && focusStyle == TileFocusStyle.GLOW) {
            val baseRadius = cornerRadius.toPx()
            for (i in 1..4) {
              val spread = i * 3.dp.toPx()
              drawRoundRect(
                color = accent.glow.copy(alpha = 0.22f / i),
                topLeft = Offset(-spread, -spread),
                size = Size(size.width + spread * 2f, size.height + spread * 2f),
                cornerRadius = CornerRadius(baseRadius + spread * 0.55f),
              )
            }
          }
        }
        .shadow(if (isFocused) 16.dp else 6.dp, shape)
        .focusRequester(focusRequester)
        .focusProperties {
          if (upFocusRequester != null) up = upFocusRequester
          if (downFocusRequester != null) down = downFocusRequester
          if (blockLeft) left = FocusRequester.Cancel
          if (blockRight) right = FocusRequester.Cancel
        }
        .onFocusChanged { st ->
          isFocused = st.isFocused
          if (st.isFocused) onFocused()
        }
        .clickable(onClick = onClick),
      shape = shape,
      color = cardSurface,
      border = BorderStroke(if (isFocused && focusStyle != TileFocusStyle.ZOOM) 3.dp else 1.dp, borderColor),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (item.thumbnail != null) {
          Image(
            bitmap = item.thumbnail,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.linearGradient(
                  listOf(accent.primary.copy(alpha = 0.24f), cardSurface),
                ),
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = item.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White.copy(alpha = 0.85f),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(14.dp),
            )
          }
        }

        // Bottom readability scrim with title + progress bar neatly inside the card (never clips!)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .align(Alignment.BottomCenter)
            .background(
              Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))),
            ),
        )

        // Top-left Episode / Movie badge
        if (item.badge.isNotBlank()) {
          Surface(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, accent.primary.copy(alpha = 0.48f)),
          ) {
            Text(
              text = item.badge,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = accent.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }

        // Top-right remaining time badge
        if (item.remainingLabel.isNotBlank() && item.remainingLabel != item.badge) {
          Surface(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.72f),
          ) {
            Text(
              text = item.remainingLabel,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color = Color.White.copy(alpha = 0.90f),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }

        // Bottom overlay: Title + Progress bar inside the card so it always fits cleanly
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (item.progress > 0.01f) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.28f)),
            ) {
              Box(
                modifier = Modifier
                  .fillMaxHeight()
                  .fillMaxWidth(item.progress.coerceIn(0.04f, 1f))
                  .background(accent.primary),
              )
            }
          }
        }
      }
    }
  }
}
