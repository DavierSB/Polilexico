package app.lexico.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import app.lexico.model.Board
import app.lexico.model.Position

private const val GAP_MS = 160

private const val DROP_MS = 300

@Composable
internal fun rememberLanding(board: Board, enabled: Boolean): TileLanding? {
  val first = remember { board.latest }
  val landing = remember(board.latest) { landingOf(board.latest).takeIf { enabled && board.latest != first } }
  LaunchedEffect(landing) { landing?.run() }
  return landing
}

private fun landingOf(latest: Set<Position>): TileLanding? =
  if (latest.isEmpty()) null else TileLanding(latest.sortedWith(compareBy({ it.row }, { it.column })))

@Stable
internal class TileLanding(private val order: List<Position>) {
  private val elapsed = Animatable(0f)

  val durationMs: Int = (order.size - 1) * GAP_MS + DROP_MS

  suspend fun run() {
    elapsed.animateTo(durationMs.toFloat(), tween(durationMs, easing = LinearEasing))
  }

  fun progress(p: Position): Float? {
    val i = order.indexOf(p)
    if (i < 0 || elapsed.value >= durationMs) return null
    return FastOutSlowInEasing.transform(((elapsed.value - i * GAP_MS) / DROP_MS).coerceIn(0f, 1f))
  }
}
