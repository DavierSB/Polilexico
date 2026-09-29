package app.lexico.ui.classic

/** La ventaja se elige entre -LEAD_LIMIT y LEAD_LIMIT puntos. */
const val LEAD_LIMIT = 200

/**
 * Como sera una partida de Finales: HastyBot juega contra si mismo hasta que la bolsa baja a
 * `maxBag` fichas; si tu ventaja (tus puntos menos los suyos) esta entre `minLead` y `maxLead`,
 * la partida sigue contigo contra HastyBot hasta el final, como una clasica.
 */
data class EndgameConfig(
  val maxBag: Int = 8,
  val minLead: Int = -40,
  val maxLead: Int = 0,
  /** Void (false) o single (true). */
  val single: Boolean = false,
  val timed: Boolean = true,
  val timeMs: Long = 5 * 60_000L,
  val overtimeMs: Long = 60_000L,
  /** Si la bolsa muestra las fichas por salir o solo cuantas quedan. */
  val showUnseen: Boolean = true,
)
