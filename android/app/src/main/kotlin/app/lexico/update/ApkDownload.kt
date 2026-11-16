package app.lexico.update

import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASES = "https://github.com/DavierSB/Polilexico/releases/download"
private const val TIMEOUT_MS = 15_000
private const val RANGE_NOT_SATISFIABLE = 416
private val SPLIT_ABIS = listOf("arm64-v8a", "armeabi-v7a")

internal class FatalDownload(code: Int) : Exception("HTTP $code")

internal fun apkUrl(version: String): String = "$RELEASES/v$version/app-${abi()}-release.apk"

internal fun resumeDownload(url: String, part: File, onChunk: (Long, Long) -> Unit) {
  val conn = open(url, part.length())
  try {
    receive(conn, part, onChunk)
  } finally {
    conn.disconnect()
  }
}

private fun abi(): String = Build.SUPPORTED_ABIS.firstOrNull { it in SPLIT_ABIS } ?: "universal"

private fun open(url: String, from: Long): HttpURLConnection =
  (URL(url).openConnection() as HttpURLConnection).apply {
    connectTimeout = TIMEOUT_MS
    readTimeout = TIMEOUT_MS
    if (from > 0) setRequestProperty("Range", "bytes=$from-")
  }

private fun receive(conn: HttpURLConnection, part: File, onChunk: (Long, Long) -> Unit) {
  when (val code = conn.responseCode) {
    HttpURLConnection.HTTP_PARTIAL -> write(conn, part, part.length(), rangeTotal(conn), onChunk)
    HttpURLConnection.HTTP_OK -> write(conn, part, 0L, conn.contentLengthLong, onChunk)
    RANGE_NOT_SATISFIABLE -> settle(part, rangeTotal(conn))
    in 400..499 -> throw FatalDownload(code)
    else -> throw IOException("HTTP $code")
  }
}

private fun write(conn: HttpURLConnection, part: File, start: Long, total: Long, onChunk: (Long, Long) -> Unit) {
  conn.inputStream.use { input -> FileOutputStream(part, start > 0).use { copy(input, it, start, total, onChunk) } }
  if (total > 0 && part.length() != total) throw IOException("incomplete")
}

private fun settle(part: File, total: Long) {
  if (part.length() == total) return
  part.delete()
  throw IOException("range")
}

private fun rangeTotal(conn: HttpURLConnection): Long =
  conn.getHeaderField("Content-Range")?.substringAfterLast('/')?.toLongOrNull() ?: -1L

private fun copy(input: InputStream, output: OutputStream, start: Long, total: Long, onChunk: (Long, Long) -> Unit) {
  val buffer = ByteArray(64 * 1024)
  var done = start
  while (true) {
    val read = input.read(buffer)
    if (read < 0) return
    output.write(buffer, 0, read)
    done += read
    onChunk(done, total)
  }
}
