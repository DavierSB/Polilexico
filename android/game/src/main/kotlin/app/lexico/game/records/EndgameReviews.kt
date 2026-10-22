package app.lexico.game.records

import app.lexico.game.engine.moveText
import app.lexico.game.engine.rackTiles
import app.lexico.go.review.Endgame
import app.lexico.go.review.EndgameLine as GoLine
import app.lexico.model.Board
import app.lexico.model.Placement

internal fun endgameReview(board: Board, e: Endgame): EndgameReview =
  EndgameReview((0 until e.lineCount()).map { line(board, e.lineAt(it)) }, e.depth.toInt(), e.exact)

private fun line(board: Board, l: GoLine): EndgameLine {
  val replies = (0 until l.stepCount()).map { l.stepAt(it) }.map { it.description to it.score.toInt() }
  return EndgameLine(l.spread.toInt(), steps(board, listOf(l.description to l.score.toInt()) + replies), ending(l), l.depth == 0L)
}

internal fun ending(l: GoLine): LineEnding =
  LineEnding(rackTiles(l.moverLeft), rackTiles(l.otherLeft), l.moverDelta.toInt(), l.otherDelta.toInt())

internal fun steps(start: Board, moves: List<Pair<String, Int>>): List<LineStep> {
  val placements = moves.map { (description, _) -> Placement.parseOrNull(description) }
  val boards = placements.runningFold(start) { board, p -> p?.let(board::play) ?: board }
  return moves.mapIndexed { i, (description, score) ->
    LineStep(ReviewMove(moveText(boards[i], description), score, null, placements[i]), boards[i])
  }
}
