package app.lexico.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.BarIconButton
import app.lexico.ui.common.ConfirmDialog
import app.lexico.ui.common.Header
import app.lexico.ui.common.LexicoIcons

data class InProgressItem(val id: String, val title: String, val detail: String, val score: String? = null, val note: String? = null)

@Composable
fun InProgressScreen(items: List<InProgressItem>, onBack: () -> Unit, onOpen: (String) -> Unit, onDelete: (Set<String>) -> Unit) {
  var selected by remember { mutableStateOf(emptySet<String>()) }
  var confirming by remember { mutableStateOf(false) }
  val selecting = selected.isNotEmpty()
  BackHandler(enabled = selecting) { selected = emptySet() }
  Column(Modifier.fillMaxSize()) {
    InProgressHeader(selected.size, onBack, onClear = { selected = emptySet() }, onTrash = { confirming = true })
    InProgressBody(items, selected, onOpen) { id -> selected = selected.toggle(id) }
  }
  if (confirming) DeleteDialog(selected.size, onDelete = { onDelete(selected); selected = emptySet(); confirming = false }) { confirming = false }
}

@Composable
private fun InProgressHeader(count: Int, onBack: () -> Unit, onClear: () -> Unit, onTrash: () -> Unit) {
  if (count == 0) return Header("Partidas en curso", onBack)
  Header(selectedText(count), onClear) { BarIconButton(LexicoIcons.Trash, "Borrar", onTrash) }
}

@Composable
private fun InProgressBody(items: List<InProgressItem>, selected: Set<String>, onOpen: (String) -> Unit, onToggle: (String) -> Unit) {
  if (items.isEmpty()) return EmptyList("No hay partidas sin terminar.")
  if (selected.isEmpty()) Hint()
  LazyColumn {
    items(items, key = { it.id }) { item ->
      InProgressRow(item, item.id in selected, selecting = selected.isNotEmpty(), onOpen = { onOpen(item.id) }) { onToggle(item.id) }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InProgressRow(item: InProgressItem, checked: Boolean, selecting: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
  val tint = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
  Row(
    Modifier.fillMaxWidth().background(tint).combinedClickable(onClick = if (selecting) onToggle else onOpen, onLongClick = onToggle)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (selecting) Checkbox(checked, { onToggle() })
    RowTexts(item, Modifier.weight(1f))
    item.score?.let { RowScore(it, item.note) }
  }
  HorizontalDivider()
}

@Composable
private fun RowTexts(item: InProgressItem, modifier: Modifier) {
  Column(modifier) {
    Text(item.title, fontWeight = FontWeight.Bold)
    Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun RowScore(score: String, note: String?) {
  Column(horizontalAlignment = Alignment.End) {
    Text(score, fontWeight = FontWeight.Bold)
    note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
  }
}

@Composable
private fun Hint() {
  Text("Mantén pulsada una partida para seleccionarla.", Modifier.padding(bottom = 4.dp),
    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DeleteDialog(count: Int, onDelete: () -> Unit, onKeep: () -> Unit) {
  val what = if (count == 1) "la partida seleccionada" else "las $count partidas seleccionadas"
  ConfirmDialog("¿Borrar $what? No se podrán continuar.", yes = "Borrar", no = "No", onYes = onDelete, onNo = onKeep)
}

private fun selectedText(count: Int): String = if (count == 1) "1 seleccionada" else "$count seleccionadas"

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id
