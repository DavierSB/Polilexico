package app.lexico.game.scoring

import app.lexico.game.engine.boardText
import app.lexico.game.engine.engine
import app.lexico.go.engine.Engine
import app.lexico.model.Board
import app.lexico.model.Placement

class EngineScorer internal constructor() {
  suspend fun score(board: Board, placement: Placement): Int? =
    engine { runCatching { Engine.placementScore(boardText(board), placement.toString()).toInt() }.getOrNull() }
}
