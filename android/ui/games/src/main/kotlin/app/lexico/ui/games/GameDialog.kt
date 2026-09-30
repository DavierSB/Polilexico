package app.lexico.ui.games

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun GameDialog(game: ChartGame, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(game.title, fontWeight = FontWeight.Bold) },
    text = { Column { game.lines.forEach { Text(it) } } },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}
