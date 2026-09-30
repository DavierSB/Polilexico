package app.lexico.ui.classic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.common.BagCountDialog
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
  return dialogs
}

@Stable
internal class ClassicDialogs {
  var moves by mutableStateOf(false)
  var bag by mutableStateOf(false)
  var pass by mutableStateOf(false)
  var resign by mutableStateOf(false)
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
internal fun ClassicDialogsShown(view: ClassicView, c: TilePlacer, actions: ClassicActions, dialogs: ClassicDialogs) {
  val end = view.end
  if (dialogs.ending && end?.ending != null) EndingDialog(view, end.ending, dialogs::closeEnding)
  if (dialogs.result && end != null) ResultDialog(view, end) { dialogs.result = false }
  if (dialogs.moves) MovesDialog(view) { dialogs.moves = false }
  if (dialogs.bag) UnseenDialog(view) { dialogs.bag = false }
  if (dialogs.pass) PassDialog(c, actions, dialogs)
  if (dialogs.resign) ResignDialog(onResign = actions::resign, onContinue = { dialogs.resign = false })
}

@Composable
private fun UnseenDialog(view: ClassicView, close: () -> Unit) {
  val title = "Por salir (bolsa ${view.bag} + atril del rival)"
  if (LocalShowUnseen.current) BagDialog(title, view.unseen, close) else BagCountDialog(title, view.unseen.size, close)
}

@Composable
private fun PassDialog(c: TilePlacer, actions: ClassicActions, dialogs: ClassicDialogs) {
  ConfirmDialog(
    "¿Pasar el turno?", yes = "Pasar", no = "No",
    onYes = { dialogs.pass = false; c.recall(); actions.pass() },
    onNo = { dialogs.pass = false },
  )
}
