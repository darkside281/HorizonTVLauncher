package com.horizontv.launcher.ui.components

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.AppUpdateInfo
import java.util.Locale

@Composable
fun AppUpdateDialog(
  currentVersionName: String,
  update: AppUpdateInfo,
  isDownloading: Boolean,
  downloadProgress: Float,
  statusText: String?,
  accent: AccentPreset,
  onUpdateNow: () -> Unit,
  onDismiss: () -> Unit,
) {
  val updateBtnRequester = remember { FocusRequester() }
  val notesScroll = rememberScrollState()

  Dialog(
    onDismissRequest = { if (!isDownloading) onDismiss() },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = !isDownloading,
      dismissOnClickOutside = !isDownloading,
    ),
  ) {
    LaunchedEffect(Unit) {
      runCatching { updateBtnRequester.requestFocus() }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.68f)),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier.width(620.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0E1525),
        border = BorderStroke(1.5.dp, accent.primary.copy(alpha = 0.45f)),
        shadowElevation = 28.dp,
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = accent.primary.copy(alpha = 0.18f),
              border = BorderStroke(1.dp, accent.primary.copy(alpha = 0.45f)),
              modifier = Modifier.size(48.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.SystemUpdateAlt,
                  contentDescription = null,
                  tint = accent.primary,
                  modifier = Modifier.size(26.dp),
                )
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Update Available",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
              )
              val sizeText = if (update.apkSizeBytes > 0L) {
                String.format(Locale.US, " • %.1f MB", update.apkSizeBytes / (1024f * 1024f))
              } else {
                ""
              }
              Text(
                text = "v$currentVersionName  →  v${update.versionName}$sizeText",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = accent.primary,
              )
            }
          }

          if (update.releaseNotes.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color.White.copy(alpha = 0.05f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier
                  .heightIn(max = 220.dp)
                  .verticalScroll(notesScroll)
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "WHAT'S NEW",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.White.copy(alpha = 0.55f),
                )
                Text(
                  text = update.releaseNotes
                    .replace(Regex("""^#+\s*""", RegexOption.MULTILINE), "")
                    .replace("**", ""),
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.86f),
                )
              }
            }
          }

          if (isDownloading) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              val pct = (downloadProgress * 100f).toInt().coerceIn(0, 100)
              Text(
                text = "Downloading ${update.apkFileName}… $pct%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.88f),
              )
              LinearProgressIndicator(
                progress = { downloadProgress.coerceIn(0f, 1f) },
                color = accent.primary,
                trackColor = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp)),
              )
            }
          } else if (!statusText.isNullOrBlank()) {
            Text(
              text = statusText,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFFCA5A5),
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            UpdateDialogButton(
              icon = Icons.Rounded.Download,
              label = if (isDownloading) "Downloading…" else "Update Now",
              accent = accent,
              primary = true,
              enabled = !isDownloading,
              onClick = onUpdateNow,
              modifier = Modifier
                .weight(1f)
                .focusRequester(updateBtnRequester),
            )
            UpdateDialogButton(
              icon = Icons.Rounded.Close,
              label = "Not Now",
              accent = accent,
              primary = false,
              enabled = !isDownloading,
              onClick = onDismiss,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun UpdateDialogButton(
  icon: ImageVector,
  label: String,
  accent: AccentPreset,
  primary: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg = when {
    !enabled -> Color.White.copy(alpha = 0.06f)
    isFocused -> accent.primary
    primary -> accent.primary.copy(alpha = 0.24f)
    else -> Color.White.copy(alpha = 0.08f)
  }
  val fg = when {
    !enabled -> Color.White.copy(alpha = 0.45f)
    isFocused -> accent.onPrimary
    else -> Color.White
  }

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(enabled = enabled, onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      primary -> BorderStroke(1.5.dp, accent.primary.copy(alpha = 0.65f))
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    },
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = fg,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
      )
    }
  }
}
