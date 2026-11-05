package app.lexico.ui.duplicate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

@Composable
internal fun ChartDialog(view: SeatView, close: () -> Unit) {
  AlertDialog(
    onDismissRequest = close,
    title = { Text("Evolución (% del Máster)") },
    text = { ChartAndStandings(view) },
    confirmButton = { TextButton(onClick = close) { Text("Cerrar") } },
  )
}

@Composable
private fun ChartAndStandings(view: SeatView) {
  Column {
    if (view.rounds.isEmpty()) Text("Aún no ha terminado ninguna ronda.")
    else if (view.watching) ColoredChart(percents(view))
    else PercentChart(percents(view), view.me)
    StandingsTable(view, maxHeight = 200)
  }
}

@Composable
private fun PercentChart(lines: Map<String, List<Float>>, me: String) {
  val mine = MaterialTheme.colorScheme.primary
  val others = MaterialTheme.colorScheme.outline
  val measurer = rememberTextMeasurer()
  val label = MaterialTheme.typography.labelSmall
  val floor = floorOf(lines)
  Canvas(Modifier.fillMaxWidth().height(190.dp).padding(top = 22.dp, bottom = 8.dp, start = 6.dp, end = 26.dp)) {
    val plot = Plot(floor, measurer, label)
    lines.filterKeys { it != me }.values.forEach { plot.line(this, it, others, 3f) }
    lines[me]?.let { plot.line(this, it, mine, 6f) }
  }
}

@Composable
private fun ColoredChart(lines: Map<String, List<Float>>) {
  val colors = lines.keys.withIndex().associate { (i, name) -> name to PLAYER_COLORS[i % PLAYER_COLORS.size] }
  val measurer = rememberTextMeasurer()
  val label = MaterialTheme.typography.labelSmall
  val floor = floorOf(lines)
  Canvas(Modifier.fillMaxWidth().height(190.dp).padding(top = 22.dp, bottom = 8.dp, start = 6.dp, end = 26.dp)) {
    val plot = Plot(floor, measurer, label)
    lines.forEach { (name, points) -> plot.line(this, points, colors.getValue(name), 4f) }
  }
  Legend(colors)
}

@Composable
private fun Legend(colors: Map<String, Color>) {
  FlowRow(Modifier.fillMaxWidth().padding(bottom = 8.dp), Arrangement.spacedBy(10.dp), Arrangement.spacedBy(4.dp)) {
    colors.forEach { (name, color) ->
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(name, style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}

private val PLAYER_COLORS = listOf(
  Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1),
  Color(0xFFFDD835), Color(0xFFD81B60), Color(0xFF3949AB), Color(0xFF7CB342), Color(0xFF6D4C41), Color(0xFF90A4AE),
)

private class Plot(val floor: Float, val measurer: TextMeasurer, val label: TextStyle) {
  fun line(scope: DrawScope, points: List<Float>, color: Color, width: Float) = with(scope) {
    val at = { i: Int -> point(points, i) }
    for (i in 1 until points.size) drawLine(color, at(i - 1), at(i), strokeWidth = width)
    val end = at(points.lastIndex)
    drawCircle(color, width * 1.6f, end)
    percentLabel(points.last(), end, color)
  }

  private fun DrawScope.point(points: List<Float>, i: Int): Offset {
    val step = if (points.size > 1) size.width / (points.size - 1) else size.width / 2
    val x = if (points.size > 1) i * step else step
    return Offset(x, size.height * (1 - (points[i] - floor) / (100f - floor).coerceAtLeast(1f)))
  }

  private fun DrawScope.percentLabel(value: Float, at: Offset, color: Color) {
    val text = measurer.measure("${value.toInt()} %", label.copy(color = color))
    drawText(text, topLeft = Offset(at.x - text.size.width / 2f, at.y - text.size.height - 8f))
  }
}

private fun floorOf(lines: Map<String, List<Float>>): Float =
  ((lines.values.flatten().minOrNull() ?: 0f).coerceAtMost(80f) / 10).toInt() * 10f

private fun percents(view: SeatView): Map<String, List<Float>> {
  val names = view.rounds.flatMap { r -> r.entries.map { it.name } }.distinct().filter { it != MASTER_NAME }
  return names.associateWith { name -> view.rounds.map { percentOf(it, name) } }
}

private fun percentOf(round: SeatRound, name: String): Float {
  val master = round.entries.find { it.name == MASTER_NAME }?.total ?: 0
  val mine = round.entries.find { it.name == name }?.total ?: 0
  return if (master > 0) mine * 100f / master else 100f
}
