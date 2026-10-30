package app.lexico.ui.classic

data class ClassicConfig(
  val bot: String = "HastyBot",
  val starter: Int = 0,
  val single: Boolean = true,
  val timed: Boolean = true,
  val timeMs: Long = 30 * 60_000L,
  val overtimeMs: Long = 8 * 60_000L,
  val study: Boolean = false,
  val players: Pair<String, String>? = null,
)
