package app.lexico.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.DictionaryWord

@Composable
internal fun AnagramResults(tiles: Int, found: List<String>?, pins: Map<Int, String>) {
  if (tiles < MIN_TILES) return
  ResultCard {
    when {
      found == null -> Text("Buscando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
      found.isEmpty() -> Empty("Sin anagramas")
      else -> Shown(found.filter { fitsPins(it, pins) })
    }
  }
}

@Composable
private fun Shown(words: List<String>) {
  if (words.isEmpty()) return Empty("Ninguno con esas letras fijadas")
  Count(words.size, if (words.size == 1) "anagrama" else "anagramas")
  FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    words.forEach { DictionaryWord(it, fontSize = 22.sp) }
  }
}

@Composable
private fun ResultCard(content: @Composable () -> Unit) {
  Column(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) { content() }
}

@Composable
private fun Count(n: Int, label: String) {
  Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("$n", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
    Text(label, Modifier.padding(bottom = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun Empty(text: String) {
  Text(text, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
