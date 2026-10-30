package app.lexico.games

import app.lexico.game.Outcome
import app.lexico.game.records.FinishedGame
import app.lexico.game.records.Statistics
import app.lexico.game.storage.Mode
import app.lexico.game.storage.UnfinishedGame
import app.lexico.ui.classic.alias
import app.lexico.ui.classic.isBot
import app.lexico.ui.games.ChartGame
import app.lexico.ui.games.StatsFilter
import app.lexico.ui.games.StatsMode
import app.lexico.ui.games.StatsPage

private const val EFFICIENCY_NOTE =
  "Efectividad: tus puntos como porcentaje de los del máster. Acierto: un turno con el mismo puntaje que el máster."

data class StatsGames(val finished: List<FinishedGame>, val abandoned: List<UnfinishedGame>, val inProgress: List<UnfinishedGame>)

fun statsPage(filter: StatsFilter, games: StatsGames, reset: Boolean): StatsPage {
  val chosen = games.finished.filter { chosen(filter, it.mode, it.opponent) }
  val stats = Statistics.of(chosen, games.abandoned.count { chosen(filter, it.mode, it.opponent) }, games.inProgress.count { chosen(filter, it.mode, it.opponent) })
  val empty = emptyText(filter.opponent, reset)
  return when {
    stats.started == 0 -> StatsPage(emptyList(), empty, null, emptyList())
    filter.mode == StatsMode.CLASSIC -> StatsPage(classicRows(stats), empty, null, chosen.map(::chartGame))
    else -> StatsPage(duplicateRows(stats), empty, EFFICIENCY_NOTE.takeIf { stats.games > 0 }, chosen.map(::chartGame))
  }
}

private fun chosen(filter: StatsFilter, mode: Mode, opponent: String): Boolean =
  mode == mode(filter.mode) && (mode != Mode.CLASSIC || isBot(opponent)) && (filter.opponent == null || alias(opponent) == filter.opponent)

private fun mode(mode: StatsMode): Mode = if (mode == StatsMode.CLASSIC) Mode.CLASSIC else Mode.DUPLICATE

private fun emptyText(opponent: String?, reset: Boolean): String = when {
  reset -> "Aún no hay partidas desde que reiniciaste las estadísticas."
  opponent != null -> "Aún no hay partidas contra $opponent."
  else -> "Aún no hay partidas."
}

private fun countRows(s: Statistics): List<Pair<String, String>> = listOf(
  "Partidas iniciadas" to "${s.started}",
  "Terminadas" to "${s.games}",
  "Abandonadas" to "${s.abandoned}",
)

private fun classicRows(s: Statistics): List<Pair<String, String>> = countRows(s) + listOf(
  "Victorias" to "${s.wins}  (%.0f %%)".format(s.winRate),
) + if (s.games == 0) emptyList() else listOf(
  "Promedio de puntos" to "%.0f".format(s.averageScore),
  "Puntuación máxima" to "${s.bestScore}",
  "Scrabbles" to "${s.bingos}  (%.1f por partida)".format(s.bingos.toDouble() / s.games),
  "Palabra más valiosa" to bestWord(s),
)

private fun duplicateRows(s: Statistics): List<Pair<String, String>> = countRows(s) + if (s.games == 0) emptyList() else listOf(
  "Aciertos" to percent(s.hitRate),
  "Efectividad promedio" to percent(s.averageEfficiency),
  "Mejor efectividad" to percent(s.bestEfficiency),
  "Promedio de puntos" to "%.0f".format(s.averageScore),
  "Puntuación máxima" to "${s.bestScore}",
  "Scrabbles" to "${s.bingos}",
  "Palabra más valiosa" to bestWord(s),
)

private fun bestWord(s: Statistics): String = s.bestWord?.let { (word, points) -> "$word ($points)" } ?: "—"

private fun percent(x: Double): String = "%.1f %%".format(x)

private fun chartGame(g: FinishedGame): ChartGame = ChartGame(
  myScore = g.myScore, opponentScore = g.opponentScore,
  value = if (g.mode == Mode.CLASSIC) g.myScore else g.efficiency.toInt(),
  title = "Tú ${g.myScore} – ${g.opponentScore} ${alias(g.opponent)}", lines = gameLines(g),
)

private fun gameLines(g: FinishedGame): List<String> = listOfNotNull(
  outcomeText(g.outcome),
  if (g.mode == Mode.DUPLICATE) "Eficiencia: ${percent(g.efficiency)}" else null,
  if (g.mode == Mode.DUPLICATE) "Aciertos: ${g.hits} de ${g.turns}" else null,
  g.bestWord.takeIf { it.isNotEmpty() }?.let { "Palabra más valiosa: $it (${g.bestWordScore})" },
  if (g.bingos > 0) "Scrabbles: ${g.bingos}" else null,
  finishedAt(g),
)

private fun outcomeText(outcome: Outcome): String = when (outcome) {
  Outcome.WIN -> "Ganaste"
  Outcome.LOSS -> "Perdiste"
  Outcome.TIE -> "Empate"
}
