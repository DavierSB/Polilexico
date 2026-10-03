package app.lexico.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PlacingRack(c: TilePlacer, style: BoardStyle, enabled: Boolean, size: Dp = 46.dp) {
  Box(Modifier.fillMaxWidth().rackDrops(c)) {
    Row(Modifier.fillMaxWidth().tray(style), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
      c.order.forEach { i -> PlacingRackTile(c, i, style, enabled, size) }
    }
    DraggedTile(c, style, size)
  }
}

@Composable
fun TileRow(tiles: List<String>, style: BoardStyle, size: Dp, modifier: Modifier = Modifier) {
  Row(modifier.tray(style), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
    tiles.forEach { RackTile(it, style, size) }
  }
}

@Composable
private fun PlacingRackTile(c: TilePlacer, i: Int, style: BoardStyle, enabled: Boolean, size: Dp) {
  val isPlaced = i in c.placed.values
  val marked = if (c.exchanging) i in c.toExchange else i == c.selected
  val faded = isPlaced || i == c.drag.tile
  Box(
    Modifier.alpha(if (faded) 0.15f else 1f)
      .rackTileDrags(c, i, enabled = enabled && !isPlaced && !c.exchanging)
      .clickable(enabled = enabled && !isPlaced) { c.tapRack(i) },
  ) {
    RackTile(c.tiles[i], style, size = size, marked = marked)
  }
}

private fun Modifier.tray(style: BoardStyle): Modifier {
  val color = style.tray ?: return this
  return background(color, RoundedCornerShape(8.dp)).padding(4.dp)
}
