package app.lexico.game.records

import app.lexico.game.engine.attempted
import app.lexico.game.engine.moveText
import app.lexico.game.engine.parseBoard
import app.lexico.game.engine.rackTiles
import app.lexico.go.review.Mark
import app.lexico.go.review.Move
import app.lexico.go.review.Turn
import app.lexico.model.Board
import app.lexico.model.Placement

internal fun reviewTurn(t: Turn): ReviewTurn {
  val board = parseBoard(t.board)
  return ReviewTurn(
    number = t.number.toInt(), player = t.player, rack = rackTiles(t.rack), board = board,
    candidates = (0 until t.candidateCount()).map { move(board, t.candidateAt(it)) },
    marks = (0 until t.markCount()).map { mark(board, t.markAt(it)) },
    myScore = t.myScore.toInt(), opponentScore = t.opponentScore.toInt(), endgame = t.endgame, preEndgame = t.preEndgame,
  )
}

private fun move(board: Board, m: Move): ReviewMove =
  reviewMove(board, m.description, m.score.toInt(), m.equity.takeIf { m.hasEquity })

private fun mark(board: Board, m: Mark): ReviewMark =
  ReviewMark(m.who, reviewMove(board, m.description, m.score.toInt(), null), m.rank.toInt().takeIf { it >= 0 })

private fun reviewMove(board: Board, description: String, score: Int, equity: Double?): ReviewMove =
  ReviewMove(moveText(board, description), score, equity, Placement.parseOrNull(attempted(description)))
