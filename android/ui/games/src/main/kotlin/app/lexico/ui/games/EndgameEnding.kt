package app.lexico.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal val STEP_NUMBER_WIDTH = 24.dp
internal val WHO_WIDTH = 72.dp
internal val NUMBER_WIDTH = 48.dp

@Composable
internal fun EndgameResult(position: ExploredPosition) {
  Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text("FIN DE LA PARTIDA", style = headerStyle())
    position.ending.forEach { EndingLine(it) }
    val margin = position.scores[position.me] - position.scores[1 - position.me]
    Outcome(if (margin > 0) ME else position.players[1 - position.me], abs(margin))
  }
}

@Composable
internal fun EndingLine(row: EndingRow) {
  Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(row.who, Modifier.width(STEP_NUMBER_WIDTH + WHO_WIDTH), style = captionStyle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(endingText(row), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text("%+d".format(row.delta), Modifier.width(NUMBER_WIDTH), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
  }
}

@Composable
internal fun Outcome(winner: String?, margin: Int) {
  Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(outcomeText(winner, margin), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text("$margin", Modifier.width(NUMBER_WIDTH), textAlign = TextAlign.End, style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
  }
}

private fun endingText(row: EndingRow): String =
  if (row.left.isEmpty()) "Cierra" else "Se queda con " + row.left.joinToString(" ")

private fun outcomeText(winner: String?, margin: Int): String = when {
  margin == 0 || winner == null -> "Empate"
  winner == ME -> "Ganas tú por"
  else -> "Gana $winner por"
}
