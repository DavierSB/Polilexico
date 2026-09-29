package app.lexico.ui.board

import app.lexico.model.Position
import app.lexico.model.Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TilePlacerTest {
  private fun p(s: String) = Position(s[0].uppercaseChar() - 'A', s.substring(1).toInt() - 1)

  /** El atril nuevo sale barajado; aqui se pone en el orden del motor para poder comprobarlo. */
  private fun placer() = TilePlacer().apply {
    reset(Board.of("h8 CASA"), listOf("E", "R", "O", "?"))
    order.clear()
    order.addAll(tiles.indices)
  }

  @Test fun arrowPlacesTilesSkippingOccupied() {
    val c = placer()
    c.tapBoard(p("G8"))
    c.tapBoard(p("G8")) // segundo toque: vertical
    c.tapRack(0) // E en G8; la flecha salta la C de H8
    c.tapRack(1) // R en I8
    assertEquals("8G E.R", c.placement().toString()) // G8 E, H8 C (ya estaba), I8 R
  }

  @Test fun selectThenTapSquare() {
    val c = placer()
    c.tapRack(2)
    c.tapBoard(p("I8"))
    assertEquals("8H .O", c.placement().toString())
    // Tocarla otra vez la devuelve al atril.
    c.tapBoard(p("I8"))
    assertFalse(c.hasPlaced)
    assertNull(c.placement())
  }

  @Test fun blankAsksForLetter() {
    val c = placer()
    c.tapRack(3)
    c.tapBoard(p("I8"))
    assertEquals(p("I8"), c.pendingBlank)
    c.placeBlank("E")
    assertNull(c.pendingBlank)
    assertEquals("8H .e", c.placement().toString())
  }

  @Test fun tappingTwoTilesSwapsThem() {
    val c = placer()
    c.tapRack(0)
    c.tapRack(2)
    assertEquals(listOf(2, 1, 0, 3), c.order.toList())
  }

  @Test fun exchangeModeMarksTiles() {
    val c = placer()
    c.startExchange()
    c.tapRack(1)
    c.tapRack(3)
    assertTrue(c.exchanging)
    assertEquals(listOf("R", "?"), c.tilesToExchange())
  }

  @Test fun resetRecallsEverything() {
    val c = placer()
    c.tapRack(0)
    c.tapBoard(p("I8"))
    c.reset(Board.of("h8 CASA", "8h .ERO"), listOf("A", "B"))
    assertFalse(c.hasPlaced)
    assertEquals(setOf(0, 1), c.order.toSet())
  }

  @Test fun dropFromRackPlacesTile() {
    val c = placer()
    c.dropOnBoard(2, p("I8"))
    assertEquals("8H .O", c.placement().toString())
    c.dropOnBoard(0, p("I8")) // ocupada: no pasa nada
    c.dropOnBoard(1, p("H8")) // ocupada en el tablero
    assertEquals(mapOf(p("I8") to 2), c.placed.toMap())
  }

  @Test fun droppedBlankAsksForLetter() {
    val c = placer()
    c.dropOnBoard(3, p("I8"))
    assertEquals(p("I8"), c.pendingBlank)
  }

  @Test fun moveOnBoardKeepsBlankLetter() {
    val c = placer()
    c.dropOnBoard(3, p("I8"))
    c.placeBlank("E")
    c.moveOnBoard(p("I8"), p("G8"))
    assertEquals("8G e.", c.placement().toString())
    assertEquals(mapOf(p("G8") to 3), c.placed.toMap())
  }

  @Test fun dropOnRackReordersAndTakesBack() {
    val c = placer()
    c.dropOnRack(3, 0)
    assertEquals(listOf(3, 0, 1, 2), c.order.toList())
    c.dropOnBoard(1, p("I8"))
    c.dropOnRack(1, 4)
    assertFalse(c.hasPlaced)
    assertEquals(listOf(3, 0, 2, 1), c.order.toList())
  }
}
