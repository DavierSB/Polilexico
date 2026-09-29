package app.lexico.games

import app.lexico.game.Outcome
import app.lexico.game.records.FinishedGame
import app.lexico.game.records.Statistics
import app.lexico.game.storage.Mode
import app.lexico.ui.classic.alias
import app.lexico.ui.games.ChartGame
import app.lexico.ui.games.StatsFilter
import app.lexico.ui.games.StatsMode
import app.lexico.ui.games.StatsPage

/*
 * Las estadisticas de las partidas terminadas, como las muestra la pantalla de :ui:games.
 */

private const val EFFICIENCY_NOTE =
  "Efectividad: tus puntos como porcentaje de los del máster. Acierto: un turno con el mismo puntaje que el máster."

/** Lo que se ve para un filtro. `reset`: si las estadisticas se reiniciaron (cambia el aviso sin partidas). */
fun statsPage(filter: StatsFilter, games: List<FinishedGame>, reset: Boolean): StatsPage {
  val chosen = games.filter { it.mode == mode(filter.mode) && (filter.opponent == null || alias(it.opponent) == filter.opponent) }
  val empty = emptyText(filter.opponent, reset)
  return when {
    chosen.isEmpty() -> StatsPage(emptyList(), empty, null, emptyList())
    filter.mode == StatsMode.CLASSIC -> StatsPage(classicRows(Statistics.of(chosen)), empty, null, chosen.map(::chartGame))
    else -> StatsPage(duplicateRows(Statistics.of(chosen)), empty, EFFICIENCY_NOTE, chosen.map(::chartGame))
  }
}

private fun mode(mode: StatsMode): Mode = if (mode == StatsMode.CLASSIC) Mode.CLASSIC else Mode.DUPLICATE

private fun emptyText(opponent: String?, reset: Boolean): String = when {
  reset -> "Aún no hay partidas terminadas desde que reiniciaste las estadísticas."
  opponent != null -> "Aún no hay partidas terminadas contra $opponent."
  else -> "Aún no hay partidas terminadas."
}

private fun classicRows(s: Statistics): List<Pair<String, String>> = listOf(
  "Partidas" to "${s.games}",
  "Victorias" to "${s.wins}  (${s.wins * 100 / s.games} %)",
  "Promedio de puntos" to "%.0f".format(s.averageScore),
  "Puntuación máxima" to "${s.bestScore}",
  "Scrabbles" to "${s.bingos}  (%.1f por partida)".format(s.bingos.toDouble() / s.games),
  "Palabra más valiosa" to bestWord(s),
)

private fun duplicateRows(s: Statistics): List<Pair<String, String>> = listOf(
  "Partidas" to "${s.games}",
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

/** Una partida en las graficas: el valor de la tira es tus puntos (clasica) o la eficiencia (duplicada). */
private fun chartGame(g: FinishedGame): ChartGame = ChartGame(
  myScore = g.myScore, opponentScore = g.opponentScore,
  value = if (g.mode == Mode.CLASSIC) g.myScore else g.efficiency.toInt(),
  title = "Tú ${g.myScore} – ${g.opponentScore} ${alias(g.opponent)}", lines = gameLines(g),
)

/** Lo que se ve al tocarla: resultado, eficiencia y aciertos (duplicada), palabra, scrabbles y fecha. */
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
