package app.lexico.game.records

import app.lexico.game.engine.EngineListener
import app.lexico.game.engine.attempted
import app.lexico.game.engine.engine
import app.lexico.game.engine.moveText
import app.lexico.game.engine.stoppingOnCancel
import app.lexico.go.review.PreEndgame
import app.lexico.go.review.PreEndgameAnalysis
import app.lexico.go.review.PreEndgameMove
import app.lexico.model.Board
import app.lexico.model.Placement
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class PreEndgameAnalyzer internal constructor(private val analysis: PreEndgameAnalysis, private val board: Board) {
  suspend fun analyze(update: suspend (PreEndgameReview) -> Unit) = coroutineScope {
    val changes = Channel<Unit>(Channel.CONFLATED)
    launch { run(changes) }
    for (change in changes) update(latest())
    update(latest())
  }

  private suspend fun run(changes: Channel<Unit>) {
    val listener = EngineListener().apply { target = { changes.trySend(Unit) } }
    try {
      stoppingOnCancel(analysis::stop) { engine { analysis.run(listener) } }
    } finally {
      changes.close()
    }
  }

  private suspend fun latest(): PreEndgameReview = engine { review(analysis.latest()) }

  private fun review(p: PreEndgame): PreEndgameReview = PreEndgameReview(
    (0 until p.moveCount()).map { rated(p.moveAt(it)) }, p.played?.let(::rated), p.playedRank.toInt().takeIf { it >= 0 }, p.verdict, p.done,
  )

  private fun rated(m: PreEndgameMove): RatedMove = RatedMove(
    ReviewMove(moveText(board, m.description), m.score.toInt(), m.value, Placement.parseOrNull(attempted(m.description))), m.adjustment, m.verdict,
  )
}
