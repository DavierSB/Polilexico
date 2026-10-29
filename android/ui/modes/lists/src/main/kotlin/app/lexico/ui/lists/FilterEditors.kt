package app.lexico.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import app.lexico.ui.common.InfoButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.RadioOption

@Composable
fun FilterEditorDialog(type: FilterType, spec: FilterSpec, onApply: (FilterSpec) -> Unit, onCancel: () -> Unit) {
  var draft by remember(type) { mutableStateOf(type.started(spec)) }
  AlertDialog(
    onDismissRequest = onCancel,
    title = { TitleWithInfo(type) },
    text = { Column(Modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(10.dp)) { FilterEditor(type, draft) { draft = it } } },
    confirmButton = { TextButton(onClick = { onApply(draft) }) { Text("Aplicar") } },
    dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } },
  )
}

@Composable
private fun FilterEditor(type: FilterType, spec: FilterSpec, onSpec: (FilterSpec) -> Unit) {
  when (type) {
    FilterType.KIND -> Choices(listOf("Verbos", "No verbos"), spec.kind.ordinal - 1) { onSpec(spec.copy(kind = Kind.entries[it + 1])) }
    FilterType.LENGTH -> LengthSteppers(spec, onSpec)
    FilterType.COUNTS -> CountSteppers(spec, onSpec)
    FilterType.CONTAINS -> LettersField(spec, onSpec)
    FilterType.FIXED -> Positions(spec, fromEnd = false, onSpec)
    FilterType.FIXED_END -> Positions(spec, fromEnd = true, onSpec)
  }
}

@Composable
private fun Choices(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
  options.forEachIndexed { i, text -> RadioOption(text, i == selected) { onSelect(i) } }
}

@Composable
private fun LengthSteppers(spec: FilterSpec, onSpec: (FilterSpec) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    LabeledStepper("Mín", "${spec.minLength}", Modifier.weight(1f),
      onMinus = { onSpec(spec.copy(minLength = spec.minLength - 1)) }.takeIf { spec.minLength > MIN_TILES },
      onPlus = { onSpec(spec.copy(minLength = spec.minLength + 1)) }.takeIf { spec.minLength < spec.maxLength })
    LabeledStepper("Máx", "${spec.maxLength}", Modifier.weight(1f),
      onMinus = { onSpec(spec.copy(maxLength = spec.maxLength - 1)) }.takeIf { spec.maxLength > spec.minLength },
      onPlus = { onSpec(spec.copy(maxLength = spec.maxLength + 1)) }.takeIf { spec.maxLength < MAX_TILES })
  }
}

@Composable
private fun CountSteppers(spec: FilterSpec, onSpec: (FilterSpec) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    CountStepper("Vocales", spec.vowels, Modifier.weight(1f)) { onSpec(spec.copy(vowels = it)) }
    CountStepper("Consonantes", spec.consonants, Modifier.weight(1f)) { onSpec(spec.copy(consonants = it)) }
  }
  Text("«—» = cualquier número.")
}

@Composable
private fun CountStepper(label: String, value: Int?, modifier: Modifier, onValue: (Int?) -> Unit) {
  LabeledStepper(label, value?.toString() ?: "—", modifier,
    onMinus = { onValue(if (value == 0) null else value!! - 1) }.takeIf { value != null },
    onPlus = { onValue((value ?: -1) + 1) }.takeIf { (value ?: -1) < MAX_TILES })
}

@Composable
private fun LettersField(spec: FilterSpec, onSpec: (FilterSpec) -> Unit) {
  OutlinedTextField(
    value = spec.contains, onValueChange = { onSpec(spec.copy(contains = it)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
    placeholder = { Text("p. ej. aerch") }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
  )
  val tiles = spec.filter().contains
  if (tiles.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { tiles.forEach { MiniTile(it) } }
  Text("«aa» exige dos A.", color = MaterialTheme.colorScheme.onSurfaceVariant)
  SimpleRest(spec.simpleOnly) { onSpec(spec.copy(simpleOnly = it)) }
}

@Composable
private fun SimpleRest(checked: Boolean, onChange: (Boolean) -> Unit) {
  Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onChange(!checked) }) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked, onChange)
      Text("Las demás letras serán simples", fontWeight = FontWeight.Bold)
    }
    Text(SIMPLE_NOTE, Modifier.padding(start = 12.dp, bottom = 6.dp), style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun TitleWithInfo(type: FilterType) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(type.title, Modifier.weight(1f, fill = false))
    InfoButton(type.title, type.info)
  }
}

@Composable
private fun Positions(spec: FilterSpec, fromEnd: Boolean, onSpec: (FilterSpec) -> Unit) {
  if (fromEnd) SlotRow(spec.maxLength, spec.fixedEnd, fromEnd = true) { onSpec(spec.copy(fixedEnd = it)) }
  else SlotRow(spec.maxLength, spec.fixed, fromEnd = false) { onSpec(spec.copy(fixed = it)) }
  Text(if (fromEnd) "La casilla 1 es la última letra." else "La casilla 1 es la primera letra.")
}

private const val SIMPLE_NOTE =
  "Se consideran simples las vocales (A, E, I, O, U) y las consonantes C, D, L, N, R, S y T. Las letras que escribas arriba pueden ser cualquiera."
