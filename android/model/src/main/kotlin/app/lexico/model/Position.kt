package app.lexico.model

/** Casilla del tablero: fila 0-14 (A-O) y columna 0-14 (1-15). */
data class Position(val row: Int, val column: Int) {
  val onBoard: Boolean get() = row in 0 until Board.SIZE && column in 0 until Board.SIZE

  /** "H8" */
  override fun toString(): String = "${'A' + row}${column + 1}"
}
