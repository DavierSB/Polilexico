package app.lexico.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lexico.model.Letters
import app.lexico.model.Tile
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile

internal val TILE = 36.dp

@Composable
internal fun PinnedCard(tiles: List<String>, pins: MutableMap<Int, Int>, drag: PinDrag, style: BoardStyle) {
  Column(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text("Letras fijadas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      tiles.indices.forEach { slot -> PinSlot(pins[slot]?.let(tiles::get), style, Modifier.pinSlot(drag, slot)) { pins.remove(slot) } }
    }
  }
}

@Composable
internal fun LetterTile(letter: String, style: BoardStyle, marked: Boolean = false) {
  Box(contentAlignment = Alignment.Center) {
    RackTile(letter, style, size = TILE, marked = marked)
    if (letter == Letters.BLANK) Text("?", color = style.blank, fontWeight = FontWeight.Bold)
  }
}

internal fun fitsPins(word: String, pins: Map<Int, String>): Boolean {
  val tiles = Letters.tiles(word)
  return pins.all { (slot, letter) -> tiles.getOrNull(slot)?.let { fits(it, letter) } ?: false }
}

@Composable
private fun PinSlot(letter: String?, style: BoardStyle, modifier: Modifier, onClear: () -> Unit) {
  if (letter != null) Box(modifier.clickable(onClick = onClear)) { LetterTile(letter, style) }
  else EmptySlot(modifier)
}

@Composable
private fun EmptySlot(modifier: Modifier) {
  val shape = RoundedCornerShape(6.dp)
  Box(modifier.size(TILE).border(1.5.dp, MaterialTheme.colorScheme.outline, shape).background(MaterialTheme.colorScheme.surface, shape))
}

private fun fits(tile: Tile, letter: String): Boolean = if (letter == Letters.BLANK) tile.blank else tile.letter == letter
