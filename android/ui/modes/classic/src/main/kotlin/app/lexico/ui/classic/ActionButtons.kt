package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.MoveButtons
import app.lexico.ui.board.ShuffleOrRecall
import app.lexico.ui.board.SmallButton
import app.lexico.ui.board.TilePlacer

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
  MoveButtons(c, myTurn, onPlay) {
    SmallButton("Pasar", enabled = myTurn, onClick = onPass)
    ShuffleOrRecall(c)
    SmallButton("Cambiar", enabled = myTurn, onClick = c::startExchange)
  }
}

@Composable
private fun ExchangeButtons(c: TilePlacer, myTurn: Boolean, onExchange: (List<String>) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Button(enabled = myTurn && c.toExchange.isNotEmpty(), onClick = { onExchange(c.confirmExchange()) }) { Text("Cambiar ${c.toExchange.size}") }
    OutlinedButton(onClick = c::cancelExchange) { Text("Cancelar") }
  }
}
