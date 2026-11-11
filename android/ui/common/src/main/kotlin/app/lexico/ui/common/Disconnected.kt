package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
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
    TextButton(onClick = onHome) { Text("Volver") }
  }
}

@Composable
fun AwaitingReconnection(offline: Boolean, content: @Composable () -> Unit) {
  Covered(offline, OFFLINE_ALPHA, content) {
    CircularProgressIndicator()
    Text("Esperando que te reconectes…", style = MaterialTheme.typography.titleMedium)
  }
}

@Composable
fun GoneCover(gone: Boolean, onLeave: () -> Unit, content: @Composable () -> Unit) {
  LaunchedEffect(gone) {
    if (!gone) return@LaunchedEffect
    delay(GONE_MS)
    onLeave()
  }
  Covered(gone, OFFLINE_ALPHA, content) {
    Text("La partida ya no está disponible", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text("Volviendo al menú…")
  }
}

private const val GONE_MS = 2_500L

@Composable
fun AdjournedCover(adjourned: Boolean, onHome: () -> Unit, content: @Composable () -> Unit) {
  Covered(adjourned, OFFLINE_ALPHA, content) {
    Text("Partida aplazada", style = MaterialTheme.typography.headlineSmall)
    Text("Se guarda 3 días para retomarla.")
    TextButton(onClick = onHome) { Text("Volver") }
  }
}

@Composable
private fun Reconnecting() {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    Text("Reconectando…")
  }
}
