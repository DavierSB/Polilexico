package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile

private val LETTER_TILE = 34.dp

@Composable
internal fun InvalidMoveDialog(move: Move, style: BoardStyle, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = {},
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    title = { Text("Jugada inválida") },
    text = { InvalidMoveDetails(move, style) },
    confirmButton = { TextButton(onClick = close) { Text("OK") } },
  )
}

@Composable
private fun InvalidMoveDetails(move: Move, style: BoardStyle) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Una o varias de las palabras formadas no son válidas")
    if (move.words.isNotEmpty()) FormedWords(move.words)
    if (move.letters.isNotEmpty()) UsedLetters(move.letters, style)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormedWords(words: List<String>) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    words.forEach { Text(it, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp) }
  }
}

@Composable
private fun UsedLetters(letters: List<String>, style: BoardStyle) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Las letras usadas fueron:")
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) { letters.forEach { RackTile(it, style, LETTER_TILE) } }
  }
}
