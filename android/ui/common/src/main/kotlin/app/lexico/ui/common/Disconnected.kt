package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val OFFLINE_ALPHA = 0.92f
const val CLOCK_RUNS = "Tu reloj sigue corriendo."

@Composable
fun Disconnected(offline: Boolean, onHome: () -> Unit, note: String? = CLOCK_RUNS, content: @Composable () -> Unit) {
  Covered(offline, OFFLINE_ALPHA, content) {
    Text("Te desconectaste", style = MaterialTheme.typography.headlineSmall)
    Reconnecting()
    note?.let { Text(it) }
    TextButton(onClick = onHome) { Text("Ir al inicio") }
  }
}

@Composable
fun AdjournedCover(adjourned: Boolean, onHome: () -> Unit, content: @Composable () -> Unit) {
  Covered(adjourned, OFFLINE_ALPHA, content) {
    Text("Partida aplazada", style = MaterialTheme.typography.headlineSmall)
    Text("Se guarda 3 días para retomarla.")
    TextButton(onClick = onHome) { Text("Ir al inicio") }
  }
}

@Composable
private fun Reconnecting() {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    Text("Reconectando…")
  }
}
