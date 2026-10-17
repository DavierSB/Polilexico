package app.lexico.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.ScrabbleBoard
import kotlin.math.abs

private const val ME = "Tú"

private val NUMBER_WIDTH = 24.dp
private val WHO_WIDTH = 72.dp
private val POINTS_WIDTH = 40.dp

@Composable
internal fun EndgameLineDialog(line: LineView, style: BoardStyle, close: () -> Unit) {
  var step by remember(line) { mutableIntStateOf(0) }
  val go = { i: Int -> step = i.coerceIn(0, line.steps.lastIndex) }
  Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 8.dp), shape = AlertDialogDefaults.shape) {
      Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(line.move.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        StepBoard(line.steps[step], style)
        StepNavigator(step, line.steps.size, go)
        line.steps.forEachIndexed { i, s -> StepRow(i, s, active = i == step) { go(i) } }
        HorizontalDivider(Modifier.padding(top = 4.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        line.ending.forEach { EndingLine(it) }
        Text(outcome(line, MaterialTheme.colorScheme.primary), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = close) { Text("Cerrar") } }
      }
    }
  }
}

@Composable
private fun StepBoard(step: StepView, style: BoardStyle) {
  ScrabbleBoard(step.board, Modifier.fillMaxWidth(), style, pending = step.move.placement?.placed.orEmpty())
}

@Composable
private fun StepNavigator(step: Int, count: Int, go: (Int) -> Unit) {
  Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
    OutlinedButton(onClick = { go(step - 1) }, enabled = step > 0) { Text("‹") }
    Text("Paso ${step + 1} de $count", Modifier.weight(1f), textAlign = TextAlign.Center)
    OutlinedButton(onClick = { go(step + 1) }, enabled = step < count - 1) { Text("›") }
  }
}

@Composable
private fun StepRow(index: Int, step: StepView, active: Boolean, select: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().background(rowFill(active), RoundedCornerShape(10.dp)).clickable(onClick = select).padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("${index + 1}", Modifier.width(NUMBER_WIDTH), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(step.who, Modifier.width(WHO_WIDTH), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(step.move.text, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text("${step.move.points}", Modifier.width(POINTS_WIDTH), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun EndingLine(row: EndingRow) {
  Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(row.who, Modifier.width(NUMBER_WIDTH + WHO_WIDTH), maxLines = 1, overflow = TextOverflow.Ellipsis,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(endingText(row), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text("%+d".format(row.delta), Modifier.width(POINTS_WIDTH), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun rowFill(active: Boolean): Color = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent

private fun endingText(row: EndingRow): String =
  if (row.left.isEmpty()) "Cierra" else "Se queda con " + row.left.joinToString(" ")

private fun outcome(line: LineView, accent: Color): AnnotatedString {
  val winner = line.ending.getOrNull(if (line.spread > 0) 0 else 1)?.who
  if (line.spread == 0 || winner == null) return AnnotatedString("Empate")
  return buildAnnotatedString {
    append(if (winner == ME) "Ganas tú por " else "Gana $winner por ")
    withStyle(SpanStyle(color = accent)) { append("${abs(line.spread)}") }
  }
}
