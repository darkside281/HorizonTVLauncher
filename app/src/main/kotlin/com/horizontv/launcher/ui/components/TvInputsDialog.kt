package com.horizontv.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SettingsInputComponent
import androidx.compose.material.icons.rounded.SettingsInputHdmi
import androidx.compose.material.icons.rounded.Tv
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.TvInputSource

@Composable
fun TvInputsDialog(
  inputs: List<TvInputSource>,
  accent: AccentPreset,
  onSelectInput: (TvInputSource) -> Unit,
  onDismiss: () -> Unit,
) {
  val firstItemRequester = remember { FocusRequester() }
  val scrollState = rememberScrollState()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    LaunchedEffect(inputs.size) {
      runCatching { firstItemRequester.requestFocus() }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.68f)),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier.width(520.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0E1525),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.16f)),
        shadowElevation = 28.dp,
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.primary.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, accent.primary.copy(alpha = 0.45f)),
                modifier = Modifier.size(42.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.SettingsInputHdmi,
                    contentDescription = null,
                    tint = accent.primary,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
              Column {
                Text(
                  text = "TV Inputs",
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                )
                Text(
                  text = "Select an HDMI or TV source",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.62f),
                )
              }
            }

            QuickActionIcon(
              icon = Icons.Rounded.Close,
              contentDescription = "Close TV Inputs",
              accent = accent,
              onClick = onDismiss,
            )
          }

          if (inputs.isEmpty()) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color.White.copy(alpha = 0.05f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "No hardware HDMI or TV inputs were reported by this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.68f),
                modifier = Modifier.padding(16.dp),
              )
            }
          } else {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(scrollState),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              inputs.forEachIndexed { idx, input ->
                TvInputRow(
                  input = input,
                  accent = accent,
                  onClick = { onSelectInput(input) },
                  modifier = if (idx == 0) Modifier.focusRequester(firstItemRequester) else Modifier,
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
private fun TvInputRow(
  input: TvInputSource,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = if (isFocused) accent.primary else Color.White.copy(alpha = 0.06f),
    label = "tvInputRowBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White

  val icon = when {
    input.typeLabel.equals("HDMI", ignoreCase = true) -> Icons.Rounded.SettingsInputHdmi
    input.typeLabel.equals("Tuner", ignoreCase = true) -> Icons.Rounded.Tv
    else -> Icons.Rounded.SettingsInputComponent
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = if (isFocused) {
      BorderStroke(2.dp, Color.White)
    } else {
      BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isFocused) accent.onPrimary else accent.primary,
          modifier = Modifier.size(22.dp),
        )
        Column {
          Text(
            text = input.label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = if (input.isConnected) "${input.typeLabel} • Connected" else input.typeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isFocused) accent.onPrimary.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.58f),
          )
        }
      }

      if (input.isConnected) {
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(if (isFocused) accent.onPrimary else Color(0xFF34D399)),
        )
      }
    }
  }
}
