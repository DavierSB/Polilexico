package app.lexico.games

import app.lexico.game.records.PreEndgameAnalyzer
import app.lexico.game.records.PreEndgameReview
import app.lexico.game.records.RatedMove
import app.lexico.ui.common.RankedMove
import app.lexico.ui.games.LiveRanking
import app.lexico.ui.games.MarkView
import app.lexico.ui.games.PreEndgameVerdict
import app.lexico.ui.games.ReviewTurnView

private const val BINGO = "bingo"
private const val PASS = "pass"

suspend fun preEndgameRanking(analyzer: PreEndgameAnalyzer, turn: ReviewTurnView, update: (LiveRanking) -> Unit) =
  analyzer.analyze { update(liveRanking(it, turn)) }

private fun liveRanking(r: PreEndgameReview, turn: ReviewTurnView): LiveRanking {
  val who = turn.marks.firstOrNull()?.who.orEmpty()
  val candidates = r.moves.mapIndexed { i, m -> rankedMove(m, if (i == r.playedRank) who else "") }
  val marks = r.played?.let { listOf(MarkView(who, rankedMove(it), r.playedRank)) } ?: turn.marks
  return LiveRanking(candidates, marks, r.done, verdict(r.verdict))
}

private fun rankedMove(r: RatedMove, playedBy: String = ""): RankedMove =
  RankedMove(r.move.text, r.move.score, r.move.equity, r.move.placement, playedBy, note = note(r), adjusted = true)

private fun note(r: RatedMove): String = when {
  r.verdict == BINGO -> "scrabble ganador"
  r.verdict == PASS -> "pasar gana"
  r.adjustment != 0.0 -> "Q %+.1f".format(r.adjustment)
  else -> ""
}

private fun verdict(v: String): PreEndgameVerdict = when (v) {
  BINGO -> PreEndgameVerdict.BINGO
  PASS -> PreEndgameVerdict.PASS
  else -> PreEndgameVerdict.NONE
}
