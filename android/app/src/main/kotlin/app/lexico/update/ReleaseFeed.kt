package app.lexico.update

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

private const val FEED_URL = "https://polilexico.github.io/version.json"

private const val TIMEOUT_MS = 5_000

internal fun fetchLatest(): Release? = runCatching { parse(download()) }.getOrNull()

private fun download(): String {
  val conn = URL(FEED_URL).openConnection() as HttpURLConnection
  conn.connectTimeout = TIMEOUT_MS
  conn.readTimeout = TIMEOUT_MS
  conn.useCaches = false
  return try {
    conn.inputStream.bufferedReader().use { it.readText() }
  } finally {
    conn.disconnect()
  }
}

private fun parse(text: String): Release {
  val json = JSONObject(text)
  return Release(json.getString("version"), json.getString("url"))
}
