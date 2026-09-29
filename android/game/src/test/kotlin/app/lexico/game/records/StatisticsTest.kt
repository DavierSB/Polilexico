package app.lexico.game.records

import app.lexico.game.Outcome
import app.lexico.game.storage.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticsTest {
  private fun game(me: Int, them: Int, outcome: Outcome, bingos: Int = 0, word: String = "", wordScore: Int = 0, hits: Int = 0, turns: Int = 0) =
    FinishedGame("p", Mode.CLASSIC, "", "HastyBot", me, them, outcome, hits = hits, bingos = bingos, bestWord = word, bestWordScore = wordScore, turns = turns)

  @Test fun winsPointsAndWords() {
    val s = Statistics.of(listOf(
      game(400, 300, Outcome.WIN, bingos = 1, word = "CASERO", wordScore = 70),
      game(300, 400, Outcome.LOSS, word = "ESTRELLAS", wordScore = 40),
      game(350, 350, Outcome.TIE),
    ))
    assertEquals(1, s.wins)
    assertEquals(350.0, s.averageScore, 0.001)
    assertEquals(400, s.bestScore)
    assertEquals(1, s.bingos)
    assertEquals("CASERO" to 70, s.bestWord)
    assertEquals(400.0 * 100 / 300, s.bestEfficiency, 0.001)
  }

  @Test fun hitRateIsOverAllTurns() {
    val s = Statistics.of(listOf(game(0, 0, Outcome.TIE, hits = 3, turns = 10), game(0, 0, Outcome.TIE, hits = 7, turns = 10)))
    assertEquals(50.0, s.hitRate, 0.001)
  }

  @Test fun noGames() {
    val s = Statistics.of(emptyList())
    assertEquals(0, s.games)
    assertNull(s.bestScore)
    assertNull(s.bestWord)
  }
}
