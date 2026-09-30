package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoundDialog(round: Round, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text("Ronda ${round.number}") },
    text = { RoundSummary(round) },
    confirmButton = { TextButton(onClick = close) { Text("Seguir") } },
  )
}

@Composable
private fun RoundSummary(round: Round) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    RoundPlayRow("Máster", round.master)
    RoundPlayRow("Tú", round.mine)
    if (round.hit) HitLabel()
  }
}

@Composable
private fun RoundPlayRow(who: String, play: RoundPlay) {
  Column {
    Text(who, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(play.text, Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 17.sp)
      Text("${play.points}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun HitLabel() {
  Text("¡Acierto!", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, fontSize = 24.sp,
    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}
