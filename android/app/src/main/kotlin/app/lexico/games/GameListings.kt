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

/*
 * Las partidas guardadas y terminadas, como las muestran las pantallas de :ui:games.
 */

private val SAVED_AT = SimpleDateFormat("d MMM, HH:mm", Locale("es"))
private val FINISHED_AT = SimpleDateFormat("d/M/yyyy HH:mm", Locale("es"))

fun inProgressItem(saved: SavedGame): InProgressItem =
  InProgressItem(saved.id, modeName(saved.mode), "Guardada el " + SAVED_AT.format(Date(saved.updatedAt)))

fun finishedItem(g: FinishedGame): FinishedItem =
  FinishedItem(g.path, title(g), detail(g), "${g.myScore} – ${g.opponentScore}", won(g.outcome))

/** La carpeta de "Mis partidas" de cada modalidad: Finales es un minijuego. */
fun folderOf(mode: Mode): GameFolder = when (mode) {
  Mode.CLASSIC -> GameFolder.CLASSIC
  Mode.DUPLICATE -> GameFolder.DUPLICATE
  Mode.ENDGAME -> GameFolder.MINIGAMES
}

fun reviewView(review: GameReview): ReviewView =
  ReviewView("${modeName(review.game.mode)} · ${review.game.myScore}–${review.game.opponentScore}", review.turns.map(::turnView), review.startTurn)

private fun modeName(mode: Mode): String = when (mode) {
  Mode.CLASSIC -> "Clásica"
  Mode.ENDGAME -> "Finales"
  Mode.DUPLICATE -> "Duplicada"
}

private fun title(g: FinishedGame): String = when (g.mode) {
  Mode.DUPLICATE -> "Duplicada contra el máster"
  else -> "${modeName(g.mode)} contra ${alias(g.opponent)}"
}

/** "27/9/2026 23:30", y en duplicada los aciertos. */
private fun detail(g: FinishedGame): String {
  val date = finishedAt(g)
  return if (g.mode == Mode.DUPLICATE) "$date · ${g.hits} aciertos" else date
}

/** Cuando termino, en la hora del telefono (la del registro va en UTC: el motor no sabe la zona). */
fun finishedAt(g: FinishedGame): String = FINISHED_AT.format(Date(g.finishedAt))

private fun won(outcome: Outcome): Boolean? = when (outcome) {
  Outcome.WIN -> true
  Outcome.LOSS -> false
  Outcome.TIE -> null
}

private fun turnView(t: ReviewTurn): ReviewTurnView =
  ReviewTurnView(t.number, t.player, t.rack, t.board, candidates(t), t.marks.map(::markView))

/** Las mejores jugadas, cada una con quien la jugo (si alguien). */
private fun candidates(t: ReviewTurn): List<RankedMove> =
  t.candidates.mapIndexed { i, m -> rankedMove(m, t.marks.filter { it.rank == i }.joinToString(" ") { alias(it.who) }) }

private fun markView(m: ReviewMark): MarkView = MarkView(alias(m.who), rankedMove(m.move), m.rank)

private fun rankedMove(m: ReviewMove, playedBy: String = ""): RankedMove = RankedMove(m.text, m.score, m.equity, m.placement, playedBy)
