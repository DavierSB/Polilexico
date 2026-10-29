package app.lexico.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import app.lexico.ui.common.InfoButton
import app.lexico.ui.common.Mulish
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppliedFilters(spec: FilterSpec, onSpec: (FilterSpec) -> Unit) {
  var adding by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf<FilterType?>(null) }
  FilterCard(spec, { editing = it }) { onSpec(it.cleared(spec)) }
  OutlinedButton(onClick = { adding = true }, Modifier.fillMaxWidth()) { Text(if (spec.isEmpty) "Añadir filtro" else "Añadir más filtros") }
  if (adding) AddFilterDialog(spec, { adding = false }) { adding = false; editing = it }
  editing?.let { type -> FilterEditorDialog(type, spec, { onSpec(it); editing = null }) { editing = null } }
}

@Composable
private fun FilterCard(spec: FilterSpec, onEdit: (FilterType) -> Unit, onRemove: (FilterType) -> Unit) {
  if (spec.isEmpty) return
  Column {
    Label("Filtros", Modifier.padding(bottom = 4.dp))
    FilterType.entries.filter { it.isSet(spec) }.forEach { type -> FilterRow(type.line(spec)!!, type, onEdit, onRemove) }
  }
}

@Composable
private fun FilterRow(text: String, type: FilterType, onEdit: (FilterType) -> Unit, onRemove: (FilterType) -> Unit) {
  Row(
    Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).clickable { onEdit(type) },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Dotted(text, Modifier.weight(1f))
    TextButton(onClick = { onRemove(type) }) { Text("✕", fontSize = 18.sp) }
  }
}

@Composable
private fun AddFilterDialog(spec: FilterSpec, onCancel: () -> Unit, onPick: (FilterType) -> Unit) {
  val available = FilterType.entries.filter { !it.isSet(spec) }
  AlertDialog(
    onDismissRequest = onCancel,
    title = { Text("Añadir filtro") },
    text = { FilterChoices(available, onPick) },
    confirmButton = { TextButton(onClick = onCancel) { Text("Cancelar") } },
  )
}

@Composable
private fun FilterChoices(available: List<FilterType>, onPick: (FilterType) -> Unit) {
  if (available.isEmpty()) return Text("Ya están puestos todos los filtros. Toca uno para cambiarlo.")
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { available.forEach { FilterChoice(it) { onPick(it) } } }
}

@Composable
private fun FilterChoice(type: FilterType, onPick: () -> Unit) {
  Surface(Modifier.fillMaxWidth().clickable(onClick = onPick), RoundedCornerShape(12.dp), MaterialTheme.colorScheme.surfaceContainerHighest) {
    Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(type.title, Modifier.weight(1f), fontFamily = Mulish, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      InfoButton(type.title, type.info)
    }
  }
}
