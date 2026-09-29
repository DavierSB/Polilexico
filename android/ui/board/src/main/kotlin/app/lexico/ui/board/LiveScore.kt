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

/**
 * Quien sabe los puntos de una colocacion (en la app, el motor). Devuelve `null` si la jugada no
 * cabe en el tablero.
 */
fun interface PlayScorer {
  suspend fun score(board: Board, placement: Placement): Int?
}

/**
 * El [PlayScorer] de los tableros donde se colocan fichas ([PlacingBoard]). Lo pone la app segun
 * las opciones; `null` = sin puntos en vivo.
 */
val LocalPlayScorer = compositionLocalOf<PlayScorer?> { null }

/** Los puntos de la jugada a medio colocar y la casilla sobre la que se muestran (su ultima ficha). */
@Immutable
data class LiveScore(val points: Int, val square: Position)

/**
 * Los puntos de las fichas `pending` sobre `board`, recalculados con cada cambio; `null` si no hay
 * [LocalPlayScorer] o las fichas no forman una jugada.
 */
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
