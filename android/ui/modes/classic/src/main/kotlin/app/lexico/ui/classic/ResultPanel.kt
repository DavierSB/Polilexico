package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.EndButtons
import app.lexico.ui.common.Spread
import app.lexico.ui.common.SpreadDialog
import app.lexico.ui.common.WordSection

@Composable
fun ResultPanel(
  view: ClassicView, end: GameEnd, onAnalyze: () -> Unit, onAnalyzeEndgame: () -> Unit, onExport: () -> Unit, onMenu: () -> Unit,
) {
  var charting by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text("${resultTitle(view, end)}   ${view.myScore} – ${view.opponentScore}",
      style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      EndButtons(onAnalyze, onMenu, onAnalyzeEndgame.takeIf { view.bag == 0 }, onExport, playedWords(view))
      TextButton(onClick = { charting = true }) { Text("Ventaja") }
    }
  }
  if (charting) SpreadDialog(spread(view), close = { charting = false })
}

private fun playedWords(view: ClassicView): List<WordSection> =
  listOf(Side.ME to view.me, Side.OPPONENT to view.opponent).map { (side, name) -> WordSection(name, view.moves.filter { it.side == side && it.type == MoveType.PLACEMENT }.map { it.text }) }

private fun spread(view: ClassicView): Spread {
  val totals = listOf(0 to 0) + view.moves.map { it.myTotal to it.opponentTotal } + (view.myScore to view.opponentScore)
  return Spread.of(view.me, view.opponent, view.moves.firstOrNull()?.side != Side.OPPONENT, totals.distinctLast())
}

internal fun localVerdict(view: ClassicView, end: GameEnd): String = when (end.winner) {
  Side.ME -> "Ganó ${view.me}"
  Side.OPPONENT -> "Ganó ${view.opponent}"
  null -> "Empate"
}

private fun <T> List<T>.distinctLast(): List<T> = if (size > 1 && last() == this[lastIndex - 1]) dropLast(1) else this

private fun resultTitle(view: ClassicView, end: GameEnd): String = when {
  view.local -> localVerdict(view, end) + if (end.byTimeout) " por tiempo" else ""
  end.byTimeout -> "Perdiste por tiempo"
  end.winner == Side.ME -> "¡Ganaste!"
  end.winner == Side.OPPONENT -> "Ganó ${view.opponent}"
  else -> "Empate"
}
