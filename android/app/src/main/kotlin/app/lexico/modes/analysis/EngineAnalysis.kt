package app.lexico.modes.analysis

import app.lexico.game.Lexico
import app.lexico.game.analysis.StudyPosition
import app.lexico.ui.analysis.Anagrams
import app.lexico.ui.analysis.Analyst
import app.lexico.ui.analysis.EditedPosition
import app.lexico.ui.analysis.Phase
import app.lexico.ui.common.RankedMove
import app.lexico.model.Board

fun engineAnalyst(lexico: Lexico): Analyst = object : Analyst {
  override suspend fun bestMoves(board: Board, rack: List<String>): List<RankedMove> =
    lexico.analyst.bestMoves(board, rack).map { RankedMove(it.text, it.score, it.equity, it.placement) }

  override suspend fun phase(board: Board, rack: List<String>): Phase {
    val turn = lexico.studies.turn(StudyPosition(board, rack))
    return when {
      turn.endgame -> Phase.ENDGAME
      turn.preEndgame -> Phase.PRE_ENDGAME
      else -> Phase.MIDGAME
    }
  }
}

fun engineAnagrams(lexico: Lexico): Anagrams = Anagrams { lexico.anagrammer.anagrams(it) }

fun studyPosition(p: EditedPosition): StudyPosition = StudyPosition(p.board, p.rack, p.myScore, p.opponentScore)
