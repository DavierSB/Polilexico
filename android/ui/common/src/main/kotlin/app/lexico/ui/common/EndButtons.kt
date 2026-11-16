package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private class EndButton(val label: String, val filled: Boolean, val onClick: () -> Unit)

@Composable
fun EndButtons(
  onAnalyze: () -> Unit, onMenu: () -> Unit, onAnalyzeEndgame: (() -> Unit)? = null, onExport: (() -> Unit)? = null,
  words: List<WordSection> = emptyList(), onSpread: (() -> Unit)? = null,
) {
  var listing by remember { mutableStateOf(false) }
  val buttons = listOfNotNull(
    EndButton("Analizar", true, onAnalyze),
    onAnalyzeEndgame?.let { EndButton("Analizar final", true, it) },
    EndButton("Palabras", false) { listing = true }.takeIf { words.isNotEmpty() },
    onExport?.let { EndButton("Exportar", false, it) },
    onSpread?.let { EndButton("Ventaja", false, it) },
    EndButton("Menú", false, onMenu),
  )
  ButtonGrid(buttons)
  if (listing) PlayedWordsDialog(words) { listing = false }
}

@Composable
private fun ButtonGrid(buttons: List<EndButton>) {
  val rows = if (buttons.size % 2 == 1) listOf(buttons.take(1)) + buttons.drop(1).chunked(2) else buttons.chunked(2)
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    rows.forEach { row -> ButtonRow(row) }
  }
}

@Composable
private fun ButtonRow(row: List<EndButton>) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    row.forEach { EndButtonView(it, Modifier.weight(1f)) }
  }
}

@Composable
private fun EndButtonView(button: EndButton, modifier: Modifier) {
  val label = @Composable { Text(button.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
  if (button.filled) Button(onClick = button.onClick, modifier, contentPadding = RowButtonPadding) { label() }
  else OutlinedButton(onClick = button.onClick, modifier, contentPadding = RowButtonPadding) { label() }
}
