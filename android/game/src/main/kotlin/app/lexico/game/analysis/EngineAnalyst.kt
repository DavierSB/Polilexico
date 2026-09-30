package app.lexico.game.analysis

import app.lexico.game.engine.boardText
import app.lexico.game.engine.engine
import app.lexico.game.engine.parseCandidates
import app.lexico.game.engine.moveText
import app.lexico.game.engine.rackText
import app.lexico.go.engine.Engine
import app.lexico.model.Board
import app.lexico.model.Placement

/** Cuantas jugadas pide el analizador. */
private const val CANDIDATES = 15L

/**
 * Una jugada propuesta por el motor: `text` es "H8 CASA" o "(Pasar)"; `placement`, la
 * colocacion (null en pases y cambios); `equity`, la valoracion de Woogles.
 */
data class BestMove(val text: String, val score: Int, val equity: Double, val placement: Placement?)

/** Las mejores jugadas de una posicion cualquiera, segun HastyBot (el "gen" de macondo). */
class EngineAnalyst internal constructor() {
  suspend fun bestMoves(board: Board, rack: List<String>): List<BestMove> =
    engine { parseCandidates(Engine.bestMoves(boardText(board), rackText(rack), CANDIDATES)) }.map {
      BestMove(moveText(board, it.description), it.score, it.equity, Placement.parseOrNull(it.description))
    }
}
