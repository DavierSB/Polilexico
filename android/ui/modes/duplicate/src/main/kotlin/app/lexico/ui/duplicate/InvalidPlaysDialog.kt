package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InvalidPlaysDialog(plays: List<InvalidPlay>, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text("Jugadas inválidas") },
    text = { if (plays.isEmpty()) Text("No hubo jugadas inválidas.") else InvalidPlaysList(plays) },
    confirmButton = { TextButton(onClick = close) { Text("Ver resultados") } },
  )
}

@Composable
private fun InvalidPlaysList(plays: List<InvalidPlay>) {
  Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("${plays.size} de tus jugadas no eran válidas y valen 0 puntos:", style = MaterialTheme.typography.bodyMedium)
    plays.forEach { InvalidPlayRow(it) }
  }
}

@Composable
private fun InvalidPlayRow(play: InvalidPlay) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text("${play.round}", Modifier.width(32.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(play.play.text, Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 16.sp)
    Text("${play.play.points}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error,
      textDecoration = TextDecoration.LineThrough)
  }
}
