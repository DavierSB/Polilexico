package app.lexico.ui.analysis

import app.lexico.model.Board
import app.lexico.ui.common.RankedMove

fun interface Analyst {
  suspend fun bestMoves(board: Board, rack: List<String>): List<RankedMove>

  suspend fun phase(board: Board, rack: List<String>): Phase = Phase.MIDGAME
}

enum class Phase { MIDGAME, PRE_ENDGAME, ENDGAME }

data class EditedPosition(val board: Board, val rack: List<String>, val myScore: Int, val opponentScore: Int)
