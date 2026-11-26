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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.horizontv.launcher.data.DiscoveredSmbServer
import com.horizontv.launcher.data.SmbBrowseResult
import com.horizontv.launcher.data.SmbWallpaperClient
import kotlinx.coroutines.launch
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
            if (isSmb) {
              val scanScope = rememberCoroutineScope()
              var isScanningSmb by remember { mutableStateOf(false) }
              var isBrowsingSmb by remember { mutableStateOf(false) }
              var discoveredServers by remember { mutableStateOf<List<DiscoveredSmbServer>>(emptyList()) }
              var browseResult by remember { mutableStateOf<SmbBrowseResult?>(null) }
              var scanStatusNote by remember { mutableStateOf<String?>(null) }

              fun triggerBrowse(host: String, share: String?, subPath: String) {
                if (isBrowsingSmb) return
                isBrowsingSmb = true
                scanStatusNote = "Browsing smb://$host${if (!share.isNullOrBlank()) "/$share" else ""}${if (subPath.isNotBlank()) "/$subPath" else ""}…"
                scanScope.launch {
                  val res = if (share.isNullOrBlank()) {
                    SmbWallpaperClient.listServerShares(host, username, password)
                  } else {
                    SmbWallpaperClient.listShareFolders(host, share, subPath, username, password)
                  }
                  res.fold(
                    onSuccess = { b ->
                      browseResult = b
                      serverUrl = b.currentPath
                      scanStatusNote = if (b.shareName == null) {
                        if (b.folders.isEmpty()) "No public shares listed — enter credentials below & tap Browse" else "Select a share:"
                      } else {
                        "${b.imageCount} image${if (b.imageCount == 1) "" else "s"} here — select subfolder or tap Connect"
                      }
                    },
                    onFailure = { err ->
                      scanStatusNote = err.message ?: "Could not browse server (enter credentials below if private)"
                    },
                  )
                  isBrowsingSmb = false
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                AuthModePill(
                  label = if (isScanningSmb) "Scanning LAN…" else "Scan LAN",
                  selected = isScanningSmb,
                  accent = accent,
                  onClick = {
                    if (!isScanningSmb) {
                      isScanningSmb = true
                      scanStatusNote = "Scanning local network (port 445)…"
                      scanScope.launch {
                        val found = SmbWallpaperClient.scanLanSmbServers()
                        discoveredServers = found
                        scanStatusNote = if (found.isEmpty()) {
                          "No SMB servers found on this subnet"
                        } else {
                          "Found ${found.size} server${if (found.size == 1) "" else "s"} — select one to browse shares:"
                        }
                        isScanningSmb = false
                      }
                    }
                  },
                )
                AuthModePill(
                  label = if (isBrowsingSmb) "Browsing…" else "Browse Shares",
                  selected = isBrowsingSmb,
                  accent = accent,
                  onClick = {
                    val cleaned = serverUrl.trim().replace('\\', '/').removePrefix("smb://").removePrefix("SMB://").trimStart('/')
                    val segs = cleaned.split('/').filter { it.isNotBlank() }
                    if (segs.isNotEmpty()) {
                      triggerBrowse(segs[0], segs.getOrNull(1), segs.drop(2).joinToString("/"))
                    } else {
                      scanStatusNote = "Scan LAN or enter an SMB server IP first"
                    }
                  },
                )
                if (!scanStatusNote.isNullOrBlank()) {
                  Text(
                    text = scanStatusNote!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f),
                  )
                }
              }

              if (discoveredServers.isNotEmpty()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  discoveredServers.take(4).forEach { srv ->
                    val label = if (!srv.hostName.isNullOrBlank()) "${srv.hostName} (${srv.ip})" else srv.ip
                    AuthModePill(
                      label = label,
                      selected = serverUrl.contains(srv.ip),
                      accent = accent,
                      onClick = {
                        serverUrl = "smb://${srv.ip}/"
                        triggerBrowse(srv.ip, null, "")
                      },
                    )
                  }
                }
              }

              val currentBrowse = browseResult
              if (currentBrowse != null) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Color.White.copy(alpha = 0.05f),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      if (!currentBrowse.shareName.isNullOrBlank()) {
                        AuthModePill(
                          label = "⬅ Up",
                          selected = false,
                          accent = accent,
                          onClick = {
                            if (currentBrowse.subPath.isBlank()) {
                              triggerBrowse(currentBrowse.host, null, "")
                            } else {
                              val parts = currentBrowse.subPath.split('/').filter { it.isNotBlank() }.dropLast(1)
                              triggerBrowse(currentBrowse.host, currentBrowse.shareName, parts.joinToString("/"))
                            }
                          },
                        )
                      }
                      Text(
                        text = currentBrowse.currentPath,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent.primary,
                        modifier = Modifier.weight(1f),
                      )
                    }
                    if (currentBrowse.folders.isNotEmpty()) {
                      currentBrowse.folders.chunked(3).take(4).forEach { rowItems ->
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          rowItems.forEach { folder ->
                            AuthModePill(
                              label = "📁 $folder",
                              selected = false,
                              accent = accent,
                              onClick = {
                                if (currentBrowse.shareName.isNullOrBlank()) {
                                  triggerBrowse(currentBrowse.host, folder, "")
                                } else {
                                  val next = if (currentBrowse.subPath.isBlank()) folder else "${currentBrowse.subPath}/$folder"
                                  triggerBrowse(currentBrowse.host, currentBrowse.shareName, next)
                                }
                              },
                              modifier = Modifier.weight(1f),
                            )
                          }
                          repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            TvClickToEditField(
              label = "${provider.label} Server URL",
              value = serverUrl,
              placeholder = provider.defaultPortHint,
              accent = accent,
              fieldColors = fieldColors,
              onValueChange = { serverUrl = it },
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
            TvClickToEditField(
              label = provider.tokenLabel,
              value = apiKey,
              placeholder = when (provider) {
                MediaWallpaperProvider.PLEX -> "Your X-Plex-Token value"
                MediaWallpaperProvider.TMDB -> "Free key from themoviedb.org/settings/api"
                else -> "Dashboard → API Keys → +"
              },
              accent = accent,
              fieldColors = fieldColors,
              onValueChange = { apiKey = it },
            )
          } else {
            TvClickToEditField(
              label = provider.usernameLabel,
              value = username,
              placeholder = if (isSmb) "Leave blank for Guest" else "Username",
              accent = accent,
              fieldColors = fieldColors,
              onValueChange = { username = it },
            )
            TvClickToEditField(
              label = "Password",
              value = password,
              placeholder = if (isSmb) "Leave blank for Guest" else "Password",
              accent = accent,
              fieldColors = fieldColors,
              isPassword = true,
              onValueChange = { password = it },
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
private fun TvClickToEditField(
  label: String,
  value: String,
  placeholder: String,
  accent: AccentPreset,
  fieldColors: androidx.compose.material3.TextFieldColors,
  onValueChange: (String) -> Unit,
  isPassword: Boolean = false,
) {
  var isEditing by remember { mutableStateOf(false) }
  var isCardFocused by remember { mutableStateOf(false) }
  val editFocusRequester = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current

  if (isEditing) {
    LaunchedEffect(Unit) {
      runCatching { editFocusRequester.requestFocus() }
      keyboard?.show()
    }
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier
        .fillMaxWidth()
        .focusRequester(editFocusRequester)
        .onFocusChanged { state ->
          if (!state.isFocused && isEditing) {
            isEditing = false
            keyboard?.hide()
          }
        },
      singleLine = true,
      label = { Text(label) },
      placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.42f)) },
      visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(
        onDone = {
          isEditing = false
          keyboard?.hide()
        },
      ),
      shape = RoundedCornerShape(14.dp),
      colors = fieldColors,
    )
  } else {
    val bg by animateColorAsState(
      targetValue = if (isCardFocused) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.28f),
      label = "tvFieldBg",
    )
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { isCardFocused = it.isFocused }
        .clickable { isEditing = true },
      shape = RoundedCornerShape(14.dp),
      color = bg,
      border = if (isCardFocused) BorderStroke(2.dp, accent.primary) else BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCardFocused) accent.primary else Color.White.copy(alpha = 0.62f),
          )
          val displayText = when {
            value.isBlank() -> placeholder
            isPassword -> "•".repeat(value.length.coerceAtMost(16))
            else -> value
          }
          Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (value.isBlank()) Color.White.copy(alpha = 0.42f) else Color.White,
            maxLines = 1,
          )
        }
        if (isCardFocused) {
          Text(
            text = "OK to edit",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent.primary,
          )
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
