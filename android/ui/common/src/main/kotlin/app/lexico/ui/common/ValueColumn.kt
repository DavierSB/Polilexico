package app.lexico.ui.common

class ValueColumn(val title: String, val info: String)

val EQUITY_COLUMN = ValueColumn("Equity", EQUITY_INFO)

val FINAL_COLUMN = ValueColumn("Final", FINAL_INFO)

val PRE_ENDGAME_COLUMN = ValueColumn("Equity", PRE_ENDGAME_INFO)

const val FINAL_INFO = "La diferencia de puntos con la que se espera terminar la partida tras esta jugada, si los dos " +
  "siguen la mejor línea encontrada. Positiva: gana quien juega. Al tocar una jugada se ve en el tablero con su línea, " +
  "y con «Jugar» se sigue desde ella."

fun valueColumn(moves: List<RankedMove>): ValueColumn? = when {
  moves.any { it.final != null } -> FINAL_COLUMN
  moves.any { it.adjusted } -> PRE_ENDGAME_COLUMN
  moves.any { it.equity != null } -> EQUITY_COLUMN
  else -> null
}

fun valueText(m: RankedMove): String = m.final?.let { "%+d".format(it) } ?: m.equity?.let { "%.1f".format(it) }.orEmpty()
