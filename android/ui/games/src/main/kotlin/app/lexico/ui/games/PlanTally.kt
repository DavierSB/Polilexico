package app.lexico.ui.games

import app.lexico.model.Letters
import app.lexico.ui.common.RankedMove

internal data class PlanTally(val scores: List<Int>, val racks: List<List<String>>)

internal fun planTallies(position: ExploredPosition, line: LineView): List<PlanTally> {
  val played = line.steps.runningFold(PlanTally(position.scores, position.racks)) { t, s ->
    t.after(position.players.indexOf(s.who), s.move)
  }.drop(1)
  if (line.pending || played.isEmpty()) return played
  return played.dropLast(1) + played.last().ended(position.players, line.ending)
}

private fun PlanTally.after(player: Int, move: RankedMove): PlanTally =
  PlanTally(scores.changed(player) { it + move.points }, racks.changed(player) { it.without(spent(move)) })

private fun PlanTally.ended(players: List<String>, ending: List<EndingRow>): PlanTally =
  copy(scores = scores.mapIndexed { i, s -> s + ending.filter { it.who == players[i] }.sumOf { it.delta } })

private fun spent(move: RankedMove): List<String> =
  move.placement?.placed.orEmpty().values.map { if (it.blank) Letters.BLANK else it.letter.uppercase() }

private fun List<String>.without(tiles: List<String>): List<String> = toMutableList().apply { tiles.forEach { remove(it) } }

private fun <T> List<T>.changed(index: Int, change: (T) -> T): List<T> = mapIndexed { i, v -> if (i == index) change(v) else v }
