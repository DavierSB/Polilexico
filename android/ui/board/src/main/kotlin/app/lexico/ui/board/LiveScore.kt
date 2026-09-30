package app.lexico.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import app.lexico.model.Board
import app.lexico.model.Placement
import app.lexico.model.Position
import app.lexico.model.Tile

fun interface PlayScorer {
  suspend fun score(board: Board, placement: Placement): Int?
}

val LocalPlayScorer = compositionLocalOf<PlayScorer?> { null }

@Immutable
data class LiveScore(val points: Int, val square: Position)

@Composable
internal fun rememberLiveScore(board: Board, pending: Map<Position, Tile>): LiveScore? {
  val scorer = LocalPlayScorer.current
  val tiles = pending.toMap()
  val live by produceState<LiveScore?>(null, scorer, board, tiles) {
    value = scorer?.let { liveScore(it, board, tiles) }
  }
  return live
}

private suspend fun liveScore(scorer: PlayScorer, board: Board, tiles: Map<Position, Tile>): LiveScore? {
  val placement = board.placementOf(tiles) ?: return null
  val points = scorer.score(board, placement) ?: return null
  return LiveScore(points, lastSquare(tiles.keys))
}
