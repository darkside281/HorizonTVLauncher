package com.horizontv.launcher.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tv
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.horizontv.launcher.model.AccentPreset
import com.horizontv.launcher.model.AppInfo
import com.horizontv.launcher.model.ExternalSearchProvider
import com.horizontv.launcher.model.JellyfinSearchItem
import com.horizontv.launcher.model.SearchProviderFilter
import com.horizontv.launcher.model.StreamingOffer
import com.horizontv.launcher.model.StreamingSearchItem
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UniversalSearchDialog(
  query: String,
  selectedProvider: SearchProviderFilter,
  matchingApps: List<AppInfo>,
  isJellyfinConfigured: Boolean,
  isSearchingJellyfin: Boolean,
  jellyfinResults: List<JellyfinSearchItem>,
  jellyfinError: String?,
  isSearchingStreaming: Boolean,
  isLoadingMoreStreaming: Boolean = false,
  streamingResults: List<StreamingSearchItem>,
  streamingHasMore: Boolean = false,
  streamingError: String?,
  isServiceFilterActive: Boolean = false,
  externalSearchProviders: List<ExternalSearchProvider>,
  appsProviderEnabled: Boolean,
  jellyfinProviderEnabled: Boolean,
  streamingProviderEnabled: Boolean,
  youtubeProviderEnabled: Boolean,
  webProviderEnabled: Boolean,
  accent: AccentPreset,
  onQueryChange: (String) -> Unit,
  onSelectProvider: (SearchProviderFilter) -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  onOpenJellyfinItem: (JellyfinSearchItem) -> Unit,
  onOpenStreamingOffer: (StreamingOffer) -> Unit,
  onLoadMoreStreaming: () -> Unit = {},
  onSearchExternalProvider: (ExternalSearchProvider, String) -> Unit,
  onSearchYouTube: (String) -> Unit,
  onSearchWeb: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val inputRequester = remember { FocusRequester() }
  val closeButtonRequester = remember { FocusRequester() }
  val firstFilterRequester = remember { FocusRequester() }
  val scrollState = rememberScrollState()
  val coroutineScope = rememberCoroutineScope()
  val hostView = LocalView.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  fun findActivity(ctx: Context): Activity? {
    var current: Context? = ctx
    while (current is ContextWrapper) {
      if (current is Activity) return current
      current = current.baseContext
    }
    return null
  }

  fun hideDialogKeyboard() {
    keyboardController?.hide()
    val imm = hostView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(hostView.windowToken, 0)
    findActivity(hostView.context)?.window?.let { win ->
      WindowCompat.getInsetsController(win, hostView).hide(WindowInsetsCompat.Type.ime())
    }
    hostView.post {
      keyboardController?.hide()
      imm?.hideSoftInputFromWindow(hostView.windowToken, 0)
      findActivity(hostView.context)?.window?.let { win ->
        WindowCompat.getInsetsController(win, hostView).hide(WindowInsetsCompat.Type.ime())
      }
    }
  }

  fun dismissKeyboardAndMoveToResults() {
    focusManager.clearFocus(force = true)
    hideDialogKeyboard()
    val moved = runCatching { firstFilterRequester.requestFocus() }.isSuccess
    if (!moved) {
      focusManager.moveFocus(FocusDirection.Down)
    }
  }

  LaunchedEffect(Unit) {
    runCatching { inputRequester.requestFocus() }
  }

  DisposableEffect(Unit) {
    onDispose { hideDialogKeyboard() }
  }

  // Rendered as an in-window full-screen overlay (not a separate Android Dialog window)
  // so Android TV 11 / Nvidia Shield Gboard IME hide animations never get stuck on a Dialog leash.
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.66f))
      .pointerInput(Unit) {
        detectTapGestures {
          hideDialogKeyboard()
          onDismiss()
        }
      }
      .padding(horizontal = 36.dp, vertical = 28.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier
        .width(860.dp)
        .height(540.dp)
        .pointerInput(Unit) { detectTapGestures { /* consume */ } },
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF0E1525),
      border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.16f)),
      shadowElevation = 28.dp,
    ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // Header + Search Input
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedTextField(
              value = query,
              onValueChange = onQueryChange,
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions = KeyboardActions(
                onSearch = { dismissKeyboardAndMoveToResults() },
                onDone = { dismissKeyboardAndMoveToResults() },
                onGo = { dismissKeyboardAndMoveToResults() },
                onNext = { dismissKeyboardAndMoveToResults() },
                onSend = { dismissKeyboardAndMoveToResults() },
              ),
              modifier = Modifier
                .weight(1f)
                .focusRequester(inputRequester)
                .focusProperties {
                  up = FocusRequester.Cancel
                  left = FocusRequester.Cancel
                  down = firstFilterRequester
                  right = closeButtonRequester
                }
                .onFocusChanged { focusState ->
                  if (focusState.isFocused) {
                    coroutineScope.launch { scrollState.animateScrollTo(0) }
                  } else {
                    hideDialogKeyboard()
                  }
                }
                .onPreviewKeyEvent { keyEvent ->
                  val native = keyEvent.nativeKeyEvent
                  if (native.action == KeyEvent.ACTION_DOWN) {
                    when (native.keyCode) {
                      KeyEvent.KEYCODE_DPAD_DOWN,
                      KeyEvent.KEYCODE_ENTER,
                      KeyEvent.KEYCODE_NUMPAD_ENTER,
                      -> {
                        dismissKeyboardAndMoveToResults()
                        return@onPreviewKeyEvent true
                      }
                      KeyEvent.KEYCODE_DPAD_RIGHT -> if (query.isEmpty()) {
                        focusManager.clearFocus(force = true)
                        hideDialogKeyboard()
                        runCatching { closeButtonRequester.requestFocus() }
                        return@onPreviewKeyEvent true
                      }
                    }
                  }
                  false
                },
              placeholder = {
                Text(
                  text = if (isJellyfinConfigured) {
                    "Search apps, Jellyfin movies & TV shows, YouTube, or web…"
                  } else {
                    "Search apps, YouTube, or web…"
                  },
                  color = Color.White.copy(alpha = 0.54f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.Search,
                  contentDescription = null,
                  tint = accent.primary,
                )
              },
              trailingIcon = if (query.isNotEmpty()) {
                {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear Search",
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.clickable { onQueryChange("") },
                  )
                }
              } else {
                null
              },
              shape = RoundedCornerShape(16.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = accent.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                focusedContainerColor = Color.Black.copy(alpha = 0.36f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.26f),
                cursorColor = accent.primary,
              ),
            )

            QuickActionIcon(
              icon = Icons.Rounded.Close,
              contentDescription = "Close Search",
              accent = accent,
              onClick = onDismiss,
              modifier = Modifier
                .focusRequester(closeButtonRequester)
                .focusProperties {
                  up = FocusRequester.Cancel
                  right = FocusRequester.Cancel
                  left = inputRequester
                  down = firstFilterRequester
                },
            )
          }

          // Provider Filter Pills
          val jellyfinActive = isJellyfinConfigured && jellyfinProviderEnabled
          val effectiveProvider = when {
            selectedProvider == SearchProviderFilter.APPS && !appsProviderEnabled -> SearchProviderFilter.ALL
            selectedProvider == SearchProviderFilter.JELLYFIN && !jellyfinActive -> SearchProviderFilter.ALL
            selectedProvider == SearchProviderFilter.STREAMING && !streamingProviderEnabled -> SearchProviderFilter.ALL
            else -> selectedProvider
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val providers = buildList {
              add(SearchProviderFilter.ALL)
              if (streamingProviderEnabled) add(SearchProviderFilter.STREAMING)
              if (jellyfinActive) add(SearchProviderFilter.JELLYFIN)
              if (appsProviderEnabled) add(SearchProviderFilter.APPS)
            }
            providers.forEachIndexed { idx, provider ->
              val label = when (provider) {
                SearchProviderFilter.ALL -> "All Providers"
                SearchProviderFilter.APPS -> if (query.isNotBlank()) {
                  "Apps (${matchingApps.size})"
                } else {
                  "Apps"
                }
                SearchProviderFilter.JELLYFIN -> if (query.trim().length >= 2) {
                  "Jellyfin (${jellyfinResults.size})"
                } else {
                  "Jellyfin"
                }
                SearchProviderFilter.STREAMING -> if (query.trim().length >= 2) {
                  "Where to Watch (${streamingResults.size})"
                } else {
                  "Where to Watch"
                }
              }
              // Fixed chip widths so changing result counts never nudges neighbouring chips sideways
              val chipMinWidth = when (provider) {
                SearchProviderFilter.ALL -> 132.dp
                SearchProviderFilter.STREAMING -> 176.dp
                SearchProviderFilter.JELLYFIN -> 132.dp
                SearchProviderFilter.APPS -> 104.dp
              }
              ProviderFilterChip(
                label = label,
                selected = effectiveProvider == provider,
                accent = accent,
                onClick = { onSelectProvider(provider) },
                modifier = Modifier
                  .width(chipMinWidth)
                  .then(if (idx == 0) Modifier.focusRequester(firstFilterRequester) else Modifier)
                  .focusProperties {
                    up = inputRequester
                    if (idx == 0) {
                      left = FocusRequester.Cancel
                    }
                    if (idx == providers.lastIndex) {
                      right = closeButtonRequester
                    }
                  }
                  .onPreviewKeyEvent { keyEvent ->
                    val native = keyEvent.nativeKeyEvent
                    if (native.action == KeyEvent.ACTION_DOWN) {
                      when (native.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                          runCatching { inputRequester.requestFocus() }
                          return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> if (idx == providers.lastIndex) {
                          runCatching { closeButtonRequester.requestFocus() }
                          return@onPreviewKeyEvent true
                        }
                      }
                    }
                    false
                  }
                  .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                      coroutineScope.launch { scrollState.animateScrollTo(0) }
                    }
                  },
              )
            }
          }

          // Scrollable Results Body
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(18.dp),
          ) {
            val showApps = appsProviderEnabled &&
              (effectiveProvider == SearchProviderFilter.ALL ||
                effectiveProvider == SearchProviderFilter.APPS)
            val showJellyfin = jellyfinActive &&
              (effectiveProvider == SearchProviderFilter.ALL ||
                effectiveProvider == SearchProviderFilter.JELLYFIN)
            val showStreaming = streamingProviderEnabled &&
              (effectiveProvider == SearchProviderFilter.ALL ||
                effectiveProvider == SearchProviderFilter.STREAMING)

            val hasStreamingItems = showStreaming && query.trim().length >= 2 && streamingResults.isNotEmpty()
            val hasJellyfinItems = showJellyfin && query.trim().length >= 2 && jellyfinResults.isNotEmpty()
            val hasAppItems = showApps && query.isNotBlank() && matchingApps.isNotEmpty()

            fun moveFocusUpToFilters() {
              coroutineScope.launch { scrollState.animateScrollTo(0) }
              runCatching { firstFilterRequester.requestFocus() }
            }

            fun Modifier.topResultUpModifier(isTopRow: Boolean): Modifier = if (isTopRow) {
              this
                .focusProperties { up = firstFilterRequester }
                .onPreviewKeyEvent { keyEvent ->
                  if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                  ) {
                    moveFocusUpToFilters()
                    true
                  } else {
                    false
                  }
                }
            } else {
              this
            }

            // 1. Where to Watch (JustWatch UK) — live streaming availability across Netflix, Prime, Disney+, etc.
            if (showStreaming && query.trim().length >= 2) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SearchSectionHeader(
                  icon = Icons.Rounded.Tv,
                  title = "Where to Watch (JustWatch UK)",
                  trailing = if (isSearchingStreaming) "Searching…" else "${streamingResults.size}",
                  accent = accent,
                )

                when {
                  isSearchingStreaming && streamingResults.isEmpty() -> {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      modifier = Modifier.padding(vertical = 6.dp),
                    ) {
                      CircularProgressIndicator(
                        color = accent.primary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(18.dp),
                      )
                      Text(
                        text = "Checking Netflix, Prime Video, Disney+ & more for \"${query.trim()}\"…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                      )
                    }
                  }
                  streamingError != null -> {
                    Text(
                      text = streamingError,
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color(0xFFFCA5A5),
                    )
                  }
                  streamingResults.isEmpty() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                      Text(
                        text = if (isServiceFilterActive) {
                          "No streaming matches found on your selected services for \"${query.trim()}\"."
                        } else {
                          "No streaming matches found on JustWatch for \"${query.trim()}\"."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.60f),
                      )
                      if (streamingHasMore || isLoadingMoreStreaming) {
                        ShowMoreStreamingButton(
                          isLoading = isLoadingMoreStreaming,
                          accent = accent,
                          onClick = onLoadMoreStreaming,
                          modifier = Modifier.topResultUpModifier(true),
                        )
                      }
                    }
                  }
                  else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                      streamingResults.forEachIndexed { idx, item ->
                        key(item.id) {
                          StreamingSearchResultCard(
                            item = item,
                            accent = accent,
                            isServiceFilterActive = isServiceFilterActive,
                            onOpenOffer = onOpenStreamingOffer,
                            upTarget = if (idx == 0) firstFilterRequester else null,
                            onNavigateUp = if (idx == 0) ({ moveFocusUpToFilters() }) else null,
                          )
                        }
                      }
                      if (streamingHasMore || isLoadingMoreStreaming) {
                        ShowMoreStreamingButton(
                          isLoading = isLoadingMoreStreaming,
                          accent = accent,
                          onClick = onLoadMoreStreaming,
                        )
                      }
                    }
                  }
                }
              }
            }

            // 1. Jellyfin Movies & Shows Section (prioritized when user typed a query!)
            if (showJellyfin) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SearchSectionHeader(
                  icon = Icons.Rounded.Movie,
                  title = "Jellyfin Movies & TV Shows",
                  trailing = if (isSearchingJellyfin) "Searching…" else null,
                  accent = accent,
                )

                when {
                  query.trim().length < 2 -> {
                    Text(
                      text = "Type at least 2 letters to search your Jellyfin server and jump straight to any movie or TV show.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color.White.copy(alpha = 0.56f),
                    )
                  }
                  isSearchingJellyfin && jellyfinResults.isEmpty() -> {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      modifier = Modifier.padding(vertical = 6.dp),
                    ) {
                      CircularProgressIndicator(
                        color = accent.primary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(18.dp),
                      )
                      Text(
                        text = "Searching Jellyfin for \"${query.trim()}\"…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                      )
                    }
                  }
                  jellyfinError != null -> {
                    Text(
                      text = jellyfinError,
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color(0xFFFCA5A5),
                    )
                  }
                  jellyfinResults.isEmpty() -> {
                    Text(
                      text = "No matching movies or TV shows found in Jellyfin for \"${query.trim()}\".",
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color.White.copy(alpha = 0.60f),
                    )
                  }
                  else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                      jellyfinResults.chunked(2).forEachIndexed { rowIdx, pair ->
                        val isTopRow = !hasStreamingItems && rowIdx == 0
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                          pair.forEach { item ->
                            JellyfinSearchResultCard(
                              item = item,
                              accent = accent,
                              onClick = { onOpenJellyfinItem(item) },
                              modifier = Modifier
                                .weight(1f)
                                .topResultUpModifier(isTopRow),
                            )
                          }
                          if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            // 2. Matching Apps Section (only shown once the user types a query, or as a hint on the Apps tab)
            if (showApps && (query.isNotBlank() || effectiveProvider == SearchProviderFilter.APPS)) {
              val showEmptyAppsNotice = effectiveProvider == SearchProviderFilter.APPS
              if (query.isBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  SearchSectionHeader(
                    icon = Icons.Rounded.Apps,
                    title = "Installed Apps",
                    trailing = null,
                    accent = accent,
                  )
                  Text(
                    text = "Type a name to search your installed apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.56f),
                  )
                }
              } else if (matchingApps.isNotEmpty() || showEmptyAppsNotice) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  SearchSectionHeader(
                    icon = Icons.Rounded.Apps,
                    title = "Matching Apps",
                    trailing = "${matchingApps.size}",
                    accent = accent,
                  )

                  if (matchingApps.isEmpty()) {
                    Text(
                      text = "No installed apps match \"${query.trim()}\".",
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color.White.copy(alpha = 0.56f),
                    )
                  } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      matchingApps.chunked(3).forEachIndexed { rowIdx, rowApps ->
                        val isTopRow = !hasStreamingItems && !hasJellyfinItems && rowIdx == 0
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          rowApps.forEach { app ->
                            SearchAppChip(
                              app = app,
                              accent = accent,
                              onClick = {
                                onLaunchApp(app)
                                onDismiss()
                              },
                              modifier = Modifier
                                .weight(1f)
                                .topResultUpModifier(isTopRow),
                            )
                          }
                          repeat(3 - rowApps.size) {
                            Spacer(modifier = Modifier.weight(1f))
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            // 3. External Search Providers (Enabled Streaming Apps + YouTube & Web)
            if (query.trim().isNotEmpty()) {
              val trimmed = query.trim()
              val providerButtons: List<Triple<ImageVector, String, () -> Unit>> = buildList {
                externalSearchProviders.forEach { provider ->
                  add(
                    Triple(
                      Icons.Rounded.Tv,
                      "Search ${provider.label} for \"$trimmed\"",
                      { onSearchExternalProvider(provider, trimmed) },
                    ),
                  )
                }
                if (youtubeProviderEnabled) {
                  add(Triple(Icons.Rounded.PlayCircleOutline, "Search YouTube for \"$trimmed\"", { onSearchYouTube(trimmed) }))
                }
                if (webProviderEnabled) {
                  add(Triple(Icons.Rounded.Language, "Search Web for \"$trimmed\"", { onSearchWeb(trimmed) }))
                }
              }
              if (providerButtons.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  SearchSectionHeader(
                    icon = Icons.Rounded.Language,
                    title = "Search In Streaming Apps & Web",
                    trailing = null,
                    accent = accent,
                  )
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    providerButtons.chunked(2).forEachIndexed { rowIdx, pair ->
                      val isTopRow = !hasStreamingItems && !hasJellyfinItems && !hasAppItems && rowIdx == 0
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        pair.forEach { (icon, label, action) ->
                          ExternalProviderButton(
                            icon = icon,
                            label = label,
                            accent = accent,
                            onClick = action,
                            modifier = Modifier
                              .weight(1f)
                              .topResultUpModifier(isTopRow),
                          )
                        }
                        if (pair.size == 1) {
                          Spacer(modifier = Modifier.weight(1f))
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

@Composable
private fun SearchSectionHeader(
  icon: ImageVector,
  title: String,
  trailing: String?,
  accent: AccentPreset,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = accent.primary,
      modifier = Modifier.size(16.dp),
    )
    Text(
      text = title.uppercase(),
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
      color = Color.White.copy(alpha = 0.65f),
    )
    if (!trailing.isNullOrBlank()) {
      Text(
        text = "• $trailing",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.45f),
      )
    }
  }
}

@Composable
private fun ProviderFilterChip(
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
      else -> Color.White.copy(alpha = 0.08f)
    },
    label = "searchProviderBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(50),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      selected -> BorderStroke(1.5.dp, accent.primary)
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    },
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
      color = fg,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 7.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun JellyfinSearchResultCard(
  item: JellyfinSearchItem,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = if (isFocused) accent.primary.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.06f),
    label = "jfSearchCardBg",
  )

  Surface(
    modifier = modifier
      .height(102.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = bg,
    border = if (isFocused) {
      BorderStroke(2.dp, Color.White)
    } else {
      BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Poster thumbnail
      Box(
        modifier = Modifier
          .width(56.dp)
          .height(82.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(
            Brush.linearGradient(
              listOf(accent.primary.copy(alpha = 0.22f), Color(0xFF1E293B)),
            ),
          ),
        contentAlignment = Alignment.Center,
      ) {
        if (item.thumbnail != null) {
          Image(
            bitmap = item.thumbnail,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            imageVector = if (item.itemType.equals("Series", ignoreCase = true)) {
              Icons.Rounded.Tv
            } else {
              Icons.Rounded.Movie
            },
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.size(26.dp),
          )
        }
      }

      // Metadata
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = accent.primary.copy(alpha = 0.24f),
          ) {
            Text(
              text = item.typeLabel,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = Color.White,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          if (item.year != null) {
            Text(
              text = "${item.year}",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.68f),
            )
          }
          if (item.communityRating != null) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = String.format(Locale.US, "%.1f", item.communityRating),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f),
              )
            }
          }
        }
        Text(
          text = "Open in Jellyfin",
          style = MaterialTheme.typography.labelSmall,
          color = if (isFocused) accent.primary else Color.White.copy(alpha = 0.45f),
        )
      }
    }
  }
}

@Composable
private fun SearchAppChip(
  app: AppInfo,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = if (isFocused) accent.primary else Color.White.copy(alpha = 0.06f),
    border = if (isFocused) {
      BorderStroke(2.dp, Color.White)
    } else {
      BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    },
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      AppIconBadge(app = app, sizeDp = 32)
      Text(
        text = app.label,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = if (isFocused) accent.onPrimary else Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun ExternalProviderButton(
  icon: ImageVector,
  label: String,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val fg = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = if (isFocused) accent.primary else Color.White.copy(alpha = 0.08f),
    border = if (isFocused) {
      BorderStroke(2.dp, Color.White)
    } else {
      BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isFocused) accent.onPrimary else accent.primary,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun StreamingSearchResultCard(
  item: StreamingSearchItem,
  accent: AccentPreset,
  isServiceFilterActive: Boolean = false,
  onOpenOffer: (StreamingOffer) -> Unit,
  upTarget: FocusRequester? = null,
  onNavigateUp: (() -> Unit)? = null,
) {
  val pillUpModifier = if (upTarget != null && onNavigateUp != null) {
    Modifier
      .focusProperties { up = upTarget }
      .onPreviewKeyEvent { keyEvent ->
        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
          keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
        ) {
          onNavigateUp()
          true
        } else {
          false
        }
      }
  } else {
    Modifier
  }
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = Color.White.copy(alpha = 0.06f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Poster thumbnail
      Box(
        modifier = Modifier
          .width(56.dp)
          .height(82.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(
            Brush.linearGradient(
              listOf(accent.primary.copy(alpha = 0.30f), Color(0xFF1E293B)),
            ),
          ),
        contentAlignment = Alignment.Center,
      ) {
        if (item.poster != null) {
          Image(
            bitmap = item.poster,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            imageVector = if (item.typeLabel == "TV Show") Icons.Rounded.Tv else Icons.Rounded.Movie,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(24.dp),
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = accent.primary.copy(alpha = 0.22f),
          ) {
            Text(
              text = item.typeLabel,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = accent.primary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          if (item.year != null) {
            Text(
              text = item.year.toString(),
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.65f),
            )
          }
          if (item.imdbScore != null) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = String.format(Locale.US, "%.1f", item.imdbScore),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.80f),
              )
            }
          }
        }

        if (item.offers.isEmpty()) {
          var isEmptyPillFocused by remember { mutableStateOf(false) }
          Surface(
            modifier = pillUpModifier
              .onFocusChanged { isEmptyPillFocused = it.isFocused }
              .clickable { /* Focusable status badge for smooth D-pad scrolling */ },
            shape = RoundedCornerShape(10.dp),
            color = if (isEmptyPillFocused) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
            border = if (isEmptyPillFocused) {
              BorderStroke(1.5.dp, Color.White.copy(alpha = 0.70f))
            } else {
              BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            },
          ) {
            Text(
              text = if (isServiceFilterActive) {
                "Not currently on your selected streaming services"
              } else {
                "Not currently listed on UK streaming services"
              },
              style = MaterialTheme.typography.labelMedium,
              color = if (isEmptyPillFocused) Color.White else Color.White.copy(alpha = 0.50f),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            item.offers.take(4).forEach { offer ->
              StreamingOfferPill(
                offer = offer,
                accent = accent,
                onClick = { onOpenOffer(offer) },
                modifier = pillUpModifier,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StreamingOfferPill(
  offer: StreamingOffer,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      offer.isIncluded -> accent.primary.copy(alpha = 0.24f)
      else -> Color.White.copy(alpha = 0.08f)
    },
    label = "streamingOfferBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(10.dp),
    color = bg,
    border = when {
      isFocused -> BorderStroke(2.dp, Color.White)
      offer.isIncluded -> BorderStroke(1.dp, accent.primary.copy(alpha = 0.65f))
      else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.PlayCircleOutline,
        contentDescription = null,
        tint = if (isFocused) accent.onPrimary else if (offer.isIncluded) accent.primary else Color.White.copy(alpha = 0.72f),
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = offer.badgeLabel,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun ShowMoreStreamingButton(
  isLoading: Boolean,
  accent: AccentPreset,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFocused by remember { mutableStateOf(false) }
  val bg by animateColorAsState(
    targetValue = when {
      isFocused -> accent.primary
      else -> accent.primary.copy(alpha = 0.16f)
    },
    label = "showMoreStreamingBg",
  )
  val fg = if (isFocused) accent.onPrimary else Color.White

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .onFocusChanged { isFocused = it.isFocused }
      .clickable(enabled = !isLoading, onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = if (isFocused) {
      BorderStroke(2.dp, Color.White)
    } else {
      BorderStroke(1.dp, accent.primary.copy(alpha = 0.45f))
    },
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          color = fg,
          strokeWidth = 2.dp,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Loading more results…",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          color = fg,
        )
      } else {
        Text(
          text = "Show More Results",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          color = fg,
        )
      }
    }
  }
}
