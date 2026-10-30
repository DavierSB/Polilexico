package app.lexico.wifi

import android.content.Context
import app.lexico.game.modes.recall.ScoredPlacement
import app.lexico.game.race.Race
import app.lexico.game.race.RaceSetup
import app.lexico.game.race.toJson
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

class RaceRoom(
  context: Context, val name: String, private val kind: RoomKind, private val setup: RaceSetup, private val scope: CoroutineScope,
  private val nextGame: (suspend () -> List<ScoredPlacement>)?,
) {
  private val advert = RoomAdvert(context, kind)
  private var server: ServerSocket? = null
  private val links = ConcurrentHashMap<Link, String>()
  private val _players = MutableStateFlow(listOf(name))
  val players: StateFlow<List<String>> = _players.asStateFlow()
  private val _race = MutableStateFlow<Race?>(null)
  val race: StateFlow<Race?> = _race.asStateFlow()

  fun open(): String? = runCatching {
    server = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(kind.port)) }
    advert.register(name)
    scope.launch(Dispatchers.IO) { acceptWhileOpen() }
  }.exceptionOrNull()?.let { "No se pudo abrir la sala en esta red." }

  fun begin() {
    if (_race.value != null) return
    advert.unregister()
    val race = Race(setup, _players.value, scope, nextGame)
    _race.value = race
    scope.launch { race.view.collect { view -> broadcast(Wire.Race(view.toJson())) } }
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

  private fun handle(from: Link, wire: Wire) {
    if (wire is Wire.Hello) return admit(from, wire)
    val player = links[from] ?: return
    when (wire) {
      is Wire.Score -> _race.value?.report(player, wire.score, wire.points, wire.done, wire.lives)
      is Wire.RoundDone -> _race.value?.finishRound(player, wire.round)
      Wire.Ready -> _race.value?.ready(player)
      Wire.Bye -> dropped(from)
      else -> Unit
    }
  }

  private fun admit(from: Link, hello: Wire.Hello) {
    val player = hello.name.trim()
    refusal(player, hello.version)?.let { from.send(Wire.Refused(it)); from.close(); return }
    links.entries.filter { it.value.equals(player, true) }.forEach { links.remove(it.key); it.key.close() }
    links[from] = player
    from.send(Wire.Welcome(name))
    val race = _race.value ?: return seat(player)
    race.setPresent(player, true)
    from.send(Wire.Race(race.view.value.toJson()))
  }

  private fun refusal(player: String, version: Int): String? {
    val race = _race.value
    return when {
      version != PROTOCOL -> "Los teléfonos deben tener la misma versión de Lexico."
      race != null -> if (race.players.any { it.equals(player, true) }) null else "La partida de $name ya empezó."
      player.equals(name, true) || links.values.any { it.equals(player, true) } -> "Ese nombre ya está en la sala: elige otro."
      else -> null
    }
  }

  private fun seat(player: String) {
    _players.update { it + player }
    broadcast(Wire.Lobby(_players.value))
  }

  private fun broadcast(wire: Wire) {
    links.keys.forEach { it.send(wire) }
  }

  private fun dropped(from: Link) {
    val player = links.remove(from) ?: return
    val race = _race.value ?: return run {
      _players.update { it - player }
      broadcast(Wire.Lobby(_players.value))
    }
    race.setPresent(player, false)
  }
}
