package app.lexico.ui.classic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.board.MoveButtons
import app.lexico.ui.board.ShuffleOrRecall
import app.lexico.ui.board.SmallButton
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.common.ButtonText
import app.lexico.ui.common.Compact

data class TurnState(val myTurn: Boolean, val bag: Int, val claimWord: String? = null, val finalPass: Boolean = false)

@Composable
fun ActionButtons(
  c: TilePlacer,
  turn: TurnState,
  onPlay: () -> Unit,
  onExchange: (List<String>) -> Unit,
  onPass: () -> Unit,
  onClaim: () -> Unit = {},
) {
  if (c.exchanging) ExchangeButtons(c, turn.myTurn, onExchange) else TurnButtons(c, turn, onPlay, onPass, onClaim)
}

@Composable
private fun TurnButtons(c: TilePlacer, turn: TurnState, onPlay: () -> Unit, onPass: () -> Unit, onClaim: () -> Unit) {
  val open = turn.myTurn && !turn.finalPass
  MoveButtons(c, open, onPlay, middle = turn.claimWord?.let { { ClaimButton(it, onClaim) } }) {
    SmallButton(if (turn.finalPass) "Aceptar" else "Pasar", enabled = turn.myTurn, onClick = onPass)
    ShuffleOrRecall(c)
    SmallButton("Cambiar", enabled = open && turn.bag > 0, onClick = { c.startExchange(turn.bag) })
  }
}

@Composable
private fun ClaimButton(word: String, onClaim: () -> Unit) {
  OutlinedButton(
    onClick = onClaim, modifier = Modifier.fillMaxWidth().height(36.dp), contentPadding = Compact,
    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
  ) { ButtonText("Reclamar $word", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ExchangeButtons(c: TilePlacer, myTurn: Boolean, onExchange: (List<String>) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Button(enabled = myTurn && c.toExchange.isNotEmpty(), onClick = { onExchange(c.confirmExchange()) }) { Text("Cambiar ${c.toExchange.size}") }
    OutlinedButton(onClick = c::cancelExchange) { Text("Cancelar") }
  }
}
