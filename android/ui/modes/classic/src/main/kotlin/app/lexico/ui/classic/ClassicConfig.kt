package app.lexico.ui.classic

/** Como sera una partida clasica nueva. */
data class ClassicConfig(
  val bot: String = "HastyBot",
  /** Void (false) o single (true). */
  val single: Boolean = false,
  val timed: Boolean = true,
  /** Tiempo por jugador. */
  val timeMs: Long = 20 * 60_000L,
  /** Tiempo de descuento, cuando se acaba el principal. */
  val overtimeMs: Long = 60_000L,
  /** Si la bolsa muestra las fichas por salir o solo cuantas quedan. */
  val showUnseen: Boolean = true,
)
