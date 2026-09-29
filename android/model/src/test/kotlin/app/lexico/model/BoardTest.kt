package app.lexico.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BoardTest {
  private fun p(s: String) = Position(s[0].uppercaseChar() - 'A', s.substring(1).toInt() - 1)

  @Test fun tilesWithDigraphsBlanksAndEnye() {
    assertEquals(listOf(Tile("CH"), Tile("I"), Tile("C"), null), Letters.tiles("CHIC."))
    assertEquals(listOf(Tile("C"), Tile("A"), Tile("S", true), Tile("A")), Letters.tiles("CAsA"))
    assertEquals(listOf(Tile("LL", true), Tile("Ñ"), Tile("Ñ", true)), Letters.tiles("ll[N][n]"))
    assertEquals(listOf(Tile("C"), Tile("H")), Letters.tiles("[C]H"))
    assertEquals(listOf(Tile("C"), null, Tile("A")), Letters.tiles("C(A)A"))
    assertEquals(listOf(Tile("RR")), Letters.tiles("[RR]"))
  }

  @Test fun parsesFiseCoordinates() {
    val h = Placement.parse("h8 CASA")
    assertEquals(Triple(7, 7, false), Triple(h.row, h.column, h.vertical))
    val v = Placement.parse("10e CHIC.")
    assertEquals(Triple(4, 9, true), Triple(v.row, v.column, v.vertical))
    assertEquals("10E [CH]IC.", v.toString())
    assertEquals(Placement.parse("h8 CASA"), Placement.parse("H8 CASA (12 pts)"))
    assertNull(Placement.parseOrNull("(Pasar)"))
    assertNull(Placement.parseOrNull("p8 CASA"))
    assertNull(Placement.parseOrNull("h16 CASA"))
  }

  @Test fun playPlacesAndHighlightsOnlyNewTiles() {
    val t = Board.of("h7 PERRO", "7h .ALO")
    assertEquals(Tile("RR"), t[p("H9")])
    assertEquals(Tile("O"), t[p("K7")])
    assertEquals(setOf(p("I7"), p("J7"), p("K7")), t.latest)
    assertEquals(7, t.tiles.size)
  }

  @Test fun playingThroughSameLetterIsAllowed() {
    val t = Board.of("h8 CASA").play("8h CERO")
    assertEquals(setOf(p("I8"), p("J8"), p("K8")), t.latest)
  }

  @Test fun placementErrors() {
    val t = Board.of("h8 CASA")
    assertThrows(IllegalArgumentException::class.java) { t.play("8h PERO") } // pisa la C
    assertThrows(IllegalArgumentException::class.java) { t.play("a1 .A") } // pasa por vacia
    assertThrows(IllegalArgumentException::class.java) { t.play("h13 CASAS") } // se sale
  }

  @Test fun moveListsSkipPassesAndExchanges() {
    val t = Board.EMPTY.playAll(listOf("h8 CASA (12 pts)", "(Pasar)", "(Cambiar ABC)", "8h .ERO (7 pts)"))
    assertEquals(Board.of("h8 CASA", "8h .ERO"), t)
  }

  @Test fun bonuses() {
    assertEquals(Bonus.TRIPLE_WORD, Board.bonus(p("A1")))
    assertEquals(Bonus.TRIPLE_WORD, Board.bonus(p("O15")))
    assertEquals(Bonus.CENTER, Board.bonus(p("H8")))
    assertEquals(Bonus.DOUBLE_LETTER, Board.bonus(p("A4")))
    assertEquals(Bonus.TRIPLE_LETTER, Board.bonus(p("B6")))
    assertEquals(Bonus.DOUBLE_WORD, Board.bonus(p("N14")))
    val counts = (0 until 15).flatMap { f -> (0 until 15).map { c -> Board.bonus(Position(f, c)) } }
      .groupingBy { it }.eachCount()
    assertEquals(8, counts[Bonus.TRIPLE_WORD])
    assertEquals(16, counts[Bonus.DOUBLE_WORD])
    assertEquals(12, counts[Bonus.TRIPLE_LETTER])
    assertEquals(24, counts[Bonus.DOUBLE_LETTER])
  }
}
