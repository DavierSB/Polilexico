package app.lexico.ui.recall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile

private val MAX_TILE = 52.dp
private val TILE_GAP = 4.dp

/** El verde de los aciertos. */
internal val HIT = Color(0xFF2E7D32)

/** Una fila de `count` fichas que siempre cabe en el ancho: cada una mide a lo sumo [MAX_TILE]. */
@Composable
internal fun TileLine(count: Int, tile: @Composable (index: Int, size: Dp) -> Unit) {
  BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    val size = minOf(MAX_TILE, (maxWidth - TILE_GAP * (count - 1)) / count)
    Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
      repeat(count) { tile(it, size) }
    }
  }
}

/** Una ficha que ya esta en el tablero: sobre su casilla, que asoma alrededor. */
@Composable
internal fun FixedTile(letter: String, style: BoardStyle, size: Dp) {
  Box(Modifier.size(size).background(style.square), contentAlignment = Alignment.Center) {
    RackTile(letter, style, size * 0.84f)
  }
}

/** Una casilla vacia de la palabra. */
@Composable
internal fun EmptySlot(style: BoardStyle, size: Dp) {
  val shape = RoundedCornerShape(size * style.rounding)
  Box(Modifier.size(size).border(2.dp, style.tileBorder.takeIf { it != Color.Transparent } ?: style.lines, shape))
}
