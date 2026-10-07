package app.lexico.ui.board

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.lexico.model.Position

data class Arrow(val position: Position, val vertical: Boolean)

internal fun DrawScope.drawArrow(style: BoardStyle, arrow: Arrow, corner: Offset, side: Float) {
  drawFrame(style, corner, side)
  val shape = ArrowShape(arrow.vertical, corner, side)
  drawShape(shape, style.arrow, side * (SHAFT + HALO), halo = true)
  drawShape(shape, style.arrowInk, side * SHAFT, halo = false)
}

private fun DrawScope.drawFrame(style: BoardStyle, corner: Offset, side: Float) {
  val inset = side * style.lineWidth / 2 + side * FRAME_INSET
  val topLeft = corner + Offset(inset, inset)
  val size = Size(side - 2 * inset, side - 2 * inset)
  val radius = CornerRadius(side * style.rounding.coerceIn(0.06f, 0.16f))
  val frame = side * FRAME
  drawRoundRect(style.arrowInk, topLeft, size, radius, style = Stroke(frame + maxOf(1f, side * FRAME_EDGE)))
  drawRoundRect(style.arrow, topLeft, size, radius, style = Stroke(frame))
}

private fun DrawScope.drawShape(shape: ArrowShape, color: Color, width: Float, halo: Boolean) {
  drawLine(color, shape.tail, shape.base, strokeWidth = width, cap = StrokeCap.Round)
  drawPath(shape.head(), color)
  if (halo) drawPath(shape.head(), color, style = Stroke(width - shape.shaft, join = StrokeJoin.Round))
}

private class ArrowShape(private val vertical: Boolean, corner: Offset, side: Float) {
  private val tip = side * 0.26f
  private val center = corner + Offset(side / 2, side / 2)
  private val reach = side / 2 - side * 0.20f

  val shaft = side * SHAFT
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

private const val SHAFT = 0.11f
private const val HALO = 0.08f
private const val FRAME = 0.09f
private const val FRAME_EDGE = 0.04f
private const val FRAME_INSET = 0.085f
