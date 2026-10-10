package app.lexico.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LettersTest {
  @Test fun bagHoldsTheHundredSpanishTiles() {
    assertEquals(100, Letters.BAG.size)
    assertEquals(2, Letters.BAG.count { it == Letters.BLANK })
    assertEquals(12, Letters.BAG.count { it == "A" })
    assertEquals(1, Letters.BAG.count { it == "RR" })
  }
}
