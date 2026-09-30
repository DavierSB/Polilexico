package app.lexico.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlacementOfTest {
  private fun p(s: String) = Position(s[0].uppercaseChar() - 'A', s.substring(1).toInt() - 1)
  private val boardWithCasa = Board.of("h8 CASA")

  @Test fun goesThroughBoardLetters() {
    val j = boardWithCasa.placementOf(mapOf(p("I8") to Tile("E"), p("J8") to Tile("R"), p("K8") to Tile("O")))
    assertEquals("8H .ERO", j.toString())
  }

  @Test fun extendsWithAdjacentLetters() {
    assertEquals("H8 ....S", boardWithCasa.placementOf(mapOf(p("H12") to Tile("S"))).toString())
  }

  @Test fun singleTileWithOnlyUpperNeighborIsVertical() {
    assertEquals("8H .E", boardWithCasa.placementOf(mapOf(p("I8") to Tile("E"))).toString())
  }

  @Test fun spelledFillsBoardLetters() {
    val board = Board.of("h8 [CH]AsA")
    assertEquals("8H CHERO", Placement.parse("8h .ERO").spelled(board))
    assertEquals("H8 CHAsAS", Placement.parse("h8 ....S").spelled(board))
  }

  @Test fun digraphsAndBlanks() {
    val j = Board.EMPTY.placementOf(mapOf(p("H8") to Tile("CH"), p("H9") to Tile("E", blank = true)))
    assertEquals("H8 [CH]e", j.toString())
  }

  @Test fun noContinuousLineNoPlacement() {
    assertNull(boardWithCasa.placementOf(mapOf(p("I8") to Tile("E"), p("K8") to Tile("O"))))
    assertNull(boardWithCasa.placementOf(mapOf(p("I8") to Tile("E"), p("J9") to Tile("O"))))
    assertNull(boardWithCasa.placementOf(mapOf(p("H8") to Tile("E"))))
    assertNull(boardWithCasa.placementOf(emptyMap()))
  }
}
