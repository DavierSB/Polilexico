package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val ADJUDICATION_RULES =
  "Una partida aplazada puede retomarse durante 3 días, cuando los dos jugadores estén conectados; si es sin tiempo, " +
    "aunque el rival no lo esté. Si pasan los 3 días, se adjudica en contra del responsable: quien propuso aplazar; si se " +
    "aplazó porque el rival llevaba más de un minuto desconectado, ese rival; si se aplazó con los dos desconectados, el " +
    "primero en desconectarse. Si el otro jugador va por delante, gana; si no, la partida se anula. Si el responsable hizo " +
    "la última jugada, esa jugada no cuenta."

@Composable
fun AdjournConfirm(rival: String, rivalOnline: Boolean, onYes: () -> Unit, onNo: () -> Unit) = ConfirmDialog(
  if (rivalOnline) "¿Proponer a $rival aplazar la partida?" else "¿Aplazar la partida? Se guarda 3 días para retomarla.",
  yes = if (rivalOnline) "Proponer" else "Aplazar", no = "No", onYes = onYes, onNo = onNo,
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
fun RivalAwayDialog(rival: String, onAdjourn: () -> Unit, onWait: () -> Unit) {
  AlertDialog(
    onDismissRequest = onWait, title = { Text("$rival lleva 3 minutos fuera") },
    text = { AwayText(onAdjourn) },
    confirmButton = { TextButton(onClick = onWait) { Text("Esperar") } },
  )
}

@Composable
private fun AwayText(onAdjourn: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Puedes aplazar la partida y retomarla cuando estéis los dos conectados, en los próximos 3 días.")
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        "Si pasan los 3 días, se adjudica según las reglas de adjudicación.",
        Modifier.weight(1f),
      )
      InfoButton("Reglas de adjudicación", ADJUDICATION_RULES)
    }
    Button(onClick = onAdjourn, Modifier.fillMaxWidth()) { Text("Aplazar") }
  }
}

@Composable
fun VerdictNotice(opponent: String, country: String, result: String, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close, icon = { PersonAvatar(opponent, country, size = 56.dp) },
    title = { Text(verdictTitle(result)) },
    text = { Text("Tu partida aplazada con $opponent ${flagOf(country)} ${verdictText(result)}") },
    confirmButton = { TextButton(onClick = close) { Text("Entendido") } },
  )
}

private fun verdictTitle(result: String): String = when (result) {
  "win" -> "¡Ganaste!"
  "loss" -> "Perdiste"
  else -> "Partida anulada"
}

private fun verdictText(result: String): String = when (result) {
  "win" -> "se adjudicó a tu favor."
  "loss" -> "se adjudicó en tu contra."
  else -> "se anuló al pasar los 3 días."
}
