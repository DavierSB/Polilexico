package app.lexico.ui.classic

const val LEAD_LIMIT = 200

data class EndgameConfig(
  val minBag: Int = 2,
  val maxBag: Int = 8,
  val minLead: Int = -40,
  val maxLead: Int = 0,
  val q: QPlace = QPlace.ANYWHERE,
  val single: Boolean = true,
  val timed: Boolean = false,
  val timeMs: Long = 5 * 60_000L,
  val overtimeMs: Long = 60_000L,
)

enum class QPlace(val engineName: String, val option: String, val summary: String) {
  ANYWHERE("anywhere", "Puede o no haberse jugado", "puede o no haberse jugado"),
  UNPLAYED("unplayed", "No se ha jugado", "no se ha jugado"),
  PLAYED("played", "Ya se jugó", "ya se jugó"),
  YOURS("yours", "Está en tu atril", "está en tu atril"),
  HIDDEN("hidden", "No se ha jugado y no está en tu atril", "no se ha jugado y no está en tu atril"),
}
