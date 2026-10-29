package app.lexico.ui.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.model.Letters
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile
import app.lexico.ui.common.BarTextButton
import app.lexico.ui.common.DictionaryWord
import app.lexico.ui.common.Header
import kotlinx.coroutines.CancellationException

private const val MAX_TILES = 15
private const val MAX_BLANKS = 2
private const val MIN_TILES = 2
private const val HINT = "Toca letras para formar el atril (? = comodín, hasta $MAX_BLANKS). Toca una ficha del atril para quitarla."

fun interface Anagrams {
  suspend fun of(tiles: List<String>): List<String>
}

@Composable
fun AnagrammerScreen(style: BoardStyle, anagrams: Anagrams, onBack: () -> Unit) {
  val tiles = remember { mutableStateListOf<String>() }
  val found = remember { mutableStateOf<List<String>?>(null) }
  LaunchedEffect(tiles.toList()) { found.value = search(anagrams, tiles.toList()) }
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Header("Anagramador", onBack) { BarTextButton("Vaciar", tiles::clear) }
    TileRow(tiles, style)
    Results(tiles.size, found.value)
    LetterPalette(style, marked = null, modifier = Modifier.fillMaxWidth()) { add(tiles, it) }
  }
}

private suspend fun search(anagrams: Anagrams, tiles: List<String>): List<String>? {
  if (tiles.size < MIN_TILES) return null
  return try {
    anagrams.of(tiles)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    emptyList()
  }
}

private fun add(tiles: MutableList<String>, letter: String) {
  if (tiles.size >= MAX_TILES) return
  if (letter == Letters.BLANK && tiles.count { it == Letters.BLANK } >= MAX_BLANKS) return
  tiles.add(letter)
}

@Composable
private fun TileRow(tiles: MutableList<String>, style: BoardStyle) {
  if (tiles.isEmpty()) return Text(HINT, style = MaterialTheme.typography.bodySmall)
  FlowRow(Modifier.fillMaxWidth().heightIn(min = 36.dp), Arrangement.spacedBy(3.dp), Arrangement.spacedBy(3.dp)) {
    tiles.forEachIndexed { i, l -> Box(Modifier.clickable { tiles.removeAt(i) }) { RackTile(l, style, size = 32.dp) } }
  }
}

@Composable
private fun ColumnScope.Results(count: Int, found: List<String>?) {
  Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    when {
      count < MIN_TILES -> Unit
      found == null -> Text("Buscando…", style = MaterialTheme.typography.bodySmall)
      found.isEmpty() -> Text("No hay anagramas válidos con estas fichas.", style = MaterialTheme.typography.bodyMedium)
      else -> Words(found)
    }
  }
}

@Composable
private fun Words(words: List<String>) {
  Text(if (words.size == 1) "1 anagrama" else "${words.size} anagramas", style = MaterialTheme.typography.titleSmall)
  FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    words.forEach { DictionaryWord(it, fontSize = 22.sp) }
  }
}
