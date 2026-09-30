package app.lexico.game.modes.classic

data class EndgameSetup(
  val maxBag: Int,
  val minLead: Int,
  val maxLead: Int,
  val invalidLosesTurn: Boolean,
  val timeMs: Long,
  val overtimeMs: Long,
)
