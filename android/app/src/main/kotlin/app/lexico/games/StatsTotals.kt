package app.lexico.games

import app.lexico.game.records.Statistics
import app.lexico.online.OnlineStatsRow

internal data class Totals(
  val games: Int, val decided: Int, val wins: Int, val averageScore: Double, val averageEfficiency: Double?, val bestPlay: Int,
) {
  fun classicRows(): List<Pair<String, String>> = listOf(
    "Partidas" to "$games",
    "Victorias" to "$wins  (%.0f %%)".format(if (decided > 0) wins * 100.0 / decided else 0.0),
    "Promedio de puntos" to "%.0f".format(averageScore),
    "Mejor jugada" to bestText(),
  )

  fun duplicateRows(): List<Pair<String, String>> = listOf(
    "Partidas" to "$games",
    "Efectividad promedio" to (averageEfficiency?.let { "%.1f %%".format(it) } ?: "—"),
    "Promedio de puntos" to "%.0f".format(averageScore),
    "Mejor jugada" to bestText(),
  )

  private fun bestText(): String = if (bestPlay > 0) "$bestPlay" else "—"

  companion object {
    fun of(offline: Statistics, online: OnlineStatsRow?): Totals {
      val onlineGames = online?.games ?: 0
      return Totals(
        games = offline.games + onlineGames,
        decided = offline.games + offline.abandoned + onlineGames,
        wins = offline.wins + (online?.wins ?: 0),
        averageScore = weighted(offline.averageScore to offline.games, (online?.avgScore ?: 0.0) to onlineGames),
        averageEfficiency = efficiency(offline, online),
        bestPlay = maxOf(offline.bestWord?.second ?: 0, online?.bestPlay ?: 0),
      )
    }

    private fun efficiency(offline: Statistics, online: OnlineStatsRow?): Double? {
      val parts = listOfNotNull(
        (offline.averageEfficiency to offline.games).takeIf { offline.games > 0 },
        online?.avgEfficiency?.let { (it * 100) to online.games },
      )
      return if (parts.isEmpty()) null else weighted(*parts.toTypedArray())
    }

    private fun weighted(vararg parts: Pair<Double, Int>): Double {
      val count = parts.sumOf { it.second }
      return if (count == 0) 0.0 else parts.sumOf { it.first * it.second } / count
    }
  }
}
