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
import app.lexico.ui.games.StatsSource
import app.lexico.online.OnlineStatsRow

private const val EFFICIENCY_NOTE =
  "Efectividad: tus puntos como porcentaje de los del máster. Acierto: un turno con el mismo puntaje que el máster."

data class StatsGames(val finished: List<FinishedGame>, val abandoned: List<UnfinishedGame>, val inProgress: List<UnfinishedGame>)

fun statsPage(filter: StatsFilter, games: StatsGames, online: Result<List<OnlineStatsRow>>?, reset: Boolean): StatsPage = when (filter.source) {
  StatsSource.OFFLINE -> offlinePage(filter.mode, games, reset)
  StatsSource.ONLINE -> onlinePage(filter.mode, online)
  StatsSource.ALL -> allPage(filter.mode, offlineStats(filter.mode, games), online?.getOrNull()?.let { rowFor(filter.mode, it) }, reset)
}

private fun offlinePage(mode: StatsMode, games: StatsGames, reset: Boolean): StatsPage {
  val chosen = games.finished.filter { chosen(mode, it.mode, it.opponent) }
  val stats = offlineStats(mode, games)
  val empty = emptyText(reset)
  return when {
    stats.started == 0 -> StatsPage(emptyList(), empty, null, emptyList())
    mode == StatsMode.CLASSIC -> StatsPage(classicRows(stats), empty, null, chosen.map(::chartGame))
    else -> StatsPage(duplicateRows(stats), empty, EFFICIENCY_NOTE.takeIf { stats.games > 0 }, chosen.map(::chartGame))
  }
}

private fun offlineStats(mode: StatsMode, games: StatsGames): Statistics = Statistics.of(
  games.finished.filter { chosen(mode, it.mode, it.opponent) },
  games.abandoned.count { chosen(mode, it.mode, it.opponent) }, games.inProgress.count { chosen(mode, it.mode, it.opponent) },
)

private fun onlinePage(mode: StatsMode, online: Result<List<OnlineStatsRow>>?): StatsPage {
  val rows = online?.getOrElse { return emptyPage(it.message ?: NO_CONNECTION) } ?: return emptyPage("Cargando…")
  val row = rowFor(mode, rows) ?: return emptyPage("Aún no hay partidas en línea.")
  return StatsPage(onlineRows(row), "", null, emptyList())
}

private fun allPage(mode: StatsMode, offline: Statistics, online: OnlineStatsRow?, reset: Boolean): StatsPage {
  val total = Totals.of(offline, online)
  if (total.games == 0 && offline.started == 0) return emptyPage(emptyText(reset))
  return StatsPage(if (mode == StatsMode.CLASSIC) total.classicRows() else total.duplicateRows(), "", null, emptyList())
}

private fun emptyPage(text: String): StatsPage = StatsPage(emptyList(), text, null, emptyList())

private fun rowFor(mode: StatsMode, rows: List<OnlineStatsRow>): OnlineStatsRow? =
  rows.find { it.mode == if (mode == StatsMode.CLASSIC) "classic" else "duplicate" }?.takeIf { it.games > 0 }

private fun chosen(filter: StatsMode, mode: Mode, opponent: String): Boolean =
  mode == mode(filter) && (mode != Mode.CLASSIC || isBot(opponent))

private fun mode(mode: StatsMode): Mode = if (mode == StatsMode.CLASSIC) Mode.CLASSIC else Mode.DUPLICATE

private fun emptyText(reset: Boolean): String =
  if (reset) "Aún no hay partidas desde que reiniciaste las estadísticas." else "Aún no hay partidas."

const val NO_PROFILE = "Sin perfil en línea."

private const val NO_CONNECTION = "Sin conexión."

fun onlineRows(row: OnlineStatsRow): List<Pair<String, String>> = if (row.mode == "duplicate") onlineDuplicateRows(row) else onlineClassicRows(row)

private fun onlineClassicRows(row: OnlineStatsRow): List<Pair<String, String>> = listOf(
  "Partidas" to "${row.games}",
  "Victorias" to "${row.wins}  (%.0f %%)".format(100.0 * row.wins / row.games.coerceAtLeast(1)),
  "Derrotas" to "${row.losses}",
  "Empates" to "${row.draws}",
  "Promedio de puntos" to "%.0f".format(row.avgScore),
  "Mayor scrabble" to if (row.bestBingo > 0) "${row.bestBingo}" else "—",
  "Mejor jugada" to "${row.bestPlay}",
)

private fun onlineDuplicateRows(row: OnlineStatsRow): List<Pair<String, String>> = listOf(
  "Partidas" to "${row.games}",
  "Primeros puestos" to "${row.wins}",
  "Efectividad promedio" to (row.avgEfficiency?.let { percent(it * 100) } ?: "—"),
  "Mayor scrabble" to if (row.bestBingo > 0) "${row.bestBingo}" else "—",
  "Mejor jugada" to "${row.bestPlay}",
)

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
