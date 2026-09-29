package app.lexico.ui.recall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile
import app.lexico.ui.common.Mulish

/** Las palabras de la ronda con ✓ o ✗, y seguir o salir. */
@Composable
internal fun RoundDone(session: RecallSession, style: BoardStyle, onExit: () -> Unit) {
  Summary(session, style)
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = session::nextRound) { Text("Siguiente ronda") }
    OutlinedButton(onClick = onExit) { Text("Salir") }
  }
}

/** La ultima ronda, el total de la serie y la pregunta de si jugar otra. */
@Composable
internal fun SeriesDone(session: RecallSession, style: BoardStyle, onExit: () -> Unit) {
  Summary(session, style)
  if (session.rounds.size > 1) SeriesTotals(session.rounds)
  Text("¿Jugar otra serie?", style = MaterialTheme.typography.titleMedium)
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = session::restart) { Text("Otra serie") }
    OutlinedButton(onClick = onExit) { Text("Menú") }
  }
}

/** Aciertos de la ronda y cada palabra, de mas a menos puntos. */
@Composable
private fun Summary(session: RecallSession, style: BoardStyle) {
  val answers = session.answers
  ScoreCard("RECORDASTE", answers.count { it.hit }, answers.size)
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    answers.sortedByDescending { it.word.score }.forEach { AnswerRow(it, style) }
  }
}

/** Una tarjeta con un titulo pequeño y "7 / 10" en grande. */
@Composable
private fun ScoreCard(title: String, hits: Int, total: Int, detail: String? = null) {
  Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(title, fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
      Row(verticalAlignment = Alignment.Bottom) {
        Text("$hits", fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 48.sp)
        Text(" / $total", Modifier.padding(bottom = 7.dp), fontFamily = Mulish, fontWeight = FontWeight.Bold, fontSize = 20.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
  }
}

@Composable
private fun AnswerRow(answer: Answer, style: BoardStyle) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(if (answer.hit) "✓" else "✗", color = if (answer.hit) HIT else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
    answer.word.tiles.forEach { RackTile(it, style, 26.dp) }
    Text("${answer.word.score} pts", fontFamily = Mulish, fontWeight = FontWeight.Bold, fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

/** "Serie: 12 de 20" y el detalle por ronda. `rounds` = (aciertos, palabras) de cada una. */
@Composable
private fun SeriesTotals(rounds: List<Pair<Int, Int>>) {
  val detail = rounds.mapIndexed { i, (hits, total) -> "Ronda ${i + 1}: $hits/$total" }.joinToString("   ")
  ScoreCard("EN LA SERIE", rounds.sumOf { it.first }, rounds.sumOf { it.second }, detail)
}
