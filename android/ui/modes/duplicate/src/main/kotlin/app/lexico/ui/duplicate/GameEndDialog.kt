package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameEndDialog(view: DuplicateView, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text("Partida terminada") },
    text = { EndSummary(view) },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

@Composable
private fun EndSummary(view: DuplicateView) {
  Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Efficiency(view)
    Spacer(Modifier.height(14.dp))
    Text("${view.hits} aciertos de ${view.rounds.size} rondas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun Efficiency(view: DuplicateView) {
  Text("Eficiencia", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
  Text("%.1f %%".format(view.efficiency), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
  Text("${view.myScore} de ${view.masterScore} puntos del máster", style = MaterialTheme.typography.bodySmall)
}
