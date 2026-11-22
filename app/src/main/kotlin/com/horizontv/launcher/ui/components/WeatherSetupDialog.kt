package com.horizontv.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.WeatherLocation

@Composable
fun WeatherSetupDialog(
  currentLocationName: String,
  isSearching: Boolean,
  searchResults: List<WeatherLocation>,
  errorMessage: String?,
  accent: AccentPreset,
  onDismiss: () -> Unit,
  onSearch: (String) -> Unit,
  onSelectLocation: (WeatherLocation) -> Unit,
) {
  var query by remember(currentLocationName) { mutableStateOf(currentLocationName) }
  val searchFocusRequester = remember { FocusRequester() }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    LaunchedEffect(Unit) {
      runCatching { searchFocusRequester.requestFocus() }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.74f))
        .pointerInput(Unit) { detectTapGestures { onDismiss() } },
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier
          .width(500.dp)
          .heightIn(max = 540.dp)
          .pointerInput(Unit) { detectTapGestures { /* consume */ } },
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF101828),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.16f)),
        tonalElevation = 12.dp,
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Weather Location",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
            Text(
              text = "Search for your city or town to show live weather & 1–7 day forecast under the clock.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.66f),
            )
          }

          OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("City or Town") },
            placeholder = { Text("e.g. London, New York, Manchester", color = Color.White.copy(alpha = 0.42f)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = accent.primary,
              )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = accent.primary,
              unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
              focusedContainerColor = Color.Black.copy(alpha = 0.35f),
              unfocusedContainerColor = Color.Black.copy(alpha = 0.24f),
              cursorColor = accent.primary,
              focusedLabelColor = accent.primary,
              unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
            ),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            WeatherDialogBtn(
              label = if (isSearching) "Searching…" else "Search City",
              accent = accent,
              isPrimary = true,
              isLoading = isSearching,
              onClick = { if (!isSearching) onSearch(query) },
              modifier = Modifier
                .weight(1.3f)
                .focusRequester(searchFocusRequester),
            )
            WeatherDialogBtn(
              label = "Cancel",
              accent = accent,
              isPrimary = false,
              onClick = onDismiss,
              modifier = Modifier.weight(0.9f),
            )
          }

          if (errorMessage != null) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF7F1D1D).copy(alpha = 0.55f),
              border = BorderStroke(1.dp, Color(0xFFEF4444)),
            ) {
              Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFECACA),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              )
            }
          }

          if (searchResults.isNotEmpty()) {
            Text(
              text = "SELECT YOUR LOCATION",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White.copy(alpha = 0.58f),
            )
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              searchResults.forEach { loc ->
                LocationResultRow(
                  location = loc,
                  accent = accent,
                  onClick = { onSelectLocation(loc) },
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
private fun LocationResultRow(
  location: WeatherLocation,
  accent: AccentPreset,
  onClick: () -> Unit,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = if (isFocused) accent.primary else Color.White.copy(alpha = 0.06f),
    label = "locRowBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = if (isFocused) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.LocationOn,
        contentDescription = null,
        tint = if (isFocused) accent.onPrimary else accent.primary,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = location.displayName,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun WeatherDialogBtn(
  label: String,
  accent: AccentPreset,
  isPrimary: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isLoading: Boolean = false,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      isPrimary -> accent.primary.copy(alpha = 0.30f)
      else -> Color.White.copy(alpha = 0.08f)
    },
    label = "weatherBtnBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      isPrimary -> BorderStroke(1.5.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(16.dp),
          strokeWidth = 2.dp,
          color = fg,
        )
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        maxLines = 1,
      )
    }
  }
}
