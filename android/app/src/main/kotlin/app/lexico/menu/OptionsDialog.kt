package app.lexico.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Hint

/** El dialogo "Opciones" del menu lateral: los puntos al colocar. Se guarda al momento; el tablero va con el tema. */
@Composable
fun OptionsDialog(settings: Settings, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
    title = { Text("Opciones") },
    text = { Options(settings) },
  )
}

@Composable
private fun Options(settings: Settings) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    LiveScoreSwitch(settings.liveScore, settings::showLiveScore)
  }
}

/** Contar los puntos de la jugada mientras se coloca, encendido o apagado. */
@Composable
private fun LiveScoreSwitch(on: Boolean, change: (Boolean) -> Unit) {
  Row(
    Modifier.fillMaxWidth().toggleable(on, role = Role.Switch, onValueChange = change),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      Text("Contar los puntos al colocar")
      Hint("Mientras pones fichas, el tablero muestra los puntos que valdría la jugada.")
    }
    Switch(checked = on, onCheckedChange = null)
  }
}
