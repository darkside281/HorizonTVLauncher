package com.horizontv.launcher.data

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ImpersonationLevel
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.PipeShare
import com.horizontv.launcher.model.JellyfinBackdrop
import com.horizontv.launcher.model.JellyfinLibrary
import com.horizontv.launcher.model.JellyfinMediaType
import com.horizontv.launcher.model.JellyfinSearchItem
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

data class SmbBrowseResult(
  val host: String,
  val shareName: String? = null,
  val subPath: String = "",
  val currentPath: String,
  val folders: List<String> = emptyList(),
  val imageCount: Int = 0,
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
                // Two-pass streaming decode avoids loading huge 20MP+ camera photos into a ByteArray on the heap
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                f.inputStream.use { stream ->
                  BitmapFactory.decodeStream(stream, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > 1920 * 2 || bounds.outHeight / sampleSize > 1080 * 2) {
                  sampleSize *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                f.inputStream.use { stream ->
                  BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                }
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
              val name = InetAddress.getByName(ip).hostName.trim()
              if (name.isNotBlank() && name != ip) name.removeSuffix(".local") else null
            }.getOrNull()
            DiscoveredSmbServer(ip = ip, hostName = host)
          }
        }.awaitAll()
      }.sortedBy { it.ip.substringAfterLast('.').toIntOrNull() ?: 999 }
    }

    private fun buildAuthContext(username: String, password: String): AuthenticationContext {
      val trimmedUser = username.trim()
      if (trimmedUser.isBlank()) return AuthenticationContext.guest()
      val domain = if (trimmedUser.contains('\\')) trimmedUser.substringBefore('\\') else ""
      val user = if (trimmedUser.contains('\\')) trimmedUser.substringAfter('\\') else trimmedUser
      return AuthenticationContext(user, password.toCharArray(), domain)
    }

    /**
     * Lists non-hidden disk shares on an SMB server via IPC$\srvsvc DCE/RPC NetShareEnumAll (level 1).
     */
    suspend fun listServerShares(
      rawHost: String,
      username: String = "",
      password: String = "",
    ): Result<SmbBrowseResult> = withContext(Dispatchers.IO) {
      runCatching {
        val cleaned = rawHost.trim().replace('\\', '/').removePrefix("smb://").removePrefix("SMB://").trimStart('/')
        val hostPart = cleaned.substringBefore('/')
        val host = hostPart.substringBefore(':').trim()
        val port = hostPart.substringAfter(':', "445").toIntOrNull() ?: 445
        if (host.isBlank()) throw IllegalArgumentException("Enter an SMB server IP or hostname")

        val config = SmbConfig.builder()
          .withTimeout(10, TimeUnit.SECONDS)
          .withSoTimeout(12, TimeUnit.SECONDS)
          .build()
        val auth = buildAuthContext(username, password)
        val client = SMBClient(config)
        try {
          client.connect(host, port).use { connection ->
            connection.authenticate(auth).use { session ->
              val pipeShare = session.connectShare("IPC$") as? PipeShare
                ?: throw IllegalStateException("Server did not expose IPC$ share enumeration")
              pipeShare.use { ipc ->
                val pipe = ipc.open(
                  "srvsvc",
                  SMB2ImpersonationLevel.Impersonation,
                  EnumSet.of(AccessMask.MAXIMUM_ALLOWED),
                  EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                  SMB2ShareAccess.ALL,
                  SMB2CreateDisposition.FILE_OPEN,
                  null,
                )
                pipe.use { p ->
                  // 1. DCE/RPC Bind to SRVSVC v3.0
                  val bindResp = p.transact(buildSrvsvcBindPacket())
                  if (bindResp.size < 16 || bindResp[2].toInt() != 12) {
                    throw IllegalStateException("SRVSVC RPC bind rejected by $host")
                  }
                  // 2. DCE/RPC Request: NetShareEnumAll (opnum 15, level 1)
                  val firstFrag = p.transact(buildNetShareEnumAllPacket(host))
                  val stubOut = ByteArrayOutputStream()
                  var frag = firstFrag
                  while (frag.size >= 24) {
                    val flags = frag[3].toInt() and 0xFF
                    val fragLen = ((frag[8].toInt() and 0xFF) or ((frag[9].toInt() and 0xFF) shl 8))
                      .coerceAtMost(frag.size)
                    if (fragLen > 24) {
                      stubOut.write(frag, 24, fragLen - 24)
                    }
                    if ((flags and 0x02) != 0) break // PFC_LAST_FRAG
                    val nextBuf = ByteArray(16384)
                    val read = p.read(nextBuf)
                    if (read <= 0) break
                    frag = nextBuf.copyOf(read)
                  }
                  val shares = parseNetShareEnumAllLevel1(stubOut.toByteArray())
                  SmbBrowseResult(
                    host = host,
                    shareName = null,
                    subPath = "",
                    currentPath = "smb://$host/",
                    folders = shares,
                    imageCount = 0,
                  )
                }
              }
            }
          }
        } finally {
          runCatching { client.close() }
        }
      }
    }

    /**
     * Lists subdirectories and counts image files inside a specific SMB share + optional subfolder path.
     */
    suspend fun listShareFolders(
      rawHost: String,
      shareName: String,
      subPath: String = "",
      username: String = "",
      password: String = "",
    ): Result<SmbBrowseResult> = withContext(Dispatchers.IO) {
      runCatching {
        val cleaned = rawHost.trim().replace('\\', '/').removePrefix("smb://").removePrefix("SMB://").trimStart('/')
        val hostPart = cleaned.substringBefore('/')
        val host = hostPart.substringBefore(':').trim()
        val port = hostPart.substringAfter(':', "445").toIntOrNull() ?: 445
        val cleanShare = shareName.trim().trim('/', '\\')
        val cleanSub = subPath.trim().replace('/', '\\').trim('\\')
        if (host.isBlank() || cleanShare.isBlank()) {
          throw IllegalArgumentException("Host and share name are required")
        }

        val config = SmbConfig.builder()
          .withTimeout(10, TimeUnit.SECONDS)
          .withSoTimeout(12, TimeUnit.SECONDS)
          .build()
        val authCandidates = if (username.isBlank()) {
          listOf(AuthenticationContext.guest(), AuthenticationContext.anonymous())
        } else {
          listOf(buildAuthContext(username, password))
        }
        val client = SMBClient(config)
        try {
          var lastError: Throwable? = null
          for (auth in authCandidates) {
            try {
              client.connect(host, port).use { connection ->
                connection.authenticate(auth).use { session ->
                  val share = session.connectShare(cleanShare) as? DiskShare
                    ?: throw IllegalStateException("$cleanShare is not a browseable folder share")
                  return@withContext Result.success(
                    share.use { disk ->
                      val folders = mutableListOf<String>()
                      var imageCount = 0
                      val rawList = runCatching { disk.list(cleanSub) }.getOrElse { disk.list(cleanSub, "*") }
                      for (entry in rawList) {
                        val name = entry.fileName.orEmpty()
                        if (name.isBlank() || name == "." || name == ".." || name.startsWith(".") || name.startsWith("$") || name == "@eaDir" || name == "System Volume Information") {
                          continue
                        }
                        val isDir = (entry.fileAttributes and 0x10L) != 0L
                        if (isDir) {
                          folders += name
                        } else if (isImageFilename(name)) {
                          imageCount++
                        }
                      }
                      val normalizedPath = buildString {
                        append("smb://").append(host)
                        if (port != 445) append(':').append(port)
                        append('/').append(cleanShare)
                        if (cleanSub.isNotEmpty()) append('/').append(cleanSub.replace('\\', '/'))
                      }
                      SmbBrowseResult(
                        host = host,
                        shareName = cleanShare,
                        subPath = cleanSub.replace('\\', '/'),
                        currentPath = normalizedPath,
                        folders = folders.sortedBy { it.lowercase() },
                        imageCount = imageCount,
                      )
                    },
                  )
                }
              }
            } catch (t: Throwable) {
              lastError = t
            }
          }
          val msg = lastError?.message.orEmpty()
          if (msg.contains("ACCESS_DENIED", ignoreCase = true) ||
            msg.contains("LOGON_FAILURE", ignoreCase = true) ||
            msg.contains("BAD_NETWORK_NAME", ignoreCase = true)
          ) {
            throw IllegalStateException("Access denied to \"$cleanShare\" — enter Username & Password below, then tap Browse Shares.")
          }
          throw (lastError ?: IllegalStateException("Could not open share \"$cleanShare\""))
        } finally {
          runCatching { client.close() }
        }
      }
    }

    private fun buildSrvsvcBindPacket(): ByteArray {
      val buf = ByteBuffer.allocate(72).order(ByteOrder.LITTLE_ENDIAN)
      buf.put(5).put(0).put(11).put(0x03) // RPC v5.0, BIND(11), FIRST|LAST
      buf.putInt(0x10) // Little-endian ASCII/IEEE data rep
      buf.putShort(72).putShort(0).putInt(1) // frag_len=72, auth_len=0, call_id=1
      buf.putShort(4280).putShort(4280).putInt(0) // max_xmit, max_recv, assoc_group
      buf.put(1).put(0).putShort(0) // n_context_elem=1
      buf.putShort(0).put(1).put(0) // context_id=0, n_transfer_syn=1
      // Abstract syntax: SRVSVC 4b324fc8-1670-01d3-1278-5a47bf6ee188 v3.0
      buf.putInt(0x4b324fc8).putShort(0x1670).putShort(0x01d3)
      buf.put(byteArrayOf(0x12, 0x78, 0x5a, 0x47, 0xbf.toByte(), 0x6e, 0xe1.toByte(), 0x88.toByte()))
      buf.putInt(3)
      // Transfer syntax: NDR 8a885d04-1ceb-11c9-9fe8-08002b104860 v2.0
      buf.putInt(0x8a885d04.toInt()).putShort(0x1ceb).putShort(0x11c9)
      buf.put(byteArrayOf(0x9f.toByte(), 0xe8.toByte(), 0x08, 0x00, 0x2b, 0x10, 0x48, 0x60))
      buf.putInt(2)
      return buf.array()
    }

    private fun buildNetShareEnumAllPacket(host: String): ByteArray {
      val unc = "\\\\$host\u0000"
      val uncChars = unc.length
      val strBytes = unc.toByteArray(Charsets.UTF_16LE)
      val pad = (4 - (strBytes.size % 4)) % 4

      val stub = ByteBuffer.allocate(16 + strBytes.size + pad + 32).order(ByteOrder.LITTLE_ENDIAN)
      // server_unc (unique pointer to conformant varying string)
      stub.putInt(0x00020000)
      stub.putInt(uncChars).putInt(0).putInt(uncChars)
      stub.put(strBytes)
      repeat(pad) { stub.put(0) }
      // info_struct: level=1, switch=1, container_ptr=0x00020004, entries_read=0, array_ptr=0
      stub.putInt(1)
      stub.putInt(1)
      stub.putInt(0x00020004)
      stub.putInt(0)
      stub.putInt(0)
      // prefmaxlen = -1 (0xFFFFFFFF), resume_handle ptr = 0x00020008, resume_handle = 0
      stub.putInt(-1)
      stub.putInt(0x00020008)
      stub.putInt(0)

      val stubArr = stub.array()
      val totalLen = 24 + stubArr.size
      val pkt = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN)
      pkt.put(5).put(0).put(0).put(0x03) // RPC v5.0, REQUEST(0), FIRST|LAST
      pkt.putInt(0x10)
      pkt.putShort(totalLen.toShort()).putShort(0).putInt(2) // frag_len, auth_len=0, call_id=2
      pkt.putInt(stubArr.size) // alloc_hint
      pkt.putShort(0).putShort(15) // context_id=0, opnum=15 (NetShareEnumAll)
      pkt.put(stubArr)
      return pkt.array()
    }

    private fun parseNetShareEnumAllLevel1(stub: ByteArray): List<String> {
      if (stub.size < 28) return emptyList()
      val buf = ByteBuffer.wrap(stub).order(ByteOrder.LITTLE_ENDIAN)
      fun align4() {
        val rem = buf.position() % 4
        if (rem != 0) buf.position((buf.position() + (4 - rem)).coerceAtMost(buf.limit()))
      }
      fun readNdrString(): String {
        align4()
        if (buf.remaining() < 12) return ""
        buf.int // maxCount
        buf.int // offset
        val actualCount = buf.int.coerceIn(0, 1024)
        val byteCount = (actualCount * 2).coerceAtMost(buf.remaining())
        val bytes = ByteArray(byteCount)
        buf.get(bytes)
        align4()
        return String(bytes, Charsets.UTF_16LE).trimEnd('\u0000')
      }

      val level = buf.int
      val switchLevel = buf.int
      if (level != 1 || switchLevel != 1) return emptyList()
      val containerPtr = buf.int
      if (containerPtr == 0) return emptyList()
      val entriesRead = buf.int.coerceIn(0, 512)
      val arrayPtr = buf.int
      if (entriesRead == 0 || arrayPtr == 0) return emptyList()
      val maxCount = buf.int.coerceIn(0, 512)
      val count = minOf(entriesRead, maxCount)

      // Read fixed array of SHARE_INFO_1 structs: (namePtr, type, commentPtr)
      val entries = ArrayList<Triple<Int, Int, Int>>(count)
      repeat(count) {
        if (buf.remaining() < 12) return@repeat
        val namePtr = buf.int
        val shareType = buf.int
        val commentPtr = buf.int
        entries += Triple(namePtr, shareType, commentPtr)
      }

      // Read deferred UTF-16LE strings in order
      val shares = mutableListOf<String>()
      for ((namePtr, shareType, commentPtr) in entries) {
        val name = if (namePtr != 0) readNdrString() else ""
        if (commentPtr != 0) readNdrString() // skip remark
        val baseType = shareType and 0x0FFFFFFF
        if (baseType == 0 && name.isNotBlank() && !name.endsWith("$")) {
          shares += name
        }
      }
      return shares.distinct().sortedBy { it.lowercase() }
    }
  }
}
