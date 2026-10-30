package app.lexico.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization

@Composable
fun SoloOrWifiTabs(wifi: Boolean, select: (wifi: Boolean) -> Unit) {
  PrimaryTabRow(selectedTabIndex = if (wifi) 1 else 0) {
    Tab(selected = !wifi, onClick = { select(false) }, text = { Text("Solo") })
    Tab(selected = wifi, onClick = { select(true) }, text = { Text("Por wifi") })
  }
}

@Composable
fun WifiTip() {
  var shown by remember { mutableStateOf(true) }
  if (!shown) return
  AlertDialog(
    onDismissRequest = { shown = false },
    title = { Text("Jugar por wifi") },
    text = { Text(WIFI_TIP) },
    confirmButton = { TextButton(onClick = { shown = false }) { Text("Entendido") } },
  )
}

@Composable
fun WifiForm(onJoin: (name: String) -> Unit, hostSettings: @Composable (name: String) -> Unit) {
  WifiTip()
  var name by remember { mutableStateOf("") }
  var hosting by remember { mutableStateOf(true) }
  SectionTitle("Tu nombre")
  PlayerNameField(name) { name = it }
  TwoOptions("Crear sala", "Unirse a una sala", !hosting) { hosting = !it }
  if (hosting) hostSettings(name.trim())
  else Button(modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank(), onClick = { onJoin(name.trim()) }) { Text("Buscar salas") }
}

@Composable
fun CreateRoomButton(enabled: Boolean, onClick: () -> Unit) {
  Button(modifier = Modifier.fillMaxWidth(), enabled = enabled, onClick = onClick) { Text("Crear sala") }
}

@Composable
fun PlayerNameField(name: String, label: String = "Nombre", error: Boolean = false, onChange: (String) -> Unit) {
  OutlinedTextField(
    name, { onChange(it.take(NAME_CHARS)) }, Modifier.fillMaxWidth(), singleLine = true, isError = error, label = { Text(label) },
    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
  )
}

private const val NAME_CHARS = 16

private const val WIFI_TIP =
  "Uno de los jugadores debe compartir su wifi mediante hotspot y crear la sala; los demás deben unirse a esa wifi."
