package app.lexico.model

/** Los premios del tablero estandar (el mismo que `CrosswordGameLayout` de macondo). */
internal object BonusLayout {
  // Cuadrante superior izquierdo (8x8); el resto sale por simetria.
  // = triple palabra, - doble palabra, " triple letra, ' doble letra, * centro
  private val QUADRANT = listOf(
    "=  '   =",
    " -   \"  ",
    "  -   ' ",
    "'  -   '",
    "    -   ",
    " \"   \"  ",
    "  '   ' ",
    "=  '   *",
  )

  private val SYMBOLS = mapOf(
    '=' to Bonus.TRIPLE_WORD, '-' to Bonus.DOUBLE_WORD, '"' to Bonus.TRIPLE_LETTER,
    '\'' to Bonus.DOUBLE_LETTER, '*' to Bonus.CENTER,
  )

  private val BONUSES: List<Bonus> = List(Board.SIZE * Board.SIZE) { bonusAt(it / Board.SIZE, it % Board.SIZE) }

  /** El premio de la casilla `index` (fila * 15 + columna). */
  fun at(index: Int): Bonus = BONUSES[index]

  private fun bonusAt(row: Int, column: Int): Bonus = SYMBOLS[QUADRANT[fold(row)][fold(column)]] ?: Bonus.NONE

  /** Lleva una fila o columna 8..14 a su simetrica 6..0. */
  private fun fold(i: Int): Int = if (i > 7) 14 - i else i
}
