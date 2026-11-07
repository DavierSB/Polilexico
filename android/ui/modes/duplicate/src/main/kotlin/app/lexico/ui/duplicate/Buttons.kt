package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Durations
import app.lexico.ui.board.MoveButtons
import app.lexico.ui.board.ShuffleOrRecall
import app.lexico.ui.board.TilePlacer

@Composable
fun ShowRackButton(turnMs: Long, onShowRack: () -> Unit) {
  Button(onClick = onShowRack, modifier = Modifier.fillMaxWidth()) {
    Text("Ver atril y arrancar el reloj (${Durations.format(turnMs)})")
  }
}

@Composable
fun PlayingButtons(c: TilePlacer, onPlay: () -> Unit) {
  MoveButtons(c, playEnabled = true, onPlay) { ShuffleOrRecall(c) }
}

@Composable
fun ConfirmingButtons(phase: Phase.Confirming, onCancel: () -> Unit) {
  val left = phase.remainingMs?.let { minOf(it, phase.cancelMs) } ?: phase.cancelMs
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text("Vas a jugar: ${phase.placement}", fontWeight = FontWeight.Bold)
    Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar (${Durations.format(left)})") }
  }
}
