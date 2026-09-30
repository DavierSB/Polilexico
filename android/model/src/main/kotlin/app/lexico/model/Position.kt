package app.lexico.model

data class Position(val row: Int, val column: Int) {
  val onBoard: Boolean get() = row in 0 until Board.SIZE && column in 0 until Board.SIZE

  override fun toString(): String = "${'A' + row}${column + 1}"
}
