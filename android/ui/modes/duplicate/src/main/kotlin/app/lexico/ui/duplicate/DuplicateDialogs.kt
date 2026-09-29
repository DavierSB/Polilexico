package app.lexico.ui.duplicate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.ui.common.BagDialog
import app.lexico.ui.common.ResignDialog

/**
 * Los dialogos de la partida, incluidos los que se abren solos: el cierre de cada ronda que
 * termine con la pantalla abierta (no las que venian de una partida continuada) y el final.
 */
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

/** Que dialogos estan abiertos. [round] es la ronda recien cerrada que se esta mostrando. */
@Stable
internal class DuplicateDialogs {
  var moves by mutableStateOf(false)
  var bag by mutableStateOf(false)
  var resign by mutableStateOf(false)
  var round: Round? by mutableStateOf(null)
  var end by mutableStateOf(false)

  /** Abre el final si la partida termino, o la ronda nueva si se cerro alguna desde `seen`. */
  fun announce(view: DuplicateView, seen: Int) {
    if (view.phase == Phase.Finished) end = true
    else if (view.rounds.size > seen) round = view.rounds.last()
  }
}

@Composable
internal fun DuplicateDialogsShown(view: DuplicateView, actions: DuplicateActions, dialogs: DuplicateDialogs) {
  dialogs.round?.let { RoundDialog(it) { dialogs.round = null } }
  if (dialogs.end) GameEndDialog(view) { dialogs.end = false }
  if (dialogs.moves) MovesDialog(view.rounds) { dialogs.moves = false }
  if (dialogs.bag) BagDialog("Quedan en la bolsa", view.bag) { dialogs.bag = false }
  if (dialogs.resign) ResignDialog(onResign = actions::resign, onContinue = { dialogs.resign = false })
}
