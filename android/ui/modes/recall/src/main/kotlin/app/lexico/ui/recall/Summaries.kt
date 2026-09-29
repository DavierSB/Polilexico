package app.lexico.ui.recall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile

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
  Text("Recordaste ${answers.count { it.hit }} de ${answers.size}", fontSize = 22.sp, fontWeight = FontWeight.Black)
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    answers.sortedByDescending { it.word.score }.forEach { AnswerRow(it, style) }
  }
}

@Composable
private fun AnswerRow(answer: Answer, style: BoardStyle) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(if (answer.hit) "✓" else "✗", color = if (answer.hit) HIT else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
    answer.word.tiles.forEach { RackTile(it, style, 26.dp) }
    Text("${answer.word.score} pts", style = MaterialTheme.typography.bodySmall)
  }
}

/** "Serie: 12 de 20" y el detalle por ronda. `rounds` = (aciertos, palabras) de cada una. */
@Composable
private fun SeriesTotals(rounds: List<Pair<Int, Int>>) {
  Text("Serie: ${rounds.sumOf { it.first }} de ${rounds.sumOf { it.second }}", fontSize = 22.sp, fontWeight = FontWeight.Black)
  Text(rounds.mapIndexed { i, (hits, total) -> "Ronda ${i + 1}: $hits/$total" }.joinToString("   "), style = MaterialTheme.typography.bodySmall)
}
