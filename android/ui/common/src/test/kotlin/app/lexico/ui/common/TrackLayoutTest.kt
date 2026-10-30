package app.lexico.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackLayoutTest {
  private fun runner(name: String, score: Int) = RaceRunner(name, score, done = false, present = true)

  private fun layout(vararg runners: RaceRunner, width: Float = 300f) =
    TrackLayout.of(runners.toList(), goal = 10, width = width, dot = 20f, gap = 6f) { it.length * 8f }

  @Test fun tiedPlayersShareOneDot() {
    val spots = layout(runner("Luis", 4), runner("Pepe", 4), runner("Ana", 6))
    assertEquals(listOf(2, 1), spots.map { it.runners.size })
    assertEquals("Luis, Pepe", spots[0].label)
  }

  @Test fun closeLabelsAlternateRows() {
    val spots = layout(runner("Eva", 2), runner("Ana", 3), runner("Luis", 4))
    assertEquals(3, spots.size)
    assertEquals(listOf(true, false, true), spots.map { it.above })
  }

  @Test fun labelsNeverOverlapInARow() {
    val names = listOf("Alejandro", "Bernardo", "Carolina", "Daniela", "Eugenio", "Fernanda", "Gonzalo")
    val spots = layout(*names.mapIndexed { i, n -> runner(n, i + 2) }.toTypedArray(), width = 250f)
    for (row in listOf(true, false)) {
      spots.filter { it.above == row }.zipWithNext().forEach { (a, b) ->
        assertTrue("${a.label} pisa a ${b.label}", a.labelX + a.label.length * 8f + 6f <= b.labelX)
      }
    }
    assertEquals(names.size, spots.sumOf { it.runners.size })
  }

  @Test fun dotsTooCloseAreMerged() {
    val spots = TrackLayout.of(listOf(runner("Ana", 10), runner("Eva", 11)), goal = 50, width = 300f, dot = 20f, gap = 6f) { 10f }
    assertEquals(1, spots.size)
  }
}
