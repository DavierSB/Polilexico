package app.lexico.game.records

import app.lexico.game.Outcome

/**
 * Tus numeros en un conjunto de partidas terminadas (las de una modalidad, o contra un rival):
 * victorias, puntos, scrabbles y tu palabra mas valiosa; en duplicada, aciertos y eficiencia.
 */
data class Statistics(
  val games: Int,
  val wins: Int,
  val averageScore: Double,
  val bestScore: Int?,
  val bingos: Int,
  /** Tu jugada que mas puntos dio: la palabra completa y sus puntos; null si no hay. */
  val bestWord: Pair<String, Int>?,
  /** Duplicada: % de turnos en que hiciste los mismos puntos que el master, sobre todos. */
  val hitRate: Double,
  /** Tus puntos sobre los del rival (en duplicada, el master), en %: la media y la mejor. */
  val averageEfficiency: Double,
  val bestEfficiency: Double,
) {
  companion object {
    fun of(games: List<FinishedGame>): Statistics = Statistics(
      games = games.size, wins = games.count { it.outcome == Outcome.WIN },
      averageScore = games.averageOf { it.myScore.toDouble() }, bestScore = games.maxOfOrNull { it.myScore },
      bingos = games.sumOf { it.bingos }, bestWord = bestWord(games), hitRate = hitRate(games),
      averageEfficiency = games.averageOf { it.efficiency }, bestEfficiency = games.maxOfOrNull { it.efficiency } ?: 0.0,
    )

    private fun List<FinishedGame>.averageOf(value: (FinishedGame) -> Double): Double = if (isEmpty()) 0.0 else sumOf(value) / size

    private fun bestWord(games: List<FinishedGame>): Pair<String, Int>? =
      games.filter { it.bestWord.isNotEmpty() }.maxByOrNull { it.bestWordScore }?.let { it.bestWord to it.bestWordScore }

    private fun hitRate(games: List<FinishedGame>): Double {
      val turns = games.sumOf { it.turns }
      return if (turns > 0) games.sumOf { it.hits } * 100.0 / turns else 0.0
    }
  }
}
