package app.lexico.games

import app.lexico.game.Outcome
import app.lexico.game.records.FinishedGame
import app.lexico.game.records.GameReview
import app.lexico.game.records.ReviewMark
import app.lexico.game.records.ReviewMove
import app.lexico.game.records.ReviewTurn
import app.lexico.game.storage.GameProgress
import app.lexico.game.storage.Mode
import app.lexico.game.storage.SavedGame
import app.lexico.model.Letters
import app.lexico.ui.classic.alias
import app.lexico.ui.common.Efficiency
import app.lexico.ui.common.RankedMove
import app.lexico.ui.common.Spread
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
private const val ME = "Tú"
private const val MASTER = "Máster"
private const val RACK = 7
private const val TIE = 1e-6

fun inProgressItem(saved: SavedGame, progress: GameProgress?): InProgressItem = InProgressItem(
  saved.id, gameTitle(saved.mode, progress?.opponent), savedAt(saved),
  progress?.let { scoreText(saved.mode, it.myScore, it.opponentScore) }, progress?.round?.let { "Ronda $it" },
)

fun finishedItem(g: FinishedGame): FinishedItem =
  FinishedItem(g.path, title(g), detail(g), score(g), won(g))

fun folderOf(mode: Mode): GameFolder = when (mode) {
  Mode.CLASSIC -> GameFolder.CLASSIC
  Mode.DUPLICATE -> GameFolder.DUPLICATE
  Mode.ENDGAME -> GameFolder.MINIGAMES
}

fun reviewView(review: GameReview): ReviewView {
  val equity = review.game.mode != Mode.DUPLICATE
  val title = "${modeName(review.game.mode)} · ${review.game.myScore}–${review.game.opponentScore}"
  val opponent = alias(review.game.opponent)
  val turns = review.turns.map { turnView(it, equity, review.game.opponent) }
  return ReviewView(
    title, turns, review.startTurn, opponent, spread(review, opponent),
    classic = equity, efficiency = efficiency(review), moves = movesTable(review, opponent),
  )
}

private fun efficiency(review: GameReview): Efficiency? {
  if (review.game.mode != Mode.DUPLICATE) return null
  val points = { who: String -> review.turns.map { t -> t.marks.find { it.who == who }?.move?.score ?: 0 } }
  return Efficiency.of(points(ME), points(MASTER))
}

private fun spread(review: GameReview, opponent: String): Spread {
  val totals = review.turns.map { it.myScore to it.opponentScore } + (review.game.myScore to review.game.opponentScore)
  return Spread.of(ME, opponent, review.humanStarts, totals)
}

private fun savedAt(saved: SavedGame): String = "Guardada el " + SAVED_AT.format(Date(saved.updatedAt))

private fun score(g: FinishedGame): String = scoreText(g.mode, g.myScore, g.opponentScore)

private fun scoreText(mode: Mode, mine: Int, theirs: Int): String =
  if (mode == Mode.DUPLICATE) "$mine/$theirs" else "$mine – $theirs"

private fun modeName(mode: Mode): String = when (mode) {
  Mode.CLASSIC -> "Clásica"
  Mode.ENDGAME -> "Finales"
  Mode.DUPLICATE -> "Duplicada"
}

private fun title(g: FinishedGame): String = gameTitle(g.mode, g.opponent)

private fun gameTitle(mode: Mode, opponent: String?): String =
  if (mode == Mode.DUPLICATE || opponent == null) modeName(mode) else "${modeName(mode)} contra ${alias(opponent)}"

private fun detail(g: FinishedGame): String {
  val date = finishedAt(g)
  return if (g.mode == Mode.DUPLICATE) "$date · ${g.hits} aciertos" else date
}

fun finishedAt(g: FinishedGame): String = FINISHED_AT.format(Date(g.finishedAt))

private fun won(g: FinishedGame): Boolean? = if (g.mode == Mode.DUPLICATE) null else won(g.outcome)

private fun won(outcome: Outcome): Boolean? = when (outcome) {
  Outcome.WIN -> true
  Outcome.LOSS -> false
  Outcome.TIE -> null
}

private fun turnView(t: ReviewTurn, equity: Boolean, opponent: String): ReviewTurnView {
  val unseen = unseen(t)
  return ReviewTurnView(
    t.number, alias(t.player), t.rack, t.board, candidates(t, equity), t.marks.map { markView(it, t, equity) },
    t.myScore, t.opponentScore, mine(t.player, opponent), unseen, bag(unseen, equity),
  )
}

private fun mine(player: String, opponent: String): Boolean? = when (player) {
  "" -> null
  opponent -> false
  else -> true
}

private fun candidates(t: ReviewTurn, equity: Boolean): List<RankedMove> =
  t.candidates.mapIndexed { i, m -> rankedMove(m, equity, t.marks.filter { it.rank == i }.joinToString(" ") { alias(it.who) }) }

private fun markView(m: ReviewMark, t: ReviewTurn, equity: Boolean): MarkView {
  val ranked = m.rank?.let { t.candidates.getOrNull(it) }
  return MarkView(alias(m.who), rankedMove(m.move.copy(equity = ranked?.equity), equity), ranked?.let { tieRank(t, it, equity) })
}

private fun tieRank(t: ReviewTurn, played: ReviewMove, equity: Boolean): Int =
  t.candidates.count { worth(it, equity) > worth(played, equity) + TIE }

private fun worth(m: ReviewMove, equity: Boolean): Double = if (equity) m.equity ?: m.score.toDouble() else m.score.toDouble()

private fun unseen(t: ReviewTurn): List<String> {
  val seen = t.board.tiles.values.map { if (it.blank) Letters.BLANK else it.letter.uppercase() } + t.rack
  return Letters.BAG.toMutableList().apply { seen.forEach { remove(it) } }
}

private fun bag(unseen: List<String>, classic: Boolean): Int = if (classic) unseen.size - minOf(RACK, unseen.size) else unseen.size

private fun rankedMove(m: ReviewMove, equity: Boolean, playedBy: String = ""): RankedMove =
  RankedMove(m.text, m.score, m.equity?.takeIf { equity }, m.placement, playedBy)
