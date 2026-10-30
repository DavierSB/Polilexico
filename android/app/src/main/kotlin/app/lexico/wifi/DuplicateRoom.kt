package app.lexico.wifi

import android.content.Context
import app.lexico.game.modes.duplicate.table.DuplicateTable
import app.lexico.game.modes.duplicate.table.MASTER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

class DuplicateRoom(
  context: Context,
  val name: String,
  private val scope: CoroutineScope,
  private val start: suspend (players: List<String>) -> DuplicateTable,
) {
  private val advert = RoomAdvert(context, RoomKind.DUPLICATE)
  private var server: ServerSocket? = null
  private val links = ConcurrentHashMap<Link, String>()
  private val _players = MutableStateFlow(listOf(name))
  val players: StateFlow<List<String>> = _players.asStateFlow()
  private val _table = MutableStateFlow<DuplicateTable?>(null)
  val table: StateFlow<DuplicateTable?> = _table.asStateFlow()

  fun open(): String? = runCatching {
    server = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(RoomKind.DUPLICATE.port)) }
    advert.register(name)
    scope.launch(Dispatchers.IO) { acceptWhileOpen() }
  }.exceptionOrNull()?.let { "No se pudo abrir la sala en esta red." }

  fun begin() {
    if (_table.value != null) return
    advert.unregister()
    scope.launch {
      val table = start(_players.value)
      _table.value = table
      table.changes.collect { broadcast(table) }
    }
  }

  fun close() {
    links.keys.forEach { it.send(Wire.Bye); it.close() }
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
    val player = links[from] ?: return
    val table = _table.value ?: return
    when (wire) {
      is Wire.Move -> table.propose(player, wire.input)?.let { from.send(Wire.Rejected(it)) }
      Wire.Ready -> table.ready(player)
      Wire.Bye -> dropped(from)
      else -> Unit
    }
  }

  private suspend fun admit(from: Link, hello: Wire.Hello) {
    val player = hello.name.trim()
    refusal(player, hello.version)?.let { from.send(Wire.Refused(it)); from.close(); return }
    links.entries.filter { it.value.equals(player, true) }.forEach { links.remove(it.key); it.key.close() }
    links[from] = player
    from.send(Wire.Welcome(name))
    val table = _table.value ?: return seat(player)
    table.setPresent(player, true)
    from.send(Wire.Table(table.view(player)))
  }

  private fun refusal(player: String, version: Int): String? = when {
    version != PROTOCOL -> "Los teléfonos deben tener la misma versión de Lexico."
    _table.value != null -> if (_table.value!!.players.any { it.equals(player, true) }) null else "La partida de $name ya empezó."
    player.equals(MASTER, true) -> "El nombre $MASTER está reservado: elige otro."
    player.equals(name, true) -> "Ese nombre ya está en la sala: elige otro."
    links.values.any { it.equals(player, true) } -> "Ese nombre ya está en la sala: elige otro."
    else -> null
  }

  private fun seat(player: String) {
    _players.update { it + player }
    sendLobby()
  }

  private fun sendLobby() {
    links.keys.forEach { it.send(Wire.Lobby(_players.value)) }
  }

  private suspend fun broadcast(table: DuplicateTable) {
    links.forEach { (link, player) -> link.send(Wire.Table(table.view(player))) }
  }

  private fun dropped(from: Link) {
    val player = links.remove(from) ?: return
    val table = _table.value
    if (table == null) {
      _players.update { it - player }
      sendLobby()
    } else scope.launch { table.setPresent(player, false) }
  }
}
