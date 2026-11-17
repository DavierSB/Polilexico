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

private val DOT_STEP = 16.dp
private val GROUP_LABEL_HEIGHT = 16.dp

@Composable
internal fun DotStrip(games: List<ChartGame>, label: String, unit: String = "") {
  var selected by remember(games) { mutableStateOf<ChartGame?>(null) }
  val scale = remember(games, unit) { ChartScale(games.map { it.value }, cap = if (unit == "%") 100 else null) }
  val colors = chartColors()
  val measurer = rememberTextMeasurer()
  Column {
    Canvas(Modifier.fillMaxWidth().height(110.dp).pointerInput(games) { onTap(games, scale) { selected = it } }) {
      drawStrip(StripLayout(size.width, size.height, this, scale, games), scale, colors, measurer, unit)
    }
    Text(label, Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = colors.faint, textAlign = TextAlign.Center)
  }
  selected?.let { GameDialog(it) { selected = null } }
}

private suspend fun PointerInputScope.onTap(games: List<ChartGame>, scale: ChartScale, select: (ChartGame?) -> Unit) {
  detectTapGestures { tap ->
    val layout = StripLayout(size.width.toFloat(), size.height.toFloat(), this, scale, games)
    select(nearestGame(games, tap, TAP_RADIUS.toPx()) { i, _ -> layout.point(i) })
  }
}

private fun DrawScope.drawStrip(layout: StripLayout, scale: ChartScale, colors: ChartColors, measurer: TextMeasurer, unit: String) {
  drawLine(colors.grid, Offset(layout.left, layout.bottom), Offset(size.width, layout.bottom), strokeWidth = 1.dp.toPx())
  scale.ticks.forEach { drawTick(layout, it, colors, measurer, unit) }
  layout.dots.spots.forEach { drawDot(Offset(it.x, it.y), colors) }
  layout.dots.labels.forEach { drawGroupLabel(it, colors, measurer, unit) }
}

private fun DrawScope.drawTick(layout: StripLayout, value: Int, colors: ChartColors, measurer: TextMeasurer, unit: String) {
  val x = layout.x(value)
  drawLine(colors.grid, Offset(x, layout.bottom), Offset(x, layout.bottom + 4.dp.toPx()), strokeWidth = 1.dp.toPx())
  drawCenteredLabel(measurer, "$value$unit", colors.axisText, x, layout.bottom + 5.dp.toPx())
}

private fun DrawScope.drawGroupLabel(label: GroupLabel, colors: ChartColors, measurer: TextMeasurer, unit: String) {
  val text = if (label.low == label.high) "${label.low}$unit" else "${label.low}–${label.high}$unit"
  val height = measurer.measure(text, colors.axisText).size.height
  drawCenteredLabel(measurer, text, colors.axisText, label.x, label.bottom - height)
}

private class StripLayout(width: Float, height: Float, density: Density, private val scale: ChartScale, games: List<ChartGame>) {
  val left = with(density) { LEFT_MARGIN.toPx() } * 0.3f
  val bottom = height - with(density) { BOTTOM_MARGIN.toPx() }
  private val plotWidth = width - left
  val dots = layoutDots(games.map { x(it.value) }, games.map { it.value }, frame(width, density))

  fun x(value: Int): Float = left + plotWidth * scale.fraction(value)

  fun point(i: Int): Offset = dots.spots[i].let { Offset(it.x, it.y) }

  private fun frame(width: Float, density: Density): StripFrame =
    with(density) { StripFrame(left, width, bottom, DOT_STEP.toPx(), GROUP_LABEL_HEIGHT.toPx()) }
}
