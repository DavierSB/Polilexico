package app.lexico.games.export

import app.lexico.model.Board
import app.lexico.ui.duplicate.SeatEntry
import app.lexico.ui.duplicate.SeatRound
import app.lexico.ui.duplicate.SeatView

private const val MASTER = "Máster"
private const val TITLE = "Duplicada"
private const val PLACES = 3

internal data class Place(val name: String, val total: Int, val percent: Int)

internal data class Podium(val places: List<Place>, val master: Int)

internal fun tableStills(view: SeatView): List<Still> {
  val boards = view.rounds.runningFold(Board.EMPTY) { board, round -> afterRound(board, round) }.drop(1)
  val rounds = view.rounds.zip(boards) { round, board -> Still(board, TITLE, roundLine(round), podium(round.entries)) }
  return rounds + finalStill(view)
}

internal fun finalStill(view: SeatView): Still = Still(view.board, TITLE, summary(view), podium(view.rounds.lastOrNull()?.entries.orEmpty()))

private fun afterRound(board: Board, round: SeatRound): Board {
  val placement = master(round.entries)?.placement?.takeIf { it.isNotEmpty() } ?: return board
  return runCatching { board.play(placement) }.getOrDefault(board)
}

private fun master(entries: List<SeatEntry>): SeatEntry? = entries.find { it.name == MASTER }

private fun roundLine(round: SeatRound): String {
  val top = master(round.entries) ?: return "Ronda ${round.number}"
  return "Ronda ${round.number} · Máster: ${top.text} (${top.points})"
}

private fun summary(view: SeatView): String {
  val players = view.players.count { it != MASTER }
  return "${view.rounds.size} rondas · $players jugadores"
}

private fun podium(entries: List<SeatEntry>): Podium {
  val master = master(entries)?.total ?: 0
  val places = entries.filter { it.name != MASTER }.sortedByDescending { it.total }.take(PLACES)
  return Podium(places.map { Place(it.name, it.total, percent(it.total, master)) }, master)
}

private fun percent(total: Int, master: Int): Int = if (master > 0) total * 100 / master else 0
