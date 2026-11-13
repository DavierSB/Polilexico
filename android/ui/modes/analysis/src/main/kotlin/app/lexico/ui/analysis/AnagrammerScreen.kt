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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import app.lexico.model.Letters
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.BarTextButton
import app.lexico.ui.common.Header
import kotlinx.coroutines.CancellationException

private const val MAX_TILES = 15
private const val MAX_BLANKS = 2
internal const val MIN_TILES = 2

fun interface Anagrams {
  suspend fun of(tiles: List<String>): List<String>
}

@Composable
fun AnagrammerScreen(style: BoardStyle, anagrams: Anagrams, onBack: () -> Unit) {
  val rack = remember { AnagramRack() }
  val drag = remember { PinDrag(rack::pin) }
  val found = remember { mutableStateOf<List<String>?>(null) }
  val focus = remember { FocusRequester() }
  LaunchedEffect(rack.tiles.toList()) { found.value = search(anagrams, rack.tiles.toList()) }
  Box(Modifier.fillMaxSize().onGloballyPositioned { drag.root = it }) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Header("Anagramador", onBack) { BarTextButton("Vaciar", rack::clear) }
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RackCard(rack, drag, style) { focus.requestFocus() }
        if (rack.tiles.isNotEmpty()) PinnedCard(rack.tiles, rack.pins, drag, style)
        AnagramResults(rack.tiles.size, found.value, rack.pinnedLetters())
      }
      LetterPalette(style, marked = null, modifier = Modifier.fillMaxWidth(), onLetter = rack::add)
    }
    DraggedPin(drag, rack.tiles, style)
    SoftKeys(focus, onTile = rack::add) { rack.tiles.lastIndex.takeIf { it >= 0 }?.let(rack::remove) }
  }
}

private class AnagramRack {
  val tiles = mutableStateListOf<String>()
  val pins = mutableStateMapOf<Int, Int>()

  fun add(letter: String) {
    if (tiles.size >= MAX_TILES) return
    if (letter == Letters.BLANK && tiles.count { it == Letters.BLANK } >= MAX_BLANKS) return
    tiles.add(letter)
  }

  fun remove(i: Int) {
    pins.clear()
    tiles.removeAt(i)
  }

  fun clear() {
    pins.clear()
    tiles.clear()
  }

  fun pin(slot: Int, tile: Int) {
    if (slot >= tiles.size || tile >= tiles.size) return
    pins.entries.removeAll { it.value == tile }
    pins[slot] = tile
  }

  fun isPinned(tile: Int): Boolean = tile in pins.values

  fun pinnedLetters(): Map<Int, String> = pins.mapValues { tiles[it.value] }
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

@Composable
private fun RackCard(rack: AnagramRack, drag: PinDrag, style: BoardStyle, onType: () -> Unit) {
  val keyboard = LocalSoftwareKeyboardController.current
  Box(
    Modifier.fillMaxWidth().heightIn(min = 56.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).clickable { onType(); keyboard?.show() }.padding(10.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    if (rack.tiles.isEmpty()) Text("Atril vacío", color = MaterialTheme.colorScheme.onSurfaceVariant)
    else RackTiles(rack, drag, style)
  }
}

@Composable
private fun RackTiles(rack: AnagramRack, drag: PinDrag, style: BoardStyle) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    rack.tiles.forEachIndexed { i, l -> RackKey(l, style, rack.isPinned(i), Modifier.pinSource(drag, i)) { rack.remove(i) } }
  }
}

@Composable
private fun RackKey(letter: String, style: BoardStyle, pinned: Boolean, modifier: Modifier, onClick: () -> Unit) {
  Box(modifier.alpha(if (pinned) 0.4f else 1f).clickable(onClick = onClick)) { LetterTile(letter, style) }
}
