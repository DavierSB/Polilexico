package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.PlacingBoard
import app.lexico.ui.board.PlacingRack
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.board.rememberTilePlacer
import app.lexico.ui.common.GameBar
import app.lexico.ui.common.NOT_IN_A_LINE
import app.lexico.ui.common.Notice
import app.lexico.ui.common.Pausable
import app.lexico.ui.common.rememberNotice

@Composable
fun ClassicScreen(view: ClassicView, style: BoardStyle, actions: ClassicActions, onTheme: (() -> Unit)? = null) {
  val c = rememberTilePlacer()
  LaunchedEffect(view.board, view.rack) { c.reset(view.board, view.rack) }
  val dialogs = rememberClassicDialogs()
  var message by rememberNotice(view.notice)
  Pausable(view.paused, actions::resume) { ClassicLayout(view, c, style, actions, dialogs, message, onTheme) { message = it } }
  ClassicDialogsShown(view, c, actions, dialogs)
}

@Composable
private fun ClassicLayout(
  view: ClassicView, c: TilePlacer, style: BoardStyle, actions: ClassicActions, dialogs: ClassicDialogs, message: String?,
  onTheme: (() -> Unit)?, onNotice: (String) -> Unit,
) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    TopBar(view, actions, dialogs, onTheme)
    OpponentBar(view, style)
    BoardWithBanner(view, c, style)
    MyBar(view, c, style)
    message?.let { Notice(it) }
    Controls(view, c, actions, dialogs, onNotice)
  }
}

@Composable
private fun TopBar(view: ClassicView, actions: ClassicActions, dialogs: ClassicDialogs, onTheme: (() -> Unit)?) {
  GameBar(
    bag = view.bag, onExit = actions::exit, onMoves = { dialogs.moves = true }, onBag = { dialogs.bag = true },
    onResign = if (view.end == null) ({ dialogs.resign = true }) else null,
    onPause = if (view.end == null && view.myClock != null) actions::pause else null, onTheme = onTheme,
  )
}

@Composable
private fun OpponentBar(view: ClassicView, style: BoardStyle) {
  val onTurn = view.turn == Side.OPPONENT
  PlayerBar(
    name = view.opponent, points = view.opponentScore, clock = view.opponentClock, onTurn = onTurn, thinking = onTurn,
    photo = { OpponentPhoto(view.opponent, 24.dp) },
  ) { OpponentRack(view.opponentRack, style) }
}

@Composable
private fun BoardWithBanner(view: ClassicView, c: TilePlacer, style: BoardStyle) {
  Box(contentAlignment = Alignment.Center) {
    PlacingBoard(c, style, enabled = view.turn == Side.ME, latestScore = view.latestScore, showScore = true)
    OpponentBanner(view.moves, view.opponent)
  }
}

@Composable
private fun MyBar(view: ClassicView, c: TilePlacer, style: BoardStyle) {
  PlayerBar(name = "Tú", points = view.myScore, clock = view.myClock, onTurn = view.turn == Side.ME)
  PlacingRack(c, style, enabled = view.turn == Side.ME)
}

@Composable
private fun Controls(view: ClassicView, c: TilePlacer, actions: ClassicActions, dialogs: ClassicDialogs, onNotice: (String) -> Unit) {
  val end = view.end
  if (end != null) return ResultPanel(view, end, onAnalyze = actions::analyze, onMenu = actions::exit)
  ActionButtons(
    c, myTurn = view.turn == Side.ME,
    onPlay = { c.placement()?.let(actions::play) ?: onNotice(NOT_IN_A_LINE) },
    onExchange = actions::exchange,
    onPass = { dialogs.pass = true },
  )
}
