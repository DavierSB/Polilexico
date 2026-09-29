package app.lexico.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.model.Letters

/** Las letras que quedan, como en ISC: cada letra repetida tantas veces como quedan, en orden. */
@Composable
fun BagDialog(title: String, tiles: List<String>, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(summary(tiles), style = MaterialTheme.typography.bodySmall)
        Text(grouped(tiles), fontFamily = FontFamily.Monospace, fontSize = 17.sp, lineHeight = 24.sp)
      }
    },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

/** Solo cuantas fichas quedan, para las partidas que no muestran cuales. */
@Composable
fun BagCountDialog(title: String, tiles: Int, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text(title) },
    text = { Text("$tiles fichas") },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

/** "86 fichas · 38 vocales · 46 consonantes · 2 comodines". */
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

/** "AAAA  B  CC ...": cada letra repetida tantas veces como queda, en orden alfabetico. */
private fun grouped(tiles: List<String>): String {
  val counts = tiles.groupingBy { it }.eachCount()
  return (Letters.ALL + Letters.BLANK).filter { it in counts }.joinToString("  ") { it.repeat(counts.getValue(it)) }
}
