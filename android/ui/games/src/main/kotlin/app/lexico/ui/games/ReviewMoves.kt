package app.lexico.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.EQUITY_INFO
import app.lexico.ui.common.InfoDialog
import app.lexico.ui.common.InfoIcon
import app.lexico.ui.common.Mulish
import app.lexico.ui.common.RankedMove

private val RankWidth = 36.sp
private val PointsWidth = 62.sp
private val EquityWidth = 82.sp
private val ColumnGap = 8.dp
private val CellSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 16.sp, stepSize = 0.5.sp)
private val HeaderSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = 11.sp, stepSize = 0.5.sp)

private const val NUMBERS_SHARE = 0.55f

private class Line(val caption: String, val rank: String, val move: RankedMove)

private class Widths(val rank: Dp, val points: Dp, val equity: Dp)

@Composable
internal fun TurnMoves(turn: ReviewTurnView, classic: Boolean, selected: RankedMove?, select: (RankedMove) -> Unit) {
  val equity = turn.candidates.any { it.equity != null }
  var explaining by remember { mutableStateOf(false) }
  BoxWithConstraints(Modifier.fillMaxWidth()) {
    val widths = widths(maxWidth, equity)
    Column {
      TableHeader(widths, equity) { explaining = true }
      HorizontalDivider()
      lines(turn, classic).forEach { line -> LineRow(line, widths, equity, line.move == selected) { select(line.move) } }
    }
  }
  if (explaining) InfoDialog("Equity", EQUITY_INFO) { explaining = false }
}

private fun lines(turn: ReviewTurnView, classic: Boolean): List<Line> =
  listOfNotNull(turn.candidates.firstOrNull()?.takeIf { classic }?.let { Line("Mejor jugada", "#1", it) }) +
    turn.marks.map { Line(markLabel(it.who), it.rank?.let { r -> "#${r + 1}" } ?: "—", it.move) }

private fun markLabel(who: String): String = when (who) {
  "Tú" -> "Tu jugada"
  "Máster" -> "Jugada del Máster"
  else -> "Jugada de $who"
}

@Composable
private fun widths(total: Dp, equity: Boolean): Widths {
  val (rank, points, value) = with(LocalDensity.current) { listOf(RankWidth.toDp(), PointsWidth.toDp(), EquityWidth.toDp()) }
  val wanted = rank + points + if (equity) value else 0.dp
  val scale = minOf(1f, total * NUMBERS_SHARE / wanted)
  return Widths(rank * scale, points * scale, value * scale)
}

@Composable
private fun TableHeader(widths: Widths, equity: Boolean, explain: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    HeaderCell("Jugada", Modifier.weight(1f))
    HeaderCell("#", Modifier.column(widths.rank), TextAlign.End)
    HeaderCell("Puntos", Modifier.column(widths.points), TextAlign.End)
    if (equity) EquityHeader(widths.equity, explain)
  }
}

@Composable
private fun EquityHeader(width: Dp, explain: () -> Unit) {
  Row(Modifier.column(width).clickable(onClick = explain), Arrangement.End, Alignment.CenterVertically) {
    HeaderCell("Equity", Modifier.weight(1f, fill = false), TextAlign.End)
    InfoIcon(Modifier.padding(start = 3.dp))
  }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
  Text(text.uppercase(), modifier, style = headerStyle(), textAlign = align, maxLines = 1, softWrap = false, autoSize = HeaderSize)
}

@Composable
private fun LineRow(line: Line, widths: Widths, equity: Boolean, active: Boolean, select: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().padding(vertical = 1.dp).background(rowFill(active), RoundedCornerShape(10.dp)).clickable(onClick = select)
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MoveCell(line)
    Cell(line.rank, widths.rank, cellStyle(muted = true))
    Cell("${line.move.points}", widths.points, cellStyle())
    if (equity) Cell(line.move.equity?.let { "%.1f".format(it) } ?: "—", widths.equity, cellStyle(muted = true))
  }
}

@Composable
private fun RowScope.MoveCell(line: Line) {
  Column(Modifier.weight(1f)) {
    Text(line.caption.uppercase(), style = captionStyle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(
      line.move.text, maxLines = 1, softWrap = false, style = cellStyle().copy(letterSpacing = 0.6.sp),
      autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 16.sp, stepSize = 0.5.sp),
    )
  }
}

@Composable
private fun Cell(text: String, width: Dp, style: TextStyle) {
  Text(text, Modifier.column(width), style = style, textAlign = TextAlign.End, maxLines = 1, softWrap = false, autoSize = CellSize)
}

private fun Modifier.column(width: Dp): Modifier = padding(start = ColumnGap).width(width)

@Composable
private fun headerStyle(): TextStyle = TextStyle(
  fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary,
)

@Composable
private fun captionStyle(): TextStyle = TextStyle(
  fontFamily = Mulish, fontWeight = FontWeight.Bold, fontSize = 9.sp, lineHeight = 11.sp, letterSpacing = 0.8.sp,
  color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun cellStyle(muted: Boolean = false): TextStyle = TextStyle(
  fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
  color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun rowFill(active: Boolean): Color = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
