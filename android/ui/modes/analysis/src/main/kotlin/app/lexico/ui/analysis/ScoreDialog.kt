package app.lexico.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val SCORE_HINT = "Con marcador, en el pre-final y en el final se analiza esa fase (pasar o el scrabble pueden ganar). Sin marcador, solo equity."

@Composable
internal fun ScoreDialog(editor: PositionEditor, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    confirmButton = { TextButton(onClick = close) { Text("Listo") } },
    title = { Text("Marcador") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScoreField("Tú", editor.myScore) { editor.myScore = it; editor.scored = true }
        ScoreField("Rival", editor.opponentScore) { editor.opponentScore = it; editor.scored = true }
        Text(SCORE_HINT, style = MaterialTheme.typography.bodySmall)
      }
    },
  )
}

@Composable
private fun ScoreField(label: String, value: Int, onChange: (Int) -> Unit) {
  OutlinedTextField(
    value = value.toString(), onValueChange = { text -> onChange(text.filter(Char::isDigit).take(4).toIntOrNull() ?: 0) },
    label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    modifier = Modifier.fillMaxWidth(),
  )
}
