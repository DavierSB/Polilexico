package app.lexico.online

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.json.JSONObject

class OfflineBackup(filesDir: File) {
  private val games = File(filesDir, "games")
  private val abandoned = File(filesDir, "abandoned.tsv")

  fun pack(statsSince: Long): String {
    val files = JSONObject()
    games.listFiles().orEmpty().filter { it.isFile }.forEach { files.put(it.name, it.readText()) }
    val json = JSONObject().put("files", files).put("abandoned", abandonedLines().joinToString("\n")).put("statsSince", statsSince)
    return encode(json.toString())
  }

  fun merge(data: String): Long {
    if (data.isBlank()) return 0
    val json = JSONObject(decode(data))
    restoreFiles(json.getJSONObject("files"))
    restoreAbandoned(json.optString("abandoned"))
    return json.optLong("statsSince")
  }

  private fun restoreFiles(files: JSONObject) {
    games.mkdirs()
    files.keys().forEach { name ->
      val file = File(games, File(name).name)
      if (!file.exists()) file.writeText(files.getString(name))
    }
  }

  private fun restoreAbandoned(remote: String) {
    val merged = (abandonedLines() + remote.lines().filter { it.isNotBlank() }).distinct()
    abandoned.parentFile?.mkdirs()
    abandoned.writeText(merged.joinToString("") { "$it\n" })
  }

  private fun abandonedLines(): List<String> = runCatching { abandoned.readLines().filter { it.isNotBlank() } }.getOrDefault(emptyList())

  private fun encode(text: String): String {
    val bytes = ByteArrayOutputStream()
    GZIPOutputStream(bytes).use { it.write(text.toByteArray()) }
    return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
  }

  private fun decode(data: String): String =
    GZIPInputStream(ByteArrayInputStream(Base64.decode(data, Base64.NO_WRAP))).use { it.readBytes().decodeToString() }
}
