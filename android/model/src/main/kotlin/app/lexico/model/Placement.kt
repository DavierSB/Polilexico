package app.lexico.model

data class Placement(
  val row: Int,
  val column: Int,
  val vertical: Boolean,
  val tiles: List<Tile?>,
) {
  init {
    require(tiles.isNotEmpty()) { "jugada sin fichas" }
  }

  val positions: List<Position>
    get() = tiles.indices.map { i -> if (vertical) Position(row + i, column) else Position(row, column + i) }

  val placed: Map<Position, Tile>
    get() = tiles.zip(positions).mapNotNull { (tile, p) -> tile?.let { p to it } }.toMap()

  val coordinates: String
    get() = if (vertical) "${column + 1}${'A' + row}" else "${'A' + row}${column + 1}"

  override fun toString(): String = coordinates + " " + tiles.joinToString("") { written(it) }

  fun spelled(board: Board): String =
    coordinates + " " + tiles.zip(positions).joinToString("") { (tile, p) -> (tile ?: letterAt(board, p))?.toString() ?: "." }

  private fun letterAt(board: Board, p: Position): Tile? = if (p.onBoard) board[p] else null

  companion object {
    fun parse(text: String): Placement {
      val parts = text.trim().split(Regex("\\s+"))
      require(parts.size >= 2) { "formato: COORDENADA PALABRA (ej: h8 CASA), no \"$text\"" }
      val (row, column, vertical) = Coordinates.parse(parts[0])
      return Placement(row, column, vertical, Letters.tiles(parts[1]))
    }

    fun parseOrNull(text: String): Placement? = runCatching { parse(text) }.getOrNull()

    private fun written(tile: Tile?): String = when {
      tile == null -> "."
      tile.letter.length > 1 -> "[$tile]"
      else -> tile.toString()
    }
  }
}
