package app.lexico.ui.board

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import app.lexico.model.Board

fun DrawScope.drawStillBoard(board: Board, style: BoardStyle, measurer: TextMeasurer) {
  with(BoardPainter(style, measurer, BoardGeometry(size.width, style.showCoordinates))) { drawBoard(board, emptyMap(), null) }
}
