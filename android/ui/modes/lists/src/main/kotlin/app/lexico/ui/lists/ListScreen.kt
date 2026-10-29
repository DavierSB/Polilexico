package app.lexico.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.ConfirmDialog
import app.lexico.ui.common.Header

@Composable
fun ListScreen(list: WordList, onBack: () -> Unit, onStudy: () -> Unit, onTest: () -> Unit, onDelete: () -> Unit) {
  var deleting by remember { mutableStateOf(false) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
    Header(list.name, onBack, titleLines = 2)
    Progress(list)
    FilterSummary(list.summary)
    Actions(onStudy, onTest)
    if (!list.isDefault) TextButton(onClick = { deleting = true }, Modifier.align(Alignment.CenterHorizontally)) {
      Text("Borrar lista", color = MaterialTheme.colorScheme.error)
    }
  }
  if (deleting) ConfirmDialog("¿Borrar la lista «${list.name}»?", "Borrar", "Cancelar", onDelete) { deleting = false }
}

@Composable
private fun Progress(list: WordList) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Big("${list.percent}%")
      Label("completado", Modifier.padding(bottom = 6.dp), MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LinearProgressIndicator(progress = { list.percent / 100f }, Modifier.fillMaxWidth())
    Chips("${list.learned.size} de ${list.members.size} aprendidas")
  }
}

@Composable
fun FilterSummary(lines: List<String>) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Label("Filtros")
    lines.ifEmpty { listOf("Ninguno: todas las familias") }.forEach { Dotted(it) }
  }
}

@Composable
private fun Actions(onStudy: () -> Unit, onTest: () -> Unit) {
  Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
    Button(onClick = onStudy, Modifier.weight(1f)) { Text("Estudiar") }
    Button(onClick = onTest, Modifier.weight(1f)) { Text("Evaluar") }
  }
}
