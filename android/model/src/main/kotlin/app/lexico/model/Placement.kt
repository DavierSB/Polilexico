package app.lexico.model

/**
 * Una jugada de colocacion en notacion FISE: "H8 CASA" horizontal (fila H, columna 8), "8H CASA"
 * vertical (columna 8, fila H). `tiles[i] == null` = se pasa por una letra que ya esta en el
 * tablero.
 */
data class Placement(
  val row: Int,
  val column: Int,
  val vertical: Boolean,
  val tiles: List<Tile?>,
) {
  init {
    require(tiles.isNotEmpty()) { "jugada sin fichas" }
  }

  /** Casillas que ocupa la palabra completa, en orden. */
  val positions: List<Position>
    get() = tiles.indices.map { i -> if (vertical) Position(row + i, column) else Position(row, column + i) }

  /** Solo las fichas nuevas, por casilla. */
  val placed: Map<Position, Tile>
    get() = tiles.zip(positions).mapNotNull { (tile, p) -> tile?.let { p to it } }.toMap()

  val coordinates: String
    get() = if (vertical) "${column + 1}${'A' + row}" else "${'A' + row}${column + 1}"

  /** "H8 CAsA", "8H .ALO", "10E [CH]IC.": se puede pasar tal cual al motor. */
  override fun toString(): String = coordinates + " " + tiles.joinToString("") { written(it) }

  /**
   * "H8 CASA" para mostrar: la palabra entera, con las letras de `board` en las casillas por las
   * que pasa (en vez de puntos) y los digrafos sin corchetes.
   */
  fun spelled(board: Board): String =
    coordinates + " " + tiles.zip(positions).joinToString("") { (tile, p) -> (tile ?: letterAt(board, p))?.toString() ?: "." }

  private fun letterAt(board: Board, p: Position): Tile? = if (p.onBoard) board[p] else null

  companion object {
    /**
     * Lee "h8 CASA", "8h .ALO", "H8 CA(S)A" o una linea como "h8 CASA (12 pts)" (lo que sigue a
     * la palabra se ignora). Lanza IllegalArgumentException si no es una jugada de colocacion.
     */
    fun parse(text: String): Placement {
      val parts = text.trim().split(Regex("\\s+"))
      require(parts.size >= 2) { "formato: COORDENADA PALABRA (ej: h8 CASA), no \"$text\"" }
      val (row, column, vertical) = Coordinates.parse(parts[0])
      return Placement(row, column, vertical, Letters.tiles(parts[1]))
    }

    /** Como [parse], pero `null` para pases, cambios o texto que no es una colocacion. */
    fun parseOrNull(text: String): Placement? = runCatching { parse(text) }.getOrNull()

    /** Una ficha como va en la notacion: "." si ya esta en el tablero, digrafos entre corchetes. */
    private fun written(tile: Tile?): String = when {
      tile == null -> "."
      tile.letter.length > 1 -> "[$tile]"
      else -> tile.toString()
    }
  }
}
