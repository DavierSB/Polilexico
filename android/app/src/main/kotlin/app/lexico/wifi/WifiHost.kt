package app.lexico.wifi

import android.content.Context
import app.lexico.game.modes.classic.ClassicGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.ServerSocket

class WifiHost(
  context: Context,
  private val name: String,
  private val scope: CoroutineScope,
  private val kind: RoomKind = RoomKind.CLASSIC,
  private val start: suspend (guest: String) -> ClassicGame,
) {
  private val advert = RoomAdvert(context, kind)
  private var server: ServerSocket? = null
  @Volatile private var link: Link? = null
  @Volatile private var guest: String? = null
  private val _game = MutableStateFlow<ClassicGame?>(null)
  val game: StateFlow<ClassicGame?> = _game.asStateFlow()
  private val _notice = MutableStateFlow<String?>(null)
  val notice: StateFlow<String?> = _notice.asStateFlow()

  fun open(): String? = runCatching {
    server = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(kind.port)) }
    advert.register(name)
    scope.launch(Dispatchers.IO) { acceptWhileOpen() }
  }.exceptionOrNull()?.let { "No se pudo abrir la sala en esta red." }

  fun close() {
    link?.send(Wire.Bye)
    link?.close()
    runCatching { server?.close() }
    advert.unregister()
  }

  private fun acceptWhileOpen() {
    while (true) {
      val socket = runCatching { server?.accept() }.getOrNull() ?: return
      Link(socket).run(scope, ::handle, ::dropped)
    }
  }

  private suspend fun handle(from: Link, wire: Wire) {
    if (wire is Wire.Hello) return admit(from, wire)
    if (from != link) return
    val game = _game.value ?: return
    when (wire) {
      is Wire.Move -> game.guestMove(wire.input)?.let { from.send(Wire.Rejected(it)) }
      Wire.Pause -> game.pause()
      Wire.Resume -> game.resume()
      Wire.Bye -> left(game)
      else -> Unit
    }
  }

  private suspend fun admit(from: Link, hello: Wire.Hello) {
    refusal(hello)?.let { from.send(Wire.Refused(it)); from.close(); return }
    link?.takeIf { it != from }?.close()
    link = from
    from.send(Wire.Welcome(name))
    _notice.value = null
    if (guest == null) begin(hello.name) else _game.value?.let { sendState(it) }
  }

  private fun refusal(hello: Wire.Hello): String? = when {
    hello.version != PROTOCOL -> "Los dos teléfonos deben tener la misma versión de Lexico."
    guest == null && hello.name.trim().equals(name.trim(), ignoreCase = true) -> "Ese nombre ya lo usa $name: elige otro."
    guest != null && !hello.name.equals(guest, ignoreCase = true) -> "La sala de $name ya está ocupada."
    else -> null
  }

  private suspend fun begin(guestName: String) {
    guest = guestName
    advert.unregister()
    val game = start(guestName)
    _game.value = game
    scope.launch { game.state.map { it.copy(clocks = null) }.distinctUntilChanged().collect { sendState(game) } }
  }

  private suspend fun sendState(game: ClassicGame) {
    link?.send(Wire.State(game.snapshot(), game.state.value.paused))
  }

  private fun dropped(from: Link) {
    if (from != link) return
    link = null
    val game = _game.value ?: return
    game.pause()
    if (game.state.value.result == null) _notice.value = "Se perdió la conexión con $guest. Esperando a que vuelva…"
  }

  private fun left(game: ClassicGame) {
    link = null
    game.pause()
    if (game.state.value.result == null) _notice.value = "$guest salió de la partida."
  }
}
