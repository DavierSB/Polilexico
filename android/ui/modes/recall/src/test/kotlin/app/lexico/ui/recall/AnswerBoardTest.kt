package app.lexico.ui.recall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerBoardTest {
  private fun word(tiles: String, fixed: Set<Int>, rack: String) =
    WordToRecall(tiles.map { it.toString() }, 10, fixed, rack.map { it.toString() })

  @Test fun placesClearsAndResets() {
    var b = AnswerBoard.start(word("LOBO", emptySet(), "OLBO"))
    b = b.place(1).place(0).place(0) // la O del pool 0 no se pone dos veces
    assertEquals(listOf("L", "O", null, null), b.letters)
    b = b.place(2).place(3)
    assertTrue(b.isComplete)
    assertEquals(listOf("L", "O", "B", "O"), b.letters)
    b = b.clear(0)
    assertFalse(b.isComplete)
    assertEquals(setOf(0, 2, 3), b.used)
    assertEquals(listOf("L", "O", "B", "O"), b.place(1).letters)
    assertEquals(AnswerBoard.start(word("LOBO", emptySet(), "OLBO")), b.reset())
  }

  @Test fun fixedTilesStayAndAreSkipped() {
    var b = AnswerBoard.start(word("LOBO", setOf(1, 3), "BL"))
    assertEquals(listOf(null, "O", null, "O"), b.letters)
    b = b.place(1).place(0) // L en la casilla 0 y B en la 2, saltando la O fija
    assertEquals(listOf("L", "O", "B", "O"), b.letters)
    assertTrue(b.isComplete)
    assertEquals(b, b.clear(1))
    assertEquals(listOf(null, "O", null, "O"), b.reset().letters)
  }
}
