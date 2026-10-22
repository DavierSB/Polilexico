package app.lexico.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.board.ScrabbleBoard
import app.lexico.ui.common.BarIconButton
import app.lexico.ui.common.LexicoIcons
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val STEP_MS = 1_400L
private val FRAME = 12.dp

@Composable
internal fun PlanDialog(review: ReviewView, position: ExploredPosition, line: LineView, style: BoardStyle, close: () -> Unit) {
  val tallies = remember(position, line) { planTallies(position, line) }
  var step by remember(line.move.text) { mutableIntStateOf(0) }
  var playing by remember(line.move.text) { mutableStateOf(true) }
  val shown = step.coerceAtMost(line.steps.lastIndex)
  LaunchedEffect(playing, shown) { if (playing) delay(STEP_MS).also { if (shown < line.steps.lastIndex) step = shown + 1 else playing = false } }
  val go = { i: Int -> step = i.coerceIn(0, line.steps.lastIndex); playing = false }
  Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 8.dp), shape = AlertDialogDefaults.shape) {
      BoxWithConstraints {
        val height = maxHeight - FRAME * 2
        Column(Modifier.verticalScroll(rememberScrollState()).padding(FRAME)) {
          BoardFit(
            height,
            top = { Stacked { PlanTitle(line, close); PlanBar(review, position, tallies[shown], 1 - position.me, line.steps[shown], style) } },
            board = { PlanBoard(line.steps[shown], style) },
            bottom = {
              Stacked {
                PlanBar(review, position, tallies[shown], position.me, line.steps[shown], style)
                PlanSteps(line, shown, playing, go) { playing = !playing; if (playing && shown == line.steps.lastIndex) step = 0 }
              }
            },
          )
        }
      }
    }
  }
}

@Composable
private fun PlanSteps(line: LineView, shown: Int, playing: Boolean, go: (Int) -> Unit, toggle: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    PlanControls(shown, line.steps.size, playing, go, toggle)
    line.steps.forEachIndexed { i, s -> PlanStep(i, s, active = i == shown) { go(i) } }
    PlanEnding(line)
  }
}

@Composable
private fun PlanTitle(line: LineView, close: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    BarIconButton(LexicoIcons.Back, "Volver", close)
    Text("Plan: ${line.move.text}", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun PlanBar(review: ReviewView, position: ExploredPosition, tally: PlanTally, player: Int, step: StepView, style: BoardStyle) {
  val name = position.players[player]
  ScoreBar(review, name, tally.scores[player], tally.racks[player], onTurn = step.who == name, style)
}

@Composable
private fun PlanBoard(step: StepView, style: BoardStyle) {
  ScrabbleBoard(step.board, Modifier, style, pending = step.move.placement?.placed.orEmpty())
}

@Composable
private fun PlanControls(step: Int, count: Int, playing: Boolean, go: (Int) -> Unit, toggle: () -> Unit) {
  Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
    OutlinedButton(onClick = { go(step - 1) }, enabled = step > 0) { Text("‹") }
    TextButton(onClick = toggle, Modifier.weight(1f)) { Text(if (playing) "Pausa" else "Reproducir") }
    OutlinedButton(onClick = { go(step + 1) }, enabled = step < count - 1) { Text("›") }
  }
}

@Composable
private fun PlanStep(index: Int, step: StepView, active: Boolean, select: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().background(stepFill(active), RoundedCornerShape(10.dp)).clickable(onClick = select).padding(horizontal = 8.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("${index + 1}", Modifier.width(STEP_NUMBER_WIDTH), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(step.who, Modifier.width(WHO_WIDTH), style = captionStyle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(step.move.text, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text("${step.move.points}", Modifier.width(NUMBER_WIDTH), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun PlanEnding(line: LineView) {
  if (line.pending) return Text("Calculando el resto del plan…", style = MaterialTheme.typography.bodySmall)
  HorizontalDivider(Modifier.padding(top = 4.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
  line.ending.forEach { EndingLine(it) }
  Outcome(winner(line), abs(line.spread))
}

@Composable
private fun stepFill(active: Boolean): Color =
  if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

private fun winner(line: LineView): String? = when {
  line.spread > 0 -> line.mover
  line.spread < 0 -> line.ending.map { it.who }.firstOrNull { it != line.mover }
  else -> null
}
