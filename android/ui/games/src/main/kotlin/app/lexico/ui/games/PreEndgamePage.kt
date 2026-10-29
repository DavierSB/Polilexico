package app.lexico.ui.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import app.lexico.ui.board.BoardStyle
import app.lexico.ui.common.RankedMove
import kotlinx.coroutines.CancellationException

typealias PreEndgameAnalyses = suspend (Int, (LiveRanking) -> Unit) -> Unit

data class LiveRanking(val candidates: List<RankedMove>, val marks: List<MarkView>, val done: Boolean, val verdict: PreEndgameVerdict)

enum class PreEndgameVerdict { NONE, BINGO, PASS, FAILED }

private const val THINKING = "Pensando el pre-final…"

@Composable
internal fun PreEndgamePage(review: ReviewView, index: Int, analyses: PreEndgameAnalyses, style: BoardStyle, height: Dp, go: (Int) -> Unit) {
  val turn = review.turns[index]
  var live by remember { mutableStateOf<LiveRanking?>(null) }
  LaunchedEffect(index) { live = analyzed(analyses, index) { live = it } ?: LiveRanking(turn.candidates, turn.marks, true, PreEndgameVerdict.FAILED) }
  val shown = live?.let { turn.copy(candidates = it.candidates, marks = it.marks) } ?: turn
  PlainPage(review, index, shown, status(live), style, height, go)
}

private fun status(live: LiveRanking?): String? = when {
  live == null || !live.done -> THINKING
  else -> when (live.verdict) {
    PreEndgameVerdict.BINGO -> "Este scrabble gana el final aun en el peor caso: va primero."
    PreEndgameVerdict.PASS -> "Pasando ya se gana el final: va primero."
    PreEndgameVerdict.FAILED -> "No se pudo analizar el pre-final de este turno."
    PreEndgameVerdict.NONE -> null
  }
}

private suspend fun analyzed(analyses: PreEndgameAnalyses, index: Int, update: (LiveRanking) -> Unit): LiveRanking? {
  var last: LiveRanking? = null
  try {
    analyses(index) { last = it; update(it) }
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    return null
  }
  return last
}
