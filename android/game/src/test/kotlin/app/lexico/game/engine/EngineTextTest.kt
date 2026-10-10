package app.lexico.game.engine

import app.lexico.model.Board
import app.lexico.model.Position
import app.lexico.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Test

class EngineTextTest {
  private fun p(s: String) = Position(s[0] - 'A', s.substring(1).toInt() - 1)

  @Test fun boardGoesAndComesBack() {
    val board = Board.of("h8 CAsA", "8h .[CH]E")
    val text = boardText(board)
    assertEquals(225, text.split(" ").size)
    assertEquals(board.tiles, parseBoard(text).tiles)
    assertEquals(Tile("S", blank = true), parseBoard(text)[p("H10")])
    assertEquals(Tile("CH"), parseBoard(text)[p("I8")])
  }

  @Test fun racks() {
    assertEquals(listOf("A", "CH", "E", "?"), rackTiles("A CH E ?"))
    assertEquals("A[CH]E?", rackText(listOf("A", "CH", "E", "?")))
    assertEquals(emptyList<String>(), rackTiles(""))
  }

  @Test fun placedSquaresOfAMove() {
    assertEquals(setOf(p("I8"), p("J8")), placedSquares("8H", ".ER"))
    assertEquals(emptySet<Position>(), placedSquares("", ""))
  }

  @Test fun movesShowTheWholeWord() {
    val board = Board.of("h8 CASA", "8h .[CH]E")
    assertEquals("8H CCHE", moveText(board, "8H .[CH]E (20 pts)"))
    assertEquals("H8 CASAS", moveText(board, "H8 ....S"))
    assertEquals("(Pasar)", moveText(board, "(Pasar)"))
    assertEquals("H8 CASAS", moveText(board, "(Inválida H8 ....S: pierde el turno)"))
    assertEquals("A[CH]E".let(::plainTiles), moveText(board, " A[CH]E"))
  }

  @Test fun candidatesAndDemoGames() {
    val candidates = parseCandidates("""[{"coords":"H8","description":"H8 CASA (12 pts)","score":12,"equity":15.5},{"coords":"","description":"(Pasar)","score":0,"equity":-3}]""")
    assertEquals(EngineCandidate("H8", "H8 CASA (12 pts)", 12, 15.5), candidates[0])
    assertEquals(2, candidates.size)
    assertEquals(listOf(EnginePlacement("H8 CA.A", 12)), parseScoredPlacements("""[{"placement":"H8 CA.A","score":12}]"""))
  }
}
