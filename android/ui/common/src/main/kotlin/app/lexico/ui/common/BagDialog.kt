package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.lexico.model.Letters

@Composable
fun BagDialog(title: String, tiles: List<String>, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(summary(tiles), style = MaterialTheme.typography.bodySmall)
        LetterChips(counts(tiles))
      }
    },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

@Composable
fun BagCountDialog(title: String, tiles: Int, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(title) },
    text = { Text("$tiles fichas") },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

@Composable
private fun LetterChips(counts: List<Pair<String, Int>>) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    counts.forEach { (letter, count) -> LetterChip(letter, count) }
  }
}

@Composable
private fun LetterChip(letter: String, count: Int) {
  Row(
    modifier = Modifier
      .widthIn(min = 52.dp).height(36.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
      .padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("$count", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(letter, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
  }
}

private fun summary(tiles: List<String>): String {
  val vowels = tiles.count(Letters::isVowel)
  val blanks = tiles.count { it == Letters.BLANK }
  val consonants = tiles.size - vowels - blanks
  return "${tiles.size} fichas · $vowels vocales · $consonants consonantes" + blanksText(blanks)
}

private fun blanksText(blanks: Int): String = when (blanks) {
  0 -> ""
  1 -> " · 1 comodín"
  else -> " · $blanks comodines"
}

private fun counts(tiles: List<String>): List<Pair<String, Int>> {
  val counts = tiles.groupingBy { it }.eachCount()
  return (Letters.ALL + Letters.BLANK).filter { it in counts }.map { it to counts.getValue(it) }
}
