package app.lexico.model

data class Tile(val letter: String, val blank: Boolean = false) {
  val value: Int get() = if (blank) 0 else Letters.value(letter)

  override fun toString(): String = if (blank) letter.lowercase() else letter
}
