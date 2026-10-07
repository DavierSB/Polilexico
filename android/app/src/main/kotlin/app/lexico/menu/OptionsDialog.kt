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
import app.lexico.ui.common.InfoButton

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
    UnseenSwitch(settings.showUnseen, settings::showUnseenTiles)
    OptionSwitch(settings.sounds, settings::useSounds, "Sonidos")
    OptionSwitch(settings.vibration, settings::useVibration, "Vibración")
  }
}

@Composable
private fun LiveScoreSwitch(on: Boolean, change: (Boolean) -> Unit) =
  OptionSwitch(on, change, "Contar los puntos al colocar", "Mientras pones fichas, el tablero muestra los puntos que valdría la jugada.")

@Composable
private fun UnseenSwitch(on: Boolean, change: (Boolean) -> Unit) =
  OptionSwitch(on, change, "Mostrar las letras faltantes", "En la clásica, al tocar la bolsa: las fichas que no has visto (bolsa y atril del rival); si no, solo cuántas quedan.")

@Composable
private fun OptionSwitch(on: Boolean, change: (Boolean) -> Unit, title: String, hint: String? = null) {
  Row(
    Modifier.fillMaxWidth().toggleable(on, role = Role.Switch, onValueChange = change),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
      Text(title, Modifier.weight(1f, fill = false))
      hint?.let { InfoButton(title, it) }
    }
    Switch(checked = on, onCheckedChange = null)
  }
}
