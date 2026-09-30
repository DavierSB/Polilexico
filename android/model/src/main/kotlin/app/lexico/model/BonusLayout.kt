package app.lexico.model

internal object BonusLayout {
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

  fun at(index: Int): Bonus = BONUSES[index]

  private fun bonusAt(row: Int, column: Int): Bonus = SYMBOLS[QUADRANT[fold(row)][fold(column)]] ?: Bonus.NONE

  private fun fold(i: Int): Int = if (i > 7) 14 - i else i
}
