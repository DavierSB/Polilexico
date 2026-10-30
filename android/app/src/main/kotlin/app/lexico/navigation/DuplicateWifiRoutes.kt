package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.lexico.game.modes.duplicate.table.DuplicateTable
import app.lexico.game.modes.duplicate.table.TableSetup
import app.lexico.game.modes.duplicate.table.TableState
import app.lexico.game.modes.duplicate.table.readTable
import app.lexico.menu.ThemeState
import app.lexico.model.Placement
import app.lexico.modes.duplicate.seatView
import app.lexico.ui.common.GuestLobbyScreen
import app.lexico.ui.common.HostLobbyScreen
import app.lexico.ui.duplicate.SeatActions
import app.lexico.ui.duplicate.SeatScreen
import app.lexico.wifi.DuplicateRoom
import app.lexico.wifi.DuplicateSeat
import app.lexico.wifi.Room
import app.lexico.wifi.RoomKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DuplicateHostRoute(screen: Screen.DuplicateWifiHost, nav: Navigator, themes: ThemeState) {
  val room = rememberRoom(screen)
  val error = remember(room) { room.open() }
  val table by room.table.collectAsState()
  DisposableEffect(room) { onDispose { room.close(); room.table.value?.close() } }
  val players by room.players.collectAsState()
  table?.let { HostTable(it, room.name, nav, themes) }
    ?: HostLobbyScreen(room.name, players, error, room::begin, nav::back)
}

@Composable
internal fun DuplicateJoinRoute(screen: Screen.DuplicateWifiJoin, nav: Navigator, themes: ThemeState) {
  var room by remember { mutableStateOf<Room?>(null) }
  var refusal by remember { mutableStateOf<String?>(null) }
  val chosen = room
  if (chosen == null) FindRooms(RoomKind.DUPLICATE, refusal, nav) { room = it; refusal = null }
  else GuestSeat(screen.name, chosen, nav, themes) { refusal = it; room = null }
}

@Composable
private fun rememberRoom(screen: Screen.DuplicateWifiHost): DuplicateRoom {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val c = screen.config
  return remember(screen) { DuplicateRoom(context, c.name, scope) { DuplicateTable.open(it, TableSetup(c.turnMs, c.rounds, c.live)) } }
}

@Composable
private fun HostTable(table: DuplicateTable, me: String, nav: Navigator, themes: ThemeState) {
  val scope = rememberCoroutineScope()
  var state by remember { mutableStateOf<TableState?>(null) }
  var notice by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(table) { table.changes.collect { state = read(table.view(me)) } }
  val actions = remember(table) {
    seatActions(nav, propose = { input -> notice = null; scope.launch { notice = table.propose(me, input) } }) { scope.launch { table.ready(me) } }
  }
  state?.let { SeatScreen(seatView(it, me, notice), themes.board, actions) }
}

@Composable
private fun GuestSeat(name: String, room: Room, nav: Navigator, themes: ThemeState, onRefused: (String) -> Unit) {
  val scope = rememberCoroutineScope()
  val seat = remember(room) { DuplicateSeat(name, room, scope) }
  DisposableEffect(seat) { seat.open(); onDispose { seat.close() } }
  val json by seat.view.collectAsState()
  val players by seat.players.collectAsState()
  val notice by seat.notice.collectAsState()
  val status by seat.status.collectAsState()
  val refusal by seat.refusal.collectAsState()
  LaunchedEffect(refusal) { refusal?.let(onRefused) }
  var state by remember { mutableStateOf<TableState?>(null) }
  LaunchedEffect(json) { json?.let { state = read(it) } }
  val actions = remember(seat) { seatActions(nav, propose = seat::propose, ready = seat::ready) }
  state?.let { SeatScreen(seatView(it, name, notice, status), themes.board, actions) }
    ?: GuestLobbyScreen(room.name, players, status, name) { onRefused("") }
}

private suspend fun read(json: String): TableState = withContext(Dispatchers.Default) { readTable(json) }

private fun seatActions(nav: Navigator, propose: (String) -> Unit, ready: () -> Unit): SeatActions = object : SeatActions {
  override fun propose(placement: Placement) = propose(placement.toString())
  override fun pass() = propose("pasar")
  override fun ready() = ready()
  override fun exit() = nav.back()
}
