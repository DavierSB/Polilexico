package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.game.modes.classic.ClassicState
import app.lexico.game.modes.classic.ClassicGame
import app.lexico.game.modes.duplicate.table.TablePhase
import app.lexico.game.modes.duplicate.table.TableState
import app.lexico.menu.ThemeState
import app.lexico.model.Placement
import app.lexico.modes.classic.classicView
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
import app.lexico.ui.duplicate.Standing
import app.lexico.ui.duplicate.WatchDuplicateScreen
import app.lexico.ui.duplicate.WatchDuplicateView

@Composable
internal fun Watch(client: OnlineClient, info: RoomInfo, nav: Navigator, themes: ThemeState) {
  val watchers = client.watchers.collectAsState().value.map(::personItem)
  val leave = { client.leave(); nav.back() }
  if (info.duplicate) WatchTable(client, watchers, themes, leave) else WatchGame(client, info, watchers, themes, leave)
}

@Composable
private fun WatchGame(client: OnlineClient, info: RoomInfo, watchers: List<PersonItem>, themes: ThemeState, leave: () -> Unit) {
  val game by client.game.collectAsState()
  val current = game ?: return OnlineWaitScreen("Observando", "Conectando…", false, leave)
  WatchedGame(current, info.players, watchers, themes, leave)
}

@Composable
private fun WatchedGame(game: ClassicGame, players: List<String>, watchers: List<PersonItem>, themes: ThemeState, leave: () -> Unit) {
  val state by game.state.collectAsState()
  val actions = remember(leave) { watchActions(leave) }
  ClassicScreen(spectatorView(state, players, watchers), themes.board, actions)
}

private fun spectatorView(state: ClassicState, players: List<String>, watchers: List<PersonItem>): ClassicView {
  val view = classicView(state, players.getOrElse(1) { "" }, null, players.getOrElse(0) { "" }, handOff = false)
    .copy(local = false, watching = true, watchers = watchers)
  return if (state.result == null && !state.myTurn) fromSecondSeat(view, state) else view
}

private fun fromSecondSeat(view: ClassicView, state: ClassicState): ClassicView = view.copy(
  me = view.opponent, opponent = view.me, rack = state.opponentRack, opponentRack = OpponentRack.Hidden(state.rack.size),
  myScore = view.opponentScore, opponentScore = view.myScore, myClock = view.opponentClock, opponentClock = view.myClock,
  turn = Side.ME, unseen = view.unseen.without(state.opponentRack) + state.rack, moves = view.moves.map(::otherSide),
)

private fun otherSide(m: Move): Move =
  m.copy(side = if (m.side == Side.ME) Side.OPPONENT else Side.ME, myTotal = m.opponentTotal, opponentTotal = m.myTotal)

private fun List<String>.without(tiles: List<String>): List<String> {
  val left = toMutableList()
  tiles.forEach { left.remove(it) }
  return left
}

private fun watchActions(leave: () -> Unit): ClassicActions = object : ClassicActions {
  override fun play(placement: Placement) {}
  override fun exchange(tiles: List<String>) {}
  override fun pass() {}
  override fun resign() {}
  override fun exit() = leave()
  override fun analyze() {}
  override fun analyzeEndgame() {}
  override fun export() {}
  override fun pause() {}
  override fun resume() {}
}

@Composable
private fun WatchTable(client: OnlineClient, watchers: List<PersonItem>, themes: ThemeState, leave: () -> Unit) {
  val json by client.table.collectAsState()
  var table by remember { mutableStateOf<TableState?>(null) }
  LaunchedEffect(json) { json?.let { table = read(it) } }
  val shown = table ?: return OnlineWaitScreen("Observando", "Conectando…", false, leave)
  WatchDuplicateScreen(watchTableView(shown, watchers), themes.board, leave)
}

private fun watchTableView(t: TableState, watchers: List<PersonItem>): WatchDuplicateView {
  val totals = t.rounds.lastOrNull()?.entries.orEmpty().map { Standing(it.name, it.total) }.sortedByDescending { it.total }
  val round = if (t.phase == TablePhase.PLAYING) t.round else t.rounds.size
  return WatchDuplicateView(t.board, t.rack, round, t.phase == TablePhase.PLAYING, totals, watchers)
}
