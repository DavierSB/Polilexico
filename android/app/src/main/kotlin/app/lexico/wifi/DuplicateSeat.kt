package app.lexico.wifi

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

class DuplicateSeat(val name: String, private val room: Room, private val scope: CoroutineScope) {
  @Volatile private var link: Link? = null
  @Volatile private var done = false
  private val _players = MutableStateFlow<List<String>>(emptyList())
  val players: StateFlow<List<String>> = _players.asStateFlow()
  private val _view = MutableStateFlow<String?>(null)
  val view: StateFlow<String?> = _view.asStateFlow()
  private val _notice = MutableStateFlow<String?>(null)
  val notice: StateFlow<String?> = _notice.asStateFlow()
  private val _status = MutableStateFlow<String?>(null)
  val status: StateFlow<String?> = _status.asStateFlow()
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

  fun propose(input: String) {
    _notice.value = null
    link?.send(Wire.Move(input)) ?: run { _notice.value = "Sin conexión: la jugada no se envió." }
  }

  fun ready() {
    link?.send(Wire.Ready)
  }

  private suspend fun connect() {
    while (!done) {
      val socket = runCatching { Socket().apply { connect(InetSocketAddress(room.address, room.port), CONNECT_MS) } }.getOrNull()
      if (socket != null) return attach(Link(socket))
      _status.value = "No se encuentra la sala de ${room.name}. Reintentando…"
      delay(RETRY_MS)
    }
  }

  private fun attach(fresh: Link) {
    link = fresh
    fresh.run(scope, ::handle, ::dropped)
    fresh.send(Wire.Hello(name, PROTOCOL))
  }

  private fun handle(from: Link, wire: Wire) {
    when (wire) {
      is Wire.Welcome -> _status.value = null
      is Wire.Lobby -> _players.value = wire.players
      is Wire.Table -> _view.value = wire.view
      is Wire.Rejected -> _notice.value = wire.reason
      is Wire.Refused -> { done = true; _refusal.value = wire.reason }
      Wire.Bye -> { done = true; _status.value = "${room.name} cerró la sala." }
      else -> Unit
    }
  }

  private fun dropped(from: Link) {
    if (from != link) return
    link = null
    if (done) return
    _status.value = "Se perdió la conexión con ${room.name}. Reconectando…"
    scope.launch(Dispatchers.IO) { delay(RETRY_MS); connect() }
  }
}
