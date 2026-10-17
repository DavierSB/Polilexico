package app.lexico.ui.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.lexico.model.Board
import app.lexico.ui.common.RankedMove
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EndgameView(val turn: ReviewTurnView, val lines: List<LineView>) {
  fun lineOf(m: RankedMove): LineView? = lines.find { it.move.text == m.text }
}

data class LineView(val move: RankedMove, val spread: Int, val mover: String, val steps: List<StepView>, val ending: List<EndingRow>)

data class EndingRow(val who: String, val left: List<String>, val delta: Int)

data class StepView(val who: String, val move: RankedMove, val board: Board)

typealias EndgameAnalyst = suspend (Int) -> EndgameView

internal class EndgameShown(val view: EndgameView?, val calculating: Boolean)

@Composable
internal fun rememberEndgameAnalyses(analyst: EndgameAnalyst?): EndgameAnalyses {
  val scope = rememberCoroutineScope()
  return remember(analyst) { EndgameAnalyses(scope, analyst) }
}

@Composable
internal fun EndgameAnalyses.shown(page: Int, turn: ReviewTurnView): EndgameShown {
  LaunchedEffect(page) { if (wanted(turn)) request(page) }
  return EndgameShown(result(page), wanted(turn) && !done(page))
}

internal class EndgameAnalyses(private val scope: CoroutineScope, private val analyst: EndgameAnalyst?) {
  private val results = mutableStateMapOf<Int, EndgameView?>()
  private val requested = mutableSetOf<Int>()
  private val oneAtATime = Mutex()

  fun wanted(turn: ReviewTurnView): Boolean = analyst != null && turn.endgame

  fun result(page: Int): EndgameView? = results[page]

  fun done(page: Int): Boolean = page in results

  fun request(page: Int) {
    val analyst = analyst ?: return
    if (requested.add(page)) scope.launch { oneAtATime.withLock { results[page] = analysis(analyst, page) } }
  }
}

private suspend fun analysis(analyst: EndgameAnalyst, page: Int): EndgameView? =
  try {
    analyst(page)
  } catch (e: CancellationException) {
    throw e
  } catch (_: Exception) {
    null
  }
