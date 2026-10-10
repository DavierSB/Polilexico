package app.lexico.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EfficiencyTest {
  private val e = Efficiency.of(mine = listOf(10, 0, 5), master = listOf(10, 0, 10))

  @Test fun eachTurnIsTheShareOfTheMasterPoints() {
    assertEquals(listOf(100f, null, 50f), e.turns)
  }

  @Test fun runningEfficiencySkipsTurnsWithoutPoints() {
    assertEquals(listOf(100f, 100f, 75f), e.running)
  }

  @Test fun onlyFullTurnsAreHits() {
    assertTrue(e.isHit(0))
    assertFalse(e.isHit(1))
    assertFalse(e.isHit(2))
  }
}
