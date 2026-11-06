package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.PlacingBoard
import app.lexico.ui.board.PlacingRack
import app.lexico.ui.board.RackRenewal
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.board.rememberTilePlacer
import app.lexico.ui.common.BagCountDialog
import app.lexico.ui.common.BoardColumn
import app.lexico.ui.common.ConfirmDialog
import app.lexico.ui.common.GameBar
import app.lexico.ui.common.NOT_IN_A_LINE
import app.lexico.ui.common.Notice
import app.lexico.ui.common.RoundBar
import app.lexico.ui.common.rememberNotice
import kotlinx.coroutines.delay

@Composable
fun SeatScreen(view: SeatView, style: BoardStyle, actions: SeatActions, onExport: (() -> Unit)? = null, onAdjourn: (() -> Unit)? = null) {
  val c = rememberTilePlacer(RackRenewal.MASTER_PLAYS)
  LaunchedEffect(view.board, view.rack) { c.reset(view.board, view.rack) }
  var message by rememberNotice(view.notice)
  var chart by remember { mutableStateOf(false) }
  var moves by remember { mutableStateOf(false) }
  var bag by remember { mutableStateOf(false) }
  BoardColumn(
    top = {
      GameBar(bag = view.bag, onExit = actions::exit, onMoves = { moves = true }, onBag = { bag = true }, onResign = null, onStandings = { chart = true }, watchers = view.watchers,
        onAdjourn = onAdjourn.takeIf { view.phase != SeatPhase.FINISHED && !view.watching },
      )
    },
    board = { PlacingBoard(c, style, enabled = view.phase == SeatPhase.PLAYING && !view.watching, lookUpWhenIdle = view.phase == SeatPhase.FINISHED) },
    bottom = { SeatBottom(view, c, style, actions, message, onExport) { message = it } },
  )
  if (chart) ChartDialog(view) { chart = false }
  if (moves) MovesDialog(movesOf(view), if (view.watching) "Mejor" else "Tú") { moves = false }
  if (bag) BagCountDialog("En la bolsa", view.bag) { bag = false }
  SeatVerdictOnce(view)
}

@Composable
private fun SeatBottom(
  view: SeatView, c: TilePlacer, style: BoardStyle, actions: SeatActions, message: String?, onExport: (() -> Unit)?, onNotice: (String) -> Unit,
) {
  view.status?.let { Notice(it, bold = true) }
  when {
    view.phase == SeatPhase.PLAYING && view.watching -> WatchingRound(view, c, style)
    view.phase == SeatPhase.PLAYING -> Playing(view, c, style, actions, message, onNotice)
    view.phase == SeatPhase.FINISHED -> Finished(view, actions, onExport)
    view.iAmReady -> WaitingRound(view)
    else -> RoundResults(view, actions)
  }
}

@Composable
private fun Playing(view: SeatView, c: TilePlacer, style: BoardStyle, actions: SeatActions, message: String?, onNotice: (String) -> Unit) {
  RoundBar(view.round, view.remainingMs?.let { rememberLeft(it) })
  PlacingRack(c, style, enabled = true)
  Text(view.proposal?.let { "Tu jugada: ${it.text} · ${it.points} pts" } ?: "Aún no has jugado", fontWeight = FontWeight.Bold)
  message?.let { Notice(it, bold = true) }
  var pass by remember { mutableStateOf(false) }
  PlayingButtons(c, onPlay = { c.placement()?.let(actions::propose) ?: onNotice(NOT_IN_A_LINE) }, onPass = { pass = true })
  if (pass) ConfirmDialog(
    "¿Pasar el turno?", yes = "Pasar", no = "No",
    onYes = { pass = false; c.recall(); actions.pass() }, onNo = { pass = false },
  )
}

@Composable
private fun WatchingRound(view: SeatView, c: TilePlacer, style: BoardStyle) {
  RoundBar(view.round, view.remainingMs?.let { rememberLeft(it) })
  PlacingRack(c, style, enabled = false)
  if (view.live) Table(view)
}

@Composable
private fun RoundResults(view: SeatView, actions: SeatActions) {
  Text("Resultados de la ronda ${view.round}", style = MaterialTheme.typography.titleSmall)
  Table(view)
  if (!view.watching) Button(modifier = Modifier.fillMaxWidth(), onClick = actions::ready) { Text("Comenzar nueva ronda") }
}

@Composable
private fun WaitingRound(view: SeatView) {
  Text("Esperando la nueva ronda: ${view.ready} de ${view.waiting} listos", style = MaterialTheme.typography.titleSmall)
  if (view.live) Table(view)
}

@Composable
private fun Finished(view: SeatView, actions: SeatActions, onExport: (() -> Unit)?) {
  Text("Partida terminada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
  Table(view)
  onExport?.let { OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = it) { Text("Exportar") } }
  Button(modifier = Modifier.fillMaxWidth(), onClick = actions::exit) { Text("Salir") }
}

@Composable
private fun rememberLeft(remainingMs: Long): Long {
  var left by remember(remainingMs) { mutableLongStateOf(remainingMs) }
  LaunchedEffect(remainingMs) {
    val start = System.currentTimeMillis()
    while (left > 0) {
      delay(TICK_MS)
      left = (remainingMs - (System.currentTimeMillis() - start)).coerceAtLeast(0)
    }
  }
  return left
}

@Composable
private fun Table(view: SeatView) = if (view.watching) LeaderboardTable(view) else StandingsTable(view)

private fun movesOf(view: SeatView): List<Round> = view.rounds.mapNotNull { r ->
  val master = r.entries.find { it.name == MASTER_NAME } ?: return@mapNotNull null
  val other = (if (view.watching) bestOf(r) else r.entries.find { it.name == view.me }) ?: return@mapNotNull null
  Round(r.number, RoundPlay(master.text, master.points), RoundPlay(other.text, other.points), hit = other.points > 0 && other.points == master.points)
}

private fun bestOf(r: SeatRound): SeatEntry? = r.entries.filter { it.name != MASTER_NAME }.maxByOrNull { it.points }

private const val TICK_MS = 200L
