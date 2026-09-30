package app.lexico.ui.classic

const val LEAD_LIMIT = 200

data class EndgameConfig(
  val maxBag: Int = 8,
  val minLead: Int = -40,
  val maxLead: Int = 0,
  val single: Boolean = false,
  val timed: Boolean = true,
  val timeMs: Long = 5 * 60_000L,
  val overtimeMs: Long = 60_000L,
)
