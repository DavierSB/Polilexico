package app.lexico.ui.classic

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.SectionTitle

internal enum class RivalTab(val label: String) { LOCAL("Local"), ONLINE("En línea") }

@Composable
internal fun RivalTabs(selected: RivalTab, onlineLabel: String, select: (RivalTab) -> Unit) {
  PrimaryTabRow(selectedTabIndex = selected.ordinal) {
    RivalTab.entries.forEach { tab ->
      Tab(selected = tab == selected, onClick = { select(tab) }, text = { Text(if (tab == RivalTab.ONLINE) onlineLabel else tab.label, maxLines = 1) })
    }
  }
}

@Composable
internal fun RivalTabs(human: Boolean, select: (human: Boolean) -> Unit) {
  PrimaryTabRow(selectedTabIndex = if (human) 1 else 0) {
    Tab(selected = !human, onClick = { select(false) }, text = { Text("Bot") })
    Tab(selected = human, onClick = { select(true) }, text = { Text("Humano") })
  }
}

@Composable
internal fun PlayModeOptions(wifi: Boolean, select: (wifi: Boolean) -> Unit) {
  PlayModeOption("Compartiendo el teléfono", selected = !wifi) { select(false) }
  PlayModeOption("Por wifi", selected = wifi) { select(true) }
}

@Composable
internal fun LocalHumansDialog(onClose: () -> Unit, onSelect: (wifi: Boolean) -> Unit) {
  var wifi by remember { mutableStateOf(false) }
  AlertDialog(
    onDismissRequest = onClose,
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Si quieres jugar de manera local contra otro humano puedes hacerlo")
        PlayModeOptions(wifi) { wifi = it }
      }
    },
    confirmButton = { Button(onClick = { onSelect(wifi) }) { Text("Seleccionar") } },
    dismissButton = { TextButton(onClick = onClose) { Text("Jugar contra un bot") } },
  )
}

@Composable
private fun PlayModeOption(title: String, selected: Boolean, select: () -> Unit) {
  val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
  Column(
    Modifier.fillMaxWidth().border(if (selected) 2.dp else 1.dp, color, RoundedCornerShape(10.dp)).clickable(onClick = select).padding(horizontal = 12.dp, vertical = 14.dp),
  ) {
    Text(title, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
  }
}

@Composable
internal fun MyNameSection(form: ClassicForm) {
  SectionTitle("Tu nombre")
  NameField(form.myName, form.myName.isNotEmpty() && !form.nameValid(), "Nombre") { form.myName = it }
}

@Composable
internal fun NamesSection(form: ClassicForm) {
  SectionTitle("Jugadores")
  NameField(form.first, form.nameError(form.first, form.second), "Jugador A") { form.first = it }
  NameField(form.second, form.nameError(form.second, form.first), "Jugador B") { form.second = it }
}

@Composable
private fun NameField(value: String, error: Boolean, label: String, onChange: (String) -> Unit) {
  OutlinedTextField(
    value = value, onValueChange = { onChange(it.take(NAME_CHARS)) }, singleLine = true, isError = error,
    label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
  )
}

private const val NAME_CHARS = 16
