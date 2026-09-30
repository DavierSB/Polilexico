package app.lexico.ui.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import kotlin.math.hypot

internal fun DrawScope.drawDot(center: Offset, colors: ChartColors) {
  drawCircle(colors.background, (DOT_RADIUS + DOT_RING).toPx(), center)
  drawCircle(colors.dot, DOT_RADIUS.toPx(), center)
}

internal fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, style: TextStyle, topLeft: Offset) {
  drawText(measurer.measure(text, style), topLeft = topLeft)
}

internal fun DrawScope.drawCenteredLabel(measurer: TextMeasurer, text: String, style: TextStyle, x: Float, top: Float) {
  val layout = measurer.measure(text, style)
  val left = (x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
  drawText(layout, topLeft = Offset(left, top))
}

internal fun nearestGame(games: List<ChartGame>, tap: Offset, radius: Float, point: (Int, ChartGame) -> Offset): ChartGame? =
  games.indices.minByOrNull { distance(point(it, games[it]), tap) }
    ?.takeIf { distance(point(it, games[it]), tap) < radius }?.let { games[it] }

private fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)
