package app.lexico.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Tus puntos (X) contra los del rival (Y), con la diagonal X = Y: por debajo de ella ganaste,
 * por encima perdiste. Mismo rango en los dos ejes, para que la diagonal sea de 45 grados.
 */
@Composable
internal fun ScoreScatter(games: List<ChartGame>) {
  var selected by remember(games) { mutableStateOf<ChartGame?>(null) }
  val scale = remember(games) { ChartScale(games.flatMap { listOf(it.myScore, it.opponentScore) }) }
  val colors = chartColors()
  val measurer = rememberTextMeasurer()
  Column {
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(games) { onTap(games, scale) { selected = it } }) {
      drawScatter(ScatterLayout(size.width, size.height, this, scale), games, scale, colors, measurer)
    }
    Text("X: tus puntos · Y: puntos del rival", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall,
      color = colors.faint, textAlign = TextAlign.Center)
  }
  selected?.let { GameDialog(it) { selected = null } }
}

private suspend fun PointerInputScope.onTap(games: List<ChartGame>, scale: ChartScale, select: (ChartGame?) -> Unit) {
  detectTapGestures { tap ->
    val layout = ScatterLayout(size.width.toFloat(), size.height.toFloat(), this, scale)
    select(nearestGame(games, tap, TAP_RADIUS.toPx()) { _, g -> layout.point(g) })
  }
}

private fun DrawScope.drawScatter(layout: ScatterLayout, games: List<ChartGame>, scale: ChartScale, colors: ChartColors, measurer: TextMeasurer) {
  scale.ticks.forEach { drawGridLine(layout, it, colors, measurer) }
  drawDiagonal(layout, scale, colors, measurer)
  games.forEach { drawDot(layout.point(it), colors) }
}

/** Las lineas de la rejilla en `value`, en los dos ejes, con sus valores. */
private fun DrawScope.drawGridLine(layout: ScatterLayout, value: Int, colors: ChartColors, measurer: TextMeasurer) {
  val stroke = 0.6.dp.toPx()
  drawLine(colors.grid, Offset(layout.x(value), layout.bottom), Offset(layout.x(value), layout.top), strokeWidth = stroke)
  drawLine(colors.grid, Offset(layout.left, layout.y(value)), Offset(layout.right, layout.y(value)), strokeWidth = stroke)
  drawCenteredLabel(measurer, "$value", colors.axisText, layout.x(value), layout.bottom + 4.dp.toPx())
  drawLabel(measurer, "$value", colors.axisText, Offset(0f, layout.y(value) - 7.dp.toPx()))
}

/** La diagonal X = Y, la frontera entre ganar y perder, con "Victoria" debajo y "Derrota" encima. */
private fun DrawScope.drawDiagonal(layout: ScatterLayout, scale: ChartScale, colors: ChartColors, measurer: TextMeasurer) {
  val from = Offset(layout.x(scale.min), layout.y(scale.min))
  val to = Offset(layout.x(scale.max), layout.y(scale.max))
  drawLine(colors.faint, from, to, strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
  drawLabel(measurer, "Victoria", colors.axisText, Offset(layout.right - 60.dp.toPx(), layout.bottom - 20.dp.toPx()))
  drawLabel(measurer, "Derrota", colors.axisText, Offset(layout.left + 6.dp.toPx(), layout.top + 4.dp.toPx()))
}

/** Donde cae cada partida: un cuadrado con los dos ejes en la misma escala. */
private class ScatterLayout(width: Float, height: Float, density: Density, private val scale: ChartScale) {
  val left = with(density) { LEFT_MARGIN.toPx() }
  val bottom = height - with(density) { BOTTOM_MARGIN.toPx() }
  private val side = minOf(width - left, bottom)
  val right = left + side
  val top = bottom - side

  fun x(value: Int): Float = left + side * scale.fraction(value)

  fun y(value: Int): Float = bottom - side * scale.fraction(value)

  fun point(game: ChartGame): Offset = Offset(x(game.myScore), y(game.opponentScore))
}
