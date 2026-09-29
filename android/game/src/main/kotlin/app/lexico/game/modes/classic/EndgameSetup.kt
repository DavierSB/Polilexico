package app.lexico.game.modes.classic

/**
 * Como sera una partida de Finales: la bolsa con `maxBag` fichas o menos y tu ventaja (tus
 * puntos menos los del rival) entre `minLead` y `maxLead` al tomar el turno. Despues es una
 * clasica contra HastyBot, con su comprobacion de jugadas y su tiempo (0 = sin tiempo) y, como en
 * [ClassicSetup], si la bolsa muestra las fichas por salir.
 */
data class EndgameSetup(
  val maxBag: Int,
  val minLead: Int,
  val maxLead: Int,
  val invalidLosesTurn: Boolean,
  val timeMs: Long,
  val overtimeMs: Long,
  val showUnseen: Boolean = true,
)
