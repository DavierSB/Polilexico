package app.lexico.game.analysis

import app.lexico.game.engine.boardText
import app.lexico.game.engine.engine
import app.lexico.game.engine.rackText
import app.lexico.game.records.EndgameExplorer
import app.lexico.game.records.PreEndgameAnalyzer
import app.lexico.game.records.ReviewTurn
import app.lexico.game.records.reviewTurn
import app.lexico.go.review.Review
import app.lexico.go.review.Turn

class PositionStudies internal constructor() {
  suspend fun turn(p: StudyPosition): ReviewTurn = engine { reviewTurn(goTurn(p)) }

  suspend fun explore(p: StudyPosition): EndgameExplorer = engine { EndgameExplorer(goTurn(p).explore(), p.board) }

  suspend fun preEndgame(p: StudyPosition): PreEndgameAnalyzer = engine { PreEndgameAnalyzer(goTurn(p).analyzePreEndgame(), p.board) }

  private fun goTurn(p: StudyPosition): Turn =
    Review.fromPosition(boardText(p.board), rackText(p.rack), p.myScore.toLong(), p.opponentScore.toLong())
}
