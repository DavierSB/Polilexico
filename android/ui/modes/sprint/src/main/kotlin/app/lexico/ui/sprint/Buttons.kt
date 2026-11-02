package app.lexico.ui.sprint

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.MoveButtons
import app.lexico.ui.board.ShuffleOrRecall
import app.lexico.ui.board.SmallButton
import app.lexico.ui.board.TilePlacer
import app.lexico.ui.common.ConfirmDialog

@Composable
fun SolvingButtons(c: TilePlacer, onPlay: () -> Unit, onGiveUp: () -> Unit) {
  var giveUp by remember { mutableStateOf(false) }
  MoveButtons(c, playEnabled = true, onPlay) {
    SmallButton("Rendirse") { giveUp = true }
    ShuffleOrRecall(c)
  }
  if (giveUp) ConfirmDialog(
    "¿Rendirse? Pierdes una vida.", yes = "Rendirse", no = "Seguir",
    onYes = { giveUp = false; onGiveUp() }, onNo = { giveUp = false },
  )
}

@Composable
fun NextButton(onNext: () -> Unit) {
  Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Siguiente mano", fontWeight = FontWeight.Bold) }
}

