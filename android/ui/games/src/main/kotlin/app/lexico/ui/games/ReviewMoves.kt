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
import androidx.compose.foundation.layout.Spacer
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
import app.lexico.ui.common.InfoDialog
import app.lexico.ui.common.LocalWordLookup
import app.lexico.ui.common.PLAY_LOOKUP_WIDTH
import app.lexico.ui.common.PlayLookup
import app.lexico.ui.common.InfoIcon
import app.lexico.ui.common.Mulish
import app.lexico.ui.common.RankedMove
import app.lexico.ui.common.ValueColumn
import app.lexico.ui.common.valueColumn
import app.lexico.ui.common.valueText

private val RankWidth = 36.sp
private val PointsWidth = 62.sp
private val ValueWidth = 82.sp
private val ColumnGap = 8.dp
private val CellSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 16.sp, stepSize = 0.5.sp)
private val HeaderSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = 11.sp, stepSize = 0.5.sp)

private const val NUMBERS_SHARE = 0.55f

private class Line(val caption: String, val rank: String, val move: RankedMove)

private class Widths(val rank: Dp, val points: Dp, val value: Dp)

@Composable
internal fun TurnMoves(turn: ReviewTurnView, classic: Boolean, selected: RankedMove?, select: (RankedMove) -> Unit) {
  val column = valueColumn(turn.candidates)
  val value = { m: RankedMove -> valueText(m).ifEmpty { "—" } }
  var explaining by remember { mutableStateOf(false) }
  BoxWithConstraints(Modifier.fillMaxWidth()) {
    val widths = widths(maxWidth, column != null)
    Column {
      TableHeader(widths, column) { explaining = true }
      HorizontalDivider()
      lines(turn, classic).forEach { line -> LineRow(line, widths, column?.let { value(line.move) }, line.move == selected) { select(line.move) } }
    }
  }
  if (explaining && column != null) InfoDialog(column.title, column.info) { explaining = false }
}

private fun lines(turn: ReviewTurnView, classic: Boolean): List<Line> =
  listOfNotNull(turn.candidates.firstOrNull()?.takeIf { classic }?.let { Line(noted("Mejor jugada", it), "#1", it) }) +
    turn.marks.map { Line(noted(markLabel(it.who), it.move), it.rank?.let { r -> "#${r + 1}" } ?: "—", it.move) }

private fun noted(caption: String, move: RankedMove): String = if (move.note.isEmpty()) caption else "$caption · ${move.note}"

private fun markLabel(who: String): String = when (who) {
  "Tú" -> "Tu jugada"
  "Máster" -> "Jugada del Máster"
  else -> "Jugada de $who"
}

@Composable
private fun widths(total: Dp, withValue: Boolean): Widths {
  val (rank, points, value) = with(LocalDensity.current) { listOf(RankWidth.toDp(), PointsWidth.toDp(), ValueWidth.toDp()) }
  val wanted = rank + points + if (withValue) value else 0.dp
  val scale = minOf(1f, total * NUMBERS_SHARE / wanted)
  return Widths(rank * scale, points * scale, value * scale)
}

@Composable
private fun TableHeader(widths: Widths, column: ValueColumn?, explain: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    HeaderCell("Jugada", Modifier.weight(1f))
    HeaderCell("#", Modifier.column(widths.rank), TextAlign.End)
    HeaderCell("Puntos", Modifier.column(widths.points), TextAlign.End)
    if (column != null) ValueHeader(column, widths.value, explain)
    if (LocalWordLookup.current != null) Spacer(Modifier.width(PLAY_LOOKUP_WIDTH))
  }
}

@Composable
private fun ValueHeader(column: ValueColumn, width: Dp, explain: () -> Unit) {
  Row(Modifier.column(width).clickable(onClick = explain), Arrangement.End, Alignment.CenterVertically) {
    HeaderCell(column.title, Modifier.weight(1f, fill = false), TextAlign.End)
    InfoIcon(Modifier.padding(start = 3.dp))
  }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
  Text(text.uppercase(), modifier, style = headerStyle(), textAlign = align, maxLines = 1, softWrap = false, autoSize = HeaderSize)
}

@Composable
private fun LineRow(line: Line, widths: Widths, value: String?, active: Boolean, select: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().padding(vertical = 1.dp).background(rowFill(active), RoundedCornerShape(10.dp)).clickable(onClick = select)
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MoveCell(line)
    Cell(line.rank, widths.rank, cellStyle(muted = true))
    Cell("${line.move.points}", widths.points, cellStyle())
    if (value != null) Cell(value, widths.value, cellStyle(muted = true))
    if (LocalWordLookup.current != null) PlayLookup(line.move.text)
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
internal fun headerStyle(): TextStyle = TextStyle(
  fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary,
)

@Composable
internal fun captionStyle(): TextStyle = TextStyle(
  fontFamily = Mulish, fontWeight = FontWeight.Bold, fontSize = 9.sp, lineHeight = 11.sp, letterSpacing = 0.8.sp,
  color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
internal fun cellStyle(muted: Boolean = false): TextStyle = TextStyle(
  fontFamily = Mulish, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
  color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
)

@Composable
internal fun rowFill(active: Boolean): Color = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
