package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.model.Board
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile
import app.lexico.ui.board.ScrabbleBoard
import app.lexico.ui.common.BoardColumn
import app.lexico.ui.common.Header

data class Standing(val name: String, val total: Int)

data class WatchDuplicateView(
  val board: Board, val rack: List<String>, val round: Int, val playing: Boolean, val standings: List<Standing>, val watchers: Int,
)

@Composable
fun WatchDuplicateScreen(view: WatchDuplicateView, style: BoardStyle, onExit: () -> Unit) {
  BoardColumn(
    top = {
      Header("Observando · ronda ${view.round}", onExit) { if (view.watchers > 0) Text("👁 ${view.watchers}", fontWeight = FontWeight.Bold) }
      if (view.playing) Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { view.rack.forEach { RackTile(it, style, 28.dp) } }
    },
    board = { ScrabbleBoard(view.board, style = style) },
    bottom = { view.standings.forEachIndexed { i, s -> StandingRow(i + 1, s) } },
  )
}

@Composable
private fun StandingRow(place: Int, s: Standing) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text("$place.  ${s.name}", Modifier.weight(1f), maxLines = 1)
    Text("${s.total}", fontWeight = FontWeight.Bold)
  }
}
