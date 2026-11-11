package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexico.games.export.TableExport
import app.lexico.sound.SeatCues
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.duplicate.SeatActions
import app.lexico.ui.duplicate.SeatScreen
import app.lexico.ui.duplicate.SeatView

@Composable
internal fun ExportableSeat(
  view: SeatView, style: BoardStyle, actions: SeatActions, onAdjourn: (() -> Unit)? = null, onAbandon: (() -> Unit)? = null, online: Boolean = false,
) {
  var exporting by remember { mutableStateOf(false) }
  SeatScreen(view, style, actions, { exporting = true }, onAdjourn, onAbandon, online)
  SeatCues(view)
  if (exporting) TableExport(view, style) { exporting = false }
}
