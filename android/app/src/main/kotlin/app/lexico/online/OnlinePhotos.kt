package app.lexico.online

import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class OnlinePhotos(private val dir: File, private val api: OnlineApi) {
  val images = mutableStateMapOf<String, ImageBitmap>()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val checkedAt = ConcurrentHashMap<String, Long>()

  fun request(name: String, country: String) {
    val key = key(name, country)
    val now = System.currentTimeMillis()
    if (now - (checkedAt[key] ?: 0L) < RECHECK_MS) return
    checkedAt[key] = now
    scope.launch { refresh(key, name, country) }
  }

  fun forget(name: String, country: String) {
    checkedAt.remove(key(name, country))
  }

  private suspend fun refresh(key: String, name: String, country: String) {
    if (key !in images) local(key)?.let { show(key, it) }
    when (val reply = api.photo(name, country, etag(key))) {
      is PhotoReply.Fresh -> store(key, reply)
      PhotoReply.None -> erase(key)
      PhotoReply.Unreachable -> checkedAt.remove(key)
      PhotoReply.Same -> Unit
    }
  }

  private suspend fun store(key: String, reply: PhotoReply.Fresh) {
    dir.mkdirs()
    image(key).writeBytes(reply.bytes)
    reply.etag?.let { tag(key).writeText(it) }
    decode(reply.bytes)?.let { show(key, it) }
  }

  private suspend fun erase(key: String) {
    image(key).delete()
    tag(key).delete()
    withContext(Dispatchers.Main) { images.remove(key) }
  }

  private suspend fun show(key: String, bitmap: ImageBitmap) = withContext(Dispatchers.Main) { images[key] = bitmap }

  private fun local(key: String): ImageBitmap? = image(key).takeIf { it.exists() }?.readBytes()?.let(::decode)

  private fun etag(key: String): String? = tag(key).takeIf { it.exists() && image(key).exists() }?.readText()

  private fun image(key: String) = File(dir, "$key.jpg")

  private fun tag(key: String) = File(dir, "$key.etag")

  private fun decode(bytes: ByteArray): ImageBitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()

  companion object {
    private const val RECHECK_MS = 10 * 60_000L
    private val UNSAFE = Regex("[^a-z0-9._-]")

    fun key(name: String, country: String): String = "${name.trim().lowercase()}_${country.uppercase()}".replace(UNSAFE, "_")
  }
}
