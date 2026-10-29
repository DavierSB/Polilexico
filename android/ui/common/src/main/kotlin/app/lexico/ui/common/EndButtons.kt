package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun EndButtons(
  onAnalyze: () -> Unit, onMenu: () -> Unit, onAnalyzeEndgame: (() -> Unit)? = null, onExport: (() -> Unit)? = null,
  words: List<WordSection> = emptyList(),
) {
  var listing by remember { mutableStateOf(false) }
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = onAnalyze) { Text("Analizar") }
    onAnalyzeEndgame?.let { Button(onClick = it) { Text("Analizar final") } }
    if (words.isNotEmpty()) OutlinedButton(onClick = { listing = true }) { Text("Palabras") }
    onExport?.let { OutlinedButton(onClick = it) { Text("Exportar") } }
    OutlinedButton(onClick = onMenu) { Text("Menú") }
  }
  if (listing) PlayedWordsDialog(words) { listing = false }
}
