package app.lexico.ui.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.Dp
import app.lexico.ui.board.BoardStyle
import kotlinx.coroutines.CancellationException

typealias TurnRankings = suspend (Int) -> ReviewTurnView

@Composable
internal fun RankedPage(review: ReviewView, index: Int, rankings: TurnRankings, style: BoardStyle, height: Dp, go: (Int) -> Unit) {
  val ranked by produceState<ReviewTurnView?>(null, index) { value = ranked(rankings, index) }
  PlainPage(review, index, ranked ?: review.turns[index], null, style, height, go)
}

private suspend fun ranked(rankings: TurnRankings, index: Int): ReviewTurnView? = try {
  rankings(index)
} catch (e: CancellationException) {
  throw e
} catch (e: Exception) {
  null
}
