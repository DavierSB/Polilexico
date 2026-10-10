package app.lexico.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

private val PAD = 6.dp
private const val NAME_SHARE = 0.6f

private class SpreadColors(
  val top: Color, val bottom: Color, val line: Color, val zero: Color, val grid: Color, val mark: Color,
  val names: TextStyle, val result: TextStyle,
)

@Composable
fun SpreadChart(
  spread: Spread, modifier: Modifier = Modifier, height: Dp = 170.dp, selected: Int? = null,
  pick: ((Int) -> Unit)? = null, tapped: () -> Unit = {},
) {
  val colors = spreadColors()
  val measurer = rememberTextMeasurer()
  Canvas(modifier.fillMaxWidth().height(height).picking(spread, pick, tapped)) {
    drawAreas(spread, colors)
    drawGrid(spread, colors)
    selected?.let { drawSelected(spread, it, colors.mark) }
    drawLine(spread, colors.line)
    drawNames(measurer, spread, colors.names)
    drawResult(measurer, spread, colors.result)
  }
}

@Composable
private fun spreadColors(): SpreadColors = with(MaterialTheme.colorScheme) {
  SpreadColors(
    top = primary.copy(alpha = 0.28f), bottom = tertiary.copy(alpha = 0.28f), line = onSurface, zero = outline,
    grid = outlineVariant, mark = primary,
    names = TextStyle(fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = onSurfaceVariant),
    result = TextStyle(fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = onSurface),
  )
}

private fun Modifier.picking(spread: Spread, pick: ((Int) -> Unit)?, tapped: () -> Unit): Modifier {
  if (pick == null || spread.lastIndex < 1) return this
  val at = { x: Float, width: Int -> pick((x / width * spread.lastIndex).roundToInt().coerceIn(0, spread.lastIndex)) }
  return pointerInput(spread) { detectTapGestures { at(it.x, size.width); tapped() } }
    .pointerInput(spread) { detectHorizontalDragGestures { change, _ -> at(change.position.x, size.width) } }
}

private fun DrawScope.x(spread: Spread, i: Int): Float = if (spread.lastIndex < 1) 0f else size.width * i / spread.lastIndex

private fun DrawScope.y(spread: Spread, value: Int): Float = center.y - value.toFloat() / spread.limit * (center.y - PAD.toPx())

private fun DrawScope.linePath(spread: Spread): Path = Path().apply {
  spread.points.forEachIndexed { i, v -> if (i == 0) moveTo(x(spread, i), y(spread, v)) else lineTo(x(spread, i), y(spread, v)) }
}

private fun DrawScope.areaPath(spread: Spread): Path = linePath(spread).apply {
  lineTo(x(spread, spread.lastIndex), center.y)
  lineTo(0f, center.y)
  close()
}

private fun DrawScope.drawAreas(spread: Spread, colors: SpreadColors) {
  val area = areaPath(spread)
  clipRect(bottom = center.y) { drawPath(area, colors.top) }
  clipRect(top = center.y) { drawPath(area, colors.bottom) }
}

private fun DrawScope.drawGrid(spread: Spread, colors: SpreadColors) {
  val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
  spread.gridLines.forEach { v -> listOf(v, -v).forEach { horizontal(y(spread, it), colors.grid, dash) } }
  horizontal(center.y, colors.zero)
}

private fun DrawScope.horizontal(y: Float, color: Color, effect: PathEffect? = null) {
  drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = effect)
}

private fun DrawScope.drawLine(spread: Spread, color: Color) {
  drawPath(linePath(spread), color, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.drawSelected(spread: Spread, i: Int, color: Color) {
  val at = Offset(x(spread, i), y(spread, spread.points[i.coerceIn(0, spread.lastIndex)]))
  drawLine(color.copy(alpha = 0.5f), Offset(at.x, 0f), Offset(at.x, size.height), 1.dp.toPx())
  drawCircle(color, 5.dp.toPx(), at)
}

private fun DrawScope.drawNames(measurer: TextMeasurer, spread: Spread, style: TextStyle) {
  val pad = PAD.toPx()
  drawText(name(measurer, spread.top, style), topLeft = Offset(pad, pad))
  val bottom = name(measurer, spread.bottom, style)
  drawText(bottom, topLeft = Offset(pad, size.height - pad - bottom.size.height))
}

private fun DrawScope.name(measurer: TextMeasurer, text: String, style: TextStyle): TextLayoutResult =
  measurer.measure(text, style, TextOverflow.Ellipsis, maxLines = 1, constraints = Constraints(maxWidth = (size.width * NAME_SHARE).toInt()))

private fun DrawScope.drawResult(measurer: TextMeasurer, spread: Spread, style: TextStyle) {
  val last = spread.points.last()
  val text = measurer.measure(if (last == 0) "0" else "+${abs(last)}", style)
  val end = y(spread, last)
  val top = if (last >= 0) end - text.size.height - 4.dp.toPx() else end + 4.dp.toPx()
  val left = size.width - text.size.width - PAD.toPx()
  drawText(text, topLeft = Offset(left, top.coerceIn(0f, size.height - text.size.height)))
}
