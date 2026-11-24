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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.PinPromptMode
import com.horizontv.launcher.model.PinPromptState

@Composable
fun PinDialog(
  prompt: PinPromptState,
  currentPin: String,
  accent: AccentPreset,
  onPinVerified: () -> Unit,
  onNewPinConfirmed: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var mode by remember(prompt) { mutableStateOf(prompt.mode) }
  var firstEntry by remember(prompt) { mutableStateOf("") }
  var digits by remember(prompt) { mutableStateOf("") }
  var errorMessage by remember(prompt) { mutableStateOf<String?>(null) }
  val firstKeyRequester = remember { FocusRequester() }

  fun handleDigit(d: Char) {
    if (digits.length >= 4) return
    errorMessage = null
    val next = digits + d
    if (next.length < 4) {
      digits = next
      return
    }
    when (mode) {
      PinPromptMode.VERIFY -> {
        if (next == currentPin) {
          onPinVerified()
        } else {
          digits = ""
          errorMessage = "Incorrect PIN — try again"
        }
      }
      PinPromptMode.SET_NEW -> {
        firstEntry = next
        digits = ""
        mode = PinPromptMode.CONFIRM_NEW
      }
      PinPromptMode.CONFIRM_NEW -> {
        if (next == firstEntry) {
          onNewPinConfirmed(next)
        } else {
          firstEntry = ""
          digits = ""
          mode = PinPromptMode.SET_NEW
          errorMessage = "PINs didn't match — enter new PIN again"
        }
      }
    }
  }

  fun handleBackspace() {
    errorMessage = null
    if (digits.isNotEmpty()) {
      digits = digits.dropLast(1)
    }
  }

  fun handleClear() {
    errorMessage = null
    digits = ""
  }

  val headerTitle = when (mode) {
    PinPromptMode.CONFIRM_NEW -> "Confirm 4-Digit PIN"
    else -> prompt.title
  }
  val headerSubtitle = when (mode) {
    PinPromptMode.CONFIRM_NEW -> "Re-enter your new PIN to confirm"
    else -> prompt.subtitle
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    LaunchedEffect(prompt) {
      runCatching { firstKeyRequester.requestFocus() }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.76f))
        .pointerInput(Unit) { detectTapGestures { onDismiss() } },
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier
          .width(380.dp)
          .onPreviewKeyEvent { keyEvent ->
            val native = keyEvent.nativeKeyEvent
            if (native.action == KeyEvent.ACTION_DOWN) {
              when (native.keyCode) {
                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                  handleDigit(('0'.code + (native.keyCode - KeyEvent.KEYCODE_0)).toChar())
                  return@onPreviewKeyEvent true
                }
                in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> {
                  handleDigit(('0'.code + (native.keyCode - KeyEvent.KEYCODE_NUMPAD_0)).toChar())
                  return@onPreviewKeyEvent true
                }
                KeyEvent.KEYCODE_DEL -> {
                  handleBackspace()
                  return@onPreviewKeyEvent true
                }
              }
            }
            false
          }
          .pointerInput(Unit) { detectTapGestures { /* consume */ } },
        shape = RoundedCornerShape(26.dp),
        color = Color(0xF20D1422),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.16f)),
        tonalElevation = 16.dp,
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 26.dp, vertical = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .background(accent.primary.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = null,
              tint = accent.primary,
              modifier = Modifier.size(22.dp),
            )
          }

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = headerTitle,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
              textAlign = TextAlign.Center,
            )
            Text(
              text = headerSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.68f),
              textAlign = TextAlign.Center,
            )
          }

          // 4-dot PIN indicator
          Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            repeat(4) { idx ->
              val filled = idx < digits.length
              val dotColor by animateColorAsState(
                targetValue = when {
                  errorMessage != null -> Color(0xFFEF4444)
                  filled -> accent.primary
                  else -> Color.White.copy(alpha = 0.18f)
                },
                label = "pinDot",
              )
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .background(dotColor, CircleShape),
              )
            }
          }

          if (errorMessage != null) {
            Text(
              text = errorMessage!!,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = Color(0xFFFCA5A5),
              textAlign = TextAlign.Center,
            )
          } else {
            Spacer(modifier = Modifier.height(2.dp))
          }

          // Numeric keypad
          val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
          )
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            rows.forEachIndexed { rowIdx, rowKeys ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                rowKeys.forEachIndexed { colIdx, label ->
                  PinKeyButton(
                    label = label,
                    accent = accent,
                    onClick = { handleDigit(label[0]) },
                    modifier = Modifier
                      .weight(1f)
                      .then(
                        if (rowIdx == 0 && colIdx == 0) Modifier.focusRequester(firstKeyRequester) else Modifier,
                      ),
                  )
                }
              }
            }
            // Bottom row: Clear | 0 | Backspace
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              PinKeyButton(
                label = "Clear",
                accent = accent,
                isSecondary = true,
                onClick = ::handleClear,
                modifier = Modifier.weight(1f),
              )
              PinKeyButton(
                label = "0",
                accent = accent,
                onClick = { handleDigit('0') },
                modifier = Modifier.weight(1f),
              )
              PinIconKeyButton(
                accent = accent,
                onClick = ::handleBackspace,
                modifier = Modifier.weight(1f),
              )
            }
          }

          PinKeyButton(
            label = "Cancel",
            accent = accent,
            isSecondary = true,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

@Composable
private fun PinKeyButton(
  label: String,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isSecondary: Boolean = false,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      isSecondary -> Color.White.copy(alpha = 0.05f)
      else -> Color.White.copy(alpha = 0.09f)
    },
    label = "pinKeyBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White.copy(alpha = if (isSecondary) 0.78f else 0.96f)

  Surface(
    modifier = modifier
      .height(46.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = bg,
    border = if (isFocused) {
      BorderStroke(2.dp, Color.White)
    } else {
      BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = if (isSecondary) {
          MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        } else {
          MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        },
        color = fg,
      )
    }
  }
}

@Composable
private fun PinIconKeyButton(
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = if (isFocused) accent.primary else Color.White.copy(alpha = 0.05f),
    label = "pinDelBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White.copy(alpha = 0.78f)

  Surface(
    modifier = modifier
      .height(46.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = bg,
    border = if (isFocused) {
      BorderStroke(2.dp, Color.White)
    } else {
      BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.Backspace,
        contentDescription = "Backspace",
        tint = fg,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
