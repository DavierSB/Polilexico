package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun EndButtons(onAnalyze: () -> Unit, onMenu: () -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = onAnalyze) { Text("Analizar") }
    OutlinedButton(onClick = onMenu) { Text("Menú") }
  }
}
