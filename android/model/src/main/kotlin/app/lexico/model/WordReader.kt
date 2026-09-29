package app.lexico.model

/** Lee una palabra ficha a ficha, con las convenciones de [Letters.tiles]. Se usa una sola vez. */
internal class WordReader(private val word: String) {
  private val tiles = mutableListOf<Tile?>()
  private var i = 0
  private var inParens = false

  fun read(): List<Tile?> {
    while (i < word.length) readNext()
    return tiles
  }

  private fun readNext() {
    when (word[i]) {
      '(' -> { inParens = true; i++ }
      ')' -> { inParens = false; i++ }
      '.' -> { tiles += null; i++ }
      else -> add(nextToken())
    }
  }

  private fun nextToken(): String = if (word[i] == '[') bracketed() else plain()

  /** "[CH]" o "[C]" tal cual; "[N]" y "[n]" son la Ñ. */
  private fun bracketed(): String {
    val end = word.indexOf(']', i)
    require(end > i + 1) { "corchete sin cerrar en \"$word\"" }
    val inside = word.substring(i + 1, end)
    i = end + 1
    return ENHE[inside] ?: inside
  }

  /** Una letra, o dos si forman un digrafo escrito todo en mayusculas o todo en minusculas. */
  private fun plain(): String {
    val two = word.substring(i, minOf(i + 2, word.length))
    val token = if (isDigraph(two)) two else word[i].toString()
    i += token.length
    return token
  }

  private fun isDigraph(two: String): Boolean =
    two.length == 2 && Letters.isDigraph(two.uppercase()) && two.all { it.isUpperCase() == two[0].isUpperCase() }

  /** Minuscula = comodin; entre parentesis = letra que ya esta en el tablero. */
  private fun add(token: String) {
    val letter = token.uppercase()
    require(Letters.isLetter(letter)) { "letra desconocida \"$token\" en \"$word\"" }
    tiles += if (inParens) null else Tile(letter, blank = token.first().isLowerCase())
  }

  private companion object {
    val ENHE = mapOf("N" to "Ñ", "n" to "ñ")
  }
}
