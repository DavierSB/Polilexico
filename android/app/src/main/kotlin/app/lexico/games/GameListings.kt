package app.lexico.games

import app.lexico.game.Outcome
import app.lexico.game.records.FinishedGame
import app.lexico.game.records.GameReview
import app.lexico.game.records.ReviewMark
import app.lexico.game.records.ReviewMove
import app.lexico.game.records.ReviewTurn
import app.lexico.game.storage.Mode
import app.lexico.game.storage.SavedGame
import app.lexico.ui.classic.alias
import app.lexico.ui.common.RankedMove
import app.lexico.ui.games.FinishedItem
import app.lexico.ui.games.GameFolder
import app.lexico.ui.games.InProgressItem
import app.lexico.ui.games.MarkView
import app.lexico.ui.games.ReviewTurnView
import app.lexico.ui.games.ReviewView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SAVED_AT = SimpleDateFormat("d MMM, HH:mm", Locale("es"))
private val FINISHED_AT = SimpleDateFormat("d/M/yyyy HH:mm", Locale("es"))

fun inProgressItem(saved: SavedGame): InProgressItem =
  InProgressItem(saved.id, modeName(saved.mode), "Guardada el " + SAVED_AT.format(Date(saved.updatedAt)))

fun finishedItem(g: FinishedGame): FinishedItem =
  FinishedItem(g.path, title(g), detail(g), "${g.myScore} – ${g.opponentScore}", won(g.outcome))

fun folderOf(mode: Mode): GameFolder = when (mode) {
  Mode.CLASSIC -> GameFolder.CLASSIC
  Mode.DUPLICATE -> GameFolder.DUPLICATE
  Mode.ENDGAME -> GameFolder.MINIGAMES
}

fun reviewView(review: GameReview): ReviewView {
  val equity = review.game.mode != Mode.DUPLICATE
  val title = "${modeName(review.game.mode)} · ${review.game.myScore}–${review.game.opponentScore}"
  return ReviewView(title, review.turns.map { turnView(it, equity) }, review.startTurn)
}

private fun modeName(mode: Mode): String = when (mode) {
  Mode.CLASSIC -> "Clásica"
  Mode.ENDGAME -> "Finales"
  Mode.DUPLICATE -> "Duplicada"
}

private fun title(g: FinishedGame): String = when (g.mode) {
  Mode.DUPLICATE -> "Duplicada"
  else -> "${modeName(g.mode)} contra ${alias(g.opponent)}"
}

private fun detail(g: FinishedGame): String {
  val date = finishedAt(g)
  return if (g.mode == Mode.DUPLICATE) "$date · ${g.hits} aciertos" else date
}

fun finishedAt(g: FinishedGame): String = FINISHED_AT.format(Date(g.finishedAt))

private fun won(outcome: Outcome): Boolean? = when (outcome) {
  Outcome.WIN -> true
  Outcome.LOSS -> false
  Outcome.TIE -> null
}

private fun turnView(t: ReviewTurn, equity: Boolean): ReviewTurnView =
  ReviewTurnView(t.number, t.player, t.rack, t.board, candidates(t, equity), t.marks.map { markView(it, equity) })

private fun candidates(t: ReviewTurn, equity: Boolean): List<RankedMove> =
  t.candidates.mapIndexed { i, m -> rankedMove(m, equity, t.marks.filter { it.rank == i }.joinToString(" ") { alias(it.who) }) }

private fun markView(m: ReviewMark, equity: Boolean): MarkView = MarkView(alias(m.who), rankedMove(m.move, equity), m.rank)

private fun rankedMove(m: ReviewMove, equity: Boolean, playedBy: String = ""): RankedMove =
  RankedMove(m.text, m.score, m.equity?.takeIf { equity }, m.placement, playedBy)
