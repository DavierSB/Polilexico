package app.lexico.game.engine

import app.lexico.model.Board
import app.lexico.model.Placement
import app.lexico.model.Position
import app.lexico.model.Tile

/*
 * Los formatos de texto del motor y su traduccion a los tipos de :model.
 */

/** Las 225 casillas del motor ("." vacia, minuscula = comodin) como tablero, con `latest` resaltadas. */
internal fun parseBoard(text: String, latest: Set<Position> = emptySet()): Board =
  Board.ofCells(text.trim().split(Regex("\\s+")).map(::parseSquare), latest)

/** El tablero en el formato que lee el motor (engine.BestMoves). */
internal fun boardText(board: Board): String =
  (0 until Board.SIZE * Board.SIZE).joinToString(" ") { board[it / Board.SIZE, it % Board.SIZE]?.toString() ?: "." }

/** "A CH E ?" -> ["A", "CH", "E", "?"]. */
internal fun rackTiles(text: String): List<String> = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

/** ["A", "CH", "?"] -> "A[CH]?", el atril como lo lee el motor. */
internal fun rackText(rack: List<String>): String = rack.joinToString("") { if (it.length > 1) "[$it]" else it }

/**
 * Una jugada del motor ("H8 CA.A (12 pts)", "(Pasar)", fichas cambiadas...) para mostrar: las
 * colocaciones con la palabra entera, tomando de `board` las letras por las que pasan. Como las
 * fichas no se quitan del tablero, vale el tablero actual tambien para jugadas pasadas.
 */
internal fun moveText(board: Board, move: String): String =
  Placement.parseOrNull(move)?.spelled(board) ?: plainTiles(move.substringBefore(" (").trim())

/** "CA.A" o "[CH]E" sin los corchetes de los digrafos, para mostrar. */
internal fun plainTiles(tiles: String): String = tiles.replace("[", "").replace("]", "")

/** Las casillas nuevas de una colocacion ("H8", "CA.A"); vacio si no es una colocacion. */
internal fun placedSquares(coords: String, tiles: String): Set<Position> =
  Placement.parseOrNull("$coords $tiles")?.placed?.keys.orEmpty()

/** "." = vacia; minuscula = comodin. */
private fun parseSquare(square: String): Tile? =
  if (square == ".") null else Tile(square.uppercase(), blank = square != square.uppercase())
