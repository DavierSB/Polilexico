package app.lexico.online

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val RETRY_MS = 3_000L
private const val NORMAL_CLOSURE = 1000

internal class OnlineLink(
  private val server: String,
  private val http: OkHttpClient,
  private val scope: CoroutineScope,
  private val token: suspend () -> String?,
  private val onStatus: (String?) -> Unit,
  private val onEvent: suspend (OnlineEvent) -> Unit,
) {
  @Volatile private var socket: WebSocket? = null
  @Volatile private var closed = false
  private val events = Channel<OnlineEvent>(Channel.UNLIMITED)

  fun open() {
    scope.launch { for (e in events) onEvent(e) }
    scope.launch(Dispatchers.IO) { connectWhileOpen() }
  }

  fun send(text: String) {
    socket?.send(text)
  }

  fun close() {
    closed = true
    socket?.close(NORMAL_CLOSURE, null)
    events.close()
  }

  private suspend fun connectWhileOpen() {
    while (!closed) {
      val t = token() ?: return refuse()
      connectOnce(t).await()
      if (closed) return
      onStatus("Sin conexión. Reconectando…")
      delay(RETRY_MS)
    }
  }

  private fun refuse() {
    events.trySend(OnlineEvent.Refused("La sesión caducó: vuelve a entrar."))
  }

  private fun connectOnce(token: String): CompletableDeferred<Unit> {
    val done = CompletableDeferred<Unit>()
    socket = http.newWebSocket(Request.Builder().url(socketUrl()).build(), listener(token, done))
    return done
  }

  private fun listener(token: String, done: CompletableDeferred<Unit>) = object : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
      webSocket.send(hello(token))
      onStatus(null)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      event(text)?.let { events.trySend(it) }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
      webSocket.close(NORMAL_CLOSURE, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      done.complete(Unit)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      done.complete(Unit)
    }
  }

  private fun socketUrl(): String = server.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/ws"
}
