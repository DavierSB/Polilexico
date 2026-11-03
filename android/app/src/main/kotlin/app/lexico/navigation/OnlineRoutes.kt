package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.lexico.game.Lexico
import app.lexico.game.modes.duplicate.table.TableState
import app.lexico.menu.Settings
import app.lexico.menu.ThemeState
import app.lexico.modes.duplicate.seatView
import app.lexico.online.AccountForm
import app.lexico.online.Online
import app.lexico.online.OnlineClient
import app.lexico.online.OnlineSession
import app.lexico.online.OnlineStatsRow
import app.lexico.online.Person
import app.lexico.online.Proposal
import app.lexico.online.RoomInfo
import app.lexico.online.Seek
import app.lexico.ui.classic.NewOnlineClassicScreen
import app.lexico.ui.common.AccountInput
import app.lexico.ui.common.Durations
import app.lexico.ui.common.GuestLobbyScreen
import app.lexico.ui.common.HostLobbyScreen
import app.lexico.ui.common.OnlineAccountScreen
import app.lexico.ui.common.OnlineHubActions
import app.lexico.ui.common.OnlineHubScreen
import app.lexico.ui.common.OnlineHubState
import app.lexico.ui.common.OnlineProfile
import app.lexico.ui.common.OnlineStatsScreen
import app.lexico.ui.common.OnlineWaitScreen
import app.lexico.ui.common.PersonItem
import app.lexico.ui.common.SeekItem
import app.lexico.ui.common.ServerDialog
import app.lexico.ui.common.StatsSection
import app.lexico.ui.common.WaitingRoomScreen
import app.lexico.ui.common.flagOf
import app.lexico.ui.duplicate.NewOnlineDuplicateScreen
import app.lexico.ui.duplicate.SeatScreen
import kotlinx.coroutines.launch

private const val RIVAL_AWAY = "Tu rival se desconectó. Su reloj sigue corriendo."

private var backupSynced = false

@Composable
internal fun OnlineRoute(lexico: Lexico, settings: Settings, nav: Navigator) {
  val online = rememberOnline()
  val session by online.account.session.collectAsState()
  var editingServer by remember { mutableStateOf(false) }
  session?.let { OnlineHub(it, online, lexico, settings, nav) } ?: AccountRoute(online, nav) { editingServer = true }
  if (editingServer) ServerDialog(online.store.server, { online.store.server = it }) { editingServer = false }
}

@Composable
internal fun NewOnlineClassicRoute(screen: Screen.OnlineNewClassic, lexico: Lexico, nav: Navigator) {
  val client = rememberClient(lexico)
  val rival = screen.rival
  val title = rival?.let { "Retar a ${it.name} ${flagOf(it.country)}" } ?: "Buscar clásica"
  NewOnlineClassicScreen(title, if (rival == null) "Publicar búsqueda" else "Enviar reto", nav::back) { time, overtime ->
    val proposal = Proposal.Classic(time, overtime)
    if (rival == null) client.publish(proposal) else client.challenge(rival, proposal)
    nav.replace(Screen.OnlinePlay)
  }
}

@Composable
internal fun NewOnlineDuplicateRoute(lexico: Lexico, nav: Navigator) {
  val client = rememberClient(lexico)
  NewOnlineDuplicateScreen(nav::back) { turn, rounds, live ->
    client.publish(Proposal.Duplicate(turn, rounds, live))
    nav.replace(Screen.OnlinePlay)
  }
}

@Composable
internal fun OnlineStatsRoute(nav: Navigator) {
  val online = rememberOnline()
  val session = online.account.session.collectAsState().value ?: return
  val loaded by produceState<Result<List<OnlineStatsRow>>?>(null) {
    value = online.account.token()?.let { online.api.stats(it) } ?: Result.failure(Exception("La sesión caducó: vuelve a entrar."))
  }
  OnlineStatsScreen(profileOf(session), loaded?.getOrNull()?.map(::statsSection), loaded?.exceptionOrNull()?.message, nav::back)
}

@Composable
internal fun OnlinePlayRoute(lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val client = rememberClient(lexico)
  DisposableEffect(client) { onDispose { client.forgetIfFinished() } }
  val info by client.room.collectAsState()
  val ended by client.ended.collectAsState()
  val status by client.status.collectAsState()
  val current = info
  when {
    current == null -> OnlineWaitScreen("En línea", ended ?: status ?: "Conectando…", ended != null) { client.forget(); nav.back() }
    current.duplicate -> DuplicateOnline(client, current, nav, themes)
    else -> ClassicOnline(client, current, lexico, nav, themes)
  }
}

@Composable
private fun AccountRoute(online: Online, nav: Navigator, onServer: () -> Unit) {
  val scope = rememberCoroutineScope()
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val submit = { signingUp: Boolean, input: AccountInput ->
    scope.launch {
      busy = true
      error = with(online.account) { if (signingUp) signUp(input.form()) else logIn(input.form()) }
      busy = false
    }
    Unit
  }
  OnlineAccountScreen(busy, error, nav::back, onServer, { submit(true, it) }) { submit(false, it) }
}

@Composable
private fun OnlineHub(session: OnlineSession, online: Online, lexico: Lexico, settings: Settings, nav: Navigator) {
  val client = rememberClient(lexico)
  LaunchedEffect(session.name) { syncBackupOnce(online, settings) }
  val lobby by client.lobby.collectAsState()
  val room by client.room.collectAsState()
  val status by client.status.collectAsState()
  val refused by client.refused.collectAsState()
  val seekOf = { id: String -> (lobby.seeks + lobby.invites).find { it.id == id } }
  val actions = OnlineHubActions(
    back = nav::back, logOut = online::logOut, resume = { nav.go(Screen.OnlinePlay) },
    seekClassic = { nav.go(Screen.OnlineNewClassic()) }, createDuplicate = { nav.go(Screen.OnlineNewDuplicate) },
    accept = { id -> seekOf(id)?.let { client.accept(it); nav.go(Screen.OnlinePlay) } },
    challenge = { nav.go(Screen.OnlineNewClassic(Person(it.name, it.country, it.busy))) },
    decline = { id -> seekOf(id)?.let(client::decline) }, stats = { nav.go(Screen.OnlineStats) },
  )
  val state = OnlineHubState(
    refused ?: status, room != null, lobby.seeks.map(::seekItem), lobby.people.map { PersonItem(it.name, it.country, it.busy) },
    lobby.invites.firstOrNull()?.let(::seekItem),
  )
  OnlineHubScreen(profileOf(session), state, actions)
}

@Composable
private fun ClassicOnline(client: OnlineClient, info: RoomInfo, lexico: Lexico, nav: Navigator, themes: ThemeState) {
  val game by client.game.collectAsState()
  val notice by client.notice.collectAsState()
  val status by client.status.collectAsState()
  val ended by client.ended.collectAsState()
  val rivalOnline by client.rivalOnline.collectAsState()
  val shown = status ?: ended ?: RIVAL_AWAY.takeUnless { rivalOnline } ?: notice
  game?.let { ClassicPlay(it, nav, themes, lexico.finishedGames, shown, handOff = false, pausable = false) }
    ?: WaitingRoomScreen(info.host, ended, { client.leave(); nav.back() }, wifi = false)
}

@Composable
private fun DuplicateOnline(client: OnlineClient, info: RoomInfo, nav: Navigator, themes: ThemeState) {
  val json by client.table.collectAsState()
  val notice by client.notice.collectAsState()
  val status by client.status.collectAsState()
  val ended by client.ended.collectAsState()
  var state by remember { mutableStateOf<TableState?>(null) }
  LaunchedEffect(json) { json?.let { state = read(it) } }
  val actions = remember(client) { seatActions(nav, propose = client::propose, ready = client::ready) }
  val table = state
  val leave = { client.leave(); nav.back() }
  when {
    table != null -> SeatScreen(seatView(table, info.me, notice, status ?: ended), themes.board, actions)
    info.first -> HostLobbyScreen(info.me, info.players, ended, client::start, leave, wifi = false)
    else -> GuestLobbyScreen(info.host, info.players, status ?: ended, info.me, leave)
  }
}

@Composable
private fun rememberClient(lexico: Lexico): OnlineClient {
  val online = rememberOnline()
  val client = remember { online.client(lexico) }
  DisposableEffect(client) { client.acquire(); onDispose { client.release() } }
  return client
}

@Composable
private fun rememberOnline(): Online {
  val context = LocalContext.current
  return remember { Online.of(context) }
}

private suspend fun syncBackupOnce(online: Online, settings: Settings) {
  if (backupSynced) return
  online.syncBackup(settings.statsSince)?.let { settings.adoptStatsSince(it); backupSynced = true }
}

private fun seekItem(s: Seek): SeekItem = SeekItem(s.id, s.host, s.country, seekDetail(s), s.mine)

private fun seekDetail(s: Seek): String =
  if (s.duplicate) "Duplicada · ${Durations.format(s.turnMs)} por ronda · ${s.players} en la sala"
  else "Clásica · ${Durations.format(s.timeMs)} + ${Durations.format(s.overtimeMs)} de descuento"

private fun profileOf(session: OnlineSession): OnlineProfile = OnlineProfile(session.name, session.country)

private fun AccountInput.form(): AccountForm = AccountForm(name, country, password, email)

private fun statsSection(row: OnlineStatsRow): StatsSection = if (row.mode == "duplicate") duplicateSection(row) else classicSection(row)

private fun classicSection(row: OnlineStatsRow): StatsSection = StatsSection(
  "Clásica",
  listOf(
    "Partidas" to "${row.games}",
    "Victorias" to "${row.wins}  (%.0f %%)".format(100.0 * row.wins / row.games.coerceAtLeast(1)),
    "Derrotas" to "${row.losses}",
    "Empates" to "${row.draws}",
    "Promedio de puntos" to "%.0f".format(row.avgScore),
    "Mayor scrabble" to if (row.bestBingo > 0) "${row.bestBingo}" else "—",
    "Mejor jugada" to "${row.bestPlay}",
  ),
)

private fun duplicateSection(row: OnlineStatsRow): StatsSection = StatsSection(
  "Duplicada",
  listOf(
    "Partidas" to "${row.games}",
    "Primeros puestos" to "${row.wins}",
    "Efectividad promedio" to (row.avgEfficiency?.let { "%.1f %%".format(it * 100) } ?: "—"),
    "Mayor scrabble" to if (row.bestBingo > 0) "${row.bestBingo}" else "—",
    "Mejor jugada" to "${row.bestPlay}",
  ),
)
