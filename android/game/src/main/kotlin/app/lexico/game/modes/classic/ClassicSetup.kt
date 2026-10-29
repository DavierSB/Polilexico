package app.lexico.game.modes.classic

data class ClassicSetup(
  val bot: String,
  val starter: Int,
  val invalidLosesTurn: Boolean,
  val timeMs: Long,
  val overtimeMs: Long,
  val study: Boolean = false,
)
