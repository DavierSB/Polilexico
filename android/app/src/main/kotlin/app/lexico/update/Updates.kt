package app.lexico.update

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.work.WorkManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Stable
class Updates(private val ctx: Context, private val current: String) {
  private val prefs = ctx.getSharedPreferences("updates", Context.MODE_PRIVATE)

  var available: Release? by mutableStateOf(stored())
    private set

  var progress: Float? by mutableStateOf(null)
    private set

  var offer: Release? by mutableStateOf(null)
    private set

  suspend fun check() {
    if (!due()) return
    refresh()
    offer = available
  }

  fun dismiss() {
    offer = null
  }

  suspend fun refresh() {
    val latest = withContext(Dispatchers.IO) { fetchLatest() } ?: return
    save(latest)
    available = latest.takeIf { isNewer(it.version, current) }
  }

  suspend fun install() {
    val release = available ?: return
    if (progress != null) return
    val ready = readyApk(ctx, release.version)
    if (ready != null) return installReady(ready)
    progress = 0f
    enqueueDownload(ctx, release.version)
  }

  suspend fun watch() {
    settlePending()
    WorkManager.getInstance(ctx).getWorkInfosForUniqueWorkFlow(UPDATE_WORK).collect { progress = downloadProgress(ctx, it.firstOrNull()) }
  }

  private suspend fun settlePending() {
    val release = available ?: return withContext(Dispatchers.IO) { discardUpdates(ctx) }
    readyApk(ctx, release.version)?.let { installReady(it) }
  }

  private suspend fun installReady(apk: File) {
    progress = 1f
    withContext(Dispatchers.IO) { installApk(ctx, apk) }
    progress = null
  }

  private fun due(): Boolean = System.currentTimeMillis() - prefs.getLong(CHECKED_KEY, 0L) >= HOUR_MS

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
    const val HOUR_MS = 60 * 60 * 1000L
    const val CHECKED_KEY = "checkedAt"
    const val VERSION_KEY = "latestVersion"
    const val URL_KEY = "latestUrl"
  }
}
