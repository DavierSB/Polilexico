package app.lexico.ui.sprint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.common.Compact

@Composable
fun SolvingButtons(c: TilePlacer, onPlay: () -> Unit, onGiveUp: () -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Button(modifier = Modifier.weight(1f), contentPadding = Compact, enabled = c.hasPlaced, onClick = onPlay) {
      Text("Jugar")
    }
    FilledTonalButton(modifier = Modifier.weight(1f), contentPadding = Compact, onClick = {
      if (c.hasPlaced) c.recall() else c.shuffle()
    }) { Text(if (c.hasPlaced) "Recoger" else "Mezclar") }
    OutlinedButton(modifier = Modifier.weight(1f), contentPadding = Compact, onClick = onGiveUp) { Text("Rendirse") }
  }
}

@Composable
fun NextButton(onNext: () -> Unit) {
  Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Siguiente mano") }
}

@Composable
fun FinishedButtons(onRestart: () -> Unit, onExit: () -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = onRestart) { Text("Otra serie") }
    OutlinedButton(onClick = onExit) { Text("Salir") }
  }
}
