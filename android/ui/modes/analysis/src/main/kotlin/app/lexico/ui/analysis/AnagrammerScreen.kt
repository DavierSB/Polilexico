package app.lexico.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.model.Letters
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile
import app.lexico.ui.common.BarTextButton
import app.lexico.ui.common.Header
import app.lexico.ui.lists.AppliedFilters
import app.lexico.ui.lists.FilterSpec
import app.lexico.ui.lists.FilterType
import kotlinx.coroutines.CancellationException

private const val MAX_TILES = 15
private const val MAX_BLANKS = 2
internal const val MIN_TILES = 2

fun interface Anagrams {
  suspend fun of(tiles: List<String>): List<String>
}

@Composable
fun AnagrammerScreen(style: BoardStyle, anagrams: Anagrams, onBack: () -> Unit) {
  val tiles = remember { mutableStateListOf<String>() }
  val found = remember { mutableStateOf<List<String>?>(null) }
  var spec by remember { mutableStateOf(FilterSpec()) }
  LaunchedEffect(tiles.toList()) { found.value = search(anagrams, tiles.toList()) }
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Header("Anagramador", onBack) { BarTextButton("Vaciar", tiles::clear) }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      RackCard(tiles, style)
      AppliedFilters(spec, FilterType.ANAGRAM) { spec = it }
      AnagramResults(tiles.size, found.value, spec)
    }
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
private fun RackCard(tiles: MutableList<String>, style: BoardStyle) {
  Box(
    Modifier.fillMaxWidth().heightIn(min = 56.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(10.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    if (tiles.isEmpty()) Text("Atril vacío", color = MaterialTheme.colorScheme.onSurfaceVariant)
    else RackTiles(tiles, style)
  }
}

@Composable
private fun RackTiles(tiles: MutableList<String>, style: BoardStyle) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    tiles.forEachIndexed { i, l -> RackKey(l, style) { tiles.removeAt(i) } }
  }
}

@Composable
private fun RackKey(letter: String, style: BoardStyle, onClick: () -> Unit) {
  Box(Modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
    RackTile(letter, style, size = 36.dp)
    if (letter == Letters.BLANK) Text("?", color = style.blank, fontWeight = FontWeight.Bold)
  }
}
