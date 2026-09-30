package app.lexico.ui.board

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp
import app.lexico.model.Board
import app.lexico.model.Bonus
import app.lexico.model.Position
import app.lexico.model.Tile

internal class BoardPainter(
  private val style: BoardStyle,
  private val measurer: TextMeasurer,
  private val geometry: BoardGeometry,
) {
  private val side = geometry.side
  private val edge = side * style.lineWidth

  fun DrawScope.drawBoard(board: Board, pending: Map<Position, Tile>, arrow: Arrow?) {
    drawRect(style.backdrop)
    if (style.showCoordinates) drawCoordinates()
    ALL_POSITIONS.forEach { drawCell(it, board, pending) }
    arrow?.let { drawArrow(style, it, geometry.corner(it.position), side) }
  }

  fun DrawScope.drawScore(popup: ScorePopup) {
    if (!popup.visible) return
    val text = measurer.measure("+${popup.score}", scoreTextStyle(side * SCORE_SIZE, popup.ink(style)))
    val start = scoreStart(popup, text.size.height.toFloat())
    drawScoreText(text, start - Offset(0f, popup.rise * side * 0.8f), popup.alpha)
  }

  private fun scoreStart(popup: ScorePopup, textHeight: Float): Offset {
    val lift = if (popup.vertical) textHeight / 2 else side * 0.1f
    return geometry.corner(popup.square) + Offset(side / 2, -lift)
  }

  fun DrawScope.drawLiveScore(live: LiveScore) {
    val text = measurer.measure("${live.points}", textStyle(side * LIVE_SIZE, style.arrowInk, FontWeight.Black))
    val box = Size(maxOf(text.size.width + side * 0.2f, side * 0.5f), text.size.height.toFloat())
    val topLeft = liveScoreTopLeft(geometry.corner(live.square), box)
    drawRoundRect(style.arrow, topLeft, box, CornerRadius(box.height / 2))
    drawText(text, topLeft = topLeft + Offset((box.width - text.size.width) / 2, 0f))
  }

  private fun DrawScope.drawCell(p: Position, board: Board, pending: Map<Position, Tile>) {
    val placed = board[p]
    val tentative = pending[p]
    when {
      placed != null -> drawTile(placed, geometry.corner(p), if (p in board.latest) style.latest ?: style.tile else style.tile)
      tentative != null -> drawTile(tentative, geometry.corner(p), style.pending)
      else -> drawSquare(Board.bonus(p), geometry.corner(p))
    }
  }

  private fun DrawScope.drawSquare(bonus: Bonus, corner: Offset) {
    drawRect(style.bonusColor(bonus), inset(corner), insetSize())
    val label = style.labels[bonus] ?: return
    val px = side * if (bonus == Bonus.CENTER) 0.55f else 0.3f
    drawCentered(label, corner + Offset(side / 2, side / 2), px, style.bonusText, FontWeight.Normal)
  }

  private fun DrawScope.drawTile(tile: Tile, corner: Offset, backdrop: Color) {
    drawTileBody(corner, backdrop)
    val withValue = style.showValues && !tile.blank
    val ink = if (tile.blank) style.blank else style.letter
    drawCentered(tile.toString(), letterCenter(corner, withValue), letterSize(tile), ink, style.fontWeight)
    if (withValue) drawCentered("${tile.value}", corner + Offset(side * 0.8f, side * 0.78f), side * 0.26f, style.value, FontWeight.Normal)
  }

  private fun DrawScope.drawTileBody(corner: Offset, backdrop: Color) {
    val radius = CornerRadius(side * style.rounding)
    drawRoundRect(backdrop, inset(corner), insetSize(), radius)
    if (style.tileBorder == Color.Transparent) return
    drawRoundRect(style.tileBorder, inset(corner), insetSize(), radius, style = Stroke(width = maxOf(1f, side * 0.035f)))
  }

  private fun DrawScope.drawCoordinates() {
    val half = geometry.origin.x / 2
    for (i in 0 until Board.SIZE) {
      val center = geometry.origin.x + (i + 0.5f) * side
      drawCentered("${i + 1}", Offset(center, half), side * 0.32f, style.coordinates, FontWeight.Normal)
      drawCentered("${'A' + i}", Offset(half, center), side * 0.32f, style.coordinates, FontWeight.Normal)
    }
  }

  private fun DrawScope.drawCentered(text: String, center: Offset, px: Float, color: Color, weight: FontWeight) {
    val layout: TextLayoutResult = measurer.measure(text, textStyle(px, color, weight))
    drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
  }

  private fun DrawScope.textStyle(px: Float, color: Color, weight: FontWeight): TextStyle =
    TextStyle(color = color, fontSize = (px / density / fontScale).sp, fontFamily = style.fontFamily, fontWeight = weight)

  private fun DrawScope.scoreTextStyle(px: Float, ink: Color): TextStyle = textStyle(px, ink, FontWeight.Black).copy(
    textGeometricTransform = TextGeometricTransform(scaleX = SCORE_NARROWING),
    shadow = Shadow(Color.Black.copy(alpha = 0.8f), blurRadius = side * 0.25f),
  )

  private fun DrawScope.liveScoreTopLeft(corner: Offset, box: Size): Offset = Offset(
    (corner.x + side - box.width / 2).coerceIn(0f, size.width - box.width),
    (corner.y - box.height / 2).coerceIn(0f, size.height - box.height),
  )

  private fun letterCenter(corner: Offset, withValue: Boolean): Offset =
    corner + if (withValue) Offset(side * 0.45f, side * 0.47f) else Offset(side / 2, side / 2)

  private fun letterSize(tile: Tile): Float = side * if (tile.letter.length > 1) 0.42f else 0.62f

  private fun inset(corner: Offset): Offset = corner + Offset(edge / 2, edge / 2)

  private fun insetSize(): Size = Size(side - edge, side - edge)

  private companion object {
    const val SCORE_SIZE = 1.1f
    const val SCORE_NARROWING = 0.7f

    const val LIVE_SIZE = 0.42f

    val ALL_POSITIONS = (0 until Board.SIZE).flatMap { row -> (0 until Board.SIZE).map { Position(row, it) } }
  }
}
