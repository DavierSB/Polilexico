package app.lexico.game.scoring

import app.lexico.game.engine.boardText
import app.lexico.game.engine.engine
import app.lexico.go.engine.Engine
import app.lexico.model.Board
import app.lexico.model.Placement

/** Los puntos de una colocacion segun el motor, sin mirar el diccionario (los que se ven al colocar). */
class EngineScorer internal constructor() {
  /** `null` si la jugada no cabe: sin tocar otras fichas o sin pasar por el centro en la primera. */
  suspend fun score(board: Board, placement: Placement): Int? =
    engine { runCatching { Engine.placementScore(boardText(board), placement.toString()).toInt() }.getOrNull() }
}
