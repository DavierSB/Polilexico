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

@Composable
fun Disconnected(offline: Boolean, onHome: () -> Unit, content: @Composable () -> Unit) {
  Covered(offline, OFFLINE_ALPHA, content) {
    Text("Te desconectaste", style = MaterialTheme.typography.headlineSmall)
    Reconnecting()
    Text("Tu reloj sigue corriendo.")
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
