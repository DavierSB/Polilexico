package app.lexico.game.engine

import app.lexico.model.Board
import app.lexico.model.Placement
import app.lexico.model.Position
import app.lexico.model.Tile

internal fun parseBoard(text: String, latest: Set<Position> = emptySet()): Board =
  Board.ofCells(text.trim().split(Regex("\\s+")).map(::parseSquare), latest)

internal fun boardText(board: Board): String =
  (0 until Board.SIZE * Board.SIZE).joinToString(" ") { board[it / Board.SIZE, it % Board.SIZE]?.toString() ?: "." }

internal fun rackTiles(text: String): List<String> = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

internal fun rackText(rack: List<String>): String = rack.joinToString("") { if (it.length > 1) "[$it]" else it }

private val INVALID = Regex("""^\(Inválida (\S+ \S+):.*\)$""")

internal fun moveText(board: Board, move: String): String =
  Placement.parseOrNull(attempted(move))?.spelled(board) ?: plainTiles(move.substringBefore(" (").trim())

internal fun attempted(move: String): String = INVALID.find(move.trim())?.groupValues?.get(1) ?: move

internal fun plainTiles(tiles: String): String = tiles.replace("[", "").replace("]", "")

internal fun placedSquares(coords: String, tiles: String): Set<Position> =
  Placement.parseOrNull("$coords $tiles")?.placed?.keys.orEmpty()

private fun parseSquare(square: String): Tile? =
  if (square == ".") null else Tile(square.uppercase(), blank = square != square.uppercase())
