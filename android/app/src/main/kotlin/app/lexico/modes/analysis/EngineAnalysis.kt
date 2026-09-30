package app.lexico.modes.analysis

import app.lexico.game.Lexico
import app.lexico.ui.analysis.Analyst
import app.lexico.ui.common.RankedMove

fun engineAnalyst(lexico: Lexico): Analyst = Analyst { board, rack ->
  lexico.analyst.bestMoves(board, rack).map { RankedMove(it.text, it.score, it.equity, it.placement) }
}
