package app.lexico.ui.classic

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.model.Board
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile
import app.lexico.ui.board.ScrabbleBoard
import app.lexico.ui.common.BoardColumn
import app.lexico.ui.common.Header
import app.lexico.ui.common.Notice
import app.lexico.ui.common.SeatAvatar

data class WatchSeat(val name: String, val points: Int, val clock: Clock?, val onTurn: Boolean, val rack: List<String>)

data class WatchClassicView(
  val board: Board, val top: WatchSeat, val bottom: WatchSeat, val watchers: Int, val lastMove: String?, val result: String?,
)

@Composable
fun WatchClassicScreen(view: WatchClassicView, style: BoardStyle, onExit: () -> Unit) {
  BoardColumn(
    top = {
      Header("Observando", onExit) { WatchersBadge(view.watchers) }
      SeatBar(view.top, style)
    },
    board = { ScrabbleBoard(view.board, style = style) },
    bottom = {
      SeatBar(view.bottom, style)
      view.result?.let { Notice(it, bold = true) } ?: view.lastMove?.let { Notice(it) }
    },
  )
}

@Composable
fun WatchersBadge(count: Int) {
  if (count > 0) Text("👁 $count", fontWeight = FontWeight.Bold)
}

@Composable
private fun SeatBar(seat: WatchSeat, style: BoardStyle) {
  PlayerBar(seat.name, seat.points, seat.clock, seat.onTurn, photo = { SeatAvatar(seat.name, 24.dp) }) {
    seat.rack.forEach { RackTile(it, style, 24.dp) }
  }
}
