package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Compact
import app.lexico.ui.common.Durations
import app.lexico.ui.board.TilePlacer

/** Entre rondas: el boton para ver el atril, que arranca el reloj. */
@Composable
fun ShowRackButton(turnMs: Long, onShowRack: () -> Unit) {
  Button(onClick = onShowRack, modifier = Modifier.fillMaxWidth()) {
    Text("Ver atril y arrancar el reloj (${Durations.format(turnMs)})")
  }
}

/** Mientras piensas: Jugar, Mezclar/Recoger y Pasar. En duplicada no se cambian fichas. */
@Composable
fun PlayingButtons(c: TilePlacer, onPlay: () -> Unit, onPass: () -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Button(modifier = Modifier.weight(1f), contentPadding = Compact, enabled = c.hasPlaced, onClick = onPlay) {
      Text("Jugar")
    }
    FilledTonalButton(modifier = Modifier.weight(1f), contentPadding = Compact, onClick = {
      if (c.hasPlaced) c.recall() else c.shuffle()
    }) { Text(if (c.hasPlaced) "Recoger" else "Mezclar") }
    OutlinedButton(modifier = Modifier.weight(1f), contentPadding = Compact, onClick = onPass) { Text("Pasar") }
  }
}

/**
 * Con la jugada propuesta: cual es y el boton para cancelarla, con la cuenta atras. Se anota
 * con lo primero que venza, la ventana para cancelar o el reloj del turno.
 */
@Composable
fun ConfirmingButtons(phase: Phase.Confirming, onCancel: () -> Unit) {
  val left = phase.remainingMs?.let { minOf(it, phase.cancelMs) } ?: phase.cancelMs
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text("Vas a jugar: ${phase.placement}", fontWeight = FontWeight.Bold)
    Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar (${Durations.format(left)})") }
  }
}
