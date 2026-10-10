package app.lexico.games

import app.lexico.game.records.GameReview
import app.lexico.game.records.ReviewMark
import app.lexico.game.records.ReviewTurn
import app.lexico.game.storage.Mode
import app.lexico.ui.games.MovesRow
import app.lexico.ui.games.MovesTable
import app.lexico.ui.games.PlayedMove

private const val ME = "Tú"

internal fun movesTable(review: GameReview, opponent: String): MovesTable =
  if (review.game.mode == Mode.DUPLICATE) duplicateTable(review) else classicTable(review, opponent)

private fun duplicateTable(review: GameReview): MovesTable = MovesTable(ME, review.game.opponent, review.turns.mapIndexed { i, t ->
  MovesRow(i, t.marks.find { it.who == ME }?.let { played(it, t.myScore) }, t.marks.find { it.who != ME }?.let { played(it, t.opponentScore) })
})

private fun classicTable(review: GameReview, opponent: String): MovesTable {
  val names = if (review.humanStarts) ME to opponent else opponent to ME
  val rows = review.turns.foldIndexed(mutableListOf<MovesRow>()) { i, rows, t -> rows.apply { place(i, t, review) } }
  return MovesTable(names.first, names.second, rows)
}

private fun MutableList<MovesRow>.place(i: Int, t: ReviewTurn, review: GameReview) {
  val mine = t.player != review.game.opponent
  val move = t.marks.firstOrNull()?.let { played(it, if (mine) t.myScore else t.opponentScore) } ?: return
  val last = lastOrNull()
  when {
    mine == review.humanStarts -> add(MovesRow(i, move, null))
    last == null || last.right != null -> add(MovesRow(i, null, move))
    else -> this[lastIndex] = last.copy(right = move)
  }
}

private fun played(mark: ReviewMark, before: Int): PlayedMove = PlayedMove(mark.move.text, mark.move.score, before + mark.move.score)
