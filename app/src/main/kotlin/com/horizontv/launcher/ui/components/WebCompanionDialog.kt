package com.horizontv.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.WebCompanionInfo

@Composable
fun WebCompanionDialog(
  info: WebCompanionInfo,
  accent: AccentPreset,
  onStopAndClose: () -> Unit,
) {
  val closeRequester = remember { FocusRequester() }
  val qrBitmap = remember(info.qrUrl) { generateQrImageBitmap(info.qrUrl, 360) }

  LaunchedEffect(Unit) {
    runCatching { closeRequester.requestFocus() }
  }

  Dialog(
    onDismissRequest = onStopAndClose,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = false,
    ),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.78f))
        .padding(horizontal = 48.dp, vertical = 30.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier.width(880.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xF60C1322),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.18f)),
        shadowElevation = 28.dp,
      ) {
        Column(
          modifier = Modifier.padding(28.dp),
          verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
          // Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .background(accent.primary.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Devices,
                  contentDescription = null,
                  tint = accent.primary,
                  modifier = Modifier.size(24.dp),
                )
              }
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "Local Web Companion",
                  style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                )
                Text(
                  text = "Edit settings, paste API keys & upload icons from your phone or PC over Wi-Fi",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.65f),
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(50),
              color = Color(0xFF10B981).copy(alpha = 0.16f),
              border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF10B981), CircleShape),
                )
                Text(
                  text = "Server Running",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF6EE7B7),
                )
              }
            }
          }

          // Main Content: QR Code Left + URL/Session Code Right
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = Color.White.copy(alpha = 0.06f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
              modifier = Modifier.width(248.dp),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(204.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(8.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  if (qrBitmap != null) {
                    Image(
                      bitmap = qrBitmap,
                      contentDescription = "QR Code for ${info.url}",
                      contentScale = ContentScale.Fit,
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                }
                Text(
                  text = "Scan with Phone Camera",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Color.White.copy(alpha = 0.78f),
                )
              }
            }

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "1. OPEN THIS ADDRESS ON YOUR PHONE OR PC",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = accent.primary,
                )
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = accent.primary.copy(alpha = 0.14f),
                  border = BorderStroke(1.5.dp, accent.primary.copy(alpha = 0.55f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = info.url,
                    style = MaterialTheme.typography.titleLarge.copy(
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                  )
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "2. SESSION CODE (AUTO-FILLED BY QR)",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = accent.primary,
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                  ) {
                    Text(
                      text = info.sessionCode,
                      style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                      ),
                      color = accent.primary,
                      modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    )
                  }
                  Text(
                    text = "Local Wi-Fi only — zero cloud servers.\nStops automatically when you close this screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f),
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = if (!info.lastActivity.isNullOrBlank()) {
                      "Latest activity: ${info.lastActivity}"
                    } else {
                      "Quick tasks from your browser:"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (!info.lastActivity.isNullOrBlank()) Color(0xFF6EE7B7) else Color.White,
                  )
                  Text(
                    text = "•  Paste Jellyfin, Plex, Emby & TMDB URLs / API keys\n•  Rename & reorder Home rows  •  Upload custom icons & wallpaper  •  Backup & Restore",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f),
                  )
                }
              }
            }
          }

          // Footer: Stop Server & Close
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            var isCloseFocused by remember { mutableStateOf(false) }
            val btnBg by animateColorAsState(
              targetValue = if (isCloseFocused) Color.White else accent.primary,
              label = "webCompCloseBg",
            )
            Surface(
              modifier = Modifier
                .focusRequester(closeRequester)
                .onFocusChanged { isCloseFocused = it.isFocused }
                .clickable(onClick = onStopAndClose),
              shape = RoundedCornerShape(50),
              color = btnBg,
              border = if (isCloseFocused) BorderStroke(2.dp, accent.primary) else null,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = null,
                  tint = Color(0xFF04121F),
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Stop Server & Close",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF04121F),
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun generateQrImageBitmap(content: String, sizePx: Int): ImageBitmap? = runCatching {
  val hints = mapOf(EncodeHintType.MARGIN to 1)
  val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
  val pixels = IntArray(sizePx * sizePx)
  for (y in 0 until sizePx) {
    val offset = y * sizePx
    for (x in 0 until sizePx) {
      pixels[offset + x] = if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
    }
  }
  val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  bmp.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
  bmp.asImageBitmap()
}.getOrNull()
