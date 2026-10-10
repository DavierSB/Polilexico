package app.lexico.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.floor

enum class EfficiencyView { RUNNING, PER_TURN }

private val TOP = 10.dp
private val GUTTER = 24.dp
private const val BAR_SHARE = 0.6f
private const val FULL = 100f
private val STEPS = listOf(5f, 10f, 25f)
private const val MAX_LINES = 4

private class EfficiencyColors(
  val hit: Color, val miss: Color, val empty: Color, val line: Color, val grid: Color, val band: Color,
  val axis: TextStyle, val result: TextStyle,
)

private class Scale(val floor: Float) {
  val lines: List<Float> get() {
    val step = STEPS.firstOrNull { (FULL - floor) / it <= MAX_LINES } ?: 25f
    return generateSequence(FULL) { it - step }.takeWhile { it >= floor }.toList()
  }
}

@Composable
fun EfficiencyChart(
  efficiency: Efficiency, view: EfficiencyView, modifier: Modifier = Modifier, height: Dp = 170.dp, selected: Int? = null,
  pick: ((Int) -> Unit)? = null, tapped: () -> Unit = {},
) {
  val colors = efficiencyColors()
  val measurer = rememberTextMeasurer()
  val gutter = with(LocalDensity.current) { GUTTER.toPx() }
  val scale = if (view == EfficiencyView.RUNNING) Scale(floorOf(efficiency.running)) else Scale(0f)
  Canvas(modifier.fillMaxWidth().height(height).picking(efficiency, gutter, pick, tapped)) {
    selected?.let { drawBand(efficiency, it, colors.band) }
    drawGrid(measurer, scale, colors)
    if (view == EfficiencyView.PER_TURN) drawBars(efficiency, scale, colors) else drawRunning(measurer, efficiency, scale, colors)
  }
}

private fun floorOf(running: List<Float>): Float = (floor(((running.minOrNull() ?: 0f) - 5f) / 10f) * 10f).coerceIn(0f, 90f)

@Composable
private fun efficiencyColors(): EfficiencyColors = with(MaterialTheme.colorScheme) {
  EfficiencyColors(
    hit = primary, miss = onSurfaceVariant.copy(alpha = 0.35f), empty = outline, line = onSurface, grid = outlineVariant,
    band = primary.copy(alpha = 0.12f),
    axis = TextStyle(fontFamily = Mulish, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, color = onSurfaceVariant),
    result = TextStyle(fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = onSurface),
  )
}

private fun Modifier.picking(efficiency: Efficiency, gutter: Float, pick: ((Int) -> Unit)?, tapped: () -> Unit): Modifier {
  if (pick == null || efficiency.turns.isEmpty()) return this
  val at = { x: Float, width: Int ->
    pick(((x - gutter) / (width - gutter) * efficiency.turns.size).toInt().coerceIn(0, efficiency.lastIndex))
  }
  return pointerInput(efficiency) { detectTapGestures { at(it.x, size.width); tapped() } }
    .pointerInput(efficiency) { detectHorizontalDragGestures { change, _ -> at(change.position.x, size.width) } }
}

private fun DrawScope.slot(efficiency: Efficiency): Float = (size.width - GUTTER.toPx()) / efficiency.turns.size.coerceAtLeast(1)

private fun DrawScope.center(efficiency: Efficiency, i: Int): Float = GUTTER.toPx() + slot(efficiency) * (i + 0.5f)

private fun DrawScope.y(scale: Scale, percent: Float): Float =
  size.height - (percent - scale.floor) / (FULL - scale.floor) * (size.height - TOP.toPx())

private fun DrawScope.drawGrid(measurer: TextMeasurer, scale: Scale, colors: EfficiencyColors) {
  val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
  scale.lines.forEach { v ->
    drawLine(colors.grid, Offset(GUTTER.toPx(), y(scale, v)), Offset(size.width, y(scale, v)), 1.dp.toPx(), pathEffect = dash)
    val label = measurer.measure(v.toInt().toString(), colors.axis)
    drawText(label, topLeft = Offset(0f, (y(scale, v) - label.size.height / 2).coerceIn(0f, size.height - label.size.height)))
  }
  drawLine(colors.grid, Offset(GUTTER.toPx(), size.height), Offset(size.width, size.height), 1.dp.toPx())
}

private fun DrawScope.drawBand(efficiency: Efficiency, i: Int, color: Color) {
  drawRect(color, Offset(center(efficiency, i) - slot(efficiency) / 2, 0f), Size(slot(efficiency), size.height))
}

private fun DrawScope.drawBars(efficiency: Efficiency, scale: Scale, colors: EfficiencyColors) {
  val width = slot(efficiency) * BAR_SHARE
  efficiency.turns.forEachIndexed { i, percent ->
    val left = center(efficiency, i) - width / 2
    if (percent == null) return@forEachIndexed drawEmpty(left, width, colors.empty)
    val top = y(scale, percent)
    val color = if (efficiency.isHit(i)) colors.hit else colors.miss
    drawRoundRect(color, Offset(left, top), Size(width, size.height - top), CornerRadius(minOf(width / 2, 2.dp.toPx())))
  }
}

private fun DrawScope.drawEmpty(left: Float, width: Float, color: Color) {
  drawLine(color, Offset(left, size.height - 1.dp.toPx()), Offset(left + width, size.height - 1.dp.toPx()), 2.dp.toPx())
}

private fun DrawScope.drawRunning(measurer: TextMeasurer, efficiency: Efficiency, scale: Scale, colors: EfficiencyColors) {
  val path = Path()
  efficiency.running.forEachIndexed { i, v ->
    if (i == 0) path.moveTo(center(efficiency, i), y(scale, v)) else path.lineTo(center(efficiency, i), y(scale, v))
  }
  drawPath(path, colors.line, style = Stroke(2.dp.toPx()))
  val last = efficiency.running.lastOrNull() ?: return
  val end = Offset(center(efficiency, efficiency.lastIndex), y(scale, last))
  drawCircle(colors.line, 4.dp.toPx(), end)
  drawResult(measurer, last, end, colors.result)
}

private fun DrawScope.drawResult(measurer: TextMeasurer, value: Float, end: Offset, style: TextStyle) {
  val text = measurer.measure(String.format(Locale.forLanguageTag("es"), "%.1f %%", value), style)
  val gap = 6.dp.toPx()
  val left = (end.x - text.size.width - gap).coerceAtLeast(GUTTER.toPx())
  val above = end.y - text.size.height - gap
  val top = if (above >= 0f) above else end.y + gap
  drawText(text, topLeft = Offset(left, top.coerceIn(0f, size.height - text.size.height)))
}
