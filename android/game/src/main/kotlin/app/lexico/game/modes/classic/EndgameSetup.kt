package app.lexico.game.modes.classic

data class EndgameSetup(
  val minBag: Int,
  val maxBag: Int,
  val minLead: Int,
  val maxLead: Int,
  val q: String,
  val invalidLosesTurn: Boolean,
  val timeMs: Long,
  val overtimeMs: Long,
)
