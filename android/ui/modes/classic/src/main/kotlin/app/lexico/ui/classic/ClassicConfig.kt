package app.lexico.ui.classic

data class ClassicConfig(
  val bot: String = "HastyBot",
  val single: Boolean = false,
  val timed: Boolean = true,
  val timeMs: Long = 20 * 60_000L,
  val overtimeMs: Long = 60_000L,
)
