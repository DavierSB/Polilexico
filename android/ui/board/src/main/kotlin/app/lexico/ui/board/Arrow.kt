package app.lexico.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.lexico.model.Position

data class Arrow(val position: Position, val vertical: Boolean)

internal fun DrawScope.drawArrow(style: BoardStyle, arrow: Arrow, corner: Offset, side: Float) {
  val edge = side * style.lineWidth
  drawRect(style.arrow, corner + Offset(edge / 2, edge / 2), Size(side - edge, side - edge))
  val shape = ArrowShape(arrow.vertical, corner, side)
  drawLine(style.arrowInk, shape.tail, shape.base, strokeWidth = side * 0.11f, cap = StrokeCap.Round)
  drawPath(shape.head(), style.arrowInk)
}

private class ArrowShape(private val vertical: Boolean, corner: Offset, side: Float) {
  private val tip = side * 0.26f
  private val center = corner + Offset(side / 2, side / 2)
  private val reach = side / 2 - side * 0.20f

  val tail: Offset = along(-reach)
  private val point: Offset = along(reach)
  val base: Offset = along(reach - tip)

  fun head(): Path = Path().apply {
    moveTo(point.x, point.y)
    corner(1f).let { lineTo(it.x, it.y) }
    corner(-1f).let { lineTo(it.x, it.y) }
    close()
  }

  private fun corner(sign: Float): Offset = base + across(sign * tip * 0.8f)

  private fun along(d: Float): Offset = center + if (vertical) Offset(0f, d) else Offset(d, 0f)

  private fun across(d: Float): Offset = if (vertical) Offset(d, 0f) else Offset(0f, d)
}
