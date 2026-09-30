package app.lexico.ui.classic

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.RackTile
import app.lexico.ui.board.FaceDownTile

@Composable
fun OpponentRack(hand: OpponentRack, style: BoardStyle, size: Dp = 22.dp) {
  when (hand) {
    is OpponentRack.Hidden -> repeat(hand.tiles) { FaceDownTile(style, size) }
    is OpponentRack.Visible -> hand.tiles.forEach { RackTile(it, style, size) }
  }
}
