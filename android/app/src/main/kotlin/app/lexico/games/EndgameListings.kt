package app.lexico.games

import app.lexico.game.records.EndgameLine
import app.lexico.game.records.EndgameReview
import app.lexico.game.records.ReviewMove
import app.lexico.ui.common.RankedMove
import app.lexico.ui.games.EndgameView
import app.lexico.ui.games.EndingRow
import app.lexico.ui.games.LineView
import app.lexico.ui.games.MarkView
import app.lexico.ui.games.ReviewTurnView
import app.lexico.ui.games.StepView

private const val ME = "Tú"

fun endgameView(analysis: EndgameReview, turn: ReviewTurnView, opponent: String): EndgameView {
  val players = if (turn.mine == false) listOf(opponent, ME) else listOf(ME, opponent)
  val lines = analysis.lines.map { lineView(it, players) }
  return EndgameView(turn.copy(candidates = candidates(lines, turn), marks = turn.marks.map { markOf(it, lines) }), lines)
}

private fun lineView(l: EndgameLine, players: List<String>): LineView =
  LineView(
    ranked(l.move, l.spread), l.spread, players[0], l.steps.mapIndexed { i, s -> StepView(players[i % 2], ranked(s.move, null), s.board) },
    listOf(EndingRow(players[0], l.ending.moverLeft, l.ending.moverDelta), EndingRow(players[1], l.ending.otherLeft, l.ending.otherDelta)),
  )

private fun ranked(m: ReviewMove, final: Int?): RankedMove = RankedMove(m.text, m.score, null, m.placement, final = final)

private fun candidates(lines: List<LineView>, turn: ReviewTurnView): List<RankedMove> =
  lines.map { l -> l.move.copy(playedBy = turn.marks.filter { it.move.text == l.move.text }.joinToString(" ") { it.who }) }

private fun markOf(mark: MarkView, lines: List<LineView>): MarkView {
  val line = lines.find { it.move.text == mark.move.text }
  return MarkView(mark.who, mark.move.copy(equity = null, final = line?.spread), line?.let { l -> lines.count { it.spread > l.spread } })
}
