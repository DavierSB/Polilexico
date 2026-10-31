package app.lexico.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASES = "https://github.com/DavierSB/Polilexico/releases/latest/download"
private const val APK_TYPE = "application/vnd.android.package-archive"
private const val TIMEOUT_MS = 15_000
private val SPLIT_ABIS = listOf("arm64-v8a", "armeabi-v7a")

internal fun downloadApk(ctx: Context, onProgress: (Float) -> Unit): File {
  val file = File(ctx.cacheDir, "updates/polilexico.apk").apply { parentFile?.mkdirs() }
  val conn = URL(apkUrl()).openConnection() as HttpURLConnection
  conn.connectTimeout = TIMEOUT_MS
  conn.readTimeout = TIMEOUT_MS
  try {
    val total = conn.contentLengthLong
    conn.inputStream.use { input -> file.outputStream().use { copy(input, it, total, onProgress) } }
  } finally {
    conn.disconnect()
  }
  return file
}

internal fun installIntent(ctx: Context, apk: File): Intent {
  val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", apk)
  return Intent(Intent.ACTION_VIEW).setDataAndType(uri, APK_TYPE)
    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun apkUrl(): String = "$RELEASES/app-${abi()}-release.apk"

private fun abi(): String = Build.SUPPORTED_ABIS.firstOrNull { it in SPLIT_ABIS } ?: "universal"

private fun copy(input: InputStream, output: OutputStream, total: Long, onProgress: (Float) -> Unit) {
  val buffer = ByteArray(64 * 1024)
  var done = 0L
  while (true) {
    val read = input.read(buffer)
    if (read < 0) return
    output.write(buffer, 0, read)
    done += read
    if (total > 0) onProgress(done.toFloat() / total)
  }
}
