package app.lexico.ui.duplicate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.common.BagDialog
import app.lexico.ui.common.ConfirmDialog
import app.lexico.ui.common.ResignDialog

@Composable
internal fun rememberDuplicateDialogs(view: DuplicateView): DuplicateDialogs {
  val dialogs = remember { DuplicateDialogs() }
  var seen by remember { mutableIntStateOf(-1) }
  val finished = view.phase == Phase.Finished
  LaunchedEffect(view.rounds.size, finished) {
    if (seen >= 0) dialogs.announce(view, seen)
    seen = view.rounds.size
  }
  return dialogs
}

@Stable
internal class DuplicateDialogs {
  var moves by mutableStateOf(false)
  var bag by mutableStateOf(false)
  var resign by mutableStateOf(false)
  var pass by mutableStateOf(false)
  var round: Round? by mutableStateOf(null)
  var end by mutableStateOf(false)
  var invalids by mutableStateOf(false)

  fun announce(view: DuplicateView, seen: Int) {
    pass = false
    if (view.phase == Phase.Finished) finish(view)
    else if (view.rounds.size > seen) round = view.rounds.last()
  }

  private fun finish(view: DuplicateView) {
    if (view.revealAtEnd) invalids = true else end = true
  }

  fun closeInvalids() {
    invalids = false
    end = true
  }
}

@Composable
internal fun DuplicateDialogsShown(view: DuplicateView, c: TilePlacer, actions: DuplicateActions, dialogs: DuplicateDialogs) {
  dialogs.round?.let { RoundDialog(it) { dialogs.round = null } }
  if (dialogs.invalids) InvalidPlaysDialog(view.invalidPlays, dialogs::closeInvalids)
  if (dialogs.end) GameEndDialog(view) { dialogs.end = false }
  if (dialogs.moves) MovesDialog(view.rounds) { dialogs.moves = false }
  if (dialogs.bag) BagDialog("Quedan en la bolsa", view.bag) { dialogs.bag = false }
  if (dialogs.resign) ResignDialog(onResign = actions::resign, onContinue = { dialogs.resign = false })
  if (dialogs.pass && view.phase is Phase.Playing) PassDialog(c, actions, dialogs)
}

@Composable
private fun PassDialog(c: TilePlacer, actions: DuplicateActions, dialogs: DuplicateDialogs) {
  ConfirmDialog(
    "¿Seguro que quieres pasar?", yes = "Pasar", no = "No",
    onYes = { dialogs.pass = false; c.recall(); actions.pass() },
    onNo = { dialogs.pass = false },
  )
}
