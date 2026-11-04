package app.lexico.online

import android.content.Context
import app.lexico.game.Lexico
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Online private constructor(context: Context) {
  val store = OnlineStore(context)

  val http: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(90, TimeUnit.SECONDS)
    .readTimeout(90, TimeUnit.SECONDS)
    .pingInterval(20, TimeUnit.SECONDS)
    .build()

  val api = OnlineApi({ store.server }, http)

  val account = OnlineAccount(store, api)

  private val backup = OfflineBackup(context.filesDir)

  val photos = OnlinePhotos(File(context.cacheDir, "photos"), api)

  private var client: OnlineClient? = null

  @Synchronized
  fun client(lexico: Lexico): OnlineClient = client ?: OnlineClient(this, lexico).also { client = it }

  suspend fun loadAccount(): AccountForm? = account.token()?.let { api.account(it).getOrNull() }

  suspend fun editAccount(form: AccountForm): String? {
    val token = account.token() ?: return "La sesión caducó: vuelve a entrar."
    return api.updateAccount(token, form).fold({ saved -> account.renamed(saved.name, saved.country); client?.shutDown(); null }) { it.message }
  }

  suspend fun setPhoto(base64: String): String? {
    val token = account.token() ?: return "La sesión caducó: vuelve a entrar."
    val failed = api.savePhoto(token, base64).exceptionOrNull()?.message
    account.session.value?.let { photos.forget(it.name, it.country); photos.request(it.name, it.country) }
    return failed
  }

  fun logOut() {
    client?.shutDown()
    account.logOut()
  }

  suspend fun syncBackup(statsSince: Long): Long? {
    val token = account.token() ?: return null
    val remote = api.backup(token).getOrNull() ?: return null
    val since = maxOf(statsSince, withContext(Dispatchers.IO) { runCatching { backup.merge(remote) }.getOrDefault(0L) })
    val packed = withContext(Dispatchers.IO) { backup.pack(since) }
    return api.saveBackup(token, packed).map { since }.getOrNull()
  }

  companion object {
    @Volatile private var instance: Online? = null

    fun of(context: Context): Online = instance ?: synchronized(this) {
      instance ?: Online(context.applicationContext).also { instance = it }
    }
  }
}
