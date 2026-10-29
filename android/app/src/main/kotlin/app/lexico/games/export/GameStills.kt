package app.lexico.games.export

import app.lexico.game.records.GameReview
import app.lexico.game.records.ReviewMark
import app.lexico.game.records.ReviewTurn
import app.lexico.game.storage.Mode
import app.lexico.model.Board
import app.lexico.ui.classic.alias

private const val ME = "Tú"

internal data class Still(val board: Board, val score: String, val caption: String)

internal fun stills(review: GameReview): List<Still> {
  val first = review.turns.firstOrNull() ?: return emptyList()
  val opening = Still(first.board, score(review, first.myScore, first.opponentScore), title(review))
  return listOf(opening) + review.turns.mapIndexed { i, t -> after(review, t, review.turns.getOrNull(i + 1)) }
}

private fun after(review: GameReview, turn: ReviewTurn, next: ReviewTurn?): Still {
  val mark = played(review, turn)
  val board = mark?.move?.placement?.let { runCatching { turn.board.play(it) }.getOrNull() } ?: next?.board ?: turn.board
  val score = if (next != null) score(review, next.myScore, next.opponentScore) else score(review, review.game.myScore, review.game.opponentScore)
  return Still(board, score, mark?.let(::moveText).orEmpty())
}

private fun played(review: GameReview, turn: ReviewTurn): ReviewMark? =
  if (review.game.mode == Mode.DUPLICATE) turn.marks.find { it.who != ME } else turn.marks.firstOrNull()

private fun moveText(mark: ReviewMark): String = "${alias(mark.who)}: ${mark.move.text} (${mark.move.score})"

private fun score(review: GameReview, mine: Int, theirs: Int): String = "$ME $mine – $theirs ${alias(review.game.opponent)}"

private fun title(review: GameReview): String =
  if (review.game.mode == Mode.DUPLICATE) "Duplicada" else "Partida contra ${alias(review.game.opponent)}"
