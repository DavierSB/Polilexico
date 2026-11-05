package app.lexico.online

import android.os.SystemClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val RETRY_MS = 3_000L
private const val CHECK_MS = 2_000L
private const val PING_MS = 8_000L
private const val SILENCE_MS = 16_000L
private const val NORMAL_CLOSURE = 1000
private const val LINK_DOWN = "Sin conexión. Reconectando…"

internal class OnlineLink(
  private val server: String,
  private val http: OkHttpClient,
  private val network: NetworkWatch,
  private val scope: CoroutineScope,
  private val token: suspend () -> String?,
  private val onStatus: (String?) -> Unit,
  private val onEvent: suspend (OnlineEvent) -> Unit,
) {
  @Volatile private var socket: WebSocket? = null
  @Volatile private var closed = false
  @Volatile private var live = false
  @Volatile private var heard = 0L
  @Volatile private var pinged = 0L
  private val events = Channel<OnlineEvent>(Channel.UNLIMITED)
  private val wake = Channel<Unit>(Channel.CONFLATED)
  private var unwatch: (() -> Unit)? = null

  fun open() {
    unwatch = network.watch(::lost, ::back)
    scope.launch { for (e in events) onEvent(e) }
    scope.launch(Dispatchers.IO) { connectWhileOpen() }
    scope.launch { heartbeat() }
  }

  fun send(text: String): Boolean = live && socket?.send(text) == true

  fun close() {
    closed = true
    unwatch?.invoke()
    socket?.close(NORMAL_CLOSURE, null)
    events.close()
  }

  private suspend fun connectWhileOpen() {
    while (!closed) {
      val t = token() ?: return refuse()
      connectOnce(t).await()
      if (closed) return
      onStatus(LINK_DOWN)
      withTimeoutOrNull(RETRY_MS) { wake.receive() }
    }
  }

  private suspend fun heartbeat() {
    while (!closed) {
      delay(CHECK_MS)
      if (live) check(now())
    }
  }

  private fun check(at: Long) {
    when {
      at - heard > SILENCE_MS -> drop()
      at - pinged >= PING_MS -> { pinged = at; socket?.send(ping()) }
    }
  }

  private fun lost() {
    if (closed) return
    onStatus(LINK_DOWN)
    drop()
  }

  private fun back(switched: Boolean) {
    if (switched) drop()
    wake.trySend(Unit)
  }

  private fun drop() {
    live = false
    socket?.cancel()
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
      heard = now()
      pinged = heard
      live = true
      webSocket.send(hello(token))
      onStatus(null)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      heard = now()
      event(text)?.let { events.trySend(it) }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
      webSocket.close(NORMAL_CLOSURE, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = ended(webSocket, done)

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = ended(webSocket, done)
  }

  private fun ended(webSocket: WebSocket, done: CompletableDeferred<Unit>) {
    if (webSocket == socket) live = false
    done.complete(Unit)
  }

  private fun now(): Long = SystemClock.elapsedRealtime()

  private fun socketUrl(): String = server.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/ws"
}
