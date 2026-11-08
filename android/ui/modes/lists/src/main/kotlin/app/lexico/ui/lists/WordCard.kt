package app.lexico.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ColumnScope.WordCard(unit: Family, lexicon: Lexicon, onOpen: (String) -> Unit) {
  Surface(Modifier.fillMaxWidth().weight(1f), RoundedCornerShape(20.dp), MaterialTheme.colorScheme.surfaceContainerHigh) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), Arrangement.spacedBy(14.dp), Alignment.CenterHorizontally) {
      HeadWord(unit.head)
      Chips(*(listOf(kindText(unit)) + unit.traits(unit.head).map { it.label }).toTypedArray())
      MeaningLine(unit.head, lexicon)
      WordGroups {
        Details(unit, lexicon) { onOpen(WORD_KEY + it) }
        WordGroup("Similares", lexicon.similar(unit).map { it.word }, lexicon) { word -> lexicon.familyOf(word)?.let { onOpen(it.key) } }
      }
    }
  }
}

@Composable
internal fun ColumnScope.LooseWordCard(word: String, lexicon: Lexicon) {
  Surface(Modifier.fillMaxWidth().weight(1f), RoundedCornerShape(20.dp), MaterialTheme.colorScheme.surfaceContainerHigh) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), Arrangement.spacedBy(14.dp), Alignment.CenterHorizontally) {
      HeadWord(word)
      Text(OUTSIDE, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      MeaningLine(word, lexicon)
    }
  }
}

@Composable
private fun HeadWord(key: String) {
  BasicText(
    WordKeys.upper(key), Modifier.fillMaxWidth(), maxLines = 1,
    autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 56.sp),
    style = TextStyle(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurface),
  )
}

@Composable
private fun Details(unit: Family, lexicon: Lexicon, onWord: (String) -> Unit) {
  when {
    unit.isWord -> lexicon.owners(unit).forEach { WordGroup("Familia de ${WordKeys.upper(it.head)}", it.words, lexicon, onWord) }
    unit.verbal -> WordGroup("Familia", unit.words.drop(1), lexicon, onWord)
    else -> WordGroup("Familia", unit.words, lexicon, onWord)
  }
}

@Composable
private fun WordGroups(content: @Composable ColumnScope.() -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.Start, content = content)
}

@Composable
private fun WordGroup(title: String, words: List<String>, lexicon: Lexicon, onWord: (String) -> Unit) {
  if (words.isEmpty()) return
  Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.Start) {
    Label(title, Modifier.padding(start = WORD_INSET, top = 8.dp))
    AlignedWords(words, lexicon, onWord)
  }
}

@Composable
private fun AlignedWords(words: List<String>, lexicon: Lexicon, onWord: (String) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    words.forEach { WordLine(it, lexicon.tag(it), onWord) }
  }
}

@Composable
private fun WordLine(word: String, tag: String?, onWord: (String) -> Unit) {
  Row(
    Modifier.clip(RoundedCornerShape(8.dp)).clickable { onWord(word) }.padding(horizontal = WORD_INSET),
    Arrangement.spacedBy(8.dp), Alignment.CenterVertically,
  ) {
    Text(WordKeys.upper(word), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
    tag?.let { Text("($it)", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
  }
}

internal fun kindText(unit: Family): String = when {
  unit.isWord && unit.verbal && unit.nonVerbal -> "Verbal y no verbal"
  unit.isWord -> if (unit.verbal) "Forma verbal" else "No verbo"
  unit.verbal -> "Verbo"
  else -> "No verbo"
}

private const val WORD_KEY = "w:"

private val WORD_INSET = 10.dp
private const val OUTSIDE = "No está en el léxico de estudio (palabras válidas de hasta 9 letras)."
