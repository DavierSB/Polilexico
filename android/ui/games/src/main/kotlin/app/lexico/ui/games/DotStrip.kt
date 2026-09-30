package app.lexico.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@Composable
internal fun DotStrip(games: List<ChartGame>, label: String, unit: String = "") {
  var selected by remember(games) { mutableStateOf<ChartGame?>(null) }
  val scale = remember(games, unit) { ChartScale(games.map { it.value }, cap = if (unit == "%") 100 else null) }
  val colors = chartColors()
  val measurer = rememberTextMeasurer()
  Column {
    Canvas(Modifier.fillMaxWidth().height(110.dp).pointerInput(games) { onTap(games, scale) { selected = it } }) {
      drawStrip(StripLayout(size.width, size.height, this, scale), games, scale, colors, measurer, unit)
    }
    Text(label, Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = colors.faint, textAlign = TextAlign.Center)
  }
  selected?.let { GameDialog(it) { selected = null } }
}

private suspend fun PointerInputScope.onTap(games: List<ChartGame>, scale: ChartScale, select: (ChartGame?) -> Unit) {
  detectTapGestures { tap ->
    val layout = StripLayout(size.width.toFloat(), size.height.toFloat(), this, scale)
    select(nearestGame(games, tap, TAP_RADIUS.toPx()) { i, g -> layout.point(i, g.value) })
  }
}

private fun DrawScope.drawStrip(layout: StripLayout, games: List<ChartGame>, scale: ChartScale, colors: ChartColors, measurer: TextMeasurer, unit: String) {
  drawLine(colors.grid, Offset(layout.left, layout.bottom), Offset(size.width, layout.bottom), strokeWidth = 1.dp.toPx())
  scale.ticks.forEach { drawTick(layout, it, colors, measurer, unit) }
  games.forEachIndexed { i, g -> drawDot(layout.point(i, g.value), colors) }
}

private fun DrawScope.drawTick(layout: StripLayout, value: Int, colors: ChartColors, measurer: TextMeasurer, unit: String) {
  val x = layout.x(value)
  drawLine(colors.grid, Offset(x, layout.bottom), Offset(x, layout.bottom + 4.dp.toPx()), strokeWidth = 1.dp.toPx())
  drawCenteredLabel(measurer, "$value$unit", colors.axisText, x, layout.bottom + 5.dp.toPx())
}

private class StripLayout(width: Float, height: Float, density: Density, private val scale: ChartScale) {
  val left = with(density) { LEFT_MARGIN.toPx() } * 0.3f
  val bottom = height - with(density) { BOTTOM_MARGIN.toPx() }
  private val plotWidth = width - left
  private val middle = bottom / 2

  fun x(value: Int): Float = left + plotWidth * scale.fraction(value)

  fun point(i: Int, value: Int): Offset = Offset(x(value), middle + jitter(i) * middle * 0.6f)

  private fun jitter(i: Int): Float = ((i * 37) % 7 - 3) / 3f
}
