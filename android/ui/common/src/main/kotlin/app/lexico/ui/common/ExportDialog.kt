package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExportButton(onExport: () -> Unit) {
  OutlinedButton(onClick = onExport, Modifier.fillMaxWidth()) { Text("Exportar") }
}

@Composable
fun ExportDialog(onJpg: () -> Unit, onGif: () -> Unit, onGcg: () -> Unit, onClose: () -> Unit) {
  AlertDialog(
    onDismissRequest = onClose,
    title = { Text("Exportar partida") },
    text = { ExportChoices(onJpg, onGif, onGcg) },
    confirmButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
  )
}

@Composable
fun ExportingDialog() {
  AlertDialog(
    onDismissRequest = {},
    confirmButton = {},
    text = {
      Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp))
        Column {
          Text("Exportando la partida…")
          Text("Esto podría tardar un poco.", style = MaterialTheme.typography.bodySmall)
        }
      }
    },
  )
}

@Composable
private fun ExportChoices(onJpg: () -> Unit, onGif: () -> Unit, onGcg: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = onJpg, Modifier.fillMaxWidth()) { Text("Imagen (.jpg)") }
    Button(onClick = onGif, Modifier.fillMaxWidth()) { Text("Animación (.gif)") }
    OutlinedButton(onClick = onGcg, Modifier.fillMaxWidth()) { Text("Formato Scrabble (.gcg)") }
  }
}
