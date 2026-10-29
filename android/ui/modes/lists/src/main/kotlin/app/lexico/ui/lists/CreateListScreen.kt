package app.lexico.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Header
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CreateListScreen(lexicon: Lexicon, onBack: () -> Unit, onSave: (name: String, spec: FilterSpec, members: List<String>) -> Unit) {
  var spec by remember { mutableStateOf(FilterSpec()) }
  var naming by remember { mutableStateOf(false) }
  val results by produceState<List<Family>?>(null, spec) { value = null; value = withContext(Dispatchers.Default) { lexicon.filter(spec) } }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Header("Crear lista", onBack)
    AppliedFilters(spec) { spec = it }
    HorizontalDivider(Modifier.padding(vertical = 6.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
    Count(results, spec)
    Button(onClick = { naming = true }, Modifier.fillMaxWidth(), enabled = !results.isNullOrEmpty()) { Text("Guardar lista") }
  }
  if (naming) NameDialog({ naming = false }) { name -> results?.let { onSave(name, spec, it.take(MAX_MEMBERS).map(Family::key)) } }
}

@Composable
private fun Count(results: List<Family>?, spec: FilterSpec) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Big(results?.size?.let(::thousands) ?: "…")
      Label("familias", Modifier.padding(bottom = 6.dp), MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(if (spec.isEmpty) "en total" else "cumplen estos filtros", style = MaterialTheme.typography.bodyLarge)
    if ((results?.size ?: 0) > MAX_MEMBERS) Text(
      "Se guardarán las primeras $MAX_MEMBERS, las más importantes.",
      color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Composable
private fun NameDialog(onCancel: () -> Unit, onSave: (String) -> Unit) {
  var name by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onCancel,
    title = { Text("Nombre de la lista") },
    text = {
      OutlinedTextField(value = name, onValueChange = { name = it.take(MAX_NAME) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("p. ej. Verbos con Z") })
    },
    confirmButton = { TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("Guardar") } },
    dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } },
  )
}

private fun thousands(n: Int): String = String.format(Locale.forLanguageTag("es"), "%,d", n)

private const val MAX_NAME = 40
