package app.lexico.ui.duplicate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.PlacingBoard
import app.lexico.ui.board.PlacingRack
import app.lexico.ui.board.RackRenewal
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.board.rememberTilePlacer
import app.lexico.ui.common.BoardColumn
import app.lexico.ui.common.EndButtons
import app.lexico.ui.common.WordSection
import app.lexico.ui.common.GameBar
import app.lexico.ui.common.NOT_IN_A_LINE
import app.lexico.ui.common.Notice
import app.lexico.ui.common.Pausable
import app.lexico.ui.common.TurnClock
import app.lexico.ui.common.rememberNotice

@Composable
fun DuplicateScreen(view: DuplicateView, style: BoardStyle, actions: DuplicateActions, onTheme: (() -> Unit)? = null) {
  val c = rememberTilePlacer(RackRenewal.MASTER_PLAYS)
  LaunchedEffect(view.board, rackOf(view.phase)) { c.reset(view.board, rackOf(view.phase)) }
  val dialogs = rememberDuplicateDialogs(view)
  var message by rememberNotice(view.notice)
  Pausable(view.paused, actions::resume) { DuplicateLayout(view, c, style, actions, dialogs, message, onTheme) { message = it } }
  DuplicateDialogsShown(view, c, actions, dialogs)
}

@Composable
private fun DuplicateLayout(
  view: DuplicateView, c: TilePlacer, style: BoardStyle, actions: DuplicateActions, dialogs: DuplicateDialogs, message: String?,
  onTheme: (() -> Unit)?, onNotice: (String) -> Unit,
) {
  BoardColumn(
    top = { TopBar(view, actions, dialogs, onTheme) },
    board = { PlacingBoard(c, style, enabled = view.phase is Phase.Playing) },
    bottom = {
      PhasePanel(view.phase, c, style, actions)
      message?.let { Notice(it, bold = true) }
      PhaseButtons(view, c, actions, dialogs, onNotice)
    },
    footer = { Scoreboard(view) },
  )
}

@Composable
private fun TopBar(view: DuplicateView, actions: DuplicateActions, dialogs: DuplicateDialogs, onTheme: (() -> Unit)?) {
  GameBar(
    bag = view.bag.size, onExit = actions::exit, onMoves = { dialogs.moves = true }, onBag = { dialogs.bag = true },
    onResign = if (view.phase != Phase.Finished) ({ dialogs.resign = true }) else null,
    onPause = if (view.phase != Phase.Finished) actions::pause else null, onTheme = onTheme,
  )
}

@Composable
private fun PhasePanel(phase: Phase, c: TilePlacer, style: BoardStyle, actions: DuplicateActions) {
  when (phase) {
    is Phase.Waiting -> ShowRackButton(phase.turnMs, actions::showRack)
    is Phase.InvalidRack -> InvalidRack(phase.rack, style)
    is Phase.ManyInvalid -> ManyInvalidDialog(phase.rack, style)
    is Phase.Playing -> ClockAndRack(phase.remainingMs, c, style, enabled = true)
    is Phase.Confirming -> ClockAndRack(phase.remainingMs, c, style, enabled = false)
    Phase.Finished -> Unit
  }
}

@Composable
private fun ClockAndRack(remainingMs: Long?, c: TilePlacer, style: BoardStyle, enabled: Boolean) {
  remainingMs?.let { TurnClock(it) }
  PlacingRack(c, style, enabled = enabled)
}

@Composable
private fun PhaseButtons(view: DuplicateView, c: TilePlacer, actions: DuplicateActions, dialogs: DuplicateDialogs, onNotice: (String) -> Unit) {
  when (val phase = view.phase) {
    is Phase.Playing -> PlayingButtons(
      c, onPlay = { c.placement()?.let(actions::propose) ?: onNotice(NOT_IN_A_LINE) }, onPass = { dialogs.pass = true },
    )
    is Phase.Confirming -> ConfirmingButtons(phase, actions::cancel)
    Phase.Finished -> EndButtons(onAnalyze = actions::analyze, onMenu = actions::exit, onExport = actions::export, words = playedWords(view))
    else -> Unit
  }
}

private fun rackOf(phase: Phase): List<String> = when (phase) {
  is Phase.Playing -> phase.rack
  is Phase.Confirming -> phase.rack
  else -> emptyList()
}

private fun playedWords(view: DuplicateView): List<WordSection> =
  listOf(WordSection("Máster", view.rounds.map { it.master.text }), WordSection("Tú", view.rounds.map { it.mine.text }))
