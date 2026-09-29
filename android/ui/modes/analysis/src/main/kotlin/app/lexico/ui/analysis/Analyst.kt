package app.lexico.ui.analysis

import app.lexico.model.Board
import app.lexico.ui.common.RankedMove

/** Quien calcula las mejores jugadas para un tablero y un atril ("A", "CH", "?"...). */
fun interface Analyst {
  suspend fun bestMoves(board: Board, rack: List<String>): List<RankedMove>
}
