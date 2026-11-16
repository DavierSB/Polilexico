package app.lexico.navigation

import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import app.lexico.ui.common.LocalChat
import app.lexico.ui.common.ContactGroup
import app.lexico.ui.common.ROOM_CONTACT
import app.lexico.games.export.GameExport
import app.lexico.game.records.FinishedGames
import app.lexico.ui.common.ChatEntry
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.game.modes.classic.ClassicState
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.duplicate.table.TableState
import app.lexico.menu.ThemeState
import app.lexico.model.Placement
import app.lexico.modes.classic.classicView
import app.lexico.modes.classic.invalidNotice
import app.lexico.online.OnlineClient
import app.lexico.online.RoomInfo
import app.lexico.ui.classic.ClassicActions
import app.lexico.ui.classic.ClassicScreen
import app.lexico.ui.classic.ClassicView
import app.lexico.ui.classic.Move
import app.lexico.ui.classic.OpponentRack
import app.lexico.ui.classic.Side
import app.lexico.ui.common.OnlineWaitScreen
import app.lexico.ui.common.PersonItem
import app.lexico.modes.duplicate.seatView
import app.lexico.ui.duplicate.SeatActions

@Composable
internal fun Watch(client: OnlineClient, info: RoomInfo, nav: Navigator, themes: ThemeState, finished: FinishedGames) {
  val watchers = client.watchers.collectAsState().value.map(::personItem)
  val leave = { client.leave(); nav.back() }
  WatchChat(client, info) {
    if (info.duplicate) WatchTable(client, watchers, themes, leave) { nav.replace(Screen.Review(it)) } else WatchGame(client, info, watchers, themes, WatchExits(leave, nav, finished))
  }
  WatchingNotice(info.id)
}

@Composable
private fun WatchChat(client: OnlineClient, info: RoomInfo, content: @Composable () -> Unit) {
  val composer = remember { Composer() }
  val lobby by client.lobby.collectAsState()
  val watchers by client.watchers.collectAsState()
  val list = listOfNotNull(ROOM_CONTACT.takeIf { info.duplicate && info.players.isNotEmpty() }) + contacts(lobby, info.players, watchers)
  val onTurn = onTurn(client, info)?.let(::plainName)
  val fallback = list.find { it.group == ContactGroup.RIVALS && it.name == onTurn } ?: list.firstOrNull { it.group == ContactGroup.RIVALS }
  val latest by rememberUpdatedState(fallback)
  val entry = remember { ChatEntry { composer.write(latest) } }
  CompositionLocalProvider(LocalChat provides entry) {
    ProfileHost(rememberOnline(), ProfileContext(client, composer, {}, inGame = true)) { content(); MessageDialogs(client, list, fallback, composer) }
  }
}

@Composable
private fun onTurn(client: OnlineClient, info: RoomInfo): String? {
  if (info.duplicate) return null
  val state = client.game.collectAsState().value?.state?.collectAsState()?.value ?: return null
  return info.players.getOrNull(if (state.myTurn) 0 else 1)
}

@Composable
private fun WatchingNotice(roomId: String) {
  var shown by remember(roomId) { mutableStateOf(true) }
  if (!shown) return
  AlertDialog(
    onDismissRequest = { shown = false },
    icon = { Text("👁", fontSize = 32.sp) },
    title = { Text("Estás solo observando") },
    confirmButton = { TextButton(onClick = { shown = false }) { Text("Entendido") } },
  )
}

private class WatchExits(val leave: () -> Unit, val nav: Navigator, val finished: FinishedGames)

@Composable
private fun WatchGame(client: OnlineClient, info: RoomInfo, watchers: List<PersonItem>, themes: ThemeState, exits: WatchExits) {
  val game by client.game.collectAsState()
  val current = game ?: return OnlineWaitScreen("Observando", "Conectando…", false, exits.leave)
  WatchedGame(current, info.players, watchers, themes, exits)
}

@Composable
private fun WatchedGame(game: ClassicGame, players: List<String>, watchers: List<PersonItem>, themes: ThemeState, exits: WatchExits) {
  val state by game.state.collectAsState()
  var exporting by remember { mutableStateOf<String?>(null) }
  val record = state.result?.recordPath?.takeIf { it.isNotEmpty() }
  val actions = watchActions(exits, record) { exporting = it }
  ClassicScreen(spectatorView(state, players, watchers), themes.board, actions)
  exporting?.let { GameExport(it, exits.finished, themes.board) { exporting = null } }
}

private fun spectatorView(state: ClassicState, players: List<String>, watchers: List<PersonItem>): ClassicView {
  val notice = invalidNotice(state) { m -> players.getOrElse(if (m.byMe) 0 else 1) { "" } }
  val view = classicView(state, players.getOrElse(1) { "" }, notice, players.getOrElse(0) { "" }, handOff = false)
    .copy(local = false, watching = true, watchers = watchers, opponentRack = OpponentRack.Visible(state.opponentRack), unseen = state.unseen.without(state.opponentRack))
  return if (state.result == null && !state.myTurn) fromSecondSeat(view, state) else view
}

private fun fromSecondSeat(view: ClassicView, state: ClassicState): ClassicView = view.copy(
  me = view.opponent, opponent = view.me, rack = state.opponentRack, opponentRack = OpponentRack.Visible(state.rack),
  myScore = view.opponentScore, opponentScore = view.myScore, myClock = view.opponentClock, opponentClock = view.myClock,
  turn = Side.ME, moves = view.moves.map(::otherSide),
)

private fun otherSide(m: Move): Move =
  m.copy(side = if (m.side == Side.ME) Side.OPPONENT else Side.ME, myTotal = m.opponentTotal, opponentTotal = m.myTotal)

private fun List<String>.without(tiles: List<String>): List<String> {
  val left = toMutableList()
  tiles.forEach { left.remove(it) }
  return left
}

private fun watchActions(exits: WatchExits, record: String?, export: (String) -> Unit): ClassicActions = object : ClassicActions {
  override fun play(placement: Placement) {}
  override fun exchange(tiles: List<String>) {}
  override fun pass() {}
  override fun resign() {}
  override fun exit() = exits.leave()
  override fun analyze() { record?.let { exits.nav.replace(Screen.Review(it)) } }
  override fun analyzeEndgame() { record?.let { exits.nav.replace(Screen.Review(it, atEndgame = true)) } }
  override fun export() { record?.let(export) }
  override fun pause() {}
  override fun resume() {}
}

@Composable
private fun WatchTable(client: OnlineClient, watchers: List<PersonItem>, themes: ThemeState, leave: () -> Unit, review: (String) -> Unit) {
  val json by client.table.collectAsState()
  val record by client.record.collectAsState()
  var table by remember { mutableStateOf<TableState?>(null) }
  LaunchedEffect(json) { json?.let { table = read(it) } }
  val shown = table ?: return OnlineWaitScreen("Observando", "Conectando…", false, leave)
  val actions = remember(leave) { watchSeatActions(leave) }
  ExportableSeat(seatView(shown, "", null).copy(watching = true, watchers = watchers), themes.board, actions, onAnalyze = record?.let { path -> { review(path) } })
}

private fun watchSeatActions(leave: () -> Unit): SeatActions = object : SeatActions {
  override fun propose(placement: Placement) {}
  override fun ready() {}
  override fun exit() = leave()
}
