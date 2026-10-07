package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.MoveText
import app.lexico.ui.common.moveNumberWidth
import app.lexico.ui.common.MovesTableDialog

@Composable
fun MovesDialog(rounds: List<Round>, close: () -> Unit) {
  MovesTableDialog(rounds, header = { MovesHeader() }, close = close) { _, round -> RoundRow(round) }
}

@Composable
private fun MovesHeader() {
  Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
    Text("#", Modifier.width(moveNumberWidth()), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Text("Máster", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Text("Tú", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun RoundRow(round: Round) {
  Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("${round.number}", Modifier.width(moveNumberWidth()), fontSize = 11.sp)
    PlayCell(round.master)
    PlayCell(round.mine, hit = round.hit)
  }
}

@Composable
private fun RowScope.PlayCell(play: RoundPlay, hit: Boolean = false) {
  Row(Modifier.weight(1f).padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    MoveText(play.text, Modifier.weight(1f), bingo = play.bingo)
    Text("${play.points}" + if (hit) " ✓" else "", Modifier.padding(start = 4.dp), fontSize = 12.sp,
      color = pointsColor(hit || play.bingo), fontWeight = if (hit || play.bingo) FontWeight.Bold else FontWeight.Normal)
  }
}

@Composable
private fun pointsColor(marked: Boolean): Color =
  if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
