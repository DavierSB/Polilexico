package app.lexico.wifi

import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.classic.ClassicRemote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket

private const val CONNECT_MS = 5_000
private const val RETRY_MS = 2_000L

class WifiGuest(
  private val name: String,
  private val room: Room,
  private val scope: CoroutineScope,
  private val join: suspend (save: String, paused: Boolean, remote: ClassicRemote) -> ClassicGame,
) : ClassicRemote {
  @Volatile private var link: Link? = null
  @Volatile private var done = false
  private val _game = MutableStateFlow<ClassicGame?>(null)
  val game: StateFlow<ClassicGame?> = _game.asStateFlow()
  private val _notice = MutableStateFlow<String?>(null)
  val notice: StateFlow<String?> = _notice.asStateFlow()
  private val _refusal = MutableStateFlow<String?>(null)
  val refusal: StateFlow<String?> = _refusal.asStateFlow()

  fun open() {
    scope.launch(Dispatchers.IO) { connect() }
  }

  fun close() {
    done = true
    link?.send(Wire.Bye)
    link?.close()
  }

  override fun move(input: String) {
    link?.send(Wire.Move(input)) ?: run { _notice.value = "Sin conexión: la jugada no se envió." }
  }

  override fun pause() {
    link?.send(Wire.Pause)
  }

  override fun resume() {
    link?.send(Wire.Resume)
  }

  private suspend fun connect() {
    while (!done) {
      val socket = runCatching { Socket().apply { connect(InetSocketAddress(room.address, room.port), CONNECT_MS) } }.getOrNull()
      if (socket != null) return attach(Link(socket))
      _notice.value = "No se encuentra la sala de ${room.name}. Reintentando…"
      delay(RETRY_MS)
    }
  }

  private fun attach(fresh: Link) {
    link = fresh
    fresh.run(scope, ::handle, ::dropped)
    fresh.send(Wire.Hello(name, PROTOCOL))
  }

  private suspend fun handle(from: Link, wire: Wire) {
    when (wire) {
      is Wire.Welcome -> _notice.value = null
      is Wire.State -> mirror(wire)
      is Wire.Rejected -> _notice.value = wire.reason
      is Wire.Refused -> refuse(wire.reason)
      Wire.Bye -> hostLeft()
      else -> Unit
    }
  }

  private suspend fun mirror(state: Wire.State) {
    val game = _game.value ?: return run { _game.value = join(state.save, state.paused, this) }
    if (game.state.value.result == null) game.mirror(state.save, state.paused)
  }

  private fun refuse(reason: String) {
    done = true
    _refusal.value = reason
  }

  private fun hostLeft() {
    done = true
    if (_game.value?.state?.value?.result == null) _notice.value = "${room.name} cerró la sala."
  }

  private fun dropped(from: Link) {
    if (from != link) return
    link = null
    if (done) return
    _notice.value = "Se perdió la conexión con ${room.name}. Reconectando…"
    scope.launch(Dispatchers.IO) { delay(RETRY_MS); connect() }
  }
}
