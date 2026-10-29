package app.lexico.game.records

import app.lexico.game.Outcome

data class Statistics(
  val games: Int,
  val abandoned: Int,
  val inProgress: Int,
  val wins: Int,
  val averageScore: Double,
  val bestScore: Int?,
  val bingos: Int,
  val bestWord: Pair<String, Int>?,
  val hitRate: Double,
  val averageEfficiency: Double,
  val bestEfficiency: Double,
) {
  val started: Int get() = games + abandoned + inProgress

  val winRate: Double get() = if (games + abandoned > 0) wins * 100.0 / (games + abandoned) else 0.0

  companion object {
    fun of(games: List<FinishedGame>, abandoned: Int = 0, inProgress: Int = 0): Statistics = Statistics(
      games = games.size, abandoned = abandoned, inProgress = inProgress, wins = games.count { it.outcome == Outcome.WIN },
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
