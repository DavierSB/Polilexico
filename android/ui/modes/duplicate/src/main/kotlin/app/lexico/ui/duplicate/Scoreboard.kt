package app.lexico.ui.duplicate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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

@Composable
fun Scoreboard(view: DuplicateView) {
  Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(vertical = 8.dp)) {
    scores(view).forEach { (label, value) -> ScoreItem(label, value) }
  }
}

private fun scores(view: DuplicateView): List<Pair<String, String>> = listOf(
  "Ronda" to "${view.round}",
  "Tú" to "${view.myScore}",
  "Máster" to "${view.masterScore}",
  "Aciertos" to "${view.hits}/${view.rounds.size}",
)

@Composable
private fun RowScope.ScoreItem(label: String, value: String) {
  Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
  }
}
