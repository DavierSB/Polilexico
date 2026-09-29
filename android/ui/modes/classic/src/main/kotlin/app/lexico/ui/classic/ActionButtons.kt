package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.common.Compact

/**
 * Los botones de tu turno: Jugar, Mezclar/Recoger, Cambiar y Pasar. En modo cambio, en su lugar,
 * confirmar el cambio de las fichas marcadas o cancelarlo.
 */
@Composable
fun ActionButtons(
  c: TilePlacer,
  myTurn: Boolean,
  onPlay: () -> Unit,
  onExchange: (List<String>) -> Unit,
  onPass: () -> Unit,
) {
  if (c.exchanging) ExchangeButtons(c, myTurn, onExchange) else TurnButtons(c, myTurn, onPlay, onPass)
}

@Composable
private fun TurnButtons(c: TilePlacer, myTurn: Boolean, onPlay: () -> Unit, onPass: () -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Button(modifier = Modifier.weight(1f), contentPadding = Compact, enabled = myTurn && c.hasPlaced, onClick = onPlay) { Text("Jugar") }
    ShuffleOrRecall(c)
    OutlinedButton(modifier = Modifier.weight(1f), contentPadding = Compact, enabled = myTurn, onClick = c::startExchange) { Text("Cambiar") }
    OutlinedButton(modifier = Modifier.weight(1f), contentPadding = Compact, enabled = myTurn, onClick = onPass) { Text("Pasar") }
  }
}

/** Sin fichas puestas, mezcla el atril; con fichas puestas, las recoge. */
@Composable
private fun RowScope.ShuffleOrRecall(c: TilePlacer) {
  FilledTonalButton(modifier = Modifier.weight(1f), contentPadding = Compact, onClick = { if (c.hasPlaced) c.recall() else c.shuffle() }) {
    Text(if (c.hasPlaced) "Recoger" else "Mezclar")
  }
}

@Composable
private fun ExchangeButtons(c: TilePlacer, myTurn: Boolean, onExchange: (List<String>) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Button(enabled = myTurn && c.toExchange.isNotEmpty(), onClick = { onExchange(c.confirmExchange()) }) { Text("Cambiar ${c.toExchange.size}") }
    OutlinedButton(onClick = c::cancelExchange) { Text("Cancelar") }
  }
}
