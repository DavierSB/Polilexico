package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.EndButtons

@Composable
fun ResultPanel(view: ClassicView, end: GameEnd, onAnalyze: () -> Unit, onMenu: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text("${resultTitle(view, end)}   ${view.myScore} – ${view.opponentScore}",
      style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    EndButtons(onAnalyze, onMenu)
  }
}

private fun resultTitle(view: ClassicView, end: GameEnd): String = when {
  end.byTimeout -> "Perdiste por tiempo"
  end.winner == Side.ME -> "¡Ganaste!"
  end.winner == Side.OPPONENT -> "Ganó ${view.opponent}"
  else -> "Empate"
}
