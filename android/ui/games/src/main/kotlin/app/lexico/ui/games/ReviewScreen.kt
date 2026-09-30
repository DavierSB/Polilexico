package app.lexico.ui.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.ScrabbleBoard
import app.lexico.ui.common.Header
import app.lexico.ui.common.RankedMoveList

@Composable
fun ReviewScreen(review: ReviewView, style: BoardStyle, onBack: () -> Unit) {
  var index by remember { mutableIntStateOf(review.startTurn.coerceIn(0, maxOf(0, review.turns.lastIndex))) }
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Header(review.title, onBack)
    if (review.turns.isEmpty()) return@Column Text("La partida no tiene turnos.")
    TurnNavigator(review.turns[index], index, review.turns.size) { index = it }
    TurnContent(review.turns[index], style)
  }
}

@Composable
private fun TurnNavigator(turn: ReviewTurnView, index: Int, count: Int, go: (Int) -> Unit) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    OutlinedButton(onClick = { go(index - 1) }, enabled = index > 0) { Text("‹") }
    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
      Text("Turno ${turn.number}/$count" + if (turn.player.isNotEmpty()) " · ${turn.player}" else "", fontWeight = FontWeight.Bold)
      Text("Atril: ${turn.rack.joinToString(" ")}", fontFamily = FontFamily.Monospace, fontSize = 14.sp)
    }
    OutlinedButton(onClick = { go(index + 1) }, enabled = index < count - 1) { Text("›") }
  }
}

@Composable
private fun ColumnScope.TurnContent(turn: ReviewTurnView, style: BoardStyle) {
  var selected by remember(turn) { mutableStateOf(turn.marks.firstOrNull()?.move) }
  ScrabbleBoard(turn.board, Modifier.fillMaxWidth(), style, pending = selected?.placement?.placed.orEmpty())
  turn.marks.forEach { mark -> MarkLine(mark) { selected = mark.move } }
  Text("Mejores jugadas:", style = MaterialTheme.typography.labelMedium)
  RankedMoveList(turn.candidates, selected, Modifier.weight(1f)) { selected = it }
}

@Composable
private fun MarkLine(mark: MarkView, select: () -> Unit) {
  Text(mark.text, Modifier.fillMaxWidth().clickable(onClick = select), fontSize = 13.sp)
}

