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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.MediaWallpaperProvider

@Composable
fun JellyfinSetupDialog(
  provider: MediaWallpaperProvider,
  initialServerUrl: String,
  initialApiKey: String,
  isTesting: Boolean,
  errorMessage: String?,
  isConfigured: Boolean,
  accent: AccentPreset,
  onDismiss: () -> Unit,
  onConnect: (serverUrl: String, apiKey: String?, username: String?, password: String?) -> Unit,
  onDisconnect: () -> Unit,
) {
  val isSmb = provider == MediaWallpaperProvider.SMB
  var serverUrl by remember(provider, initialServerUrl) {
    mutableStateOf(
      initialServerUrl.ifBlank { if (isSmb) "smb://" else "http://" },
    )
  }
  var useApiKey by remember(provider, initialApiKey) { mutableStateOf(!isSmb) }
  var apiKey by remember(provider, initialApiKey) { mutableStateOf(if (isSmb) "" else initialApiKey) }
  var username by remember(provider, initialApiKey) {
    mutableStateOf(if (isSmb && initialApiKey.contains('\n')) initialApiKey.substringBefore('\n') else "")
  }
  var password by remember(provider, initialApiKey) {
    mutableStateOf(if (isSmb && initialApiKey.contains('\n')) initialApiKey.substringAfter('\n') else "")
  }

  val connectFocusRequester = remember { FocusRequester() }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    LaunchedEffect(Unit) {
      runCatching { connectFocusRequester.requestFocus() }
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
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "${provider.label} Wallpaper Slideshow",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
            Text(
              text = when (provider) {
                MediaWallpaperProvider.TMDB ->
                  "Pulls 1080p/4K movie & TV backdrops and official ClearLogos from The Movie Database (themoviedb.org)."
                MediaWallpaperProvider.SMB ->
                  "Connect to a Windows, NAS, or Samba shared folder over Wi-Fi/LAN (e.g. smb://192.168.0.50/Wallpapers). Leave username & password blank for Guest access."
                else ->
                  "Pulls 1080p movie & TV backdrops and logos from your ${provider.label} server for a smooth crossfading home wallpaper."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.66f),
            )
          }

          val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = accent.primary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
            focusedContainerColor = Color.Black.copy(alpha = 0.35f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.24f),
            cursorColor = accent.primary,
            focusedLabelColor = accent.primary,
            unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
          )

          if (provider.requiresServerUrl) {
            OutlinedTextField(
              value = serverUrl,
              onValueChange = { serverUrl = it },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              label = { Text("${provider.label} Server URL") },
              placeholder = { Text(provider.defaultPortHint, color = Color.White.copy(alpha = 0.42f)) },
              shape = RoundedCornerShape(14.dp),
              colors = fieldColors,
            )

            // Auth mode switch: Token/API Key vs Username + Password (hidden for SMB)
            if (!isSmb) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                AuthModePill(
                  label = if (provider == MediaWallpaperProvider.PLEX) "Plex Token" else "API Key",
                  selected = useApiKey,
                  accent = accent,
                  onClick = { useApiKey = true },
                  modifier = Modifier.weight(1f),
                )
                AuthModePill(
                  label = if (provider == MediaWallpaperProvider.PLEX) "Plex Account Sign-In" else "Username & Password",
                  selected = !useApiKey,
                  accent = accent,
                  onClick = { useApiKey = false },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }

          if (useApiKey || !provider.requiresServerUrl) {
            OutlinedTextField(
              value = apiKey,
              onValueChange = { apiKey = it },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              label = { Text(provider.tokenLabel) },
              placeholder = {
                Text(
                  text = when (provider) {
                    MediaWallpaperProvider.PLEX -> "Your X-Plex-Token value"
                    MediaWallpaperProvider.TMDB -> "Free key from themoviedb.org/settings/api"
                    else -> "Dashboard → API Keys → +"
                  },
                  color = Color.White.copy(alpha = 0.42f),
                )
              },
              shape = RoundedCornerShape(14.dp),
              colors = fieldColors,
            )
          } else {
            OutlinedTextField(
              value = username,
              onValueChange = { username = it },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              label = { Text(provider.usernameLabel) },
              shape = RoundedCornerShape(14.dp),
              colors = fieldColors,
            )
            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              label = { Text("Password") },
              visualTransformation = PasswordVisualTransformation(),
              shape = RoundedCornerShape(14.dp),
              colors = fieldColors,
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

          Spacer(modifier = Modifier.height(2.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            JellyfinDialogButton(
              label = if (isTesting) "Connecting…" else "Connect & Start",
              accent = accent,
              isPrimary = true,
              isLoading = isTesting,
              onClick = {
                if (!isTesting) {
                  if (useApiKey) {
                    onConnect(serverUrl, apiKey, null, null)
                  } else {
                    onConnect(serverUrl, null, username, password)
                  }
                }
              },
              modifier = Modifier
                .weight(1.4f)
                .focusRequester(connectFocusRequester),
            )

            if (isConfigured) {
              JellyfinDialogButton(
                label = "Disconnect",
                accent = accent,
                isPrimary = false,
                isDestructive = true,
                onClick = onDisconnect,
                modifier = Modifier.weight(1f),
              )
            }

            JellyfinDialogButton(
              label = "Cancel",
              accent = accent,
              isPrimary = false,
              onClick = onDismiss,
              modifier = Modifier.weight(0.9f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AuthModePill(
  label: String,
  selected: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      selected -> accent.primary.copy(alpha = 0.28f)
      else -> Color.White.copy(alpha = 0.07f)
    },
    label = "authPillBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      selected -> BorderStroke(1.5.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun JellyfinDialogButton(
  label: String,
  accent: AccentPreset,
  isPrimary: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isDestructive: Boolean = false,
  isLoading: Boolean = false,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused && isDestructive -> Color(0xFFEF4444)
      isFocused -> accent.primary
      isPrimary -> accent.primary.copy(alpha = 0.30f)
      else -> Color.White.copy(alpha = 0.08f)
    },
    label = "jfBtnBg",
  )
  val fg = when {
    isFocused && isDestructive -> Color.White
    isFocused -> accent.onPrimary
    isDestructive -> Color(0xFFFCA5A5)
    else -> Color.White
  }

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
