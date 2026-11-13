package app.lexico.game.modes.classic

import app.lexico.model.Board
import app.lexico.model.Letters
import app.lexico.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Test

class UnseenTest {
  @Test fun leavesOutTheBoardAndMyRack() {
    val board = Board.ofCells(List(Board.SIZE * Board.SIZE) { if (it == 112) Tile("T") else if (it == 113) Tile("A", blank = true) else null })
    val unseen = unseenBy(board, listOf("T", "T", "J"))
    assertEquals(1, unseen.count { it == "T" })
    assertEquals(0, unseen.count { it == "J" })
    assertEquals(1, unseen.count { it == Letters.BLANK })
    assertEquals(Letters.BAG.size - 5, unseen.size)
  }
}
