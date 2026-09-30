package app.lexico.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import app.lexico.model.Board
import app.lexico.model.Position

private const val SCORE_MS = 1600

private const val HIGH_SCORE = 40

@Composable
internal fun rememberScorePopup(board: Board, score: Int?): ScorePopup? {
  val shown = score != null && board.latest.isNotEmpty()
  val progress = remember { Animatable(1f) }
  LaunchedEffect(board.latest, shown) {
    progress.snapTo(if (shown) 0f else 1f)
    progress.animateTo(1f, tween(SCORE_MS, easing = LinearEasing))
  }
  return if (shown) scorePopup(board, score!!, progress) else null
}

private fun scorePopup(board: Board, score: Int, progress: Animatable<Float, AnimationVector1D>): ScorePopup {
  val vertical = isVertical(board)
  val square = if (vertical) wordTop(board) else lastSquare(board.latest)
  return ScorePopup(score, square, vertical, progress)
}

@Stable
internal class ScorePopup(
  val score: Int,
  val square: Position,
  val vertical: Boolean,
  private val progress: Animatable<Float, AnimationVector1D>,
) {
  fun ink(style: BoardStyle): Color = if (score >= HIGH_SCORE) style.highScoreInk else style.scoreInk

  val rise: Float get() = progress.value

  val visible: Boolean get() = rise < 1f

  val alpha: Float
    get() = when {
      rise < 0.12f -> rise / 0.12f
      rise > 0.65f -> (1f - rise) / 0.35f
      else -> 1f
    }
}

internal fun DrawScope.drawScoreText(text: TextLayoutResult, center: Offset, alpha: Float) {
  val box = Size(text.size.width.toFloat(), text.size.height.toFloat())
  drawText(text, topLeft = clampedTopLeft(center, box), alpha = alpha)
}

private fun DrawScope.clampedTopLeft(center: Offset, box: Size): Offset = Offset(
  (center.x - box.width / 2).coerceIn(0f, maxOf(0f, size.width - box.width)),
  (center.y - box.height / 2).coerceIn(0f, maxOf(0f, size.height - box.height)),
)

internal fun lastSquare(latest: Set<Position>): Position = latest.maxWith(compareBy({ it.row }, { it.column }))

private fun isVertical(board: Board): Boolean {
  val latest = board.latest
  if (latest.size > 1) return latest.map { it.column }.distinct().size == 1
  val p = latest.first()
  val column = hasTile(board, p, -1, 0) || hasTile(board, p, 1, 0)
  val row = hasTile(board, p, 0, -1) || hasTile(board, p, 0, 1)
  return column && !row
}

private fun wordTop(board: Board): Position {
  var top = board.latest.minBy { it.row }
  while (hasTile(board, top, -1, 0)) top = Position(top.row - 1, top.column)
  return top
}

private fun hasTile(board: Board, p: Position, dRow: Int, dColumn: Int): Boolean {
  val row = p.row + dRow
  val column = p.column + dColumn
  return row in 0 until Board.SIZE && column in 0 until Board.SIZE && board[row, column] != null
}
