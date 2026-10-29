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

enum class ExportKind { GCG, GIF }

@Composable
fun ExportButtons(onExport: (ExportKind) -> Unit) {
  Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = { onExport(ExportKind.GCG) }, Modifier.weight(1f)) { Text("Guardar .gcg") }
    OutlinedButton(onClick = { onExport(ExportKind.GIF) }, Modifier.weight(1f)) { Text("Guardar .gif") }
  }
}

@Composable
fun ExportDialog(onGcg: () -> Unit, onGif: () -> Unit, onClose: () -> Unit) {
  AlertDialog(
    onDismissRequest = onClose,
    title = { Text("Exportar partida") },
    text = { ExportChoices(onGcg, onGif) },
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
        Text("Exportando la partida…")
      }
    },
  )
}

@Composable
private fun ExportChoices(onGcg: () -> Unit, onGif: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Elige el formato y luego dónde guardarla.", style = MaterialTheme.typography.bodySmall)
    Button(onClick = onGcg, Modifier.fillMaxWidth()) { Text("Formato Scrabble (.gcg)") }
    Button(onClick = onGif, Modifier.fillMaxWidth()) { Text("Animación (.gif)") }
  }
}
