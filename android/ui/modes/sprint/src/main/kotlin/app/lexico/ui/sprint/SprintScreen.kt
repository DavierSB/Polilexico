package app.lexico.ui.sprint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.model.Board
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.PlacingBoard
import app.lexico.ui.board.PlacingRack
import app.lexico.ui.board.RackRenewal
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.board.rememberTilePlacer
import app.lexico.ui.common.BarTitle
import app.lexico.ui.common.ExitButton
import app.lexico.ui.common.Lives
import app.lexico.ui.common.NOT_IN_A_LINE
import app.lexico.ui.common.Notice
import app.lexico.ui.common.Pausable
import app.lexico.ui.common.PauseButton
import app.lexico.ui.common.ThemeButton
import app.lexico.ui.common.TurnClock
import app.lexico.ui.common.rememberNotice

/**
 * Scrabble Sprint: el tablero y el atril de la mano, con su reloj; al cerrarse la mano, sus
 * scrabbles (tocar uno lo pone en el tablero). Abajo, las vidas como corazones y las manos
 * resueltas.
 *
 * Solo dibuja la [view] y traduce toques en [actions].
 */
@Composable
fun SprintScreen(view: SprintView, style: BoardStyle, actions: SprintActions, onTheme: (() -> Unit)? = null) {
  val c = rememberTilePlacer(RackRenewal.WHOLE)
  var shown by remember(view.phase.result) { mutableStateOf(view.phase.result?.let { it.answer ?: it.bingos.firstOrNull() }) }
  val board = view.phase.hand?.board?.let { shownBoard(it, shown) }
  LaunchedEffect(board, view.phase.hand?.rack) { c.reset(board ?: Board.EMPTY, view.phase.hand?.rack.orEmpty()) }
  var message by rememberNotice(view.notice)
  Pausable(view.paused, actions::resume) {
    SprintLayout(view, c, style, actions, message, onTheme, onNotice = { message = it }, shown = shown, onShow = { shown = it })
  }
}

@Composable
private fun SprintLayout(
  view: SprintView, c: TilePlacer, style: BoardStyle, actions: SprintActions, message: String?, onTheme: (() -> Unit)?,
  onNotice: (String) -> Unit, shown: Bingo?, onShow: (Bingo) -> Unit,
) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    TopBar(view, actions, onTheme)
    if (view.phase.hand == null) Searching() else PlacingBoard(c, style, enabled = view.phase is Phase.Solving)
    PhasePanel(view, c, style, actions, onNotice, shown, onShow)
    message?.let { Notice(it, bold = true) }
    // Con los scrabbles a la vista, su lista ocupa todo lo que sobra; si no, los corazones van abajo.
    if (!view.phase.showsBingos) Spacer(Modifier.weight(1f))
    Lives(view.lives, view.maxLives, "Resueltas", view.solved, view.best)
  }
}

/** Salir, cambiar el tema y, mientras la serie sigue, pausar. */
@Composable
private fun TopBar(view: SprintView, actions: SprintActions, onTheme: (() -> Unit)?) {
  Row(Modifier.fillMaxWidth().height(44.dp), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
    ExitButton(actions::exit)
    BarTitle("Scrabble Sprint", Modifier.weight(1f).padding(start = 6.dp))
    if (view.phase !is Phase.Finished) PauseButton(actions::pause)
    if (onTheme != null) ThemeButton(onTheme)
  }
}

/** Mientras HastyBot busca la mano, en el sitio del tablero. */
@Composable
private fun Searching() {
  Column(Modifier.fillMaxWidth().aspectRatio(1f), Arrangement.spacedBy(12.dp, Alignment.CenterVertically), Alignment.CenterHorizontally) {
    CircularProgressIndicator()
    Text("HastyBot busca un scrabble…")
  }
}

/** Bajo el tablero: el reloj, el atril y los botones de la mano; o como fue y sus scrabbles. */
@Composable
private fun ColumnScope.PhasePanel(
  view: SprintView, c: TilePlacer, style: BoardStyle, actions: SprintActions, onNotice: (String) -> Unit,
  shown: Bingo?, onShow: (Bingo) -> Unit,
) {
  when (val phase = view.phase) {
    Phase.Searching -> Unit
    is Phase.Solving -> Solving(
      phase, c, style, onPlay = { c.placement()?.let(actions::propose) ?: onNotice(NOT_IN_A_LINE) }, onGiveUp = actions::giveUp,
    )
    is Phase.Revealed -> Box(Modifier.weight(1f)) { Revealed(phase.result, shown, onShow, actions::next) }
    is Phase.Finished -> Box(Modifier.weight(1f)) { Finished(phase.result, view.solved, view.record, shown, onShow, actions) }
  }
}

/** La mano en juego: el reloj, el atril y sus botones. */
@Composable
private fun Solving(phase: Phase.Solving, c: TilePlacer, style: BoardStyle, onPlay: () -> Unit, onGiveUp: () -> Unit) {
  TurnClock(phase.remainingMs)
  PlacingRack(c, style, enabled = true)
  SolvingButtons(c, onPlay, onGiveUp)
}

/** El tablero con el scrabble elegido puesto (y resaltado). */
private fun shownBoard(board: Board, bingo: Bingo?): Board =
  bingo?.let { runCatching { board.play(it.placement) }.getOrNull() } ?: board

/** Mano cerrada o serie terminada: se ven los scrabbles de la mano. */
private val Phase.showsBingos: Boolean
  get() = this is Phase.Revealed || this is Phase.Finished

private val Phase.hand: Hand?
  get() = when (this) {
    is Phase.Solving -> hand
    is Phase.Revealed -> hand
    is Phase.Finished -> hand
    Phase.Searching -> null
  }

private val Phase.result: HandResult?
  get() = when (this) {
    is Phase.Revealed -> result
    is Phase.Finished -> result
    else -> null
  }
