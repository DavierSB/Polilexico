package app.lexico.ui.classic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.common.BagDialog
import app.lexico.ui.common.ConfirmDialog
import app.lexico.ui.common.ResignDialog

@Composable
internal fun rememberClassicDialogs(view: ClassicView): ClassicDialogs {
  val dialogs = remember { ClassicDialogs() }
  var wasOver: Boolean? by remember { mutableStateOf(null) }
  val end = view.end
  LaunchedEffect(end != null) {
    if (wasOver == false && end != null) dialogs.announce(end)
    wasOver = end != null
  }
  WatchInvalid(view, dialogs)
  return dialogs
}

@Composable
private fun WatchInvalid(view: ClassicView, dialogs: ClassicDialogs) {
  var seen by remember { mutableIntStateOf(-1) }
  val moves = view.moves
  LaunchedEffect(moves.size) {
    if (seen >= 0 && !view.watching) moves.drop(seen).lastOrNull { it.type == MoveType.INVALID }?.let { dialogs.invalid = it }
    seen = moves.size
  }
}

@Stable
internal class ClassicDialogs {
  var moves by mutableStateOf(false)
  var bag by mutableStateOf(false)
  var pass by mutableStateOf(false)
  var resign by mutableStateOf(false)
  var invalid by mutableStateOf<Move?>(null)
  var ending by mutableStateOf(false)
  var result by mutableStateOf(false)

  fun announce(end: GameEnd) {
    if (end.ending != null) ending = true else result = true
  }

  fun closeEnding() {
    ending = false
    result = true
  }
}

@Composable
internal fun ClassicDialogsShown(view: ClassicView, c: TilePlacer, actions: ClassicActions, dialogs: ClassicDialogs, style: BoardStyle) {
  val end = view.end
  if (dialogs.ending && end?.ending != null) EndingDialog(view, end.ending, dialogs::closeEnding)
  if (dialogs.result && end != null) ResultPopup(view, end, actions) { dialogs.result = false }
  if (dialogs.moves) MovesDialog(view) { dialogs.moves = false }
  if (dialogs.bag) UnseenDialog(view) { dialogs.bag = false }
  if (dialogs.pass) PassDialog(c, actions, dialogs)
  if (dialogs.resign) ResignDialog(onResign = actions::resign, onContinue = { dialogs.resign = false })
  dialogs.invalid?.let { InvalidMoveDialog(it, style) { dialogs.invalid = null } }
}

@Composable
private fun UnseenDialog(view: ClassicView, close: () -> Unit) {
  BagDialog(view.bag, view.unseen.size - view.bag, view.unseen.takeIf { LocalShowUnseen.current }, close)
}

@Composable
private fun PassDialog(c: TilePlacer, actions: ClassicActions, dialogs: ClassicDialogs) {
  ConfirmDialog(
    "¿Pasar el turno?", yes = "Pasar", no = "No",
    onYes = { dialogs.pass = false; c.recall(); actions.pass() },
    onNo = { dialogs.pass = false },
  )
}
