package app.lexico.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.lexico.model.dictionary.Meaning
import app.lexico.model.dictionary.Sense
import app.lexico.model.dictionary.Summary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MeaningLine(word: String, lexicon: Lexicon) {
  val meanings by produceState<List<Meaning>?>(null, word) { value = withContext(Dispatchers.IO) { lexicon.meanings(word) } }
  if (meanings?.isEmpty() == true) return RaeButton(word)
  var open by remember { mutableStateOf(false) }
  Surface(Modifier.fillMaxWidth().clickable { open = true }, RoundedCornerShape(12.dp), MaterialTheme.colorScheme.surfaceContainerHighest) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
      Text(summaryText(meanings), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
      Text("Ver más", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
  }
  if (open) MeaningsDialog(word, meanings.orEmpty()) { open = false }
}

@Composable
private fun RaeButton(word: String) {
  var rae by remember { mutableStateOf(false) }
  OutlinedButton(onClick = { rae = true }, Modifier.fillMaxWidth()) { Text("Consultar en la RAE") }
  if (rae) RaeDialog(word) { rae = false }
}

@Composable
private fun MeaningsDialog(word: String, meanings: List<Meaning>, onClose: () -> Unit) {
  var rae by remember { mutableStateOf(false) }
  AlertDialog(
    onDismissRequest = onClose,
    confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } },
    dismissButton = { TextButton(onClick = { rae = true }) { Text("Consultar en la RAE") } },
    title = { Text(WordKeys.upper(word)) },
    text = { Column(Modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp)) { MeaningList(meanings, word) } },
  )
  if (rae) RaeDialog(word) { rae = false }
}

@Composable
private fun MeaningList(meanings: List<Meaning>, word: String) {
  if (meanings.isEmpty()) return Muted(NO_MEANING)
  meanings.forEach { MeaningBlock(it, word) }
  Muted("Fuente: " + meanings.flatMap { it.senses }.map { it.source.label }.distinct().joinToString(", "))
}

@Composable
private fun MeaningBlock(meaning: Meaning, word: String) {
  Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
    heading(meaning, word)?.let { Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
    meaning.senses.forEachIndexed { i, sense -> SenseLine(i + 1, sense) }
  }
}

@Composable
private fun SenseLine(number: Int, sense: Sense) {
  Text(buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) { append("$number. ") }
    append(sense.text)
  }, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun Muted(text: String) {
  Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun summaryText(meanings: List<Meaning>?) = when {
  meanings == null -> "Buscando el significado…"
  else -> Summary.of(meanings)?.text ?: NO_MEANING
}

private fun heading(meaning: Meaning, word: String): String? = when {
  meaning.relation.isNotEmpty() -> meaning.relation
  meaning.lemma != WordKeys.show(word) -> meaning.lemma
  else -> null
}

private const val NO_MEANING = "No está en el diccionario sin conexión."
