package com.horizontv.launcher.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.horizontv.launcher.model.JellyfinBackdrop
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JellyfinSearchItem
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local Folder & Web-Uploaded Photo Album wallpaper provider implementing [JellyfinDataSource].
 *
 * Combines:
 * 1) Any Android SAF folder URI chosen on the TV (zero storage permissions required), and
 * 2) Photos batch-uploaded via the Local Web Companion into `filesDir/wallpaper_album/`.
 */
class LocalFolderWallpaperClient(
  private val context: Context,
  private val photoSource: ScreensaverPhotoSource = AndroidScreensaverPhotoSource(context),
) : JellyfinDataSource {

  private val albumDir: File
    get() = File(context.filesDir, ALBUM_DIR_NAME).apply { mkdirs() }

  fun saveAlbumPhoto(bytes: ByteArray, rawFilename: String?): Boolean {
    if (bytes.isEmpty()) return false
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
    val ext = rawFilename.orEmpty().substringAfterLast('.', "jpg").lowercase()
      .takeIf { it in setOf("jpg", "jpeg", "png", "webp") } ?: "jpg"
    val safeBase = rawFilename.orEmpty().substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32)
      .ifBlank { "photo" }
    val outFile = File(albumDir, "${System.currentTimeMillis()}_${safeBase}.$ext")
    return runCatching {
      outFile.writeBytes(bytes)
      true
    }.getOrDefault(false)
  }

  fun albumPhotoCount(): Int = listAlbumFiles().size

  fun clearAlbum(): Int {
    val files = listAlbumFiles()
    files.forEach { runCatching { it.delete() } }
    return files.size
  }

  private fun listAlbumFiles(): List<File> =
    albumDir.listFiles()
      ?.filter { it.isFile && isImageFilename(it.name) }
      ?.sortedByDescending { it.lastModified() }
      ?: emptyList()

  override suspend fun authenticateAndFetch(
    rawServerUrl: String,
    apiKey: String?,
    username: String?,
    password: String?,
    mediaType: JellyfinMediaType,
    libraryIds: Set<String>,
    limit: Int,
  ): Result<JellyfinAuthResult> = withContext(Dispatchers.IO) {
    runCatching {
      val backdrops = fetchBackdropsInternal(rawServerUrl, limit)
      JellyfinAuthResult(
        serverUrl = rawServerUrl,
        token = "",
        userId = "",
        libraries = emptyList(),
        backdrops = backdrops,
      )
    }
  }

  override suspend fun fetchLibraries(
    serverUrl: String,
    token: String,
    userId: String,
  ): Result<List<JellyfinLibrary>> = Result.success(emptyList())

  override suspend fun fetchBackdrops(
    serverUrl: String,
    token: String,
    userId: String,
    mediaType: JellyfinMediaType,
    libraryIds: Set<String>,
    limit: Int,
  ): Result<List<JellyfinBackdrop>> = withContext(Dispatchers.IO) {
    runCatching { fetchBackdropsInternal(serverUrl, limit) }
  }

  private suspend fun fetchBackdropsInternal(folderUri: String, limit: Int): List<JellyfinBackdrop> {
    val all = mutableListOf<JellyfinBackdrop>()

    // 1. Built-in Web Companion uploaded album photos
    listAlbumFiles().forEach { file ->
      val title = file.nameWithoutExtension.substringAfter('_').replace('_', ' ').ifBlank { file.nameWithoutExtension }
      all.add(
        JellyfinBackdrop(
          itemId = "album:${file.name}",
          title = title,
          year = null,
          imageUrl = "$FILE_PREFIX${file.absolutePath}",
          logoUrl = null,
        ),
      )
    }

    // 2. SAF Local Folder photos (if picked on TV)
    if (folderUri.isNotBlank()) {
      photoSource.listPhotos(folderUri = folderUri, photoUris = emptyList()).forEach { entry ->
        all.add(
          JellyfinBackdrop(
            itemId = "saf:${entry.uri.hashCode()}",
            title = entry.title,
            year = null,
            imageUrl = entry.uri,
            logoUrl = null,
          ),
        )
      }
    }

    return all.shuffled().take(limit.coerceIn(10, 500))
  }

  override suspend fun downloadBackdropImage(
    imageUrl: String,
    token: String,
  ): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
      if (imageUrl.startsWith(FILE_PREFIX)) {
        val file = File(imageUrl.removePrefix(FILE_PREFIX))
        if (!file.exists()) return@runCatching null
        decodeSampledFile(file, maxWidth = 1920, maxHeight = 1080)
      } else {
        decodeSampledUri(Uri.parse(imageUrl), maxWidth = 1920, maxHeight = 1080)
      }
    }.getOrNull()
  }

  override suspend fun downloadLogoImage(logoUrl: String, token: String): ImageBitmap? = null

  override suspend fun downloadThumbnail(imageUrl: String, token: String): ImageBitmap? = null

  override suspend fun searchItems(
    serverUrl: String,
    token: String,
    userId: String,
    query: String,
    limit: Int,
  ): Result<List<JellyfinSearchItem>> = Result.success(emptyList())

  private fun decodeSampledFile(file: File, maxWidth: Int, maxHeight: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxWidth * 2 || bounds.outHeight / sampleSize > maxHeight * 2) {
      sampleSize *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
  }

  private fun decodeSampledUri(uri: Uri, maxWidth: Int, maxHeight: Int): ImageBitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxWidth * 2 || bounds.outHeight / sampleSize > maxHeight * 2) {
      sampleSize *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
  }

  companion object {
    private const val ALBUM_DIR_NAME = "wallpaper_album"
    private const val FILE_PREFIX = "localfile://"

    private fun isImageFilename(name: String): Boolean {
      val lower = name.lowercase()
      return lower.endsWith(".jpg") ||
        lower.endsWith(".jpeg") ||
        lower.endsWith(".png") ||
        lower.endsWith(".webp") ||
        lower.endsWith(".bmp")
    }
  }
}
