package app.lexico.model

object Letters {
  const val BLANK = "?"

  private val VALUES = mapOf(
    "A" to 1, "B" to 3, "C" to 3, "CH" to 5, "D" to 2, "E" to 1, "F" to 4, "G" to 2, "H" to 4,
    "I" to 1, "J" to 8, "L" to 1, "LL" to 8, "M" to 3, "N" to 1, "Ñ" to 8, "O" to 1, "P" to 3,
    "Q" to 5, "R" to 1, "RR" to 8, "S" to 1, "T" to 1, "U" to 1, "V" to 4, "X" to 8, "Y" to 4,
    "Z" to 10,
  )

  private val COUNTS = mapOf(
    BLANK to 2, "A" to 12, "B" to 2, "C" to 4, "CH" to 1, "D" to 5, "E" to 12, "F" to 1, "G" to 2, "H" to 2,
    "I" to 6, "J" to 1, "L" to 4, "LL" to 1, "M" to 2, "N" to 5, "Ñ" to 1, "O" to 9, "P" to 2, "Q" to 1,
    "R" to 5, "RR" to 1, "S" to 6, "T" to 4, "U" to 5, "V" to 1, "X" to 1, "Y" to 1, "Z" to 1,
  )

  private val VOWELS = setOf("A", "E", "I", "O", "U")

  private val DIGRAPHS = setOf("CH", "LL", "RR")

  val ALL: List<String> = VALUES.keys.toList()

  val BAG: List<String> = COUNTS.flatMap { (letter, n) -> List(n) { letter } }

  fun tiles(word: String): List<Tile?> = WordReader(word).read()

  fun value(letter: String): Int = VALUES[letter.uppercase()] ?: 0

  fun isVowel(letter: String): Boolean = letter in VOWELS

  fun isLetter(letter: String): Boolean = letter in VALUES

  fun isDigraph(letters: String): Boolean = letters in DIGRAPHS
}
