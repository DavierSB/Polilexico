package app.lexico.model

class Board private constructor(
  private val cells: List<Tile?>,
  val latest: Set<Position>,
) {
  operator fun get(p: Position): Tile? = cells[index(p)]
  operator fun get(row: Int, column: Int): Tile? = get(Position(row, column))

  val isEmpty: Boolean get() = cells.all { it == null }

  val tiles: Map<Position, Tile>
    get() = cells.withIndex().filter { it.value != null }.associate { positionOf(it.index) to it.value!! }

  fun play(placement: Placement): Board {
    val next = cells.toMutableList()
    val placed = placement.tiles.zip(placement.positions).mapNotNull { (tile, p) -> place(next, placement, tile, p) }
    return Board(next, placed.toSet())
  }

  fun play(placement: String): Board = play(Placement.parse(placement))

  fun playAll(placements: Iterable<Placement>): Board = placements.fold(this) { board, p -> board.play(p) }

  @JvmName("playAllText")
  fun playAll(placements: Iterable<String>): Board = playAll(placements.mapNotNull { Placement.parseOrNull(it) })

  fun placementOf(newTiles: Map<Position, Tile>): Placement? = PlacementWriter(this, newTiles).write()

  fun withTile(p: Position, tile: Tile): Board = Board(cells.replaced(p, tile), latest - p)

  fun withoutTile(p: Position): Board = Board(cells.replaced(p, null), latest - p)

  fun withoutHighlight(): Board = if (latest.isEmpty()) this else Board(cells, emptySet())

  override fun equals(other: Any?): Boolean = other is Board && cells == other.cells && latest == other.latest

  override fun hashCode(): Int = 31 * cells.hashCode() + latest.hashCode()

  override fun toString(): String = (0 until SIZE).joinToString("\n") { row -> rowText(row) }

  private fun place(next: MutableList<Tile?>, placement: Placement, tile: Tile?, p: Position): Position? {
    require(p.onBoard) { "\"$placement\" se sale del tablero" }
    val current = next[index(p)]
    if (current == null) {
      require(tile != null) { "\"$placement\": $p esta vacia y la jugada pasa por ella" }
      next[index(p)] = tile
      return p
    }
    require(tile == null || current.letter == tile.letter) { "\"$placement\": $p ya tiene ${current.letter}" }
    return null
  }

  private fun rowText(row: Int): String =
    (0 until SIZE).joinToString(" ") { column -> this[row, column]?.toString()?.padEnd(2) ?: ". " }.trimEnd()

  private fun List<Tile?>.replaced(p: Position, tile: Tile?): List<Tile?> =
    toMutableList().also { it[index(p)] = tile }

  companion object {
    const val SIZE = 15

    val EMPTY = Board(List(SIZE * SIZE) { null }, emptySet())

    fun of(vararg placements: String): Board = EMPTY.playAll(placements.asList())

    fun ofCells(cells: List<Tile?>, latest: Set<Position> = emptySet()): Board {
      require(cells.size == SIZE * SIZE) { "un tablero tiene ${SIZE * SIZE} casillas, no ${cells.size}" }
      return Board(cells, latest)
    }

    fun bonus(p: Position): Bonus = BonusLayout.at(index(p))

    private fun index(p: Position): Int {
      require(p.onBoard) { "casilla fuera del tablero: $p" }
      return p.row * SIZE + p.column
    }

    private fun positionOf(index: Int) = Position(index / SIZE, index % SIZE)
  }
}
