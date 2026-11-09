package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.games.export.BoardExport
import app.lexico.sound.SeatCues
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.duplicate.SeatActions
import app.lexico.ui.duplicate.SeatScreen
import app.lexico.ui.duplicate.SeatView

private const val MASTER = "Máster"
private const val SHOWN_PLACES = 3

@Composable
internal fun ExportableSeat(view: SeatView, style: BoardStyle, actions: SeatActions, onAdjourn: (() -> Unit)? = null, onAbandon: (() -> Unit)? = null) {
  var exporting by remember { mutableStateOf(false) }
  SeatScreen(view, style, actions, { exporting = true }, onAdjourn, onAbandon)
  SeatCues(view)
  if (exporting) BoardExport(view.board, exportTitle(view), exportCaption(view), style) { exporting = false }
}

private fun players(view: SeatView) = view.standings.filter { it.name != MASTER }

private fun exportTitle(view: SeatView): String {
  val top = players(view).firstOrNull() ?: return "Duplicada"
  return "Duplicada · ganó ${top.name} con ${top.total}"
}

private fun exportCaption(view: SeatView): String =
  players(view).take(SHOWN_PLACES).mapIndexed { i, e -> "${i + 1}. ${e.name} ${e.total}" }.joinToString("  ·  ")
