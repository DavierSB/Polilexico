package app.lexico.ui.analysis

import app.lexico.model.Board
import app.lexico.ui.common.RankedMove

fun interface Analyst {
  suspend fun bestMoves(board: Board, rack: List<String>): List<RankedMove>
}
