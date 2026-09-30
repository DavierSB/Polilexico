package app.lexico.model

internal object Coordinates {
  private val HORIZONTAL = Regex("^([A-Oa-o])([0-9]{1,2})$")
  private val VERTICAL = Regex("^([0-9]{1,2})([A-Oa-o])$")

  fun parse(text: String): Triple<Int, Int, Boolean> {
    HORIZONTAL.find(text)?.let { return Triple(row(it.groupValues[1]), column(it.groupValues[2], text), false) }
    VERTICAL.find(text)?.let { return Triple(row(it.groupValues[2]), column(it.groupValues[1], text), true) }
    throw IllegalArgumentException("coordenada invalida \"$text\" (ej: h8 horizontal, 8h vertical)")
  }

  private fun row(letter: String): Int = letter.uppercase()[0] - 'A'

  private fun column(number: String, text: String): Int {
    val c = number.toInt()
    require(c in 1..Board.SIZE) { "columna invalida en \"$text\" (1-${Board.SIZE})" }
    return c - 1
  }
}
