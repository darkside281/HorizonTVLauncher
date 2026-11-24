package com.horizontv.launcher.ui.components

import android.view.KeyEvent
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.AppInfo
import com.horizontv.launcher.model.AppRow

@Composable
fun AppActionDialog(
  app: AppInfo,
  allRows: List<AppRow>,
  sourceRowId: String?,
  isLayoutLocked: Boolean,
  isPinEnabled: Boolean = false,
  isAppPinLocked: Boolean = false,
  accent: AccentPreset,
  onDismiss: () -> Unit,
  onLaunch: () -> Unit,
  onToggleAppInRow: (rowId: String) -> Unit,
  onStartReorder: (rowId: String) -> Unit,
  onMoveLeft: (rowId: String) -> Unit,
  onMoveRight: (rowId: String) -> Unit,
  onRenameApp: () -> Unit,
  onPickCustomIcon: () -> Unit,
  onResetCustomIcon: () -> Unit,
  onHideApp: () -> Unit,
  onTogglePinLock: () -> Unit = {},
  onOpenAppInfo: () -> Unit,
  onUninstall: () -> Unit,
) {
  val firstFocusRequester = remember { FocusRequester() }
  // Ignore leftover key-repeat/release from the long-press OK that opened this menu so "Open App" never fires accidentally
  var sawFreshSelectDown by remember(app.packageName, sourceRowId) { mutableStateOf(false) }
  val sourceRow = remember(allRows, sourceRowId) {
    if (sourceRowId != null) allRows.firstOrNull { it.id == sourceRowId } else null
  }
  val sourceIdx = sourceRow?.packageNames?.indexOf(app.packageName) ?: -1
  val canMoveLeft = sourceRow != null && sourceIdx > 0
  val canMoveRight = sourceRow != null && sourceIdx in 0 until (sourceRow.packageNames.lastIndex)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    LaunchedEffect(app.packageName, sourceRowId) {
      runCatching { firstFocusRequester.requestFocus() }
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
          .width(440.dp)
          .onPreviewKeyEvent { keyEvent ->
            val native = keyEvent.nativeKeyEvent
            val isSelectKey = native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
              native.keyCode == KeyEvent.KEYCODE_ENTER ||
              native.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            if (!isSelectKey) return@onPreviewKeyEvent false
            when (native.action) {
              KeyEvent.ACTION_DOWN -> {
                if (native.repeatCount == 0) {
                  sawFreshSelectDown = true
                  false
                } else {
                  // Consume key-repeats carried over from the long-press that opened this dialog
                  true
                }
              }
              KeyEvent.ACTION_UP -> {
                if (sawFreshSelectDown) {
                  sawFreshSelectDown = false
                  false
                } else {
                  // Consume the release of the long-press OK that opened this dialog
                  true
                }
              }
              else -> false
            }
          }
          .pointerInput(Unit) { detectTapGestures { /* consume */ } },
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF101828),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.16f)),
        tonalElevation = 12.dp,
      ) {
        Column(
          modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Header
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            AppIconBadge(app = app, sizeDp = 52)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = app.label,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = buildString {
                  append(if (app.isTvApp) "Android TV App" else "Standard App")
                  if (sourceRow != null && sourceIdx >= 0) {
                    append(" • ${sourceRow.title} #${sourceIdx + 1}/${sourceRow.packageNames.size}")
                  } else if (app.isSystemApp) {
                    append(" • System")
                  }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.68f),
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          ActionMenuRow(
            icon = Icons.AutoMirrored.Rounded.OpenInNew,
            label = "Open App",
            accent = accent,
            onClick = {
              onDismiss()
              onLaunch()
            },
            modifier = Modifier.focusRequester(firstFocusRequester),
          )

          if (isLayoutLocked) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.White.copy(alpha = 0.06f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Home layout is locked — unlock in Customize → Home to reorder, rename, or edit rows.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.68f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              )
            }
          } else {
            if (sourceRow != null && sourceIdx >= 0) {
              ActionMenuRow(
                icon = Icons.Rounded.SwapHoriz,
                label = "Reorder in ${sourceRow.title} (◄ / ►)",
                accent = accent,
                onClick = { onStartReorder(sourceRow.id) },
              )
              if (canMoveLeft) {
                ActionMenuRow(
                  icon = Icons.AutoMirrored.Rounded.ArrowBack,
                  label = "Move Left",
                  accent = accent,
                  onClick = { onMoveLeft(sourceRow.id) },
                )
              }
              if (canMoveRight) {
                ActionMenuRow(
                  icon = Icons.AutoMirrored.Rounded.ArrowForward,
                  label = "Move Right",
                  accent = accent,
                  onClick = { onMoveRight(sourceRow.id) },
                )
              }
              ActionMenuRow(
                icon = Icons.Rounded.StarBorder,
                label = "Remove from ${sourceRow.title}",
                accent = accent,
                onClick = {
                  onToggleAppInRow(sourceRow.id)
                  onDismiss()
                },
              )
            } else {
              // Opened from All Apps: allow adding/removing from any custom row
              allRows.forEach { row ->
                val inRow = app.packageName in row.packageNames
                ActionMenuRow(
                  icon = if (inRow) Icons.Rounded.StarBorder else Icons.Rounded.Star,
                  label = if (inRow) "Remove from ${row.title}" else "Add to ${row.title}",
                  accent = accent,
                  onClick = {
                    onToggleAppInRow(row.id)
                    onDismiss()
                  },
                )
              }
            }

            ActionMenuRow(
              icon = Icons.Rounded.Edit,
              label = "Rename App",
              accent = accent,
              onClick = onRenameApp,
            )

            ActionMenuRow(
              icon = Icons.Rounded.Image,
              label = "Change Icon / Banner",
              accent = accent,
              onClick = {
                onDismiss()
                onPickCustomIcon()
              },
            )

            if (app.hasCustomIcon) {
              ActionMenuRow(
                icon = Icons.Rounded.Restore,
                label = "Reset Default Icon",
                accent = accent,
                onClick = onResetCustomIcon,
              )
            }

            ActionMenuRow(
              icon = Icons.Rounded.VisibilityOff,
              label = "Hide App",
              accent = accent,
              onClick = onHideApp,
            )
          }

          if (isPinEnabled) {
            ActionMenuRow(
              icon = if (isAppPinLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
              label = if (isAppPinLocked) "Unlock App (Remove PIN)" else "Lock App with PIN",
              accent = accent,
              onClick = onTogglePinLock,
            )
          }

          ActionMenuRow(
            icon = Icons.Rounded.Info,
            label = "App Info & Permissions",
            accent = accent,
            onClick = onOpenAppInfo,
          )

          if (!app.isSystemApp && !isLayoutLocked) {
            ActionMenuRow(
              icon = Icons.Rounded.DeleteOutline,
              label = "Uninstall App",
              accent = accent,
              isDestructive = true,
              onClick = onUninstall,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ActionMenuRow(
  icon: ImageVector,
  label: String,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isDestructive: Boolean = false,
) {
  var isFocused by remember { mutableStateOf(false) }
  val focusBg = if (isDestructive) Color(0xFFEF4444) else accent.primary
  val focusFg = if (isDestructive) Color.White else accent.onPrimary

  val bg by animateColorAsState(
    targetValue = if (isFocused) focusBg else Color.White.copy(alpha = 0.06f),
    label = "menuRowBg",
  )
  val contentColor = when {
    isFocused -> focusFg
    isDestructive -> Color(0xFFFCA5A5)
    else -> Color.White
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = if (isFocused) BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)) else null,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = contentColor,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium,
        ),
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
