package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class WordSection(val title: String, val plays: List<String>) {
  val words: List<String> get() = plays.mapNotNull(::playedWord).distinctBy { it.lowercase() }
}

@Composable
fun PlayedWordsDialog(sections: List<WordSection>, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
    title = { Text("Palabras de la partida") },
    text = { Column(Modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(4.dp)) { SectionsList(sections) } },
  )
}

@Composable
private fun SectionsList(sections: List<WordSection>) {
  Text("Toca una palabra para ver su ficha en el diccionario.", style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant)
  sections.filter { it.words.isNotEmpty() }.forEach { Section(it) }
}

@Composable
private fun Section(section: WordSection) {
  Column(Modifier.fillMaxWidth().padding(top = 10.dp), Arrangement.spacedBy(2.dp), Alignment.CenterHorizontally) {
    Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    section.words.forEach { DictionaryWord(it, fontSize = 22.sp) }
  }
}
