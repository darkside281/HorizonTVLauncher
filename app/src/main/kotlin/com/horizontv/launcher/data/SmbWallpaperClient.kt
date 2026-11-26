package com.horizontv.launcher.data

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.horizontv.launcher.model.JellyfinBackdrop
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JellyfinSearchItem
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * SMB2/SMB3 Network Share wallpaper provider implementing [JellyfinDataSource].
 *
 * Connects directly to Windows / NAS / Samba shares over local LAN/Wi-Fi using smbj
 * (zero extra Android permissions beyond INTERNET).
 *
 * Supported share URL formats:
 * - `smb://192.168.0.50/Wallpapers`
 * - `smb://192.168.0.50/Media/Wallpapers/4K`
 * - `//192.168.0.50/Wallpapers` or `\\192.168.0.50\Wallpapers`
 *
 * Credentials are passed via `token` as `"username\npassword"` (or blank for Guest/Anonymous).
 */
data class DiscoveredSmbServer(
  val ip: String,
  val hostName: String? = null,
)

class SmbWallpaperClient : JellyfinDataSource {

  private data class SmbTarget(
    val host: String,
    val port: Int,
    val shareName: String,
    val subPath: String,
  ) {
    val normalizedUrl: String
      get() = buildString {
        append("smb://").append(host)
        if (port != 445) append(':').append(port)
        append('/').append(shareName)
        if (subPath.isNotEmpty()) append('/').append(subPath.replace('\\', '/'))
      }
  }

  private fun parseSmbUrl(raw: String): SmbTarget? {
    val cleaned = raw.trim()
      .replace('\\', '/')
      .removePrefix("smb://")
      .removePrefix("SMB://")
      .trimStart('/')
    if (cleaned.isBlank()) return null
    val segments = cleaned.split('/').filter { it.isNotBlank() }
    if (segments.size < 2) return null
    val hostPart = segments[0]
    val host = hostPart.substringBefore(':')
    val port = hostPart.substringAfter(':', "445").toIntOrNull() ?: 445
    val shareName = segments[1]
    val subPath = if (segments.size > 2) segments.drop(2).joinToString("\\") else ""
    if (host.isBlank() || shareName.isBlank()) return null
    return SmbTarget(host = host, port = port, shareName = shareName, subPath = subPath)
  }

  private fun buildAuth(token: String?, username: String? = null, password: String? = null): AuthenticationContext {
    val resolvedUser: String
    val resolvedPass: String
    if (username != null || password != null) {
      resolvedUser = username.orEmpty().trim()
      resolvedPass = password.orEmpty()
    } else {
      val t = token.orEmpty()
      if (t.contains('\n')) {
        resolvedUser = t.substringBefore('\n').trim()
        resolvedPass = t.substringAfter('\n')
      } else {
        resolvedUser = t.trim()
        resolvedPass = ""
      }
    }
    if (resolvedUser.isBlank()) return AuthenticationContext.guest()
    val domain = if (resolvedUser.contains('\\')) resolvedUser.substringBefore('\\') else ""
    val user = if (resolvedUser.contains('\\')) resolvedUser.substringAfter('\\') else resolvedUser
    return AuthenticationContext(user, resolvedPass.toCharArray(), domain)
  }

  private fun createClient(): SMBClient {
    val config = SmbConfig.builder()
      .withTimeout(12, TimeUnit.SECONDS)
      .withSoTimeout(15, TimeUnit.SECONDS)
      .build()
    return SMBClient(config)
  }

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
      val target = parseSmbUrl(rawServerUrl)
        ?: throw IllegalArgumentException("Enter a share path like smb://192.168.0.50/Wallpapers")
      val encodedToken = if (!username.isNullOrBlank() || !password.isNullOrBlank()) {
        "${username.orEmpty().trim()}\n${password.orEmpty()}"
      } else {
        apiKey.orEmpty()
      }
      val backdrops = listShareImages(target, buildAuth(encodedToken), limit)
      if (backdrops.isEmpty()) {
        throw IllegalStateException("Connected to ${target.shareName}, but no JPG/PNG/WebP images were found.")
      }
      JellyfinAuthResult(
        serverUrl = target.normalizedUrl,
        token = encodedToken,
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
    runCatching {
      val target = parseSmbUrl(serverUrl) ?: return@runCatching emptyList()
      listShareImages(target, buildAuth(token), limit)
    }
  }

  private fun listShareImages(
    target: SmbTarget,
    auth: AuthenticationContext,
    limit: Int,
  ): List<JellyfinBackdrop> {
    val results = mutableListOf<JellyfinBackdrop>()
    createClient().use { client ->
      client.connect(target.host, target.port).use { conn ->
        val session = runCatching { conn.authenticate(auth) }
          .getOrElse {
            if (auth.username.isNullOrBlank()) conn.authenticate(AuthenticationContext.anonymous())
            else throw it
          }
        session.use { s ->
          val share = s.connectShare(target.shareName) as? DiskShare
            ?: throw IllegalStateException("Share \"${target.shareName}\" is not a file share")
          share.use { disk ->
            collectImages(
              share = disk,
              target = target,
              dirPath = target.subPath,
              depth = 0,
              maxResults = limit.coerceIn(10, 400),
              out = results,
            )
          }
        }
      }
    }
    return results.shuffled().take(limit.coerceIn(10, 400))
  }

  private fun collectImages(
    share: DiskShare,
    target: SmbTarget,
    dirPath: String,
    depth: Int,
    maxResults: Int,
    out: MutableList<JellyfinBackdrop>,
  ) {
    if (depth > MAX_DEPTH || out.size >= maxResults) return
    val entries = runCatching { share.list(dirPath) }.getOrNull() ?: return
    for (entry in entries) {
      if (out.size >= maxResults) break
      val name = entry.fileName.orEmpty()
      if (name == "." || name == ".." || name.startsWith(".")) continue
      val relPath = if (dirPath.isEmpty()) name else "$dirPath\\$name"
      val isDir = (entry.fileAttributes and 0x10L) != 0L
      if (isDir) {
        collectImages(share, target, relPath, depth + 1, maxResults, out)
      } else if (isImageFilename(name)) {
        val urlPath = relPath.replace('\\', '/')
        val fullUrl = buildString {
          append("smb://").append(target.host)
          if (target.port != 445) append(':').append(target.port)
          append('/').append(target.shareName).append('/').append(urlPath)
        }
        val title = name.substringBeforeLast('.').replace('_', ' ').trim()
        out.add(
          JellyfinBackdrop(
            itemId = "smb:${fullUrl.hashCode()}",
            title = title,
            year = null,
            imageUrl = fullUrl,
            logoUrl = null,
          ),
        )
      }
    }
  }

  override suspend fun downloadBackdropImage(
    imageUrl: String,
    token: String,
  ): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
      val target = parseSmbUrl(imageUrl) ?: return@runCatching null
      if (target.subPath.isEmpty()) return@runCatching null
      val auth = buildAuth(token)
      createClient().use { client ->
        client.connect(target.host, target.port).use { conn ->
          val session = runCatching { conn.authenticate(auth) }
            .getOrElse {
              if (auth.username.isNullOrBlank()) conn.authenticate(AuthenticationContext.anonymous())
              else throw it
            }
          session.use { s ->
            val share = s.connectShare(target.shareName) as? DiskShare ?: return@runCatching null
            share.use { disk ->
              val file = disk.openFile(
                target.subPath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null,
              )
              file.use { f ->
                val bytes = f.inputStream.use { it.readBytes() }
                if (bytes.isEmpty()) return@runCatching null
                decodeSampledBytes(bytes, maxWidth = 1920, maxHeight = 1080)
              }
            }
          }
        }
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

  private fun decodeSampledBytes(bytes: ByteArray, maxWidth: Int, maxHeight: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxWidth * 2 || bounds.outHeight / sampleSize > maxHeight * 2) {
      sampleSize *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
  }

  companion object {
    private const val MAX_DEPTH = 2

    private fun isImageFilename(name: String): Boolean {
      val lower = name.lowercase()
      return lower.endsWith(".jpg") ||
        lower.endsWith(".jpeg") ||
        lower.endsWith(".png") ||
        lower.endsWith(".webp")
    }

    fun encodeCredentials(username: String, password: String): String =
      if (username.isBlank() && password.isBlank()) "" else "${username.trim()}\n$password"

    /**
     * Scans the local /24 LAN subnet(s) in parallel for devices listening on TCP port 445 (SMB2/SMB3).
     * Requires zero extra Android permissions beyond INTERNET and finishes in ~1.5 seconds.
     */
    suspend fun scanLanSmbServers(timeoutMs: Int = 260): List<DiscoveredSmbServer> = withContext(Dispatchers.IO) {
      val localPrefixes = LinkedHashSet<String>()
      val selfIps = HashSet<String>()
      runCatching {
        val ifaces = NetworkInterface.getNetworkInterfaces()
        while (ifaces != null && ifaces.hasMoreElements()) {
          val iface = ifaces.nextElement()
          if (!iface.isUp || iface.isLoopback) continue
          val addrs = iface.inetAddresses
          while (addrs.hasMoreElements()) {
            val addr = addrs.nextElement()
            if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
              val ip = addr.hostAddress ?: continue
              selfIps.add(ip)
              val parts = ip.split('.')
              if (parts.size == 4) {
                localPrefixes.add("${parts[0]}.${parts[1]}.${parts[2]}")
              }
            }
          }
        }
      }
      if (localPrefixes.isEmpty()) return@withContext emptyList()

      val candidateIps = localPrefixes.flatMap { prefix ->
        (1..254).mapNotNull { host ->
          val ip = "$prefix.$host"
          if (ip in selfIps) null else ip
        }
      }

      val chunkSize = ((candidateIps.size + 31) / 32).coerceAtLeast(1)
      val foundIps = coroutineScope {
        candidateIps.chunked(chunkSize).map { chunk ->
          async(Dispatchers.IO) {
            chunk.mapNotNull { ip ->
              val open = runCatching {
                Socket().use { sock ->
                  sock.connect(InetSocketAddress(ip, 445), timeoutMs)
                  true
                }
              }.getOrDefault(false)
              if (open) ip else null
            }
          }
        }.awaitAll().flatten()
      }

      coroutineScope {
        foundIps.distinct().map { ip ->
          async(Dispatchers.IO) {
            val host = runCatching {
              val name = InetAddress.getByName(ip).hostName?.trim().orEmpty()
              if (name.isNotBlank() && name != ip) name.removeSuffix(".local") else null
            }.getOrNull()
            DiscoveredSmbServer(ip = ip, hostName = host)
          }
        }.awaitAll()
      }.sortedBy { it.ip.substringAfterLast('.').toIntOrNull() ?: 999 }
    }
  }
}
