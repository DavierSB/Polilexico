package app.lexico.ui.board

import app.lexico.model.Board
import app.lexico.model.Position
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class RackOrderTest {
  @Test
  fun stayingTilesKeepTheirOrderAndDrawnOnesGoBehind() {
    val rack = listOf("A", "X", "C", "E", "B")
    val order = arrangeRack(listOf("C", "A", "B"), rack, Random(1))
    assertEquals(listOf("C", "A", "B"), order.take(3).map { rack[it] })
    assertEquals(setOf("X", "E"), order.drop(3).map { rack[it] }.toSet())
  }

  @Test
  fun repeatedLettersAreMatchedOnce() {
    val rack = listOf("A", "A", "B")
    assertEquals(listOf("A", "B", "A"), arrangeRack(listOf("A", "B"), rack).map { rack[it] })
  }

  @Test
  fun inClassicPlayedTilesLeaveAndANewCopyGoesLast() {
    val c = placerWith(RackRenewal.MY_PLAYS, listOf("A", "B", "C"))
    playFirst(c, "A")
    c.reset(Board.of("h8 A"), listOf("C", "A", "B"))
    assertEquals(listOf("B", "C", "A"), screen(c))
  }

  @Test
  fun inClassicExchangedTilesLeave() {
    val c = placerWith(RackRenewal.MY_PLAYS, listOf("A", "B", "C"))
    c.startExchange()
    c.tapRack(1)
    assertEquals(listOf("B"), c.confirmExchange())
    c.reset(Board.EMPTY, listOf("B", "C", "A"))
    assertEquals(listOf("A", "C", "B"), screen(c))
  }

  @Test
  fun inDuplicateMyPlacedTilesStay() {
    val c = placerWith(RackRenewal.MASTER_PLAYS, listOf("A", "B", "C"))
    playFirst(c, "A")
    c.reset(Board.of("h8 C"), listOf("E", "B", "A"))
    assertEquals(listOf("A", "B", "E"), screen(c))
  }

  @Test
  fun inDuplicateTheOrderSurvivesTheWaitBetweenRounds() {
    val c = placerWith(RackRenewal.MASTER_PLAYS, listOf("A", "B", "C"))
    c.reset(Board.EMPTY, emptyList())
    c.reset(Board.of("h8 B"), listOf("E", "C", "A"))
    assertEquals(listOf("A", "C", "E"), screen(c))
  }

  private fun placerWith(renewal: RackRenewal, screen: List<String>) = TilePlacer(renewal).apply {
    reset(Board.EMPTY, screen)
    order.clear()
    order.addAll(screen.indices)
  }

  private fun playFirst(c: TilePlacer, letter: String) {
    c.tapBoard(Position(7, 7))
    c.tapRack(c.tiles.indexOf(letter))
  }

  private fun screen(c: TilePlacer): List<String> = c.order.map { c.tiles[it] }
}
