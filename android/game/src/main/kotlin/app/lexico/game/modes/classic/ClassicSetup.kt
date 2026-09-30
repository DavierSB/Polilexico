package app.lexico.game.modes.classic

data class ClassicSetup(
  val bot: String,
  val invalidLosesTurn: Boolean,
  val timeMs: Long,
  val overtimeMs: Long,
)
