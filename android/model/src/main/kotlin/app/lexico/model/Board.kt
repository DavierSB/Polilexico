package app.lexico.model

/**
 * Tablero de 15x15, inmutable: cada operacion devuelve un tablero nuevo, asi que sirve directo
 * como estado de Compose. `latest` son las casillas que puso la ultima jugada (para resaltarlas).
 *
 * No valida palabras ni conexion con el resto: eso es cosa del motor. Solo comprueba que la
 * jugada quepa y que no pise letras distintas.
 */
class Board private constructor(
  private val cells: List<Tile?>,
  val latest: Set<Position>,
) {
  operator fun get(p: Position): Tile? = cells[index(p)]
  operator fun get(row: Int, column: Int): Tile? = get(Position(row, column))

  val isEmpty: Boolean get() = cells.all { it == null }

  /** Fichas colocadas, por casilla. */
  val tiles: Map<Position, Tile>
    get() = cells.withIndex().filter { it.value != null }.associate { positionOf(it.index) to it.value!! }

  /**
   * Coloca una jugada. Un `null` en la jugada exige que la casilla ya tenga ficha; una letra sobre
   * una casilla ocupada vale si es la misma letra (se toma como "pasar por ella").
   */
  fun play(placement: Placement): Board {
    val next = cells.toMutableList()
    val placed = placement.tiles.zip(placement.positions).mapNotNull { (tile, p) -> place(next, placement, tile, p) }
    return Board(next, placed.toSet())
  }

  fun play(placement: String): Board = play(Placement.parse(placement))

  /** Coloca las jugadas una tras otra y devuelve el tablero final. */
  fun playAll(placements: Iterable<Placement>): Board = placements.fold(this) { board, p -> board.play(p) }

  /** Igual, a partir de texto. Pases, cambios y lineas que no son colocaciones se saltan. */
  @JvmName("playAllText")
  fun playAll(placements: Iterable<String>): Board = playAll(placements.mapNotNull { Placement.parseOrNull(it) })

  /**
   * Las fichas `newTiles` (aun no puestas) escritas como jugada: "H8 CA.A", con un punto por cada
   * letra del tablero que atraviesa la palabra. `null` si no forman una sola linea continua.
   * Solo escribe la jugada; si es valida (conexion, palabras, puntos) lo decide el motor.
   */
  fun placementOf(newTiles: Map<Position, Tile>): Placement? = PlacementWriter(this, newTiles).write()

  /** Pone (o cambia) una ficha suelta. */
  fun withTile(p: Position, tile: Tile): Board = Board(cells.replaced(p, tile), latest - p)

  /** Quita la ficha de una casilla (si la hay). */
  fun withoutTile(p: Position): Board = Board(cells.replaced(p, null), latest - p)

  /** Mismo tablero sin resaltar la ultima jugada. */
  fun withoutHighlight(): Board = if (latest.isEmpty()) this else Board(cells, emptySet())

  override fun equals(other: Any?): Boolean = other is Board && cells == other.cells && latest == other.latest

  override fun hashCode(): Int = 31 * cells.hashCode() + latest.hashCode()

  /** Dibujo en texto, util en pruebas y registros. */
  override fun toString(): String = (0 until SIZE).joinToString("\n") { row -> rowText(row) }

  /** Pone `tile` en `p` dentro de `next`; devuelve `p` si puso una ficha nueva. */
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

    /** Tablero con las jugadas colocadas en orden. */
    fun of(vararg placements: String): Board = EMPTY.playAll(placements.asList())

    /** Tablero con estas 225 casillas, fila a fila, y las de `latest` resaltadas. */
    fun ofCells(cells: List<Tile?>, latest: Set<Position> = emptySet()): Board {
      require(cells.size == SIZE * SIZE) { "un tablero tiene ${SIZE * SIZE} casillas, no ${cells.size}" }
      return Board(cells, latest)
    }

    /** Premio de una casilla del tablero estandar. */
    fun bonus(p: Position): Bonus = BonusLayout.at(index(p))

    private fun index(p: Position): Int {
      require(p.onBoard) { "casilla fuera del tablero: $p" }
      return p.row * SIZE + p.column
    }

    private fun positionOf(index: Int) = Position(index / SIZE, index % SIZE)
  }
}
