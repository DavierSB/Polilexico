package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val ADJUDICATION_RULES =
  "Si tu rival lleva 3 minutos desconectado puedes adjudicar la partida. Ganas si vas por delante; si no, la partida se " +
    "anula y no cuenta. Si tu rival se desconectó justo después de jugar, esa última jugada no cuenta para decidir quién va " +
    "por delante."

@Composable
fun AdjournConfirm(rival: String, rivalOnline: Boolean, onYes: () -> Unit, onNo: () -> Unit) = ConfirmDialog(
  if (rivalOnline) "¿Proponer a $rival aplazar la partida?" else "¿Aplazar la partida? Se guarda 3 días para retomarla.",
  yes = if (rivalOnline) "Proponer" else "Aplazar", no = "No", onYes = onYes, onNo = onNo,
)

@Composable
fun AdjudicateConfirm(onYes: () -> Unit, onNo: () -> Unit) = ConfirmDialog(
  "¿Adjudicar la partida? Ganas si vas por delante; si no, se anula.", yes = "Adjudicar", no = "No", onYes = onYes, onNo = onNo,
)

@Composable
fun AdjournAskDialog(rival: String, onAccept: () -> Unit, onDecline: () -> Unit) {
  AlertDialog(
    onDismissRequest = {}, title = { Text("$rival propone aplazar") }, text = { Text("La partida se guarda 3 días para retomarla.") },
    confirmButton = { Button(onClick = onAccept) { Text("Aceptar") } },
    dismissButton = { TextButton(onClick = onDecline) { Text("Rechazar") } },
  )
}

@Composable
fun RivalAwayDialog(rival: String, onAdjourn: () -> Unit, onAdjudicate: () -> Unit, onWait: () -> Unit) {
  AlertDialog(
    onDismissRequest = onWait, title = { Text("$rival lleva 3 minutos fuera") },
    text = { AwayChoices(onAdjourn, onAdjudicate) },
    confirmButton = { TextButton(onClick = onWait) { Text("Esperar") } },
  )
}

@Composable
private fun AwayChoices(onAdjourn: () -> Unit, onAdjudicate: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Aplazar guarda la partida para luego.")
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Adjudicar atribuye la victoria a uno de los jugadores según las reglas de adjudicación.", Modifier.weight(1f))
      InfoButton("Reglas de adjudicación", ADJUDICATION_RULES)
    }
    Button(onClick = onAdjourn, Modifier.fillMaxWidth()) { Text("Aplazar · aconsejado") }
    OutlinedButton(onClick = onAdjudicate, Modifier.fillMaxWidth()) { Text("Adjudicar", color = MaterialTheme.colorScheme.primary) }
  }
}
