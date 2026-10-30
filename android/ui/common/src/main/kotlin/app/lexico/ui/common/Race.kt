package app.lexico.ui.common

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val TICK_MS = 200L

private val HEART = Color(0xFFE5484D)

const val RACE_LIVES = 5

const val RACE_INFO = "Gana el primero en llegar a la meta. Cada jugador tiene $RACE_LIVES vidas; si se acaba el tiempo o nadie tiene vidas, gana quien más lleve."

data class RaceRunner(val name: String, val score: Int, val done: Boolean, val present: Boolean, val lives: Int = 0, val ready: Boolean = false)

data class RaceBoard(val me: String, val runners: List<RaceRunner>, val goal: Int, val endsAt: Long?, val unit: String, val units: String) {
  fun count(n: Int): String = "$n ${if (n == 1) unit else units}"
}

@Composable
fun rememberRaceLeft(endsAt: Long?): Long? {
  endsAt ?: return null
  var left by remember(endsAt) { mutableLongStateOf(leftUntil(endsAt)) }
  LaunchedEffect(endsAt) {
    while (left > 0) {
      delay(TICK_MS)
      left = leftUntil(endsAt)
    }
  }
  return left
}

private fun leftUntil(endsAt: Long): Long = maxOf(0, endsAt - SystemClock.elapsedRealtime())

@Composable
fun RaceBar(board: RaceBoard) {
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    board.runners.forEachIndexed { i, r -> RunnerChip(i + 1, r, r.name == board.me) }
  }
}

@Composable
private fun RunnerChip(place: Int, runner: RaceRunner, mine: Boolean) {
  val colors = MaterialTheme.colorScheme
  val text = buildAnnotatedString {
    append("$place. ${runner.name} ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (mine) colors.onPrimary else colors.primary)) { append("${runner.score}") }
    if (runner.done) append(" ✓")
  }
  Text(
    text, Modifier.background(if (mine) colors.primary else colors.surfaceVariant, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
    color = if (mine) colors.onPrimary else if (runner.present) colors.onSurface else colors.outline, maxLines = 1, fontSize = 14.sp,
  )
}

@Composable
fun RaceWaiting(board: RaceBoard, title: String) {
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
      CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    board.runners.forEachIndexed { i, r -> RunnerCard(i + 1, r, board) }
  }
}

@Composable
private fun RunnerCard(place: Int, runner: RaceRunner, board: RaceBoard) {
  val colors = MaterialTheme.colorScheme
  val mine = runner.name == board.me
  Row(
    Modifier.fillMaxWidth().background(if (mine) colors.primaryContainer else colors.surfaceVariant, RoundedCornerShape(14.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    PlayerAvatar(runner.name, mine)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text("$place. ${runner.name}", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (runner.lives > 0 || runner.done) Hearts(runner.lives)
        StatusChip(runner)
      }
    }
    Column(horizontalAlignment = Alignment.End) {
      Text("${runner.score}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = colors.primary)
      Text(if (runner.score == 1) board.unit else board.units, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
  }
}

@Composable
private fun Hearts(lives: Int) {
  Text(if (lives > 0) "♥".repeat(lives) else "♡", color = HEART, fontSize = 14.sp, letterSpacing = 1.sp)
}

@Composable
private fun StatusChip(runner: RaceRunner) {
  val colors = MaterialTheme.colorScheme
  val (text, color) = when {
    !runner.present -> "Desconectado" to colors.outline
    runner.ready -> "✓ Listo" to colors.primary
    runner.done -> "Sin vidas" to colors.error
    else -> "Jugando" to colors.onSurfaceVariant
  }
  Text(text, Modifier.border(1.dp, color, CircleShape).padding(horizontal = 8.dp, vertical = 1.dp), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun RaceResultsScreen(board: RaceBoard, onExit: () -> Unit) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Header("Resultados", onExit)
    Winner(board)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      board.runners.forEachIndexed { i, r -> ResultRow(i + 1, r, board.count(r.score), r.name == board.me) }
    }
    Button(modifier = Modifier.fillMaxWidth(), onClick = onExit) { Text("Salir") }
  }
}

@Composable
private fun ColumnScope.Winner(board: RaceBoard) {
  val first = board.runners.firstOrNull() ?: return
  val title = if (first.name == board.me) "¡Ganaste!" else "Ganó ${first.name}"
  Text(title, Modifier.align(Alignment.CenterHorizontally), fontSize = 30.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ResultRow(place: Int, runner: RaceRunner, score: String, mine: Boolean) {
  val colors = MaterialTheme.colorScheme
  Row(
    Modifier.fillMaxWidth().background(if (mine) colors.primaryContainer else colors.surfaceVariant, RoundedCornerShape(10.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("$place.", Modifier.width(32.dp), fontWeight = FontWeight.Bold, color = placeColor(place))
    Text(runner.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(score)
  }
}

@Composable
private fun placeColor(place: Int): Color = if (place == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
