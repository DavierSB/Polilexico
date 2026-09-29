package app.lexico.game.modes.classic

/**
 * Como sera una partida clasica: el bot rival, si una jugada con palabras no validas hace perder
 * el turno (single) o se rechaza (void), y el tiempo de cada jugador y su descuento (0 = sin
 * tiempo). `showUnseen`: si la bolsa muestra las fichas por salir o solo cuantas quedan.
 */
data class ClassicSetup(
  val bot: String,
  val invalidLosesTurn: Boolean,
  val timeMs: Long,
  val overtimeMs: Long,
  val showUnseen: Boolean = true,
)
