package app.lexico.ui.classic

import androidx.compose.runtime.Composable
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
internal fun rememberClassicDialogs(): ClassicDialogs = remember { ClassicDialogs() }

/** Que dialogos de la partida estan abiertos. */
@Stable
internal class ClassicDialogs {
  var moves by mutableStateOf(false)
  var bag by mutableStateOf(false)
  var pass by mutableStateOf(false)
  var resign by mutableStateOf(false)
}

/** Los dialogos abiertos: movidas, bolsa y las confirmaciones de pasar y abandonar. */
@Composable
internal fun ClassicDialogsShown(view: ClassicView, c: TilePlacer, actions: ClassicActions, dialogs: ClassicDialogs) {
  if (dialogs.moves) MovesDialog(view.moves, view.opponent) { dialogs.moves = false }
  if (dialogs.bag) UnseenDialog(view) { dialogs.bag = false }
  if (dialogs.pass) PassDialog(c, actions, dialogs)
  if (dialogs.resign) ResignDialog(onResign = actions::resign, onContinue = { dialogs.resign = false })
}

/** Las fichas por salir o, si las opciones no las muestran, solo cuantas son. */
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
