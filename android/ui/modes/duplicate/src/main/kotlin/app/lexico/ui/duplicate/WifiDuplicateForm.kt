package app.lexico.ui.duplicate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.lexico.ui.common.Durations
import app.lexico.ui.common.SectionTitle
import app.lexico.ui.common.TimeField
import app.lexico.ui.common.TwoOptions
import app.lexico.ui.common.WifiTip

data class WifiDuplicateConfig(val name: String, val turnMs: Long, val rounds: Int, val live: Boolean)

@Composable
internal fun WifiDuplicateForm(onHost: (WifiDuplicateConfig) -> Unit, onJoin: (name: String) -> Unit) {
  WifiTip()
  var name by remember { mutableStateOf("") }
  var hosting by remember { mutableStateOf(true) }
  SectionTitle("Tu nombre")
  NameField(name) { name = it }
  TwoOptions("Crear sala", "Unirse a una sala", !hosting) { hosting = !it }
  val valid = name.isNotBlank() && !name.trim().equals(MASTER_NAME, true)
  if (hosting) HostForm(name.trim(), valid, onHost) else JoinForm(valid) { onJoin(name.trim()) }
}

@Composable
private fun NameField(name: String, onChange: (String) -> Unit) {
  OutlinedTextField(
    name, { onChange(it.take(NAME_CHARS)) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Nombre") },
    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
  )
}

@Composable
private fun HostForm(name: String, valid: Boolean, onHost: (WifiDuplicateConfig) -> Unit) {
  var turn by remember { mutableStateOf(Durations.format(DEFAULT_TURN_MS)) }
  var fixed by remember { mutableStateOf(false) }
  var rounds by remember { mutableIntStateOf(PRESET_ROUNDS) }
  var live by remember { mutableStateOf(true) }
  val turnMs = Durations.parse(turn)?.takeIf { it > 0 }
  RoundsSection(fixed, rounds, { fixed = it }, { rounds = it })
  SectionTitle("Tiempo", info = "Cada ronda dura el tiempo completo.")
  TimeField(turn, { turn = it }, "Tiempo por ronda")
  SectionTitle("Puntuaciones en vivo", info = LIVE_INFO)
  TwoOptions("Sí", "No", !live) { live = !it }
  Button(modifier = Modifier.fillMaxWidth(), enabled = valid && turnMs != null, onClick = {
    turnMs?.let { onHost(WifiDuplicateConfig(name, it, if (fixed) rounds else 0, live)) }
  }) { Text("Crear sala") }
}

@Composable
private fun JoinForm(valid: Boolean, onJoin: () -> Unit) {
  Button(modifier = Modifier.fillMaxWidth(), enabled = valid, onClick = onJoin) { Text("Buscar salas") }
}

private const val NAME_CHARS = 16

private const val LIVE_INFO = "Ver los puntos de todos tras cada ronda."
