package app.lexico.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.lexico.online.OnlineClient

@Composable
internal fun InvolvedGate(client: OnlineClient, nav: Navigator, content: @Composable () -> Unit) {
  val room by client.room.collectAsState()
  content()
  if (room != null && client.seatedInGame()) InvolvedDialog(nav)
}

@Composable
private fun InvolvedDialog(nav: Navigator) {
  AlertDialog(
    onDismissRequest = nav::back,
    text = { Text(INVOLVED) },
    confirmButton = { TextButton(onClick = { nav.replace(Screen.OnlinePlay) }) { Text("Continuar partida") } },
    dismissButton = { TextButton(onClick = nav::back) { Text("Cerrar") } },
  )
}

private const val INVOLVED = "Termina la partida en la que estás involucrado primero."
