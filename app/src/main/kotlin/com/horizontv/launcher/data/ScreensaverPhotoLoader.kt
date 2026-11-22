package com.horizontv.launcher.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.horizontv.launcher.model.ScreensaverPhotoSlide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhotoEntry(
  val uri: String,
  val title: String,
)

interface ScreensaverPhotoSource {
  suspend fun listPhotos(folderUri: String?, photoUris: List<String>): List<PhotoEntry>
  suspend fun loadPhoto(entry: PhotoEntry): ScreensaverPhotoSlide?
}

class AndroidScreensaverPhotoSource(
  private val context: Context,
) : ScreensaverPhotoSource {

  override suspend fun listPhotos(folderUri: String?, photoUris: List<String>): List<PhotoEntry> =
    withContext(Dispatchers.IO) {
      val results = LinkedHashMap<String, PhotoEntry>()

      if (!folderUri.isNullOrBlank()) {
        runCatching {
          val treeUri = Uri.parse(folderUri)
          val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
          collectFromTree(treeUri, rootDocId, depth = 0, out = results)
        }
      }

      photoUris.forEach { raw ->
        if (results.size >= MAX_PHOTOS) return@forEach
        val clean = raw.trim()
        if (clean.isNotEmpty() && clean !in results) {
          val title = queryDisplayName(Uri.parse(clean))
          results[clean] = PhotoEntry(uri = clean, title = title)
        }
      }

      results.values.toList()
    }

  private fun collectFromTree(
    treeUri: Uri,
    parentDocId: String,
    depth: Int,
    out: LinkedHashMap<String, PhotoEntry>,
  ) {
    if (depth > MAX_DEPTH || out.size >= MAX_PHOTOS) return
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
    val projection = arrayOf(
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_MIME_TYPE,
    )
    runCatching {
      context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
        val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
        while (cursor.moveToNext() && out.size < MAX_PHOTOS) {
          val docId = if (idCol >= 0) cursor.getString(idCol) else continue
          val name = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
          val mime = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else ""
          if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
            collectFromTree(treeUri, docId, depth + 1, out)
          } else if (mime.startsWith("image/") || isImageFilename(name)) {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
            out[docUri] = PhotoEntry(uri = docUri, title = cleanPhotoTitle(name))
          }
        }
      }
    }
  }

  override suspend fun loadPhoto(entry: PhotoEntry): ScreensaverPhotoSlide? =
    withContext(Dispatchers.IO) {
      runCatching {
        val uri = Uri.parse(entry.uri)
        val image = decodeSampledBitmap(uri, maxWidth = 1920, maxHeight = 1080) ?: return@runCatching null
        ScreensaverPhotoSlide(
          uri = entry.uri,
          title = entry.title,
          image = image,
        )
      }.getOrNull()
    }

  private fun decodeSampledBitmap(uri: Uri, maxWidth: Int, maxHeight: Int): ImageBitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxWidth * 2 || bounds.outHeight / sampleSize > maxHeight * 2) {
      sampleSize *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
      ?: return null
    return bmp.asImageBitmap()
  }

  private fun queryDisplayName(uri: Uri): String {
    runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
          val raw = c.getString(0)
          if (!raw.isNullOrBlank()) return cleanPhotoTitle(raw)
        }
      }
    }
    return cleanPhotoTitle(uri.lastPathSegment.orEmpty())
  }

  companion object {
    private const val MAX_PHOTOS = 300
    private const val MAX_DEPTH = 3

    private fun isImageFilename(name: String): Boolean {
      val lower = name.lowercase()
      return lower.endsWith(".jpg") ||
        lower.endsWith(".jpeg") ||
        lower.endsWith(".png") ||
        lower.endsWith(".webp") ||
        lower.endsWith(".bmp") ||
        lower.endsWith(".heic")
    }

    private fun cleanPhotoTitle(raw: String): String {
      val base = raw.substringAfterLast('/').substringAfterLast(':')
      return base.substringBeforeLast('.').replace('_', ' ').trim()
    }
  }
}
