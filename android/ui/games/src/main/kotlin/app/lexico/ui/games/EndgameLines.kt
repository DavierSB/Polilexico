package app.lexico.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lexico.ui.common.FINAL_COLUMN
import app.lexico.ui.common.InfoDialog
import app.lexico.ui.common.InfoIcon
import app.lexico.ui.common.valueText

private val RANK_WIDTH = 36.dp
private val POINTS_WIDTH = 64.dp
private val FINAL_WIDTH = 60.dp
private val MOVE_TEXT = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 16.sp, stepSize = 0.5.sp)

private const val PENDING = "…"

internal class Ranked(val caption: String, val rank: String, val line: LineView)

internal class AnalysisState(val lines: LiveLines, val analyzing: Boolean, val failed: Boolean)

@Composable
internal fun Summary(turn: ReviewTurnView, lines: LiveLines, x: Exploration, plan: (LineView) -> Unit) {
  LinesTable(summary(turn, lines.lines), AnalysisState(lines, x.analyzing, x.failed), plan)
}

@Composable
internal fun LinesDialog(lines: LiveLines, analyzing: Boolean, close: () -> Unit, plan: (LineView) -> Unit) {
  Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 8.dp), shape = AlertDialogDefaults.shape) {
      Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)) {
        Text("Mejores jugadas", Modifier.padding(start = 4.dp, bottom = 8.dp), style = MaterialTheme.typography.headlineSmall)
        LinesTable(listed(lines.lines), AnalysisState(lines, analyzing, false), plan, Modifier.weight(1f), Modifier.verticalScroll(rememberScrollState()))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = close) { Text("Cerrar") } }
      }
    }
  }
}

@Composable
internal fun LinesTable(
  rows: List<Ranked>, state: AnalysisState, plan: (LineView) -> Unit, modifier: Modifier = Modifier, body: Modifier = Modifier,
) {
  var explaining by remember { mutableStateOf(false) }
  Column(modifier) {
    LinesHeader(state) { explaining = true }
    HorizontalDivider()
    Column(Modifier.fillMaxWidth().then(body)) { rows.forEach { r -> LineRow(r) { plan(r.line) } } }
  }
  if (explaining) InfoDialog(FINAL_COLUMN.title, FINAL_COLUMN.info) { explaining = false }
}

private fun summary(turn: ReviewTurnView, lines: List<LineView>): List<Ranked> =
  listOfNotNull(lines.firstOrNull()?.let { Ranked("Mejor jugada", rankOf(lines, 0), it) }) +
    turn.marks.mapNotNull { m -> lines.indexOfFirst { it.move.text == m.move.text }.takeIf { it >= 0 }?.let { i -> Ranked(markCaption(m.who), rankOf(lines, i), lines[i]) } }

private fun listed(lines: List<LineView>): List<Ranked> =
  lines.mapIndexed { i, l -> Ranked(l.move.playedBy.takeIf { it.isNotEmpty() }?.let(::markCaption).orEmpty(), rankOf(lines, i), l) }

private fun rankOf(lines: List<LineView>, i: Int): String = if (lines[i].pending) "—" else "#${i + 1}"

private fun markCaption(who: String): String = if (who == ME) "Tu jugada" else "Jugada de $who"

private fun statusText(state: AnalysisState): String = when {
  state.failed -> "sin análisis"
  state.lines.depth == 0 -> ""
  state.lines.exact && !state.analyzing -> "exacto"
  else -> "prof. ${state.lines.depth}"
}

@Composable
private fun LinesHeader(state: AnalysisState, explain: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Row(Modifier.weight(1f), Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
      HeaderText("Jugada")
      if (state.analyzing) CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
      Text(statusText(state).uppercase(), style = captionStyle(), maxLines = 1)
    }
    HeaderText("#", Modifier.width(RANK_WIDTH), TextAlign.End)
    HeaderText("Puntos", Modifier.width(POINTS_WIDTH), TextAlign.End)
    Row(Modifier.width(FINAL_WIDTH).clickable(onClick = explain), Arrangement.End, Alignment.CenterVertically) {
      HeaderText(FINAL_COLUMN.title, align = TextAlign.End)
      InfoIcon(Modifier.padding(start = 3.dp))
    }
  }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
  Text(text.uppercase(), modifier, style = headerStyle(), textAlign = align, maxLines = 1)
}

@Composable
private fun LineRow(row: Ranked, select: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().padding(vertical = 1.dp).background(rowFill(false), RoundedCornerShape(10.dp)).clickable(onClick = select)
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MoveCell(row)
    Cell(row.rank, RANK_WIDTH, cellStyle(muted = true))
    Cell("${row.line.move.points}", POINTS_WIDTH, cellStyle())
    Cell(if (row.line.pending) PENDING else valueText(row.line.move), FINAL_WIDTH, cellStyle(muted = true))
  }
}

@Composable
private fun RowScope.MoveCell(row: Ranked) {
  Column(Modifier.weight(1f)) {
    if (row.caption.isNotEmpty()) Text(row.caption.uppercase(), style = captionStyle(), maxLines = 1)
    Text(row.line.move.text, maxLines = 1, softWrap = false, style = cellStyle().copy(letterSpacing = 0.6.sp), autoSize = MOVE_TEXT)
  }
}

@Composable
private fun Cell(text: String, width: Dp, style: TextStyle) {
  Text(text, Modifier.width(width), style = style, textAlign = TextAlign.End, maxLines = 1, softWrap = false)
}
