package app.lexico.ui.board

import androidx.compose.ui.geometry.Offset
import app.lexico.model.Board
import app.lexico.model.Position

internal class BoardGeometry(width: Float, showCoordinates: Boolean) {
  private val margin = if (showCoordinates) MARGIN else 0f

  val side: Float = width / (Board.SIZE + margin)

  val origin: Offset = Offset(side * margin, side * margin)

  fun corner(p: Position): Offset = origin + Offset(p.column * side, p.row * side)

  fun positionAt(point: Offset): Position? {
    if (point.x < origin.x || point.y < origin.y) return null
    val p = Position(((point.y - origin.y) / side).toInt(), ((point.x - origin.x) / side).toInt())
    return p.takeIf { it.onBoard }
  }

  private companion object {
    const val MARGIN = 0.6f
  }
}
