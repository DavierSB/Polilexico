package app.lexico.ui.classic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.DictionaryWord
import app.lexico.ui.common.playedWord

@Composable
internal fun WatchPanel(view: ClassicView) {
  Column(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    view.end?.let { Text("${resultTitle(view, it)}   ${view.myScore} – ${view.opponentScore}", fontWeight = FontWeight.Bold) }
    view.moves.lastOrNull()?.let { LastMove(it, if (it.side == Side.ME) view.me else view.opponent) }
  }
}

@Composable
private fun LastMove(move: Move, player: String) {
  Text("Última jugada · $player", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    MoveText(move)
    Text("${move.points} pts", fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun MoveText(move: Move) {
  val word = playedWord(move.text) ?: return Text(move.text, fontWeight = FontWeight.SemiBold)
  Text(move.text.substringBefore(' '), color = MaterialTheme.colorScheme.onSurfaceVariant)
  DictionaryWord(word, fontSize = 20.sp)
}
