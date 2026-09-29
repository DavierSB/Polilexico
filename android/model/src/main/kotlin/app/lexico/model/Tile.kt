package app.lexico.model

/**
 * Una ficha. `letter` va siempre en mayusculas y los digrafos son una sola ficha ("CH", "LL",
 * "RR"), como en la distribucion `spanish` de macondo. Un comodin lleva la letra que representa y
 * `blank = true`.
 */
data class Tile(val letter: String, val blank: Boolean = false) {
  /** Puntos de la ficha (0 si es comodin). */
  val value: Int get() = if (blank) 0 else Letters.value(letter)

  /** Como se escribe en notacion FISE: minuscula = comodin. */
  override fun toString(): String = if (blank) letter.lowercase() else letter
}
