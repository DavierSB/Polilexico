package app.lexico.update

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Stable
class Updates(private val ctx: Context, private val current: String) {
  private val prefs = ctx.getSharedPreferences("updates", Context.MODE_PRIVATE)

  var available: Release? by mutableStateOf(stored())
    private set

  var progress: Float? by mutableStateOf(null)
    private set

  suspend fun check() {
    if (due()) refresh()
  }

  suspend fun refresh() {
    val latest = withContext(Dispatchers.IO) { fetchLatest() } ?: return
    save(latest)
    available = latest.takeIf { isNewer(it.version, current) }
  }

  suspend fun install() {
    if (progress != null) return
    progress = 0f
    val apk = withContext(Dispatchers.IO) { runCatching { downloadApk(ctx) { progress = it } }.getOrNull() }
    progress = null
    apk?.let { ctx.startActivity(installIntent(ctx, it)) }
  }

  private fun due(): Boolean = System.currentTimeMillis() - prefs.getLong(CHECKED_KEY, 0L) >= DAY_MS

  private fun stored(): Release? {
    val version = prefs.getString(VERSION_KEY, null) ?: return null
    val url = prefs.getString(URL_KEY, null) ?: return null
    return Release(version, url).takeIf { isNewer(version, current) }
  }

  private fun save(latest: Release) {
    prefs.edit {
      putLong(CHECKED_KEY, System.currentTimeMillis())
      putString(VERSION_KEY, latest.version)
      putString(URL_KEY, latest.url)
    }
  }

  private companion object {
    const val DAY_MS = 24 * 60 * 60 * 1000L
    const val CHECKED_KEY = "checkedAt"
    const val VERSION_KEY = "latestVersion"
    const val URL_KEY = "latestUrl"
  }
}
