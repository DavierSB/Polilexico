package app.lexico.model

/**
 * Escribe como jugada las fichas nuevas puestas sobre un tablero ([Board.placementOf]): busca la
 * direccion, alarga la palabra con las letras pegadas y marca con un punto las del tablero.
 */
internal class PlacementWriter(private val board: Board, private val newTiles: Map<Position, Tile>) {
  private val squares = newTiles.keys

  fun write(): Placement? {
    if (squares.isEmpty() || squares.any { !it.onBoard || board[it] != null }) return null
    val vertical = isVertical() ?: return null
    return wordAlong(Line(vertical, squares.first()))
  }

  /** La direccion de las fichas; null si no estan en una sola fila ni en una sola columna. */
  private fun isVertical(): Boolean? = when {
    squares.size == 1 -> singleTileIsVertical(squares.first())
    squares.map { it.row }.toSet().size == 1 -> false
    squares.map { it.column }.toSet().size == 1 -> true
    else -> null
  }

  /** Una ficha sola se toma horizontal, salvo que solo tenga vecinas arriba o abajo. */
  private fun singleTileIsVertical(p: Position): Boolean =
    !(filled(p.row, p.column - 1) || filled(p.row, p.column + 1)) && (filled(p.row - 1, p.column) || filled(p.row + 1, p.column))

  /** La palabra completa a lo largo de `line`; null si queda algun hueco en medio. */
  private fun wordAlong(line: Line): Placement? {
    val start = extend(line, squares.minOf(line::indexOf), step = -1)
    val end = extend(line, squares.maxOf(line::indexOf), step = 1)
    if ((start..end).any { !occupied(line.at(it)) }) return null
    val origin = line.at(start)
    return Placement(origin.row, origin.column, line.vertical, (start..end).map { newTiles[line.at(it)] })
  }

  /** Avanza desde `from` mientras la casilla siguiente tenga letra. */
  private fun extend(line: Line, from: Int, step: Int): Int {
    var i = from
    while (occupied(line.at(i + step))) i += step
    return i
  }

  private fun occupied(p: Position): Boolean = p.onBoard && (board[p] != null || p in newTiles)

  private fun filled(row: Int, column: Int): Boolean = Position(row, column).let { it.onBoard && board[it] != null }

  /** Una fila (horizontal) o columna (vertical) del tablero, recorrida por indice. */
  private class Line(val vertical: Boolean, through: Position) {
    private val fixed = if (vertical) through.column else through.row

    fun at(i: Int): Position = if (vertical) Position(i, fixed) else Position(fixed, i)

    fun indexOf(p: Position): Int = if (vertical) p.row else p.column
  }
}
