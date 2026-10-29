package app.lexico.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.lexico.ui.common.Header
import app.lexico.ui.common.LexicoIcons

@Composable
fun WordScreen(entry: String, lexicon: Lexicon, onBack: () -> Unit, onOpen: (String) -> Unit) {
  val unit = remember(entry) { lexicon.resolve(entry) }
  Column(Modifier.fillMaxSize().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header(WordKeys.upper(unit?.head ?: WordKeys.bare(entry)), onBack)
    EntryCard(entry, unit, lexicon, onOpen)
    WordActions(lexicon, onOpen)
  }
}

@Composable
fun WordSearchScreen(lexicon: Lexicon, onBack: () -> Unit) {
  var entry by rememberSaveable { mutableStateOf(lexicon.words.random().key) }
  WordScreen(entry, lexicon, onBack) { entry = WordKeys.entry(it) }
}

@Composable
private fun WordActions(lexicon: Lexicon, onOpen: (String) -> Unit) {
  var searching by remember { mutableStateOf(false) }
  Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = { searching = true }, Modifier.weight(1f).height(48.dp)) {
      Icon(LexicoIcons.Lookup, null, Modifier.size(18.dp))
      Text("Buscar palabra", Modifier.padding(start = 6.dp), fontWeight = FontWeight.Bold)
    }
    Button(onClick = { onOpen(lexicon.words.random().key) }, Modifier.weight(1f).height(48.dp)) {
      Text("Palabra al azar", fontWeight = FontWeight.Bold)
    }
  }
  if (searching) SearchDialog({ searching = false }) { searching = false; onOpen(it) }
}

@Composable
private fun SearchDialog(onClose: () -> Unit, onSearch: (String) -> Unit) {
  var text by rememberSaveable { mutableStateOf("") }
  val focus = remember { FocusRequester() }
  val submit = { if (WordKeys.parse(text).isNotEmpty()) onSearch(WordKeys.entry(text)) }
  AlertDialog(
    onDismissRequest = onClose,
    title = { Text("Buscar palabra") },
    text = {
      OutlinedTextField(
        text, { text = it }, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true, placeholder = { Text("Escribe una palabra") },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
      )
    },
    confirmButton = { TextButton(onClick = submit) { Text("Buscar") } },
    dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
  )
  LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun ColumnScope.EntryCard(entry: String, unit: Family?, lexicon: Lexicon, onOpen: (String) -> Unit) {
  if (unit == null) key(entry) { LooseWordCard(WordKeys.bare(entry), lexicon) }
  else key(unit.key) { WordCard(unit, lexicon, onOpen) }
}
