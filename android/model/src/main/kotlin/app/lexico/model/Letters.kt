package app.lexico.model

/** Letras del Scrabble en espanol (distribucion `spanish` de macondo / FILE2017). */
object Letters {
  /** Como se escribe el comodin en un atril ("A CH ?"). */
  const val BLANK = "?"

  private val VALUES = mapOf(
    "A" to 1, "B" to 3, "C" to 3, "CH" to 5, "D" to 2, "E" to 1, "F" to 4, "G" to 2, "H" to 4,
    "I" to 1, "J" to 8, "L" to 1, "LL" to 8, "M" to 3, "N" to 1, "Ñ" to 8, "O" to 1, "P" to 3,
    "Q" to 5, "R" to 1, "RR" to 8, "S" to 1, "T" to 1, "U" to 1, "V" to 4, "X" to 8, "Y" to 4,
    "Z" to 10,
  )

  private val VOWELS = setOf("A", "E", "I", "O", "U")

  private val DIGRAPHS = setOf("CH", "LL", "RR")

  /** Todas las letras en orden alfabetico espanol (sin el comodin). */
  val ALL: List<String> = VALUES.keys.toList()

  /**
   * Parte una palabra en fichas. `null` en la lista = casilla ya ocupada ("." o letras entre
   * parentesis, que tambien marcan letras del tablero en notacion GCG).
   *
   * - "CH", "LL", "RR" se leen como un digrafo (como hace macondo); "[CH]" fuerza el digrafo y
   *   "[C]H" lo evita.
   * - Minuscula = comodin: "CAsA", "[ch]", "ñ".
   * - "[N]" = Ñ y "[n]" = comodin como Ñ, para teclados sin Ñ.
   */
  fun tiles(word: String): List<Tile?> = WordReader(word).read()

  /** Puntos de una letra de atril; el comodin ("?") vale 0. */
  fun value(letter: String): Int = VALUES[letter.uppercase()] ?: 0

  fun isVowel(letter: String): Boolean = letter in VOWELS

  fun isLetter(letter: String): Boolean = letter in VALUES

  fun isDigraph(letters: String): Boolean = letters in DIGRAPHS
}
