package app.lexico.model

object Letters {
  const val BLANK = "?"

  private val VALUES = mapOf(
    "A" to 1, "B" to 3, "C" to 3, "CH" to 5, "D" to 2, "E" to 1, "F" to 4, "G" to 2, "H" to 4,
    "I" to 1, "J" to 8, "L" to 1, "LL" to 8, "M" to 3, "N" to 1, "Ñ" to 8, "O" to 1, "P" to 3,
    "Q" to 5, "R" to 1, "RR" to 8, "S" to 1, "T" to 1, "U" to 1, "V" to 4, "X" to 8, "Y" to 4,
    "Z" to 10,
  )

  private val VOWELS = setOf("A", "E", "I", "O", "U")

  private val DIGRAPHS = setOf("CH", "LL", "RR")

  val ALL: List<String> = VALUES.keys.toList()

  fun tiles(word: String): List<Tile?> = WordReader(word).read()

  fun value(letter: String): Int = VALUES[letter.uppercase()] ?: 0

  fun isVowel(letter: String): Boolean = letter in VOWELS

  fun isLetter(letter: String): Boolean = letter in VALUES

  fun isDigraph(letters: String): Boolean = letters in DIGRAPHS
}
