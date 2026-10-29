package app.lexico.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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

private const val FULL_RACK = 7

private val TILE_GAP = 4.dp

private val TRAY_PADDING = 4.dp

@Composable
fun PlacingRack(c: TilePlacer, style: BoardStyle, enabled: Boolean, size: Dp = 46.dp) {
  BoxWithConstraints(Modifier.fillMaxWidth().rackDrops(c)) {
    val fitted = fittedTile(size, maxWidth)
    Row(Modifier.fillMaxWidth().tray(style), horizontalArrangement = Arrangement.spacedBy(TILE_GAP, Alignment.CenterHorizontally)) {
      c.order.forEach { i -> PlacingRackTile(c, i, style, enabled, fitted) }
    }
    DraggedTile(c, style, fitted)
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

private fun fittedTile(size: Dp, width: Dp): Dp =
  minOf(size, (width - TRAY_PADDING * 2 - TILE_GAP * (FULL_RACK - 1)) / FULL_RACK)

private fun Modifier.tray(style: BoardStyle): Modifier {
  val color = style.tray ?: return this
  return background(color, RoundedCornerShape(8.dp)).padding(TRAY_PADDING)
}
