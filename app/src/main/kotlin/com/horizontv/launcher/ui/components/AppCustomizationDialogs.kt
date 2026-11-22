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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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

@Composable
fun RenameTextDialog(
  title: String,
  subtitle: String,
  initialValue: String,
  placeholder: String,
  resetLabel: String,
  accent: AccentPreset,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
  onReset: () -> Unit,
) {
  var textValue by remember(initialValue) { mutableStateOf(initialValue) }
  val saveFocusRequester = remember { FocusRequester() }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    LaunchedEffect(Unit) {
      runCatching { saveFocusRequester.requestFocus() }
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
          .width(460.dp)
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
              text = title,
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.65f),
            )
          }

          OutlinedTextField(
            value = textValue,
            onValueChange = { textValue = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
              Text(text = placeholder, color = Color.White.copy(alpha = 0.45f))
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = accent.primary,
              unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
              focusedContainerColor = Color.Black.copy(alpha = 0.35f),
              unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
              cursorColor = accent.primary,
            ),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DialogActionButton(
              label = "Save",
              accent = accent,
              isPrimary = true,
              onClick = { onSave(textValue) },
              modifier = Modifier
                .weight(1f)
                .focusRequester(saveFocusRequester),
            )
            DialogActionButton(
              label = resetLabel,
              accent = accent,
              isPrimary = false,
              onClick = onReset,
              modifier = Modifier.weight(1f),
            )
            DialogActionButton(
              label = "Cancel",
              accent = accent,
              isPrimary = false,
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
fun AddFavoritePickerDialog(
  rowTitle: String,
  allApps: List<AppInfo>,
  favoritePackages: Set<String>,
  accent: AccentPreset,
  onToggleFavorite: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val closeFocusRequester = remember { FocusRequester() }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    LaunchedEffect(Unit) {
      runCatching { closeFocusRequester.requestFocus() }
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
          .width(500.dp)
          .heightIn(max = 520.dp)
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
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Add Apps to $rowTitle",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Select any app to pin or unpin it from your dock",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
              )
            }
            QuickActionIcon(
              icon = Icons.Rounded.Close,
              contentDescription = "Done",
              accent = accent,
              onClick = onDismiss,
              modifier = Modifier.focusRequester(closeFocusRequester),
            )
          }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            allApps.forEach { app ->
              val isPinned = app.packageName in favoritePackages
              PickerAppRow(
                app = app,
                isPinned = isPinned,
                accent = accent,
                onClick = { onToggleFavorite(app.packageName) },
              )
            }
          }

          Spacer(modifier = Modifier.height(2.dp))
          DialogActionButton(
            label = "Done",
            accent = accent,
            isPrimary = true,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

@Composable
fun TileOrderDialog(
  rowTitle: String,
  orderedApps: List<AppInfo>,
  accent: AccentPreset,
  onMoveApp: (packageName: String, delta: Int) -> Unit,
  onSortAlphabetically: () -> Unit,
  onDismiss: () -> Unit,
) {
  var grabbedPackage by remember { mutableStateOf<String?>(null) }
  val itemFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
  val doneFocusRequester = remember { FocusRequester() }
  val scrollState = rememberScrollState()

  // Focus first item on open
  LaunchedEffect(Unit) {
    val firstPkg = orderedApps.firstOrNull()?.packageName
    val target = if (firstPkg != null) {
      itemFocusRequesters.getOrPut(firstPkg) { FocusRequester() }
    } else {
      doneFocusRequester
    }
    withFrameNanos { }
    runCatching { target.requestFocus() }
  }

  // Keep focus glued to the grabbed tile as its index changes
  LaunchedEffect(grabbedPackage, orderedApps) {
    val pkg = grabbedPackage ?: return@LaunchedEffect
    withFrameNanos { }
    runCatching { itemFocusRequesters[pkg]?.requestFocus() }
  }

  Dialog(
    onDismissRequest = {
      if (grabbedPackage != null) {
        grabbedPackage = null
      } else {
        onDismiss()
      }
    },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.64f))
        .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        .onPreviewKeyEvent { keyEvent ->
          val pkg = grabbedPackage ?: return@onPreviewKeyEvent false
          val native = keyEvent.nativeKeyEvent
          if (native.action == KeyEvent.ACTION_DOWN) {
            when (native.keyCode) {
              KeyEvent.KEYCODE_DPAD_UP -> {
                onMoveApp(pkg, -1)
                return@onPreviewKeyEvent true
              }
              KeyEvent.KEYCODE_DPAD_DOWN -> {
                onMoveApp(pkg, +1)
                return@onPreviewKeyEvent true
              }
              KeyEvent.KEYCODE_DPAD_LEFT,
              KeyEvent.KEYCODE_DPAD_RIGHT,
              -> return@onPreviewKeyEvent true
              KeyEvent.KEYCODE_DPAD_CENTER,
              KeyEvent.KEYCODE_ENTER,
              KeyEvent.KEYCODE_NUMPAD_ENTER,
              KeyEvent.KEYCODE_BACK,
              -> {
                grabbedPackage = null
                return@onPreviewKeyEvent true
              }
            }
          }
          false
        },
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier
          .width(560.dp)
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
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Tile Order — $rowTitle",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = if (grabbedPackage != null) {
                  "Moving tile — press Up / Down to reposition, OK or Back when done"
                } else {
                  "Press OK on an app to grab & move with Up/Down, or use ↑ / ↓"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (grabbedPackage != null) accent.primary else Color.White.copy(alpha = 0.65f),
              )
            }
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (orderedApps.size >= 2) {
                QuickActionIcon(
                  icon = Icons.Rounded.SortByAlpha,
                  contentDescription = "Sort A–Z",
                  accent = accent,
                  onClick = {
                    grabbedPackage = null
                    onSortAlphabetically()
                  },
                )
              }
              QuickActionIcon(
                icon = Icons.Rounded.Close,
                contentDescription = "Done",
                accent = accent,
                onClick = onDismiss,
              )
            }
          }

          if (orderedApps.isEmpty()) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color.White.copy(alpha = 0.05f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "No apps in this row yet. Use + to add apps first.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.68f),
                modifier = Modifier.padding(16.dp),
              )
            }
          } else {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(scrollState),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              orderedApps.forEachIndexed { index, app ->
                val isGrabbed = grabbedPackage == app.packageName
                val requester = itemFocusRequesters.getOrPut(app.packageName) { FocusRequester() }
                TileOrderRow(
                  position = index + 1,
                  app = app,
                  isFirst = index == 0,
                  isLast = index == orderedApps.lastIndex,
                  isGrabbed = isGrabbed,
                  accent = accent,
                  focusRequester = requester,
                  onToggleGrab = {
                    grabbedPackage = if (isGrabbed) null else app.packageName
                  },
                  onMoveUp = { onMoveApp(app.packageName, -1) },
                  onMoveDown = { onMoveApp(app.packageName, +1) },
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))
          DialogActionButton(
            label = "Done",
            accent = accent,
            isPrimary = true,
            onClick = onDismiss,
            modifier = Modifier
              .fillMaxWidth()
              .focusRequester(doneFocusRequester),
          )
        }
      }
    }
  }
}

@Composable
private fun TileOrderRow(
  position: Int,
  app: AppInfo,
  isFirst: Boolean,
  isLast: Boolean,
  isGrabbed: Boolean,
  accent: AccentPreset,
  focusRequester: FocusRequester,
  onToggleGrab: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isGrabbed -> accent.primary.copy(alpha = 0.30f)
      isFocused -> accent.primary
      else -> Color.White.copy(alpha = 0.06f)
    },
    label = "tileOrderRowBg",
  )
  val fg = if (isFocused && !isGrabbed) accent.onPrimary else Color.White

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Surface(
      modifier = Modifier
        .weight(1f)
        .focusRequester(focusRequester)
        .onFocusChanged { isFocused = it.isFocused }
        .clickable(onClick = onToggleGrab),
      shape = RoundedCornerShape(14.dp),
      color = bg,
      border = when {
        isGrabbed -> BorderStroke(2.dp, accent.primary)
        isFocused -> BorderStroke(2.dp, Color.White)
        else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
      },
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isFocused && !isGrabbed) {
            accent.onPrimary.copy(alpha = 0.18f)
          } else {
            Color.Black.copy(alpha = 0.35f)
          },
        ) {
          Text(
            text = "#$position",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isGrabbed) accent.primary else fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }

        AppIconBadge(app = app, sizeDp = 36)

        Text(
          text = app.label,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
          color = fg,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )

        if (isGrabbed) {
          Surface(
            shape = RoundedCornerShape(50),
            color = accent.primary,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.SwapVert,
                contentDescription = null,
                tint = accent.onPrimary,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "MOVE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent.onPrimary,
              )
            }
          }
        }
      }
    }

    if (!isFirst) {
      TileOrderArrowButton(
        icon = Icons.Rounded.ArrowUpward,
        contentDescription = "Move ${app.label} Up",
        accent = accent,
        onClick = onMoveUp,
      )
    } else {
      Spacer(modifier = Modifier.size(38.dp))
    }

    if (!isLast) {
      TileOrderArrowButton(
        icon = Icons.Rounded.ArrowDownward,
        contentDescription = "Move ${app.label} Down",
        accent = accent,
        onClick = onMoveDown,
      )
    } else {
      Spacer(modifier = Modifier.size(38.dp))
    }
  }
}

@Composable
private fun TileOrderArrowButton(
  icon: ImageVector,
  contentDescription: String,
  accent: AccentPreset,
  onClick: () -> Unit,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = if (isFocused) accent.primary else Color.White.copy(alpha = 0.08f),
    label = "tileOrderArrowBg",
  )
  Surface(
    modifier = Modifier
      .size(38.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(10.dp),
    color = bg,
    border = if (isFocused) {
      BorderStroke(2.dp, Color.White)
    } else {
      BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    },
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (isFocused) accent.onPrimary else Color.White,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun PickerAppRow(
  app: AppInfo,
  isPinned: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      isPinned -> accent.primary.copy(alpha = 0.20f)
      else -> Color.White.copy(alpha = 0.06f)
    },
    label = "pickerRowBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      isPinned -> BorderStroke(1.dp, accent.primary.copy(alpha = 0.65f))
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    },
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AppIconBadge(app = app, sizeDp = 38)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = app.label,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
          color = fg,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (app.isTvApp) "Android TV App" else "Standard App",
          style = MaterialTheme.typography.labelMedium,
          color = fg.copy(alpha = 0.72f),
        )
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = if (isPinned) Icons.Rounded.Check else Icons.Rounded.Add,
          contentDescription = if (isPinned) "Pinned" else "Add",
          tint = if (isFocused) accent.onPrimary else if (isPinned) accent.primary else Color.White.copy(alpha = 0.75f),
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = if (isPinned) "Added" else "Add",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = if (isFocused) accent.onPrimary else if (isPinned) accent.primary else Color.White.copy(alpha = 0.8f),
        )
      }
    }
  }
}

@Composable
private fun DialogActionButton(
  label: String,
  accent: AccentPreset,
  isPrimary: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      isPrimary -> accent.primary.copy(alpha = 0.28f)
      else -> Color.White.copy(alpha = 0.08f)
    },
    label = "dialogBtnBg",
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
    Box(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        maxLines = 1,
      )
    }
  }
}
