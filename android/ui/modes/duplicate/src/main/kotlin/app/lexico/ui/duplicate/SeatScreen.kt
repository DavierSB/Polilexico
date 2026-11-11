package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
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
import app.lexico.ui.common.BagDialog
import app.lexico.ui.common.BoardColumn
import app.lexico.ui.common.ConfirmDialog
import app.lexico.ui.common.GameBar
import app.lexico.ui.common.LocalChat
import app.lexico.ui.common.NOT_IN_A_LINE
import app.lexico.ui.common.Notice
import app.lexico.ui.common.RoundBar
import app.lexico.ui.common.rememberNotice
import kotlinx.coroutines.delay

@Composable
fun SeatScreen(
  view: SeatView, style: BoardStyle, actions: SeatActions, onExport: (() -> Unit)? = null, onAdjourn: (() -> Unit)? = null, onAbandon: (() -> Unit)? = null,
  online: Boolean = false,
) {
  val c = rememberTilePlacer(RackRenewal.MASTER_PLAYS)
  LaunchedEffect(view.board, view.rack) { c.reset(view.board, view.rack) }
  var message by rememberNotice(view.notice)
  var chart by remember { mutableStateOf(false) }
  var coloredChart by remember { mutableStateOf(false) }
  var moves by remember { mutableStateOf(false) }
  var bag by remember { mutableStateOf(false) }
  var abandoning by remember { mutableStateOf(false) }
  val canAbandon = onAbandon != null && onAdjourn == null && view.phase != SeatPhase.FINISHED && !view.watching
  var standings by remember { mutableStateOf(false) }
  val openStandings = { if (online && !view.watching) standings = true else chart = true }
  val bar: @Composable () -> Unit = {
    GameBar(bag = view.bag, onExit = actions::exit, onMoves = { moves = true }, onBag = { bag = true }, onResign = { abandoning = true }.takeIf { canAbandon },
      onStandings = openStandings.takeIf { view.watching || view.phase != SeatPhase.PLAYING }, watchers = view.watchers,
      onAdjourn = onAdjourn.takeIf { view.phase != SeatPhase.FINISHED && !view.watching },
      onChat = LocalChat.current?.onOpen?.takeIf { view.watching },
      onChart = { coloredChart = true }.takeIf { online && !view.watching },
    )
  }
  var results by remember { mutableStateOf(false) }
  BoardColumn(
    top = { bar() },
    board = { PlacingBoard(c, style, enabled = view.phase == SeatPhase.PLAYING && !view.watching, lookUpWhenIdle = view.phase == SeatPhase.FINISHED) },
    bottom = { SeatBottom(view, c, style, actions, message, onExport, { results = true }) { message = it } },
    footer = {
      if (view.phase == SeatPhase.PLAYING && !view.watching) {
        PlayingButtons(c, onPlay = { c.placement()?.let(actions::propose) ?: run { message = NOT_IN_A_LINE } }, onStandings = openStandings)
      }
    },
  )
  RoundResultsPopup(view, actions, results) { results = it }
  if (chart) ChartDialog(view) { chart = false }
  if (coloredChart) ChartDialog(view, colored = true) { coloredChart = false }
  if (moves) MovesDialog(movesOf(view), if (view.watching) "Mejor" else "Tú") { moves = false }
  if (bag) BagDialog("Quedan en la bolsa", view.unseen) { bag = false }
  if (standings) StandingsDialog(view) { standings = false }
  if (abandoning) ConfirmDialog("¿Seguro que quieres abandonar la partida?", yes = "Abandonar", no = "Seguir", onYes = { abandoning = false; onAbandon?.invoke() }) { abandoning = false }
  SeatVerdictOnce(view, { actions.exit() }.takeIf { online })
}

@Composable
private fun SeatBottom(
  view: SeatView, c: TilePlacer, style: BoardStyle, actions: SeatActions, message: String?, onExport: (() -> Unit)?, onShowResults: () -> Unit,
  onNotice: (String) -> Unit,
) {
  view.status?.let { Notice(it, bold = true) }
  when {
    view.phase == SeatPhase.PLAYING && view.watching -> WatchingRound(view, c, style)
    view.phase == SeatPhase.PLAYING -> Playing(view, c, style, actions, message, onNotice)
    view.phase == SeatPhase.FINISHED -> Finished(view, actions, onExport)
    else -> RoundEnd(view, actions, onShowResults)
  }
}

@Composable
private fun Playing(view: SeatView, c: TilePlacer, style: BoardStyle, actions: SeatActions, message: String?, onNotice: (String) -> Unit) {
  RoundBar(view.round, view.remainingMs?.let { rememberLeft(it) })
  PlacingRack(c, style, enabled = true)
  ProposalCard(view.proposal)
  message?.let { Notice(it, bold = true) }
}

@Composable
private fun WatchingRound(view: SeatView, c: TilePlacer, style: BoardStyle) {
  RoundBar(view.round, view.remainingMs?.let { rememberLeft(it) })
  PlacingRack(c, style, enabled = false)
  if (view.live) Table(view)
}

@Composable
private fun RoundEnd(view: SeatView, actions: SeatActions, onShow: () -> Unit) {
  Text("Resultados de la ronda ${view.round}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
  OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onShow) { Text("Ver resultados") }
  when {
    view.watching -> Unit
    view.iAmReady -> Text("Esperando la nueva ronda: ${view.ready} de ${view.waiting} listos", style = MaterialTheme.typography.titleSmall)
    else -> Button(modifier = Modifier.fillMaxWidth(), onClick = actions::ready) { Text("Comenzar nueva ronda") }
  }
}

@Composable
private fun ResultsDialog(view: SeatView, actions: SeatActions, close: () -> Unit) {
  val canStart = !view.watching && !view.iAmReady && view.phase == SeatPhase.RESULTS
  AlertDialog(
    onDismissRequest = close, title = { Text("Resultados de la ronda ${view.round}") },
    text = { if (view.watching) LeaderboardTable(view, maxHeight = DIALOG_HEIGHT) else StandingsTable(view, Modifier.heightIn(max = DIALOG_HEIGHT.dp)) },
    confirmButton = {
      if (canStart) Button(onClick = { close(); actions.ready() }) { Text("Comenzar nueva ronda") }
      else TextButton(onClick = close) { Text("Cerrar") }
    },
    dismissButton = { if (canStart) TextButton(onClick = close) { Text("Cerrar") } },
  )
}

@Composable
private fun RoundResultsPopup(view: SeatView, actions: SeatActions, open: Boolean, setOpen: (Boolean) -> Unit) {
  var shown by remember { mutableStateOf(0) }
  LaunchedEffect(view.phase, view.round) {
    if (view.phase == SeatPhase.RESULTS && view.round != shown) {
      shown = view.round
      setOpen(true)
    }
  }
  if (open && view.phase == SeatPhase.RESULTS) ResultsDialog(view, actions) { setOpen(false) }
}

private const val DIALOG_HEIGHT = 460

@Composable
private fun StandingsDialog(view: SeatView, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close, title = { Text("Posiciones") },
    text = { LeaderboardTable(view, maxHeight = DIALOG_HEIGHT, me = view.me) },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
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
